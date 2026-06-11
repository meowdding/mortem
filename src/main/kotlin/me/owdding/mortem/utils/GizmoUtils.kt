package me.owdding.mortem.utils

import net.minecraft.gizmos.GizmoProperties
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.helpers.McClient

object GizmoUtils {

    val renderDebugGizmos by debugToggle("debug_gizmos", "Render debug gizmos.")

    fun debugGizmo(runnable: GizmoUtils.() -> Unit) {
        if (!renderDebugGizmos) return
        runnable()
    }

    fun cuboid(aabb: AABB, style: GizmoStyle, coloredCornerStroke: Boolean = false): GizmoDsl = gizmo { Gizmos.cuboid(aabb, style, coloredCornerStroke) }

    private fun gizmo(creator: () -> GizmoProperties): GizmoDsl {
        val gizmo = GizmoDsl()
        McClient.runNextTick {
            creator().apply {
                if (gizmo.alwaysOnTop) setAlwaysOnTop()
                gizmo.persistFor?.let(::persistForMillis)
                if (gizmo.fadeOut) fadeOut()
            }
        }
        return gizmo
    }

    data class GizmoDsl(var alwaysOnTop: Boolean = false, var persistFor: Int? = null, var fadeOut: Boolean = true) : GizmoProperties {
        override fun setAlwaysOnTop(): GizmoProperties = apply {
            alwaysOnTop = true
        }

        override fun persistForMillis(milliseconds: Int): GizmoProperties = apply {
            persistFor = milliseconds
        }

        override fun fadeOut(): GizmoProperties = apply {
            fadeOut = true
        }
    }


}
