package me.owdding.mortem.utils.extensions

fun <Key, Value> Map<Key, Value>.transpose(): Map<Value, Key> = entries.associate { (key, value) -> value to key }

fun <Value, Compare : Comparable<Compare>> Collection<Value>.maxOfNotNullOrNull(value: (Value) -> Compare?): Compare? {
    val iterator = this.iterator()
    if (!this.iterator().hasNext()) return null
    var maxValue: Compare? = null

    for (item in iterator) {
        val testValue = value(item) ?: continue
        if (maxValue == null || maxValue < testValue) {
            maxValue = testValue
        }
    }

    return maxValue
}
