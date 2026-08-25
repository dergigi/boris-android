package org.dergigi.boris.data

/**
 * Home-list visibility for archived, completed, and NSFW articles (#102).
 * Completed uses the same 95% threshold as the green progress state.
 */
object HomeFilters {
    const val COMPLETE_FRACTION = 0.95f

    fun visible(
        articles: List<HighlightedArticle>,
        archivedKeys: Set<String>,
        hideArchived: Boolean,
        hideCompleted: Boolean,
        hideNsfw: Boolean,
    ): List<HighlightedArticle> {
        return articles.filter { article ->
            if (hideArchived && ArchivedArticles.isArchived(article.url, archivedKeys)) return@filter false
            if (hideCompleted && isComplete(article.url)) return@filter false
            if (hideNsfw && SensitiveContent.classify(article) != null) return@filter false
            true
        }
    }

    fun isComplete(url: String): Boolean =
        ReadingPositionStore.fraction(url) >= COMPLETE_FRACTION
}
