package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.exceptions.AttributeNotPresentException;
import io.xol.chunkstories.api.exceptions.InvalidShaderException;
import io.xol.chunkstories.api.exceptions.ShaderCompileException;
import io.xol.chunkstories.api.rendering.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.PipelineConfiguration.PolygonFillMode;
import io.xol.engine.graphics.PipelineConfigurationImplementation;
import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.Texture1D;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3d;
import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderingInterface
{
	public CameraInterface getCamera();
	
	/* shaders */
	
	public ShaderInterface useShader(String shaderName) throws InvalidShaderException, ShaderCompileException;
	
	public ShaderInterface currentShader();
	
	/* Texturing configuraiton */
	
	public TexturingConfiguration getTexturingConfiguration();
	
	public TexturingConfiguration bindAlbedoTexture(Texture2D texture);
	
	public TexturingConfiguration bindNormalTexture(Texture2D texture);
	
	public TexturingConfiguration bindMaterialTexture(Texture2D texture);

	public TexturingConfiguration bindTexture1D(String textureSamplerName, Texture1D texture);
	
	public TexturingConfiguration bindTexture2D(String textureSamplerName, Texture2D texture);
	
	public TexturingConfiguration bindCubemap(String cubemapSamplerName, Cubemap cubemapTexture);
	
	/* Object location */
	
	/**
	 * Sets the object location and generates an object matrix based on that and the optional rotation set by setObjectRotation()
	 */
	//public Matrix4f setObjectPosition(Vector3f position);
	
	/**
	 * Sets the object location and generates an object matrix based on that and the optional rotation set by setObjectRotation()
	 */
	//public default Matrix4f setObjectPosition(Vector3d position)
	//{
	//	return setObjectPosition(position.castToSimplePrecision());
	//}

	/**
	 * Rotates the object matrix generated by setObjectPosition by the provided rotation matrix
	 */
	//public Matrix4f setObjectRotation(Matrix4f objectRotationOnlyMatrix);
	
	/**
	 * Sets the object rotation and generates an object matrix based on that and the optional position set by setObjectPosition()
	 */
	//public Matrix4f setObjectRotation(double horizontalRotation, double verticalRotation);
	
	/**
	 * returns the current object matrix
	 */
	public Matrix4f getObjectMatrix();
	
	/**
	 * Sets the object matrix to the given argument
	 */
	public Matrix4f setObjectMatrix(Matrix4f objectMatrix);
	
	/* Pipeline configuration */
	
	/**
	 * @return The current PipelineConfiguration
	 */
	public PipelineConfiguration getPipelineConfiguration();

	public PipelineConfiguration setDepthTestMode(DepthTestMode depthTestMode);

	public PipelineConfiguration setBlendMode(BlendMode blendMode);

	public PipelineConfiguration setPolygonFillMode(PolygonFillMode polygonFillMode);
	
	/* Attributes */
	
	/**
	 * Returns the configuration of the bound vertex shader inputs
	 */
	public AttributesConfiguration getAttributesConfiguration();
	
	/**
	 * If attributeSource != null, setups the currently bound vertex shader attribute input 'attributeName' with it
	 * If attibuteSource == null, disables the shader input 'attributeName'
	 * @throws AttributeNotPresentException If 'attributeName' doesn't resolve to a real attribute location
	 * Returns the configuration of the bound vertex shader inputs
	 */
	public AttributesConfiguration bindAttribute(String attributeName, AttributeSource attributeSource) throws AttributeNotPresentException;
	
	/**
	 * Draws N primitives made of 'count' vertices, offset at vertice 'startAt', using data specified in the AttributesConfiguration
	 * @return Returns a RenderingCommand object, containing a snapshot of the current state of the RenderingInterface and adds it to the rendering queue
	 */
	public RenderingCommand draw(Primitive primitive, int startAt, int count);
	
	public enum Primitive {
		POINT, LINE, TRIANGLE;
	}
	
	/**
	 * Executes ALL commands in the queue up to this point before continuing
	 */
	public void flush();
}
