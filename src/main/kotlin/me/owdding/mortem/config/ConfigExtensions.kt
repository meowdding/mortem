package me.owdding.mortem.config

import com.teamresourceful.resourcefulconfigkt.api.*
import com.teamresourceful.resourcefulconfigkt.api.builders.CategoryBuilder
import com.teamresourceful.resourcefulconfigkt.api.builders.SeparatorBuilder
import tech.thatgravyboat.skyblockapi.helpers.McClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.toTimeUnit

fun <T> CategoryBuilder.observable(entry: ConfigDelegateProvider<RConfigKtEntry<T>>, onChange: () -> Unit) =
    this.observable(entry) { _, _ -> onChange() }

fun CategoryBuilder.requiresChunkRebuild(entry: ConfigDelegateProvider<RConfigKtEntry<Boolean>>) = observable(entry) {
    runCatching { McClient.self.levelRenderer.allChanged() }
}

var SeparatorBuilder.translation: String
    get() = ""
    set(value) {
        this.title = value
        this.description = "$value.desc"
    }

fun CategoryBuilder.category(category: CategoryKt, init: CategoryKt.() -> Unit) {
    category(category)
    category.init()
}

fun CategoryBuilder.separator(translation: String) = this.separator { this.translation = translation }

fun ConfigDelegateProvider<RConfigKtEntry<Long>>.duration(unit: DurationUnit): CachedTransformedEntry<Long, Duration> {
    val timeUnit = unit.toTimeUnit()
    return cachedTransform({ it.toLong(unit) }) { timeUnit.toMillis(it).milliseconds }
}

fun <T, R> ConfigDelegateProvider<RConfigKtEntry<T>>.cachedTransform(from: (R) -> T, to: (T) -> R) = CachedTransformedEntry(this, from, to)

fun <T, R> ConfigDelegateProvider<RConfigKtEntry<T>>.transform(from: (R) -> T, to: (T) -> R) = TransformedEntry(this, from, to)

fun <T> ConfigDelegateProvider<RConfigKtEntry<T>>.observable(onChange: (T, T) -> Unit) = ObservableEntry(this, onChange)
