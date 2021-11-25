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

package love.forte.simbot.definition

import kotlinx.coroutines.runBlocking
import love.forte.simbot.Api4J
import love.forte.simbot.Bot
import love.forte.simbot.ID
import love.forte.simbot.action.MessageSendSupport
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageReceipt

/**
 * [Target] 是对与 [Bot] 相关联的对象 （[组织][Organization] 或一个具体的 [用户][User]） 的统称。
 *
 * 不论 [组织][Organization] 还是 [用户][User]，它们均来自一个 [Bot].
 *
 * [Target] 本身仅代表这个对象的概念，不能保证其本身拥有 [发送消息][MessageSendSupport] 的能力。
 *
 *
 *
 * @author ForteScarlet
 */
public sealed interface Target : BotContainer {

    /**
     * 当前对象对应的唯一ID。
     *
     * @see ID
     */
    public val id: ID

    /**
     * 当前 [Target] 来自的bot。
     */
    override val bot: Bot


    /**
     * 如果当前支持发送消息，则发送.
     * 否则得到null。
     */
    @Api4J
    public fun trySendBlocking(message: Message): MessageReceipt? =
        if (this is MessageSendSupport) runBlocking { send(message) } else null


}