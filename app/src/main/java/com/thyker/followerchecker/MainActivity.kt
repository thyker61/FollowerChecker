package com.thyker.followerchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class XUser(
    val id: String,
    val username: String,
    val name: String,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val activityScore: Long = 0L
)

enum class Filter(val title: String) {
    NOT_FOLLOWING_ME("Beni Takip Etmeyenler"), I_DONT_FOLLOW("Benim Takip Etmediklerim"),
    MUTUAL("Karşılıklı"), FOLLOWERS("Takipçiler"), FOLLOWING("Takip Edilenler")
}

enum class SortMode(val title: String) {
    ACTIVE("Aktif kullanıcı"), FOLLOWERS_HIGH("Takipçi: yüksekten düşüğe"),
    FOLLOWERS_LOW("Takipçi: düşükten yükseğe"), FOLLOWING_HIGH("Takip edilen: yüksekten düşüğe"),
    FOLLOWING_LOW("Takip edilen: düşükten yükseğe"), NAME("İsim"), USERNAME("Kullanıcı adı")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FollowerCheckerApp() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowerCheckerApp() {
    var filter by remember { mutableStateOf(Filter.NOT_FOLLOWING_ME) }
    var sortMode by remember { mutableStateOf(SortMode.ACTIVE) }
    var search by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    val followers = remember { mutableStateListOf<XUser>() }
    val following = remember { mutableStateListOf<XUser>() }

    val followerIds = followers.map { it.id }.toSet()
    val followingIds = following.map { it.id }.toSet()
    val filtered = when (filter) {
        Filter.NOT_FOLLOWING_ME -> following.filter { it.id !in followerIds }
        Filter.I_DONT_FOLLOW -> followers.filter { it.id !in followingIds }
        Filter.MUTUAL -> following.filter { it.id in followerIds }
        Filter.FOLLOWERS -> followers
        Filter.FOLLOWING -> following
    }.filter { search.isBlank() || it.username.contains(search, true) || it.name.contains(search, true) }

    val source = when (sortMode) {
        SortMode.ACTIVE -> filtered.sortedByDescending { it.activityScore }
        SortMode.FOLLOWERS_HIGH -> filtered.sortedByDescending { it.followersCount }
        SortMode.FOLLOWERS_LOW -> filtered.sortedBy { it.followersCount }
        SortMode.FOLLOWING_HIGH -> filtered.sortedByDescending { it.followingCount }
        SortMode.FOLLOWING_LOW -> filtered.sortedBy { it.followingCount }
        SortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
        SortMode.USERNAME -> filtered.sortedBy { it.username.lowercase() }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Follower Checker", fontWeight = FontWeight.Bold) },
            actions = {
                Box {
                    TextButton(onClick = { sortMenu = true }) { Text("Sırala") }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortMode.entries.forEach { mode ->
                            DropdownMenuItem(text = { Text(if (mode == sortMode) "✓ ${mode.title}" else mode.title) }, onClick = {
                                sortMode = mode; sortMenu = false
                            })
                        }
                    }
                }
            }
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("X takipçi kontrolü", style = MaterialTheme.typography.titleMedium)
            Text("Takipçi: ${followers.size}   Takip edilen: ${following.size}")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("X Hesabını Bağla") }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(search, { search = it }, label = { Text("Kullanıcı ara") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Filter.entries.forEach { item ->
                TextButton(onClick = { filter = item }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (item == filter) "• ${item.title}" else item.title)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${filter.title}: ${source.size}", fontWeight = FontWeight.SemiBold)
                Text(sortMode.title, style = MaterialTheme.typography.labelMedium)
            }
            if (source.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Henüz veri yok") }
            } else {
                LazyColumn {
                    items(source, key = { it.id }) { user ->
                        ListItem(
                            headlineContent = { Text(user.name) },
                            supportingContent = { Text("@${user.username}  •  ${user.followersCount} takipçi  •  ${user.followingCount} takip") }
                        )
                    }
                }
            }
        }
    }
}
