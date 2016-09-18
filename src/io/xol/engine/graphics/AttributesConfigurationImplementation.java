package io.xol.engine.graphics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.xol.chunkstories.api.exceptions.AttributeNotPresentException;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.AttributeSource;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.engine.graphics.shaders.ShaderProgram;

import static org.lwjgl.opengl.GL20.*;

public class AttributesConfigurationImplementation implements AttributesConfiguration
{
	Map<String, AttributeSource> attributes;
	
	public AttributesConfigurationImplementation()
	{
		attributes = new HashMap<String, AttributeSource>();
	}
	
	public AttributesConfigurationImplementation(Map<String, AttributeSource> attributes)
	{
		this.attributes = attributes;
	}
	
	@Override
	public Map<String, AttributeSource> getBoundAttributes()
	{
		return attributes;
	}

	public AttributesConfigurationImplementation bindAttribute(String attributeName, AttributeSource attributeSource) throws AttributeNotPresentException
	{
		//Clone the hashMap
		Map<String, AttributeSource> attributes = new HashMap<String, AttributeSource>(this.attributes);
		if(attributeSource == null)
			attributes.remove(attributeName);
		else
			attributes.put(attributeName, attributeSource);
		
		//Returns the new object
		return new AttributesConfigurationImplementation(attributes);
	}
	
	@Override
	public boolean isCompatibleWith(AttributesConfiguration attributesConfiguration)
	{
		//Simple check
		//TODO improve
		return attributesConfiguration == this;
	}

	@Override
	public void setup(RenderingInterface renderingInterface)
	{
		ShaderProgram shaderProgram = (ShaderProgram) renderingInterface.currentShader();
		
		Set<Integer> unusedAttributes = enabledVertexAttributes;
		enabledVertexAttributes = new HashSet<Integer>(enabledVertexAttributes);
		
		for(Entry<String, AttributeSource> e : attributes.entrySet())
		{
			String attributeName = e.getKey();
			AttributeSource attributeSource = e.getValue();
			
			int attributeLocation = shaderProgram.getVertexAttributeLocation(attributeName);
			if(attributeLocation == -1)
				continue;
			
			unusedAttributes.remove(attributeLocation);
			
			//Enable only when it wasn't
			if(enabledVertexAttributes.add(attributeLocation))
				glEnableVertexAttribArray(attributeLocation);
			
			attributeSource.setup(attributeLocation);
		}
		
		//Disable and forget about unused ones
		for(int unused : unusedAttributes)
		{
			glDisableVertexAttribArray(unused);
			enabledVertexAttributes.remove(unused);
		}
	}

	static Set<Integer> enabledVertexAttributes = new HashSet<Integer>();
}
