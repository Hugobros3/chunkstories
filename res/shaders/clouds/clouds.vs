#version 130
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec4 vertexIn;
in vec4 normalIn;
in float alphaIn;

//Passed variables
out vec3 eyeDirection;
out float alphaPassed;

//Misc
uniform vec3 camPos;
uniform vec3 sunPos;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
	alphaPassed = alphaIn;
	
	eyeDirection = normalize(vertexIn.xyz - camPos);
	
    gl_Position = projectionMatrix * modelViewMatrix * vertexIn;
}