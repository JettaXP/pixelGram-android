package com.pixelgram.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

enum class TagKind { Owner, Admin, Member }

data class MemberTag(val text: String, val kind: TagKind)

data class TagRequest(val chatId: Long, val userId: Long, val name: String, val chatTitle: String, val tag: MemberTag, val canEdit: Boolean)

object TagSheets {
    var current by mutableStateOf<TagRequest?>(null)
}

@Composable
fun tagColor(kind: TagKind): Color = when (kind) {
    TagKind.Owner -> Color(0xFFA77BFF)
    TagKind.Admin -> Color(0xFF5FC35A)
    TagKind.Member -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun tagText(tag: MemberTag): String = tag.text.ifBlank {
    when (tag.kind) {
        TagKind.Owner -> stringResource(R.string.profile_owner)
        TagKind.Admin -> stringResource(R.string.profile_admin)
        TagKind.Member -> ""
    }
}

@Composable
fun MemberTagLabel(tag: MemberTag, modifier: Modifier = Modifier, small: Boolean = false, onClick: (() -> Unit)? = null) {
    val text = tagText(tag)
    if (text.isBlank()) return
    val color = tagColor(tag.kind)
    val style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    val click = if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick) else Modifier
    if (tag.kind == TagKind.Member) {
        Text(text, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.then(click).padding(horizontal = 4.dp, vertical = 2.dp))
    } else {
        Text(
            text,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.then(click).clip(CircleShape).background(color.copy(alpha = 0.16f)).padding(horizontal = if (small) 7.dp else 10.dp, vertical = if (small) 1.dp else 3.dp),
        )
    }
}

fun memberTag(member: TdApi.ChatMember): MemberTag? = when (member.status) {
    is TdApi.ChatMemberStatusCreator -> MemberTag(member.tag, TagKind.Owner)
    is TdApi.ChatMemberStatusAdministrator -> MemberTag(member.tag, TagKind.Admin)
    else -> member.tag.takeIf { it.isNotBlank() }?.let { MemberTag(it, TagKind.Member) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSheetHost() {
    val request = TagSheets.current ?: return
    val graph = LocalGraph.current
    val scope = rememberCoroutineScope()
    val dismiss = { TagSheets.current = null }
    val color = tagColor(request.tag.kind)
    val shown = tagText(request.tag)
    var editing by remember(request) { mutableStateOf(false) }
    var value by remember(request) { mutableStateOf(request.tag.text) }
    var error by remember(request) { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp),
        ) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Badge, null, Modifier.size(44.dp), tint = color)
            }
            Text(
                stringResource(when (request.tag.kind) { TagKind.Owner -> R.string.tag_owner_title; TagKind.Admin -> R.string.tag_admin_title; TagKind.Member -> R.string.tag_member_title }),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (!editing) {
                val pre = stringResource(when (request.tag.kind) { TagKind.Owner -> R.string.tag_explain_owner_pre; TagKind.Admin -> R.string.tag_explain_admin_pre; TagKind.Member -> R.string.tag_explain_member_pre })
                val post = stringResource(when (request.tag.kind) { TagKind.Owner -> R.string.tag_explain_owner_post; TagKind.Admin -> R.string.tag_explain_admin_post; TagKind.Member -> R.string.tag_explain_member_post }, request.name, request.chatTitle)
                Text(
                    buildAnnotatedString {
                        append(pre)
                        append(" ")
                        withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold, background = if (request.tag.kind == TagKind.Member) Color.Transparent else color.copy(alpha = 0.16f))) { append(" $shown ") }
                        append(" ")
                        append(post)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(16) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    supportingText = { Text(error ?: stringResource(R.string.tag_edit_hint)) },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (request.canEdit) {
                if (editing) {
                    Button(onClick = {
                        scope.launch {
                            runCatching { graph.td.send(TdApi.SetChatMemberTag(request.chatId, request.userId, value.trim())) }
                                .onSuccess {
                                    TagSheets.current = null
                                    MemberTagsCache.invalidate(request.chatId)
                                }
                                .onFailure { error = it.message }
                        }
                    }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.tag_save)) }
                    TextButton(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) }
                } else {
                    Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.tag_edit)) }
                    TextButton(onClick = dismiss) { Text(stringResource(R.string.tag_understood)) }
                }
            } else {
                Button(onClick = dismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.tag_understood)) }
            }
        }
    }
}

object MemberTagsCache {
    private val admins = HashMap<Long, Map<Long, MemberTag>>()
    var version by mutableStateOf(0)

    fun invalidate(chatId: Long) {
        admins.remove(chatId)
        version++
    }

    suspend fun admins(graph: com.pixelgram.android.AppGraph, chatId: Long): Map<Long, MemberTag> {
        admins[chatId]?.let { return it }
        val list = runCatching { graph.td.send(TdApi.GetChatAdministrators(chatId)).administrators.toList() }.getOrDefault(emptyList())
        val map = list.associate { it.userId to MemberTag(it.customTitle, if (it.isOwner) TagKind.Owner else TagKind.Admin) }
        admins[chatId] = map
        return map
    }
}
