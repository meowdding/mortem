package me.owdding.mortem.core

interface Instance {
    val instance: InstanceType
}

enum class InstanceType {
    CATACOMBS,
    KUUDRA,
    ;
}
