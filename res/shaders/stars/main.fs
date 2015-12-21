#version 120
// Copyright 2015 XolioWare Interactive

varying vec4 calculatedLight;

uniform vec3 color;

varying float blend;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
	//Diffuse G-Buffer
	gl_FragData[0] = vec4(color, 1.0 * blend * 0.5);
	//Normal G-Buffer
	gl_FragData[1] = vec4(0.0, 1.0, 0.0, blend * 1.0);
	//Light color G-buffer
	gl_FragData[2] = vec4(calculatedLight.rgb, blend);
	//Specular G-Buffer
	gl_FragData[3] = vec4(0.0);
}