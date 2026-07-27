package com.smartagents.desktop

/**
 * Shared state between KnowledgeBasePage and HomeScreen's chat input.
 * When a file is selected in KnowledgeBasePage, it stores the info here.
 * HomeScreen checks and consumes this state to attach the file to the next message.
 */
object KnowledgeBaseState {
    var selectedFilePath: String? = null
    var selectedFileName: String? = null
    var selectedFileContent: String? = null

    fun set(filePath: String, fileName: String, content: String?) {
        selectedFilePath = filePath
        selectedFileName = fileName
        selectedFileContent = content
    }

    /** Pull and clear. Returns triple (path, name, content). */
    fun consume(): Triple<String, String, String?>? {
        val path = selectedFilePath ?: return null
        val name = selectedFileName ?: return null
        val content = selectedFileContent
        selectedFilePath = null
        selectedFileName = null
        selectedFileContent = null
        return Triple(path, name, content)
    }

    fun isSet(): Boolean = selectedFilePath != null
}
