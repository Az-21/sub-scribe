package az21.subscribe.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import az21.subscribe.domain.model.Tag
import az21.subscribe.domain.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives tag management: list, create, edit and delete. */
@HiltViewModel
class TagsViewModel
  @Inject
  constructor(
    private val tagRepository: TagRepository,
  ) : ViewModel() {
    private val dialogs = MutableStateFlow(Dialogs())

    val uiState: StateFlow<TagsUiState> =
      combine(tagRepository.observeTags(), dialogs) { tags, dialogState ->
        TagsUiState(
          isLoading = false,
          tags = tags,
          editor = dialogState.editor,
          pendingDelete = dialogState.pendingDelete,
        )
      }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        TagsUiState(),
      )

    fun startCreate() {
      dialogs.value = dialogs.value.copy(editor = TagEditorState())
    }

    fun startEdit(tag: Tag) {
      dialogs.value = dialogs.value.copy(editor = TagEditorState(id = tag.id, name = tag.name, color = tag.color))
    }

    fun dismissEditor() {
      dialogs.value = dialogs.value.copy(editor = null)
    }

    fun onNameChange(name: String) {
      dialogs.value = dialogs.value.copy(editor = dialogs.value.editor?.copy(name = name))
    }

    fun onColorChange(color: Int?) {
      dialogs.value = dialogs.value.copy(editor = dialogs.value.editor?.copy(color = color))
    }

    fun save() {
      val editor = dialogs.value.editor ?: return
      val name = editor.name.trim()
      if (name.isEmpty()) return
      viewModelScope.launch {
        val id = editor.id
        if (id == null) {
          tagRepository.createTag(name, editor.color)
        } else {
          tagRepository.updateTag(Tag(id = id, name = name, color = editor.color))
        }
        dialogs.value = dialogs.value.copy(editor = null)
      }
    }

    fun requestDelete(tag: Tag) {
      dialogs.value = dialogs.value.copy(pendingDelete = tag)
    }

    fun cancelDelete() {
      dialogs.value = dialogs.value.copy(pendingDelete = null)
    }

    fun confirmDelete() {
      val tag = dialogs.value.pendingDelete ?: return
      dialogs.value = dialogs.value.copy(pendingDelete = null)
      viewModelScope.launch { tagRepository.deleteTag(tag.id) }
    }

    private data class Dialogs(
      val editor: TagEditorState? = null,
      val pendingDelete: Tag? = null,
    )

    private companion object {
      const val STOP_TIMEOUT_MILLIS = 5_000L
    }
  }
