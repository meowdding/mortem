package me.owdding.mortem.utils

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.owdding.ktcodecs.IncludedCodec
import me.owdding.mortem.utils.extensions.mutableCopy
import net.minecraft.Util
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector3ic

object PosCodecs {

    val vec3: Codec<Vec3> = createCodec<Vec3, Double>(
        ::Vec3,
        Codec.DOUBLE,
        { toDouble() },
        { x },
        { y },
        { z },
    )

    @IncludedCodec
    val aabb: Codec<AABB> = Codec.withAlternative(
        Codec.DOUBLE.listOf().comapFlatMap(
            { list -> Util.fixedSize(list, 6).map { AABB(it[0], it[1], it[2], it[3], it[4], it[5]) } },
            { listOf(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ) },
        ),
        RecordCodecBuilder.create {
            it.group(
                vec3.fieldOf("min").forGetter(AABB::getMinPosition),
                vec3.fieldOf("max").forGetter(AABB::getMaxPosition),
            ).apply(it, ::AABB)
        }
    )

    @IncludedCodec
    val vector3iCodec: Codec<Vector3i> = createCodec(
        ::Vector3i,
        Codec.INT,
        String::toInt,
        { x }, { y }, { z },
    )

    @IncludedCodec
    val vector3icCodec: Codec<Vector3ic> = vector3iCodec.xmap({ it }, { it.mutableCopy() })

    fun <VecType : Any, NumberType : Number> createCodec(
        constructor: (NumberType, NumberType, NumberType) -> VecType,
        numberCodec: Codec<NumberType>,
        toNumber: String.() -> NumberType,
        first: VecType.() -> NumberType,
        second: VecType.() -> NumberType,
        third: VecType.() -> NumberType,
        asString: NumberType.() -> String = Number::toString,
    ): Codec<VecType> {
        return Codec.either(
            Codec.STRING.xmap(
                {
                    it.split(":").let { elements ->
                        constructor(elements[0].toNumber(), elements[1].toNumber(), elements[2].toNumber())
                    }
                },
                { "${it.first().asString()}:${it.second().asString()}:${it.third().asString()}" },
            ),
            Codec.either(
                numberCodec.listOf().comapFlatMap(
                    { list -> Util.fixedSize(list, 3).map { constructor(it[0], it[1], it[2]) } },
                    { listOf(it.first(), it.second(), it.third()) },
                ),
                RecordCodecBuilder.create<VecType> {
                    it.group(
                        numberCodec.fieldOf("x").forGetter(first),
                        numberCodec.fieldOf("y").forGetter(second),
                        numberCodec.fieldOf("z").forGetter(third),
                    ).apply(it, constructor)
                },
            ).xmap(Either<VecType, VecType>::unwrap) { Either.left(it) },
        ).xmap(Either<Vector3f, Vector3f>::unwrap) { Either.left(it) }
    }

}
