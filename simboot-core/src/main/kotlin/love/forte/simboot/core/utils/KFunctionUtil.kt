/*
 * Copyright (c) 2022-2023 ForteScarlet <ForteScarlet@163.com>
 *
 * 本文件是 simply-robot (或称 simple-robot 3.x 、simbot 3.x 、simbot3 等) 的一部分。
 * simply-robot 是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是（按你的决定）任何以后版都可以。
 * 发布 simply-robot 是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 *
 * 你应该随程序获得一份 GNU 通用公共许可证的复本。如果没有，请看:
 * https://www.gnu.org/licenses
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 * https://www.gnu.org/licenses/lgpl-3.0-standalone.html
 */

package love.forte.simboot.core.utils

import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.instanceParameter


/**
 * 构建一个此监听函数的签名。一般类似于全限定名。
 */
public fun KFunction<*>.sign(): String {
    return buildString {
        instanceParameter?.type?.also {
            append(it.toString())
            append('.')
        }
        append(name)
        val pms = parameters.filter { it.kind != KParameter.Kind.INSTANCE }
        if (pms.isNotEmpty()) {
            append('(')
            pms.joinTo(this, separator = ",") {
                it.type.toString()
            }
            append(')')
        }
    }
}