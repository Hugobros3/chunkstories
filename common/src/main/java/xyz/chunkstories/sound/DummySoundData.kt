package xyz.chunkstories.sound

class DummySoundData(val soundName: String) : SoundData() {
    override fun getLengthMs(): Long = 0L

    override fun loadedOk(): Boolean = true

    override fun getBuffer(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun destroy() {
    }

    override fun getName() = soundName

}