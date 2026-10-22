package com.thyker.followerchecker

import android.content.Context
import android.net.Uri
import java.util.zip.ZipInputStream

/**
 * Offline fallback for X's official data archive.
 * Reads follower.js and following.js without sending archive contents anywhere.
 */
object ArchiveImporter {
    data class Result(val followers: List<XUser>, val following: List<XUser>)

    fun importZip(context: Context, uri: Uri): Result {
        val followers = linkedMapOf<String, XUser>()
        val following = linkedMapOf<String, XUser>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.substringAfterLast('/').lowercase()
                    if (!entry.isDirectory && (name == "follower.js" || name == "following.js")) {
                        val text = zip.readBytes().toString(Charsets.UTF_8)
                        val target = if (name == "follower.js") followers else following
                        parseUsers(text).forEach { target[it.id] = it }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Arşiv açılamadı")
        if (followers.isEmpty() && following.isEmpty()) error("Arşivde follower.js/following.js bulunamadı")
        return Result(followers.values.toList(), following.values.toList())
    }

    private fun parseUsers(text: String): List<XUser> {
        val idRegex = Regex("\\\"accountId\\\"\\s*:\\s*\\\"(\\d+)\\\"")
        val usernameRegex = Regex("\\\"userLink\\\"\\s*:\\s*\\\"https?://(?:www\\.)?(?:twitter|x)\\.com/([^\\\"/?]+)")
        val ids = idRegex.findAll(text).map { it.groupValues[1] }.toList()
        val usernames = usernameRegex.findAll(text).map { it.groupValues[1] }.toList()
        return ids.mapIndexed { index, id ->
            val username = usernames.getOrNull(index).orEmpty()
            XUser(id = id, username = username, name = if (username.isBlank()) id else username)
        }
    }
}
