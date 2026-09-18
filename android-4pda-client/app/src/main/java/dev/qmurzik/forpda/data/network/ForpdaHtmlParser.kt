package dev.qmurzik.forpda.data.network

import dev.qmurzik.forpda.domain.model.Forum
import dev.qmurzik.forpda.domain.model.Post
import dev.qmurzik.forpda.domain.model.Topic
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject

/**
 * Fetches and parses server-rendered forum pages with Jsoup, since there is no
 * official 4pda API to call instead. CSS selectors below are placeholders — they
 * need to be filled in against the live markup before this becomes the real
 * [dev.qmurzik.forpda.data.repository.ForumRepository] implementation, and kept in
 * one place so future markup changes only touch this file.
 */
class ForpdaHtmlParser @Inject constructor(
    private val httpClient: OkHttpClient,
) {
    private fun fetchDocument(url: String) = httpClient.newCall(Request.Builder().url(url).build())
        .execute().use { response ->
            val body = response.body?.string().orEmpty()
            Jsoup.parse(body, url)
        }

    fun parseForumTree(url: String): List<Forum> {
        val doc = fetchDocument(url)
        // TODO: replace with real selectors, e.g. doc.select(".forumrow")
        return doc.select(".forumrow").map { row ->
            Forum(
                id = row.attr("data-forum-id"),
                title = row.select(".forum-title").text(),
                topicsCount = row.select(".topics-count").text().filter { it.isDigit() }.toIntOrNull() ?: 0,
            )
        }
    }

    fun parseTopics(forumUrl: String): List<Topic> {
        val doc = fetchDocument(forumUrl)
        return doc.select(".topicrow").map { row ->
            Topic(
                id = row.attr("data-topic-id"),
                forumId = row.attr("data-forum-id"),
                title = row.select(".topic-title").text(),
                authorName = row.select(".topic-author").text(),
                repliesCount = row.select(".replies-count").text().filter { it.isDigit() }.toIntOrNull() ?: 0,
                viewsCount = row.select(".views-count").text().filter { it.isDigit() }.toIntOrNull() ?: 0,
            )
        }
    }

    fun parsePosts(topicUrl: String): List<Post> {
        val doc = fetchDocument(topicUrl)
        return doc.select(".postrow").map { row ->
            Post(
                id = row.attr("data-post-id"),
                topicId = row.attr("data-topic-id"),
                authorName = row.select(".post-author").text(),
                dateLabel = row.select(".post-date").text(),
                htmlBody = row.select(".post-body").html(),
            )
        }
    }
}
