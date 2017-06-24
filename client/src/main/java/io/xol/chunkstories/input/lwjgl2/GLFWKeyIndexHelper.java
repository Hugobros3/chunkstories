package io.xol.chunkstories.input.lwjgl2;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class GLFWKeyIndexHelper {
	public static Map<String, Integer> glfwKeyCodes = null;
	
	public static int getGlfwKeyByName(String keyName) {
		if(glfwKeyCodes == null) {
			glfwKeyCodes = new HashMap<String, Integer>();
			
			try {
				Field[] fields = GLFW.class.getFields();
				for(Field f : fields) {
					if(f.getName().startsWith("GLFW_KEY_")) {
						Object value = f.get(null);
						Integer iValue = (Integer)value;
						
						String fullName = f.getName();
						String shortName = fullName.substring(9);
						
						//System.out.println("Found GLFWKey: "+fullName+" ("+shortName+") resolving to "+id);
						glfwKeyCodes.put(fullName, iValue);
						glfwKeyCodes.put(shortName, iValue);
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		Integer i = null;
		i = glfwKeyCodes.get(keyName);
		if(i != null)
			return i;
		
		return GLFW.GLFW_KEY_UNKNOWN;
	}
	
	public static void main(String a[]) {
		System.out.println("Test: "+getGlfwKeyByName("U"));
	}
}
