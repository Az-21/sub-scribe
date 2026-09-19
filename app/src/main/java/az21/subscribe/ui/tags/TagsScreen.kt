package az21.subscribe.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.model.Tag
import az21.subscribe.ui.common.IconActionButton
import az21.subscribe.ui.common.PresetColors
import az21.subscribe.ui.common.SubScribeTopAppBar
import az21.subscribe.ui.common.segmentedListItemColors
import az21.subscribe.ui.theme.AppTheme

@Composable
fun TagsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: TagsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  TagsContent(
    uiState = uiState,
    onBack = onBack,
    onAdd = viewModel::startCreate,
    onEdit = viewModel::startEdit,
    onRequestDelete = viewModel::requestDelete,
    onCancelDelete = viewModel::cancelDelete,
    onConfirmDelete = viewModel::confirmDelete,
    onNameChange = viewModel::onNameChange,
    onColorChange = viewModel::onColorChange,
    onSaveEditor = viewModel::save,
    onDismissEditor = viewModel::dismissEditor,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TagsContent(
  uiState: TagsUiState,
  onBack: () -> Unit,
  onAdd: () -> Unit,
  onEdit: (Tag) -> Unit,
  onRequestDelete: (Tag) -> Unit,
  onCancelDelete: () -> Unit,
  onConfirmDelete: () -> Unit,
  onNameChange: (String) -> Unit,
  onColorChange: (Int?) -> Unit,
  onSaveEditor: () -> Unit,
  onDismissEditor: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      SubScribeTopAppBar(
        title = stringResource(R.string.tags_title),
        onNavigateUp = onBack,
        scrollBehavior = scrollBehavior,
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        text = { Text(stringResource(R.string.action_add)) },
        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
        onClick = onAdd,
      )
    },
  ) { innerPadding ->
    TagsBody(
      uiState = uiState,
      onEdit = onEdit,
      onRequestDelete = onRequestDelete,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    )
  }

  TagDialogs(
    uiState = uiState,
    onNameChange = onNameChange,
    onColorChange = onColorChange,
    onSaveEditor = onSaveEditor,
    onDismissEditor = onDismissEditor,
    onCancelDelete = onCancelDelete,
    onConfirmDelete = onConfirmDelete,
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TagsBody(
  uiState: TagsUiState,
  onEdit: (Tag) -> Unit,
  onRequestDelete: (Tag) -> Unit,
  modifier: Modifier = Modifier,
) {
  when {
    uiState.isLoading -> {
      Box(modifier = modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
    }

    uiState.tags.isEmpty() -> {
      Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.tags_empty), style = MaterialTheme.typography.bodyLarge)
      }
    }

    else -> {
      LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
      ) {
        itemsIndexed(items = uiState.tags, key = { _, tag -> tag.id }) { index, tag ->
          TagRow(
            tag = tag,
            index = index,
            count = uiState.tags.size,
            onEdit = onEdit,
            onRequestDelete = onRequestDelete,
          )
        }
      }
    }
  }
}

@Composable
private fun TagRow(
  tag: Tag,
  index: Int,
  count: Int,
  onEdit: (Tag) -> Unit,
  onRequestDelete: (Tag) -> Unit,
) {
  SegmentedListItem(
    onClick = { onEdit(tag) },
    shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
    colors = segmentedListItemColors(),
    modifier = Modifier.fillMaxWidth(),
    leadingContent = { TagColorDot(color = tag.color) },
    content = { Text(text = tag.name, style = MaterialTheme.typography.bodyLarge) },
    trailingContent = {
      Row {
        IconActionButton(
          onClick = { onEdit(tag) },
          icon = Icons.Outlined.Edit,
          contentDescription = stringResource(R.string.action_edit),
        )
        IconActionButton(
          onClick = { onRequestDelete(tag) },
          icon = Icons.Default.Delete,
          contentDescription = stringResource(R.string.tags_delete),
        )
      }
    },
  )
}

@Composable
private fun TagDialogs(
  uiState: TagsUiState,
  onNameChange: (String) -> Unit,
  onColorChange: (Int?) -> Unit,
  onSaveEditor: () -> Unit,
  onDismissEditor: () -> Unit,
  onCancelDelete: () -> Unit,
  onConfirmDelete: () -> Unit,
) {
  uiState.editor?.let { editor ->
    TagEditorDialog(
      editor = editor,
      onNameChange = onNameChange,
      onColorChange = onColorChange,
      onSave = onSaveEditor,
      onDismiss = onDismissEditor,
    )
  }

  uiState.pendingDelete?.let { tag ->
    AlertDialog(
      onDismissRequest = onCancelDelete,
      title = { Text(stringResource(R.string.tags_delete)) },
      text = { Text(stringResource(R.string.tags_delete_message, tag.name)) },
      confirmButton = {
        TextButton(onClick = onConfirmDelete) {
          Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = onCancelDelete) { Text(stringResource(R.string.action_cancel)) }
      },
    )
  }
}

@Composable
private fun TagColorDot(color: Int?) {
  if (color == null) return
  Box(modifier = Modifier.size(16.dp).background(Color(color), CircleShape))
}

@Composable
private fun TagEditorDialog(
  editor: TagEditorState,
  onNameChange: (String) -> Unit,
  onColorChange: (Int?) -> Unit,
  onSave: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(if (editor.id == null) R.string.tags_add else R.string.tags_edit))
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
          value = editor.name,
          onValueChange = onNameChange,
          label = { Text(stringResource(R.string.tags_name)) },
          singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PresetColors.forEach { color ->
            ColorSwatch(selected = editor.color == color, color = color, onClick = { onColorChange(color) })
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
  )
}

@Composable
private fun ColorSwatch(
  selected: Boolean,
  color: Int,
  onClick: () -> Unit,
) {
  Box(
    modifier =
      Modifier
        .size(32.dp)
        .background(Color(color), CircleShape)
        .border(
          width = if (selected) 3.dp else 0.dp,
          color = MaterialTheme.colorScheme.onSurface,
          shape = CircleShape,
        ).clickable(onClick = onClick),
  )
}

@Preview(showBackground = true)
@Composable
private fun TagsContentPreview() {
  AppTheme {
    TagsContent(
      uiState = TagsUiState(isLoading = false),
      onBack = {},
      onAdd = {},
      onEdit = {},
      onRequestDelete = {},
      onCancelDelete = {},
      onConfirmDelete = {},
      onNameChange = {},
      onColorChange = {},
      onSaveEditor = {},
      onDismissEditor = {},
    )
  }
}
