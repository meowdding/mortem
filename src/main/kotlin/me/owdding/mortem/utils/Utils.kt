package me.owdding.mortem.utils

import com.google.gson.JsonElement
import com.mojang.serialization.Codec
import kotlinx.coroutines.runBlocking
import me.owdding.mortem.Mortem
import me.owdding.mortem.generated.MortemCodecs
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.SkullBlockEntity
import org.joml.Vector2i
import org.joml.Vector2ic
import org.joml.times
import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.SkyBlockEvent
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.utils.json.Json
import tech.thatgravyboat.skyblockapi.utils.json.Json.readJson
import tech.thatgravyboat.skyblockapi.utils.json.Json.toDataOrThrow
import java.nio.file.Files
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

object Utils {

    internal fun SkyBlockEvent.post() = this.post(SkyBlockAPI.eventBus)

    fun McLevel.getSkullTexture(pos: BlockPos): String? {
        return level.getBlockEntity(pos, BlockEntityType.SKULL).getOrNull()?.getTexture()
    }

    fun SkullBlockEntity.getTexture(): String? {
        //? >=1.21.10 {
        return this.ownerProfile?.partialProfile()?.id?.toString()
        //?} else
        /*return ownerProfile?.id?.get()?.toString()*/
    }

    inline fun <T, R : Comparable<R>> Iterable<T>.minByWithOrNull(selector: (T) -> R): Pair<T, R>? {
        val iterator = iterator()
        if (!iterator.hasNext()) return null
        var minElem = iterator.next()
        var minValue = selector(minElem)
        if (!iterator.hasNext()) return minElem to minValue
        do {
            val e = iterator.next()
            val v = selector(e)
            if (minValue > v) {
                minElem = e
                minValue = v
            }
        } while (iterator.hasNext())
        return minElem to minValue
    }

    @Suppress("UNCHECKED_CAST")
    fun <To> Any.unsafeCast() = (this as To)

    val vectorZeroZero: Vector2ic = Vector2i(0, 0)
    val vectorZeroOne: Vector2ic = Vector2i(0, 1)
    val vectorOneZero: Vector2ic = Vector2i(1, 0)
    val vectorOneOne: Vector2ic = Vector2i(1, 1)
    val vectorZeroTwo: Vector2ic = vectorZeroOne * 2
    val vectorTwoZero: Vector2ic = vectorOneZero * 2
    val vectorTwoTwo: Vector2ic = vectorOneOne * 2

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
