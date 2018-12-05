package io.xol.chunkstories.graphics.vulkan.systems.world

import java.util.concurrent.locks.ReentrantLock

abstract class RefCountedRecyclable(val property: RefCountedProperty<*>){
    private var users = 1

    internal inline fun acquire() {
        try {
            property.lock.lock()
            users++
        } finally {
            property.lock.unlock()
        }
    }

    fun release() {
        try {
            property.lock.lock()

            if (--users == 0) {
                cleanup()
                if(property.data == this)
                    property.data = null
            }

            if(users < 0)
                throw Exception("Called release() too many times on this poor thing :(")
        } finally {
            property.lock.unlock()
        }
    }

    abstract fun cleanup()
}

class RefCountedProperty<R: RefCountedRecyclable> {
    internal val lock = ReentrantLock()

    internal var data : R? = null

    fun get() : R? {
        try {
            lock.lock()

            return data?.apply { this.acquire() }
        } finally {
            lock.unlock()
        }
    }

    fun set(value: R) {
        try {
            lock.lock()
            data?.release()
            data = value
        } finally {
            lock.unlock()
        }
    }
}