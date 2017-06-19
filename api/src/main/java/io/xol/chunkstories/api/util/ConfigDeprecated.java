package io.xol.chunkstories.api.util;

public interface ConfigDeprecated {

	public void load();

	public void save();

	public int getIntProp(String s, String a);

	public String getProp(String s, String a);

	public String getString(String s);

	public int getInteger(String s, int intProp);

	public boolean getBoolean(String string, boolean booleanProp);

	public float getFloat(String s);

	public float getFloat(String s, float f);

	public double getDouble(String s);

	public double getDouble(String s, double d);

	public long getLong(String s, long l);

	public void setString(String p, String d);

	public void setInteger(String p, int i);

	public void setLong(String p, long l);

	public void setDouble(String p, double d);

	public void setFloat(String p, float f);

	public boolean isFieldSet(String string);

}