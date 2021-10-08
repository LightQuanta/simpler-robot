plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") // version "1.5.31"
    id("org.jetbrains.dokka")
}

group = "love.forte.simply-robot"
version = "3.0.0-preview"

repositories {
    mavenCentral()
}


kotlin {
    // 严格模式
    explicitApi()

    // Jvm
    jvm("jvm") {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
            attribute(SimbotAttributes.MODULE_NAME, "api")
        }
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns.all {
            executionTask.configure {
                useJUnit()
                // useJUnitPlatform()
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
            }


            // Set src dir like xxx/main/kotlin, xxx/test/kotlin
            val (target, source) = name.toTargetAndSource()
            kotlin.setSrcDirs(project.srcList(source, target))
        }


        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.2")

            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.3.0")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
            }
        }
    }

    tasks.dokkaHtml {
        val root = rootProject.rootDir
        outputDirectory.set(File(root, "dokkaOutput/${project.name}"))
    }
}
