package com.smartagents.desktop.project.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartagents.shared.project.*

enum class ProjectTab { BLUEPRINT, VERSION }

@Composable
fun ProjectDashboard(
    blueprintViewModel: BlueprintViewModel,
    versionViewModel: VersionViewModel,
    modifier: Modifier = Modifier,
) {
    val bpState by blueprintViewModel.state.collectAsState()
    val vState by versionViewModel.state.collectAsState()

    var activeTab by remember { mutableStateOf(ProjectTab.BLUEPRINT) }

    // Version dialog
    var showVersionDialog by remember { mutableStateOf(false) }
    var versionTitle by remember { mutableStateOf("") }
    var versionDesc by remember { mutableStateOf("") }

    // Project dialog
    var showProjectDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }

    // Icon picker
    var showIconPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0A14))) {
        // --- Top bar: Search + Undo/Redo + Project switcher ---
        Row(
            Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF060610)).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Project switcher
            var projectExpanded by remember { mutableStateOf(false) }
            Box {
                Surface(onClick = { projectExpanded = true }, shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E88E5).copy(alpha = 0.15f), modifier = Modifier.height(30.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(bpState.blueprint.projectName, color = Color(0xFF90CAF9), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        Text("\u25BE", color = Color(0xFF90CAF9), fontSize = 10.sp)
                    }
                }
                DropdownMenu(expanded = projectExpanded, onDismissRequest = { projectExpanded = false },
                    modifier = Modifier.background(Color(0xFF1A1A2E))) {
                    bpState.projects.forEach { proj ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(proj.projectName, color = Color.White, fontSize = 12.sp)
                                    if (proj.projectId != bpState.blueprint.projectId) {
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = {
                                            blueprintViewModel.deleteProject(proj.projectId); projectExpanded = false
                                        }, modifier = Modifier.size(20.dp)) {
                                            Text("\uD83D\uDDD1", fontSize = 10.sp)
                                        }
                                    }
                                }
                            },
                            onClick = { blueprintViewModel.switchProject(proj.projectId); projectExpanded = false },
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    DropdownMenuItem(text = { Text("+ 新建项目", color = Color(0xFF90CAF9), fontSize = 12.sp) },
                        onClick = { projectExpanded = false; showProjectDialog = true })
                }
            }

            Spacer(Modifier.weight(1f))

            // Undo / Redo
            val repo = blueprintViewModel as? com.smartagents.desktop.project.DesktopProjectRepository
            IconButton(onClick = { blueprintViewModel.undo() }, modifier = Modifier.size(28.dp)) {
                Text("\u21A9", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            }
            IconButton(onClick = { blueprintViewModel.redo() }, modifier = Modifier.size(28.dp)) {
                Text("\u21AA", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            }

            Spacer(Modifier.width(12.dp))

            // Search
            var searchText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it; blueprintViewModel.setSearchQuery(it) },
                singleLine = true,
                placeholder = { Text("搜索节点...", color = Color.White.copy(alpha = 0.25f), fontSize = 12.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 12.sp),
                modifier = Modifier.width(180.dp).height(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                ),
            )
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { searchText = ""; blueprintViewModel.clearSearch() }, modifier = Modifier.size(24.dp)) {
                    Text("\u2715", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }
            if (searchText.isNotEmpty()) {
                Text("${bpState.searchResults.size} 结果", color = Color(0xFF90CAF9), fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }

        // --- Tabs ---
        TabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = Color(0xFF0A0A14),
            contentColor = Color(0xFF1E88E5),
            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.05f)) },
        ) {
            ProjectTab.entries.forEach { tab ->
                Tab(selected = activeTab == tab, onClick = { activeTab = tab }, text = {
                    Text(when (tab) { ProjectTab.BLUEPRINT -> "项目蓝图"; ProjectTab.VERSION -> "版本记录" },
                        color = if (activeTab == tab) Color(0xFF90CAF9) else Color.White.copy(alpha = 0.5f))
                })
            }
        }

        AnimatedContent(targetState = activeTab, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "tab", modifier = Modifier.weight(1f)) { tab ->
            when (tab) {
                ProjectTab.BLUEPRINT -> {
                    Row(Modifier.fillMaxSize()) {
                        NodeDetailPanel(
                            selectedNode = bpState.selectedNode,
                            groups = bpState.groups,
                            editingNodeId = bpState.editingNodeId,
                            onStatusChange = { status -> bpState.selectedNodeId?.let { blueprintViewModel.updateNodeStatus(it, status) } },
                            onTitleChange = { title -> bpState.selectedNodeId?.let { blueprintViewModel.updateNodeTitle(it, title) } },
                            onIconChange = { icon -> bpState.selectedNodeId?.let { blueprintViewModel.updateNodeIcon(it, icon) } },
                            onAddChild = { title -> bpState.selectedNodeId?.let { blueprintViewModel.addChildNode(it, title) } },
                            onDelete = { bpState.selectedNodeId?.let { blueprintViewModel.deleteNode(it) } },
                            onSnapshot = { versionTitle = ""; versionDesc = ""; showVersionDialog = true },
                            onShowIconPicker = { showIconPicker = true },
                        )

                        BlueprintChart(
                            blueprint = bpState.blueprint,
                            selectedNodeId = bpState.selectedNodeId,
                            groups = bpState.groups,
                            searchResults = bpState.searchResults,
                            editingNodeId = bpState.editingNodeId,
                            onNodeClicked = { blueprintViewModel.selectNode(it) },
                            onNodeDoubleClicked = { blueprintViewModel.startEditing(it) },
                            onNodeRightClicked = { _, _ -> },
                            onNodeDragReorder = { parentId, from, to -> blueprintViewModel.reorderChildren(parentId, from, to) },
                            onEditCommit = { title -> bpState.editingNodeId?.let { blueprintViewModel.updateNodeTitle(it, title) } },
                            onContextAddChild = { id -> blueprintViewModel.addChildNode(id, "新模块") },
                            onContextDeleteNode = { id -> blueprintViewModel.deleteNode(id) },
                            onContextSetStatus = { id, status -> blueprintViewModel.updateNodeStatus(id, status) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    StatusLegend()
                }

                ProjectTab.VERSION -> {
                    VersionChart(
                        versions = vState.timeline.versions,
                        selectedVersion = vState.selectedVersion,
                        activeBranch = vState.activeBranch,
                        branches = vState.timeline.branches,
                        onVersionClicked = { versionViewModel.selectVersion(it) },
                        onRollback = { versionViewModel.rollback(it) },
                        onBranchSwitch = { versionViewModel.switchBranch(it) },
                    )
                }
            }
        }
    }

    // --- Version dialog ---
    if (showVersionDialog) {
        AlertDialog(
            onDismissRequest = { showVersionDialog = false },
            title = { Text("记录版本", color = Color.White, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = versionTitle, onValueChange = { versionTitle = it }, label = { Text("版本标题") }, singleLine = true,
                        colors = dialogFieldColors())
                    OutlinedTextField(value = versionDesc, onValueChange = { versionDesc = it }, label = { Text("版本描述") }, maxLines = 3,
                        colors = dialogFieldColors())
                }
            },
            confirmButton = { Button(onClick = { blueprintViewModel.snapshotVersion(versionTitle.ifBlank { "版本快照" }, versionDesc); showVersionDialog = false },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) { Text("记录") } },
            dismissButton = { TextButton(onClick = { showVersionDialog = false }) { Text("取消", color = Color.White.copy(alpha = 0.5f)) } },
            containerColor = Color(0xFF1A1A2E),
        )
    }

    // --- New project dialog ---
    if (showProjectDialog) {
        AlertDialog(
            onDismissRequest = { showProjectDialog = false },
            title = { Text("新建项目", color = Color.White, fontSize = 16.sp) },
            text = {
                OutlinedTextField(value = newProjectName, onValueChange = { newProjectName = it }, label = { Text("项目名称") }, singleLine = true,
                    colors = dialogFieldColors())
            },
            confirmButton = { Button(onClick = { blueprintViewModel.createProject(newProjectName.ifBlank { "新项目" }); showProjectDialog = false; newProjectName = "" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) { Text("创建") } },
            dismissButton = { TextButton(onClick = { showProjectDialog = false }) { Text("取消", color = Color.White.copy(alpha = 0.5f)) } },
            containerColor = Color(0xFF1A1A2E),
        )
    }

    // --- Icon picker dialog ---
    if (showIconPicker) {
        AlertDialog(
            onDismissRequest = { showIconPicker = false },
            title = { Text("选择图标", color = Color.White, fontSize = 16.sp) },
            text = {
                Column {
                    val cols = 6
                    NodeIcon.entries.filter { it != NodeIcon.NONE }.chunked(cols).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                            row.forEach { icon ->
                                val selected = bpState.selectedNode?.icon == icon
                                Surface(onClick = {
                                    bpState.selectedNodeId?.let { blueprintViewModel.updateNodeIcon(it, icon) }
                                    showIconPicker = false
                                }, shape = RoundedCornerShape(6.dp),
                                    color = if (selected) Color(0xFF1E88E5).copy(alpha = 0.25f) else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.08f)),
                                    modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(icon.emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        bpState.selectedNodeId?.let { blueprintViewModel.updateNodeIcon(it, NodeIcon.NONE) }
                        showIconPicker = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("清除图标", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showIconPicker = false }) { Text("关闭", color = Color.White.copy(alpha = 0.5f)) } },
            containerColor = Color(0xFF1A1A2E),
        )
    }
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF1E88E5), unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = Color(0xFF90CAF9), unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
)

@Composable
fun NodeDetailPanel(
    selectedNode: ProjectNode?,
    groups: List<NodeGroup>,
    editingNodeId: String?,
    onStatusChange: (NodeStatus) -> Unit,
    onTitleChange: (String) -> Unit,
    onIconChange: (NodeIcon) -> Unit = {},
    onAddChild: (String) -> Unit,
    onDelete: () -> Unit,
    onSnapshot: () -> Unit,
    onShowIconPicker: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (selectedNode == null) {
        Box(Modifier.width(260.dp).fillMaxHeight().background(Color(0xFF0E0E18)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("点击节点查看详情", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
                Text("长按节点打开菜单", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp)
            }
        }
        return
    }

    Column(
        Modifier.width(260.dp).fillMaxHeight().background(Color(0xFF0E0E18)).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Icon + Title row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(onClick = onShowIconPicker, shape = RoundedCornerShape(6.dp),
                color = if (selectedNode.icon != NodeIcon.NONE) Color.White.copy(alpha = 0.08f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(selectedNode.icon.emoji.ifEmpty { "+" }, fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.width(10.dp))
            if (editingNodeId == selectedNode.id) {
                var editText by remember { mutableStateOf(selectedNode.title) }
                OutlinedTextField(value = editText, onValueChange = { editText = it }, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF1E88E5), unfocusedBorderColor = Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f).height(48.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onTitleChange(editText) }))
            } else {
                Text(selectedNode.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // Status
        Text("状态", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(NodeStatus.PLANNED to "计划", NodeStatus.DONE to "完成", NodeStatus.IMPROVING to "待提高", NodeStatus.ABANDONED to "放弃").forEach { (s, label) ->
                val active = selectedNode.status == s
                val bg = when (s) {
                    NodeStatus.PLANNED -> Color(0x33FFFFFF); NodeStatus.DONE -> Color(0x3300E676)
                    NodeStatus.IMPROVING -> Color(0x33FFD740); NodeStatus.ABANDONED -> Color(0x33E53935)
                }
                Surface(onClick = { onStatusChange(s) }, shape = RoundedCornerShape(6.dp),
                    color = if (active) bg else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (active) bg else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.height(28.dp)) {
                    Text(label, color = Color.White.copy(alpha = if (active) 1f else 0.4f), fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp).wrapContentHeight(Alignment.CenterVertically))
                }
            }
        }

        if (selectedNode.description.isNotBlank()) {
            Text("描述", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
            Text(selectedNode.description, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 18.sp)
        }

        Text("ID", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
        Text(selectedNode.id, color = Color.White.copy(alpha = 0.25f), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

        if (selectedNode.tags.isNotEmpty()) {
            Text("标签", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                selectedNode.tags.forEach { tag ->
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1E88E5).copy(alpha = 0.2f)) {
                        Text(tag, color = Color(0xFF90CAF9), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }

        val nodeGroups = groups.filter { selectedNode.id in it.nodeIds }
        if (nodeGroups.isNotEmpty()) {
            Text("所属分组", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                nodeGroups.forEach { g ->
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF43A047).copy(alpha = 0.15f)) {
                        Text(g.label, color = Color(0xFF81C784), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(onClick = { onAddChild("新模块") }, Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) { Text("+ 添加子模块", fontSize = 12.sp) }
        Button(onClick = onSnapshot, Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) { Text("+ 记录版本", fontSize = 12.sp) }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDelete, Modifier.fillMaxWidth()) { Text("删除此节点", color = Color(0xFFE53935), fontSize = 12.sp) }
    }
}
