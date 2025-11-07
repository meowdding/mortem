package me.owdding.mortem.utils

import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import kotlinx.coroutines.runBlocking
import me.owdding.mortem.Mortem
import me.owdding.mortem.generated.MortemCodecs
import org.joml.Vector2i
import org.joml.times
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent
import tech.thatgravyboat.skyblockapi.utils.json.Json
import tech.thatgravyboat.skyblockapi.utils.json.Json.readJson
import tech.thatgravyboat.skyblockapi.utils.json.Json.toDataOrThrow
import java.nio.file.Files
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

object Utils {

    internal fun SkyBlockEvent.post() = this.post(SkyBlockAPI.eventBus)

    @Suppress("UNCHECKED_CAST")
    fun <From, To> From.unsafeCast() = (this as To)

    val vectorZeroZero = Vector2i(0, 0)
    val vectorZeroOne = Vector2i(0, 1)
    val vectorOneZero = Vector2i(1, 0)
    val vectorOneOne = Vector2i(1, 1)
    val vectorZeroTwo = vectorZeroOne * 2
    val vectorTwoZero = vectorOneZero * 2
    val vectorTwoTwo = vectorOneOne * 2

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T : Any> loadFromRepo(file: String): T? = runBlocking {
        try {
            val json = Mortem.SELF.findPath("repo/$file.json").orElseThrow()?.let(Files::readString)?.readJson<JsonElement>() ?: return@runBlocking null
            if (T::class == JsonElement::class) {
                return@runBlocking json as T
            }
            return@runBlocking Json.gson.fromJson(json, typeOf<T>().javaType)
        } catch (e: Exception) {
            Mortem.error("Failed to load $file from repo", e)
            null
        }
    }
    internal inline fun <reified T : Any> loadRepoData(file: String): T {
        return loadRepoData<T, T>(file) { it }
    }

    internal inline fun <reified T : Any, B : Any> loadRepoData(file: String, modifier: (Codec<T>) -> Codec<B>): B {
        return loadFromRepo<JsonElement>(file).toDataOrThrow(MortemCodecs.getCodec<T>().let(modifier))
    }

    internal inline fun <B : Any> loadRepoData(file: String, supplier: () -> Codec<B>): B {
        return loadFromRepo<JsonElement>(file).toDataOrThrow(supplier())
    }

    internal fun <B : Any> loadRepoData(file: String, codec: Codec<B>): B {
        return loadFromRepo<JsonElement>(file).toDataOrThrow(codec)
    }
}
