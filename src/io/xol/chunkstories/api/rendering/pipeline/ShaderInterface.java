package io.xol.chunkstories.api.rendering.pipeline;

import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.Texture1D;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector2f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ShaderInterface
{
	public String getShaderName();

	public void setUniform1i(String uniformName, int uniformData);
	
	public void setUniform1f(String uniformName, double uniformData);
	
	public void setUniform2f(String uniformName, double uniformData_x, double uniformData_y);
	
	public void setUniform2f(String uniformName, Vector2f uniformData);
	
	public void setUniform3f(String uniformName, double uniformData_x, double uniformData_y, double uniformData_z);
	
	public void setUniform3f(String uniformName, Vector3d uniformData);
	
	public void setUniform3f(String uniformName, Vector3f uniformData);
	
	public void setUniform4f(String uniformName, double uniformData_x, double uniformData_y, double uniformData_z, double uniformData_w);
	
	public void setUniform4f(String uniformName, Vector4f uniformData);
	
	//public void setUniform4f(String uniformName, Vector4d uniformData);
	
	public void setUniformMatrix4f(String uniformName, Matrix4f uniformData);
	
	public void setUniformMatrix3f(String uniformName, Matrix3f uniformData);
	
	public UniformsConfiguration getUniformsConfiguration();
}
