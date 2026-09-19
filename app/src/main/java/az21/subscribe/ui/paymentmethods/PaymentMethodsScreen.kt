package az21.subscribe.ui.paymentmethods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import az21.subscribe.R
import az21.subscribe.domain.model.PaymentMethod
import az21.subscribe.ui.theme.AppTheme

@Composable
fun PaymentMethodsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PaymentMethodsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  PaymentMethodsContent(
    uiState = uiState,
    onBack = onBack,
    onAdd = viewModel::startCreate,
    onEdit = viewModel::startEdit,
    onRequestDelete = viewModel::requestDelete,
    onCancelDelete = viewModel::cancelDelete,
    onConfirmDelete = viewModel::confirmDelete,
    onLabelChange = viewModel::onLabelChange,
    onSaveEditor = viewModel::save,
    onDismissEditor = viewModel::dismissEditor,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsContent(
  uiState: PaymentMethodsUiState,
  onBack: () -> Unit,
  onAdd: () -> Unit,
  onEdit: (PaymentMethod) -> Unit,
  onRequestDelete: (PaymentMethod) -> Unit,
  onCancelDelete: () -> Unit,
  onConfirmDelete: () -> Unit,
  onLabelChange: (String) -> Unit,
  onSaveEditor: () -> Unit,
  onDismissEditor: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.payment_methods_title)) },
        navigationIcon = {
          TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        },
        actions = {
          IconButton(onClick = onAdd) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = stringResource(R.string.payment_methods_add),
            )
          }
        },
      )
    },
  ) { innerPadding ->
    PaymentMethodsBody(
      uiState = uiState,
      onEdit = onEdit,
      onRequestDelete = onRequestDelete,
      modifier = Modifier.fillMaxSize().padding(innerPadding),
    )
  }

  PaymentMethodDialogs(
    uiState = uiState,
    onLabelChange = onLabelChange,
    onSaveEditor = onSaveEditor,
    onDismissEditor = onDismissEditor,
    onCancelDelete = onCancelDelete,
    onConfirmDelete = onConfirmDelete,
  )
}

@Composable
private fun PaymentMethodsBody(
  uiState: PaymentMethodsUiState,
  onEdit: (PaymentMethod) -> Unit,
  onRequestDelete: (PaymentMethod) -> Unit,
  modifier: Modifier = Modifier,
) {
  when {
    uiState.isLoading -> {
      Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }

    uiState.paymentMethods.isEmpty() -> {
      Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = stringResource(R.string.payment_methods_empty), style = MaterialTheme.typography.bodyLarge)
      }
    }

    else -> {
      LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(items = uiState.paymentMethods, key = { method -> method.id }) { method ->
          PaymentMethodRow(method = method, onEdit = onEdit, onRequestDelete = onRequestDelete)
        }
      }
    }
  }
}

@Composable
private fun PaymentMethodRow(
  method: PaymentMethod,
  onEdit: (PaymentMethod) -> Unit,
  onRequestDelete: (PaymentMethod) -> Unit,
) {
  Card(onClick = { onEdit(method) }, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = method.label, style = MaterialTheme.typography.bodyLarge)
      IconButton(onClick = { onRequestDelete(method) }) {
        Icon(imageVector = Icons.Default.Clear, contentDescription = stringResource(R.string.payment_methods_delete))
      }
    }
  }
}

@Composable
private fun PaymentMethodDialogs(
  uiState: PaymentMethodsUiState,
  onLabelChange: (String) -> Unit,
  onSaveEditor: () -> Unit,
  onDismissEditor: () -> Unit,
  onCancelDelete: () -> Unit,
  onConfirmDelete: () -> Unit,
) {
  uiState.editor?.let { editor ->
    PaymentMethodEditorDialog(
      editor = editor,
      onLabelChange = onLabelChange,
      onSave = onSaveEditor,
      onDismiss = onDismissEditor,
    )
  }

  uiState.pendingDelete?.let { method ->
    AlertDialog(
      onDismissRequest = onCancelDelete,
      title = { Text(stringResource(R.string.payment_methods_delete)) },
      text = { Text(stringResource(R.string.payment_methods_delete_message, method.label)) },
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
private fun PaymentMethodEditorDialog(
  editor: PaymentMethodEditorState,
  onLabelChange: (String) -> Unit,
  onSave: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        stringResource(
          if (editor.id == null) R.string.payment_methods_add else R.string.payment_methods_edit,
        ),
      )
    },
    text = {
      Column {
        OutlinedTextField(
          value = editor.label,
          onValueChange = onLabelChange,
          label = { Text(stringResource(R.string.payment_methods_label)) },
          singleLine = true,
        )
      }
    },
    confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
  )
}

@Preview(showBackground = true)
@Composable
private fun PaymentMethodsContentPreview() {
  AppTheme {
    PaymentMethodsContent(
      uiState = PaymentMethodsUiState(isLoading = false),
      onBack = {},
      onAdd = {},
      onEdit = {},
      onRequestDelete = {},
      onCancelDelete = {},
      onConfirmDelete = {},
      onLabelChange = {},
      onSaveEditor = {},
      onDismissEditor = {},
    )
  }
}
