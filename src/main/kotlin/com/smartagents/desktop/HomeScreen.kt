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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
// Design Tokens — cohesive color system
// ============================================================
private val Accent = Color(0xFFE53935)
private val AccentLight = Color(0xFFFFEBEE)
private val AccentDark = Color(0xFFC62828)
private val BgWhite = Color(0xFFFFFFFF)
private val BgSidebar = Color(0xFFF5F5F5)
private val BgCard = Color(0xFFFAFAFA)
private val BgInput = Color(0xFFF0F0F0)
private val BorderLight = Color(0xFFE0E0E0)
private val BorderInput = Color(0xFFD0D0D0)
private val TextPri = Color(0xFF1A1A1A)
private val TextSec = Color(0xFF5F6368)
private val TextHint = Color(0xFF9AA0A6)
private val TextOnAccent = Color.White
private val DividerColor = Color(0xFFEEEEEE)

// Card accent palette — softer, harmonious
private val CardColors = listOf(
    Color(0xFFE53935), // red
    Color(0xFFFB8C00), // orange
    Color(0xFF1E88E5), // blue
    Color(0xFF43A047), // green
    Color(0xFF8E24AA), // purple
    Color(0xFF00ACC1), // teal
)

data class TaskCardData(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
fun HomeScreen(auth: AuthState, onLogout: () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        background = BgWhite,
        surface = BgWhite,
        onSurface = TextPri,
    )) {
        Row(Modifier.fillMaxSize().background(BgWhite)) {
            LeftSidebar(auth, onLogout)
            Box(Modifier.width(1.dp).fillMaxHeight().background(DividerColor))
            CenterMain(Modifier.weight(1f))
            Box(Modifier.width(1.dp).fillMaxHeight().background(DividerColor))
            RightSidebar()
        }
    }
}

// ============================================================
// LEFT SIDEBAR
// ============================================================
@Composable
private fun LeftSidebar(auth: AuthState, onLogout: () -> Unit) {
    var searchText by remember { mutableStateOf("") }
    var kbExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.width(260.dp).fillMaxHeight().background(BgSidebar)
            .padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Brand
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text("SmartAgents", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPri)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Search, null, tint = TextHint, modifier = Modifier.size(18.dp))
        }

        Spacer(Modifier.height(12.dp))

        // Search
        OutlinedTextField(
            value = searchText, onValueChange = { searchText = it },
            placeholder = { Text("搜索工具和技能", color = TextHint, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth().height(38.dp),
            singleLine = true, shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorderLight, unfocusedBorderColor = BorderLight,
                focusedContainerColor = BgWhite, unfocusedContainerColor = BgWhite,
            ),
            textStyle = TextStyle(fontSize = 13.sp),
        )

        Spacer(Modifier.height(14.dp))

        // Nav
        NavItem(Icons.Default.Add, "新建对话")
        NavItem(Icons.Default.SmartToy, "自动任务")
        NavItem(Icons.Default.Store, "技能广场")

        Spacer(Modifier.height(12.dp))
        Divider(color = DividerColor, thickness = 1.dp)
        Spacer(Modifier.height(10.dp))

        // Scrollable area
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

            // Chat History — empty
            Text("对话历史", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSec)
            Spacer(Modifier.height(20.dp))
            Text("暂无对话记录", fontSize = 13.sp, color = TextHint, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(8.dp))

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
// CENTER MAIN
// ============================================================
@Composable
private fun CenterMain(modifier: Modifier = Modifier) {
    val avatar: Painter = painterResource("avatar.png")
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("推荐", "办公学习", "电脑设置", "生活日常", "游戏娱乐")

    val cards = listOf(
        TaskCardData("失控进化福利活动一览", "查看失控进化最新福利和活动", Icons.Default.CardGiftcard, CardColors[0]),
        TaskCardData("小马帮你点咖啡", "让小马帮你下单瑞幸咖啡", Icons.Default.LocalCafe, CardColors[1]),
        TaskCardData("产品经理救急", "快速生成PRD和需求文档", Icons.Default.Description, CardColors[2]),
        TaskCardData("失控进化性能优化", "分析和优化游戏性能表现", Icons.Default.Speed, CardColors[3]),
        TaskCardData("同花顺行情分析", "实时股票行情与技术分析", Icons.Default.TrendingUp, CardColors[4]),
        TaskCardData("文件智能整理", "按类型自动归类文件夹", Icons.Default.FolderOpen, CardColors[5]),
    )

    Column(
        modifier = modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))

        // Avatar — crisp circle with subtle border
        Box(Modifier.size(88.dp).clip(CircleShape).background(BgWhite).border(2.dp, BorderLight, CircleShape),
            contentAlignment = Alignment.Center) {
            Image(avatar, "SmartAgents", Modifier.size(82.dp).clip(CircleShape))
        }

        Spacer(Modifier.height(14.dp))
        Text("SmartAgents", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPri)
        Text("AI 合作体·数据不再孤岛", fontSize = 13.sp, color = TextSec, modifier = Modifier.padding(top = 2.dp))

        Spacer(Modifier.height(28.dp))

        // Input area — bordered card feel
        Box(Modifier.fillMaxWidth().padding(horizontal = 60.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .border(1.dp, BorderInput, RoundedCornerShape(14.dp)).background(BgWhite)) {

                // Input field area
                Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text("请输入任务，交给我来帮你完成", fontSize = 15.sp, color = TextHint)
                }

                // Bottom bar
                Divider(color = BorderLight, thickness = 0.5.dp)
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachFile, null, tint = TextHint, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("选择文件", fontSize = 13.sp, color = TextHint)
                    }
                    // Send button
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Accent),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Send, null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        // Tab bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 60.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            tabs.forEachIndexed { i, t ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedTab = i }) {
                    Text(t, fontSize = 14.sp,
                        fontWeight = if (i == selectedTab) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (i == selectedTab) TextPri else TextSec)
                    if (i == selectedTab) {
                        Spacer(Modifier.height(5.dp))
                        Box(Modifier.width(20.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Accent))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Card Grid 2×3
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 60.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                cards.subList(0, 3).forEach { TaskCard(it, Modifier.weight(1f)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                cards.subList(3, 6).forEach { TaskCard(it, Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun TaskCard(data: TaskCardData, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(0.5.dp, BorderLight),
    ) {
        Row(Modifier.fillMaxSize().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon circle
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(data.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center) {
                Icon(data.icon, null, tint = data.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(data.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPri,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(data.desc, fontSize = 12.sp, color = TextSec, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp)
            }
        }
    }
}

// ============================================================
// RIGHT SIDEBAR — server push, normally empty
// ============================================================
@Composable
private fun RightSidebar() {
    Column(
        modifier = Modifier.width(260.dp).fillMaxHeight().background(BgSidebar)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("消息推送", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
        Spacer(Modifier.height(12.dp))
        Divider(color = DividerColor, thickness = 1.dp)

        // Empty state
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(BgInput), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MailOutline, null, tint = TextHint.copy(alpha = 0.5f), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("等待服务器推送", fontSize = 14.sp, color = TextHint)
            Spacer(Modifier.height(2.dp))
            Text("连接后将实时展示任务通知", fontSize = 12.sp, color = TextHint.copy(alpha = 0.5f))
        }
    }
}
