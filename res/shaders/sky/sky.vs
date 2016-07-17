#version 130

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec2 vertexIn;

//Passed variables
out vec3 eyeDirection;

//Sky
uniform vec3 sunPos;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 untranslatedMV;
uniform mat4 untranslatedMVInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
	vec4 transformedSS = vec4(vertexIn.x, vertexIn.y, -1.0, 1.0);
	
	eyeDirection = normalize(untranslatedMVInv * projectionMatrixInv * transformedSS ).xyz;
	
    gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}