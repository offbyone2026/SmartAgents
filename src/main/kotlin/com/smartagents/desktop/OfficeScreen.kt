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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
private val Indigo500 = Color(0xFF3F51B5)
private val IndigoLight = Color(0xFF7986CB)

// ============================================================
// Agent Desk Model
// ============================================================
data class OfficeAgent(
    val name: String,
    val role: String,
    val color: Color,
    val dotColor: Color,
    val icon: ImageVector,
    val faceIndex: Int,
    val description: String,
    val skills: List<String>,
    val isActive: Boolean = false,
)

// ============================================================
// Agent Model Config
// ============================================================
data class AgentModelConfig(
    val agentName: String,
    val mode: String = "cloud",            // "cloud" or "local"
    val apiMode: String = "official",      // "official" or "private" — only meaningful for Dispatcher
    val apiKey: String = "",
    val localPath: String = "D:\\SmartAgents\\models",
    val isImplemented: Boolean = false,    // 是否已实装（已开发）
    val isInstalled: Boolean = false,      // 是否已安装模型/配置云端
    val hasOfficialApi: Boolean = false,   // 是否有官方 API 可用
)

// ============================================================
// Agent Capability Registry
// ============================================================
data class ToolCapability(
    val name: String,
    val description: String,
    val enabled: Boolean = false,          // 是否已启用（实装后可勾选）
)

val agentCapabilities: Map<String, List<ToolCapability>> = mapOf(
    "Dispatcher" to listOf(
        ToolCapability("dispatch_task", "任务拆分与派发", enabled = true),
        ToolCapability("present_result", "结果汇总展示", enabled = true),
        ToolCapability("ask_user", "关键确认询问", enabled = true),
        ToolCapability("use_skill", "加载领域技能", enabled = true),
    ),
    "File Agent" to listOf(
        ToolCapability("read_file", "读取文件内容"),
        ToolCapability("write_file", "写入/创建文件"),
        ToolCapability("edit_file", "精确编辑文件"),
        ToolCapability("delete", "删除文件/目录"),
        ToolCapability("search_file", "文件搜索"),
        ToolCapability("list_dir", "列出目录内容"),
        ToolCapability("convert_format", "文件格式转换"),
    ),
    "Browser Agent" to listOf(
        ToolCapability("browser_navigate", "访问指定 URL"),
        ToolCapability("browser_click", "网页元素点击"),
        ToolCapability("browser_type", "表单输入填充"),
        ToolCapability("browser_scroll", "页面滚动"),
        ToolCapability("screenshot_page", "网页截图"),
        ToolCapability("extract_data", "数据抓取/提取"),
    ),
    "Computer Agent" to listOf(
        ToolCapability("shell_exec", "Shell 命令执行"),
        ToolCapability("registry_query", "注册表查询"),
        ToolCapability("registry_modify", "注册表修改"),
        ToolCapability("process_kill", "进程终止"),
        ToolCapability("process_list", "进程列表查询"),
        ToolCapability("service_control", "服务启停"),
        ToolCapability("window_manage", "窗口管理"),
        ToolCapability("disk_analyze", "磁盘空间分析"),
    ),
    "Search Agent" to listOf(
        ToolCapability("web_search", "联网关键词搜索"),
        ToolCapability("web_fetch", "网页正文抓取"),
        ToolCapability("rag_query", "知识库检索"),
        ToolCapability("multi_engine", "多引擎聚合搜索"),
    ),
    "App Agent" to listOf(
        ToolCapability("app_install", "应用安装"),
        ToolCapability("app_launch", "应用启动"),
        ToolCapability("app_uninstall", "应用卸载"),
        ToolCapability("app_tap", "屏幕点击"),
        ToolCapability("app_input", "文本输入"),
        ToolCapability("app_swipe", "滑动操作"),
        ToolCapability("screenshot_app", "应用截图"),
    ),
    "Media Agent" to listOf(
        ToolCapability("image_generate", "AI 图片生成"),
        ToolCapability("video_generate", "AI 视频生成"),
        ToolCapability("model_3d", "3D 模型生成"),
        ToolCapability("poster_design", "海报设计"),
        ToolCapability("image_edit", "图片编辑/修复"),
    ),
)

// ============================================================
// OFFICE SCREEN
// ============================================================
@Composable
fun OfficeScreen(auth: AuthState, onBack: () -> Unit, onLogout: () -> Unit, initialPage: String = "office") {
    var showSettings by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(initialPage) }
    var workingAgent by remember { mutableStateOf("Dispatcher") }
    var modelConfigs by remember {
        mutableStateOf(listOf(
            AgentModelConfig("Dispatcher", isImplemented = true, isInstalled = true, hasOfficialApi = true, apiMode = "official"),
            AgentModelConfig("File Agent"),
            AgentModelConfig("Browser Agent"),
            AgentModelConfig("Computer Agent"),
            AgentModelConfig("Search Agent"),
            AgentModelConfig("App Agent"),
            AgentModelConfig("Media Agent"),
        ))
    }

    MaterialTheme(colorScheme = lightColorScheme(
        background = BgWhite,
        surface = BgWhite,
        onSurface = TextPri,
    )) {
        Row(Modifier.fillMaxSize().background(BgWhite)) {
            // A栏 — Sidebar
            OfficeSidebar(auth, onBack, onLogout, onSettings = { showSettings = true }, onNavigate = { currentPage = it })

            // Red cut line A | B
            Box(Modifier.width(3.dp).fillMaxHeight().background(RedCut))

            // B栏 — Content area (conditional)
            when (currentPage) {
                "office" -> OfficeCenter(Modifier.weight(1f))
                "skill-market" -> SkillMarketPage(
                    Modifier.weight(1f),
                    onBack = { currentPage = "office" }
                )
                "auto-tasks" -> AutoTaskPage(
                    Modifier.weight(1f),
                    onCreateTask = { },
                    onBack = { currentPage = "office" }
                )
                "kb-apps" -> KnowledgeBasePage(Modifier.weight(1f), "应用", Icons.Default.Apps, onBack = { currentPage = "office" })
                "kb-docs" -> KnowledgeBasePage(Modifier.weight(1f), "文档", Icons.Default.Description, onBack = { currentPage = "office" })
                "kb-gallery" -> KnowledgeBasePage(Modifier.weight(1f), "图库", Icons.Default.PhotoLibrary, onBack = { currentPage = "office" })
                "kb-computer" -> KnowledgeBasePage(Modifier.weight(1f), "此电脑", Icons.Default.Computer, onBack = { currentPage = "office" })
            }

            // Red cut line B | C
            Box(Modifier.width(3.dp).fillMaxHeight().background(RedCut))

            // C栏 — Right Panel (tabs)
            OfficeRightPanel()
        }
    }

    if (showSettings) {
        SettingsDialog(
            modelConfigs = modelConfigs,
            workingAgent = workingAgent,
            onSwitchAgent = { newAgent -> workingAgent = newAgent },
            onUpdateConfig = { idx, newConfig -> modelConfigs = modelConfigs.toMutableList().also { it[idx] = newConfig } },
            onDismiss = { showSettings = false },
        )
    }
}

// ============================================================
// A栏 — SIDEBAR (3 sections, red dividers)
// ============================================================
@Composable
private fun OfficeSidebar(auth: AuthState, onBack: () -> Unit, onLogout: () -> Unit, onSettings: () -> Unit = {}, onNavigate: (String) -> Unit = {}) {
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

        SideNavItem(Icons.Default.Add, "新建对话", onClick = { onBack() })
        SideNavItem(Icons.Default.SmartToy, "自动任务", onClick = { onNavigate("auto-tasks") })
        SideNavItem(Icons.Default.Store, "技能广场", onClick = { onNavigate("skill-market") })
        SideNavItem(Icons.Default.Settings, "设置", onClick = { onSettings() })

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
            SideKbRow(Icons.Default.Apps, "应用", onClick = { onNavigate("kb-apps") })
            SideKbRow(Icons.Default.Description, "文档", onClick = { onNavigate("kb-docs") })
            SideKbRow(Icons.Default.PhotoLibrary, "图库", onClick = { onNavigate("kb-gallery") })
            SideKbRow(Icons.Default.Computer, "此电脑", onClick = { onNavigate("kb-computer") })
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
private fun SideNavItem(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSec, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = TextPri)
    }
}

@Composable
private fun SideKbRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp).padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp)).clickable { onClick() },
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
    var selectedAgent by remember { mutableStateOf<OfficeAgent?>(null) }

    val agents = remember {
        listOf(
            OfficeAgent("Dispatcher", "任务调度中枢", Accent, Color(0xFFEF5350),
                Icons.Default.Psychology, faceIndex = 0,
                description = "统筹全局，智能调度所有专业 Agent 协同工作，确保每个任务都能精准匹配到最合适的执行者，是办公室的最高指挥官。",
                skills = listOf("任务分派", "状态追踪", "结果汇总", "Agent 编排"),
                isActive = true),
            OfficeAgent("File Agent", "数字资产管家", Orange500, Color(0xFFFFA726),
                Icons.Default.FolderOpen, faceIndex = 1,
                description = "精通各类文件处理：查找、阅读、分析、转换、批量整理。无论是发票、合同还是代码仓库，交给他都能高效搞定。",
                skills = listOf("文件搜索", "文档分析", "格式转换", "批量整理", "内容提取")),
            OfficeAgent("Browser Agent", "网页交互专员", Blue500, Color(0xFF42A5F5),
                Icons.Default.Language, faceIndex = 2,
                description = "浏览器自动化专家，能自主访问网页、填写表单、提取数据、处理登录跳转，是网页端的全能打工人。",
                skills = listOf("网页访问", "表单填写", "数据抓取", "登录认证")),
            OfficeAgent("Computer Agent", "系统运维专家", Green500, Color(0xFF66BB6A),
                Icons.Default.Computer, faceIndex = 3,
                description = "Windows 系统管家，精通系统设置、故障排查、窗口管理、进程调度。电脑出了问题，找他准没错。",
                skills = listOf("系统配置", "故障排查", "窗口管理", "进程调度", "性能优化")),
            OfficeAgent("Search Agent", "深度搜索专家", Purple500, Color(0xFFAB47BC),
                Icons.Default.Search, faceIndex = 4,
                description = "多轮联网检索与内容综合分析引擎，擅长信息挖掘、论文检索、资料综述和竞品调研。",
                skills = listOf("联网搜索", "内容挖掘", "对比分析", "论文检索", "资料综述")),
            OfficeAgent("App Agent", "应用操作专员", Teal500, Color(0xFF26C6DA),
                Icons.Default.Apps, faceIndex = 5,
                description = "Android 与 Windows 应用全能手，负责 App 的下载、安装、打开、交互操作。在模拟器环境中自如运行各类应用。",
                skills = listOf("应用安装", "界面交互", "APK 分析")),
            OfficeAgent("Media Agent", "多媒体生成工程师", Indigo500, Color(0xFF7986CB),
                Icons.Default.Brush, faceIndex = 6,
                description = "创意多媒体生成专家，擅长 AI 绘图、视频制作、3D 建筑模型渲染。能把文字描述变成精美的视觉作品。",
                skills = listOf("AI 绘图", "视频生成", "3D 建模", "海报设计", "建筑可视化")),
        )
    }

    Column(modifier = modifier.fillMaxHeight().background(BgCenter)) {
        // Title bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Business, null, tint = RedCut, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Text("Dispatcher 办公室", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPri)
            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFE8F5E9),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Green500))
                    Spacer(Modifier.width(6.dp))
                    Text("7 位 Agents 在线", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32))
                }
            }
        }

        Divider(color = DividerColor, thickness = 1.dp)

        // Office floor with grid background
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF4F5F7))
                .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
        ) {
            // Floor grid
            Canvas(Modifier.fillMaxSize()) {
                val tileW = 48f
                val tileH = 48f
                var x = 0f
                while (x < size.width) {
                    drawLine(Color(0xFFE8E9EC), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += tileW
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color(0xFFE8E9EC), Offset(0f, y), Offset(size.width, y), 1f)
                    y += tileH
                }
            }

            // 2 rows: row1=4 agents, row2=3 agents
            Column(
                Modifier.fillMaxSize().padding(horizontal = 40.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(Modifier.height(24.dp))
                // Row 1: 4 agents
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    agents.take(4).forEach { agent ->
                        AgentDeskCard(agent, onClick = { selectedAgent = agent })
                    }
                }
                Spacer(Modifier.height(20.dp))
                // Row 2: 3 agents (centered by SpaceEvenly with padding)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 100.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    agents.drop(4).take(3).forEach { agent ->
                        AgentDeskCard(agent, onClick = { selectedAgent = agent })
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Agent Detail Dialog
    selectedAgent?.let { agent ->
        AgentDetailDialog(agent = agent, onDismiss = { selectedAgent = null })
    }
}

// ============================================================
// Agent Desk Card — grid cell
// ============================================================
@Composable
private fun AgentDeskCard(agent: OfficeAgent, onClick: () -> Unit) {
    val cardAlpha = if (agent.isActive) 1f else 0.45f
    val statusText = if (agent.isActive) "运行中" else "未实装"
    val statusColor = if (agent.isActive) Green500 else TextHint
    val statusBg = if (agent.isActive) Color(0xFFE8F5E9) else Color(0xFFEEEEEE)
    val statusDotColor = if (agent.isActive) Green500 else TextHint

    Card(
        modifier = Modifier.width(180.dp).height(210.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(0.5.dp, BorderLight),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Color bar at top (gray for inactive)
            val barColor = if (agent.isActive) agent.color else TextHint
            Box(
                Modifier.fillMaxWidth().height(4.dp)
                    .background(barColor)
            )

            Spacer(Modifier.height(12.dp))

            // Avatar — gray overlay for inactive
            Box(
                modifier = Modifier.size(52.dp).graphicsLayer(alpha = cardAlpha),
                contentAlignment = Alignment.Center
            ) {
                AvatarFace(agent.faceIndex, 52, Modifier.size(52.dp))
                if (!agent.isActive) {
                    // Gray circle overlay on avatar
                    Canvas(Modifier.size(52.dp)) {
                        drawCircle(
                            color = Color(0xFFBDBDBD).copy(alpha = 0.5f),
                            radius = size.minDimension / 2
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Name
            Text(
                agent.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (agent.isActive) TextPri else TextHint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            // Role tag
            val tagColor = if (agent.isActive) agent.color else TextHint
            val tagBgColor = if (agent.isActive) agent.color.copy(alpha = 0.08f) else Color(0xFFF0F0F0)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = tagBgColor,
            ) {
                Text(
                    agent.role,
                    fontSize = 11.sp,
                    color = tagColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(6.dp))

            // Status with dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(statusDotColor))
                Spacer(Modifier.width(5.dp))
                Text(statusText, fontSize = 12.sp, color = statusColor)
            }

            Spacer(Modifier.weight(1f))

            // Bottom bar
            val bottomColor = if (agent.isActive) agent.color.copy(alpha = 0.06f) else Color(0xFFF5F5F5)
            val bottomIconColor = if (agent.isActive) agent.color.copy(alpha = 0.7f) else TextHint
            val bottomTextColor = if (agent.isActive) agent.color.copy(alpha = 0.8f) else TextHint
            Box(
                Modifier.fillMaxWidth().height(30.dp)
                    .background(bottomColor),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        agent.icon,
                        null,
                        tint = bottomIconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (agent.isActive) "查看详情" else "敬请期待",
                        fontSize = 11.sp,
                        color = bottomTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ============================================================
// Agent Detail Dialog — full profile popup
// ============================================================
@Composable
private fun AgentDetailDialog(agent: OfficeAgent, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.width(380.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BgWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with gradient overlay
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    // Gradient background
                    Canvas(Modifier.fillMaxSize()) {
                        val gradient = Brush.verticalGradient(
                            colors = listOf(agent.color.copy(alpha = 0.15f), BgWhite),
                            startY = 0f,
                            endY = size.height
                        )
                        drawRect(gradient)
                        // Accent bar at top
                        drawRect(
                            color = agent.color,
                            topLeft = Offset(0f, 0f),
                            size = Size(size.width, 5.dp.toPx())
                        )
                    }

                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            null,
                            tint = TextHint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Avatar + name row
                    Row(
                        Modifier.fillMaxWidth().padding(start = 24.dp, top = 30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Large avatar
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp),
                            shadowElevation = 4.dp
                        ) {
                            AvatarFace(agent.faceIndex, 64, Modifier.size(64.dp))
                        }

                        Spacer(Modifier.width(16.dp))

                        Column {
                            Text(
                                agent.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPri,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                agent.role,
                                fontSize = 14.sp,
                                color = agent.color,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(4.dp))
                            // Status badge
                            val dialogStatusText = if (agent.isActive) "运行中 · DeepSeek V4" else "未实装"
                            val dialogStatusColor = if (agent.isActive) Color(0xFF2E7D32) else Color(0xFF757575)
                            val dialogStatusBg = if (agent.isActive) Color(0xFFE8F5E9) else Color(0xFFEEEEEE)
                            val dialogDotColor = if (agent.isActive) Green500 else TextHint
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = dialogStatusBg,
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(dialogDotColor))
                                    Spacer(Modifier.width(5.dp))
                                    Text(dialogStatusText, fontSize = 12.sp, color = dialogStatusColor)
                                }
                            }
                        }
                    }
                }

                // Body
                Column(Modifier.padding(horizontal = 24.dp)) {
                    // Description
                    Text(
                        "简介",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPri,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        agent.description,
                        fontSize = 13.sp,
                        color = TextSec,
                        lineHeight = 20.sp,
                    )

                    if (agent.isActive) {
                        Spacer(Modifier.height(16.dp))

                        // Skills
                        Text(
                            "技能",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPri,
                        )
                        Spacer(Modifier.height(8.dp))

                        // Skill chips in flow layout simulation
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            agent.skills.chunked(3).forEach { rowSkills ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowSkills.forEach { skill ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = agent.color.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, agent.color.copy(alpha = 0.25f)),
                                        ) {
                                            Text(
                                                skill,
                                                fontSize = 13.sp,
                                                color = agent.color,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
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

// ============================================================
// SETTINGS DIALOG
// ============================================================
@Composable
private fun SettingsDialog(
    modelConfigs: List<AgentModelConfig>,
    workingAgent: String,
    onSwitchAgent: (String) -> Unit,
    onUpdateConfig: (Int, AgentModelConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultPath = "D:\\SmartAgents\\models"
    var useLocal by remember { mutableStateOf(ChatApi.useLocal) }
    val localStatus by remember { derivedStateOf { LocalModelManager.getStatus() } }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = false) { }
    ) {
        Card(
            Modifier.fillMaxWidth(0.72f).fillMaxHeight(0.88f).align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BgWhite),
            elevation = CardDefaults.cardElevation(8.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(start = 24.dp, top = 18.dp, end = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPri)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "关闭", tint = TextHint, modifier = Modifier.size(22.dp))
                    }
                }
                Divider(color = BorderLight, thickness = 1.dp)

                // ─── Global Model Source ───
                Spacer(Modifier.height(14.dp))
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (localStatus is LocalModelManager.Status.Running) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                    border = BorderStroke(1.dp, if (localStatus is LocalModelManager.Status.Running) Color(0xFFA5D6A7) else Color(0xFFFFE082)),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("模型来源", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
                            Spacer(Modifier.weight(1f))
                            // Cloud / Local toggle
                            Row(
                                Modifier.background(BgSidebar, RoundedCornerShape(6.dp)).padding(3.dp),
                            ) {
                                for (opt in listOf(false to "云端", true to "本地")) {
                                    val selected = useLocal == opt.first
                                    Box(
                                        Modifier
                                            .background(if (selected) BgWhite else Color.Transparent, RoundedCornerShape(4.dp))
                                            .clickable {
                                                useLocal = opt.first
                                                ChatApi.useLocal = opt.first
                                                if (opt.first && localStatus is LocalModelManager.Status.InstalledButNotRunning) {
                                                    LocalModelManager.startServer()
                                                }
                                            }
                                            .padding(horizontal = 18.dp, vertical = 6.dp),
                                    ) {
                                        Text(opt.second, fontSize = 13.sp,
                                            color = if (selected) TextPri else TextSec,
                                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        // Local model status & controls
                        LocalModelStatusBar(localStatus)
                    }
                }

                // Current working model indicator
                Spacer(Modifier.height(12.dp))
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = AccentLight,
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.PlayCircle, null, tint = Accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "当前工作模型：",
                            fontSize = 13.sp, color = TextSec,
                        )
                        Text(
                            workingAgent,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Accent,
                        )
                    }
                }

                // Scrollable body
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                ) {
                    Spacer(Modifier.height(20.dp))

                    // Section title
                    Text("AI 模型设置", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "为每个 Agent 配置云端 API 或下载本地模型。默认本地模型路径：$defaultPath",
                        fontSize = 12.sp, color = TextHint,
                    )
                    Spacer(Modifier.height(16.dp))

                    modelConfigs.forEach { config ->
                        AgentModelRow(config, defaultPath, workingAgent, onSwitchAgent)
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AgentModelRow(
    config: AgentModelConfig,
    defaultPath: String,
    workingAgent: String,
    onSwitchAgent: (String) -> Unit,
) {
    var mode by remember { mutableStateOf(config.mode) }
    var apiMode by remember { mutableStateOf(config.apiMode) }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var localPath by remember { mutableStateOf(config.localPath) }
    var showKey by remember { mutableStateOf(false) }

    val statusText: String
    val statusColor: Color
    when {
        config.isImplemented && config.isInstalled -> {
            statusText = "运行中"; statusColor = Color(0xFF4CAF50)
        }
        config.isImplemented && !config.isInstalled -> {
            statusText = "未安装"; statusColor = Color(0xFFFF9800)
        }
        else -> {
            statusText = "未实装"; statusColor = TextHint
        }
    }
    val isDisabled = !config.isImplemented

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDisabled) BgSidebar else BgWhite
        ),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            // Agent name + status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(config.agentName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPri)
                Spacer(Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        statusText, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                if (isDisabled) {
                    Spacer(Modifier.width(6.dp))
                    Text("（设备不支持或尚未开发）", fontSize = 11.sp, color = TextHint)
                }
                Spacer(Modifier.weight(1f))
                val isCurrentWorking = config.agentName == workingAgent
                if (config.isInstalled && !isCurrentWorking) {
                    TextButton(
                        onClick = { onSwitchAgent(config.agentName) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Accent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    ) {
                        Text("设为工作模型", fontSize = 12.sp)
                    }
                } else if (isCurrentWorking) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9),
                    ) {
                        Text(
                            "当前工作中",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ─── Dispatcher: Official / Private API ───
            if (config.hasOfficialApi) {
                DispatcherApiSection(apiMode, apiKey, showKey, onApiModeChange = { apiMode = it }, onApiKeyChange = { apiKey = it }, onToggleShow = { showKey = !showKey })
            }
            // ─── Other agents: Cloud / Local toggle ───
            else {
                // Cloud / Local toggle
                Row(
                    Modifier.background(BgSidebar, RoundedCornerShape(6.dp)).padding(3.dp),
                ) {
                    for (opt in listOf("cloud" to "云端", "local" to "本地")) {
                        val selected = mode == opt.first
                        Box(
                            Modifier
                                .background(if (selected) BgWhite else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable(enabled = !isDisabled) { mode = opt.first }
                                .padding(horizontal = 18.dp, vertical = 6.dp),
                        ) {
                            Text(
                                opt.second, fontSize = 13.sp,
                                color = if (isDisabled) TextHint.copy(alpha = 0.4f) else if (selected) TextPri else TextSec,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (mode == "cloud") {
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it },
                        placeholder = { Text("输入 API Key", fontSize = 13.sp, color = TextHint) },
                        singleLine = true, enabled = !isDisabled,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BorderLight, unfocusedBorderColor = BorderLight,
                            focusedContainerColor = BgWhite, unfocusedContainerColor = BgSidebar,
                        ),
                        textStyle = TextStyle(fontSize = 13.sp),
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }, enabled = !isDisabled) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = if (isDisabled) TextHint.copy(alpha = 0.3f) else TextHint,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        },
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { }, enabled = !isDisabled,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isDisabled) BorderLight.copy(alpha = 0.3f) else RedCut),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDisabled) TextHint.copy(alpha = 0.3f) else RedCut,
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("下载模型", fontSize = 13.sp)
                        }
                        OutlinedTextField(
                            value = localPath, onValueChange = { localPath = it },
                            singleLine = true, enabled = !isDisabled,
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BorderLight, unfocusedBorderColor = BorderLight,
                                focusedContainerColor = BgWhite, unfocusedContainerColor = BgSidebar,
                            ),
                            textStyle = TextStyle(fontSize = 12.sp, color = if (isDisabled) TextHint.copy(alpha = 0.4f) else TextPri),
                            placeholder = { Text("下载路径", fontSize = 12.sp, color = TextHint) },
                        )
                        OutlinedButton(
                            onClick = { }, enabled = !isDisabled,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isDisabled) BorderLight.copy(alpha = 0.3f) else BorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDisabled) TextHint.copy(alpha = 0.3f) else TextSec,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("浏览", fontSize = 13.sp)
                        }
                    }
                }
            }
            // ─── Tool Capabilities ───
            Spacer(Modifier.height(10.dp))
            Divider(color = BorderLight, thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            AgentToolChipsRow(config.agentName, isDisabled)
        }
    }
}

// ============================================================
// Dispatcher: Official API / Private Key
// ============================================================
@Composable
private fun DispatcherApiSection(
    apiMode: String, apiKey: String, showKey: Boolean,
    onApiModeChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onToggleShow: () -> Unit,
) {
    // Official / Private toggle
    Row(
        Modifier.background(BgSidebar, RoundedCornerShape(6.dp)).padding(3.dp),
    ) {
        for (opt in listOf("official" to "官方 API", "private" to "私人 Key")) {
            val selected = apiMode == opt.first
            Box(
                Modifier
                    .background(if (selected) BgWhite else Color.Transparent, RoundedCornerShape(4.dp))
                    .clickable { onApiModeChange(opt.first) }
                    .padding(horizontal = 18.dp, vertical = 6.dp),
            ) {
                Text(
                    opt.second, fontSize = 13.sp,
                    color = if (selected) TextPri else TextSec,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    if (apiMode == "official") {
        // Official: just a label, no key visible
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE8F5E9),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("官方 API（DeepSeek V4）— 已连接", fontSize = 13.sp, color = Color(0xFF2E7D32))
            }
        }
    } else {
        // Private: key input
        OutlinedTextField(
            value = apiKey, onValueChange = { onApiKeyChange(it) },
            placeholder = { Text("输入私人 API Key", fontSize = 13.sp, color = TextHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorderLight, unfocusedBorderColor = BorderLight,
                focusedContainerColor = BgWhite, unfocusedContainerColor = BgSidebar,
            ),
            textStyle = TextStyle(fontSize = 13.sp),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { onToggleShow() }) {
                    Icon(
                        if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null, tint = TextHint,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        )
    }
}

// ============================================================
// Tool Chips Row
// ============================================================
@Composable
private fun AgentToolChipsRow(agentName: String, isDisabled: Boolean) {
    val caps = agentCapabilities[agentName] ?: return
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        caps.forEach { cap ->
            val chipAlpha = if (isDisabled) 0.35f else if (cap.enabled) 1f else 0.5f
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (cap.enabled) Color(0xFFE3F2FD).copy(alpha = chipAlpha) else Color(0xFFF5F5F5).copy(alpha = chipAlpha),
            ) {
                Text(
                    cap.name,
                    fontSize = 10.sp,
                    color = if (cap.enabled) Color(0xFF1565C0).copy(alpha = chipAlpha) else TextHint.copy(alpha = chipAlpha),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
        }
    }
}

// ============================================================
// Local Model Status Bar
// ============================================================
@Composable
private fun LocalModelStatusBar(status: LocalModelManager.Status) {
    when (status) {
        is LocalModelManager.Status.NotInstalled -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Orange500, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("本地模型未安装", fontSize = 12.sp, color = TextSec)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = {
                        val setupPath = System.getProperty("user.dir") + "\\setup.ps1"
                        try { Runtime.getRuntime().exec(arrayOf("powershell", "-File", setupPath)) }
                        catch (_: Exception) { }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("一键部署", fontSize = 12.sp, color = Accent)
                }
            }
        }
        is LocalModelManager.Status.InstalledButNotRunning -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PauseCircle, null, tint = Orange500, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("本地模型已安装，未启动", fontSize = 12.sp, color = TextSec)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { LocalModelManager.startServer() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("启动", fontSize = 12.sp, color = Accent)
                }
            }
        }
        is LocalModelManager.Status.Running -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Green500))
                Spacer(Modifier.width(6.dp))
                Text("本地模型运行中 · qwen2.5:14b", fontSize = 12.sp, color = Color(0xFF2E7D32))
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { LocalModelManager.stopServer() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text("停止", fontSize = 12.sp, color = TextHint)
                }
            }
        }
        is LocalModelManager.Status.Error -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, null, tint = Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(status.message, fontSize = 12.sp, color = Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ============================================================
// Skill Market Page
// ============================================================
data class SkillCardData(
    val name: String,
    val description: String,
    val platform: String,
    val users: String,
    val color: Color
)

private val skillCards = listOf(
    SkillCardData("frontend-design", "帮你更好看、更有记忆点的页面", "Claude", "12.3万人添加", Color(0xFF7C4DFF)),
    SkillCardData("brainstorming", "创意迸发，为你打开灵感之门", "Claude", "9.8万人添加", Color(0xFFFF6D00)),
    SkillCardData("canvas-design", "生成精美的海报与视觉设计作品", "Claude", "8.5万人添加", Color(0xFF00C853)),
    SkillCardData("ui-ux-pro-max", "UI/UX 设计智能体，打造专业界面", "Claude", "6.7万人添加", Color(0xFF2979FF)),
    SkillCardData("humanizer", "让 AI 生成的文字更自然流畅", "GitHub", "4.9万人添加", Color(0xFF6200EA)),
    SkillCardData("multi-search-engine", "16 个搜索引擎聚合，全球搜索", "GitHub", "3.2万人添加", Color(0xFFFF1744)),
    SkillCardData("first-principles-decomposer", "第一性原理分解，从根本重建方案", "Claude", "5.1万人添加", Color(0xFF00E5FF)),
    SkillCardData("skill-vetter", "技能安全审查，安装前检查风险", "GitHub", "2.1万人添加", Color(0xFF76FF03)),
    SkillCardData("find-skills", "发现并安装新的技能扩展", "Claude", "4.4万人添加", Color(0xFFD500F9)),
    SkillCardData("luckin-coffee-ordering", "瑞幸咖啡全链路下单助手", "MCP", "1.3万人添加", Color(0xFF3D5AFE)),
    SkillCardData("persona-update-flow", "定制你的专属 AI 人设与风格", "Claude", "3.6万人添加", Color(0xFFFFAB00)),
)

@Composable
fun SkillMarketPage(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var selectedCategory by remember { mutableStateOf("全部") }
    val categories = listOf("全部", "办公学习", "电脑设置", "生活日常", "休闲娱乐")

    Column(modifier = modifier.fillMaxHeight().background(BgWhite)) {
        // Header bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = TextPri)
            }
            Text("工具箱", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPri)
            Spacer(Modifier.width(8.dp))
            Text("探索发现", fontSize = 12.sp, color = TextHint)
        }

        // Category tabs + search
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                val isSelected = cat == selectedCategory
                TextButton(
                    onClick = { selectedCategory = cat },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isSelected) Color.White else TextSec
                    ),
                ) {
                    Box(
                        Modifier.background(
                            if (isSelected) Accent else Color.Transparent,
                            RoundedCornerShape(15.dp)
                        ).padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text(cat, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = "", onValueChange = {},
                placeholder = { Text("搜索", fontSize = 12.sp, color = TextHint) },
                modifier = Modifier.width(140.dp).height(30.dp),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BorderLight, unfocusedBorderColor = BorderLight,
                    focusedContainerColor = BgSidebar, unfocusedContainerColor = BgSidebar
                ),
                textStyle = TextStyle(fontSize = 12.sp),
            )
            Spacer(Modifier.width(8.dp))
            Text("默认排序", fontSize = 12.sp, color = TextHint)
        }

        Divider(color = DividerColor, thickness = 1.dp)

        // Skill cards grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(skillCards) { skill ->
                SkillCard(skill)
            }
        }
    }
}

@Composable
private fun SkillCard(skill: SkillCardData) {
    Card(
        modifier = Modifier.fillMaxWidth().height(175.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(skill.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = skill.color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                skill.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPri,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                skill.description,
                fontSize = 11.sp,
                color = TextHint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(skill.color))
                Spacer(Modifier.width(5.dp))
                Text(
                    "${skill.platform} · ${skill.users}",
                    fontSize = 10.sp,
                    color = TextHint,
                    maxLines = 1,
                )
            }
        }
    }
}

// ============================================================
// Auto Task Page
// ============================================================
@Composable
fun AutoTaskPage(modifier: Modifier = Modifier, onCreateTask: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = modifier.fillMaxHeight().background(BgWhite),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = TextPri)
            }
            Text("自动任务", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPri)
        }

        Divider(color = DividerColor, thickness = 1.dp)

        Spacer(Modifier.height(100.dp))
        Text(
            "开启你的第一个自动任务吧",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPri,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Tips：请保持电脑开机并运行客户端，否则在关机、休眠或退出客户端时，自动任务无法执行",
            fontSize = 12.sp,
            color = TextHint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCreateTask,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.height(40.dp),
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("新建自动任务", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ============================================================
// Knowledge Base Page — real file browser
// ============================================================

@Composable
fun KnowledgeBasePage(modifier: Modifier = Modifier, title: String, icon: ImageVector, onBack: () -> Unit) {
    // Determine starting directory per tab
    val homeDir = System.getProperty("user.home") ?: "C:\\Users\\Default"
    val startDir = remember(title) {
        when (title) {
            "应用" -> java.io.File(System.getenv("ProgramData") ?: "C:\\ProgramData",
                "Microsoft\\Windows\\Start Menu\\Programs")
            "文档" -> java.io.File(homeDir, "Documents")
            "图库" -> java.io.File(homeDir, "Pictures")
            "此电脑" -> null // drive listing
            else -> java.io.File(homeDir)
        }
    }

    var currentDir by remember(title) { mutableStateOf(startDir) }
    var selectedFile by remember { mutableStateOf<java.io.File?>(null) }
    var fileContent by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Refresh file list when currentDir changes
    val entries = remember(currentDir) {
        val dir = currentDir
        if (dir == null) {
            // Drive listing
            java.io.File.listRoots().filter { it.exists() }.map { it }.sortedBy { it.path }
        } else {
            val kids = dir.listFiles()?.toList() ?: emptyList()
            kids.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        }
    }

    Column(modifier = modifier.fillMaxHeight().background(BgWhite)) {
        // Header
        val cur = currentDir
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, null, tint = TextPri)
            }
            Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (cur != null) cur.absolutePath else "此电脑",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPri,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            // Parent directory back button
            if (cur != null && cur.parentFile != null) {
                IconButton(onClick = { currentDir = cur.parentFile }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ArrowUpward, null, tint = TextSec, modifier = Modifier.size(18.dp))
                }
            }
        }

        Divider(color = DividerColor, thickness = 1.dp)

        // File preview panel (top half when selected)
        if (selectedFile != null) {
            Column(Modifier.fillMaxWidth().heightIn(max = 200.dp).background(BgSidebar).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (selectedFile!!.isDirectory) Icons.Default.Folder else fileIcon(selectedFile!!.name),
                        null, tint = Accent, modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(selectedFile!!.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPri,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { selectedFile = null; fileContent = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = TextHint, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Accent)
                } else if (fileContent != null) {
                    Text(fileContent!!.take(500), fontSize = 11.sp, color = TextSec, maxLines = 8, overflow = TextOverflow.Ellipsis,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(4.dp)).padding(6.dp))
                } else if (!selectedFile!!.isDirectory) {
                    Text("点击「发送到对话」将此文件内容提供给 AI", fontSize = 12.sp, color = TextHint)
                }

                // Action row
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    if (selectedFile!!.isDirectory) {
                        Button(onClick = { currentDir = selectedFile; selectedFile = null; fileContent = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            modifier = Modifier.height(30.dp)) {
                            Text("打开文件夹", fontSize = 12.sp, color = Color.White)
                        }
                    } else {
                        OutlinedButton(onClick = {
                            loading = true
                            fileContent = readFileContent(selectedFile!!)
                            loading = false
                        }, modifier = Modifier.height(30.dp).padding(end = 6.dp)) {
                            Text("预览内容", fontSize = 12.sp)
                        }
                        Button(onClick = {
                            val content = fileContent ?: readFileContent(selectedFile!!)
                            KnowledgeBaseState.set(selectedFile!!.absolutePath, selectedFile!!.name, content)
                            onBack()
                        }, colors = ButtonDefaults.buttonColors(containerColor = Accent),
                            modifier = Modifier.height(30.dp)) {
                            Text("发送到对话", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
            Divider(color = DividerColor, thickness = 1.dp)
        }

        // File list
        if (entries.isEmpty()) {
            Spacer(Modifier.weight(1f))
            Text("此目录为空", fontSize = 14.sp, color = TextHint, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.weight(1f))
        } else {
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
                entries.forEach { file ->
                    val isDir = file.isDirectory
                    val ext = file.extension.lowercase()
                    val fileIcon = if (isDir) Icons.Default.Folder else fileIcon(file.name)
                    val fileSize = if (!isDir) formatSize(file.length()) else ""
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { selectedFile = file; fileContent = null }
                            .background(if (selectedFile == file) AccentLight else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(fileIcon, null, tint = if (isDir) Orange500 else fileIconColor(ext),
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(file.name, fontSize = 13.sp, color = TextPri,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (fileSize.isNotEmpty()) {
                            Text(fileSize, fontSize = 11.sp, color = TextHint)
                        }
                    }
                }
            }
        }
    }
}

// ---- helpers ----

private fun fileIcon(fileName: String): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico" -> Icons.Default.Image
        "pdf" -> Icons.Default.PictureAsPdf
        "doc", "docx" -> Icons.Default.Description
        "xls", "xlsx", "csv" -> Icons.Default.TableChart
        "ppt", "pptx" -> Icons.Default.Slideshow
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.FolderZip
        "mp3", "wav", "flac", "aac", "ogg" -> Icons.Default.Audiotrack
        "mp4", "avi", "mkv", "mov", "wmv" -> Icons.Default.Videocam
        "exe", "msi", "bat", "cmd" -> Icons.Default.Terminal
        "lnk" -> Icons.Default.OpenInBrowser
        "txt", "md", "log", "json", "xml", "yaml", "yml", "cfg", "ini", "properties" -> Icons.Default.Article
        "kt", "java", "py", "js", "ts", "html", "css", "cpp", "c", "h", "rs", "go" -> Icons.Default.Code
        else -> Icons.Default.InsertDriveFile
    }
}

private fun fileIconColor(ext: String): Color = when (ext) {
    "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico" -> Green500
    "pdf" -> RedCut
    "doc", "docx" -> Blue500
    "xls", "xlsx", "csv" -> Green500
    "ppt", "pptx" -> Orange500
    "zip", "rar", "7z", "tar", "gz" -> Orange500
    "mp3", "wav", "flac", "aac", "ogg" -> Purple500
    "mp4", "avi", "mkv", "mov", "wmv" -> Purple500
    "kt", "java", "py", "js", "ts", "html", "css", "cpp", "c", "h", "rs", "go" -> IndigoLight
    else -> TextSec
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}

private fun readFileContent(file: java.io.File): String? {
    val ext = file.extension.lowercase()
    val textExts = setOf("txt", "md", "log", "json", "xml", "yaml", "yml", "cfg", "ini", "properties",
        "kt", "java", "py", "js", "ts", "html", "css", "cpp", "c", "h", "rs", "go", "csv", "sh", "bat", "cmd", "sql")
    if (ext in textExts && file.length() < 2 * 1024 * 1024) {
        return try {
            file.readText()
        } catch (_: Exception) {
            "[二进制或不可读文件 — ${file.length()} 字节]"
        }
    }
    return "[${file.extension.uppercase()} 文件 — ${formatSize(file.length())}]"
}
