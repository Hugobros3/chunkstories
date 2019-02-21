//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.logging

import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.FileAppender

class Log4jTest {

    @Test
    fun testLog4j() {
        val lc = LoggerFactory.getILoggerFactory() as LoggerContext
        val ple = PatternLayoutEncoder()

        ple.pattern = "%date %level [%logger] [%thread] [%file:%line] %msg%n"
        ple.context = lc
        ple.start()
        val fileAppender = FileAppender<ILoggingEvent>()
        fileAppender.file = "prout.log"
        fileAppender.encoder = ple
        fileAppender.context = lc
        fileAppender.start()

        val rootLogger = LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
        rootLogger.addAppender(fileAppender)

        val logger = LoggerFactory.getLogger("content")
        logger.info("Hello World")

        LoggerFactory.getLogger("content.voxels").debug("Failed to load")
    }
}
