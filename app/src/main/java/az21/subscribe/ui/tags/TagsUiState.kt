package az21.subscribe.ui.tags

import az21.subscribe.domain.model.Tag
import java.util.UUID

/** State of the tag create/edit dialog. [id] is null when creating. */
data class TagEditorState(
  val id: UUID? = null,
  val name: String = "",
  val color: Int? = null,
)

/** Immutable state for the tag management screen. */
data class TagsUiState(
  val isLoading: Boolean = true,
  val tags: List<Tag> = emptyList(),
  val editor: TagEditorState? = null,
  val pendingDelete: Tag? = null,
)
