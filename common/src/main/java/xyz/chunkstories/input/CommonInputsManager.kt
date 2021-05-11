package xyz.chunkstories.input

import xyz.chunkstories.EngineImplemI
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.InputsManager
import java.security.MessageDigest

abstract class CommonInputsManager : InputsManager {
    var inputsList: List<Input> = emptyList()
    var inputsMappedByHash: Map<Long, Input> = emptyMap()
    var inputsMappedByName: Map<String, Input> = emptyMap()

    override val allInputs: Collection<Input>
        get() = inputsList

    abstract val context: EngineImplemI

    fun reload() {
        val inputs = mutableListOf<AbstractInput>()

        for (asset in context.modsManager.getAllAssetsByExtension("inputs")) {
            loadKeyBindsFile(asset, inputs)
        }

        addBuiltInInputs(inputs)

        inputsList = inputs
        inputsMappedByHash = inputs.associateBy { it.hash }
        inputsMappedByName = inputs.associateBy { it.name }
    }

    enum class InputType(val token: String) {
        KEY_BIND("keyBind"),
        KEY_COMPOUND("keyBindCompound"),
        VIRTUAL("virtual"),
    }

    private fun loadKeyBindsFile(asset: Asset?, list: MutableList<AbstractInput>) {
        if (asset == null)
            return

        val fileContents = asset.reader().readText()

        // Read until we get a good one
        for (line in fileContents.lines()) {
            if (line.startsWith("#")) {
                continue
            } else {
                val splitted = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splitted.size >= 2) {
                    val arguments = mutableListOf<String>()
                    var inputValue: String? = null

                    val inputTypeName = splitted[0]
                    val inputType = InputType.values().find { it.token == inputTypeName } ?: throw Exception("Unrecognized input type: $inputTypeName")

                    val inputName = splitted[1]

                    if (splitted.size >= 3) {
                        inputValue = splitted[2]
                        for (i in 3 until splitted.size)
                            arguments.add(splitted[i])
                    }

                    insertInput(list, inputType, inputName, inputValue, arguments)
                }
            }
        }

    }

    abstract fun addBuiltInInputs(inputs: MutableList<AbstractInput>)

    abstract fun insertInput(inputs: MutableList<AbstractInput>, inputType: InputType, inputName: String, defaultValue: String?, arguments: MutableList<String>)

    internal fun computeHash(name: String): Long {
        val digested = md5.digest(name.toByteArray())
        var hash = 0L
        hash = hash and 0x0FFFFFFFFFFFFFFFL or (digested[0].toLong() and 0xF shl 60)
        hash = hash and -0xf00000000000001L or (digested[1].toLong() and 0xF shl 56)
        hash = hash and -0xf0000000000001L or (digested[2].toLong() and 0xF shl 52)
        hash = hash and -0xf000000000001L or (digested[3].toLong() and 0xF shl 48)
        hash = hash and -0xf00000000001L or (digested[4].toLong() and 0xF shl 44)
        hash = hash and -0xf0000000001L or (digested[5].toLong() and 0xF shl 40)
        hash = hash and -0xf000000001L or (digested[6].toLong() and 0xF shl 36)
        hash = hash and -0xf00000001L or (digested[7].toLong() and 0xF shl 32)
        hash = hash and -0xf0000001L or (digested[8].toLong() and 0xF shl 28)
        hash = hash and -0xf000001L or (digested[9].toLong() and 0xF shl 24)
        hash = hash and -0xf00001L or (digested[10].toLong() and 0xF shl 20)
        hash = hash and -0xf0001L or (digested[11].toLong() and 0xF shl 16)
        hash = hash and -0xf001L or (digested[12].toLong() and 0xF shl 12)
        hash = hash and -0xf01L or (digested[13].toLong() and 0xF shl 8)
        hash = hash and -0xf1L or (digested[14].toLong() and 0xF shl 4)
        hash = hash and -0x10L or (digested[15].toLong() and 0xF shl 0)
        return hash
    }

    final override fun getInputByName(inputName: String): Input? = inputsMappedByName[inputName]

    final override fun getInputFromHash(hash: Long): Input? = inputsMappedByHash[hash]

    companion object {
        val md5 = MessageDigest.getInstance("MD5")
    }
}