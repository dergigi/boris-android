package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.NostrEventRef
import org.dergigi.boris.data.NostrEventRefs
import org.intellij.markdown.ast.ASTNode

internal fun standaloneEventRef(content: String, node: ASTNode): NostrEventRef? {
    return NostrEventRefs.parseStandalone(content.substring(node.startOffset, node.endOffset))
}
