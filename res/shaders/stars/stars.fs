#version 150
// Copyright 2015 XolioWare Interactive

in vec4 calculatedLight;

uniform vec3 color;

in float blend;

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
	fragColor = vec4(color, clamp(blend * 0.5, 0.0, 1.0));
}