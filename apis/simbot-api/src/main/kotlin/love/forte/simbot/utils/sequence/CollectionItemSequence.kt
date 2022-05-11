/*
 *  Copyright (c) 2022-2022 ForteScarlet <ForteScarlet@163.com>
 *
 *  本文件是 simply-robot (或称 simple-robot 3.x 、simbot 3.x ) 的一部分。
 *
 *  simply-robot 是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是（按你的决定）任何以后版都可以。
 *
 *  发布 simply-robot 是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 *
 *  你应该随程序获得一份 GNU 通用公共许可证的复本。如果没有，请看:
 *  https://www.gnu.org/licenses
 *  https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *  https://www.gnu.org/licenses/lgpl-3.0-standalone.html
 *
 */

package love.forte.simbot.utils.sequence

import love.forte.simbot.Api4J
import love.forte.simbot.utils.sequence.ItemSequence.Companion.asItemSequence
import java.util.stream.Stream


/**
 *
 * @author ForteScarlet
 */
internal class CollectionItemSequence<out V>(private val collection: Collection<V>) : ItemSequence<V> {
    
    private var matcherBuf: Matcher<V>? = null
    
    override fun filter(matcher: Matcher<V>): ItemSequence<V> {
        val buf = matcherBuf
        return if (buf != null) {
            collection.asSequence().filter(buf).filter(matcher).asItemSequence()
        } else {
            matcherBuf = matcher
            this
        }
    }
    
    override fun <T> map(mapper: Mapper<V, T>): ItemSequence<T> {
        var seq = collection.asSequence()
        matcherBuf?.also { buf ->
            seq = seq.filter(buf)
        }
        return seq.map(mapper).asItemSequence()
    }
    
    override fun collect(visitor: Visitor<V>) {
        val matcher = matcherBuf
        if (matcher != null) {
            collection.forEach {
                if (matcher(it)) {
                    visitor(it)
                }
            }
        } else {
            collection.forEach { visitor(it) }
        }
    }
    
    override fun <C : MutableCollection<in V>> collectTo(destination: C): C {
        return destination.also { c ->
            val matcher = matcherBuf
            if (matcher != null) {
                collection.forEach {
                    if (matcher(it)) {
                        c.add(it)
                    }
                }
            } else {
                c.addAll(collection)
            }
        }
    }
    
    override fun toList(): List<V> {
        val matcher = matcherBuf
        return if (matcher != null) {
            collection.filter(matcher)
        } else {
            collection.toList()
        }
    }
    
    override fun asSequence(): Sequence<V> {
        var seq = collection.asSequence()
        val matcher = matcherBuf
        if (matcher != null) {
            seq = seq.filter(matcher)
        }
        return seq
    }
    
    @Api4J
    override fun asStream(): Stream<out V> {
        var stream = collection.stream()
        val matcher = matcherBuf
        if (matcher != null) {
            stream = stream.filter(matcher)
        }
        return stream
    }
}