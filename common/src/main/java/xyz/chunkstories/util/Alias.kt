package xyz.chunkstories.util

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

class MutableAlias<T>(val delegate: KMutableProperty0<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            delegate.get()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        delegate.set(value)
    }
}

class Alias<T>(val delegate: KProperty0<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
            delegate.get()
}

fun <T> alias(delegate: KMutableProperty0<T>) = MutableAlias(delegate)
fun <T> alias(delegate: KProperty0<T>) = Alias(delegate)