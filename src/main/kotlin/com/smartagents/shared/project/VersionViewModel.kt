package com.smartagents.shared.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VersionUiState(
    val timeline: VersionTimeline = VersionTimeline("", emptyList(), emptyMap()),
    val selectedVersion: VersionRecord? = null,
    val previewNodes: Map<String, ProjectNode> = emptyMap(),
    val activeBranch: String = "main",
    val error: String? = null,
)

class VersionViewModel(
    private val repository: ProjectRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(VersionUiState())
    val state: StateFlow<VersionUiState> = _state.asStateFlow()

    init {
        scope.launch {
            val tl = repository.loadTimeline()
            _state.value = _state.value.copy(timeline = tl)
        }
    }

    fun selectVersion(version: String) {
        val record = _state.value.timeline.versions.find { it.version == version }
        _state.value = _state.value.copy(
            selectedVersion = record,
            previewNodes = record?.snapshot ?: emptyMap(),
        )
    }

    fun createVersion(title: String, description: String) {
        scope.launch {
            val bp = repository.loadBlueprint()
            val tl = repository.loadTimeline()
            val branch = _state.value.activeBranch
            val branchVersions = tl.versions.filter { it.branchName == branch }
            val next = "v0.${branchVersions.size + 1}.0"
            val record = VersionRecord(
                version = next,
                timestamp = 0L,
                title = title,
                description = description,
                snapshot = bp.nodes,
                branchName = branch,
            )
            repository.createVersion(record)
            _state.value = _state.value.copy(
                timeline = repository.loadTimeline(),
                selectedVersion = null,
                previewNodes = emptyMap(),
            )
        }
    }

    fun rollback(version: String) {
        scope.launch {
            try {
                repository.rollbackToVersion(version)
                _state.value = _state.value.copy(error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "回滚失败: ${e.message}")
            }
        }
    }

    fun switchBranch(branch: String) {
        _state.value = _state.value.copy(activeBranch = branch)
    }

    fun createBranch(branchName: String, fromVersion: String) {
        scope.launch {
            val tl = repository.loadTimeline()
            val source = tl.versions.find { it.version == fromVersion } ?: return@launch
            val newBranches = tl.branches.toMutableMap()
            newBranches[branchName] = listOf(fromVersion)
            _state.value = _state.value.copy(
                timeline = tl.copy(branches = newBranches),
                activeBranch = branchName,
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
