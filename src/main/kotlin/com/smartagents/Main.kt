package com.smartagents

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.smartagents.desktop.project.DesktopProjectRepository
import com.smartagents.desktop.project.ui.ProjectDashboard
import com.smartagents.shared.project.BlueprintViewModel
import com.smartagents.shared.project.VersionViewModel

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val repository = DesktopProjectRepository()
    val scope = rememberCoroutineScope()
    val blueprintViewModel = BlueprintViewModel(repository, scope)
    val versionViewModel = VersionViewModel(repository, scope)

    Window(
        onCloseRequest = ::exitApplication,
        title = "SmartAgents Project Manager",
        state = rememberWindowState(size = DpSize(1200.dp, 800.dp)),
        onKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                val ctrl = event.isCtrlPressed
                val shift = event.isShiftPressed
                when {
                    ctrl && event.key == Key.Z && !shift -> { blueprintViewModel.undo(); true }
                    ctrl && event.key == Key.Z && shift -> { blueprintViewModel.redo(); true }
                    ctrl && event.key == Key.Y -> { blueprintViewModel.redo(); true }
                    else -> false
                }
            } else false
        }
    ) {
        ProjectDashboard(blueprintViewModel, versionViewModel)
    }
}
