package dev.qmurzik.forpda.domain.model

data class Post(
    val id: String,
    val topicId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val authorRank: String? = null,
    val dateLabel: String,
    val htmlBody: String,
    val attachments: List<Attachment> = emptyList(),
    val quotedPostId: String? = null,
    val isAuthorOnline: Boolean = false,
    val reputation: Int = 0,
)

data class Attachment(
    val id: String,
    val fileName: String,
    val url: String,
    val isImage: Boolean,
    val sizeLabel: String? = null,
)
