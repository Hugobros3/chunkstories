package xyz.chunkstories.gui.layer

import org.joml.Vector4f
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.world.WorldUser
import xyz.chunkstories.api.world.chunk.ChunkHolder
import xyz.chunkstories.client.ingame.IngameClientImplementation
import xyz.chunkstories.world.WorldMasterImplementation
import xyz.chunkstories.world.figureOutWherePlayerWillSpawn
import xyz.chunkstories.world.spawnPlayer

class WorldLoadingUI(val world: WorldMasterImplementation, val ingameClient: IngameClientImplementation, gui: Gui, parentLayer: Layer?) : Layer(gui, parentLayer), WorldUser {

    //val waitOn = mutableListOf<Task>()
    val subs = mutableListOf<ChunkHolder>()
    val count: Int
    var todo: Int

    private var entity: Entity? = null
    private lateinit var spawnLocation: Location

    init {
        // preload arround spawn location
        val spawnLocation = world.figureOutWherePlayerWillSpawn(ingameClient.player)

        world.gameLogic.logicThreadBlocking {
            preloadArround((spawnLocation.x / 32).toInt(), (spawnLocation.y / 32).toInt(), (spawnLocation.z / 32).toInt())
        }

        count = subs.size
        todo = count
    }

    fun preloadArround(cx: Int, cy: Int, cz: Int) {
        for (x in (cx - 2)..(cx + 2)) {
            for (y in (cy - 2)..(cy + 8)) {
                if (y < 0 || y >= world.properties.size.heightInChunks)
                    continue

                for (z in (cz - 2)..(cz + 2)) {
                    val ch = world.chunksManager.acquireChunkHolder(this, x, y, z)
                    subs.add(ch)
                }
            }
        }
    }

    val jokes = listOf(
            "Not being original",
            "Hurting feelings",
            "Circumventing license terms",
            "Violating contracts",
            "Asserting false",
            "Bubble sorting",
            "Removing sharp edges",
            "Insulting your contacts",
            "Downloading a bear",
            "Rushing B",
            "Compiling recursive templates",
            "Salting the earth",
            "Culling biodiversity",
            "Humidifying sensitive electronics",
            "Lubricating ice levels",
            "Making up stuff",
            "Questioning ethics",
            "Thinking about life",
            "Preparing bilateral matrices for cremation",
            "Hacking the government",
            "Deleting map")

    val selectedJokes = jokes.shuffled().subList(0, 6)

    override fun render(drawer: GuiDrawer) {
        width = gui.viewportWidth
        height = gui.viewportHeight

        drawer.drawBox(0, 0, width, height, width / 16f, 0f, 0f, height / 16f, "voxels/textures/dirt.png", Vector4f(0.5f, 0.5f, 0.5f, 1f))

        val done = subs.count { it.state is ChunkHolder.State.Available }
        val percentage = done.toFloat() / count.toFloat()

        val font = gui.fonts.getFont("LiberationSans-Regular", 20f)
        val text = "Generating map: ${(percentage*100).toInt()}%"
        drawer.drawStringWithShadow(font, gui.viewportWidth / 2 - font.getWidth(text) / 2, gui.viewportHeight / 2, text)

        val joke = selectedJokes[(5 * percentage).toInt().coerceIn(0, 5)]

        val font2 = gui.fonts.getFont("LiberationSans-Regular", 12f)
        drawer.drawStringWithShadow(font2, gui.viewportWidth / 2 - font2.getWidth(text) / 2, gui.viewportHeight / 2 - 24, joke)

        val barWidth = 256
        val barH = gui.viewportHeight / 2 - 48
        drawer.drawBox(gui.viewportWidth / 2 - (barWidth + 2) / 2, barH - 1, barWidth + 2, 8 + 2, Vector4f(0f, 0f, 0f, 1f))

        drawer.drawBox(gui.viewportWidth / 2 - (barWidth) / 2, barH, barWidth, 8, Vector4f(0.15f, 0.15f, 0.15f, 1f))

        val progressWidth = (percentage * barWidth).toInt()
        drawer.drawBox(gui.viewportWidth / 2 - (barWidth) / 2, barH, progressWidth, 8, Vector4f(0f, 0.5f, 0f, 1f))
        //println(text)

        if (done == count) {
            world.gameLogic.logicThreadBlocking {
                world.spawnPlayer(ingameClient.player)
                ingameClient.loadingAgent.updateUsedWorldBits()
                for (ch in subs)
                    ch.unregisterUser(this)
            }

            gui.popTopLayer()
        }
    }
}