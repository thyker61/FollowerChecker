package com.thyker.followerchecker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

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
    val context = LocalContext.current
    val snapshotStore = remember { SnapshotStore(context.applicationContext) }
    var filter by remember { mutableStateOf(Filter.NOT_FOLLOWING_ME) }
    var sortMode by remember { mutableStateOf(SortMode.ACTIVE) }
    var search by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf("X arşiv ZIP dosyanı seçebilirsin.") }
    var newFollowersCount by remember { mutableIntStateOf(0) }
    var unfollowersCount by remember { mutableIntStateOf(0) }
    val followers = remember { mutableStateListOf<XUser>().apply { addAll(snapshotStore.loadFollowers()) } }
    val following = remember { mutableStateListOf<XUser>().apply { addAll(snapshotStore.loadFollowing()) } }

    val archivePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { XArchiveImporter.import(context, uri) }
                .onSuccess { result ->
                    val importedFollowers = if (result.followers.isNotEmpty()) result.followers else followers.toList()
                    val importedFollowing = if (result.following.isNotEmpty()) result.following else following.toList()
                    val diff = snapshotStore.compareAndSave(importedFollowers)
                    snapshotStore.saveFollowing(importedFollowing)
                    followers.clear(); followers.addAll(importedFollowers)
                    following.clear(); following.addAll(importedFollowing)
                    newFollowersCount = diff.newFollowers.size
                    unfollowersCount = diff.unfollowers.size
                    importStatus = "Arşiv yüklendi: ${followers.size} takipçi, ${following.size} takip edilen."
                }
                .onFailure { error ->
                    importStatus = "Arşiv okunamadı: ${error.message ?: "bilinmeyen hata"}"
                }
        }
    }

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
            Text("Yeni takipçi: $newFollowersCount   Takipten çıkan: $unfollowersCount")
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { archivePicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/javascript", "text/javascript", "text/plain")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("X Arşivini Seç") }
            Text(importStatus, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://x.com/i/flow/login".toUri()))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("X Oturumunu Tarayıcıda Aç") }
            Text("X giriş sayfası artık uygulama içi WebView yerine telefonundaki tarayıcıda açılır.", style = MaterialTheme.typography.bodySmall)
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
                            supportingContent = { Text(if (user.username.isBlank()) "X kullanıcı ID: ${user.id}" else "@${user.username}") }
                        )
                    }
                }
            }
        }
    }
}
