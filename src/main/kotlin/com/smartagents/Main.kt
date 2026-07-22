package com.smartagents

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.smartagents.desktop.project.DesktopProjectRepository
import com.smartagents.desktop.project.ui.ProjectDashboard
import com.smartagents.shared.project.BlueprintViewModel
import com.smartagents.shared.project.VersionViewModel

fun main() = application {
    val repository = DesktopProjectRepository()
    val scope = rememberCoroutineScope()
    val blueprintViewModel = BlueprintViewModel(repository, scope)
    val versionViewModel = VersionViewModel(repository, scope)

    Window(
        onCloseRequest = ::exitApplication,
        title = "SmartAgents Project Manager",
        state = rememberWindowState(size = DpSize(1200.dp, 800.dp))
    ) {
        ProjectDashboard(blueprintViewModel, versionViewModel)
    }
}
