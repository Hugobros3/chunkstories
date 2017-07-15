package io.xol.chunkstories.anvil;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SignParseUtil {
	
	private static ThreadLocal<JSONParser> threadSafeJsonParser = new ThreadLocal<JSONParser>() {

		@Override
		protected JSONParser initialValue() {
			return new JSONParser();
		}
		
	};
	
	/** Sign data is a mess of legacy and incompatible formats, the same save can contain multiple ways of formatting this data, this method should return
	 * something along the lines of what the actual Minecraft client could read out of it
	 */
	public static String parseSignData(String data)
	{
		if(data == null)
			return "";
		if(data.endsWith("null"))
			return "";
		if(data.startsWith("\""))
			return data.substring(1, data.length() - 1);
		if(data.startsWith("{"))
		{
			JSONObject jSonObject;
			try
			{
				//System.out.println(":"+data);
				jSonObject = (JSONObject) threadSafeJsonParser.get().parse(data);
				
				Object extraObject = jSonObject.get("extra");
				
				String extra = "";
				if(extraObject != null)
				{
					Object arrayObject = (((JSONArray)extraObject).get(0));
					if(arrayObject instanceof String)
						extra = (String) arrayObject;
					else if(arrayObject instanceof JSONObject)
					{
						extra = (String) ((JSONObject) (((JSONArray)extraObject).get(0))).get("text");
					}
				}
				
				String text = (String) jSonObject.get("text");
				//Get whatever one is good
				
				//System.out.println("extra : "+extra);
				//System.out.println("text : "+text);
				return text.length() > extra.length() ? text : extra;
			}
			catch (ParseException e)
			{
				System.out.println(data);
				System.out.println(data.length());
				System.out.println("Can't parse sign "+e.getMessage());
				e.printStackTrace();
			}
		}
		return data;
	}
}
