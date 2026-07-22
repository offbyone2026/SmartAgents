package com.smartagents.shared.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class BlueprintUiState(
    val blueprint: ProjectBlueprint = ProjectBlueprint("", "", "", emptyMap()),
    val selectedNodeId: String? = null,
    val selectedNode: ProjectNode? = null,
    val editingNodeId: String? = null,
    val editingField: String? = null, // "title" or "icon"
    val groups: List<NodeGroup> = emptyList(),
    val isDirty: Boolean = false,
    val error: String? = null,

    // Search
    val searchQuery: String = "",
    val searchResults: Set<String> = emptySet(),

    // Undo/Redo
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,

    // Multi-project
    val projects: List<ProjectMeta> = emptyList(),
)

class BlueprintViewModel(
    private val repository: ProjectRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(BlueprintUiState())
    val state: StateFlow<BlueprintUiState> = _state.asStateFlow()

    init {
        scope.launch {
            val bp = repository.loadBlueprint()
            val projs = repository.listProjects()
            _state.value = _state.value.copy(blueprint = bp, groups = buildGroups(bp), projects = projs)
        }
    }

    private fun buildGroups(bp: ProjectBlueprint): List<NodeGroup> {
        val tagMap = mutableMapOf<String, MutableList<String>>()
        bp.nodes.values.forEach { node ->
            node.tags.forEach { tag -> tagMap.getOrPut(tag) { mutableListOf() }.add(node.id) }
        }
        return tagMap.filter { it.value.size >= 2 }.map { (tag, ids) -> NodeGroup(tag, label = tag, nodeIds = ids) }
    }

    // --- Selection ---

    fun selectNode(nodeId: String?) {
        val bp = _state.value.blueprint
        _state.value = _state.value.copy(
            selectedNodeId = nodeId,
            selectedNode = nodeId?.let { bp.nodes[it] },
            editingNodeId = null,
            editingField = null,
        )
    }

    fun startEditing(nodeId: String, field: String = "title") {
        _state.value = _state.value.copy(editingNodeId = nodeId, editingField = field)
    }

    fun finishEditing() {
        _state.value = _state.value.copy(editingNodeId = null, editingField = null)
    }

    // --- Node mutations ---

    fun updateNodeStatus(nodeId: String, status: NodeStatus) {
        val bp = _state.value.blueprint
        val node = bp.nodes[nodeId] ?: return
        scope.launch { repository.updateNode(node.copy(status = status)); refresh() }
    }

    fun updateNodeTitle(nodeId: String, title: String) {
        val bp = _state.value.blueprint
        val node = bp.nodes[nodeId] ?: return
        scope.launch { repository.updateNode(node.copy(title = title)); refresh(); finishEditing() }
    }

    fun updateNodeIcon(nodeId: String, icon: NodeIcon) {
        val bp = _state.value.blueprint
        val node = bp.nodes[nodeId] ?: return
        scope.launch { repository.updateNode(node.copy(icon = icon)); refresh(); finishEditing() }
    }

    fun addChildNode(parentId: String, title: String = "新模块") {
        scope.launch {
            val bp = _state.value.blueprint
            val parent = bp.nodes[parentId] ?: return@launch
            repository.addNode(parentId, ProjectNode(
                id = UUID.randomUUID().toString().take(8), parentId = parentId, title = title, order = parent.children.size,
            ))
            refresh()
        }
    }

    fun deleteNode(nodeId: String) {
        scope.launch {
            repository.deleteNode(nodeId)
            _state.value = _state.value.copy(selectedNodeId = null, selectedNode = null, editingNodeId = null, editingField = null)
            refresh()
        }
    }

    fun reorderChildren(parentId: String, fromIndex: Int, toIndex: Int) {
        val bp = _state.value.blueprint
        val parent = bp.nodes[parentId] ?: return
        val newOrder = parent.children.toMutableList()
        if (fromIndex in newOrder.indices && toIndex in newOrder.indices) {
            val moved = newOrder.removeAt(fromIndex)
            newOrder.add(toIndex, moved)
            scope.launch { repository.reorderChildren(parentId, newOrder); refresh() }
        }
    }

    // --- Undo/Redo ---

    fun undo() {
        scope.launch {
            (repository as? com.smartagents.desktop.project.DesktopProjectRepository)?.undo()
            refresh()
        }
    }

    fun redo() {
        scope.launch {
            (repository as? com.smartagents.desktop.project.DesktopProjectRepository)?.redo()
            refresh()
        }
    }

    // --- Search ---

    fun setSearchQuery(query: String) {
        val bp = _state.value.blueprint
        val results = if (query.isBlank()) emptySet()
        else bp.nodes.values.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.id == query
        }.map { it.id }.toSet()
        _state.value = _state.value.copy(searchQuery = query, searchResults = results)
    }

    fun clearSearch() {
        _state.value = _state.value.copy(searchQuery = "", searchResults = emptySet())
    }

    // --- Version ---

    fun snapshotVersion(title: String, description: String) {
        scope.launch {
            val bp = _state.value.blueprint
            val tl = repository.loadTimeline()
            val mainVersions = tl.versions.filter { it.branchName == "main" }
            val next = "v0.${mainVersions.size + 1}.0"
            repository.createVersion(VersionRecord(version = next, timestamp = 0L, title = title, description = description, snapshot = bp.nodes))
            _state.value = _state.value.copy(isDirty = false)
        }
    }

    // --- Multi-project ---

    fun switchProject(projectId: String) {
        scope.launch { repository.loadProject(projectId); refresh() }
    }

    fun createProject(name: String) {
        scope.launch { repository.createProject(name); refresh() }
    }

    fun deleteProject(projectId: String) {
        scope.launch { repository.deleteProject(projectId); refresh() }
    }

    fun renameProject(projectId: String, name: String) {
        scope.launch { repository.renameProject(projectId, name); refresh() }
    }

    // --- Internal ---

    private suspend fun refresh() {
        val bp = repository.loadBlueprint()
        val projs = repository.listProjects()
        _state.value = _state.value.copy(
            blueprint = bp,
            groups = buildGroups(bp),
            selectedNode = _state.value.selectedNodeId?.let { bp.nodes[it] },
            isDirty = true,
            projects = projs,
            // re-apply search
            searchResults = if (_state.value.searchQuery.isBlank()) emptySet()
            else bp.nodes.values.filter {
                it.title.contains(_state.value.searchQuery, ignoreCase = true) ||
                it.description.contains(_state.value.searchQuery, ignoreCase = true)
            }.map { it.id }.toSet(),
        )
    }

    // --- Export ---

    fun exportPng(path: String) {
        scope.launch {
            // Compose Desktop Canvas capture — caller provides the bitmap from native layer
            _state.value = _state.value.copy(error = null)
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
