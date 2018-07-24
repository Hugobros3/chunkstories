//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;

/** This takes up way more space than it should have any right to */
public class LogbackSetupHelper {

	public LogbackSetupHelper(String loggingFilename) {
		ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(Logger.ROOT_LOGGER_NAME);

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		PatternLayoutEncoder ple = new PatternLayoutEncoder();

		String pattern = "%date %level [%logger] %msg%n";
		String fancyPattern = "%date %level [%logger] [%thread] [%file:%line] %msg%n";

		ple.setPattern(fancyPattern);
		ple.setContext(lc);
		ple.start();
		FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
		fileAppender.setFile(loggingFilename);
		fileAppender.setEncoder(ple);
		fileAppender.setContext(lc);
		fileAppender.start();
		rootLogger.addAppender(fileAppender);

		PatternLayoutEncoder ple2 = new PatternLayoutEncoder();
		ple2.setPattern(pattern);
		ple2.setContext(lc);
		ple2.start();

		ConsoleAppender<ILoggingEvent> logConsoleAppender = new ConsoleAppender<>();
		logConsoleAppender.setContext(lc);
		logConsoleAppender.setName("console");
		logConsoleAppender.setEncoder(ple2);
		logConsoleAppender.start();

		rootLogger.addAppender(logConsoleAppender);

		rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
	}

}
