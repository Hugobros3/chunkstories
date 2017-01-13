package io.xol.chunkstories.bugsreporter;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

import java.awt.Color;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class BugsReporter
{

	static ReportThread thread;
	public static JLabel label = new JLabel();
	static JPanel panel;
	static JButton cancel = new JButton("Cancel");
	public static JProgressBar progress = new JProgressBar();

	public static void main(String[] args)
	{
		panel = new JPanel();

		JFrame window = new JFrame();
		//window.setUndecorated(true); // Remove title bar
		window.setSize(540, 240);
		window.setTitle("Chunk Stories Bugs reporter");
		window.setLocationRelativeTo(null);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setContentPane(panel);
		window.setResizable(false);
		window.setLayout(null);

		if (args.length == 2)
		{
			File file = new File(args[1]);
			thread = new ReportThread(args[0], file, progress);

			panel.add(cancel);
			cancel.setBounds(240, 120 + 12 + 8, 112, 24);

			progress.setSize(540, 20);
			cancel.addActionListener(thread);

			progress.setVisible(false);

			progress.setStringPainted(true);
			label.setForeground(Color.white);
			panel.add(label);
			label.setBounds(240, 380 + 24, 320, 20);
			panel.add(progress);
			progress.setBounds(240, 380, 320, 20);

			window.setVisible(true);

			thread.start();
		}
		else
		{
			JOptionPane.showMessageDialog(null, "Invalid arguments.");
			System.out.println("Fuck off");	
		}
	}
}
