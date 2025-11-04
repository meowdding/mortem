package me.owdding.mortem.utils

import me.owdding.ktmodules.Module
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.time.TickEvent
import tech.thatgravyboat.skyblockapi.helpers.McClient
import tech.thatgravyboat.skyblockapi.utils.time.currentInstant
import tech.thatgravyboat.skyblockapi.utils.time.since
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Module
// TODO: MLIB
object GfsQueue {

    private val queue: Queue<Pair<String, Int>> = LinkedList()
    private var lastFetch = currentInstant()

    fun add(id: String, amount: Int) {
        queue.add(Pair(id, amount))
    }

    @Subscription
    fun onTick(event: TickEvent) {
        if (queue.isEmpty()) return
        if (lastFetch.since() <= 2.5.seconds) return

        val (id, amount) = queue.poll() ?: return

        McClient.sendCommand("/gfs $id $amount")
        lastFetch = currentInstant()
    }
}
