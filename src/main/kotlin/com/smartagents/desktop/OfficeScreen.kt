package com.smartagents.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartagents.auth.AuthState

// ============================================================
// Design Tokens — inherited from HomeScreen + extensions
// ============================================================
private val Accent = Color(0xFFE53935)
private val AccentLight = Color(0xFFFFEBEE)
private val BgWhite = Color(0xFFFFFFFF)
private val BgSidebar = Color(0xFFF5F5F5)
private val BgCard = Color(0xFFFAFAFA)
private val BgCenter = Color(0xFFEEF1F5)
private val BorderLight = Color(0xFFE0E0E0)
private val BorderInput = Color(0xFFD0D0D0)
private val TextPri = Color(0xFF1A1A1A)
private val TextSec = Color(0xFF5F6368)
private val TextHint = Color(0xFF80868B)
private val DividerColor = Color(0xFFEBEBEB)
private val Blue500 = Color(0xFF1E88E5)
private val Green500 = Color(0xFF43A047)
private val Orange500 = Color(0xFFFB8C00)
private val Purple500 = Color(0xFF8E24AA)
private val Teal500 = Color(0xFF00ACC1)

private val StatusDone = Color(0xFF52C41A)
private val StatusRunning = Color(0xFF1890FF)
private val StatusIdle = Color(0xFF8C8C8C)

// Agent desk data
data class AgentDesk(
    val name: String,
    val role: String,
    val icon: ImageVector,
    val color: Color,
    val task: String,
    val progress: Float,          // 0..1
    val consumed: String,         // "15.3万"
    val total: String,            // "1000万"
    val isActive: Boolean,
    val statusText: String,       // "执行中" / "空闲" / "已完成"
    val row: Int,
    val col: Int,
)

data class OfficeMessage(
    val title: String,
    val tokenConsumed: String,
    val time: String,
    val status: String,           // "已完成" / "进行中"
    val statusColor: Color,
)

// ============================================================
// OFFICE SCREEN — main entry
// ============================================================
@Composable
fun OfficeScreen(auth: AuthState, onBack: () -> Unit, onLogout: () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        background = BgWhite,
        surface = BgWhite,
        onSurface = TextPri,
    )) {
        Row(Modifier.fillMaxSize().background(BgWhite)) {
            // A栏 — Left Sidebar (office-selected variant)
            OfficeSidebar(auth, onBack, onLogout)

            // Divider A|B
            Box(Modifier.width(1.dp).fillMaxHeight().background(DividerColor))

            // B栏 — Office Floor Scene
            OfficeCenter(Modifier.weight(1f))

            // Divider B|C
            Box(Modifier.width(1.dp).fillMaxHeight().background(DividerColor))

            // C栏 — Token Stats + Message List
            OfficeRightPanel()
        }
    }
}

// ============================================================
// A栏 — OFFICE SIDEBAR
// ============================================================
@Composable
private fun OfficeSidebar(auth: AuthState, onBack: () -> Unit, onLogout: () -> Unit) {
    var kbExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.width(260.dp).fillMaxHeight().background(BgSidebar)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // Brand + Back
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text("SmartAgents", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPri)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Home, "Home", tint = TextHint, modifier = Modifier.size(18.dp).clickable { onBack() })
        }

        Spacer(Modifier.height(12.dp))

        // Nav
        NavItem(Icons.Default.Add, "新建对话")
        NavItem(Icons.Default.SmartToy, "自动任务")
        NavItem(Icons.Default.Store, "技能广场")

        Spacer(Modifier.height(12.dp))
        Divider(color = DividerColor, thickness = 1.dp)
        Spacer(Modifier.height(10.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Knowledge Base
            Row(
                Modifier.fillMaxWidth().clickable { kbExpanded = !kbExpanded }.padding(vertical = 6.dp),
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
                KbRow(Icons.Default.Apps, "应用")
                KbRow(Icons.Default.Description, "文档")
                KbRow(Icons.Default.PhotoLibrary, "图库")
                KbRow(Icons.Default.Computer, "此电脑")
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = DividerColor, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // Chat section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("对话", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSec)
            }
            Spacer(Modifier.height(6.dp))

            // Office — selected
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

            Spacer(Modifier.height(8.dp))

            // Chat History — mock items
            Text("对话历史", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSec)
            Spacer(Modifier.height(6.dp))

            val history = listOf(
                "我想做一个AI软件，手机端，安卓端…",
                "在本次对话中，你所…",
                "把水印的天气或者海…",
                "微信ClawBot消息",
                "帮我清理C盘，先看看…",
                "整理D:\\桌面",
                "我们要送我一个网站…"
            )
            history.forEach { h ->
                Text(h, fontSize = 12.sp, color = TextSec, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp).padding(start = 4.dp))
            }
        }

        // Bottom user
        Divider(color = DividerColor, thickness = 1.dp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(AccentLight), contentAlignment = Alignment.Center) {
                Text(auth.username.take(1).uppercase(), color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(auth.username, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPri)
                Text("在线", fontSize = 11.sp, color = TextHint)
            }
            Icon(Icons.Default.NotificationsNone, null, tint = TextHint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.Logout, "退出", tint = TextHint, modifier = Modifier.size(16.dp).clickable { onLogout() })
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String) {
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
private fun KbRow(icon: ImageVector, label: String) {
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
// B栏 — OFFICE FLOOR SCENE
// ============================================================
@Composable
private fun OfficeCenter(modifier: Modifier = Modifier) {
    val agents = remember {
        listOf(
            AgentDesk("Marvis", "主控 Agent", Icons.Default.Psychology, Accent,
                "正在执行 1 个项目", 0.0153f, "15.3万", "1000万",
                isActive = true, statusText = "执行中", row = 1, col = 2),
            AgentDesk("Browser Agent", "浏览器", Icons.Default.Language, Blue500,
                "空闲等待", 0f, "0", "500万",
                isActive = false, statusText = "空闲", row = 0, col = 1),
            AgentDesk("Computer Agent", "系统管理", Icons.Default.Computer, Green500,
                "已完成上次任务", 0.32f, "160万", "500万",
                isActive = false, statusText = "已完成", row = 0, col = 3),
            AgentDesk("File Agent", "文件处理", Icons.Default.FolderOpen, Orange500,
                "正在整理文件", 0.067f, "33.5万", "500万",
                isActive = true, statusText = "执行中", row = 1, col = 0),
            AgentDesk("Search Agent", "搜索检索", Icons.Default.Search, Purple500,
                "空闲等待", 0f, "0", "300万",
                isActive = false, statusText = "空闲", row = 1, col = 4),
            AgentDesk("App Agent", "应用操作", Icons.Default.Apps, Teal500,
                "任务已完成", 0.921f, "460.5万", "500万",
                isActive = false, statusText = "已完成", row = 2, col = 2),
        )
    }

    Column(modifier = modifier.fillMaxHeight().background(BgCenter)) {
        // Title bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Business, null, tint = Blue500, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("SmartAgents 办公室", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
            Spacer(Modifier.weight(1f))
            // Status indicators
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(StatusRunning))
                Spacer(Modifier.width(4.dp))
                Text("2 活跃中", fontSize = 11.sp, color = TextSec)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(7.dp).clip(CircleShape).background(StatusIdle))
                Spacer(Modifier.width(4.dp))
                Text("2 空闲", fontSize = 11.sp, color = TextSec)
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(7.dp).clip(CircleShape).background(StatusDone))
                Spacer(Modifier.width(4.dp))
                Text("2 已完成", fontSize = 11.sp, color = TextSec)
            }
        }

        Divider(color = DividerColor, thickness = 1.dp)

        // Office floor — scrollable grid
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(32.dp)
        ) {
            // Floor background with grid pattern
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF0F2F5))
                    .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            ) {
                // Grid floor tiles
                Canvas(Modifier.fillMaxSize()) {
                    val tileSize = 48f
                    var x = 0f
                    while (x < size.width) {
                        var y = 0f
                        while (y < size.height) {
                            drawRect(
                                color = Color(0xFFE8EAED),
                                topLeft = Offset(x, y),
                                size = androidx.compose.ui.geometry.Size(tileSize - 1f, tileSize - 1f)
                            )
                            y += tileSize
                        }
                        x += tileSize
                    }
                    // Subtle border
                    drawRoundRect(
                        color = BorderLight,
                        style = Stroke(1f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )
                }

                // Agent desk cards laid out in a grid
                // Row labels
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("A区", "B区", "C区", "D区", "E区").forEach {
                        Text(it, fontSize = 10.sp, color = TextHint, fontWeight = FontWeight.Medium)
                    }
                }

                // Agent desks — positioned in a 3-row × 5-col grid
                agents.forEach { agent ->
                    val xOffset = 20.dp + (agent.col * 140).dp
                    val yOffset = 40.dp + (agent.row * 170).dp

                    Box(modifier = Modifier.padding(start = xOffset, top = yOffset)) {
                        AgentDeskCard(agent)
                    }
                }

                // Center label for Marvis
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    // Blank — Marvis is positioned in the grid
                }
            }
        }
    }
}

@Composable
private fun AgentDeskCard(agent: AgentDesk) {
    val statusColor = when {
        agent.isActive -> StatusRunning
        agent.statusText == "已完成" -> StatusDone
        else -> StatusIdle
    }

    Card(
        modifier = Modifier.width(136.dp).height(156.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (agent.isActive) 4.dp else 1.dp),
        border = BorderStroke(
            width = if (agent.isActive) 2.dp else 0.5.dp,
            color = if (agent.isActive) agent.color.copy(alpha = 0.5f) else BorderLight
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status dot + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                Spacer(Modifier.width(5.dp))
                Text(agent.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPri,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(8.dp))

            // Agent icon
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(agent.color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(agent.icon, null, tint = agent.color, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.height(6.dp))

            // Role
            Text(agent.role, fontSize = 10.sp, color = TextHint)

            Spacer(Modifier.height(4.dp))

            // Task
            Text(agent.task, fontSize = 10.sp, color = TextSec, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center)

            if (agent.isActive && agent.total != "0") {
                Spacer(Modifier.height(6.dp))

                // Progress
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(agent.consumed, fontSize = 9.sp, color = TextHint)
                        Text(agent.total, fontSize = 9.sp, color = TextHint)
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(BorderLight)) {
                        Box(
                            Modifier.fillMaxWidth(agent.progress).height(3.dp)
                                .clip(RoundedCornerShape(2.dp)).background(agent.color)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Status chip
            Text(
                agent.statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusColor.copy(alpha = 0.08f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ============================================================
// C栏 — RIGHT PANEL (Token Stats + Messages)
// ============================================================
@Composable
private fun OfficeRightPanel() {
    Column(
        modifier = Modifier.width(300.dp).fillMaxHeight().background(BgSidebar)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // === TOKEN STATS ===
        Text("今日消耗 Token", fontSize = 12.sp, color = TextHint)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("15.3万", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Blue500)
            Text(" / 1000万", fontSize = 13.sp, color = TextHint, modifier = Modifier.padding(bottom = 2.dp))
        }
        Spacer(Modifier.height(4.dp))
        // Progress bar
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(BorderLight)) {
            Box(Modifier.fillMaxWidth(0.0153f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Blue500))
        }

        Spacer(Modifier.height(14.dp))
        Divider(color = DividerColor, thickness = 1.dp)
        Spacer(Modifier.height(14.dp))

        Text("今日节省 Token", fontSize = 12.sp, color = TextHint)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextHint)
            Spacer(Modifier.width(6.dp))
            Text("当前未有任何节省", fontSize = 11.sp, color = TextHint.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 1.dp))
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = DividerColor, thickness = 1.dp)
        Spacer(Modifier.height(14.dp))

        // === CONVERSATION DETAILS ===
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("对话明细", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
            Spacer(Modifier.weight(1f))
            Text("全部 >", fontSize = 12.sp, color = Blue500)
        }

        Spacer(Modifier.height(10.dp))

        // Stat cards row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 进行中
            Card(
                Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
                    Text("0", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Blue500)
                    Text("进行中", fontSize = 11.sp, color = TextSec)
                }
            }
            // 已完成
            Card(
                Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = BgWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.5.dp, BorderLight),
            ) {
                Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
                    Text("17", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPri)
                    Text("已完成", fontSize = 11.sp, color = TextSec)
                }
            }
            // 总计
            Card(
                Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = BgWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(0.5.dp, BorderLight),
            ) {
                Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
                    Text("17", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPri)
                    Text("总计", fontSize = 11.sp, color = TextSec)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // === MESSAGE HISTORY ===
        val messages = listOf(
            OfficeMessage("我想做一个AI软件，手机端，安卓端…", "46.1万", "21:05 07/19", "已完成", StatusDone),
            OfficeMessage("在本次对话中：你所在的工作目录是E…", "48.2万", "21:54 07/16", "已完成", StatusDone),
            OfficeMessage("把水印的天气或者海拔任意一个字段…", "66.6万", "18:39 07/16", "已完成", StatusDone),
            OfficeMessage("微信ClawBot消息", "12.4万", "18:51 07/15", "已完成", StatusDone),
            OfficeMessage("帮我清理C盘，先看看哪里可以清理，…", "32.1万", "11:07 07/11", "已完成", StatusDone),
            OfficeMessage("整理D:\\桌面", "32.8万", "10:56 07/10", "已完成", StatusDone),
            OfficeMessage("我们要送我一个网站或软件，具体要…", "413.7万", "09:05 07/10", "已完成", StatusDone),
        )

        messages.forEachIndexed { idx, msg ->
            if (idx > 0) {
                Divider(color = DividerColor, thickness = 0.5.dp)
            }
            Column(
                Modifier.fillMaxWidth()
                    .clickable { }
                    .padding(vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(msg.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPri,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = TextHint.copy(alpha = 0.4f),
                        modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("累计 Token: ${msg.tokenConsumed}", fontSize = 11.sp, color = TextHint)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(msg.time, fontSize = 10.sp, color = TextHint.copy(alpha = 0.7f))
                        Spacer(Modifier.width(6.dp))
                        Text(msg.status, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = msg.statusColor)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Input area
        Box(
            Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))
                .border(1.dp, BorderInput, RoundedCornerShape(10.dp))
                .background(BgWhite)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("输入消息…", fontSize = 13.sp, color = TextHint)
        }

        Spacer(Modifier.height(16.dp))
    }
}
