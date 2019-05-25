package xyz.chunkstories.input

import xyz.chunkstories.api.input.Input

abstract class AbstractInput(inputsManager: CommonInputsManager, final override val name: String) : Input {
    final override val hash = inputsManager.computeHash(name)
}