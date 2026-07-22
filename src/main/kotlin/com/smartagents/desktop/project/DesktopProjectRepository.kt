package com.smartagents.desktop.project

import com.smartagents.shared.project.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class DesktopProjectRepository : ProjectRepository {

    // --- Multi-project ---
    private val projects = mutableListOf(
        ProjectMeta("default", "未命名项目")
    )
    private val projectData = mutableMapOf(
        "default" to ProjectBlueprint(
            projectId = "default",
            projectName = "未命名项目",
            rootNodeId = "root",
            nodes = mapOf(
                "root" to ProjectNode(
                    id = "root",
                    title = "项目根",
                    description = "项目根节点",
                    status = NodeStatus.IMPROVING,
                    icon = NodeIcon.ROOT,
                )
            ),
        )
    )
    private val timelines = mutableMapOf(
        "default" to VersionTimeline("default", emptyList(), mapOf("main" to emptyList()))
    )
    private var currentProjectId = "default"

    private val _blueprint = MutableStateFlow(projectData[currentProjectId]!!)
    val blueprint: StateFlow<ProjectBlueprint> = _blueprint

    private val _timeline = MutableStateFlow(timelines[currentProjectId]!!)
    val timeline: StateFlow<VersionTimeline> = _timeline

    private val _projectList = MutableStateFlow(projects.toList())
    val projectList: StateFlow<List<ProjectMeta>> = _projectList

    // --- Undo/Redo ---
    private val undoStack = mutableListOf<BlueprintAction>()
    private val redoStack = mutableListOf<BlueprintAction>()
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo

    private fun pushAction(action: BlueprintAction) {
        undoStack.add(action)
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
        if (undoStack.size > 50) undoStack.removeFirst()
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val action = undoStack.removeLast()
        redoStack.add(action)
        when (action) {
            is BlueprintAction.UpdateNode -> updateNodeRaw(action.previous, pushHistory = false)
            is BlueprintAction.AddNode -> deleteNodeRaw(action.node.id, pushHistory = false)
            is BlueprintAction.DeleteNode -> restoreNodeRaw(action, pushHistory = false)
            is BlueprintAction.ReorderChildren -> reorderChildrenRaw(action.parentId, action.oldOrder, pushHistory = false)
        }
        undoStack.removeLast() // undo updateNode etc also pushed actions, remove those
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        refreshFlows()
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val action = redoStack.removeLast()
        undoStack.add(action)
        when (action) {
            is BlueprintAction.UpdateNode -> updateNodeRaw(action.node, pushHistory = false)
            is BlueprintAction.AddNode -> addNodeRaw(action.parentId, action.node, pushHistory = false)
            is BlueprintAction.DeleteNode -> deleteNodeRaw(action.node.id, pushHistory = false)
            is BlueprintAction.ReorderChildren -> reorderChildrenRaw(action.parentId, action.newOrder, pushHistory = false)
        }
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        refreshFlows()
        return true
    }

    private fun updateNodeRaw(node: ProjectNode, pushHistory: Boolean) {
        val bp = _blueprint.value
        val previous = bp.nodes[node.id] ?: return
        _blueprint.value = bp.copy(nodes = bp.nodes + (node.id to node.copy(updatedAt = System.currentTimeMillis())))
        projectData[currentProjectId] = _blueprint.value
        if (pushHistory) pushAction(BlueprintAction.UpdateNode(node, previous))
    }

    private fun addNodeRaw(parentId: String, node: ProjectNode, pushHistory: Boolean): ProjectNode {
        val bp = _blueprint.value
        val parent = bp.nodes[parentId]!!
        val created = node.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), parentId = parentId)
        val updatedParent = parent.copy(children = parent.children + created.id, updatedAt = System.currentTimeMillis())
        _blueprint.value = bp.copy(nodes = bp.nodes + (created.id to created) + (parentId to updatedParent))
        projectData[currentProjectId] = _blueprint.value
        if (pushHistory) pushAction(BlueprintAction.AddNode(created, parentId))
        return created
    }

    private fun deleteNodeRaw(nodeId: String, pushHistory: Boolean) {
        val bp = _blueprint.value
        val node = bp.nodes[nodeId] ?: return
        val parent = node.parentId?.let { bp.nodes[it] }

        fun collectSubtree(id: String): List<ProjectNode> =
            listOf(bp.nodes[id]!!) + (bp.nodes[id]!!.children.flatMap { collectSubtree(it) })

        val children = node.children.flatMap { collectSubtree(it) }

        val newNodes = bp.nodes.toMutableMap()
        fun removeRecursive(id: String) { bp.nodes[id]?.children?.forEach { newNodes.remove(it); removeRecursive(it) }; newNodes.remove(id) }
        removeRecursive(nodeId)

        parent?.let { newNodes[parent.id] = parent.copy(children = parent.children - nodeId, updatedAt = System.currentTimeMillis()) }
        _blueprint.value = bp.copy(nodes = newNodes)
        projectData[currentProjectId] = _blueprint.value
        if (pushHistory) pushAction(BlueprintAction.DeleteNode(node, children, node.parentId))
    }

    private fun restoreNodeRaw(action: BlueprintAction.DeleteNode, pushHistory: Boolean) {
        val bp = _blueprint.value
        val newNodes = bp.nodes.toMutableMap()
        newNodes[action.node.id] = action.node
        action.children.forEach { newNodes[it.id] = it }
        action.parentId?.let { pid ->
            val p = newNodes[pid]!!; newNodes[pid] = p.copy(children = p.children + action.node.id, updatedAt = System.currentTimeMillis())
        }
        _blueprint.value = bp.copy(nodes = newNodes)
        projectData[currentProjectId] = _blueprint.value
        if (pushHistory) pushAction(action) // inverse
    }

    private fun reorderChildrenRaw(parentId: String, newOrder: List<String>, pushHistory: Boolean) {
        val bp = _blueprint.value
        val parent = bp.nodes[parentId] ?: return
        val oldOrder = parent.children
        _blueprint.value = bp.copy(nodes = bp.nodes + (parentId to parent.copy(children = newOrder, updatedAt = System.currentTimeMillis())))
        projectData[currentProjectId] = _blueprint.value
        if (pushHistory) pushAction(BlueprintAction.ReorderChildren(parentId, newOrder, oldOrder))
    }

    private fun refreshFlows() {
        _blueprint.value = projectData[currentProjectId]!!
        _timeline.value = timelines[currentProjectId]!!
    }

    // --- ProjectRepository impl ---

    override suspend fun loadBlueprint(): ProjectBlueprint = _blueprint.value
    override suspend fun saveBlueprint(blueprint: ProjectBlueprint) {
        _blueprint.value = blueprint.copy(version = blueprint.version + 1)
        projectData[currentProjectId] = _blueprint.value
    }

    override suspend fun updateNode(node: ProjectNode) = updateNodeRaw(node, pushHistory = true)
    override suspend fun addNode(parentId: String, node: ProjectNode): ProjectNode = addNodeRaw(parentId, node, pushHistory = true)
    override suspend fun deleteNode(nodeId: String) = deleteNodeRaw(nodeId, pushHistory = true)
    override suspend fun reorderChildren(parentId: String, newOrder: List<String>) = reorderChildrenRaw(parentId, newOrder, pushHistory = true)

    override suspend fun loadTimeline(): VersionTimeline = _timeline.value

    override suspend fun createVersion(record: VersionRecord): VersionRecord {
        val tl = _timeline.value
        val created = record.copy(timestamp = System.currentTimeMillis())
        val branchVersions = tl.branches[created.branchName]?.toMutableList() ?: mutableListOf()
        branchVersions.add(created.version)
        _timeline.value = tl.copy(
            versions = tl.versions + created,
            branches = tl.branches + (created.branchName to branchVersions),
        )
        timelines[currentProjectId] = _timeline.value
        return created
    }

    override suspend fun rollbackToVersion(version: String): ProjectBlueprint {
        val tl = _timeline.value
        val record = tl.versions.find { it.version == version } ?: return _blueprint.value
        val restored = _blueprint.value.copy(nodes = record.snapshot, version = _blueprint.value.version + 1)
        _blueprint.value = restored
        projectData[currentProjectId] = _blueprint.value
        return restored
    }

    override suspend fun listProjects(): List<ProjectMeta> = projects.toList()

    override suspend fun loadProject(projectId: String): ProjectBlueprint {
        currentProjectId = projectId
        refreshFlows()
        return _blueprint.value
    }

    override suspend fun createProject(name: String): ProjectBlueprint {
        val id = UUID.randomUUID().toString().take(8)
        val rootId = "root"
        val bp = ProjectBlueprint(
            projectId = id, projectName = name, rootNodeId = rootId,
            nodes = mapOf(rootId to ProjectNode(id = rootId, title = name, description = "项目根", status = NodeStatus.IMPROVING, icon = NodeIcon.ROOT)),
        )
        projects.add(ProjectMeta(id, name))
        projectData[id] = bp
        timelines[id] = VersionTimeline(id, emptyList(), mapOf("main" to emptyList()))
        currentProjectId = id
        _projectList.value = projects.toList()
        refreshFlows()
        return bp
    }

    override suspend fun deleteProject(projectId: String) {
        projects.removeAll { it.projectId == projectId }
        projectData.remove(projectId)
        timelines.remove(projectId)
        if (projectId == currentProjectId && projects.isNotEmpty()) {
            currentProjectId = projects.first().projectId
        }
        _projectList.value = projects.toList()
        refreshFlows()
    }

    override suspend fun renameProject(projectId: String, name: String) {
        val idx = projects.indexOfFirst { it.projectId == projectId }
        if (idx >= 0) projects[idx] = projects[idx].copy(projectName = name, updatedAt = System.currentTimeMillis())
        _projectList.value = projects.toList()
        val bp = projectData[projectId] ?: return
        projectData[projectId] = bp.copy(projectName = name)
        if (currentProjectId == projectId) refreshFlows()
    }
}
