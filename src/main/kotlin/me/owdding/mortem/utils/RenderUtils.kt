package me.owdding.mortem.utils

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.util.ARGB
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent
import java.util.*

// TODO: move to mlib
object RenderUtils {

    fun RenderWorldEvent.renderOutlineBox(
        position: AABB,
        color: Int,
        lineWidth: Float = 1f
    ) {
        atCamera {
            val prev = RenderSystem.getShaderLineWidth()
            RenderSystem.lineWidth(lineWidth)
            ShapeRenderer.renderLineBox(
                /*? >=1.21.10 {*/ poseStack.last(), /*?} else*/ /* poseStack,*/
                buffer.getBuffer(NO_DEPTH_LINES),
                position.minX - 0.005, position.minY - 0.005, position.minZ - 0.005,
                position.maxX + 0.005, position.maxY + 0.005, position.maxZ + 0.005,
                ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color),
            )
            RenderSystem.lineWidth(prev)
        }
    }

    val NO_DEPTH_LINES: RenderType = RenderType.create(
        "mortem/no_depth_lines",
        1536,
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation("pipeline/lines")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build(),
        RenderType.CompositeState.builder()
            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
            .createCompositeState(false),
    )
}
