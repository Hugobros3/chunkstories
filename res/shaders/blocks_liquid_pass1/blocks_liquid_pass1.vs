#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec4 vertexIn;
in vec2 texCoordIn;
in vec4 colorIn;
in vec4 normalIn;

//Passed variables
out vec4 vertexPassed;
out vec3 normalPassed;
out vec2 texCoordPassed;
out vec3 eyeDirection;
out vec4 lightMapCoords;
out float fresnelTerm;
out float waterFogI;

//Lighthing
uniform float sunIntensity;
uniform vec3 sunPos;

//Shadow shit
uniform mat4 shadowMatrix;
uniform mat4 shadowMatrix2;

uniform float time;

uniform mat4 objectMatrix;
uniform mat3 objectMatrixNormal;

uniform float vegetation;

uniform float yAngle;
uniform float viewDistance;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform mat4 modelViewProjectionMatrix;
uniform mat4 modelViewProjectionMatrixInv;
uniform vec3 camPos;

void main(){
	//Usual variable passing
	texCoordPassed = texCoordIn;
	texCoordPassed /= 32768.0;
	normalPassed = (normalIn.xyz-0.5)*2.0;
	vec4 vertex = objectMatrix * vec4(vertexIn.xyz, 1);
	//Move vertex if needed
	
	vertexPassed = vertex;
	
	//eyeDirection transform
	eyeDirection = vertex.xyz-camPos;
	
	fresnelTerm = 0.1 + 0.6 * clamp(0.7 + dot(normalize(vertex.xyz - camPos), vec3(0, 1.0 , 0)), 0.0, 1.0);
	
	//Compute lightmap coords
	lightMapCoords = vec4(colorIn.r, colorIn.g, colorIn.b, 0);
	
	waterFogI = length(eyeDirection)/(viewDistance/2.0-16);
	
	gl_Position = modelViewProjectionMatrix * vertex;
}