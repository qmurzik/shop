package dev.qmurzik.forpda.ui.forums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qmurzik.forpda.data.repository.ForumRepository
import dev.qmurzik.forpda.domain.model.Forum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForumsUiState(
    val isLoading: Boolean = true,
    val forums: List<Forum> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class ForumsViewModel @Inject constructor(
    private val repository: ForumRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForumsUiState())
    val uiState: StateFlow<ForumsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getForumTree()
                .onSuccess { forums -> _uiState.value = ForumsUiState(isLoading = false, forums = forums) }
                .onFailure { error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message) }
        }
    }
}
