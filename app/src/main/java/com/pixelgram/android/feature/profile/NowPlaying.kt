package com.pixelgram.android.feature.profile

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.feature.settings.NowPlayingPrefs
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class NowPlayingTrack(val title: String, val artist: String, val cover: String?, val playing: Boolean, val service: String, val profileUrl: String)

object NowPlayingFeed {
    private var cached: Pair<Long, NowPlayingTrack?>? = null

    fun reset() {
        cached = null
    }

    suspend fun current(context: Context): NowPlayingTrack? {
        cached?.takeIf { System.currentTimeMillis() - it.first < CacheMs }?.let { return it.second }
        val service = NowPlayingPrefs.service(context)
        val user = NowPlayingPrefs.username(context)
        if (service == NowPlayingPrefs.None || user.isBlank()) return null
        val track = withContext(Dispatchers.IO) {
            runCatching {
                when (service) {
                    NowPlayingPrefs.LastFm -> lastFm(user, NowPlayingPrefs.apiKey(context))
                    NowPlayingPrefs.StatsFm -> statsFm(user)
                    else -> null
                }
            }.getOrNull()
        }
        cached = System.currentTimeMillis() to track
        return track
    }

    private fun get(url: String): String? {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("User-Agent", "pixelGram")
        if (connection.responseCode != 200) return null
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun lastFm(user: String, key: String): NowPlayingTrack? {
        if (key.isBlank()) return null
        val name = URLEncoder.encode(user, "UTF-8")
        val body = get("https://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=$name&api_key=${URLEncoder.encode(key, "UTF-8")}&format=json&limit=1") ?: return null
        val track = JSONObject(body).getJSONObject("recenttracks").getJSONArray("track").optJSONObject(0) ?: return null
        val playing = track.optJSONObject("@attr")?.optString("nowplaying") == "true"
        val images = track.optJSONArray("image")
        val cover = images?.let { arr -> (arr.length() - 1 downTo 0).asSequence().map { arr.getJSONObject(it).optString("#text") }.firstOrNull { it.isNotBlank() } }
        return NowPlayingTrack(track.optString("name"), track.optJSONObject("artist")?.optString("#text").orEmpty(), cover, playing, "Last.fm", "https://www.last.fm/user/$name/library")
    }

    private fun statsFm(user: String): NowPlayingTrack? {
        val name = URLEncoder.encode(user, "UTF-8")
        val body = get("https://api.stats.fm/api/v1/users/$name/streams/current") ?: return null
        val item = JSONObject(body).optJSONObject("item") ?: return null
        val track = item.optJSONObject("track") ?: return null
        val artists = track.optJSONArray("artists")
        val artist = artists?.let { arr -> (0 until arr.length()).joinToString(", ") { arr.getJSONObject(it).optString("name") } }.orEmpty()
        val cover = track.optJSONArray("albums")?.optJSONObject(0)?.optString("image")
        return NowPlayingTrack(track.optString("name"), artist, cover, item.optBoolean("isPlaying", true), "Stats.fm", "https://stats.fm/$name")
    }

    private const val CacheMs = 30_000L
}

@Composable
fun NowPlayingCard(track: NowPlayingTrack, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(14.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                if (track.cover != null) coil.compose.AsyncImage(model = track.cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp))
                else Icon(Icons.Rounded.GraphicEq, null)
            }
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (track.playing) Icon(Icons.Rounded.GraphicEq, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(track.service, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
