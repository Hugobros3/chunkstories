package xyz.chunkstories.server.player

import xyz.chunkstories.api.GameContext
import xyz.chunkstories.input.AbstractInput
import xyz.chunkstories.input.CommonInputsManager
import xyz.chunkstories.input.InputVirtual

class ServerPlayerInputsManager(private val serverPlayer: ServerPlayer) : CommonInputsManager() {
    init {
        reload()
    }

    override val context: GameContext
        get() = serverPlayer.getContext()

    override fun addBuiltInInputs(inputs: MutableList<AbstractInput>) {
        val mouseLeft = InputVirtual(this, "mouse.left")
        inputs.add(mouseLeft)
        val mouseRight = InputVirtual(this, "mouse.right")
        inputs.add(mouseRight)
        val mouseMiddle = InputVirtual(this, "mouse.middle")
        inputs.add(mouseMiddle)
    }

    override fun insertInput(inputs: MutableList<AbstractInput>, inputType: InputType, inputName: String, defaultValue: String?, arguments: MutableList<String>) {
        inputs.add(InputVirtual(this, inputName))
    }
}