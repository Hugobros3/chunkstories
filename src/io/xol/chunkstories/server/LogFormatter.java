package io.xol.chunkstories.server;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public final class LogFormatter extends Formatter
{

	public LogFormatter()
	{
		super();
	}

	private static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	@Override
	public String format(LogRecord record)
	{
		StringBuilder sb = new StringBuilder();

		sb.append(new Date(record.getMillis())).append(" ")
				.append(record.getLevel().getLocalizedName()).append(": ")
				.append(formatMessage(record)).append(LINE_SEPARATOR);

		if (record.getThrown() != null)
		{
			try
			{
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex)
			{
				// ignore
			}
		}

		return sb.toString();
	}
}