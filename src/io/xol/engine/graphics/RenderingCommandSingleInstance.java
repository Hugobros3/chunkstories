package io.xol.engine.graphics;

import io.xol.chunkstories.api.exceptions.RenderingException;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingCommand;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.UniformsConfiguration;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class RenderingCommandSingleInstance extends RenderingCommandImplementation
{
	Matrix4f objectMatrix;
	int sunLight, blockLight;

	public RenderingCommandSingleInstance(Primitive primitive, ShaderInterface shaderInterface, TexturingConfiguration texturingConfiguration, AttributesConfiguration attributesConfiguration, UniformsConfiguration uniformsConfiguration,
			PipelineConfiguration pipelineConfiguration, Matrix4f objectMatrix, int start, int count)
	{
		super(primitive, shaderInterface, texturingConfiguration, attributesConfiguration, uniformsConfiguration, pipelineConfiguration, start, count);
		this.objectMatrix = objectMatrix;
	}

	public RenderingCommand merge(RenderingCommandSingleInstance mergeWith)
	{
		//Debug
		throw new UnsupportedOperationException();

		/*assert mergeWith.canMerge(this);
		
		for (Matrix4f foreightObject : mergeWith.getObjectsMatrices())
			objectMatrices.add(foreightObject);
		
		return this;*/
	}

	private void setup(RenderingInterface renderingInterface) throws RenderingException
	{
		//Make sure to use the right shader
		((ShaderProgram) shaderInterface).use();

		//Setups vertex attributes
		this.attributesConfiguration.setup(renderingInterface);

		//Bind required textures
		this.texturingConfiguration.setup(renderingInterface);

		//Compute & send the object matrix
		if (objectMatrix != null)
		{
			((ShaderProgram) this.shaderInterface).applyUniformAttribute("objectMatrix", objectMatrix);
			//this.shaderInterface.setUniformMatrix4f("objectMatrix", objectMatrix);
			Matrix4f.invert(objectMatrix, temp);
			Matrix4f.transpose(temp, temp);
			//TODO make a clean function for this
			normal.m00 = temp.m00;
			normal.m01 = temp.m01;
			normal.m02 = temp.m02;

			normal.m10 = temp.m10;
			normal.m11 = temp.m11;
			normal.m12 = temp.m12;

			normal.m20 = temp.m20;
			normal.m21 = temp.m21;
			normal.m22 = temp.m22;
			((ShaderProgram) this.shaderInterface).applyUniformAttribute("objectMatrixNormal", normal);
			//this.shaderInterface.setUniformMatrix3f("objectMatrixNormal", normal);
		}

		//Setup pipeline state
		this.pipelineConfiguration.setup(renderingInterface);

		//Updates uniforms
		this.uniformsConfiguration.setup(renderingInterface);

	}

	@Override
	public void render(RenderingInterface renderingInterface) throws RenderingException
	{
		setup(renderingInterface);

		//Do the draw call
		GLCalls.drawArrays_(modes[primitive.ordinal()], start, count);
	}
}
