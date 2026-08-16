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
    if (node.type == MarkdownElementTypes.IMAGE) return markdownImageDestination(content, node)
    val images = node.children.filter { it.type == MarkdownElementTypes.IMAGE }
    if (images.size != 1) return null
    val leftover = node.children.any { child ->
        child.type != MarkdownElementTypes.IMAGE &&
            content.substring(child.startOffset, child.endOffset).isNotBlank()
    }
    if (leftover) return null
    return markdownImageDestination(content, images.single())
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
