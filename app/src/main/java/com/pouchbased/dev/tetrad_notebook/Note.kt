package com.pouchbased.dev.tetrad_notebook

import android.net.Uri
import android.graphics.RectF
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a handwriting note which can be standalone or an annotation over a source file.
 */
@Serializable
data class Note(
    val id: String,
    val name: String,
    @Serializable(with = NullableUriSerializer::class)
    val sourceUri: Uri? = null,
    val pages: List<Page> = emptyList()
)

/**
 * A single page in a note, containing a background and user-drawn strokes.
 */
@Serializable
data class Page(
    val index: Int,
    val background: Background = Background.None,
    val strokes: List<Stroke> = emptyList()
)

/**
 * Defines what is rendered behind the strokes.
 */
@Serializable
sealed class Background {
    @Serializable
    object None : Background()
    
    @Serializable
    data class Color(val color: Int) : Background()
    
    @Serializable
    data class Image(
        @Serializable(with = UriSerializer::class)
        val uri: Uri
    ) : Background()
    
    @Serializable
    data class Pdf(
        @Serializable(with = UriSerializer::class)
        val uri: Uri, 
        val pageNumber: Int
    ) : Background()
}

/**
 * A continuous line drawn by the user.
 */
@Serializable
data class Stroke(
    val points: List<Point>,
    val color: Int,
    val width: Float
)

/**
 * A single coordinate in a stroke, including pressure for variable width rendering.
 */
@Serializable
data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Defines which layer receives touch input and interaction events.
 */
enum class InteractionLayer {
    /** Interactions (clicks, text selection, links) go to the background source (e.g. PDF). */
    SOURCE,
    /** Interactions (drawing, erasing, moving strokes) go to the annotation layer. */
    ANNOTATION
}

/**
 * Represents a link in a PDF document.
 */
@Serializable
data class PdfLink(
    @Serializable(with = RectFSerializer::class)
    val bounds: RectF,
    val destPage: Int? = null,
    @Serializable(with = NullableUriSerializer::class)
    val uri: Uri? = null
)

object RectFSerializer : KSerializer<RectF> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor = 
        buildClassSerialDescriptor("RectF") {
            element("left", Float.serializer().descriptor)
            element("top", Float.serializer().descriptor)
            element("right", Float.serializer().descriptor)
            element("bottom", Float.serializer().descriptor)
        }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: RectF) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeFloatElement(descriptor, 0, value.left)
        composite.encodeFloatElement(descriptor, 1, value.top)
        composite.encodeFloatElement(descriptor, 2, value.right)
        composite.encodeFloatElement(descriptor, 3, value.bottom)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): RectF {
        val composite = decoder.beginStructure(descriptor)
        var left = 0f; var top = 0f; var right = 0f; var bottom = 0f
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> left = composite.decodeFloatElement(descriptor, 0)
                1 -> top = composite.decodeFloatElement(descriptor, 1)
                2 -> right = composite.decodeFloatElement(descriptor, 2)
                3 -> bottom = composite.decodeFloatElement(descriptor, 3)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        composite.endStructure(descriptor)
        return RectF(left, top, right, bottom)
    }
}

/**
 * Custom serializer for android.net.Uri.
 */
object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uri {
        return Uri.parse(decoder.decodeString())
    }
}

/**
 * Custom serializer for nullable android.net.Uri.
 */
object NullableUriSerializer : KSerializer<Uri?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uri?", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uri?) {
        encoder.encodeString(value?.toString() ?: "")
    }

    override fun deserialize(decoder: Decoder): Uri? {
        val string = decoder.decodeString()
        return if (string.isEmpty()) null else Uri.parse(string)
    }
}
