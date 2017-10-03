#version 330
// Copyright 2015 XolioWare Interactive

in vec4 interpolatedColor;

in vec2 texCoord;
uniform sampler2D diffuseTexture;

out vec4 fragColor;

void main()
{
	//Diffuse G-Buffer
	vec4 diffuse = texture(diffuseTexture, texCoord);
	if(diffuse.a < 1.0 && diffuse.a > 0.0)
	{
		diffuse.rgb *= vec3(0.2, 0.8, 0.2);
		diffuse.a = 1.0;
	}
	fragColor = diffuse * interpolatedColor;
}