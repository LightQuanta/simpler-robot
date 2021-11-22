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

@file:JvmName("AccountUtil")

package love.forte.simbot.definition

import love.forte.simbot.Bot
import love.forte.simbot.ID

/**
 * 一个 **用户**。
 *
 * 对于Bot来讲，一个用户可能是一个陌生的人，一个[群成员][Member], 或者一个好友。
 *
 * 当然，[User] 也有可能代表了 [love.forte.simbot.Bot] 自身.
 *
 * @author ForteScarlet
 */
public interface User : Something, UserInfo {

    /**
     * 这个账号的唯一ID。
     */
    override val id: ID



}

/**
 * 一个 **Bot容器**.
 * 一般代表可以得到 [Bot] 的对象，例如非 [Bot] 的 [User]，比如 [Friend].
 *
 */
public interface BotContainer : Container {
    public val bot: Bot
}