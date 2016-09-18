package io.xol.engine.graphics;

import io.xol.chunkstories.api.rendering.RenderingCommand;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.UniformsConfiguration;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;
import static org.lwjgl.opengl.GL11.*;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class RenderingCommandImplementation implements RenderingCommand
{
	//For merging draw calls
	//List<Matrix4f> objectMatrices = new LinkedList<Matrix4f>();

	//Draw call paramters
	protected Primitive primitive;
	int start, count;
	
	//Pipeline state
	protected ShaderInterface shaderInterface;
	protected TexturingConfiguration texturingConfiguration;
	protected AttributesConfiguration attributesConfiguration;
	protected UniformsConfiguration uniformsConfiguration;
	protected PipelineConfiguration pipelineConfiguration;
	
	static int modes[] = {GL_POINTS, GL_LINES, GL_TRIANGLES, GL_QUADS};
	
	protected static Matrix4f temp = new Matrix4f();
	protected static Matrix3f normal = new Matrix3f();

	public RenderingCommandImplementation(Primitive primitive, ShaderInterface shaderInterface, TexturingConfiguration texturingConfiguration, AttributesConfiguration attributesConfiguration, UniformsConfiguration uniformsConfiguration,
			PipelineConfiguration pipelineConfiguration/*, Matrix4f objectMatrix*/, int start, int count)
	{
		this.primitive = primitive;
		this.start = start;
		this.count = count;
		
		this.shaderInterface = shaderInterface;
		this.texturingConfiguration = texturingConfiguration;
		this.attributesConfiguration = attributesConfiguration;
		this.uniformsConfiguration = uniformsConfiguration;
		this.pipelineConfiguration = pipelineConfiguration;
	}

	public ShaderInterface getShader()
	{
		return shaderInterface;
	}

	public TexturingConfiguration getBoundTextures()
	{
		return texturingConfiguration;
	}

	public AttributesConfiguration getAttributesConfiguration()
	{
		return attributesConfiguration;
	}

	public UniformsConfiguration getUniformsConfiguration()
	{
		return uniformsConfiguration;
	}

	public PipelineConfiguration getPipelineConfiguration()
	{
		return pipelineConfiguration;
	}

	/**
	 * Used to automatically instanciate similar rendering commands
	 */
	public final boolean canMerge(RenderingCommand renderingCommand)
	{
		if(getPrimitive() != renderingCommand.getPrimitive())
			return false;
		
		//Check shader
		if (!getShader().equals(renderingCommand.getShader()))
			return false;

		//These rendering commands use the same pipeline configuration
		if(!getPipelineConfiguration().equals(renderingCommand.getPipelineConfiguration()))
			return false;
		
		//These rendering commands do not use different textures for the same samplers
		if (!getBoundTextures().isCompatibleWith(renderingCommand.getBoundTextures()))
			return false;

		//These rendering commands do not use different attributes sources for the same attribute locations
		if (!getAttributesConfiguration().isCompatibleWith(renderingCommand.getAttributesConfiguration()))
			return false;

		//These rendering commands do not sets differents values to the same uniforms
		if (!getUniformsConfiguration().isCompatibleWith(renderingCommand.getUniformsConfiguration()))
			return false;

		return true;
	}

	/*@Override
	public void render(RenderingInterface renderingInterface) throws RenderingException
	{
		//Make sure to use the right shader
		((ShaderProgram)shaderInterface).use();
		
		//Setups vertex attributes
		this.attributesConfiguration.setup(renderingInterface);
		
		//Bind required textures
		this.texturingConfiguration.setup(renderingInterface);
		
		//Compute & send the object matrix
		Matrix4f objectMatrix = renderingInterface.getObjectMatrix();
		if(objectMatrix != null)
		{
			this.shaderInterface.setUniformMatrix4f("objectMatrix", objectMatrix);
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
			this.shaderInterface.setUniformMatrix3f("objectMatrixNormal", normal);
		}
		
		//Setup pipeline state
		this.pipelineConfiguration.setup(renderingInterface);
		
		//Updates uniforms
		this.uniformsConfiguration.setup(renderingInterface);
		
		//Do the draw call
		GLCalls.drawArrays_(modes[primitive.ordinal()], start, count);
		
	}*/
	

	@Override
	public Primitive getPrimitive()
	{
		return primitive;
	}

}