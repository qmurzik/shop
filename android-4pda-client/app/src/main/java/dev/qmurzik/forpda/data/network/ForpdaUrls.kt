package dev.qmurzik.forpda.data.network

/**
 * 4pda's forum runs on IP.Board (IPB), whose URL scheme is a fixed set of query
 * params on a single `index.php` entry point — this hasn't changed across the
 * open-source unofficial clients (e.g. RadiationX/ForPDA, slartus/4pdaClient-plus)
 * that reverse-engineered it. Centralizing the patterns here means the actual
 * hostname/mirror is the only thing likely to need chasing over time.
 *
 * The forum has historically lived on more than one mirror domain (4pda.to /
 * 4pda.ru); [primaryHost] is tried first, with [mirrorHosts] as fallback in
 * [dev.qmurzik.forpda.data.network.NetworkModule]'s client.
 */
object ForpdaUrls {
    const val primaryHost = "4pda.to"
    val mirrorHosts = listOf("4pda.ru")

    private fun base(host: String) = "https://$host/forum/index.php"

    fun forumIndex(host: String = primaryHost) = base(host)

    /** A single forum/sub-forum's topic listing, IPB's `showforum` param. */
    fun showForum(forumId: String, page: Int = 1, host: String = primaryHost) =
        "${base(host)}?showforum=$forumId" + if (page > 1) "&st=${(page - 1) * TOPICS_PER_PAGE}" else ""

    /** A topic's posts, IPB's `showtopic` param; `st` is a post offset, not a page number. */
    fun showTopic(topicId: String, page: Int = 1, host: String = primaryHost) =
        "${base(host)}?showtopic=$topicId" + if (page > 1) "&st=${(page - 1) * POSTS_PER_PAGE}" else ""

    fun userProfile(userId: String, host: String = primaryHost) = "${base(host)}?showuser=$userId"

    fun search(query: String, host: String = primaryHost) =
        "${base(host)}?act=Search&CODE=01&searchterm=${query.encode()}"

    /** GET renders the login form; the real POST target is `act=Login&CODE=01`. */
    fun loginForm(host: String = primaryHost) = "${base(host)}?act=Login"
    fun loginSubmit(host: String = primaryHost) = "${base(host)}?act=Login&CODE=01"

    private fun String.encode() = java.net.URLEncoder.encode(this, "UTF-8")

    private const val TOPICS_PER_PAGE = 30
    private const val POSTS_PER_PAGE = 20
}
