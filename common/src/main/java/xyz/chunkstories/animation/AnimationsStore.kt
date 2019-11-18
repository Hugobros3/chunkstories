//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.animation

import java.util.HashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.Content.AnimationsLibrary
import xyz.chunkstories.content.GameContentStore

class AnimationsStore(private val store: GameContentStore) : AnimationsLibrary {
    private var animations: MutableMap<String, BiovisionAnimation> = HashMap()

    override val parent: Content
        get() = store

    private fun loadAnimation(name: String): BiovisionAnimation {
        val anim = loadBiviosionFile(store.getAsset(name)!!.reader().readText())
        animations[name] = anim
        return anim
    }

    override fun getAnimation(name: String): BiovisionAnimation =
        animations[name] ?: loadAnimation(name)

    override fun reloadAll() {
        animations.clear()
    }

    override val logger: Logger
        get() = AnimationsStore.Companion.logger

    companion object {
        private val logger = LoggerFactory.getLogger("content.animations")
    }
}
