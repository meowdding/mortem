package me.owdding.mortem.features.secrets

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import me.owdding.ktmodules.Module
import me.owdding.lib.utils.RenderUtils.renderBox
import me.owdding.lib.utils.RenderUtils.renderTextInWorld
import me.owdding.mortem.core.catacombs.CatacombsManager
import me.owdding.mortem.core.catacombs.REDSTONE_KEY
import me.owdding.mortem.core.catacombs.WITHER_ESSENCE
import me.owdding.mortem.core.catacombs.nodes.RoomNode
import me.owdding.mortem.core.catacombs.secrets.*
import me.owdding.mortem.core.event.MortemRegisterCommandsEvent
import me.owdding.mortem.core.event.catacomb.CatacombRoomChangeEvent
import me.owdding.mortem.utils.CommandExceptions
import me.owdding.mortem.utils.CommandExceptions.assertInCatacombs
import me.owdding.mortem.utils.CommandExceptions.assertInCatacombsRoom
import me.owdding.mortem.utils.CommandExceptions.getCatacomb
import me.owdding.mortem.utils.CommandExceptions.getCatacombsRoom
import me.owdding.mortem.utils.RenderUtils.renderOutlineBox
import me.owdding.mortem.utils.Utils.getTexture
import me.owdding.mortem.utils.Utils.minByWithOrNull
import me.owdding.mortem.utils.Utils.unsafeCast
import me.owdding.mortem.utils.colors.CatppuccinColors
import me.owdding.mortem.utils.extensions.mutableCopy
import me.owdding.mortem.utils.extensions.sendWithPrefix
import me.owdding.mortem.utils.extensions.toBlockPos
import me.owdding.mortem.utils.extensions.toVector3ic
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Vector3ic
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.base.predicates.OnlyIn
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.location.ServerDisconnectEvent
import tech.thatgravyboat.skyblockapi.api.events.misc.RegisterCommandsEvent.Companion.argument
import tech.thatgravyboat.skyblockapi.api.events.render.RenderWorldEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.helpers.McLevel
import tech.thatgravyboat.skyblockapi.utils.command.EnumArgument
import tech.thatgravyboat.skyblockapi.utils.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextBuilder.append
import tech.thatgravyboat.skyblockapi.utils.text.TextColor
import kotlin.jvm.optionals.getOrNull

@Module
object DungeonSecretsEditor {

    var editing: Boolean = false
        private set

    private val RoomNode.secretData: CatacombsSecretRoom?
        get() = backingData?.id?.let(CatacombsSecretManager::getOrPut)

    var editingSecret: CatacombsSecret? = null
        private set

    private var groupingSecrets = false
    private val selectedSecrets = mutableSetOf<CatacombsSecret>()

    fun lookedAtPos(): Vector3ic? {
        val hit = McClient.self.cameraEntity?.pick(70.0, 0f, false) ?: return null
        if (hit.type != HitResult.Type.BLOCK) return null
        return hit.unsafeCast<BlockHitResult>().blockPos.toVector3ic()
    }

    private fun CatacombsSecret.tryAutoModify(onlyAABB: Boolean = false) {
        when (this) {
            is RedstoneKeySecret -> {
                if (onlyAABB) return
                val room = CatacombsManager.catacomb?.lastRoom ?: return
                val state = McLevel[room.roomToWorld(this.pos).toBlockPos()]
                this.pickUp = state.block == Blocks.PLAYER_HEAD
            }

            is LeverSecret -> {
                val room = CatacombsManager.catacomb?.lastRoom ?: return
                val realPos = room.roomToWorld(this.pos).toBlockPos()
                val state = McLevel[realPos]
                val shape = state.getShape(McLevel.self, realPos)
                if (!shape.isEmpty) this.aabb = shape.bounds()
            }

            else -> return
        }
    }

    private fun assertEditing() {
        if (!editing) throw CommandExceptions.create("Not editing secrets!")
    }
    private fun assertGrouping() {
        if (!groupingSecrets) throw CommandExceptions.create("Not grouping secrets!")
    }
    private fun currentSecret(): CatacombsSecret {
        return editingSecret ?: throw CommandExceptions.create("Theres no secret selected!")
    }

    private fun guessType(pos: BlockPos, state: BlockState): Pair<BlockPos, CatacombsSecretType> {
        return when (state.block) {
            Blocks.CHEST, Blocks.TRAPPED_CHEST -> pos to CHEST
            Blocks.PLAYER_HEAD -> {
                val texture = McLevel.level.getBlockEntity(pos, BlockEntityType.SKULL).getOrNull()?.getTexture()
                pos to when (texture) {
                    WITHER_ESSENCE -> ESSENCE
                    REDSTONE_KEY -> CatacombsSecretType.REDSTONE_KEY
                    else -> ESSENCE
                }
            }

            Blocks.LEVER -> pos to LEVER
            else -> {
                val vec3 = Vec3(pos)
                val aabb = AABB(pos).inflate(4.0)
                val closestItem = McLevel.level.getEntitiesOfClass(ItemEntity::class.java, aabb).minByWithOrNull {
                    it.distanceToSqr(vec3)
                }?.takeIf { it.second < 25 }?.first
                if (closestItem != null) {
                    return closestItem.blockPosition() to ITEM
                }

                // TODO: add item/bat secret if recently collected one of them near it
                pos to NONE
            }
        }
    }

    @Subscription
    fun onCommand(event: MortemRegisterCommandsEvent) {
        event.register("dev secrets edit") {
            thenCallback("toggle") {
                assertInCatacombs()
                editing = !editing
                if (!editing) reset()
                Text.of("Secrets editing ") {
                    if (editing) append("Enabled", TextColor.GREEN)
                    else append("Disabled", TextColor.RED)
                }.sendWithPrefix()
            }
            then("create") {
                thenCallback("type", EnumArgument<CatacombsSecretType>()) {
                    val catacomb = getCatacomb()
                    assertEditing()
                    val room = getCatacombsRoom()
                    val type = argument<CatacombsSecretType>("type")
                    val pos = lookedAtPos() ?: throw CommandExceptions.create("Not looking at any position!")
                    val gridPos = CatacombsManager.worldPosToGridPos(pos.toBlockPos())
                    if (catacomb[gridPos] != room) throw CommandExceptions.create("Position is in a different room!")
                    val roomPos = room.worldToRoom(pos)
                    val secret = type.constructor(roomPos)
                    secret.tryAutoModify()
                    editingSecret = secret
                    room.secretData?.addSecret(secret)
                    Text.of("Added $type secret to ${room.backingData?.name ?: "Unknown"}!").sendWithPrefix()
                }
                callback {
                    val catacomb = getCatacomb()
                    assertEditing()
                    val room = getCatacombsRoom()
                    val originalPos = lookedAtPos() ?: throw CommandExceptions.create("Not looking at any position!")
                    val originalBlockPos = originalPos.toBlockPos()
                    val (blockPos, type) = guessType(originalBlockPos, McLevel[originalBlockPos])
                    val gridPos = CatacombsManager.worldPosToGridPos(blockPos)
                    if (catacomb[gridPos] != room) throw CommandExceptions.create("Position is in a different room!")
                    val pos = blockPos.toVector3ic()
                    val roomPos = room.worldToRoom(pos)
                    val secret = type.constructor(roomPos)
                    secret.tryAutoModify()
                    editingSecret = secret
                    room.secretData?.addSecret(secret)
                    Text.of("Added $type secret to ${room.backingData?.name ?: "Unknown"}!").sendWithPrefix()
                }
            }
            then("type") {
                thenCallback("type", EnumArgument<CatacombsSecretType>()) {
                    assertInCatacombs()
                    assertEditing()
                    val room = getCatacombsRoom()
                    val secret = currentSecret()
                    val type = argument<CatacombsSecretType>("type")
                    if (secret.type == type) throw CommandExceptions.create("Secret already has type $type!", false)
                    val newSecret = type.constructor(secret.pos)
                    newSecret.tryAutoModify()
                    editingSecret = newSecret
                    room.secretData?.replaceSecret(secret, newSecret)
                    Text.of("Changed secret type to $type!").sendWithPrefix()
                }
                thenCallback("guess") {
                    assertInCatacombs()
                    assertEditing()
                    val room = getCatacombsRoom()
                    val secret = currentSecret()
                    val worldPos = room.roomToWorld(secret.pos).toBlockPos()
                    val (blockPos, type) = guessType(worldPos, McLevel[worldPos])
                    val roomPos = room.worldToRoom(blockPos.toVector3ic())
                    if (type == secret.type && roomPos == secret.pos) return@thenCallback
                    val newSecret = type.constructor(roomPos)
                    newSecret.tryAutoModify()
                    editingSecret = newSecret
                    room.secretData?.replaceSecret(secret, newSecret)
                    Text.of("Added $type secret to ${room.backingData?.name ?: "Unknown"}!").sendWithPrefix()
                }
            }
            then("move") {
                then("up") {
                    callback {
                        assertInCatacombs()
                        assertEditing()
                        val secret = currentSecret()
                        secret.pos = secret.pos.mutableCopy().add(0, 1, 0)
                        Text.of("Moved ${secret.type} secret up by 1 block").sendWithPrefix()
                    }
                    thenCallback("amount", IntegerArgumentType.integer()) {
                        assertInCatacombs()
                        assertEditing()
                        assertInCatacombsRoom()
                        val secret = currentSecret()
                        val amount = argument<Int>("amount")
                        secret.pos = secret.pos.mutableCopy().add(0, amount, 0)
                        secret.tryAutoModify(true)
                        Text.of("Moved ${secret.type} secret up by $amount blocks").sendWithPrefix()
                    }
                }
                then("down") {
                    callback {
                        assertInCatacombs()
                        assertEditing()
                        assertInCatacombsRoom()
                        val secret = currentSecret()
                        secret.pos = secret.pos.mutableCopy().sub(0, 1, 0)
                        secret.tryAutoModify(true)
                        Text.of("Moved ${secret.type} secret down by 1 block").sendWithPrefix()
                    }
                    thenCallback("amount", IntegerArgumentType.integer()) {
                        assertInCatacombs()
                        assertEditing()
                        val secret = currentSecret()
                        val amount = argument<Int>("amount")
                        secret.pos = secret.pos.mutableCopy().sub(0, amount, 0)
                        secret.tryAutoModify(true)
                        Text.of("Moved ${secret.type} secret down by $amount blocks").sendWithPrefix()
                    }
                }
                thenCallback("x amount", IntegerArgumentType.integer()) {
                    assertInCatacombs()
                    assertEditing()
                    val room = getCatacombsRoom()
                    val secret = currentSecret()
                    val x = argument<Int>("amount")
                    if (x == 0) throw CommandExceptions.create("Amount cannot be zero!")
                    secret.pos = room.worldToRoom(room.roomToWorld(secret.pos).mutableCopy().add(x, 0, 0))
                    secret.tryAutoModify(true)
                    Text.of("Moved ${secret.type} secret in x axis by $x blocks").sendWithPrefix()
                }
                thenCallback("z amount", IntegerArgumentType.integer()) {
                    assertInCatacombs()
                    assertEditing()
                    val room = getCatacombsRoom()
                    val secret = currentSecret()
                    val z = argument<Int>("amount")
                    if (z == 0) throw CommandExceptions.create("Amount cannot be zero!")
                    secret.pos = room.worldToRoom(room.roomToWorld(secret.pos).mutableCopy().add(0, 0, z))
                    secret.tryAutoModify(true)
                    Text.of("Moved ${secret.type} secret in z axis by $z blocks").sendWithPrefix()
                }
            }
            thenCallback("set redstone_key pickup value", BoolArgumentType.bool()) {
                assertInCatacombs()
                assertEditing()
                assertInCatacombsRoom()
                val secret = currentSecret()
                if (secret.type != CatacombsSecretType.REDSTONE_KEY) throw CommandExceptions.create("Only redstone keys waypoints have \"pickup\" value!")
                val value = argument<Boolean>("value")
                val redstoneKey = secret.unsafeCast<RedstoneKeySecret>()
                if (redstoneKey.pickUp == value) throw CommandExceptions.create("Redstone key already had pickup value as $value!", false)
                redstoneKey.pickUp = value
                Text.of("Set ${secret.type} secret pickup value to $value").sendWithPrefix()
            }
            then("remove") {
                callback {
                    assertInCatacombs()
                    assertEditing()
                    val room = getCatacombsRoom()
                    val secret = currentSecret()
                    room.secretData?.removeSecret(secret)
                    Text.of("Removed ${secret.type} secret from ${room.backingData?.name ?: "Unknown"}").sendWithPrefix()
                }
                thenCallback("all") {
                    assertInCatacombs()
                    assertEditing()
                    val room = getCatacombsRoom()
                    room.secretData?.let {
                        it.secretGroups.clear()
                        it.markDirty()
                    }
                    Text.of("Removed all secrets from room ${room.backingData?.name ?: "Unknown"}").sendWithPrefix()
                }
            }
            thenCallback("select") {
                assertInCatacombs()
                assertEditing()
                val room = getCatacombsRoom()
                val secret = room.getFirstIntersected()
                editingSecret = secret
                if (secret == null) throw CommandExceptions.create("Not looking at any waypoint!")
                Text.of("Selected ${secret.type} secret!")
            }
            then("group") {
                thenCallback("toggle") {
                    assertInCatacombs()
                    assertEditing()
                    groupingSecrets = !groupingSecrets
                    Text.of("Secrets grouping ") {
                        if (editing) append("Enabled", TextColor.GREEN)
                        else append("Disabled", TextColor.RED)
                    }.sendWithPrefix()
                }
                thenCallback("add") {
                    assertInCatacombs()
                    assertEditing()
                    assertGrouping()
                    val room = getCatacombsRoom()
                    val secret = room.getFirstIntersected() ?: throw CommandExceptions.create("Not looking at any waypoint!")
                    selectedSecrets.add(secret)
                    Text.of("Selected ${secret.type} secret to group!").sendWithPrefix()
                }
                thenCallback("remove") {
                    assertInCatacombs()
                    assertEditing()
                    assertGrouping()
                    val room = getCatacombsRoom()
                    val secret = room.getFirstIntersected() ?: throw CommandExceptions.create("Not looking at any waypoint!")
                    selectedSecrets.remove(secret)
                    Text.of("Unselected ${secret.type} secret to group!").sendWithPrefix()
                }
                thenCallback("clear") {
                    assertInCatacombs()
                    assertEditing()
                    assertGrouping()
                    assertInCatacombsRoom()
                    selectedSecrets.clear()
                    Text.of("Cleared selected secrets!").sendWithPrefix()
                }
                thenCallback("finish") {
                    assertInCatacombs()
                    assertEditing()
                    assertGrouping()
                    val room = getCatacombsRoom()
                    val secrets = selectedSecrets
                    if (secrets.isEmpty()) throw CommandExceptions.create("No secrets selected!")
                    val data = room.secretData ?: throw CommandExceptions.create("No secret data!")
                    secrets.forEach(data::removeSecret)
                    val group = CatacombsSecretGroup(secrets.toMutableList())
                    data.secretGroups.add(group)
                    data.markDirty()
                    Text.of("Grouped ${secrets.size} secrets!").sendWithPrefix()
                    selectedSecrets.clear()
                }
            }
        }
    }

    @Subscription(ServerChangeEvent::class, ServerDisconnectEvent::class)
    fun reset() {
        editingSecret = null
        groupingSecrets = false
        selectedSecrets.clear()
    }

    @Subscription
    @OnlyIn(THE_CATACOMBS)
    private fun RenderWorldEvent.onRenderWorld() {
        val catacomb = CatacombsManager.catacomb ?: return
        val room = catacomb.lastRoom ?: return
        val data = room.secretData ?: return
        val secrets = data.secretGroups.flatMapIndexed { index, group -> group.secrets.map { it to index + 1 } }

        //region Render normal secrets
        for ((secret, group) in secrets) {
            val fillColor = ARGB.color(100, secret.type.getColor())
            val aabb = secret.realAABB(room)
            renderBox(aabb, fillColor)
            val textColor = ARGB.color(255, secret.type.getColor())
            renderTextInWorld(aabb.bottomCenter.add(0.0, aabb.ysize + 0.5, 0.0), secret.type.toString(), textColor)
            renderTextInWorld(aabb.bottomCenter.add(0.0, aabb.ysize + 1.0, 0.0), "GROUP $group", textColor)
        }
        //endregion

        if (!editing) return

        //region Editing secret
        val currentSecret = editingSecret
        if (currentSecret != null) {
            val aabb = currentSecret.realAABB(room)
            val color = ARGB.color(255, CatppuccinColors.Latte.lavender)
            renderOutlineBox(
                aabb.inflate(0.05),
                color,
                15f,
            )
            renderTextInWorld(aabb.center, "EDITING", color)
        }
        //endregion

        //region Render Grouping Secrets
        if (groupingSecrets) {
            for (secret in selectedSecrets) {
                val color = ARGB.color(255, CatppuccinColors.Latte.green)
                val aabb = secret.realAABB(room)
                renderOutlineBox(
                    aabb.inflate(0.05),
                    color,
                    10f,
                )
                renderTextInWorld(aabb.bottomCenter.subtract(0.0, 0.5, 0.0), "GROUPING", color)
            }
        }
        //endregion

        val pos = lookedAtPos()
        if (pos != null) {
            val blockPos = pos.toBlockPos()
            val state = McLevel[blockPos]
            val shape = state.getShape(McLevel.self, blockPos)
            val aabb = if (shape.isEmpty) AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            else shape.bounds()
            val color = ARGB.color(255, CatppuccinColors.Latte.text)
            renderOutlineBox(aabb, color, 5f)
        }
    }

    @Subscription(CatacombRoomChangeEvent::class)
    fun onCatacombRoomChange() {
        editingSecret = null
        selectedSecrets.clear()
    }

    private fun RoomNode.getFirstIntersected(): CatacombsSecret? {
        val data = secretData ?: return null
        val secrets = data.secretGroups.flatMap { it.secrets }
        val self = McClient.self.cameraEntity ?: return null
        val pos = self.eyePosition
        val rayEnd = pos.add(self.lookAngle.scale(70.0))

        return secrets.mapNotNull { secret ->
            val hit = secret.realAABB(this).clip(pos, rayEnd).getOrNull() ?: return@mapNotNull null
            secret to pos.distanceTo(hit)
        }.minByOrNull { it.second }?.first
    }

}
