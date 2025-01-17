/*
 *     Copyright (c) 2024. ForteScarlet.
 *
 *     Project    https://github.com/simple-robot/simpler-robot
 *     Email      ForteScarlet@163.com
 *
 *     This file is part of the Simple Robot Library (Alias: simple-robot, simbot, etc.).
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 *
 *     You should have received a copy of the Lesser GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package love.forte.simbot.common.weak

/**
 * 会尝试构建并使用 [`WeakRef`](https://developer.mozilla.org/zh-CN/docs/Web/JavaScript/Reference/Global_Objects/WeakRef)，
 * 如果无法构建则会降级使用 [NonWeakRefImpl]。
 *
 */
public actual fun <T : Any> weakRef(ref: T): WeakRef<T> {
    return runCatching {
        val jsWeakRef = js("new WeakRef(ref);")
        if (jsWeakRef.deref != undefined) { // check deref func.
            JsWeakRefImpl(jsWeakRef)
        } else {
            NonWeakRefImpl(ref)
        }
    }.getOrElse {
        NonWeakRefImpl(ref)
    }
}

private class JsWeakRefImpl<T : Any>(private var weakRef: dynamic) : WeakRef<T> {
    // WeakRef
    override val value: T?
        get() {
            val r = weakRef
            if (r != null) {
                return r.deref().unsafeCast<T?>().also { if (it == null) weakRef = null }
            }

            return null
        }

    override fun clear() {
        weakRef = null
    }
}
