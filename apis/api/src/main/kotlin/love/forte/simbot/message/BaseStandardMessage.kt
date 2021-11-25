/*
 *  Copyright (c) 2021-2021 ForteScarlet <https://github.com/ForteScarlet>
 *
 *  根据 Apache License 2.0 获得许可；
 *  除非遵守许可，否则您不得使用此文件。
 *  您可以在以下网址获取许可证副本：
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   有关许可证下的权限和限制的具体语言，请参见许可证。
 */

package love.forte.simbot.message

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.Component
import love.forte.simbot.ID
import love.forte.simbot.SimbotComponent
import love.forte.simbot.message.Text.Key.getEmptyText
import love.forte.simbot.message.Text.Key.getText
import java.nio.channels.ByteChannel
import kotlin.reflect.KClass

/**
 * 一些由核心提供的标准 [Message] 实例或标准.
 * 标准消息中，仅提供如下实现：
 * - [纯文本消息][Text]
 * - [AT消息][At]
 * - [图片消息][Image]
 * - [表情消息][]
 * - [emoji][]
 *
 */
public sealed interface StandardMessage<E : Message.Element<E>> : Message.Element<E>

@SerialName("m.std")
@Serializable
public sealed class BaseStandardMessage<E : Message.Element<E>> : StandardMessage<E>


/** 判断一个 [Message.Element] 是否为一个标准 [Message] 下的实现。 */
public inline val Message.Element<*>.isStandard: Boolean get() = this is StandardMessage


//region Text

/**
 * 纯文本消息。代表一段只存在[文本][text]的消息。
 *
 * @see Text
 */
public interface PlainText<A : PlainText<A>> : StandardMessage<A> {
    public val text: String
}

/**
 * 一个文本消息 [Text].
 *
 * 文本消息可以存在多个，但是对于不同平台来讲，有可能存在差异。
 * 部分平台会按照正常的方式顺序排列消息，而有的则会组合消息列表中的所有文本消息为一个整体。
 *
 *
 * @see toText
 * @see Text
 * @see getText
 * @see getEmptyText
 */
@Serializable
@SerialName("m.std.text")
public open class Text protected constructor(override val text: String) : PlainText<Text>, BaseStandardMessage<Text>() {
    override val key: Message.Key<Text> get() = Key

    public fun trim(): Text = getText(text.trim())

    public operator fun plus(other: Text): Text = when {
        text.isEmpty() -> other
        other.text.isEmpty() -> this
        else -> getText(text + other.text)
    }

    public operator fun plus(other: String): Text = if (text.isEmpty()) Text(other) else Text(text + other)
    override fun toString(): String = "Text($text)"
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Text) return false
        return text == other.text
    }

    override fun hashCode(): Int = text.hashCode()

    public companion object Key : Message.Key<Text> {
        private val empty = Text("")
        override val component: Component get() = SimbotComponent
        override val elementType: KClass<Text> get() = Text::class

        @JvmStatic
        public fun getText(text: String): Text {
            return if (text.isEmpty()) empty
            else Text(text)
        }

        @JvmStatic
        public fun getEmptyText(): Text = empty
    }

}


public fun String.toText(): Text = Text.getText(this)
public fun Text(): Text = Text.getEmptyText()
public inline fun Text(block: () -> String): Text = block().toText()
//endregion


//region At
/**
 * 一个 `at` 的标准。
 * at、或者说一个通知信息，用于通知一个用户目标。
 * 一个 At只能代表一个通知目标。
 *
 * @see AtAll
 */
@SerialName("m.std.at")
@Serializable
public data class At(
    @Serializable(with = ID.AsCharSequenceIDSerializer::class)
    public val target: ID,
) : BaseStandardMessage<At>() {
    override val key: Message.Key<At> get() = Key

    public companion object Key : Message.Key<At> {
        override val component: Component get() = SimbotComponent
        override val elementType: KClass<At> get() = At::class
    }
}


/**
 * 一个与 [At] 类似但是不太相同的消息，其代表通知一个权限组下的所有人。
 *
 */
@SerialName("m.std.atRole")
@Serializable
public data class AtRole(
    @Serializable(with = ID.AsCharSequenceIDSerializer::class)
    public val target: ID,
) : BaseStandardMessage<AtRole>() {
    override val key: Message.Key<AtRole> get() = Key

    public companion object Key : Message.Key<AtRole> {
        override val component: Component get() = SimbotComponent
        override val elementType: KClass<AtRole> get() = AtRole::class
    }
}

/**
 * 一个通知所有人的消息。
 */
@SerialName("m.std.atAll")
@Serializable
public object AtAll : BaseStandardMessage<AtAll>(), Message.Key<AtAll> {
    override val key: Message.Key<AtAll>
        get() = this

    override fun toString(): String = "AtAll"
    override fun equals(other: Any?): Boolean = other is AtAll
    override val component: Component get() = SimbotComponent
    override val elementType: KClass<AtAll> get() = AtAll::class
    override fun safeCast(instance: Any?): AtAll? = if (instance == this) this else null
}

//endregion


//region 图片


/**
 * 一个图片消息。
 *
 * 大多数情况下，图片都是需要上传到平台服务器后再使用的。
 *
 * 因此 [Image] 需要通过 [love.forte.simbot.Bot] 进行上传获取。
 *
 */
public interface Image : StandardMessage<Image> {
    /**
     * 上传后的图片会有一个服务端返回的ID。
     *
     * 根据以往的经验，相同图片所上传得到的结果并不100%是相同的。
     *
     */
    public val id: ID


    /**
     * 得到这个图片的数据。
     */
    public fun byteChannel(): ByteChannel
}


//endregion


//region Emoji
/**
 * 一个 Emoji。
 * 目前绝大多数平台已经不会再用一个独特的 "Emoji" 类型来专门标识Emoji了，
 * 此类型仅作为保留类型。
 *
 * 正常情况下，直接将emoji字符串放在 [文本消息][Text] 中就好了。
 *
 */
@SerialName("m.std.emoji")
@Serializable
public data class Emoji(
    @Serializable(ID.AsCharSequenceIDSerializer::class)
    val id: ID
) : StandardMessage<Emoji> {
    override val key: Message.Key<Emoji>
        get() = Key


    public companion object Key : Message.Key<Emoji> {
        override val component: Component get() = SimbotComponent
        override val elementType: KClass<Emoji> get() = Emoji::class
    }
}
//endregion


//region face
/**
 * 一个表情。一般代表平台提供的自带表情。
 */
@SerialName("m.std.face")
@Serializable
public data class Face(
    @Serializable(ID.AsCharSequenceIDSerializer::class)
    val id: ID
) : StandardMessage<Face> {
    override val key: Message.Key<Face>
        get() = Key

    public companion object Key : Message.Key<Face> {
        override val component: Component get() = SimbotComponent
        override val elementType: KClass<Face> get() = Face::class
    }
}
//endregion



//region 多媒体
// /**
//  * 一个多媒体消息。多媒体消息常见为图片、
//  *
//  * 常见的多媒体消息有
//  */
// public interface Multimedia<A : Message.Element<A>> : StandardMessage<A> {
//     public val name: String
// }
//endregion





