package dev.qmurzik.forpda.data.network

import dev.qmurzik.forpda.domain.model.Forum
import dev.qmurzik.forpda.domain.model.Post
import dev.qmurzik.forpda.domain.model.Topic
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject

/**
 * Fetches and parses server-rendered IP.Board pages with Jsoup — 4pda has no
 * official API, so every open-source unofficial client (RadiationX/ForPDA,
 * slartus/4pdaClient-plus) works this way too. URL construction lives in
 * [ForpdaUrls]; the CSS selectors below are still placeholders since IPB themes
 * are customized per-install and need to be checked against 4pda's live markup —
 * kept in one file so a markup change only touches this class.
 */
class ForpdaHtmlParser @Inject constructor(
    private val httpClient: OkHttpClient,
) {
    private fun fetchDocument(url: String): Document =
        httpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            Jsoup.parse(response.body?.string().orEmpty(), url)
        }

    fun parseForumTree(host: String = ForpdaUrls.primaryHost): List<Forum> {
        val doc = fetchDocument(ForpdaUrls.forumIndex(host))
        // TODO: verify against live IPB markup, e.g. doc.select("#forum-list .forumrow")
        return doc.select(".forumrow").map { row ->
            Forum(
                id = row.attr("data-forum-id"),
                title = row.select(".forum-title").text(),
                topicsCount = row.select(".topics-count").text().filter { it.isDigit() }.toIntOrNull() ?: 0,
            )
        }
    }

    fun parseTopics(forumId: String, page: Int = 1, host: String = ForpdaUrls.primaryHost): List<Topic> {
        val doc = fetchDocument(ForpdaUrls.showForum(forumId, page, host))
        return doc.select(".topicrow").map { row ->
            Topic(
                id = row.attr("data-topic-id"),
                forumId = forumId,
                title = row.select(".topic-title").text(),
                authorName = row.select(".topic-author").text(),
                repliesCount = row.select(".replies-count").text().filter { it.isDigit() }.toIntOrNull() ?: 0,
                viewsCount = row.select(".views-count").text().filter { it.isDigit() }.toIntOrNull() ?: 0,
            )
        }
    }

    fun parsePosts(topicId: String, page: Int = 1, host: String = ForpdaUrls.primaryHost): List<Post> {
        val doc = fetchDocument(ForpdaUrls.showTopic(topicId, page, host))
        return doc.select(".postrow").map { row ->
            Post(
                id = row.attr("data-post-id"),
                topicId = topicId,
                authorName = row.select(".post-author").text(),
                dateLabel = row.select(".post-date").text(),
                htmlBody = row.select(".post-body").html(),
            )
        }
    }

    fun parseSearch(query: String, host: String = ForpdaUrls.primaryHost): List<Topic> {
        val doc = fetchDocument(ForpdaUrls.search(query, host))
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
}
