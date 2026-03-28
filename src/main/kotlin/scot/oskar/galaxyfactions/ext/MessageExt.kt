package scot.oskar.galaxyfactions.ext

import com.hypixel.hytale.server.core.Message
import java.awt.Color

@DslMarker
annotation class MessageDsl

@MessageDsl
class MessageBuilder(private val message: Message) {

    var bold: Boolean
        get() = false
        set(value) { message.bold(value) }

    var italic: Boolean
        get() = false
        set(value) { message.italic(value) }

    var monospace: Boolean
        get() = false
        set(value) { message.monospace(value) }

    var color: String?
        get() = message.color
        set(value) { value?.let { message.color(it) } }

    var link: String?
        get() = null
        set(value) { value?.let { message.link(it) } }

    fun param(key: String, value: String) { message.param(key, value) }
    fun param(key: String, value: Int) { message.param(key, value) }
    fun param(key: String, value: Long) { message.param(key, value) }
    fun param(key: String, value: Double) { message.param(key, value) }
    fun param(key: String, value: Float) { message.param(key, value) }
    fun param(key: String, value: Boolean) { message.param(key, value) }
    fun param(key: String, value: Message) { message.param(key, value) }

    fun insert(text: String, block: MessageBuilder.() -> Unit = {}) {
        val child = Message.raw(text)
        MessageBuilder(child).apply(block)
        message.insert(child)
    }

    fun translation(messageId: String, block: MessageBuilder.() -> Unit = {}) {
        val child = Message.translation(messageId)
        MessageBuilder(child).apply(block)
        message.insert(child)
    }

    fun insert(msg: Message) {
        message.insert(msg)
    }

    fun build(): Message = message
}

fun message(text: String, block: MessageBuilder.() -> Unit = {}): Message {
    val msg = Message.raw(text)
    MessageBuilder(msg).apply(block)
    return msg
}

fun translatedMessage(messageId: String, block: MessageBuilder.() -> Unit = {}): Message {
    val msg = Message.translation(messageId)
    MessageBuilder(msg).apply(block)
    return msg
}

object MessageColors {
    const val SUCCESS = "#55ff55"
    const val ERROR = "#ff5555"
    const val WARNING = "#ffff55"
    const val INFO = "#aaaaaa"
    const val ACTION = "#777777"
    const val HIGHLIGHT = "#55ffff"
}

private fun prefix(): Message = Message.empty().apply {
    insert(Message.raw("[").color(MessageColors.INFO))
    insert(Message.raw("GF").color(MessageColors.HIGHLIGHT).bold(true))
    insert(Message.raw("] ").color(MessageColors.INFO))
}

fun prefixedMessage(text: String, block: MessageBuilder.() -> Unit = {}): Message {
    return Message.empty().apply {
        insert(prefix())
        insert(message(text, block))
    }
}

fun errorMessage(text: String, block: MessageBuilder.() -> Unit = {}): Message {
    return Message.empty().apply {
        insert(prefix())
        insert(message(text) {
            color = MessageColors.ERROR
            block()
        })
    }
}

fun successMessage(text: String, block: MessageBuilder.() -> Unit = {}): Message {
    return Message.empty().apply {
        insert(prefix())
        insert(message(text) {
            color = MessageColors.SUCCESS
            block()
        })
    }
}

fun warningMessage(text: String, block: MessageBuilder.() -> Unit = {}): Message {
    return Message.empty().apply {
        insert(prefix())
        insert(message(text) {
            color = MessageColors.WARNING
            block()
        })
    }
}

fun infoMessage(text: String, block: MessageBuilder.() -> Unit = {}): Message {
    return Message.empty().apply {
        insert(prefix())
        insert(message(text) {
            color = MessageColors.INFO
            block()
        })
    }
}

fun actionMessage(text: String, block: MessageBuilder.() -> Unit = {}): Message {
    return Message.empty().apply {
        insert(prefix())
        insert(message(text) {
            color = MessageColors.ACTION
            block()
        })
    }
}
