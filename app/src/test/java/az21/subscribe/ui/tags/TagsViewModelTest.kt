package az21.subscribe.ui.tags

import az21.subscribe.MainDispatcherRule
import az21.subscribe.data.fake.FakeTagDao
import az21.subscribe.data.repository.TagRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagsViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val tagRepository = TagRepositoryImpl(FakeTagDao())

  @Test
  fun createTag_addsItToList() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = TagsViewModel(tagRepository)
      collectUiState(viewModel)
      viewModel.startCreate()
      viewModel.onNameChange("Streaming")
      viewModel.onColorChange(0xFF81C784.toInt())

      viewModel.save()
      advanceUntilIdle()

      val tag = tagRepository.observeTags().first().single()
      assertEquals("Streaming", tag.name)
      assertEquals(0xFF81C784.toInt(), tag.color)
    }

  @Test
  fun startEdit_prefillsEditorAndSaveUpdates() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = TagsViewModel(tagRepository)
      collectUiState(viewModel)
      val created = tagRepository.createTag("Streaming")

      viewModel.startEdit(created)
      advanceUntilIdle()
      assertEquals(
        "Streaming",
        viewModel.uiState.value.editor
          ?.name,
      )

      viewModel.onNameChange("Media")
      viewModel.save()
      advanceUntilIdle()

      assertEquals(
        "Media",
        tagRepository
          .observeTags()
          .first()
          .single()
          .name,
      )
    }

  @Test
  fun confirmDelete_removesTag() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = TagsViewModel(tagRepository)
      collectUiState(viewModel)
      val created = tagRepository.createTag("Streaming")

      viewModel.requestDelete(created)
      viewModel.confirmDelete()
      advanceUntilIdle()

      assertTrue(tagRepository.observeTags().first().isEmpty())
    }

  private fun TestScope.collectUiState(viewModel: TagsViewModel) {
    backgroundScope.launch { viewModel.uiState.collect {} }
  }
}
