package az21.subscribe.ui.tags

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakeTagDao
import az21.subscribe.data.repository.TagRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagsViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val tagRepository = TagRepositoryImpl(FakeTagDao())

  private fun createViewModel() = TagsViewModel(tagRepository)

  @Test
  fun startCreate_exposesEmptyEditorAndSaveAddsTag() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()

      viewModel.uiState.test {
        awaitLoaded()
        viewModel.startCreate()
        assertEquals("", awaitEditor().editor?.name)

        viewModel.onNameChange("Streaming")
        viewModel.onColorChange(0xFF81C784.toInt())
        viewModel.save()

        val state = await { current -> current.editor == null && current.tags.singleOrNull()?.name == "Streaming" }
        assertEquals(0xFF81C784.toInt(), state.tags.single().color)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun startEdit_prefillsEditorAndSaveUpdates() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val created = tagRepository.createTag("Streaming")

      viewModel.uiState.test {
        await { it.tags.any { tag -> tag.id == created.id } }

        viewModel.startEdit(created)
        assertEquals("Streaming", awaitEditor().editor?.name)

        viewModel.onNameChange("Media")
        viewModel.save()

        val state = await { current -> current.editor == null && current.tags.singleOrNull()?.name == "Media" }
        assertEquals(created.id, state.tags.single().id)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun confirmDelete_removesTagAfterPendingState() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = createViewModel()
      val created = tagRepository.createTag("Streaming")

      viewModel.uiState.test {
        await { it.tags.any { tag -> tag.id == created.id } }

        viewModel.requestDelete(created)
        assertEquals(created, await { it.pendingDelete != null }.pendingDelete)

        viewModel.confirmDelete()

        await { it.pendingDelete == null && it.tags.isEmpty() }
        cancelAndIgnoreRemainingEvents()
      }
    }

  private suspend fun ReceiveTurbine<TagsUiState>.awaitLoaded(): TagsUiState = await { !it.isLoading }

  private suspend fun ReceiveTurbine<TagsUiState>.awaitEditor(): TagsUiState = await { it.editor != null }

  private suspend fun ReceiveTurbine<TagsUiState>.await(predicate: (TagsUiState) -> Boolean): TagsUiState {
    var state = awaitItem()
    while (!predicate(state)) {
      state = awaitItem()
    }
    return state
  }
}
