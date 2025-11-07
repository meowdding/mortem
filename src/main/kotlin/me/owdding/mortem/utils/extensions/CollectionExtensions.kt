package me.owdding.mortem.utils.extensions

fun <Key, Value> Map<Key, Value>.transpose(): Map<Value, Key> = entries.associate { (key, value) -> value to key }
