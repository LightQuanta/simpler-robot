/*
 *     Copyright (c) 2024. ForteScarlet.
 *
 *     Project    https://github.com/simple-robot/simpler-robot
 *     Email      ForteScarlet@163.com
 *
 *     This file is part of the Simple Robot Library.
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

import platform.Foundation.NSDate
import platform.Foundation.now
import platform.Foundation.timeIntervalSince1970
import kotlin.test.Test
import kotlin.test.assertEquals

class EpochTests {


    @Test
    fun nowTest() {
        //NSDate ?
        val now = NSDate.now()
        val millis = (now.timeIntervalSince1970() * 1000).toLong()
        println(millis)

        assertEquals(millis.toString().length, 13)
    }
}
