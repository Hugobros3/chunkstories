#version 120
// Copyright 2015 XolioWare Interactive

varying vec4 interpolatedColor;

varying vec2 texCoord;
uniform sampler2D diffuseTexture;

void main()
{
	//Diffuse G-Buffer
	gl_FragColor = texture2D(diffuseTexture, texCoord) * interpolatedColor;
}