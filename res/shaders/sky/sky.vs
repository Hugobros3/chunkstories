#version 120
varying vec2 vertex;

attribute vec2 vertexIn;

uniform vec3 sunPos;

varying vec3 eyeDirection;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
    vertex = vertexIn;
	
	vec4 transformedSS = vec4(vertexIn.x, vertexIn.y, -1.0, 1.0);
	
	eyeDirection = (modelViewMatrixInv * projectionMatrixInv * transformedSS ).xyz;
	
    gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}