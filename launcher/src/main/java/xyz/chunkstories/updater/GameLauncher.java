//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.updater;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Semaphore;

public class GameLauncher implements ActionListener {

	Semaphore noLineCrosstalk = new Semaphore(1);

	class StreamGobbler extends Thread {
		InputStream in;
		DataOutputStream out;
		String type;

		final long t = System.currentTimeMillis();

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

					// We ain't got time for 32-bit support, if you have issues with this please consult your nearest open dump and pickup an optiplex
					if (line.contains("This Java instance does not support a 64-bit JVM") && (System.currentTimeMillis() - t) < 5000) {
						wrongJVM = true;
					}

					noLineCrosstalk.acquireUninterruptibly();

					try {
						if (out != null)
							out.write((type + line + "\n").getBytes("UTF-8"));
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}

					noLineCrosstalk.release();
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

	boolean wrongJVM = false; // Set to 'true' if we are running a 32-bit JVM

	@Override public void actionPerformed(ActionEvent ee) {
		try {
			// TODO make it configurable
			// TODO mod support
			Process process = Runtime.getRuntime().exec("java -d64 -Xmx2048M -jar chunkstories.jar", null, new File(GameDirectory.INSTANCE.getGameFolder()));

			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
			String time = sdf.format(cal.getTime());

			File launcherLog = new File(GameDirectory.INSTANCE.getGameFolder() + "/logs/launcher-debug-" + time + ".log");
			launcherLog.getParentFile().mkdirs();

			FileOutputStream fos = null;
			DataOutputStream dos = null;

			try {
				fos = new FileOutputStream(launcherLog);
				dos = new DataOutputStream(fos);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

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

			if (wrongJVM) {
				JOptionPane.showMessageDialog(null, "Non 64 bit JVM detected. Please install 64-bit Java to run Chunk Stories.");
			} else if (exitVal != 0) {
				int dialogButton = JOptionPane.YES_NO_OPTION;
				int dialogResult = JOptionPane.showConfirmDialog(null,
						"The game crashed (exitval=" + exitVal + "), do you want to upload the log ? It surely will help us figure out where it went wrong.",
						"The game crashed x_x", dialogButton);
				if (dialogResult == JOptionPane.YES_OPTION) {

					Thread thread = new ReportThread("launcher-crash", launcherLog, null);
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
