//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.server;



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