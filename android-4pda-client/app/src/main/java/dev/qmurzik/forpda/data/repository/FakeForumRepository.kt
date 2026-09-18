package dev.qmurzik.forpda.data.repository

import dev.qmurzik.forpda.domain.model.Forum
import dev.qmurzik.forpda.domain.model.LastPostPreview
import dev.qmurzik.forpda.domain.model.Post
import dev.qmurzik.forpda.domain.model.Topic
import dev.qmurzik.forpda.domain.model.UserProfile
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Placeholder implementation with in-memory sample data, wired up so the UI is
 * demonstrable before the real HTML-parsing / endpoint layer (see
 * [dev.qmurzik.forpda.data.network.ForpdaHtmlParser]) is built out.
 */
class FakeForumRepository @Inject constructor() : ForumRepository {

    private val sampleForums = listOf(
        Forum(
            id = "android",
            title = "Android",
            description = "Обсуждение платформы Android",
            topicsCount = 128_430,
            postsCount = 4_512_900,
            children = listOf(
                Forum(id = "android-help", title = "Помощь новичкам", topicsCount = 41_200),
                Forum(id = "android-dev", title = "Разработка приложений", topicsCount = 18_760),
                Forum(id = "android-mods", title = "Прошивки и модификации", topicsCount = 62_340),
            ),
            lastPost = LastPostPreview("Root на Android 15: инструкция", "user_bot42", "2 мин назад"),
        ),
        Forum(
            id = "iphone",
            title = "iPhone / iOS",
            topicsCount = 76_120,
            postsCount = 2_881_400,
            lastPost = LastPostPreview("Джейлбрейк iOS 18 — статус", "applefan", "14 мин назад"),
        ),
        Forum(
            id = "hardware",
            title = "Железо и гаджеты",
            topicsCount = 33_910,
            postsCount = 990_210,
        ),
    )

    private val sampleTopics = mapOf(
        "android-mods" to listOf(
            Topic(
                id = "t1", forumId = "android-mods",
                title = "[Тема] Кастомные прошивки для Pixel 8",
                authorName = "flash_master",
                repliesCount = 812, viewsCount = 145_320,
                isPinned = true,
                lastPost = LastPostPreview("[Тема] Кастомные прошивки для Pixel 8", "dev_null", "3 мин назад"),
            ),
            Topic(
                id = "t2", forumId = "android-mods",
                title = "Magisk: модули и решение проблем",
                authorName = "root_user",
                repliesCount = 2_431, viewsCount = 501_200,
                hasUnread = true,
                lastPost = LastPostPreview("Magisk: модули и решение проблем", "anon1337", "9 мин назад"),
            ),
            Topic(
                id = "t3", forumId = "android-mods",
                title = "GSI-сборки: список рабочих образов",
                authorName = "gsi_fan",
                repliesCount = 96, viewsCount = 12_400,
                isClosed = true,
            ),
        ),
    )

    private val samplePosts = mapOf(
        "t1" to listOf(
            Post(
                id = "p1", topicId = "t1",
                authorName = "flash_master", authorRank = "Гуру",
                dateLabel = "Сегодня, 12:04",
                htmlBody = "<p>Стартовая тема по кастомным прошивкам для Pixel 8. Пишем баги сюда.</p>",
                isAuthorOnline = true, reputation = 214,
            ),
            Post(
                id = "p2", topicId = "t1",
                authorName = "dev_null", authorRank = "Опытный",
                dateLabel = "Сегодня, 12:31",
                htmlBody = "<p>А камера после прошивки нормально работает?</p>",
                quotedPostId = "p1",
                reputation = 18,
            ),
        ),
    )

    override suspend fun getForumTree(): Result<List<Forum>> {
        delay(250)
        return Result.success(sampleForums)
    }

    override suspend fun getTopics(forumId: String, page: Int): Result<List<Topic>> {
        delay(250)
        return Result.success(sampleTopics[forumId].orEmpty())
    }

    override suspend fun getPosts(topicId: String, page: Int): Result<List<Post>> {
        delay(250)
        return Result.success(samplePosts[topicId].orEmpty())
    }

    override suspend fun getCurrentUser(): Result<UserProfile> {
        return Result.success(UserProfile(id = "0", name = "Гость", isLoggedIn = false))
    }

    override suspend fun login(username: String, password: String): Result<UserProfile> {
        delay(400)
        return Result.success(
            UserProfile(id = "1", name = username, rank = "Пользователь", isLoggedIn = true),
        )
    }

    override suspend fun search(query: String): Result<List<Topic>> {
        delay(250)
        return Result.success(sampleTopics.values.flatten().filter {
            it.title.contains(query, ignoreCase = true)
        })
    }
}
