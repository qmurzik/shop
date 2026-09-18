package dev.qmurzik.forpda.data.repository

import dev.qmurzik.forpda.domain.model.Forum
import dev.qmurzik.forpda.domain.model.Post
import dev.qmurzik.forpda.domain.model.Topic
import dev.qmurzik.forpda.domain.model.UserProfile

/**
 * Abstraction over the forum's data source. The real implementation talks to 4pda over
 * HTTP (no official public API exists), so it either parses server-rendered HTML with
 * Jsoup or calls the same internal endpoints the official app/site use. Keeping this
 * behind an interface means the parsing/endpoint details can change without touching
 * any UI or ViewModel code.
 */
interface ForumRepository {
    suspend fun getForumTree(): Result<List<Forum>>
    suspend fun getTopics(forumId: String, page: Int): Result<List<Topic>>
    suspend fun getPosts(topicId: String, page: Int): Result<List<Post>>
    suspend fun getCurrentUser(): Result<UserProfile>
    suspend fun login(username: String, password: String): Result<UserProfile>
    suspend fun search(query: String): Result<List<Topic>>
}
