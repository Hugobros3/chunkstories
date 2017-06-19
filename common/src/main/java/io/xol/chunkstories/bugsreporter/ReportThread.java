package io.xol.chunkstories.bugsreporter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ReportThread extends Thread implements ActionListener{

		private final String username;
		private final File logFile;
		
		private final JProgressBar progress;
		
		public ReportThread(String username, File logFile)
		{
			this.username = username;
			this.logFile = logFile;
			
			this.progress = null;
		}
		
		public ReportThread(String username, File logFile, JProgressBar progress)
		{
			this.username = username;
			this.logFile = logFile;
			
			this.progress = progress;
			//this.pane = pane;
		}
		
		@Deprecated
		public ReportThread(String username, File logFile, JProgressBar progress, JFrame pane)
		{
			this.username = username;
			this.logFile = logFile;
			
			this.progress = progress;
		}

		@Override
		public void run()
		{
			if(progress != null)
				progress.setString("Uploading file "+logFile);
			
			String url = "http://chunkstories.xyz/debug/upload.php";
			String charset = "UTF-8";
			try
			{
				URLConnection la_vie = new URL(url).openConnection();

				//Thx stackoverflow *lenny face*
				String bound = Long.toHexString(System.currentTimeMillis());
				la_vie.setDoOutput(true);
				la_vie.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + bound);
				
				OutputStream os = la_vie.getOutputStream();
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, charset));
				
				//Parameter
				pw.append("--" + bound + "\r\n");
				pw.append("Content-Disposition: form-data; name=\"username\"" + "\r\n");
				pw.append("Content-Type: text/plain; charset=" + charset + "\r\n");
				pw.append("\r\n" + username + "\r\n");
				pw.flush();
				
				//Binary file upload
				pw.append("--" + bound + "\r\n");
				pw.append("Content-Disposition: form-data; name=\"userfile\"; filename=\"" + logFile.getName() + "\"" + "\r\n");
				pw.append("Content-Type: " + URLConnection.guessContentTypeFromName(logFile.getName()) + "\r\n");
				pw.append("Content-Transfer-Encoding: binary" + "\r\n" + "\r\n");
				pw.flush();
				long uploadedBytes = Files.copy(logFile.toPath(), os);
				System.out.println("Sending log report : " +uploadedBytes + "bytes uploaded");
				os.flush();
				pw.append("\r\n");
				//End
				pw.append("--" + bound + "--\r\n");
				pw.flush();
				
				System.out.println(((HttpURLConnection) la_vie).getResponseCode()+"code"+((HttpURLConnection) la_vie).getResponseMessage());
			}
			catch (MalformedURLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Once it's done reporting
			if(progress != null)
				System.exit(0);
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			// TODO Auto-generated method stub
			
		}
	}