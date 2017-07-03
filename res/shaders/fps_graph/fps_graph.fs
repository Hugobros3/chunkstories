#version 150
// Copyright 2015 XolioWare Interactive

uniform float currentTiming;

in float height;
in float pos;

out vec4 fragColor;

uniform vec3 graphColour;

uniform float sizeInPixels;
uniform float heightInPixels;

uniform sampler1D frametimeData;

uniform float shade;

void main()
{
	float alpha = 1.0 - (mod(currentTiming - pos / sizeInPixels * 1024 + 1024, 1024))/1024.0 * 1.5;
	
	fragColor = vec4(vec3(0.0), 0.2);
	float min = texture1D(frametimeData, (pos - 2.0) / sizeInPixels).r;
	float max = texture1D(frametimeData, pos / sizeInPixels).r;
	
	float delta = (height) - max;
	if(abs(delta) < 2)
		fragColor += vec4(graphColour, alpha);
	else if(shade >= 1.0 && height < max) {	
		fragColor += vec4(graphColour, alpha) * 0.5;
	}
	
	if(shade < 1.0) {
		if(height >= min && height <= max)
			fragColor += vec4(graphColour, alpha) * 0.5;
		
		if(height >= max && height <= min)
			fragColor += vec4(graphColour, alpha) * 0.5;
	}
	
}