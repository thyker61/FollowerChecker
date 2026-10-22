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

data class XUser(val id: String, val username: String, val name: String)

enum class Filter(val title: String) {
    NOT_FOLLOWING_ME("Beni Takip Etmeyenler"),
    I_DONT_FOLLOW("Benim Takip Etmediklerim"),
    MUTUAL("Karşılıklı"),
    FOLLOWERS("Takipçiler"),
    FOLLOWING("Takip Edilenler")
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
    var search by remember { mutableStateOf("") }
    val followers = remember { mutableStateListOf<XUser>() }
    val following = remember { mutableStateListOf<XUser>() }

    val followerIds = followers.map { it.id }.toSet()
    val followingIds = following.map { it.id }.toSet()
    val source = when (filter) {
        Filter.NOT_FOLLOWING_ME -> following.filter { it.id !in followerIds }
        Filter.I_DONT_FOLLOW -> followers.filter { it.id !in followingIds }
        Filter.MUTUAL -> following.filter { it.id in followerIds }
        Filter.FOLLOWERS -> followers
        Filter.FOLLOWING -> following
    }.filter { search.isBlank() || it.username.contains(search, true) || it.name.contains(search, true) }

    Scaffold(topBar = { TopAppBar(title = { Text("Follower Checker", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("X takipçi kontrolü", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Hesabını bağladıktan sonra takipçi ve takip edilen listeleri burada karşılaştırılacak.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = { /* Session login/data provider will be connected here. */ }, modifier = Modifier.fillMaxWidth()) {
                Text("X Hesabını Bağla")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Kullanıcı ara") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Filter.entries.forEach { item ->
                TextButton(onClick = { filter = item }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (item == filter) "• ${item.title}" else item.title)
                }
            }
            HorizontalDivider()
            Text("${filter.title}: ${source.size}", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold)
            if (source.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("Henüz veri yok") }
            } else {
                LazyColumn { items(source, key = { it.id }) { user -> ListItem(headlineContent = { Text(user.name) }, supportingContent = { Text("@${user.username}") }) } }
            }
        }
    }
}
