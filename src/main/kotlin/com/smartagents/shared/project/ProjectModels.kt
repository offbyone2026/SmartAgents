package com.smartagents.shared.project

import kotlinx.serialization.Serializable

/* ---------- Base Enums & Data ---------- */

@Serializable
enum class NodeStatus {
    PLANNED,      // 计划中
    DONE,         // 已完成
    IMPROVING,    // 待提高
    ABANDONED,    // 已放弃
}

@Serializable
enum class NodeIcon(val emoji: String) {
    NONE(""),
    ROOT("\uD83C\uDFE0"),      // 🏠
    MODULE("\uD83D\uDCE6"),     // 📦
    FEATURE("\u2B50"),          // ⭐
    BUG("\uD83D\uDC1B"),        // 🐛
    DOCS("\uD83D\uDCC4"),       // 📄
    UI("\uD83C\uDFA8"),         // 🎨
    DATA("\uD83D\uDCCA"),       // 📊
    API("\uD83D\uDD17"),        // 🔗
    CONFIG("\u2699\uFE0F"),     // ⚙️
    TEST("\uD83E\uDDEA"),       // 🧪
    DEPLOY("\uD83D\uDE80"),     // 🚀
    SECURITY("\uD83D\uDD12"),   // 🔒
}

@Serializable
data class ProjectNode(
    val id: String,
    val parentId: String? = null,
    val title: String,
    val description: String = "",
    val status: NodeStatus = NodeStatus.PLANNED,
    val icon: NodeIcon = NodeIcon.NONE,
    val children: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val order: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class ProjectBlueprint(
    val projectId: String,
    val projectName: String,
    val rootNodeId: String,
    val nodes: Map<String, ProjectNode>,
    val version: Int = 1,
)

@Serializable
data class NodeGroup(
    val tag: String,
    val label: String,
    val nodeIds: List<String>,
)

/* ---------- Version ---------- */

@Serializable
data class VersionRecord(
    val version: String,
    val timestamp: Long,
    val title: String,
    val description: String = "",
    val changedNodeIds: List<String> = emptyList(),
    val snapshot: Map<String, ProjectNode> = emptyMap(),
    val artifacts: List<String> = emptyList(),
    val parentBranch: String? = null,
    val branchName: String = "main",
)

@Serializable
data class VersionTimeline(
    val projectId: String,
    val versions: List<VersionRecord>,
    val branches: Map<String, List<String>>,
)

/* ---------- Multi-Project ---------- */

@Serializable
data class ProjectMeta(
    val projectId: String,
    val projectName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/* ---------- Undo/Redo ---------- */

sealed class BlueprintAction {
    data class UpdateNode(val node: ProjectNode, val previous: ProjectNode) : BlueprintAction()
    data class AddNode(val node: ProjectNode, val parentId: String) : BlueprintAction()
    data class DeleteNode(val node: ProjectNode, val children: List<ProjectNode>, val parentId: String?) : BlueprintAction()
    data class ReorderChildren(val parentId: String, val newOrder: List<String>, val oldOrder: List<String>) : BlueprintAction()
}

/* ---------- Repository ---------- */

interface ProjectRepository {
    suspend fun loadBlueprint(): ProjectBlueprint
    suspend fun saveBlueprint(blueprint: ProjectBlueprint)
    suspend fun updateNode(node: ProjectNode)
    suspend fun addNode(parentId: String, node: ProjectNode): ProjectNode
    suspend fun deleteNode(nodeId: String)
    suspend fun reorderChildren(parentId: String, newOrder: List<String>)

    suspend fun loadTimeline(): VersionTimeline
    suspend fun createVersion(record: VersionRecord): VersionRecord
    suspend fun rollbackToVersion(version: String): ProjectBlueprint

    // Multi-project
    suspend fun listProjects(): List<ProjectMeta>
    suspend fun loadProject(projectId: String): ProjectBlueprint
    suspend fun createProject(name: String): ProjectBlueprint
    suspend fun deleteProject(projectId: String)
    suspend fun renameProject(projectId: String, name: String)
}

/* ---------- Persistence ---------- */

@Serializable
data class PersistedState(
    val projects: List<ProjectMeta> = emptyList(),
    val projectData: Map<String, ProjectBlueprint> = emptyMap(),
    val timelines: Map<String, VersionTimeline> = emptyMap(),
    val currentProjectId: String = "",
)
