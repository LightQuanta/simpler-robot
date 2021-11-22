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

package love.forte.simbot.core.event

import kotlinx.coroutines.withContext
import love.forte.simbot.CharSequenceID
import love.forte.simbot.event.*
import love.forte.simbot.event.EventListener
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * [CoreEventManager] 的配置文件.
 */
public class CoreEventMangerConfiguration {
    @Volatile
    internal var processingInterceptors =
        TreeSet<EventProcessingInterceptor>(Comparator.comparing { i -> i.id.toString() })

    @Volatile
    internal var listenerInterceptors = TreeSet<EventListenerInterceptor>(Comparator.comparing { i -> i.id.toString() })


    /**
     * 添加一个流程拦截器，ID需要唯一。
     * 如果出现重复ID，会抛出 [IllegalStateException] 并且不会真正的向当前配置中追加数据。
     *
     * @throws IllegalStateException 如果出现重复ID
     */
    @Synchronized
    public fun addProcessingInterceptor(vararg interceptors: EventProcessingInterceptor) {
        val processingInterceptorsCopy =
            TreeSet<EventProcessingInterceptor>(Comparator.comparing { i -> i.id.toString() })
        processingInterceptorsCopy.addAll(processingInterceptors)
        for (interceptor in interceptors) {
            if (!processingInterceptorsCopy.add(interceptor)) {
                throw IllegalStateException("Duplicate ID: ${interceptor.id}")
            }
        }
        processingInterceptors = processingInterceptorsCopy
    }

    /**
     * 添加一个流程拦截器，ID需要唯一。
     * 如果出现重复ID，会抛出 [IllegalStateException] 并且不会真正的向当前配置中追加数据。
     *
     * @throws IllegalStateException 如果出现重复ID
     */
    @Synchronized
    public fun addListenerInterceptor(vararg interceptors: EventListenerInterceptor) {
        val listenerInterceptorsCopy = TreeSet<EventListenerInterceptor>(Comparator.comparing { i -> i.id.toString() })
        listenerInterceptorsCopy.addAll(listenerInterceptors)
        for (interceptor in interceptors) {
            if (!listenerInterceptorsCopy.add(interceptor)) {
                throw IllegalStateException("Duplicate ID: ${interceptor.id}")
            }
        }
        listenerInterceptors = listenerInterceptorsCopy
    }


    /**
     * 事件流程上下文的处理器。
     */
    public var eventProcessingContextResolver: EventProcessingContextResolver<*> = TODO()


}


/**
 * 核心监听函数管理器。
 */
public class CoreEventManager(
    configuration: CoreEventMangerConfiguration
) : EventProcessor,
    EventListenerRegistrar {

    /**
     * 事件过程拦截器入口。
     */
    private val processingInterceptEntrance =
        EventInterceptEntrance.eventProcessingInterceptEntrance(configuration.processingInterceptors.sortedBy { it.priority })

    /**
     * 监听函数拦截器入口。
     */
    private val listenerInterceptEntrance =
        EventInterceptEntrance.eventListenerInterceptEntrance(configuration.listenerInterceptors.sortedBy { it.priority })

    /**
     * 缓存转化的读写锁。
     */
    private val lock = ReentrantReadWriteLock()

    /**
     * 监听函数列表。ID唯一
     */
    private val listeners: MutableMap<CharSequenceID, EventListener> = ConcurrentHashMap()

    /**
     * 完成缓存与处理的监听函数队列.
     */
    private val resolvedInvokers: MutableMap<Event.Key<*>, List<ListenerInvoker>> = ConcurrentHashMap()


    private fun getInvokers(type: Event.Key<*>): List<ListenerInvoker> {
        return resolvedInvokers.computeIfAbsent(type) { key ->
            // 计算缓存
            listeners.values
                .filter { it.isTarget(key) }
                .map(::ListenerInvoker)
                .sortedBy { it.listener.priority }
                .ifEmpty { emptyList() }
        }
    }

    /**
     * 注册一个监听函数。
     */
    override fun register(listener: EventListener) {
        TODO("Not yet implemented")
    }

    /**
     * 判断指定事件类型在当前事件管理器中是否能够被执行（存在任意对应的监听函数）。
     */
    public operator fun contains(eventType: Event.Key<*>): Boolean {
        return getInvokers(eventType).isNotEmpty()
    }

    /**
     * 推送一个事件。
     */
    override suspend fun push(event: Event): EventProcessingResult {
        val invokers = getInvokers(event.key)
        if (invokers.isEmpty()) {
            return EventProcessingResult
        }

        return doInvoke(resolveToContext(event, invokers.size), invokers)
    }


    /**
     * 切换到当前管理器中的调度器并触发对应事件的内容。
     */
    private suspend fun doInvoke(context: EventProcessingContext, invokers: List<ListenerInvoker>): EventProcessingResult {
        return withContext(context) {
            processingInterceptEntrance.doIntercept(context) {
                // do invoke with intercept

                TODO()
            }
        }
    }


    @Suppress("UNCHECKED_CAST")
    private val resolver: EventProcessingContextResolver<EventProcessingContext> =
        configuration.eventProcessingContextResolver as EventProcessingContextResolver<EventProcessingContext>

    /**
     * 通过 [Event] 得到一个 [EventProcessingContext].
     */
    private suspend fun resolveToContext(event: Event, listenerSize: Int): EventProcessingContext {
        return resolver.resolveEventToContext(event, listenerSize)
    }

    private suspend fun appendResult(context: EventProcessingContext, result: EventResult) {
        resolver.appendResultIntoContext(context, result)
    }


    private inner class ListenerInvoker(
        internal val listener: EventListener,
    ) : suspend (EventProcessingContext) -> EventResult {
        override suspend fun invoke(context: EventProcessingContext): EventResult =
            listenerInterceptEntrance.doIntercept(context, listener::invoke)
    }

}


/**
 * 事件流程上下文的管理器，[CoreEventManager] 通过此接口实例完成对 [EventProcessingContext] 的统一管理。
 */
public interface EventProcessingContextResolver<C : EventProcessingContext> {

    /**
     * 根据一个事件得到对应的流程上下文。
     * 只有在对应事件存在至少一个对应的监听函数的时候才会被触发。
     */
    public suspend fun resolveEventToContext(event: Event, listenerSize: Int): C

    /**
     * 向提供的上下文 [C] 的 [EventProcessingContext.results] 中追加一个 [EventResult].
     *
     * [CoreEventManager] 会对所有得到的结果进行尝试推送，包括 [EventResult.Invalid],
     * 但是建议不会真正的添加 [EventResult.Invalid].
     */
    public suspend fun appendResultIntoContext(context: C, result: EventResult)
}

private object CoreEventProcessingContextResolver


private class CoreEventProcessingContext(
    override val event: Event,
    resultInit: () -> MutableList<EventResult>
) : EventProcessingContext {
    private val _results = resultInit()

    override val results: List<EventResult> = TODO()

}