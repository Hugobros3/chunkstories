//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.opengl.texture;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.xol.chunkstories.api.exceptions.rendering.NotEnoughtTextureUnitsException;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.shader.Shader.SamplerType;
import io.xol.chunkstories.api.rendering.textures.ArrayTexture;
import io.xol.chunkstories.api.rendering.textures.Cubemap;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.textures.Texture1D;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture3D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.shaders.ShaderProgram;

public class TexturingConfigurationImplementation
{
	private Map<String, Texture1D> textures1d;
	private Map<String, Texture2D> textures2d;
	private Map<String, Texture3D> textures3d;
	private Map<String, Cubemap> cubemaps;
	private Map<String, ArrayTexture> arrayTextures;
	
	private Map<String, Texture> allTextures = new HashMap<>();

	public TexturingConfigurationImplementation()
	{
		this.textures1d = new HashMap<String, Texture1D>();
		this.textures2d = new HashMap<String, Texture2D>();
		this.textures3d = new HashMap<String, Texture3D>();
		this.cubemaps = new HashMap<String, Cubemap>();
		this.arrayTextures = new HashMap<String, ArrayTexture>();
	}

	public void bindTexture1D(String textureSamplerName, Texture1D texture)
	{
		textures1d.put(textureSamplerName, texture);
		allTextures.put(textureSamplerName, texture);
	}

	public void bindTexture2D(String textureSamplerName, Texture2D texture)
	{
		textures2d.put(textureSamplerName, texture);
		allTextures.put(textureSamplerName, texture);
	}
	
	public void bindTexture3D(String textureSamplerName, Texture3D texture)
	{
		textures3d.put(textureSamplerName, texture);
		allTextures.put(textureSamplerName, texture);
	}

	public void bindCubemap(String cubemapSamplerName, Cubemap cubemapTexture)
	{
		cubemaps.put(cubemapSamplerName, cubemapTexture);
		allTextures.put(cubemapSamplerName, cubemapTexture);
	}

	public void bindArrayTexture(String textureSamplerName, ArrayTexture texture) {
		arrayTextures.put(textureSamplerName, texture);
		allTextures.put(textureSamplerName, texture);
	}

	public void clear() {
		this.textures1d.clear();
		this.textures2d.clear();
		this.textures3d.clear();
		this.cubemaps.clear();
		this.arrayTextures.clear();
		this.allTextures.clear();
	}

	/** Represents the unbund texture (id=0) for one texture type */
	static class DefaultTexture implements Texture {
		
		final SamplerType samplerType;
		public DefaultTexture(SamplerType samplerType) {
			this.samplerType = samplerType;
		}

		@Override
		public TextureFormat getType() {
			return null;
		}

		@Override
		public void bind() {
			switch(samplerType) {
			case ARRAY_TEXTURE_2D:
				glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
				break;
			case CUBEMAP:
				glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
				break;
			case TEXTURE_1D:
				glBindTexture(GL_TEXTURE_1D, 0);
				break;
			case TEXTURE_2D:
				glBindTexture(GL_TEXTURE_2D, 0);
				break;
			case TEXTURE_3D:
				glBindTexture(GL_TEXTURE_3D, 0);
				break;
			default:
				break;
			
			}
		}

		@Override
		public boolean destroy() {
			return false;
		}

		@Override
		public long getVramUsage() {
			return 0;
		}
	}
	
	static DefaultTexture[] defaultTextures = new DefaultTexture[Shader.SamplerType.values().length];
	static {
		for(int i = 0; i < Shader.SamplerType.values().length; i++)
			defaultTextures[i] = new DefaultTexture(Shader.SamplerType.values()[i]);
	}
	
	private Shader lastShaderConfigured = null;
	private Map<Texture, Integer> alreadyBound = new HashMap<>();
	private Texture[] boundTextures = new Texture[RenderingConfig.gl_MaxTextureUnits];
	
	/**
	 * Setups the required texturing units and links the shaders uniforms to them
	 */
	public void setup(RenderingInterface renderingInterface) throws NotEnoughtTextureUnitsException
	{
		ShaderProgram shaderProgram = (ShaderProgram) renderingInterface.currentShader();
		
		//Drop the older bound textures when a new shader is used
		if(lastShaderConfigured != shaderProgram) {
			alreadyBound.clear();
			this.resetBoundTextures();
		} else {
			//Go through the already bound textures
			Iterator<Entry<Texture, Integer>> i = alreadyBound.entrySet().iterator();
			while(i.hasNext()) {
				Entry<Texture, Integer> e = i.next();
				Texture texture = e.getKey();
				
				//If one texture is no longer used, remove it
				if(!allTextures.values().contains(texture)) {
					i.remove();
					boundTextures[e.getValue()] = null;
				}
			}
		}
		
		//For each sampler defined in the shader we will try to bind the necessary texture
		for(Entry<String, SamplerType> e : shaderProgram.samplers().entrySet()) {
			String samplerName = e.getKey();
			//System.out.println("figuring out texture for"+samplerName);
			
			Texture texture = allTextures.get(samplerName);
			if(texture == null || texture.getSamplerType() != e.getValue()) //No texture or the wrong type supplied ? No worries
				texture = defaultTextures[e.getValue().ordinal()];

			//System.out.println(texture);
			
			int alreadyBoundTextureUnit = alreadyBound.getOrDefault(texture, -1);
			
			if(alreadyBoundTextureUnit == -1) {
				int freeTextureUnit = findFreeTextureUnit();
				this.selectTextureUnit(freeTextureUnit);
				
				texture.bind();
				alreadyBound.put(texture, freeTextureUnit);
				boundTextures[freeTextureUnit] = texture;
				
				int uniform = shaderProgram.getUniformLocation(samplerName);
				if(uniform == -1)
					continue;
				
				glUniform1i(uniform, freeTextureUnit);
				
				//if(shaderProgram.getShaderName().contains("postprocess"))
				//	System.out.println("bound " + samplerName + " to " + freeTextureUnit + " ("+uniform+") :" + texture);
			} else {
				glUniform1i(shaderProgram.getUniformLocation(samplerName), alreadyBoundTextureUnit);
			}
		}
		
		lastShaderConfigured = shaderProgram;
	}

	private int findFreeTextureUnit() {
		for(int i = 0; i < boundTextures.length; i++) {
			if(boundTextures[i] == null)
				return i;
		}
		throw new RuntimeException("Out of texture units!");
	}

	private void selectTextureUnit(int id) throws NotEnoughtTextureUnitsException
	{
		if (id >= RenderingConfig.gl_MaxTextureUnits)
			throw new NotEnoughtTextureUnitsException();
		glActiveTexture(GL_TEXTURE0 + id);
	}

	public void resetBoundTextures()
	{
		for(int i = 0; i < boundTextures.length; i++)
			boundTextures[i] = null;
	}
}
