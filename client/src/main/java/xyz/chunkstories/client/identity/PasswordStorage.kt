package xyz.chunkstories.client.identity

import com.google.gson.Gson
import xyz.chunkstories.content.GameDirectory
import java.io.File

/** Deals with the password situation for the chunkstories.xyz login method */
data class PasswordStorage(val username: String, val password: String) {
    companion object {
        fun load() : PasswordStorage? {
            //TODO protect that file from being accesed from the rest of the code via some kind of SecurityManager
            val file = File(GameDirectory.getGameFolderPath()+"/gamePassword.dat")
            return if(file.exists()) {
                val contents = file.readText()
                val gson = Gson()
                gson.fromJson(contents, PasswordStorage::class.java)
            } else null
        }

        fun save(passwordStorage: PasswordStorage) {
            val file = File(GameDirectory.getGameFolderPath()+"/gamePassword.dat")
            val gson = Gson()
            val contents = gson.toJson(passwordStorage)
            file.parentFile?.mkdirs()
            file.writeText(contents)
        }
    }
}
