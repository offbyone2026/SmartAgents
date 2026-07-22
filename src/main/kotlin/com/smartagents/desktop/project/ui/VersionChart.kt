package com.smartagents.desktop.project.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartagents.shared.project.*
import java.text.SimpleDateFormat
import java.util.*

/* ============================================================
   Left Panel — Node Details
   ============================================================ */

@Composable
fun NodeDetailPanel(
    selectedNode: ProjectNode?,
    groups: List<NodeGroup>,
    editingNodeId: String?,
    onStatusChange: (NodeStatus) -> Unit,
    onTitleChange: (String) -> Unit,
    onAddChild: (String) -> Unit,
    onDelete: () -> Unit,
    onSnapshot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedNode == null) {
        Box(Modifier.width(260.dp).fillMaxHeight().background(Color(0xFF0E0E18)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("点击节点查看详情", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp)
                Text("右键更多操作", color = Color.White.copy(alpha = 0.2f), fontSize = 11.sp)
            }
        }
        return
    }

    Column(
        modifier = Modifier.width(260.dp).fillMaxHeight().background(Color(0xFF0E0E18)).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Title (with inline edit)
        if (editingNodeId == selectedNode.id) {
            var editText by remember { mutableStateOf(selectedNode.title) }
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E88E5),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                ),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onTitleChange(editText) }),
            )
        } else {
            Text(selectedNode.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // Status
        Text("状态", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val items = listOf(NodeStatus.PLANNED to "计划", NodeStatus.DONE to "完成", NodeStatus.IMPROVING to "待提高", NodeStatus.ABANDONED to "放弃")
            items.forEach { (s, label) ->
                val active = selectedNode.status == s
                val bg = when (s) {
                    NodeStatus.PLANNED -> Color(0x33FFFFFF); NodeStatus.DONE -> Color(0x3300E676)
                    NodeStatus.IMPROVING -> Color(0x33FFD740); NodeStatus.ABANDONED -> Color(0x33E53935)
                }
                Surface(onClick = { onStatusChange(s) }, shape = RoundedCornerShape(6.dp),
                    color = if (active) bg else Color.Transparent,
                    border = BorderStroke(1.dp, if (active) bg else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.height(28.dp)) {
                    Text(label, color = Color.White.copy(alpha = if (active) 1f else 0.4f), fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp).wrapContentHeight(Alignment.CenterVertically))
                }
            }
        }

        // Description
        if (selectedNode.description.isNotBlank()) {
            Text("描述", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
            Text(selectedNode.description, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 18.sp)
        }

        // ID
        Text("ID", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, letterSpacing = 1.sp)
        Text(selectedNode.id, color = Color.White.copy(alpha = 0.25f), fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

        // Tags
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

        // Groups
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

        // Actions
        Button(onClick = { onAddChild("新模块") }, Modifier.fillMaxWidth().height(38.dp),
            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))) {
            Text("+ 添加子模块", fontSize = 12.sp)
        }
        Button(onClick = onSnapshot, Modifier.fillMaxWidth().height(38.dp),
            shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))) {
            Text("+ 记录版本", fontSize = 12.sp)
        }

        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDelete, Modifier.fillMaxWidth()) { Text("删除此节点", color = Color(0xFFE53935), fontSize = 12.sp) }
    }
}

/* ============================================================
   Bottom Legend
   ============================================================ */

@Composable
fun StatusLegend(modifier: Modifier = Modifier) {
    Row(Modifier.fillMaxWidth().height(36.dp).background(Color(0xFF060610)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        mapOf(NodeStatus.PLANNED to "计划中", NodeStatus.DONE to "已完成", NodeStatus.IMPROVING to "待提高", NodeStatus.ABANDONED to "已放弃").forEach { (s, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(when (s) {
                    NodeStatus.PLANNED -> Color(0xFFE8E8F0); NodeStatus.DONE -> Color(0xFF00E676)
                    NodeStatus.IMPROVING -> Color(0xFFFFD740); NodeStatus.ABANDONED -> Color(0xFFE53935)
                }))
                Spacer(Modifier.width(6.dp))
                Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
            }
        }
    }
}

/* ============================================================
   Version Preview Mini-Blueprint
   ============================================================ */

@Composable
fun VersionPreview(
    nodes: Map<String, ProjectNode>,
    modifier: Modifier = Modifier,
) {
    if (nodes.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("无快照数据", color = Color.White.copy(alpha = 0.3f), fontSize = 12.sp)
        }
        return
    }

    // Build a mini layout
    val bp = ProjectBlueprint(projectId = "preview", projectName = "", rootNodeId = nodes.keys.firstOrNull() ?: "", nodes = nodes)
    val miniRoot = remember(nodes) {
        layoutTree(bp)?.let { assignPositions(it, 100f, 60f) }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF08080F))
    ) {
        val root = miniRoot ?: return@Canvas
        // Scale to fit
        val allX = mutableListOf<Float>(); val allY = mutableListOf<Float>()
        fun collect(n: LayoutNode) { allX.add(n.x); allY.add(n.y); n.children.forEach { collect(it) } }
        collect(root)
        if (allX.isEmpty()) return@Canvas
        val maxX = allX.max() + RADIUS + 20f; val maxY = allY.max() + RADIUS + 20f
        val minX = allX.min() - RADIUS - 20f; val minY = allY.min() - RADIUS - 20f
        val w = maxX - minX; val h = maxY - minY
        val s = minOf((size.width - 40f) / w, (size.height - 40f) / h, 1f)
        val dx = (size.width - w * s) / 2f - minX * s; val dy = (size.height - h * s) / 2f - minY * s

        withTransform({ translate(dx, dy); scale(s, s, pivot = Offset.Zero) }) {
            fun drawMini(n: LayoutNode) {
                val c = nodeColors[n.node.status] ?: Color.Gray
                val dc = nodeDarkColors[n.node.status] ?: Color.DarkGray
                draw3DNode(n.x, n.y, c, dc, false, false, 0f, n.node.status)
                n.children.forEach { drawMini(it) }
            }
            fun drawMiniConn(n: LayoutNode) {
                val px = n.x + EXTRUDE_X / 2; val py = n.y + EXTRUDE_Y / 2
                n.children.forEach { child ->
                    val cx = child.x + EXTRUDE_X / 2; val cy = child.y + EXTRUDE_Y / 2
                    drawLine(Color.White.copy(alpha = 0.15f), Offset(px, py), Offset(cx, cy), 0.8f)
                    drawMiniConn(child)
                }
            }
            drawMiniConn(root)
            drawMini(root)
        }
    }
}

/* ============================================================
   Version Chart
   ============================================================ */

@Composable
fun VersionChart(
    versions: List<VersionRecord>,
    selectedVersion: VersionRecord?,
    activeBranch: String,
    branches: Map<String, List<String>>,
    onVersionClicked: (String) -> Unit,
    onRollback: (String) -> Unit,
    onBranchSwitch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0A14))) {
        // Branch tabs
        Row(Modifier.fillMaxWidth().background(Color(0xFF060610)).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            branches.keys.forEach { b ->
                val active = b == activeBranch
                Surface(onClick = { onBranchSwitch(b) }, shape = RoundedCornerShape(6.dp),
                    color = if (active) Color(0xFF1E88E5).copy(alpha = 0.25f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (active) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.height(28.dp)) {
                    Text(b, color = Color.White.copy(alpha = if (active) 1f else 0.45f), fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp).wrapContentHeight(Alignment.CenterVertically))
                }
            }
            Spacer(Modifier.weight(1f))
            Text("${versions.size} 个版本", color = Color.White.copy(alpha = 0.25f), fontSize = 11.sp, modifier = Modifier.wrapContentHeight(Alignment.CenterVertically))
        }

        if (versions.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无版本记录", color = Color.White.copy(alpha = 0.35f), fontSize = 14.sp)
                    Text("在蓝图图中点击 +记录版本 来创建", color = Color.White.copy(alpha = 0.18f), fontSize = 12.sp)
                }
            }
        } else {
            Row(Modifier.fillMaxSize().weight(1f)) {
                // Timeline list
                LazyColumn(
                    modifier = Modifier.weight(0.55f),
                    contentPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 20.dp, bottom = 20.dp),
                ) {
                    itemsIndexed(versions.sortedByDescending { it.timestamp }) { index, version ->
                        VersionTimelineCard(
                            version = version,
                            isSelected = version.version == selectedVersion?.version,
                            isLast = index == versions.size - 1,
                            dateFormat = dateFormat,
                            onClick = { onVersionClicked(version.version) },
                            onRollback = { onRollback(version.version) },
                        )
                    }
                }

                // Preview panel
                Box(Modifier.weight(0.45f).fillMaxHeight().background(Color(0xFF060610))) {
                    if (selectedVersion != null) {
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            Text("版本快照预览", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(selectedVersion.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(selectedVersion.version, color = Color(0xFF90CAF9), fontSize = 13.sp)
                            Spacer(Modifier.height(12.dp))
                            VersionPreview(
                                nodes = selectedVersion.snapshot,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("点击左侧版本查看快照", color = Color.White.copy(alpha = 0.25f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionTimelineCard(
    version: VersionRecord,
    isSelected: Boolean,
    isLast: Boolean,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onRollback: () -> Unit,
) {
    val bgColor by animateColorAsState(if (isSelected) Color(0xFF19192E) else Color(0xFF0F0F1E), label = "bg")
    val borderColor by animateColorAsState(if (isSelected) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.06f), label = "border")

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            Box(Modifier.size(if (isSelected) 12.dp else 8.dp).clip(CircleShape)
                .background(if (isSelected) Color(0xFF1E88E5) else Color.White.copy(alpha = 0.25f))
                .offset(y = 20.dp))
            if (!isLast) Box(Modifier.width(1.5.dp).weight(1f).background(Color.White.copy(alpha = 0.08f)).offset(y = 20.dp))
            else Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.width(12.dp))

        Card(onClick = onClick, Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = bgColor),
            border = BorderStroke(1.dp, borderColor)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF1E88E5).copy(alpha = 0.15f), modifier = Modifier.size(52.dp)) {
                    Column(Modifier.wrapContentSize(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(version.version, color = Color(0xFF90CAF9), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(version.branchName, color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(version.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (version.description.isNotBlank()) { Spacer(Modifier.height(2.dp)); Text(version.description, color = Color.White.copy(alpha = 0.45f), fontSize = 12.sp, maxLines = 2) }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dateFormat.format(Date(version.timestamp)), color = Color.White.copy(alpha = 0.25f), fontSize = 10.sp)
                        if (version.changedNodeIds.isNotEmpty()) { Spacer(Modifier.width(8.dp)); Text("${version.changedNodeIds.size} 个节点变更", color = Color.White.copy(alpha = 0.2f), fontSize = 10.sp) }
                    }
                }
                if (isSelected) {
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onRollback, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                        Text("回滚", fontSize = 11.sp, color = Color(0xFFEF9A9A))
                    }
                }
            }
        }
    }
}
