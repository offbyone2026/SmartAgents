package com.smartagents.desktop.project.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartagents.shared.project.*
import kotlin.math.*

/* ============================================================
   Layout Engine
   ============================================================ */

data class LayoutNode(
    val node: ProjectNode,
    val depth: Int,
    val x: Float,
    val y: Float,
    val children: List<LayoutNode>,
)

fun layoutTree(blueprint: ProjectBlueprint, rootId: String? = null): LayoutNode? {
    val id = rootId ?: blueprint.rootNodeId
    val node = blueprint.nodes[id] ?: return null
    return layoutRecursive(node, blueprint, 0)
}

private fun layoutRecursive(node: ProjectNode, bp: ProjectBlueprint, depth: Int): LayoutNode {
    val children = node.children.mapNotNull { bp.nodes[it]?.let { layoutRecursive(it, bp, depth + 1) } }
    return LayoutNode(node = node, depth = depth, x = 0f, y = 0f, children = children)
}

fun assignPositions(root: LayoutNode, spacingX: Float, spacingY: Float): LayoutNode {
    var yCounter = 0
    data class FlatEntry(val id: String, val depth: Int, val y: Int, val parentId: String?)
    val entries = mutableListOf<FlatEntry>()
    fun walk(n: LayoutNode) {
        if (n.children.isEmpty()) entries.add(FlatEntry(n.node.id, n.depth, yCounter++, n.node.parentId))
        else { n.children.forEach { walk(it) }; entries.add(FlatEntry(n.node.id, n.depth, yCounter++, n.node.parentId)) }
    }
    walk(root)
    val orderMap = entries.withIndex().associate { (idx, entry) -> entry.id to idx }
    fun rebuild(n: LayoutNode): LayoutNode = n.copy(
        x = n.depth * spacingX,
        y = (orderMap[n.node.id] ?: 0).toFloat() * spacingY,
        children = n.children.map { rebuild(it) },
    )
    return rebuild(root)
}

/* ============================================================
   Drawing Constants
   ============================================================ */

internal val nodeColors = mapOf(
    NodeStatus.PLANNED to Color(0xFFE8E8F0),
    NodeStatus.DONE to Color(0xFF00E676),
    NodeStatus.IMPROVING to Color(0xFFFFD740),
    NodeStatus.ABANDONED to Color(0xFFE53935),
)
internal val nodeDarkColors = mapOf(
    NodeStatus.PLANNED to Color(0xFF8A8A9A),
    NodeStatus.DONE to Color(0xFF00883A),
    NodeStatus.IMPROVING to Color(0xFFB89400),
    NodeStatus.ABANDONED to Color(0xFF8B0000),
)
internal val nodeGlowColors = mapOf(
    NodeStatus.PLANNED to Color(0x44FFFFFF),
    NodeStatus.DONE to Color(0x4400E676),
    NodeStatus.IMPROVING to Color(0x44FFD740),
    NodeStatus.ABANDONED to Color(0x44E53935),
)

internal const val RADIUS = 34f
internal const val EXTRUDE_X = -6f
internal const val EXTRUDE_Y = -10f

/* ============================================================
   Geometry
   ============================================================ */

private fun hexVertices(cx: Float, cy: Float, r: Float): List<Offset> =
    (0 until 6).map { i -> Offset(cx + r * cos((PI / 6.0) * (2 * i - 1)).toFloat(), cy + r * sin((PI / 6.0) * (2 * i - 1)).toFloat()) }

fun DrawScope.draw3DNode(
    cx: Float, cy: Float, color: Color, darkColor: Color,
    isSelected: Boolean, isSearchHit: Boolean, glowProgress: Float, status: NodeStatus,
) {
    val topVerts = hexVertices(cx + EXTRUDE_X, cy + EXTRUDE_Y, RADIUS)
    val botVerts = hexVertices(cx, cy, RADIUS)

    // Selected glow
    if (isSelected) {
        val alpha = 0.25f + 0.15f * sin(glowProgress * 2 * PI.toFloat())
        drawCircle(nodeGlowColors[status]!!.copy(alpha = alpha), RADIUS + 14f + 6f * sin(glowProgress * 2 * PI.toFloat()),
            Offset(cx + EXTRUDE_X / 2, cy + EXTRUDE_Y / 2))
    }
    // Search hit ring
    if (isSearchHit) {
        drawCircle(Color(0xFF42A5F5).copy(alpha = 0.5f + 0.2f * sin(glowProgress * 3 * PI.toFloat())), RADIUS + 8f,
            Offset(cx + EXTRUDE_X / 2, cy + EXTRUDE_Y / 2), style = Stroke(width = 2f))
    }

    // Side faces
    for (i in 0 until 6) {
        val j = (i + 1) % 6; val b1 = botVerts[i]; val b2 = botVerts[j]; val t1 = topVerts[i]; val t2 = topVerts[j]
        val midX = (b1.x + b2.x + t1.x + t2.x) / 4f; val midY = (b1.y + b2.y + t1.y + t2.y) / 4f
        val lf = ((midX - cx) / RADIUS * 0.3f + (midY - cy) / RADIUS * 0.3f).coerceIn(-0.3f, 0.3f)
        drawPath(Path().apply { moveTo(b1.x, b1.y); lineTo(b2.x, b2.y); lineTo(t2.x, t2.y); lineTo(t1.x, t1.y); close() },
            darkColor.copy(red = (darkColor.red + lf).coerceIn(0f, 1f), green = (darkColor.green + lf).coerceIn(0f, 1f), blue = (darkColor.blue + lf).coerceIn(0f, 1f)))
        drawLine(Color.White.copy(alpha = 0.15f), b1, b2, 0.8f)
    }

    // Top face gradient
    drawPath(Path().apply { topVerts.forEachIndexed { i, v -> if (i == 0) moveTo(v.x, v.y) else lineTo(v.x, v.y) }; close() },
        Brush.linearGradient(listOf(color, color.copy(red = (color.red + 0.15f).coerceIn(0f, 1f), green = (color.green + 0.15f).coerceIn(0f, 1f), blue = (color.blue + 0.15f).coerceIn(0f, 1f)), color),
            Offset(cx + EXTRUDE_X - RADIUS * 0.5f, cy + EXTRUDE_Y - RADIUS * 0.5f), Offset(cx + EXTRUDE_X + RADIUS * 0.5f, cy + EXTRUDE_Y + RADIUS * 0.5f)))

    // Top edges
    for (i in 0 until 6) { val j = (i + 1) % 6; val v1 = topVerts[i]; val v2 = topVerts[j]
        val mid = Offset((v1.x + v2.x) / 2, (v1.y + v2.y) / 2); val lit = (mid.x - cx) < 0 && (mid.y - cy) < 0
        drawLine(if (lit) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f), v1, v2, if (lit) 1.8f else 1f) }
    for (i in 0 until 6) drawLine(Color.White.copy(alpha = 0.08f), botVerts[i], topVerts[i], 0.5f)
}

fun DrawScope.drawNodeLabel(cx: Float, cy: Float, icon: NodeIcon, text: String, status: NodeStatus, measurer: TextMeasurer) {
    val lx = cx + EXTRUDE_X; val ly = cy + EXTRUDE_Y
    val size = 32f // icon area on top half
    val iconStr = icon.emoji
    val hasIcon = iconStr.isNotEmpty()

    if (hasIcon) {
        // Draw icon emoji centered in upper half
        val iconStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center)
        val iconMeasured = measurer.measure(AnnotatedString(iconStr), iconStyle)
        drawText(iconMeasured, topLeft = Offset(lx - iconMeasured.size.width / 2f, ly - 16f - iconMeasured.size.height / 2f))
    }

    // Title below icon
    val textColor = if (status == NodeStatus.PLANNED) Color(0xFF1A1A2E) else Color.White
    val textStyle = TextStyle(color = textColor, fontSize = if (hasIcon) 9.sp else 10.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    val measured = measurer.measure(AnnotatedString(text), textStyle, maxLines = 2)
    val textY = if (hasIcon) ly else ly - measured.size.height / 2f
    drawText(measured, topLeft = Offset(lx - measured.size.width / 2f, textY))
}

fun DrawScope.drawConnections(root: LayoutNode) {
    fun draw(n: LayoutNode) {
        val px = n.x + EXTRUDE_X / 2; val py = n.y + EXTRUDE_Y / 2
        n.children.forEach { c ->
            val cx = c.x + EXTRUDE_X / 2; val cy = c.y + EXTRUDE_Y / 2
            val dx = cx - px; val dy = cy - py; val dist = sqrt(dx * dx + dy * dy)
            val sx = px + dx / dist * (RADIUS + 4f); val sy = py + dy / dist * (RADIUS + 4f)
            val ex = cx - dx / dist * (RADIUS + 4f); val ey = cy - dy / dist * (RADIUS + 4f)
            drawLine(Color.White.copy(alpha = 0.25f), Offset(sx, sy), Offset(ex, ey), 1.2f)
            val al = 7f; val a = atan2(dy.toDouble(), dx.toDouble())
            drawLine(Color.White.copy(alpha = 0.25f), Offset(ex, ey), Offset(ex - al * cos(a + PI * 0.82).toFloat(), ey - al * sin(a + PI * 0.82).toFloat()), 1.2f)
            drawLine(Color.White.copy(alpha = 0.25f), Offset(ex, ey), Offset(ex - al * cos(a - PI * 0.82).toFloat(), ey - al * sin(a - PI * 0.82).toFloat()), 1.2f)
            draw(c)
        }
    }
    draw(root)
}

fun DrawScope.drawGroupBorders(groups: List<NodeGroup>, layoutRoot: LayoutNode) {
    val nodeMap = mutableMapOf<String, Offset>()
    fun collect(n: LayoutNode) { nodeMap[n.node.id] = Offset(n.x + EXTRUDE_X / 2, n.y + EXTRUDE_Y / 2); n.children.forEach { collect(it) } }
    collect(layoutRoot)
    groups.forEach { group ->
        val points = group.nodeIds.mapNotNull { nodeMap[it] }
        if (points.size >= 2) {
            val minX = points.minOf { it.x } - RADIUS - 10f; val minY = points.minOf { it.y } - RADIUS - 10f
            val maxX = points.maxOf { it.x } + RADIUS + 10f; val maxY = points.maxOf { it.y } + RADIUS + 10f
            val r = 12f
            val border = Path().apply {
                moveTo(minX + r, minY); lineTo(maxX - r, minY); arcTo(Rect(maxX - r * 2, minY, maxX, minY + r * 2), -90f, 90f, false)
                lineTo(maxX, maxY - r); arcTo(Rect(maxX - r * 2, maxY - r * 2, maxX, maxY), 0f, 90f, false)
                lineTo(minX + r, maxY); arcTo(Rect(minX, maxY - r * 2, minX + r * 2, maxY), 90f, 90f, false)
                lineTo(minX, minY + r); arcTo(Rect(minX, minY, minX + r * 2, minY + r * 2), 180f, 90f, false); close()
            }
            drawPath(border, Color(0xFF1E88E5).copy(alpha = 0.25f), style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
        }
    }
}

fun hitTestNode(root: LayoutNode, tapX: Float, tapY: Float, tx: Float, ty: Float, s: Float): String? {
    fun test(n: LayoutNode): String? {
        val cx = (n.x + EXTRUDE_X / 2) * s + tx; val cy = (n.y + EXTRUDE_Y / 2) * s + ty; val r = RADIUS * s
        if ((tapX - cx).let { it * it } + (tapY - cy).let { it * it } <= r * r) return n.node.id
        return n.children.firstNotNullOfOrNull { test(it) }
    }
    return test(root)
}

/* ============================================================
   Main Chart Composable
   ============================================================ */

@Composable
fun BlueprintChart(
    blueprint: ProjectBlueprint,
    selectedNodeId: String?,
    groups: List<NodeGroup>,
    searchResults: Set<String>,
    editingNodeId: String?,
    onNodeClicked: (String) -> Unit,
    onNodeDoubleClicked: (String) -> Unit,
    onNodeRightClicked: (String, Offset) -> Unit,
    onNodeDragReorder: (parentId: String, fromIndex: Int, toIndex: Int) -> Unit,
    onEditCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    var scale by remember { mutableStateOf(0.85f) }
    var offsetX by remember { mutableStateOf(80f) }
    var offsetY by remember { mutableStateOf(120f) }

    val animOffsetX by animateFloatAsState(offsetX, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "ox")
    val animOffsetY by animateFloatAsState(offsetY, animationSpec = tween(400, easing = FastOutSlowInEasing), label = "oy")

    val glow = rememberInfiniteTransition(label = "glow")
    val gp by glow.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "gp")

    val layoutRoot = remember(blueprint) {
        layoutTree(blueprint)?.let { assignPositions(it, 170f, 110f) }
    }

    // Context menu
    var contextMenuNode by remember { mutableStateOf<String?>(null) }
    var contextMenuPos by remember { mutableStateOf(Offset.Zero) }
    var contextMenuVisible by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF08080F))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(layoutRoot) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.25f, 2.5f); offsetX += pan.x; offsetY += pan.y
                    }
                }
                .pointerInput(layoutRoot) {
                    detectTapGestures(
                        onTap = { tap -> contextMenuVisible = false; layoutRoot?.let { hitTestNode(it, tap.x, tap.y, animOffsetX, animOffsetY, scale)?.let(onNodeClicked) } },
                        onDoubleTap = { tap -> contextMenuVisible = false; layoutRoot?.let { hitTestNode(it, tap.x, tap.y, animOffsetX, animOffsetY, scale)?.let(onNodeDoubleClicked) } },
                    )
                }
                .pointerInput(layoutRoot) {
                    detectTapGestures(
                        onLongPress = { offset -> // use long-press as right-click proxy
                            layoutRoot?.let { hitTestNode(it, offset.x, offset.y, animOffsetX, animOffsetY, scale)?.let { nodeId ->
                                contextMenuNode = nodeId; contextMenuPos = offset; contextMenuVisible = true; onNodeRightClicked(nodeId, offset)
                            } }
                        }
                    )
                }
        ) {
            val root = layoutRoot ?: return@Canvas
            withTransform({ translate(animOffsetX, animOffsetY); scale(scale, scale, pivot = Offset.Zero) }) {
                drawGroupBorders(groups, root)
                drawConnections(root)
                fun drawAll(n: LayoutNode) {
                    val isHit = n.node.id in searchResults
                    draw3DNode(n.x, n.y, nodeColors[n.node.status]!!, nodeDarkColors[n.node.status]!!,
                        n.node.id == selectedNodeId, isHit, gp, n.node.status)
                    drawNodeLabel(n.x, n.y, n.node.icon, n.node.title, n.node.status, textMeasurer)
                    n.children.forEach { drawAll(it) }
                }
                drawAll(root)
            }
        }

        // Group labels
        layoutRoot?.let { root ->
            groups.forEach { group ->
                val points = group.nodeIds.mapNotNull { id ->
                    var found: LayoutNode? = null
                    fun find(n: LayoutNode) { if (n.node.id == id) found = n else n.children.forEach { find(it) } }
                    find(root); found
                }.map { Offset(it.x + EXTRUDE_X / 2, it.y + EXTRUDE_Y / 2) }
                if (points.size >= 2) {
                    val minX = points.minOf { it.x } * scale + animOffsetX
                    val minY = points.minOf { it.y } * scale + animOffsetY
                    Box(modifier = Modifier.offset { IntOffset((minX - 10f * scale).dp.roundToPx(), (minY - 24f * scale - RADIUS * scale).dp.roundToPx()) }
                        .background(Color(0xFF1E88E5).copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(group.label, color = Color(0xFF90CAF9), fontSize = 9.sp)
                    }
                }
            }
        }

        // Context menu
        if (contextMenuVisible && contextMenuNode != null) {
            Box(modifier = Modifier.offset { IntOffset(contextMenuPos.x.dp.roundToPx(), contextMenuPos.y.dp.roundToPx()) }) {
                Column(
                    modifier = Modifier.background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).width(140.dp).padding(4.dp)
                ) {
                    contextMenuItem("\u270F\uFE0F 重命名") { onNodeDoubleClicked(contextMenuNode!!); contextMenuVisible = false }
                    contextMenuItem("\uD83D\uDCCB 计划中") { contextMenuVisible = false }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    contextMenuItem("\u2795 添加子模块") { contextMenuVisible = false }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    contextMenuItem("\uD83D\uDDD1 删除", isDanger = true) { contextMenuVisible = false }
                }
            }
        }
    }
}

@Composable
private fun contextMenuItem(label: String, isDanger: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth().height(32.dp), shape = RoundedCornerShape(4.dp)) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
            Text(label, color = if (isDanger) Color(0xFFE53935) else Color.White.copy(alpha = 0.8f), fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp))
        }
    }
}
