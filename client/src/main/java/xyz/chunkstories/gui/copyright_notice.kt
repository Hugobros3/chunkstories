package xyz.chunkstories.gui

import org.joml.Vector4f
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.util.VersionInfo

fun printCopyrightNotice(drawer: GuiDrawer) {
    // Notices
    val noticeColor = Vector4f(1f)
    val version = "Chunk Stories Client " + VersionInfo.versionJson.verboseVersion
    drawer.fonts.defaultFont().getWidth(version)
    drawer.drawStringWithShadow(drawer.fonts.defaultFont(), 4, 0, version, -1, noticeColor)

    val copyrightNotice = "https://github.com/Hugobros3/chunkstories"
    val copyrightNoticeOffset = drawer.fonts.defaultFont().getWidth(copyrightNotice)
    drawer.drawStringWithShadow(drawer.fonts.defaultFont(), drawer.gui.viewportWidth - copyrightNoticeOffset - 4, 0, copyrightNotice, -1, noticeColor)
}