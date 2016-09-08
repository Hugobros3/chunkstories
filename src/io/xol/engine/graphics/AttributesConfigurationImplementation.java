package io.xol.engine.graphics;

import java.util.HashMap;
import java.util.Map;

import io.xol.chunkstories.api.exceptions.AttributeNotPresentException;
import io.xol.chunkstories.api.rendering.AttributeSource;
import io.xol.chunkstories.api.rendering.AttributesConfiguration;

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

}
