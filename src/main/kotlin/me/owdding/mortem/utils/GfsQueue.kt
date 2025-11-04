package me.owdding.mortem.utils

import me.owdding.ktmodules.Module
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.hypixel.ServerChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.time.currentInstant
import tech.thatgravyboat.skyblockapi.utils.time.since
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Module
// TODO: MLIB
//  check wether item is actually in the sack
//  combine same ids
//  last message send by player to server
//  last server switch
object GfsQueue {

    private val queue: Queue<Pair<SkyBlockId, Int>> = LinkedList()
    private var lastFetch = currentInstant()

    fun add(id: SkyBlockId, amount: Int) {
        queue.add(Pair(id, amount))
    }

    @Subscription(ServerChangeEvent::class)
    fun onServerSwap() {
        lastFetch = currentInstant().plus(2.seconds)
    }

    @Subscription
    fun onTick(event: TickEvent) {
        if (queue.isEmpty()) return
        if (lastFetch.since() <= 2.5.seconds) return

        val (id, amount) = queue.poll() ?: return

        McClient.sendCommand("/gfs ${id.skyblockId} $amount")
        lastFetch = currentInstant()
    }
}
