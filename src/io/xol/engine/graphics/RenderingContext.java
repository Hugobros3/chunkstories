package io.xol.engine.graphics;

import io.xol.chunkstories.api.exceptions.AttributeNotPresentException;
import io.xol.chunkstories.api.rendering.AttributeSource;
import io.xol.chunkstories.api.rendering.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.Light;
import io.xol.chunkstories.api.rendering.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.PipelineConfiguration.PolygonFillMode;
import io.xol.chunkstories.api.rendering.RenderingCommand;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.ShaderInterface;
import io.xol.chunkstories.api.rendering.TexturingConfiguration;
import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.Texture1D;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturingConfigurationImplementation;
import io.xol.engine.graphics.util.GuiRenderer;
import io.xol.engine.graphics.util.TrueTypeFontRenderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.lwjgl.BufferUtils;

import io.xol.engine.math.lalgb.Matrix3f;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.engine.math.lalgb.Vector3f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RenderingContext implements RenderingInterface
{
	private GameWindowOpenGL mainWindows;
	private ShaderProgram currentlyBoundShader = null;

	private Camera camera;
	private boolean isThisAShadowPass;

	private List<Light> lights = new LinkedList<Light>();

	private DirectRenderer directRenderer;
	private GuiRenderer guiRenderer;
	private TrueTypeFontRenderer trueTypeFontRenderer;

	//Texturing
	private TexturingConfigurationImplementation texturingConfiguration = new TexturingConfigurationImplementation();
	//Object matrix
	private Matrix4f currentObjectMatrix = null;
	private Vector3f currentObjectPosition = new Vector3f();
	private float rotationHorizontal = 0, rotationVertial = 0;
	//Pipeline config
	private PipelineConfigurationImplementation pipelineConfiguration = PipelineConfigurationImplementation.DEFAULT;
	private AttributesConfiguration attributesConfiguration;
	
	
	Matrix4f temp = new Matrix4f();
	Matrix3f normal = new Matrix3f();

	public RenderingContext(GameWindowOpenGL windows)
	{
		mainWindows = windows;
		directRenderer = new DirectRenderer(this);
		guiRenderer = new GuiRenderer(this);
		trueTypeFontRenderer = new TrueTypeFontRenderer(this);
	}

	public String toString()
	{
		/*String attributes = "";
		for (int i : enabledAttributes)
		{
			attributes += i;
		}
		attributes += " (" + enabledAttributes.size() + ")";
		return "[RenderingContext shadow:" + isThisAShadowPass + " enabledAttributes: " + attributes + " lights: " + lights.size() + " shader:" + currentShader() + " ]";
		 */
		return "wip";
	}

	public void setCamera(Camera camera)
	{
		this.camera = camera;
	}

	public Camera getCamera()
	{
		return camera;
	}
	
	public ShaderInterface useShader(String shaderName)
	{
		return setCurrentShader(ShadersLibrary.getShaderProgram(shaderName));
	}
	
	public ShaderInterface setCurrentShader(ShaderProgram shaderProgram)
	{
		//Save calls
		if (shaderProgram != currentlyBoundShader)
		{
			//When changing shaders, we make sure we disable whatever was enabled
			
			//resetAllVertexAttributesLocations();
			//disableUnusedVertexAttributes();
			shaderProgram.use();
		}
		currentlyBoundShader = shaderProgram;
		return currentlyBoundShader;
	}
	
	public ShaderInterface currentShader()
	{
		return currentlyBoundShader;
	}
	
	/* TEXTURING */
	
	public TexturingConfiguration getTexturingConfiguration()
	{
		return texturingConfiguration;
	}
	
	public TexturingConfiguration bindTexture1D(String textureSamplerName, Texture1D texture)
	{
		texturingConfiguration = texturingConfiguration.bindTexture1D(textureSamplerName, texture);
		return texturingConfiguration;
	}

	public TexturingConfiguration bindTexture2D(String textureSamplerName, Texture2D texture)
	{
		texturingConfiguration = texturingConfiguration.bindTexture2D(textureSamplerName, texture);
		return texturingConfiguration;
	}
	
	public TexturingConfiguration bindCubemap(String cubemapSamplerName, Cubemap cubemapTexture)
	{
		texturingConfiguration = texturingConfiguration.bindCubemap(cubemapSamplerName, cubemapTexture);
		return texturingConfiguration;
	}
	
	public TexturingConfiguration bindAlbedoTexture(Texture2D texture)
	{
		return bindTexture2D("diffuseTexture", texture);
	}

	public TexturingConfiguration bindNormalTexture(Texture2D texture)
	{
		return bindTexture2D("normalTexture", texture);
	}

	public TexturingConfiguration bindMaterialTexture(Texture2D texture)
	{
		return bindTexture2D("materialTexture", texture);
	}

	public boolean isThisAShadowPass()
	{
		return isThisAShadowPass;
	}

	public void setIsShadowPass(boolean isShadowPass)
	{
		isThisAShadowPass = isShadowPass;
	}

	public void addLight(Light light)
	{
		if (!this.isThisAShadowPass)
			lights.add(light);
	}

	public Iterator<Light> getAllLights()
	{
		return lights.iterator();
	}

	public DirectRenderer getDirectRenderer()
	{
		return directRenderer;
	}

	public GuiRenderer getGuiRenderer()
	{
		return guiRenderer;
	}

	public TrueTypeFontRenderer getTrueTypeFontRenderer()
	{
		return trueTypeFontRenderer;
	}
	
	//Matrix4f currentTransformationMatrix = new Matrix4f();
	
	/**
	 * Sets the current local matrix transformation and normal 3x3 counterpart
	 * 
	 * @param matrix
	 */
	private void sendTransformationMatrix(Matrix4f matrix)
	{
		if (matrix == null)
			matrix = new Matrix4f();
		
		//currentTransformationMatrix = matrix;
		
		this.currentlyBoundShader.setUniformMatrix4f("localTransform", matrix);
		Matrix4f.invert(matrix, temp);
		Matrix4f.transpose(temp, temp);
		normal.m00 = temp.m00;
		normal.m01 = temp.m01;
		normal.m02 = temp.m02;

		normal.m10 = temp.m10;
		normal.m11 = temp.m11;
		normal.m12 = temp.m12;

		normal.m20 = temp.m20;
		normal.m21 = temp.m21;
		normal.m22 = temp.m22;
		this.currentlyBoundShader.setUniformMatrix3f("localTransformNormal", normal);
	}

	/**
	 * Sets the current bone matrix transformation and normal 3x3 counterpart
	 * 
	 * @param matrix
	 */
	private void sendBoneTransformationMatrix(Matrix4f matrix)
	{
		if (matrix == null)
			matrix = new Matrix4f();
		this.currentlyBoundShader.setUniformMatrix4f("boneTransform", matrix);

		Matrix4f.invert(matrix, temp);
		Matrix4f.transpose(temp, temp);
		normal.m00 = temp.m00;
		normal.m01 = temp.m01;
		normal.m02 = temp.m02;

		normal.m10 = temp.m10;
		normal.m11 = temp.m11;
		normal.m12 = temp.m12;

		normal.m20 = temp.m20;
		normal.m21 = temp.m21;
		normal.m22 = temp.m22;
		this.currentlyBoundShader.setUniformMatrix3f("boneTransformNormal", normal);
	}

	public Matrix4f setObjectPosition(Vector3f position)
	{
		currentObjectMatrix = new Matrix4f();
		currentObjectMatrix.translate(position);
		
		return this.currentObjectMatrix;
	}

	@Override
	public Matrix4f setObjectRotation(Matrix4f objectRotationOnlyMatrix)
	{
		// TODO Auto-generated method stub
		return this.currentObjectMatrix;
	}

	@Override
	public Matrix4f setObjectRotation(double horizontalRotation, double verticalRotation)
	{
		// TODO Auto-generated method stub
		return this.currentObjectMatrix;
	}

	@Override
	public Matrix4f setObjectMatrix(Matrix4f objectMatrix)
	{
		currentObjectMatrix = objectMatrix;
		return this.currentObjectMatrix;
	}
	
	public Matrix4f getObjectMatrix()
	{
		return this.currentObjectMatrix;
	}
	
	static VerticesObject fsQuadVertices = null;
	
	public void drawFSQuad(int vertexAttribLocation)
	{
		if (vertexAttribLocation < 0)
			return;
		fsQuadVertices = null;
		if (fsQuadVertices == null)
		{
			fsQuadVertices = new VerticesObject();
			FloatBuffer fsQuadBuffer = BufferUtils.createFloatBuffer(6 * 2);
			fsQuadBuffer.put(new float[] { 1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f });
			fsQuadBuffer.flip();
			
			fsQuadVertices.uploadData(fsQuadBuffer);
		}
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		setVertexAttributePointerLocation(vertexAttribLocation, 2, GL_FLOAT, false, 0, 0, fsQuadVertices);
		
		GLCalls.drawArrays(GL_TRIANGLES, 0, 6);
		//disableVertexAttribute(vertexAttribLocation);
	}
	
	/* Pipeline config */

	@Override
	public PipelineConfiguration getPipelineConfiguration()
	{
		return pipelineConfiguration;
	}

	@Override
	public PipelineConfiguration setDepthTestMode(DepthTestMode depthTestMode)
	{
		pipelineConfiguration = pipelineConfiguration.setDepthTestMode(depthTestMode);
		return pipelineConfiguration;
	}

	@Override
	public PipelineConfiguration setBlendMode(BlendMode blendMode)
	{	
		pipelineConfiguration.setBlendMode(blendMode);
		return pipelineConfiguration;
	}

	@Override
	public PipelineConfiguration setPolygonFillMode(PolygonFillMode polygonFillMode)
	{
		pipelineConfiguration.setPolygonFillMode(polygonFillMode);
		return pipelineConfiguration;
	}

	@Override
	public AttributesConfiguration getAttributesConfiguration()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AttributesConfiguration bindAttribute(String attributeName, AttributeSource attributeSource) throws AttributeNotPresentException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RenderingCommand drawTriangles(int startAt, int count)
	{
		// TODO Auto-generated method stub
		return null;
	}
}