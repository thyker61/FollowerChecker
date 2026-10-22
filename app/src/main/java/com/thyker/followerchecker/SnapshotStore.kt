package com.thyker.followerchecker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SnapshotDiff(
    val newFollowers: List<XUser>,
    val unfollowers: List<XUser>
)

class SnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("follower_snapshots", Context.MODE_PRIVATE)

    fun compareAndSave(currentFollowers: List<XUser>): SnapshotDiff {
        val previous = loadUsers("followers")
        val previousById = previous.associateBy { it.id }
        val currentById = currentFollowers.associateBy { it.id }

        val newFollowers = currentFollowers.filter { it.id !in previousById }
        val unfollowers = previous.filter { it.id !in currentById }

        prefs.edit()
            .putString("followers", encode(currentFollowers))
            .putLong("last_snapshot_at", System.currentTimeMillis())
            .apply()

        return SnapshotDiff(newFollowers, unfollowers)
    }

    fun saveFollowing(users: List<XUser>) {
        prefs.edit().putString("following", encode(users)).apply()
    }

    fun loadFollowers(): List<XUser> = loadUsers("followers")
    fun loadFollowing(): List<XUser> = loadUsers("following")
    fun lastSnapshotAt(): Long = prefs.getLong("last_snapshot_at", 0L)

    private fun encode(users: List<XUser>): String {
        val array = JSONArray()
        users.forEach { user ->
            array.put(JSONObject().apply {
                put("id", user.id)
                put("username", user.username)
                put("name", user.name)
                put("followersCount", user.followersCount)
                put("followingCount", user.followingCount)
                put("activityScore", user.activityScore)
            })
        }
        return array.toString()
    }

    private fun loadUsers(key: String): List<XUser> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        XUser(
                            id = item.getString("id"),
                            username = item.optString("username"),
                            name = item.optString("name"),
                            followersCount = item.optInt("followersCount"),
                            followingCount = item.optInt("followingCount"),
                            activityScore = item.optLong("activityScore")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
