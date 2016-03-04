#version 120
// Copyright 2015 XolioWare Interactive

varying vec4 interpolatedColor;

varying vec2 texCoord;
uniform sampler2D diffuseTexture;

void main()
{
	//Diffuse G-Buffer
	vec4 diffuse = texture2D(diffuseTexture, texCoord);
	if(diffuse.a < 1.0 && diffuse.a > 0.0)
	{
		diffuse.rgb *= vec3(0.2, 0.8, 0.2);
		diffuse.a = 1.0;
	}
	gl_FragColor = diffuse * interpolatedColor;
}