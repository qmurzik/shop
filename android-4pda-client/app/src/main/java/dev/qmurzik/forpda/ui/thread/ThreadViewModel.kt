package dev.qmurzik.forpda.ui.thread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qmurzik.forpda.data.repository.ForumRepository
import dev.qmurzik.forpda.domain.model.Post
import dev.qmurzik.forpda.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThreadUiState(
    val isLoading: Boolean = true,
    val posts: List<Post> = emptyList(),
    val replyDraft: String = "",
    val quotingPost: Post? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val repository: ForumRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val topicId: String = checkNotNull(savedStateHandle[Destination.Thread.ARG_TOPIC_ID])

    private val _uiState = MutableStateFlow(ThreadUiState())
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getPosts(topicId, page = 1)
                .onSuccess { posts -> _uiState.update { it.copy(isLoading = false, posts = posts) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
        }
    }

    fun onQuoteClick(post: Post) {
        _uiState.update {
            it.copy(
                quotingPost = post,
                replyDraft = "[quote=${post.authorName}]${post.htmlBody.replace(Regex("<[^>]*>"), "")}[/quote]\n",
            )
        }
    }

    fun onDraftChange(text: String) {
        _uiState.update { it.copy(replyDraft = text) }
    }

    fun clearQuote() {
        _uiState.update { it.copy(quotingPost = null, replyDraft = "") }
    }

    // TODO: actually POST the reply once the real repository/network layer exists;
    // for now this just clears the composer to demonstrate the flow.
    fun submitReply() {
        if (_uiState.value.replyDraft.isBlank()) return
        clearQuote()
    }
}
