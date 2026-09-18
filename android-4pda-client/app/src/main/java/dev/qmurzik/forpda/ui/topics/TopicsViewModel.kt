package dev.qmurzik.forpda.ui.topics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qmurzik.forpda.data.repository.ForumRepository
import dev.qmurzik.forpda.domain.model.Topic
import dev.qmurzik.forpda.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopicsUiState(
    val isLoading: Boolean = true,
    val topics: List<Topic> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class TopicsViewModel @Inject constructor(
    private val repository: ForumRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val forumId: String = checkNotNull(savedStateHandle[Destination.Topics.ARG_FORUM_ID])

    private val _uiState = MutableStateFlow(TopicsUiState())
    val uiState: StateFlow<TopicsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getTopics(forumId, page = 1)
                .onSuccess { topics -> _uiState.value = TopicsUiState(isLoading = false, topics = topics) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message) }
        }
    }
}
