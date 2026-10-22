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

data class XUser(val id:String,val username:String,val name:String,val followersCount:Int=0,val followingCount:Int=0,val activityScore:Long=0L)
enum class Filter(val title:String){NOT_FOLLOWING_ME("Beni takip etmeyenler"),I_DONT_FOLLOW("Takip etmediklerim"),MUTUAL("Karşılıklı"),FOLLOWERS("Takipçiler"),FOLLOWING("Takip edilenler")}
enum class SortMode(val title:String){ACTIVE("Aktif"),FOLLOWERS_HIGH("Takipçi ↓"),FOLLOWERS_LOW("Takipçi ↑"),FOLLOWING_HIGH("Takip edilen ↓"),FOLLOWING_LOW("Takip edilen ↑"),NAME("İsim"),USERNAME("Kullanıcı adı")}

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme{FollowerCheckerApp()}}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FollowerCheckerApp(){
 val context=LocalContext.current
 val store=remember{SnapshotStore(context.applicationContext)}
 var filter by remember{mutableStateOf(Filter.NOT_FOLLOWING_ME)}
 var sort by remember{mutableStateOf(SortMode.ACTIVE)}
 var search by remember{mutableStateOf("")}
 var menu by remember{mutableStateOf(false)}
 var status by remember{mutableStateOf("Arşiv seçerek başlayın")}
 var newCount by remember{mutableIntStateOf(0)}
 var lostCount by remember{mutableIntStateOf(0)}
 val followers=remember{mutableStateListOf<XUser>().apply{addAll(store.loadFollowers())}}
 val following=remember{mutableStateListOf<XUser>().apply{addAll(store.loadFollowing())}}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){status="Arşiv okunuyor…";runCatching{XArchiveImporter.import(context,uri)}.onSuccess{r->
   val f=if(r.followers.isNotEmpty())r.followers else followers.toList();val g=if(r.following.isNotEmpty())r.following else following.toList();val d=store.compareAndSave(f);store.saveFollowing(g);followers.clear();followers.addAll(f);following.clear();following.addAll(g);newCount=d.newFollowers.size;lostCount=d.unfollowers.size;status="Hazır • ${followers.size} takipçi • ${following.size} takip edilen"
 }.onFailure{status="Arşiv okunamadı: ${it.message?:"bilinmeyen hata"}"}}}
 val followerIds=followers.map{it.id}.toSet();val followingIds=following.map{it.id}.toSet()
 val base=when(filter){Filter.NOT_FOLLOWING_ME->following.filter{it.id !in followerIds};Filter.I_DONT_FOLLOW->followers.filter{it.id !in followingIds};Filter.MUTUAL->following.filter{it.id in followerIds};Filter.FOLLOWERS->followers;Filter.FOLLOWING->following}.filter{search.isBlank()||it.username.contains(search,true)||it.name.contains(search,true)}
 val users=when(sort){SortMode.ACTIVE->base.sortedByDescending{it.activityScore};SortMode.FOLLOWERS_HIGH->base.sortedByDescending{it.followersCount};SortMode.FOLLOWERS_LOW->base.sortedBy{it.followersCount};SortMode.FOLLOWING_HIGH->base.sortedByDescending{it.followingCount};SortMode.FOLLOWING_LOW->base.sortedBy{it.followingCount};SortMode.NAME->base.sortedBy{it.name.lowercase()};SortMode.USERNAME->base.sortedBy{it.username.lowercase()}}
 Scaffold(topBar={TopAppBar(title={Text("Follower Checker",fontWeight=FontWeight.Bold)},actions={Box{TextButton(onClick={menu=true}){Text("Sırala")};DropdownMenu(menu,{menu=false}){SortMode.entries.forEach{m->DropdownMenuItem({Text(if(m==sort)"✓ ${m.title}" else m.title)},{sort=m;menu=false})}}}})}){pad->
  Column(Modifier.padding(pad).fillMaxSize().padding(horizontal=16.dp)){
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("${followers.size}",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Takipçi",style=MaterialTheme.typography.labelMedium)};Column(horizontalAlignment=Alignment.End){Text("${following.size}",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("Takip edilen",style=MaterialTheme.typography.labelMedium)}};Spacer(Modifier.height(8.dp));Text("+$newCount yeni   −$lostCount çıkan",style=MaterialTheme.typography.bodyMedium)}}
   Spacer(Modifier.height(12.dp));Button({picker.launch(arrayOf("application/zip","application/x-zip-compressed","application/javascript","text/javascript","text/plain"))},Modifier.fillMaxWidth()){Text("X arşivini seç")};Text(status,style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(vertical=6.dp))
   TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,"https://x.com/i/flow/login".toUri()))}},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("X'i tarayıcıda aç")}
   OutlinedTextField(search,{search=it},label={Text("Ara")},singleLine=true,modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(8.dp))
   SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){listOf(Filter.NOT_FOLLOWING_ME,Filter.I_DONT_FOLLOW,Filter.MUTUAL).forEachIndexed{i,f->SegmentedButton(selected=filter==f,onClick={filter=f},shape=SegmentedButtonDefaults.itemShape(i,3)){Text(when(f){Filter.NOT_FOLLOWING_ME->"Beni etmeyen";Filter.I_DONT_FOLLOW->"Etmediklerim";else->"Karşılıklı"})}}}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){TextButton({filter=Filter.FOLLOWERS}){Text("Takipçiler")};TextButton({filter=Filter.FOLLOWING}){Text("Takip edilenler")}}
   HorizontalDivider();Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(filter.title,fontWeight=FontWeight.SemiBold);Text("${users.size}",fontWeight=FontWeight.Bold)}
   if(users.isEmpty())Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Text(if(followers.isEmpty()&&following.isEmpty())"Önce X arşivini seç" else "Sonuç yok",style=MaterialTheme.typography.bodyMedium)}else LazyColumn{items(users,key={it.id}){u->ListItem(headlineContent={Text(u.name.ifBlank{u.username.ifBlank{"X kullanıcısı"}})},supportingContent={Text(if(u.username.isBlank())"ID ${u.id}" else "@${u.username}")});HorizontalDivider()}}
  }
 }
}
