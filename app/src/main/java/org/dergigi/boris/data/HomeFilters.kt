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
            visible(
                url = article.url,
                title = article.title,
                summary = null,
                archivedKeys = archivedKeys,
                hideArchived = hideArchived,
                hideCompleted = hideCompleted,
                hideNsfw = hideNsfw,
            )
        }
    }

    fun visible(
        url: String?,
        title: String?,
        summary: String?,
        archivedKeys: Set<String>,
        hideArchived: Boolean,
        hideCompleted: Boolean,
        hideNsfw: Boolean,
    ): Boolean {
        if (hideArchived && url?.let { ArchivedArticles.isArchived(it, archivedKeys) } == true) return false
        if (hideCompleted && url?.let(::isComplete) == true) return false
        if (hideNsfw && SensitiveContent.classify(url, title, summary) != null) return false
        return true
    }

    fun isComplete(url: String): Boolean =
        ReadingPositionStore.fraction(url) >= COMPLETE_FRACTION
}
