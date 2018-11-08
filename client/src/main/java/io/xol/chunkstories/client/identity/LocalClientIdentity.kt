package io.xol.chunkstories.client.identity

import io.xol.chunkstories.api.client.ClientIdentity
import io.xol.chunkstories.api.net.AuthenticationMethod
import io.xol.chunkstories.client.ClientImplementation
import java.util.*

class LocalClientIdentity(val client: ClientImplementation) : ClientIdentity {
    override val name = "OfflinePlayer${Random().nextInt(999)}"
    override val authenticationMethod = AuthenticationMethod.NONE
}

class LoggedInClientIdentity(val client: ClientImplementation, override val name: String, val sessionKey: String) : ClientIdentity {
    override val authenticationMethod = AuthenticationMethod.CHUNKSTORIES_OFFICIAL_WEBSITE
}