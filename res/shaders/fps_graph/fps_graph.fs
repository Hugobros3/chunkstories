#version 150
// Copyright 2015 XolioWare Interactive

uniform float currentTiming;

in float height;
in float pos;

out vec4 fragColor;

void main()
{
	//Diffuse G-Buffer
	float alpha = 1.0 - (mod(currentTiming - pos + 1000, 1000))/1000.0f;
	
	float r = max(height - 0.6f, 0.0) / 33.3f;
	float g = 1-max(height - 16.6f, 0.0) / 33.3f;
	
	fragColor = vec4(r, g, 0.0, alpha );
}