#version 330
// Copyright 2015 XolioWare Interactive

in vec4 interpolatedColor;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

out vec4 fragColor;

void main()
{
	//Diffuse G-Buffer
	fragColor = interpolatedColor;
}