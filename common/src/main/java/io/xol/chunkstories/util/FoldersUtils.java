//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;



public class FoldersUtils
{

	// Utility for copying folders
	public static void copyFolder(File source, File dest)
	{
		try
		{
			if (source.isDirectory())
			{
				if (!dest.exists())
				{
					dest.mkdir();
				}

				String[] children = source.list();
				for (int i = 0; i < children.length; i++)
				{
					copyFolder(new File(source, children[i]), new File(dest,
							children[i]));
				}
			} else
			{
				copyFile(source, dest);
			}
		} catch (Exception fe)
		{
			fe.printStackTrace();
		}
	}

	// Copy single file
	public static void copyFile(File source, File dest)
	{
		try
		{
			InputStream in = new FileInputStream(source);
			OutputStream out = new FileOutputStream(dest);

			// Copy the bits from instream to outstream
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (Exception fe)
		{
			fe.printStackTrace();
		}
	}

	// Dumb simple recursive delete method
	public static void deleteFolder(File file)
	{
		try
		{
			if (file.isDirectory())
			{
				for (File f : file.listFiles())
				{
					deleteFolder(f);
				}
			}
			file.delete();
		} catch (Exception fe)
		{
			fe.printStackTrace();
		}
	}
}
