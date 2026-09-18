package dev.qmurzik.forpda.domain.model

data class Topic(
    val id: String,
    val forumId: String,
    val title: String,
    val authorName: String,
    val repliesCount: Int,
    val viewsCount: Int,
    val isPinned: Boolean = false,
    val isClosed: Boolean = false,
    val hasUnread: Boolean = false,
    val lastPost: LastPostPreview? = null,
)
