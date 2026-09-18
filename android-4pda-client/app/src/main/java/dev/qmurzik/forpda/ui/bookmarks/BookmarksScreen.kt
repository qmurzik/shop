package dev.qmurzik.forpda.ui.bookmarks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Bookmarked/subscribed topics. 4pda only offers "subscribe by e-mail"; a local,
 * synced bookmarks list with unread-post badges is one of the improvements over
 * the original this client aims for. Backed by Room once the real data layer lands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Закладки") }) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text("Пока пусто — добавляйте темы в закладки из меню темы")
        }
    }
}
