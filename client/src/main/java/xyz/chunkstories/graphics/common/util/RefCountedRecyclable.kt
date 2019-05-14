package xyz.chunkstories.graphics.vulkan.resources

import xyz.chunkstories.graphics.common.Recyclable
import java.util.concurrent.locks.ReentrantLock

class RefCountedProperty<R: RefCountedRecyclable> {
    internal val lock = ReentrantLock()

    internal var data : R? = null

    fun getAndAcquire() : R? {
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

abstract class RefCountedRecyclable(val property: RefCountedProperty<*>) : Recyclable {
    private var users = 1

    internal inline fun acquire() {
        try {
            property.lock.lock()
            if(users < 1)
                throw Exception("Contract violated: Once the final user has been released, this may not be acquired again.")
            users++
        } finally {
            property.lock.unlock()
        }
    }

    override fun release() {
        try {
            property.lock.lock()

            if (--users == 0) {
                cleanup()
                if(property.data == this)
                    property.data = null
            }

            if(users < 0)
                throw Exception("Contract violated: release() has been called two or more times than acquire was.")
        } finally {
            property.lock.unlock()
        }
    }

    abstract fun cleanup()
}