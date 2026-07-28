package me.owdding.mortem.config.category.catacombs

import com.teamresourceful.resourcefulconfigkt.api.CategoryKt
import me.owdding.mortem.config.AutoTranslated
import me.owdding.mortem.config.category.CommandsConfig
import net.minecraft.util.EasingType
import kotlin.time.Duration.Companion.milliseconds

object CatacombsMapConfig : CategoryKt("map"), AutoTranslated {
    override val translationBase: String = "mortem.config.catacombs.map"
    override val name = Translated(translationBase)

    val enable by autoBoolean(true)

    val roomWidth by autoInt(20)
    val roomHeight by autoInt(20)

    val hallwayHeight by autoInt(4)
    val hallwayWidth by autoInt(4)

    val doorWidth by autoInt(4)
    val headSize by autoInt(4)

    val xInterpolationTime by transform(autoLong(100), { it.inWholeMilliseconds }, { it.milliseconds })
    val xEasingFunction by transform(autoEnum(EasingFunctions.LINEAR), { function -> EasingFunctions.entries.first { it.easingType == function } }, { it.easingType })
    val yInterpolationTime by transform(autoLong(100), { it.inWholeMilliseconds }, { it.milliseconds })
    val yEasingFunction by transform(autoEnum(EasingFunctions.LINEAR), { function -> EasingFunctions.entries.first { it.easingType == function } }, { it.easingType })
}

enum class EasingFunctions(val easingType: EasingType) {
    CONSTANT(EasingType.CONSTANT),
    LINEAR(EasingType.LINEAR),
    IN_BACK(EasingType.IN_BACK),
    IN_BOUNCE(EasingType.IN_BOUNCE),
    IN_CIRC(EasingType.IN_CIRC),
    IN_CUBIC(EasingType.IN_CUBIC),
    IN_ELASTIC(EasingType.IN_ELASTIC),
    IN_EXPO(EasingType.IN_EXPO),
    IN_QUAD(EasingType.IN_QUAD),
    IN_QUART(EasingType.IN_QUART),
    IN_QUINT(EasingType.IN_QUINT),
    IN_SINE(EasingType.IN_SINE),
    IN_OUT_BACK(EasingType.IN_OUT_BACK),
    IN_OUT_BOUNCE(EasingType.IN_OUT_BOUNCE),
    IN_OUT_CIRC(EasingType.IN_OUT_CIRC),
    IN_OUT_CUBIC(EasingType.IN_OUT_CUBIC),
    IN_OUT_ELASTIC(EasingType.IN_OUT_ELASTIC),
    IN_OUT_EXPO(EasingType.IN_OUT_EXPO),
    IN_OUT_QUAD(EasingType.IN_OUT_QUAD),
    IN_OUT_QUART(EasingType.IN_OUT_QUART),
    IN_OUT_QUINT(EasingType.IN_OUT_QUINT),
    IN_OUT_SINE(EasingType.IN_OUT_SINE),
    OUT_BACK(EasingType.OUT_BACK),
    OUT_BOUNCE(EasingType.OUT_BOUNCE),
    OUT_CIRC(EasingType.OUT_CIRC),
    OUT_CUBIC(EasingType.OUT_CUBIC),
    OUT_ELASTIC(EasingType.OUT_ELASTIC),
    OUT_EXPO(EasingType.OUT_EXPO),
    OUT_QUAD(EasingType.OUT_QUAD),
    OUT_QUART(EasingType.OUT_QUART),
    OUT_QUINT(EasingType.OUT_QUINT),
    OUT_SINE(EasingType.OUT_SINE),
}
