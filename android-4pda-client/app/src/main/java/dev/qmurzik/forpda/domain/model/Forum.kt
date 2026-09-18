package dev.qmurzik.forpda.domain.model

/**
 * A node in the forum's category tree (section, sub-forum, or leaf forum with topics).
 */
data class Forum(
    val id: String,
    val title: String,
    val description: String? = null,
    val topicsCount: Int = 0,
    val postsCount: Int = 0,
    val children: List<Forum> = emptyList(),
    val lastPost: LastPostPreview? = null,
)

data class LastPostPreview(
    val topicTitle: String,
    val authorName: String,
    val dateLabel: String,
)
