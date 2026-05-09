package com.tober.glyphmatrixtools.ui.glyph

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

import com.tober.glyphmatrixtools.apps.App
import com.tober.glyphmatrixtools.glyph.Glyph
import com.tober.glyphmatrixtools.glyph.GlyphImageStorage
import com.tober.glyphmatrixtools.glyph.GlyphStorage
import com.tober.glyphmatrixtools.util.ToastService

sealed interface GlyphItemDraggingTarget {
    data class Glyph(
        val glyphId: String
    ) : GlyphItemDraggingTarget
}

class GlyphItemListState(
    val title: String? = null,

    val context: Context,
    val toastService: ToastService,
    val glyphStorage: GlyphStorage,
    val glyphs: SnapshotStateList<Glyph>,

    val useImage: Boolean,
    val imagePrefix: String?,
    val imageSize: Dp,

    val useApp: Boolean,

    val useContact: Boolean
) {
    var newGlyphImage by mutableStateOf<String?>(null)
    var newGlyphApp by mutableStateOf<App?>(null)
    var newGlyphContact by mutableStateOf<String?>(null)

    var glyphDraggingTarget by mutableStateOf<GlyphItemDraggingTarget?>(null)

    fun persistGlyphs() {
        val normalized = glyphs.mapIndexed { index, item ->
            item.copy(order = index)
        }

        glyphs.clear()
        glyphs.addAll(normalized)

        glyphStorage.setGlyphs(normalized)
    }

    fun reorderGlyph(from: Int, to: Int) {
        if (from !in glyphs.indices || to !in glyphs.indices) return
        if (from == to) return

        val item = glyphs.removeAt(from)
        glyphs.add(to, item)
    }

    fun validateAndSaveGlyph() {
        val image = newGlyphImage
        val app = newGlyphApp
        val contact = newGlyphContact

        if (useImage) {
            if (image.isNullOrBlank()) {
                toastService.show("Choose a glyph")
                return
            }
        }

        if (useApp) {
            if (app == null) {
                toastService.show("Choose an app")
                return
            }

            val exists = glyphs.any {
                it.appPackageName == app.packageName
            }

            if (exists) {
                toastService.show("An entry for this app already exists")
                return
            }
        }

        if (useContact) {
            if (contact.isNullOrBlank()) {
                toastService.show("Specify a contact")
                return
            }

            val exists = glyphs.any {
                it.contact == contact
            }

            if (exists) {
                toastService.show("An entry for this contact already exists")
                return
            }
        }

        if (useImage) {
            GlyphImageStorage
                .saveGlyphImage(
                    context = context,
                    path = image!!,
                    prefix = imagePrefix!!
                )
                .onSuccess { savedImage ->
                    saveGlyph(savedImage)
                    GlyphImageStorage.deleteGlyphImage(image)
                }
                .onFailure { error ->
                    toastService.show(error.message ?: "Failed to save glyph")
                }
        } else {
            saveGlyph(null)
        }
    }

    private fun saveGlyph(
        savedImage: String?
    ) {
        val app = newGlyphApp
        val contact = newGlyphContact

        glyphs.add(
            Glyph(
                order = glyphs.size,

                image = savedImage,
                imageAnimate = true,

                appLabel = app?.label,
                appPackageName = app?.packageName,

                contact = contact
            )
        )

        persistGlyphs()

        newGlyphImage = null
        newGlyphApp = null
        newGlyphContact = null

        toastService.show("Entry saved")
    }

    fun updateGlyphImage(
        glyphId: String,
        temporaryImagePath: String
    ) {
        if (!useImage || imagePrefix.isNullOrBlank()) return

        GlyphImageStorage
            .saveGlyphImage(
                context = context,
                path = temporaryImagePath,
                prefix = imagePrefix
            )
            .onSuccess { savedImage ->
                val index = glyphs.indexOfFirst {
                    it.id == glyphId
                }

                if (index != -1) {
                    val old = glyphs[index]

                    glyphs[index] = old.copy(
                        image = savedImage
                    )

                    GlyphImageStorage.deleteGlyphImage(old.image)
                    GlyphImageStorage.deleteGlyphImage(temporaryImagePath)

                    persistGlyphs()

                    toastService.show("Glyph updated")
                }
            }
            .onFailure { error ->
                toastService.show(error.message ?: "Failed to update glyph")
            }
    }

    fun updateGlyphApp(
        glyphId: String,
        app: App
    ) {
        val exists = glyphs.any {
            it.id != glyphId && it.appPackageName == app.packageName
        }

        if (exists) {
            toastService.show("An entry for this app already exists")
            return
        }

        val index = glyphs.indexOfFirst {
            it.id == glyphId
        }

        if (index != -1) {
            glyphs[index] = glyphs[index].copy(
                appLabel = app.label,
                appPackageName = app.packageName
            )

            persistGlyphs()

            toastService.show("App updated")
        }
    }

    // Unused
    fun updateGlyphContact(
        glyphId: String,
        contact: String
    ) {
        val exists = glyphs.any {
            it.id != glyphId && it.contact == contact
        }

        if (exists) {
            toastService.show("An entry for this contact already exists")
            return
        }

        val index = glyphs.indexOfFirst {
            it.id == glyphId
        }

        if (index != -1) {
            glyphs[index] = glyphs[index].copy(
                contact = contact
            )

            persistGlyphs()
        }
    }

    fun updateGlyphSettings(
        updated: Glyph
    ) {
        val index = glyphs.indexOfFirst {
            it.id == updated.id
        }

        if (index != -1) {
            glyphs[index] = updated
            persistGlyphs()
        }
    }

    fun deleteGlyph(
        deleted: Glyph
    ) {
        glyphs.removeAll {
            it.id == deleted.id
        }

        GlyphImageStorage.deleteGlyphImage(deleted.image)
        persistGlyphs()

        toastService.show("Entry deleted")
    }
}

@Composable
fun rememberGlyphItemListState(
    title: String? = null,

    preferenceKey: String,

    useImage: Boolean = true,
    imagePrefix: String? = null,
    imageSize: Dp = 56.dp,

    useApp: Boolean = false,

    useContact: Boolean = false
): GlyphItemListState {
    val context = LocalContext.current

    val toastService = remember {
        ToastService(context)
    }

    val glyphStorage = remember(preferenceKey) {
        GlyphStorage(
            context.applicationContext,
            preferenceKey
        )
    }

    val glyphs = remember {
        mutableStateListOf<Glyph>()
    }

    LaunchedEffect(preferenceKey) {
        glyphs.clear()
        glyphs.addAll(glyphStorage.getGlyphs())
    }

    return remember(
        title,

        preferenceKey,

        useImage,
        imagePrefix,
        imageSize,

        useApp,

        useContact
    ) {
        GlyphItemListState(
            title = title,

            context = context,
            toastService = toastService,
            glyphStorage = glyphStorage,
            glyphs = glyphs,

            useImage = useImage,
            imagePrefix = imagePrefix,
            imageSize = imageSize,

            useApp = useApp,

            useContact = useContact
        )
    }
}

fun LazyListScope.glyphItemList(
    state: GlyphItemListState
) {
    item {
        if (state.title !== null) {
            Text(text = state.title)

            Spacer(modifier = Modifier.height(12.dp))
        }

        GlyphItem(
            action = GlyphItemAction.Save,

            useImage = state.useImage,
            image = state.newGlyphImage,
            imageSize = state.imageSize,
            onGlyphImagePicked = { temporaryImagePath ->
                state.newGlyphImage = temporaryImagePath
            },
            onGlyphImagePickError = { error ->
                state.toastService.show(error)
            },

            useApp = state.useApp,
            appLabel = state.newGlyphApp?.label,
            appPackageName = state.newGlyphApp?.packageName,
            onGlyphAppPicked = { app ->
                state.newGlyphApp = app
            },

            useContact = state.useContact,
            contact = state.newGlyphContact,
            onGlyphContactPicked = { contact ->
                state.newGlyphContact = contact
            },

            onSaveGlyph = {
                state.validateAndSaveGlyph()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    itemsIndexed(
        items = state.glyphs,
        key = { _, item ->
            item.id
        }
    ) { _, item ->
        var dragAmount by remember(item.id) {
            mutableFloatStateOf(0f)
        }

        var itemHeightPx by remember(item.id) {
            mutableFloatStateOf(1f)
        }

        val isDragging = state.glyphDraggingTarget == GlyphItemDraggingTarget.Glyph(item.id)

        val scale by animateFloatAsState(
            targetValue = if (isDragging) 1.025f else 1f,
            animationSpec = spring(),
            label = "glyphItemScale"
        )

        GlyphItem(
            action = GlyphItemAction.Settings(item),

            useImage = state.useImage,
            image = item.image,
            imageSize = state.imageSize,
            onGlyphImagePicked = { temporaryImagePath ->
                state.updateGlyphImage(
                    glyphId = item.id,
                    temporaryImagePath = temporaryImagePath
                )
            },
            onGlyphImagePickError = { error ->
                state.toastService.show(error)
            },

            useApp = state.useApp,
            appLabel = item.appLabel,
            appPackageName = item.appPackageName,
            onGlyphAppPicked = { app ->
                state.updateGlyphApp(
                    glyphId = item.id,
                    app = app
                )
            },

            useContact = state.useContact,
            contact = item.contact,
            onGlyphContactPicked = { contact ->
                state.updateGlyphContact(
                    glyphId = item.id,
                    contact = contact
                )
            },

            onChangeGlyphSettings = { updated ->
                state.updateGlyphSettings(updated)
            },
            onDeleteGlyph = { deletedGlyph ->
                state.deleteGlyph(deletedGlyph)
            },

            modifier = Modifier
                .animateItem()
                .then(
                    if (isDragging) {
                        Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = 16f
                        }
                    } else {
                        Modifier
                    }
                )
                .padding(top = 8.dp)
                .onSizeChanged { size ->
                    val height = size.height.toFloat().coerceAtLeast(1f)

                    if (itemHeightPx != height) {
                        itemHeightPx = height
                    }
                }
                .pointerInput(item.id, state.glyphs.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            state.glyphDraggingTarget = GlyphItemDraggingTarget.Glyph(item.id)
                            dragAmount = 0f
                        },
                        onDragCancel = {
                            state.glyphDraggingTarget = null
                            dragAmount = 0f
                            state.persistGlyphs()
                        },
                        onDragEnd = {
                            state.glyphDraggingTarget = null
                            dragAmount = 0f
                            state.persistGlyphs()
                        },
                        onDrag = { _, offset ->
                            dragAmount += offset.y

                            val from = state.glyphs.indexOfFirst {
                                it.id == item.id
                            }

                            if (from == -1) {
                                return@detectDragGesturesAfterLongPress
                            }

                            val threshold = itemHeightPx

                            if (dragAmount > threshold && from < state.glyphs.lastIndex) {
                                state.reorderGlyph(from, from + 1)
                                dragAmount -= threshold
                            }

                            if (dragAmount < -threshold && from > 0) {
                                state.reorderGlyph(from, from - 1)
                                dragAmount += threshold
                            }

                            if (abs(offset.y) < 1f) {
                                return@detectDragGesturesAfterLongPress
                            }
                        }
                    )
                }
        )
    }
}
