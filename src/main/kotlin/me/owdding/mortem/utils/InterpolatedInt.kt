package me.owdding.mortem.utils

import net.minecraft.util.EasingType
import tech.thatgravyboat.skyblockapi.utils.extentions.currentInstant
import tech.thatgravyboat.skyblockapi.utils.extentions.since
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Instant

class InterpolatedInt<Type>(val easingType: () -> EasingType, val time: () -> Duration) : ReadWriteProperty<Type, Int> {

    var interpolationStart: Instant = currentInstant()
    var start: Int = 0
    var target: Int = 0

    fun lerp(progress: Float, start: Int, end: Int) = start + (progress * (end - start)).toInt()
    override fun getValue(thisRef: Type, property: KProperty<*>): Int {
        val time = time()
        val since = interpolationStart.since()
        if (since > time) {
            if (start != target) start = target
            return start
        }

        val delta = since / time
        return lerp(easingType().apply(delta.toFloat()), start, target)
    }

    override fun setValue(thisRef: Type, property: KProperty<*>, value: Int) {
        start = getValue(thisRef, property)
        target = value
        interpolationStart = currentInstant()
    }

}
