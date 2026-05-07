package com.tober.glyphmatrixtools.ui.fields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import com.tober.glyphmatrixtools.util.PreferencesService

@Composable
fun NumberField(
    modifier: Modifier = Modifier,

    title: String,
    label: String,

    preferenceKey: String,
    defaultValue: Long,

    onSaved: (Long) -> Unit = {},
    onReset: (Long) -> Unit = {}
) {
    val context = LocalContext.current

    val preferencesService = remember {
        PreferencesService(context.applicationContext)
    }

    var value by remember(preferenceKey) {
        mutableStateOf(
            preferencesService
                .getLong(preferenceKey, defaultValue)
                .toString()
        )
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = { nextValue ->
                value = nextValue.filter { it.isDigit() }
            },
            label = {
                Text(text = label)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier.padding(top = 12.dp),
            singleLine = true
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            IconButton(
                onClick = {
                    val savedValue = value.toLongOrNull() ?: defaultValue
                    value = savedValue.toString()
                    preferencesService.setLong(preferenceKey, savedValue)
                    onSaved(savedValue)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save"
                )
            }

            IconButton(
                onClick = {
                    value = defaultValue.toString()
                    preferencesService.setLong(preferenceKey, defaultValue)
                    onReset(defaultValue)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Reset"
                )
            }
        }
    }
}
