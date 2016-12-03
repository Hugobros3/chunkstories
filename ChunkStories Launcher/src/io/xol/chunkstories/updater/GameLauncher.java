package io.xol.chunkstories.updater;

//(c) 2015 XolioWare Interactive

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import io.xol.chunkstories.bugsreporter.ReportThread;
import io.xol.chunkstories.content.GameDirectory;

public class GameLauncher implements ActionListener {

	class StreamGobbler extends Thread {
		InputStream in;
		DataOutputStream out;
		String type;

		StreamGobbler(InputStream is, DataOutputStream out, String type) {
			this.in = is;
			this.out = out;
			this.type = type;
		}

		public void run() {
			try {
				InputStreamReader isr = new InputStreamReader(in);
				BufferedReader br = new BufferedReader(isr);
				String line = null;
				while ((line = br.readLine()) != null) {
					System.out.println(type + line);
					out.writeUTF(type + line + "\n\r");
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	JFrame window;

	public GameLauncher(JFrame window) {
		this.window = window;
	}

	@Override
	public void actionPerformed(ActionEvent ee) {
		try {
			Process process = Runtime.getRuntime().exec("java -Xmx1200M -jar chunkstories.jar", null,
					new File(GameDirectory.getGameFolderPath()));

			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
			String time = sdf.format(cal.getTime());

			File launcherLog = new File(GameDirectory.getGameFolderPath() + "/logs/launcher-debug-" + time + ".log");

			FileOutputStream fos = new FileOutputStream(launcherLog);
			DataOutputStream dos = new DataOutputStream(fos);

			// any error message?
			StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), dos, "ERROR:");

			// any output?
			StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), dos, "");

			// kick them off
			errorGobbler.start();
			outputGobbler.start();

			window.setVisible(false);

			// any error???
			int exitVal = -999999999;
			try {
				exitVal = process.waitFor();
			} catch (InterruptedException e) {
				System.out.println("Interrupted waiting for application to terminate");
				e.printStackTrace();
			}

			dos.flush();
			fos.flush();
			fos.close();

			System.out.println("ExitValue: " + exitVal);

			if (exitVal != 0) {
				int dialogButton = JOptionPane.YES_NO_OPTION;
				int dialogResult = JOptionPane.showConfirmDialog(null,
						"The game crashed (exitval=" + exitVal
								+ "), do you want to upload the log ? It surely will help us figure out where it went wrong.",
						"The game crashed x_x", dialogButton);
				if (dialogResult == JOptionPane.YES_OPTION) {

					Thread thread = new ReportThread("launcher-crash", launcherLog, null, null);
					thread.start();

					try {
						thread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			// window.setVisible(true);

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

}
