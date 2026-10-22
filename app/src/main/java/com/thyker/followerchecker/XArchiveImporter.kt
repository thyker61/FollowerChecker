package com.thyker.followerchecker

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ArchiveImportResult(
    val followers: List<XUser>,
    val following: List<XUser>
)

object XArchiveImporter {
    fun import(context: Context, uri: Uri): ArchiveImportResult {
        val name = queryName(context, uri).lowercase()
        return if (name.endsWith(".zip")) importZip(context, uri) else importSingle(context, uri, name)
    }

    private fun importZip(context: Context, uri: Uri): ArchiveImportResult {
        val followers = mutableListOf<XUser>()
        val following = mutableListOf<XUser>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val path = entry.name.lowercase()
                    if (!entry.isDirectory && (path.endsWith("follower.js") || path.endsWith("following.js"))) {
                        val text = zip.bufferedReader().readText()
                        if (path.endsWith("follower.js")) followers += parseUsers(text)
                        if (path.endsWith("following.js")) following += parseUsers(text)
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("Arşiv açılamadı")
        require(followers.isNotEmpty() || following.isNotEmpty()) { "Arşivde follower.js veya following.js bulunamadı" }
        return ArchiveImportResult(followers.distinctBy { it.id }, following.distinctBy { it.id })
    }

    private fun importSingle(context: Context, uri: Uri, name: String): ArchiveImportResult {
        val text = context.contentResolver.openInputStream(uri)?.use {
            BufferedReader(InputStreamReader(it)).readText()
        } ?: error("Dosya açılamadı")
        val users = parseUsers(text)
        return when {
            name.contains("following") -> ArchiveImportResult(emptyList(), users)
            name.contains("follower") -> ArchiveImportResult(users, emptyList())
            else -> error("follower.js, following.js veya X arşiv ZIP dosyasını seç")
        }
    }

    private fun parseUsers(text: String): List<XUser> {
        val json = text.substringAfter('=', text).trim().removeSuffix(";")
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val root = array.optJSONObject(i) ?: continue
                val relation = root.optJSONObject("follower") ?: root.optJSONObject("following") ?: root
                val id = firstNonBlank(relation, "accountId", "userId", "id", "id_str") ?: continue
                val username = firstNonBlank(relation, "userLink", "screenName", "username")
                    ?.substringAfterLast('/')?.substringBefore('?').orEmpty()
                add(XUser(id = id, username = username, name = username.ifBlank { "X kullanıcısı" }))
            }
        }
    }

    private fun firstNonBlank(obj: JSONObject, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> obj.optString(key).takeIf { it.isNotBlank() && it != "null" } }

    private fun queryName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0) ?: "archive"
        }
        return uri.lastPathSegment ?: "archive"
    }
}
