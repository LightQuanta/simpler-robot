/*
 *     Copyright (c) 2021-2024. ForteScarlet.
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
rootProject.name = "simply-robot"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

include(":simbot-commons:simbot-common-annotations")
include(":simbot-commons:simbot-common-collection")
include(":simbot-commons:simbot-common-atomic")
include(":simbot-commons:simbot-common-apidefinition")
include(":simbot-commons:simbot-common-core")
include(":simbot-commons:simbot-common-suspend-runner")
include(":simbot-commons:simbot-common-stage-loop")
include(":simbot-api")
include(":simbot-test")
include(":simbot-cores:simbot-core")

include(":simbot-logger")
include(":simbot-logger-slf4j2-impl")

include(":simbot-quantcat:simbot-quantcat-annotations")
include(":simbot-quantcat:simbot-quantcat-common")

include(":simbot-cores:simbot-core-spring-boot-starter-common")
include(":simbot-cores:simbot-core-spring-boot-starter") // v3
//include(":simbot-cores:simbot-core-spring-boot-v2-starter")
