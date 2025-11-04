package me.owdding.mortem.utils.extensions

fun <Key, Value> Map<Key, Value>.transpose() = this.map { (key, value) -> value to key }.toMap()
