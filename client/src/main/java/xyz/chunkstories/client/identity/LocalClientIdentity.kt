package xyz.chunkstories.client.identity

import xyz.chunkstories.api.client.ClientIdentity
import xyz.chunkstories.api.net.AuthenticationMethod
import xyz.chunkstories.api.player.PlayerID
import xyz.chunkstories.client.ClientImplementation
import java.util.*

class LocalClientIdentity(val client: ClientImplementation) : ClientIdentity {
    override val name = "OfflinePlayer${Random().nextInt(999)}"
    override val authenticationMethod = AuthenticationMethod.NONE
    override val id: PlayerID
        get() = PlayerID(UUID.randomUUID())
}

@Deprecated("Dead")
class LoggedInClientIdentity(val client: ClientImplementation, override val name: String, val sessionKey: String) : ClientIdentity {
    override val authenticationMethod = AuthenticationMethod.CHUNKSTORIES_OFFICIAL_WEBSITE
    override val id: PlayerID
        get() = PlayerID(UUID.fromString(name))
}