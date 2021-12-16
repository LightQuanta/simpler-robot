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

package love.forte.simboot.filter

import love.forte.simbot.event.EventFilter

/**
 *
 * 事件过滤器注册器，由 `boot-api` 所提供用于 [FilterAnnotationProcessor] 进行过滤器注册的功能接口。
 *
 * @author ForteScarlet
 */
public interface EventFilterRegistrar {

    /**
     * 注册一个监听函数过滤器至当前注册器中。
     */
    public fun register(filter: EventFilter)
}