package com.smartagents.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartagents.auth.AuthState

// ============================================================
// Design Tokens
// ============================================================
private val Accent = Color(0xFFE53935)
private val AccentLight = Color(0xFFFFEBEE)
private val BgWhite = Color(0xFFFFFFFF)
private val BgSidebar = Color(0xFFF5F5F5)
private val BgCard = Color(0xFFFAFAFA)
private val BgCenter = Color(0xFFF2F3F5)
private val BorderLight = Color(0xFFE0E0E0)
private val BorderInput = Color(0xFFD0D0D0)
private val TextPri = Color(0xFF1A1A1A)
private val TextSec = Color(0xFF5F6368)
private val TextHint = Color(0xFF80868B)
private val DividerColor = Color(0xFFEBEBEB)
private val RedCut = Color(0xFFE53935)
private val Blue500 = Color(0xFF1E88E5)
private val Green500 = Color(0xFF43A047)
private val Orange500 = Color(0xFFFB8C00)
private val Purple500 = Color(0xFF8E24AA)
private val Teal500 = Color(0xFF00ACC1)

// ============================================================
// Agent Desk Model
// ============================================================
data class OfficeAgent(
    val name: String,
    val role: String,
    val color: Color,
    val dotColor: Color,
    val icon: ImageVector,
    val faceIndex: Int,    // which face to draw
    val col: Int,          // grid column 0-4
    val row: Int,          // grid row 0-1
)

// ============================================================
// OFFICE SCREEN
// ============================================================
@Composable
fun OfficeScreen(auth: AuthState, onBack: () -> Unit, onLogout: () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        background = BgWhite,
        surface = BgWhite,
        onSurface = TextPri,
    )) {
        Row(Modifier.fillMaxSize().background(BgWhite)) {
            // A栏 — Sidebar
            OfficeSidebar(auth, onBack, onLogout)

            // Red cut line A | B
            Box(Modifier.width(3.dp).fillMaxHeight().background(RedCut))

            // B栏 — Office Floor
            OfficeCenter(Modifier.weight(1f))

            // Red cut line B | C
            Box(Modifier.width(3.dp).fillMaxHeight().background(RedCut))

            // C栏 — Right Panel (tabs)
            OfficeRightPanel()
        }
    }
}

// ============================================================
// A栏 — SIDEBAR (3 sections, red dividers)
// ============================================================
@Composable
private fun OfficeSidebar(auth: AuthState, onBack: () -> Unit, onLogout: () -> Unit) {
    var kbExpanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.width(248.dp).fillMaxHeight().background(BgSidebar)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // === SECTION 1: Brand + Search + Nav ===
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text("SmartAgents", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPri)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Home, null, tint = TextHint, modifier = Modifier.size(18.dp).clickable { onBack() })
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = searchText, onValueChange = { searchText = it },
            placeholder = { Text("搜索工具和技能", color = TextHint, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth().height(36.dp),
            singleLine = true, shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorderLight, unfocusedBorderColor = BorderLight,
                focusedContainerColor = BgWhite, unfocusedContainerColor = BgWhite,
            ),
            textStyle = TextStyle(fontSize = 13.sp),
        )
        Spacer(Modifier.height(10.dp))

        SideNavItem(Icons.Default.Add, "新建对话")
        SideNavItem(Icons.Default.SmartToy, "自动任务")
        SideNavItem(Icons.Default.Store, "技能广场")

        // --- Red divider 1 ---
        Spacer(Modifier.height(6.dp))
        Divider(color = RedCut, thickness = 2.dp)
        Spacer(Modifier.height(6.dp))

        // === SECTION 2: Knowledge Base ===
        Row(
            Modifier.fillMaxWidth().clickable { kbExpanded = !kbExpanded }.padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("本地知识库", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSec)
            Spacer(Modifier.weight(1f))
            Icon(
                if (kbExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                null, tint = TextHint, modifier = Modifier.size(16.dp)
            )
        }
        if (kbExpanded) {
            SideKbRow(Icons.Default.Apps, "应用")
            SideKbRow(Icons.Default.Description, "文档")
            SideKbRow(Icons.Default.PhotoLibrary, "图库")
            SideKbRow(Icons.Default.Computer, "此电脑")
        }

        // --- Red divider 2 ---
        Spacer(Modifier.height(6.dp))
        Divider(color = RedCut, thickness = 2.dp)
        Spacer(Modifier.height(6.dp))

        // === SECTION 3: Chat & History ===
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("对话", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSec)
            Spacer(Modifier.height(5.dp))

            // Office entry — active
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE3F2FD))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(Blue500))
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Business, null, tint = Blue500, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("办公室", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Blue500)
            }
        }

        // Bottom user
        Divider(color = DividerColor, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarFace(auth.username.hashCode(), 30, Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(auth.username, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPri)
                Text("在线", fontSize = 11.sp, color = TextHint)
            }
            Icon(Icons.Default.NotificationsNone, null, tint = TextHint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Logout, null, tint = TextHint, modifier = Modifier.size(16.dp).clickable { onLogout() })
        }
    }
}

@Composable
private fun SideNavItem(icon: ImageVector, label: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .padding(vertical = 5.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSec, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = TextPri)
    }
}

@Composable
private fun SideKbRow(icon: ImageVector, label: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextHint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, color = TextPri)
    }
}

// ============================================================
// B栏 — OFFICE CENTER
// ============================================================
@Composable
private fun OfficeCenter(modifier: Modifier = Modifier) {
    val agents = remember {
        listOf(
            // Row 0: Browser, Computer
            OfficeAgent("Browser Agent", "浏览器自动化", Blue500, Color(0xFF42A5F5),
                Icons.Default.Language, faceIndex = 0, col = 0, row = 0),
            OfficeAgent("Computer Agent", "系统管理", Green500, Color(0xFF66BB6A),
                Icons.Default.Computer, faceIndex = 1, col = 4, row = 0),
            // Row 1: File, Marvis, Search
            OfficeAgent("File Agent", "文件处理", Orange500, Color(0xFFFFA726),
                Icons.Default.FolderOpen, faceIndex = 2, col = 0, row = 1),
            OfficeAgent("Marvis", "主控 Agent · 调度中枢", Accent, Color(0xFFEF5350),
                Icons.Default.Psychology, faceIndex = 3, col = 2, row = 1),
            OfficeAgent("Search Agent", "搜索检索", Purple500, Color(0xFFAB47BC),
                Icons.Default.Search, faceIndex = 4, col = 4, row = 1),
            // Row 2: App
            OfficeAgent("App Agent", "应用操作", Teal500, Color(0xFF26C6DA),
                Icons.Default.Apps, faceIndex = 5, col = 2, row = 2),
        )
    }

    Column(modifier = modifier.fillMaxHeight().background(BgCenter)) {
        // Title bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Business, null, tint = RedCut, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text("SmartAgents 办公室", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPri)
            Spacer(Modifier.weight(1f))
            // Status summary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Green500))
                Spacer(Modifier.width(4.dp))
                Text("6 位 Agents 在线", fontSize = 12.sp, color = TextSec)
            }
        }

        Divider(color = BorderLight, thickness = 1.dp)

        // Office floor grid
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF8F9FB))
                .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
        ) {
            // Floor grid canvas
            Canvas(Modifier.fillMaxSize()) {
                val tileW = 60f
                val tileH = 60f
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        drawRect(
                            color = Color(0xFFEEF0F3),
                            topLeft = Offset(x, y),
                            size = Size(tileW - 2f, tileH - 2f)
                        )
                        y += tileH
                    }
                    x += tileW
                }
            }

            // Column labels at top
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("A区", "B区", "C区", "D区", "E区").forEach {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8EAED)) {
                        Text(it, fontSize = 10.sp, color = TextHint, fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            // Agent desk cards — positioned in a neat 3-row × 5-col grid
            agents.forEach { agent ->
                // Calculate absolute position: col 0..4 maps to horizontal position, row 0..2 maps to vertical
                val xDp = (agent.col * 140 + 20).dp
                val yDp = (agent.row * 200 + 50).dp

                Box(modifier = Modifier.padding(start = xDp, top = yDp)) {
                    AgentDeskCard(agent)
                }
            }
        }
    }
}

@Composable
private fun AgentDeskCard(agent: OfficeAgent) {
    Card(
        modifier = Modifier.width(132.dp).height(164.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(0.5.dp, BorderLight),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status dot + name
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(agent.dotColor))
                Spacer(Modifier.width(6.dp))
                Text(agent.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPri,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // Personalized avatar
            AvatarFace(agent.faceIndex, 48, Modifier.size(48.dp))

            // Role
            Text(agent.role, fontSize = 10.sp, color = TextHint, maxLines = 2,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 13.sp)

            // Icon + status button
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(agent.color.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(agent.icon, null, tint = agent.color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("工作日志", fontSize = 11.sp, color = agent.color, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ============================================================
// Avatar Face — unique per agent, drawn via Canvas
// ============================================================
@Composable
private fun AvatarFace(seed: Int, @Suppress("UNUSED_PARAMETER") sizePx: Int, modifier: Modifier = Modifier) {
    // Deterministic colors from seed
    val bgColors = listOf(
        Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFFFF3E0),
        Color(0xFFFCE4EC), Color(0xFFF3E5F5), Color(0xFFE0F7FA),
        Color(0xFFFFEBEE), Color(0xFFE8EAF6)
    )
    val fgColors = listOf(
        Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFE65100),
        Color(0xFFC62828), Color(0xFF6A1B9A), Color(0xFF00838F),
        Color(0xFFD32F2F), Color(0xFF283593)
    )
    val idx = (seed and Int.MAX_VALUE) % bgColors.size
    val bg = bgColors[idx]
    val fg = fgColors[idx]
    val accent = fg.copy(alpha = 0.6f)

    Canvas(modifier = modifier.clip(CircleShape).background(bg)) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val r = w * 0.35f

        // Head
        drawCircle(color = fg, radius = r, center = Offset(cx, cy))

        // Eyes — two white circles
        val eyeY = cy - r * 0.1f
        val eyeSpacing = r * 0.45f
        drawCircle(color = Color.White, radius = r * 0.32f, center = Offset(cx - eyeSpacing, eyeY))
        drawCircle(color = Color.White, radius = r * 0.32f, center = Offset(cx + eyeSpacing, eyeY))

        // Pupils
        val pupilR = r * 0.16f
        val pupilY = eyeY + r * 0.05f
        drawCircle(color = fg.copy(alpha = 0.9f), radius = pupilR, center = Offset(cx - eyeSpacing, pupilY))
        drawCircle(color = fg.copy(alpha = 0.9f), radius = pupilR, center = Offset(cx + eyeSpacing, pupilY))

        // Mouth — slight smile
        val mouthY = cy + r * 0.25f
        drawArc(
            color = accent,
            startAngle = 0f, sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - r * 0.2f, mouthY - r * 0.15f),
            size = Size(r * 0.4f, r * 0.3f),
            style = Stroke(width = w * 0.04f, cap = StrokeCap.Round)
        )

        // Blush dots
        val blushY = cy + r * 0.05f
        val blushX = r * 0.7f
        drawCircle(color = accent.copy(alpha = 0.25f), radius = r * 0.12f, center = Offset(cx - blushX, blushY))
        drawCircle(color = accent.copy(alpha = 0.25f), radius = r * 0.12f, center = Offset(cx + blushX, blushY))
    }
}

// ============================================================
// C栏 — RIGHT PANEL (工作日志 | 蓝图 | 版本记录)
// ============================================================
@Composable
private fun OfficeRightPanel() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("工作日志", "蓝图", "版本记录")

    Column(
        modifier = Modifier.width(300.dp).fillMaxHeight().background(BgSidebar)
    ) {
        // Tab bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { i, t ->
                val isSelected = i == selectedTab
                Box(
                    Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Blue500 else Color.Transparent)
                        .clickable { selectedTab = i },
                    contentAlignment = Alignment.Center
                ) {
                    Text(t, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextSec)
                }
            }
        }

        Divider(color = BorderLight, thickness = 1.dp)

        // Tab content
        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> WorkLogTab()
                1 -> BlueprintTab()
                2 -> VersionHistoryTab()
            }
        }

        // Bottom input
        Box(
            Modifier.fillMaxWidth().height(44.dp)
                .border(1.dp, BorderInput, RoundedCornerShape(10.dp))
                .background(BgWhite).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("输入消息…", fontSize = 13.sp, color = TextHint)
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ============================================================
// C Tab 1: 工作日志
// ============================================================
@Composable
private fun WorkLogTab() {
    data class LogEntry(
        val agent: String,
        val action: String,
        val time: String,
        val color: Color,
    )

    val logs = listOf(
        LogEntry("Marvis", "调度 File Agent 执行文件整理任务", "14:32:08", Accent),
        LogEntry("File Agent", "开始整理 D:\\Work 目录下的文档", "14:32:11", Orange500),
        LogEntry("File Agent", "已完成 48 个文件的分类归档", "14:32:45", Orange500),
        LogEntry("Marvis", "接收新任务：分析用户代码仓库结构", "14:33:02", Accent),
        LogEntry("Browser Agent", "正在搜索相关技术文档…", "14:33:05", Blue500),
        LogEntry("Search Agent", "检索到 12 条相关技术资料", "14:33:28", Purple500),
        LogEntry("Computer Agent", "系统资源检查完成，内存占用 62%", "14:33:50", Green500),
        LogEntry("App Agent", "已启动 Android 模拟器环境", "14:34:10", Teal500),
        LogEntry("Marvis", "所有 Agent 运行正常，等待下一条指令", "14:34:22", Accent),
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("实时日志", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
            Text("9 条记录", fontSize = 11.sp, color = TextHint)
        }

        logs.forEach { log ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgWhite)
                    .border(0.5.dp, BorderLight, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(log.color).padding(top = 3.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(log.agent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = log.color)
                    Text(log.action, fontSize = 12.sp, color = TextPri, lineHeight = 16.sp)
                }
                Text(log.time, fontSize = 10.sp, color = TextHint)
            }
        }
    }
}

// ============================================================
// C Tab 2: 蓝图
// ============================================================
@Composable
private fun BlueprintTab() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("系统架构图", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPri)

        // Architecture diagram — drawn with Canvas
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(10.dp))
                .background(BgWhite).border(0.5.dp, BorderLight, RoundedCornerShape(10.dp))
        ) {
            Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val w = size.width
                val h = size.height

                // Top: Marvis (main)
                val topBox = androidx.compose.ui.geometry.Rect(w * 0.3f, 10f, w * 0.7f, 55f)
                drawRoundRect(Color(0xFFFFEBEE), topLeft = topBox.topLeft, size = topBox.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
                drawRoundRect(Accent, topLeft = topBox.topLeft, size = topBox.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f), style = Stroke(1.5f))

                // Bottom row agents
                val colors = listOf(Orange500, Blue500, Green500, Purple500, Teal500)
                colors.forEachIndexed { i, color ->
                    val box = androidx.compose.ui.geometry.Rect(
                        i * w / 5f + 4f, h * 0.45f,
                        (i + 1) * w / 5f - 4f, h * 0.45f + 50f
                    )
                    drawRoundRect(color.copy(alpha = 0.08f), topLeft = box.topLeft, size = box.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                    drawRoundRect(color, topLeft = box.topLeft, size = box.size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f), style = Stroke(1f))
                }

                // Lines from Marvis to each agent
                repeat(5) { i ->
                    val startX = w * 0.5f
                    val startY = 55f
                    val endX = i * w / 5f + w / 10f
                    val endY = h * 0.45f
                    drawLine(Color(0xFFBDBDBD), Offset(startX, startY), Offset(endX, endY), strokeWidth = 1.2f)
                }
            }

            // Labels overlaid on canvas positions
            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                Box(Modifier.fillMaxWidth().weight(0.25f), contentAlignment = Alignment.Center) {
                    Text("Marvis 主控", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Accent)
                }
                Row(Modifier.fillMaxWidth().weight(0.75f).padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("File", "Browser", "Computer", "Search", "App").forEachIndexed { _, name ->
                        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TextSec)
                    }
                }
            }
        }

        // Legend
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = BgWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(0.5.dp, BorderLight),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("协作流程", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "1. Marvis 接收用户指令，分析并拆解子任务" to Accent,
                    "2. 根据任务类型路由到对应专业 Agent" to TextPri,
                    "3. Agent 执行完成后汇报结果给 Marvis" to TextPri,
                    "4. Marvis 汇总并交付最终结果给用户" to TextPri,
                ).forEach { (text, color) ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Box(Modifier.size(5.dp).clip(CircleShape).background(color).padding(top = 5.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text, fontSize = 11.sp, color = TextSec, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

// ============================================================
// C Tab 3: 版本记录
// ============================================================
@Composable
private fun VersionHistoryTab() {
    data class Version(
        val hash: String,
        val message: String,
        val date: String,
        val tag: String?,
        val tagColor: Color?,
    )

    val versions = listOf(
        Version("323f0c7", "feat: add OfficeScreen with three-column agent workspace layout", "07-22 20:58", "HEAD", Blue500),
        Version("360de79", "refactor: rebuild HomeScreen UI with cohesive design system", "07-22 14:40", null, null),
        Version("7821f00", "Initial commit", "07-22 12:10", "init", Green500),
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Git 版本记录", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
            Text("3 commits", fontSize = 11.sp, color = TextHint)
        }

        Spacer(Modifier.height(8.dp))

        versions.forEachIndexed { idx, v ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgWhite)
                    .border(0.5.dp, BorderLight, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Timeline dot + line
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp)) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(
                        if (idx == 0) Blue500 else BorderLight))
                    if (idx < versions.size - 1) {
                        Box(Modifier.width(1.5.dp).height(30.dp).background(BorderLight))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(v.hash, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        if (v.tag != null) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier.clip(RoundedCornerShape(3.dp))
                                    .background(v.tagColor!!.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(v.tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = v.tagColor)
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(v.message, fontSize = 11.sp, color = TextSec, lineHeight = 15.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(v.date, fontSize = 10.sp, color = TextHint)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text("每次修改都会自动提交到 GitHub，防止代码丢失。",
            fontSize = 11.sp, color = TextHint, lineHeight = 16.sp)
    }
}
