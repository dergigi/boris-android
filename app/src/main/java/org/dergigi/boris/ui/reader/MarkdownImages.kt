package org.dergigi.boris.ui.reader

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode

internal fun markdownImageDestination(content: String, node: ASTNode): String? {
    val dest = node.findType(MarkdownElementTypes.LINK_DESTINATION) ?: return null
    return content.substring(dest.startOffset, dest.endOffset)
        .trim()
        .trim('<', '>')
        .trim()
        .trim('"', '\'')
        .takeIf { it.isNotBlank() }
}

internal fun standaloneMarkdownImageUrl(content: String, node: ASTNode): String? {
    return standaloneMarkdownImageUrls(content, node).singleOrNull()
}

internal fun standaloneMarkdownImageUrls(content: String, node: ASTNode): List<String> {
    if (node.type == MarkdownElementTypes.IMAGE) {
        return markdownImageDestination(content, node)?.let(::listOf).orEmpty()
    }
    val images = descendantImages(node)
    if (images.isEmpty() || hasLeftoverText(node, content)) return emptyList()
    val urls = images.map { markdownImageDestination(content, it) }
    if (urls.any { it == null }) return emptyList()
    return urls.filterNotNull()
}

private fun descendantImages(node: ASTNode): List<ASTNode> {
    if (node.type == MarkdownElementTypes.IMAGE) return listOf(node)
    return node.children.flatMap(::descendantImages)
}

private val syntaxNames = setOf("[", "]", "(", ")", "!", "EOL", "WHITE_SPACE")

private fun hasLeftoverText(node: ASTNode, content: String): Boolean {
    when (node.type) {
        MarkdownElementTypes.IMAGE,
        MarkdownElementTypes.LINK_DESTINATION,
        -> return false
    }
    if (node.type.name in syntaxNames) return false
    if (node.children.isEmpty()) {
        return content.substring(node.startOffset, node.endOffset).isNotBlank()
    }
    return node.children.any { hasLeftoverText(it, content) }
}

private fun ASTNode.findType(type: IElementType): ASTNode? {
    if (this.type == type) return this
    for (child in children) {
        child.findType(type)?.let { return it }
    }
    return null
}

internal fun markdownImageBox(
    container: Size,
    intrinsic: Size,
    fullWidth: Boolean,
    maxHeightPx: Float,
): Size {
    val fallback = Size(180f, 180f)
    val hasContainer = !container.isUnspecified && container.width > 0f
    val hasIntrinsic = !intrinsic.isUnspecified && intrinsic.width > 0f && intrinsic.height > 0f
    if (!hasContainer && !hasIntrinsic) return fallback
    if (!hasIntrinsic) {
        val width = if (hasContainer) container.width else fallback.width
        return Size(width, fallback.height)
    }
    val srcW = intrinsic.width
    val srcH = intrinsic.height
    if (fullWidth) {
        val width = if (hasContainer) container.width else srcW
        return Size(width, srcH * (width / srcW))
    }
    val maxWidth = if (hasContainer) container.width else srcW
    val scale = minOf(1f, maxWidth / srcW, maxHeightPx / srcH)
    return Size(srcW * scale, srcH * scale)
}
