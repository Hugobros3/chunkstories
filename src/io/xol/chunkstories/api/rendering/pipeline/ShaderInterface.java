package io.xol.chunkstories.api.rendering.pipeline;

import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.vector.Vector2;
import io.xol.engine.math.lalgb.vector.Vector3;
import io.xol.engine.math.lalgb.vector.Vector4;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ShaderInterface
{
	public String getShaderName();

	public void setUniform1i(String uniformName, int uniformData);
	
	public void setUniform1f(String uniformName, double uniformData);
	
	public void setUniform2f(String uniformName, double uniformData_x, double uniformData_y);
	
	public void setUniform2f(String uniformName, Vector2<?> uniformData);
	
	public void setUniform3f(String uniformName, double uniformData_x, double uniformData_y, double uniformData_z);
	
	public void setUniform3f(String uniformName, Vector3<?> uniformData);
	
	public void setUniform4f(String uniformName, double uniformData_x, double uniformData_y, double uniformData_z, double uniformData_w);
	
	public void setUniform4f(String uniformName, Vector4<?> uniformData);
	
	public void setUniformMatrix4f(String uniformName, Matrix4f uniformData);
	
	public void setUniformMatrix3f(String uniformName, Matrix3f uniformData);
	
	public UniformsConfiguration getUniformsConfiguration();
}
