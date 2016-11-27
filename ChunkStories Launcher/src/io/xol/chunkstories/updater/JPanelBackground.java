package io.xol.chunkstories.updater;

//(c) 2015 XolioWare Interactive

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class JPanelBackground extends JPanel{
	
	private static final long serialVersionUID = -8043479868116876680L;

	static BufferedImage logo;
	static BufferedImage bg;
	
	List<Image> backgroundImages = new ArrayList<Image>();
	
	public JPanelBackground()
	{
		super();

		try {
			logo = ImageIO.read(getClass().getResourceAsStream("/res/gfx/logo.png"));
			bg = ImageIO.read(getClass().getResourceAsStream("/res/gfx/bg.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);  //THIS LINE WAS ADDED
        if(logo != null)
        {
            g.drawImage(bg, 0, 360, null); // see javadoc for more info on the parameters
            g.drawImage(logo, 0+0, 360+16, null); // see javadoc for more info on the parameters
        }
    }
}
