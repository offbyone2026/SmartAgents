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
            OfficeAgent("Dispatcher", "任务调度中枢", Accent, Color(0xFFEF5350),
                Icons.Default.Psychology, faceIndex = 0, col = 0, row = 0),
            OfficeAgent("File Agent", "文件处理", Orange500, Color(0xFFFFA726),
                Icons.Default.FolderOpen, faceIndex = 1, col = 1, row = 0),
            OfficeAgent("Browser Agent", "浏览器自动化", Blue500, Color(0xFF42A5F5),
                Icons.Default.Language, faceIndex = 2, col = 2, row = 0),
            OfficeAgent("Computer Agent", "系统管理", Green500, Color(0xFF66BB6A),
                Icons.Default.Computer, faceIndex = 3, col = 3, row = 0),
            OfficeAgent("Search Agent", "搜索检索", Purple500, Color(0xFFAB47BC),
                Icons.Default.Search, faceIndex = 4, col = 4, row = 0),
            OfficeAgent("App Agent", "应用操作", Teal500, Color(0xFF26C6DA),
                Icons.Default.Apps, faceIndex = 5, col = 5, row = 0),
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
            Text("Dispatcher 办公室", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPri)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Green500))
                Spacer(Modifier.width(4.dp))
                Text("6 位 Agents 在线", fontSize = 12.sp, color = TextSec)
            }
        }

        Divider(color = BorderLight, thickness = 1.dp)

        // Office floor — single row
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

            // Agent cards in single vertical column
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                agents.forEach { agent ->
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
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("暂无日志", fontSize = 14.sp, color = TextHint)
    }
}

// ============================================================
// C Tab 2: 蓝图
// ============================================================
@Composable
private fun BlueprintTab() {
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("暂无蓝图", fontSize = 14.sp, color = TextHint)
    }
}

// ============================================================
// C Tab 3: 版本记录
// ============================================================
@Composable
private fun VersionHistoryTab() {
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("暂无版本记录", fontSize = 14.sp, color = TextHint)
    }
}
