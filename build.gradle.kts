@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    idea
    id("net.fabricmc.fabric-loom")
    kotlin("jvm") version "2.4.0"
    //id("org.jetbrains.kotlinx.kover") version "0.9.8"
    alias(libs.plugins.ksp)
    alias(libs.plugins.meowdding.auto.mixins)
    `versioned-catalogues`
    alias(libs.plugins.meowdding.resources)
}

repositories {
    fun scopedMaven(url: String, vararg paths: String) = maven(url) { content { paths.forEach(::includeGroupAndSubgroups) } }

    scopedMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    scopedMaven("https://repo.hypixel.net/repository/Hypixel", "net.hypixel")
    scopedMaven("https://maven.parchmentmc.org/", "org.parchmentmc")
    scopedMaven("https://api.modrinth.com/maven", "maven.modrinth")
    scopedMaven("https://maven.teamresourceful.com/repository/maven-public/", "earth.terrarium", "com.teamresourceful", "tech.thatgravyboat", "me.owdding")
    scopedMaven("https://maven.nucleoid.xyz/", "eu.pb4")
    mavenCentral()
    mavenLocal()
}

dependencies {
    minecraft(versionedCatalog["minecraft"])

    api(libs.skyblockapi) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${stonecutter.current.version}") }
    }
    include(libs.skyblockapi) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${stonecutter.current.version}") }
    }
    api(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${stonecutter.current.version}") }
    }
    include(libs.meowdding.lib) {
        capabilities { requireCapability("me.owdding.meowdding-lib:meowdding-lib-${stonecutter.current.version}") }
    }


    includeImplementation(libs.meowdding.remote.repo)
    includeImplementation(versionedCatalog["placeholders"])
    implementation(libs.fabric.loader)
    implementation(libs.repo.lib)
    implementation(libs.fabric.language.kotlin)
    implementation(versionedCatalog["fabric.api"])
    includeImplementation(versionedCatalog["resourceful.lib"])
    includeImplementation(versionedCatalog["olympus"])
    includeImplementation(versionedCatalog["resourceful.config"])
    includeImplementation(versionedCatalog["resourceful.config.kotlin"])
    compileOnly(libs.meowdding.ktmodules)
    compileOnly(libs.meowdding.ktcodecs)
    ksp(libs.meowdding.ktmodules)
    ksp(libs.meowdding.ktcodecs)

    runtimeOnly(libs.devauth)
}

fun DependencyHandlerScope.includeImplementation(dep: Any) {
    include(dep)
    implementation(dep)
}

val mcVersion = stonecutter.current.version.replace(".", "")
loom {
    runConfigs["client"].apply {
        ideConfigGenerated(true)
        runDir = "../../run"
        vmArg("-Dfabric.modsFolder=" + '"' + rootProject.projectDir.resolve("run/${mcVersion}Mods").absolutePath + '"')
    }

    val accessWidenerFile = project.file("mortem.accesswidener")
    if (accessWidenerFile.exists()) {
        accessWidenerPath.set(accessWidenerFile)
    }
}

compactingResources {
    basePath = "repo"
    pathDirectory = "../../src"

    configureTask(tasks.named<AbstractCopyTask>("processResources").get())
    compactToArray("rooms")
}

ksp {
    arg("meowdding.project_name", "mortem")
    arg("meowdding.package", "me.owdding.mortem.generated")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        optIn.add("kotlin.time.ExperimentalTime")

        freeCompilerArgs.add("-Xname-based-destructuring=complete")
        freeCompilerArgs.add("-Xexplicit-context-arguments")
        freeCompilerArgs.add("-Xcollection-literals")
        freeCompilerArgs.add("-Xnullability-annotations=@org.jspecify.annotations:warn")
    }

    incremental = false
}

tasks.processResources {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand(mapOf(
            "version" to version,
            "minecraft" to versionedCatalog.versions["minecraft"]
        ))
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true

        excludeDirs.add(file("run"))
    }
}

autoMixins {
    mixinPackage = "me.owdding.mortem.mixins"
    projectName = "mortem"
    //mixinExtrasVersion = "0.5.0"
}

/*
kover {
    reports {
        filters {
            excludes {
                packages("me.owdding.mortem.generated")
                annotatedBy("me.owdding.mortem.IgnoreCoverage")
            }
        }
        total {
            html {
                title = "Mortem coverage"
                onCheck = false
                charset = "UTF-8"
                htmlDir = project.layout.buildDirectory.dir("coverage")
            }
        }
    }
}
*/
