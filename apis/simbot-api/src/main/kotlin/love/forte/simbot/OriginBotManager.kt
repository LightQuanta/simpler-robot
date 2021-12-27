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

package love.forte.simbot

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


/**
 * [OriginBotManager] 是所有[BotManager]的管理器 .
 */
public object OriginBotManager : Set<BotManager<*>> {
    private val logger = LoggerFactory.getLogger(OriginBotManager::class)
    private val lock = ReentrantReadWriteLock()
    private val managers: MutableMap<BotManager<*>, Unit> = WeakHashMap()
    private val shutdown = AtomicBoolean(false)

    @Synchronized
    public fun cancel(reason: Throwable? = null) {
        lock.write {
            shutdown.set(true)
            logger.debug("OriginBotManager shutdown...")
            var err: Throwable? = null
            for (manager in managers.keys.toList()) {
                kotlin.runCatching {
                    runBlocking { manager.cancel(reason) }
                }.getOrElse { e ->
                    kotlin.runCatching {
                        val err0 = err
                        if (err0 == null) {
                            err = SimbotIllegalStateException("BotManager shutdown failed.")
                            err!!.addSuppressed(
                                SimbotIllegalStateException(
                                    "Manager $manager of component ${manager.component.name}",
                                    e
                                )
                            )
                        } else {
                            err0.addSuppressed(
                                SimbotIllegalStateException(
                                    "Manager $manager of component ${manager.component.name}",
                                    e
                                )
                            )
                        }
                    }.onFailure {
                        logger.error("Bot manager shutdown failed! ", it)
                    }
                }
            }
            logger.debug("All managed bot manager shutdown finished.")
            if (err != null) {
                logger.error("Some bot manager shutdown failed.", err)
            }
        }
    }

    private inline fun checkShutdown(message: () -> String = { "OriginBotManager has already shutdown!" }) {
        if (shutdown.get()) {
            throw IllegalStateException(message())
        }
    }

    internal fun register(manager: BotManager<*>) {
        lock.write {
            checkShutdown()
            managers[manager] = Unit
        }
    }

    internal fun remove(manager: BotManager<*>): Boolean {
        lock.write {
            return managers.remove(manager) != null
        }
    }

    public fun getManagers(component: Component): Flow<BotManager<*>> {
        lock.read {
            checkShutdown()
            return flow {
                val iter = managers.keys.iterator()
                while (iter.hasNext()) {
                    val next: BotManager<*> = iter.next()
                    if (next.component == component) emit(next)
                    else continue
                }
            }
        }
    }

    public fun getFirstManager(component: Component): BotManager<*>? = lock.read {
        checkShutdown()
        managers.keys.firstOrNull { it.component == component }
    }

    public fun getBot(id: ID, component: Component): Bot? {
        return managers.keys.firstOrNull {
            it.component == component
        }?.get(id)
    }

    //// set

    override val size: Int
        get() = managers.size

    override fun contains(element: BotManager<*>): Boolean = managers.containsKey(element)

    override fun containsAll(elements: Collection<BotManager<*>>): Boolean {
        return elements.all { contains(it) }
    }

    override fun isEmpty(): Boolean = managers.isEmpty()
    override fun iterator(): Iterator<BotManager<*>> = managers.keys.iterator()
}