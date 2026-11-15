package com.pixelgram.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import java.io.File

object DocumentViewer {
    var path by mutableStateOf<String?>(null)

    private val Markdown = setOf("md", "markdown")
    private val Code = mapOf("py" to "python", "pyw" to "python")
    private val Plain = setOf("txt", "log", "text")

    fun accepts(name: String): Boolean = name.substringAfterLast('.', "").lowercase().let { it in Markdown || it in Code || it in Plain }

    internal fun kind(name: String): String = name.substringAfterLast('.', "").lowercase().let { ext ->
        when {
            ext in Markdown -> "md"
            ext in Code -> Code.getValue(ext)
            else -> "text"
        }
    }
}

private sealed interface Loaded {
    class Markdown(val source: String, val blocks: List<ASTNode>) : Loaded
    class Lines(val lines: List<AnnotatedString>, val code: Boolean) : Loaded
    data object Failed : Loaded
}

@Composable
fun DocumentViewerHost() {
    DocumentViewer.path?.let { path -> DocumentViewerDialog(path) { DocumentViewer.path = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentViewerDialog(path: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val kind = remember(path) { DocumentViewer.kind(path) }
    val colors = com.pixelgram.android.ui.components.codeColors()
    val loaded by produceState<Loaded?>(null, path) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val text = File(path).readText()
                when (kind) {
                    "md" -> {
                        val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(text)
                        Loaded.Markdown(text, root.children.filter { it.type != MarkdownTokenTypes.EOL && it.type != MarkdownTokenTypes.WHITE_SPACE })
                    }
                    "text" -> Loaded.Lines(text.lines().map(::AnnotatedString), false)
                    else -> {
                        val highlighted = com.pixelgram.android.ui.components.highlightCode(text, kind, colors)
                        val lines = ArrayList<AnnotatedString>()
                        var start = 0
                        while (true) {
                            val end = highlighted.text.indexOf('\n', start)
                            if (end < 0) {
                                lines += highlighted.subSequence(start, highlighted.length)
                                break
                            }
                            lines += highlighted.subSequence(start, end)
                            start = end + 1
                        }
                        Loaded.Lines(lines, true)
                    }
                }
            }.getOrElse { Loaded.Failed }
        }
    }
    val share = {
        runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", File(path))
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("*/*").putExtra(android.content.Intent.EXTRA_STREAM, uri)
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(android.content.Intent.createChooser(intent, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    val external = {
        runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", File(path))
            val type = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(path).extension.lowercase()) ?: "text/plain"
            context.startActivity(
                android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, type)
                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(File(path).name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
                    actions = {
                        IconButton(onClick = { share() }) { Icon(Icons.Rounded.Share, stringResource(R.string.action_share)) }
                        IconButton(onClick = { external() }) { Icon(Icons.Rounded.OpenInNew, null) }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (val doc = loaded) {
                    null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
                    Loaded.Failed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.viewer_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is Loaded.Markdown -> SelectionContainer {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(doc.blocks) { node -> MdBlock(node, doc.source) }
                        }
                    }
                    is Loaded.Lines -> LinesView(doc)
                }
            }
        }
    }
}

@Composable
private fun LinesView(doc: Loaded.Lines) {
    val style = if (doc.code) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyLarge
    val gutter = doc.lines.size.toString().length
    val horizontal = rememberScrollState()
    SelectionContainer {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxSize().then(if (doc.code) Modifier.horizontalScroll(horizontal) else Modifier),
        ) {
            itemsIndexed(doc.lines) { index, line ->
                Row(Modifier.padding(horizontal = 16.dp)) {
                    if (doc.code) {
                        androidx.compose.foundation.text.selection.DisableSelection {
                            Text(
                                (index + 1).toString().padStart(gutter),
                                style = style,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 14.dp),
                            )
                        }
                    }
                    Text(line, style = style, softWrap = !doc.code)
                }
            }
        }
    }
}

@Composable
private fun MdBlock(node: ASTNode, src: String) {
    val type = node.type
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    val body = typography.bodyLarge
    when (type) {
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> Header(node, src, typography.headlineMedium)
        MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> Header(node, src, typography.headlineSmall)
        MarkdownElementTypes.ATX_3 -> Header(node, src, typography.titleLarge)
        MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> Header(node, src, typography.titleMedium)
        MarkdownElementTypes.PARAGRAPH -> Text(inline(node, src), style = body)
        MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> MdList(node, src, type == MarkdownElementTypes.ORDERED_LIST)
        MarkdownElementTypes.CODE_FENCE -> {
            val language = node.children.firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }?.getTextInNode(src)?.toString()?.trim().orEmpty()
            com.pixelgram.android.ui.components.CodeBlock(codeText(node, src), language, typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
        }
        MarkdownElementTypes.CODE_BLOCK -> com.pixelgram.android.ui.components.CodeBlock(codeText(node, src), "", typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
        MarkdownElementTypes.BLOCK_QUOTE -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(colors.primary, RoundedCornerShape(2.dp)))
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                node.children.filter { it.type != MarkdownTokenTypes.BLOCK_QUOTE && it.type != MarkdownTokenTypes.EOL && it.type != MarkdownTokenTypes.WHITE_SPACE }
                    .forEach { MdBlock(it, src) }
            }
        }
        GFMElementTypes.TABLE -> MdTable(node, src)
        MarkdownTokenTypes.HORIZONTAL_RULE -> HorizontalDivider(Modifier.padding(vertical = 6.dp))
        else -> {
            val text = node.getTextInNode(src).toString().trim()
            if (text.isNotEmpty()) Text(text, style = body)
        }
    }
}

@Composable
private fun Header(node: ASTNode, src: String, style: TextStyle) {
    val content = node.children.firstOrNull { it.type == MarkdownTokenTypes.ATX_CONTENT || it.type == MarkdownTokenTypes.SETEXT_CONTENT }
    Text(content?.let { inline(it, src) } ?: AnnotatedString(node.getTextInNode(src).toString().trimStart('#', ' ')), style = style, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun MdList(node: ASTNode, src: String, ordered: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var number = 1
        node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }.forEach { item ->
            val marker = item.children.firstOrNull { it.type == MarkdownTokenTypes.LIST_BULLET || it.type == MarkdownTokenTypes.LIST_NUMBER }
            val checkbox = item.children.firstOrNull { it.type == GFMTokenTypes.CHECK_BOX }?.getTextInNode(src)?.toString()
            val label = when {
                checkbox != null -> if (checkbox.contains('x', ignoreCase = true)) "☑" else "☐"
                ordered -> marker?.getTextInNode(src)?.toString()?.trim() ?: "$number."
                else -> "•"
            }
            number++
            Row {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.children.filter {
                        it.type != MarkdownTokenTypes.LIST_BULLET && it.type != MarkdownTokenTypes.LIST_NUMBER &&
                            it.type != GFMTokenTypes.CHECK_BOX && it.type != MarkdownTokenTypes.EOL && it.type != MarkdownTokenTypes.WHITE_SPACE
                    }.forEach { MdBlock(it, src) }
                }
            }
        }
    }
}

@Composable
private fun MdTable(node: ASTNode, src: String) {
    val rows = node.children.filter { it.type == GFMElementTypes.HEADER || it.type == GFMElementTypes.ROW }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            rows.forEachIndexed { index, row ->
                val header = row.type == GFMElementTypes.HEADER
                Row(Modifier.background(if (header) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)) {
                    row.children.filter { it.type == GFMTokenTypes.CELL }.forEach { cell ->
                        Text(
                            inline(cell, src),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (header) FontWeight.Bold else null),
                            modifier = Modifier.width(CellWidth).padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                if (index < rows.lastIndex) HorizontalDivider()
            }
        }
    }
}

private fun codeText(node: ASTNode, src: String): String =
    node.children.filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT || it.type == MarkdownTokenTypes.CODE_LINE || it.type == MarkdownTokenTypes.EOL }
        .joinToString("") { if (it.type == MarkdownTokenTypes.EOL) "\n" else it.getTextInNode(src).toString() }
        .trim('\n')

@Composable
private fun inline(node: ASTNode, src: String): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    val link = TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline))
    val code = SpanStyle(fontFamily = FontFamily.Monospace, background = colors.surfaceContainerHighest)
    return buildAnnotatedString {
        fun walk(n: ASTNode) {
            when (n.type) {
                MarkdownElementTypes.STRONG -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { n.children.forEach(::walk) }
                MarkdownElementTypes.EMPH -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { n.children.forEach(::walk) }
                GFMElementTypes.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { n.children.forEach(::walk) }
                MarkdownElementTypes.CODE_SPAN -> withStyle(code) { append(n.getTextInNode(src).toString().trim('`')) }
                MarkdownElementTypes.INLINE_LINK -> {
                    val label = n.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
                    val destination = n.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }?.getTextInNode(src)?.toString().orEmpty()
                    withLink(LinkAnnotation.Url(destination, link)) {
                        label?.children?.filter { it.type != MarkdownTokenTypes.LBRACKET && it.type != MarkdownTokenTypes.RBRACKET }?.forEach(::walk)
                            ?: append(destination)
                    }
                }
                MarkdownElementTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK -> {
                    val url = n.getTextInNode(src).toString().trim('<', '>')
                    withLink(LinkAnnotation.Url(url, link)) { append(url) }
                }
                MarkdownTokenTypes.EMPH, MarkdownTokenTypes.BACKTICK, MarkdownTokenTypes.BLOCK_QUOTE -> Unit
                MarkdownTokenTypes.EOL -> append(' ')
                else -> if (n.children.isEmpty()) append(n.getTextInNode(src).toString()) else n.children.forEach(::walk)
            }
        }
        node.children.forEach(::walk)
    }.trimmed()
}

private fun AnnotatedString.trimmed(): AnnotatedString {
    val start = text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: return AnnotatedString("")
    val end = text.indexOfLast { !it.isWhitespace() } + 1
    return subSequence(start, end)
}

private val CellWidth = 200.dp
