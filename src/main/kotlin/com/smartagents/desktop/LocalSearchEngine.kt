package com.smartagents.desktop

import java.io.File

/**
 * Local file search engine. Parses user intent from natural language,
 * searches the file system, and returns formatted results.
 *
 * Supported patterns:
 *   - "找大于20MB的PDF"       → size filter + extension
 *   - "搜索大于100MB的文件"    → size filter only
 *   - "列出所有.log文件"       → extension filter
 *   - "找最近7天修改的.docx"   → time filter + extension
 *   - "搜索C:\Projects下.java" → path + extension
 *   - "找名字包含report的文件"  → name pattern
 *   - "帮我找文件: 大于20M的图片" → explicit trigger
 */
object LocalSearchEngine {

    // Keywords that indicate a file search intent
    private val searchKeywords = listOf(
        "找", "搜索", "列出", "查找", "检索", "找一下", "帮我找",
        "有多少个", "有哪些", "列出所有", "帮我列"
    )
    private val sizeRegex = Regex("""(大于|超过|>|>=)\s*(\d+)\s*(MB|GB|KB|M|G|K|mb|gb|kb)""")
    private val smallerRegex = Regex("""(小于|低于|<|<=)\s*(\d+)\s*(MB|GB|KB|M|G|K|mb|gb|kb)""")
    private val extRegex = Regex("""\.?([a-zA-Z0-9]{2,6})\s*(文件|格式|类型)?""")
    private val dayRegex = Regex("""(最近|近)\s*(\d+)\s*(天|日|周)""")
    private val nameRegex = Regex("""(名字|名称|文件名).{0,4}[包含叫是](.+?)[的，。,\.\s]""")
    private val pathRegex = Regex("""([A-Za-z]:[\\/][^\s,，]+)""")

    data class SearchParams(
        val minSizeBytes: Long? = null,
        val maxSizeBytes: Long? = null,
        val extensions: Set<String>? = null,
        val modifiedDays: Int? = null,
        val nameContains: String? = null,
        val searchPath: File? = null,
        val rawQuery: String = "",
    )

    data class SearchResult(
        val file: File,
        val size: Long,
        val modified: Long,
    )

    /** Detect if this message is likely a file search intent. */
    fun isSearchIntent(message: String): Boolean {
        val msg = message.trim()
        if (msg.length < 3 || msg.length > 200) return false
        val hasKeyword = searchKeywords.any { msg.contains(it) }
        val hasFilter = sizeRegex.containsMatchIn(msg) || extRegex.containsMatchIn(msg) ||
                dayRegex.containsMatchIn(msg) || nameRegex.containsMatchIn(msg) ||
                pathRegex.containsMatchIn(msg)
        return hasKeyword && hasFilter
    }

    /** Parse search parameters from user message. */
    fun parseParams(message: String): SearchParams {
        var minSize: Long? = null
        var maxSize: Long? = null
        var extensions: Set<String>? = null
        var modifiedDays: Int? = null
        var nameContains: String? = null
        var searchPath: File? = null

        // Size: "大于20MB"
        sizeRegex.find(message)?.let {
            val num = it.groupValues[2].toLongOrNull() ?: 0
            val unit = it.groupValues[3].uppercase()
            val bytes = when (unit) {
                "GB", "G" -> num * 1024 * 1024 * 1024
                "MB", "M" -> num * 1024 * 1024
                "KB", "K" -> num * 1024
                else -> num
            }
            minSize = bytes
        }
        smallerRegex.find(message)?.let {
            val num = it.groupValues[2].toLongOrNull() ?: 0
            val unit = it.groupValues[3].uppercase()
            val bytes = when (unit) {
                "GB", "G" -> num * 1024 * 1024 * 1024
                "MB", "M" -> num * 1024 * 1024
                "KB", "K" -> num * 1024
                else -> num
            }
            maxSize = bytes
        }

        // Extension: extract after size/name context — find the most likely extension
        val extCandidates = extRegex.findAll(message).map { it.groupValues[1].lowercase() }.toList()
        val knownExts = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg",
            "mp4", "avi", "mkv", "mov", "wmv", "mp3", "wav", "flac",
            "zip", "rar", "7z", "tar", "gz",
            "txt", "md", "json", "xml", "yaml", "yml", "csv", "log",
            "java", "kt", "py", "js", "ts", "html", "css", "cpp", "c", "h",
            "exe", "msi", "bat", "cmd", "sh", "sql", "iso")
        val matched = extCandidates.filter { it in knownExts }
        if (matched.isNotEmpty()) extensions = matched.toSet()

        // Time: "最近7天"
        dayRegex.find(message)?.let {
            modifiedDays = it.groupValues[2].toIntOrNull()
        }

        // Name: "名字包含report"
        nameRegex.find(message)?.let {
            nameContains = it.groupValues[1].trim()
        }

        // Path: "C:\Projects"
        pathRegex.find(message)?.let {
            val path = it.groupValues[1]
            val f = File(path)
            if (f.exists() && f.isDirectory) searchPath = f
        }

        return SearchParams(minSize, maxSize, extensions, modifiedDays, nameContains, searchPath, message)
    }

    /** Execute search with given params. Returns limited results (max 100). */
    fun search(params: SearchParams): List<SearchResult> {
        val roots = if (params.searchPath != null) {
            listOf(params.searchPath)
        } else {
            // Default: search common user directories
            val home = System.getProperty("user.home") ?: "C:\\Users\\Default"
            listOf(
                File(home, "Desktop"),
                File(home, "Documents"),
                File(home, "Downloads"),
                File(home, "Pictures"),
                File(home, "Videos"),
                File(home, "Music"),
            ).filter { it.exists() }
        }

        val results = mutableListOf<SearchResult>()
        val now = System.currentTimeMillis()

        for (root in roots) {
            walk(root, params, now, results)
            if (results.size >= 200) break
        }

        return results.take(100)
    }

    private fun walk(dir: File, params: SearchParams, now: Long, results: MutableList<SearchResult>) {
        if (results.size >= 200) return
        val children = try { dir.listFiles() } catch (_: Exception) { return }
        if (children == null) return

        for (f in children) {
            if (results.size >= 200) return
            if (f.isDirectory) {
                // Skip system/hidden dirs
                val name = f.name
                if (name.startsWith(".") || name == "node_modules" || name == "__pycache__" ||
                    name == ".git" || name == "AppData" || name == "Windows" || name == "\$Recycle.Bin") continue
                walk(f, params, now, results)
            } else {
                if (matches(f, params, now)) {
                    results.add(SearchResult(f, f.length(), f.lastModified()))
                }
            }
        }
    }

    private fun matches(f: File, p: SearchParams, now: Long): Boolean {
        if (p.minSizeBytes != null && f.length() < p.minSizeBytes) return false
        if (p.maxSizeBytes != null && f.length() > p.maxSizeBytes) return false
        if (p.extensions != null && f.extension.lowercase() !in p.extensions) return false
        if (p.modifiedDays != null) {
            val ageMs = now - f.lastModified()
            val ageDays = ageMs / (24 * 60 * 60 * 1000)
            if (ageDays > p.modifiedDays) return false
        }
        if (p.nameContains != null && !f.name.contains(p.nameContains, ignoreCase = true)) return false
        return true
    }

    /** Format results as a string for injection into system prompt. */
    fun formatResults(params: SearchParams, results: List<SearchResult>): String {
        if (results.isEmpty()) return "本地搜索未找到匹配的文件。"

        val sb = StringBuilder()
        sb.appendLine("根据用户请求「${params.rawQuery}」，本地搜索找到 ${results.size} 个文件：")
        sb.appendLine()
        sb.appendLine("| 文件名 | 路径 | 大小 | 修改时间 |")
        sb.appendLine("|--------|------|------|----------|")
        for (r in results.take(50)) {
            val name = r.file.name.take(40)
            val path = r.file.parent.take(60)
            val size = formatSize(r.size)
            val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(r.modified))
            sb.appendLine("| $name | $path | $size | $time |")
        }
        if (results.size > 50) {
            sb.appendLine("| ... | 还有 ${results.size - 50} 个结果未列出 | | |")
        }
        return sb.toString()
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.1f GB".format(mb / 1024.0)
    }
}
