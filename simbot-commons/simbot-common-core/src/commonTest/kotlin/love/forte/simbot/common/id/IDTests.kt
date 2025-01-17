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

package love.forte.simbot.common.id

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import love.forte.simbot.common.id.IntID.Companion.ID
import love.forte.simbot.common.id.LongID.Companion.ID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.common.id.UIntID.Companion.ID
import love.forte.simbot.common.id.ULongID.Companion.ID
import love.forte.simbot.common.id.UUID.Companion.UUID
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.random.nextULong
import kotlin.test.*

class IDTests {

    @Test
    fun uuidGenerateTest() {
        val id = UUID.random()
        val id2 = id.toString().UUID

        assertEquals(id, id2)
        assertEquals(id as ID, id2.toString().ID as ID)
        assertEquals(id as ID, id2.copy().toString().ID as ID)
        assertTrue(id.equalsExact(id2))
        assertTrue(id.equalsExact(id.copy()))
        assertTrue(id.equalsExact(id2.copy()))
    }

    @Test
    fun equalsTest() {
        assertEquals("1".ID as ID, 1.ID as ID)
    }

    @Test
    fun serializerTest() {
        val json = Json {
            isLenient = true
        }

        val i = Random.nextInt()
        val l = Random.nextLong()
        val ui = Random.nextUInt()
        val ul = Random.nextULong()

        assertEquals(json.encodeToString(IntID.serializer(), i.ID), i.toString())
        assertEquals(json.encodeToString(LongID.serializer(), l.ID), l.toString())
        assertEquals(json.encodeToString(UIntID.serializer(), ui.ID), ui.toString())
        assertEquals(json.encodeToString(ULongID.serializer(), ul.ID), ul.toString())
    }

    @Test
    fun numericalIDTransformTests() {
        with(1.ID) {
            assertEquals(1L, toLong())
            assertEquals(1L.ID, toLongID())
            assertSame(this, toIntID())
        }
        with(1L.ID) {
            assertEquals(1, toInt())
            assertSame(this, toLongID())
            assertEquals(1.ID, toIntID())
        }
        with("1".ID) {
            assertEquals(1, toInt())
            assertEquals(1L, toLong())
            assertEquals(1L.ID, toLongID())
            assertEquals(1.ID, toIntID())
        }
        with(1u.ID) {
            assertEquals(1u, toUInt())
            val ul: ULong = 1u
            assertEquals(ul.ID, toULongID())
            assertEquals(1.ID, toIntID())
            assertSame(this, toUIntID())
        }
        with(uLongIDOf(1u)) {
            assertEquals(1, toInt())
            assertEquals(1L.ID, toLongID())
            assertSame(this, toULongID())
            assertEquals(1.ID, toIntID())
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun uuidSerializerTests() {
        val mv = 111L
        val lv = 222L
        val uuid = UUID.from(mv, lv)
        val jsonString = Json.encodeToString(UUID.StructureSerializer, uuid)
        assertEquals("""{"mostSignificantBits":$mv,"leastSignificantBits":$lv}""", jsonString)
        val decodedUUID = Json.decodeFromString(
            UUID.StructureSerializer,
            """{"mostSignificantBits":$mv,"leastSignificantBits":$lv}"""
        )
        assertEquals(uuid, decodedUUID)

        val err = assertFails {
            Json.decodeFromString(UUID.StructureSerializer, """{"leastSignificantBits":$lv}""")
        }
        assertIs<MissingFieldException>(err)
    }
}
