package com.pixelgram.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pixelgram.android.R
import com.pixelgram.android.td.Seen
import com.pixelgram.android.td.UserInfo

@Composable
fun statusLine(user: UserInfo): String = when {
    user.deleted -> stringResource(R.string.chat_seen_long_ago)
    user.support -> stringResource(R.string.chat_support)
    user.bot -> stringResource(R.string.chat_bot)
    user.seen == Seen.Online || user.online -> stringResource(R.string.chat_online)
    user.seen == Seen.Offline && user.lastSeen > 0 -> stringResource(R.string.chat_last_seen, com.pixelgram.android.util.chatListTime(user.lastSeen), com.pixelgram.android.util.messageTime(user.lastSeen))
    user.seen == Seen.Week -> stringResource(R.string.chat_seen_week)
    user.seen == Seen.Month -> stringResource(R.string.chat_seen_month)
    user.seen == Seen.LongAgo -> stringResource(R.string.chat_seen_long_ago)
    else -> stringResource(R.string.chat_last_seen_recently)
}
