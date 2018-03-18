//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.client.ClientContent.TexturesLibrary;
import io.xol.chunkstories.api.exceptions.rendering.AttributeNotPresentException;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.StateMachine.PolygonFillMode;
import io.xol.chunkstories.api.rendering.mesh.ClientMeshLibrary;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargets;
import io.xol.chunkstories.api.rendering.textures.ArrayTexture;
import io.xol.chunkstories.api.rendering.textures.Cubemap;
import io.xol.chunkstories.api.rendering.textures.Texture1D;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture3D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.vertex.AttributeSource;
import io.xol.chunkstories.api.rendering.vertex.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.font.TrueTypeFontRenderer;
import io.xol.chunkstories.renderer.lights.LightsRenderer;
import io.xol.chunkstories.renderer.opengl.GLFWGameWindow;
import io.xol.chunkstories.renderer.opengl.OpenGLStateMachine;
import io.xol.chunkstories.renderer.opengl.commands.RenderingCommandMultiDraw;
import io.xol.chunkstories.renderer.opengl.commands.RenderingCommandMultipleInstances;
import io.xol.chunkstories.renderer.opengl.commands.RenderingCommandSingleInstance;
import io.xol.chunkstories.renderer.opengl.fbo.OpenGLRenderTargetManager;
import io.xol.chunkstories.renderer.opengl.texture.Texture2DRenderTargetGL;
import io.xol.chunkstories.renderer.opengl.texture.Texture3DGL;
import io.xol.chunkstories.renderer.opengl.texture.TextureGL;
import io.xol.chunkstories.renderer.opengl.texture.TexturingConfigurationImplementation;
import io.xol.chunkstories.renderer.opengl.util.GuiRendererImplementation;
import io.xol.chunkstories.renderer.opengl.vbo.AttributesConfigurationImplementation;
import io.xol.chunkstories.renderer.opengl.vbo.VertexBufferGL;
import io.xol.chunkstories.renderer.shaders.ShaderProgram;
import io.xol.chunkstories.renderer.shaders.ShadersStore;

public class OpenGLRenderingContext implements RenderingInterface
{
	private GLFWGameWindow gameWindow;
	
	private ShaderProgram currentlyBoundShader = null;

	private final Camera mainCamera = new Camera();
	//private boolean isThisAShadowPass;
	
	private LightsRenderer lightsRenderers = new LightsRenderer(this);

	private GuiRendererImplementation guiRenderer;
	private TrueTypeFontRenderer trueTypeFontRenderer;

	//Texturing
	private TexturingConfigurationImplementation texturingConfiguration = new TexturingConfigurationImplementation();
	//Object matrix
	private Matrix4f currentObjectMatrix = null;
	
	//Pipeline config
	private final OpenGLStateMachine stateMachine = OpenGLStateMachine.DEFAULT;
	private final AttributesConfigurationImplementation attributesConfiguration = new AttributesConfigurationImplementation();
	private final RenderTargets renderTargetManager;
	
	//private Deque<RenderingCommandImplementation> commands = new ArrayDeque<RenderingCommandImplementation>();

	public OpenGLRenderingContext(GLFWGameWindow windows)
	{
		gameWindow = windows;
		
		renderTargetManager = new OpenGLRenderTargetManager(this);
		
		guiRenderer = new GuiRendererImplementation(this);
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

	public Camera getCamera()
	{
		return mainCamera;
	}

	public GLFWGameWindow getWindow()
	{
		return gameWindow;
	}
	
	public Shader useShader(String shaderName)
	{
		return setCurrentShader(shaders().getShaderProgram(shaderName));
	}

	private Shader setCurrentShader(ShaderProgram shader)
	{
		if (shader != currentlyBoundShader) {
			texturingConfiguration.clear();
			attributesConfiguration.clear();
			currentlyBoundShader = shader;
			
			RenderPass currentPass = this.getCurrentPass();
			if(currentPass != null)
				currentPass.autoBindInputs(this, shader);
		}
		return currentlyBoundShader;
	}

	public Shader currentShader()
	{
		return currentlyBoundShader;
	}

	/* TEXTURING */

	public void bindTexture1D(String textureSamplerName, Texture1D texture) {
		texturingConfiguration.bindTexture1D(textureSamplerName, texture);
	}

	public void bindTexture2D(String textureSamplerName, Texture2D texture) {
		texturingConfiguration.bindTexture2D(textureSamplerName, texture);
	}

	public void bindTexture3D(String textureSamplerName, Texture3D texture) {
		texturingConfiguration.bindTexture3D(textureSamplerName, texture);
	}

	public void bindCubemap(String cubemapSamplerName, Cubemap cubemapTexture) {
		texturingConfiguration.bindCubemap(cubemapSamplerName, cubemapTexture);
	}

	@Override
	public void bindArrayTexture(String textureSamplerName, ArrayTexture texture) {
		texturingConfiguration.bindArrayTexture(textureSamplerName, texture);
	}

	public void bindAlbedoTexture(Texture2D texture) {
		bindTexture2D("diffuseTexture", texture);
	}

	public void bindNormalTexture(Texture2D texture) {
		bindTexture2D("normalTexture", texture);
	}

	public void bindMaterialTexture(Texture2D texture) {
		bindTexture2D("materialTexture", texture);
	}

	public GuiRendererImplementation getGuiRenderer() {
		return guiRenderer;
	}

	@Override
	public Matrix4f setObjectMatrix(Matrix4f objectMatrix)
	{
		if (objectMatrix == null)
			objectMatrix = new Matrix4f();
		currentObjectMatrix = new Matrix4f(objectMatrix);
		return this.currentObjectMatrix;
	}

	public Matrix4f getObjectMatrix()
	{
		return this.currentObjectMatrix;
	}

	static VertexBuffer fsQuadVertices = null;
	static AttributeSource fsQuadAttrib;

	public void drawFSQuad()
	{
		if (fsQuadVertices == null)
		{
			fsQuadVertices = new VertexBufferGL();
			FloatBuffer fsQuadBuffer = BufferUtils.createFloatBuffer(6 * 2);
			fsQuadBuffer.put(new float[] { 1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f });
			fsQuadBuffer.flip();

			fsQuadVertices.uploadData(fsQuadBuffer);

			fsQuadAttrib = fsQuadVertices.asAttributeSource(VertexFormat.FLOAT, 2);
		}

		this.bindAttribute("vertexIn", fsQuadAttrib);

		this.draw(Primitive.TRIANGLE, 0, 6);
	}

	/* Pipeline config */

	@Override
	public StateMachine getStateMachine() {
		return stateMachine;
	}

	@Override
	public void setDepthTestMode(DepthTestMode depthTestMode) {
		stateMachine.setDepthTestMode(depthTestMode);
	}

	@Override
	public void setBlendMode(BlendMode blendMode) {
		stateMachine.setBlendMode(blendMode);
	}

	@Override
	public void setCullingMode(CullingMode cullingMode) {
		stateMachine.setCullingMode(cullingMode);
	}

	@Override
	public void setPolygonFillMode(PolygonFillMode polygonFillMode) {
		stateMachine.setPolygonFillMode(polygonFillMode);
	}

	@Override
	public AttributesConfiguration bindAttribute(String attributeName, AttributeSource attributeSource) throws AttributeNotPresentException
	{
		//TODO check in shader if attribute exists
		attributesConfiguration.bindAttribute(attributeName, attributeSource);
		return this.attributesConfiguration;
	}

	@Override
	public AttributesConfiguration unbindAttributes()
	{
		attributesConfiguration.clear();
		return this.attributesConfiguration;
	}

	@Override
	public void draw(Primitive p, int startAt, int count)
	{
		RenderingCommandSingleInstance command = new RenderingCommandSingleInstance(p, currentlyBoundShader, texturingConfiguration, attributesConfiguration, currentlyBoundShader.getUniformsConfiguration(), stateMachine, currentObjectMatrix,
				startAt, count);

		command.render(this);
		//queue(command);

		//return command;
	}
	
	@Override
	public void draw(Primitive p, int startAt, int count, int instances)
	{
		RenderingCommandMultipleInstances command = new RenderingCommandMultipleInstances(p, currentlyBoundShader, texturingConfiguration, attributesConfiguration, currentlyBoundShader.getUniformsConfiguration(), stateMachine, currentObjectMatrix,
				startAt, count, instances);

		command.render(this);
		//queue(command);

		//return command;
	}
	
	@Override
	public void drawMany(Primitive p, int... startAndCountPairs)
	{
		if(startAndCountPairs.length == 0)
			return;// null;
		if(startAndCountPairs.length % 2 == 1)
			throw new IllegalArgumentException("Non-pair amount of integers provided");
		
		int nb_arguments = startAndCountPairs.length / 2;
		IntBuffer starts = BufferUtils.createIntBuffer(nb_arguments);
		IntBuffer counts = BufferUtils.createIntBuffer(nb_arguments);
		
		for(int i = 0; i < nb_arguments; i++)
		{
			starts.put(startAndCountPairs[i*2]);
			counts.put(startAndCountPairs[i*2 + 1]);
		}
		
		starts.flip();
		counts.flip();
		
		RenderingCommandMultiDraw command = new RenderingCommandMultiDraw(p, currentlyBoundShader, texturingConfiguration, attributesConfiguration, currentlyBoundShader.getUniformsConfiguration(), stateMachine, currentObjectMatrix, starts, counts);

		command.render(this);
		//queue(command);

		//return command;
	}

	/*private void queue(RenderingCommandImplementation command)
	{
		//Limit to how many commands it may stack
		if (queuedCommandsIndex >= 1024)
			flush();

		queuedCommands[queuedCommandsIndex] = command;
		queuedCommandsIndex++;
	}

	@Override
	public void flush()
	{
		try
		{
			int kek = 0;
			while (kek < queuedCommandsIndex)
			{
				queuedCommands[kek].render(this);
				queuedCommands[kek] = null;
				kek++;
			}
		}
		catch (RenderingException e)
		{
			e.printStackTrace();
		}

		queuedCommandsIndex = 0;
	}*/

	@Override
	public long getVertexDataVramUsage()
	{
		return this.gameWindow.vramUsageVerticesObjects;
	}

	@Override
	public long getTextureDataVramUsage()
	{
		return TextureGL.getTotalVramUsage();
	}

	@Override
	public RenderTargets getRenderTargetManager()
	{
		return renderTargetManager;
	}

	@Override
	public LightsRenderer getLightsRenderer()
	{
		return lightsRenderers;
	}

	@Override
	public WorldRenderer getWorldRenderer()
	{
		WorldClient world = Client.getInstance().getWorld();
		return world == null ? null : world.getWorldRenderer();
	}

	@Override
	public Texture2DRenderTargetGL newTexture2D(TextureFormat type, int width, int height) {
		return new Texture2DRenderTargetGL(type, width, height);
	}
	
	@Override
	public Texture3DGL newTexture3D(TextureFormat type, int width, int height, int depth) {
		return new Texture3DGL(type, width, height, depth);
	}

	@Override
	public VertexBuffer newVertexBuffer() {
		return new VertexBufferGL();
	}

	@Override
	public TrueTypeFontRenderer getFontRenderer() {
		return trueTypeFontRenderer;
	}

	@Override
	public final Client getClient() {
		return Client.getInstance();
	}

	@Override
	public final TexturesLibrary textures() {
		return getClient().getContent().textures();
	}

	@Override
	public final ClientMeshLibrary meshes() {
		return getClient().getContent().meshes();
	}

	@Override
	public ShadersStore shaders() {
		return getClient().getContent().shaders();
	}

	@Override
	public RenderPass getCurrentPass() {
		WorldRenderer worldRenderer = this.getWorldRenderer();
		return worldRenderer == null ? null : worldRenderer.renderPasses().getCurrentPass();
	}
}