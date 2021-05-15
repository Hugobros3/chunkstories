//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer

import xyz.chunkstories.api.gui.*
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.gui.elements.InputText
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.identity.LocalClientIdentity
import xyz.chunkstories.client.identity.LoggedInClientIdentity
import xyz.chunkstories.client.identity.PasswordStorage
import xyz.chunkstories.gui.layer.config.LanguageSelectionUI
import org.joml.Vector4f
import org.slf4j.LoggerFactory
import xyz.chunkstories.gui.printCopyrightNotice

class LoginUI(gui: Gui, parent: Layer?) : Layer(gui, parent) {
    private val usernameForm = InputText(this, 0, 0, 250)
    private val passwordForm = InputText(this, 0, 0, 250)

    private val loginButton = Button(this, 0, 0, 64, "#{login.login}")

    private var logging_in = false
    private var autologin = false

    private var startCounter = 0L

    private var message = ""

    private var failed_login: Boolean = false

    init {

        elements.add(usernameForm)
        passwordForm.isPassword = true
        elements.add(passwordForm)
        elements.add(loginButton)

        // Autologin fills in the forms automagically
        // TODO Secure storage of password
        val previousPassword = PasswordStorage.load()
        previousPassword?.let {
            usernameForm.text = it.username
            passwordForm.text = it.password
        }

        if (gui.client.configuration.getValue("client.login.auto") == "ok")
            autologin = true

        loginButton.action = Runnable { this.connect() }

        this.focusedElement = usernameForm
        startCounter = System.currentTimeMillis()
    }

    override fun render(drawer: GuiDrawer) {
        if (gui.client.configuration.getValue("client.game.language") == "undefined") {
            gui.topLayer = LanguageSelectionUI(gui, this, false)
        }

        usernameForm.setPosition(gui.viewportWidth / 2 - 125, gui.viewportHeight / 2 + 16)
        usernameForm.render(drawer)
        passwordForm.setPosition(usernameForm.positionX, usernameForm.positionY - usernameForm.height - (20 + 4))
        // passwordForm.render(drawer)

        loginButton.setPosition(usernameForm.positionX, passwordForm.positionY - 30)

        drawer.drawStringWithShadow(drawer.fonts.defaultFont(), usernameForm.positionX, usernameForm.positionY + usernameForm.height + 4,
                gui.client.content.localization().localize("#{login.username}"), -1, Vector4f(1.0f))
        drawer.drawStringWithShadow(drawer.fonts.defaultFont(), passwordForm.positionX, passwordForm.positionY + usernameForm.height + 4,
                gui.client.content.localization().localize("#{login.password}"), -1, Vector4f(1.0f))

        if (logging_in) {
            drawer.drawStringWithShadow(drawer.fonts.defaultFont(), gui.viewportWidth / 2 - 230, gui.viewportHeight / 2 - 90,
                    gui.client.content.localization().localize("#{login.loggingIn}"), -1, Vector4f(1.0f))
        } else {
            val decal_lb = loginButton.width
            loginButton.render(drawer)

            drawer.drawStringWithShadow(drawer.fonts.defaultFont(), loginButton.positionX + 4 + decal_lb, loginButton.positionY + 2,
                    gui.client.content.localization().localize("#{login.register}"), -1, Vector4f(1.0f))

            if (failed_login)
                drawer.drawStringWithShadow(drawer.fonts.defaultFont(), gui.viewportWidth / 2 - 250, gui.viewportHeight / 2 - 160, message, -1,
                        Vector4f(1.0f, 0.0f, 0.0f, 1.0f))
        }

        if (autologin) {
            val seconds = 10
            val autologin2 = gui.client.content.localization()
                    .localize("#{login.auto1} " + (seconds - (System.currentTimeMillis() - startCounter) / 1000) + " #{login.auto2}")

            val autologinLength = drawer.fonts.defaultFont().getWidth(autologin2) * 2
            drawer.drawStringWithShadow(drawer.fonts.defaultFont(2), gui.viewportWidth / 2 - autologinLength / 2, gui.viewportHeight / 2 - 170,
                    autologin2, -1, Vector4f(0.0f, 1.0f, 0.0f, 1.0f))

            if ((System.currentTimeMillis() - startCounter) / 1000 > seconds) {
                connect()
                autologin = false
            }
        }

        printCopyrightNotice(drawer)
    }

    private fun connect() {
        if (true || usernameForm.text == "OFFLINE") {
            val client = gui.client as ClientImplementation
            client.user = LocalClientIdentity(client)

            gui.topLayer = MainMenuUI(gui, parentLayer)
        } else {
            TODO("Rewrite")
            /*logging_in = true

            SimplePostRequest("https://chunkstories.xyz/api/login.php", "user=" + usernameForm.text + "&pass=" + passwordForm.text, RequestResultAction { result ->
                logger.debug("Received login answer")

                logging_in = false
                if (result == null) {
                    failed_login = true
                    message = "Can't connect to server."
                } else if (result.startsWith("ok")) {
                    val session = result.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

                    val client = gui.client as ClientImplementation
                    client.user = LoggedInClientIdentity(client, usernameForm.text, session)

                    //TODO actually secure storage
                    //TODO ask if need to remember
                    PasswordStorage.save(PasswordStorage(usernameForm.text, passwordForm.text))

                    // If the user didn't opt-out, look for crash files and upload those
                    if (gui.client.configuration.getValue("client.game.logPolicy") == "send") {
                        val t = JavaCrashesUploader(gui.client as ClientImplementation)
                        t.start()
                    }

                    gui.topLayer = MainMenuUI(gui, parentLayer)
                } else if (result.startsWith("ko")) {
                    failed_login = true
                    val reason = result.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                    if (reason == "invalidcredentials")
                        message = "Invalid credentials"
                } else {
                    message = "Unknown error"
                }
            })*/
        }
    }

    override fun handleInput(input: Input): Boolean {
        when (input.name) {
            "exit" -> autologin = false
            "enter" -> connect()
            "tab" -> {
                val shift = if (gui.client.inputsManager.getInputByName("shift")!!.isPressed) -1 else 1
                var i = focusedElement?.let {this.elements.indexOf(it) } ?: 0

                var elem: GuiElement? = null

                while (elem == null || elem !is FocusableGuiElement) {
                    i += shift
                    if (i < 0)
                        i = this.elements.size
                    if (i >= this.elements.size)
                        i = 0

                    elem = this.elements[i]
                }

                this.focusedElement = elem as FocusableGuiElement?
            }
        }

        return super.handleInput(input)
    }

    companion object {
        private val logger = LoggerFactory.getLogger("client.login")
    }
}
