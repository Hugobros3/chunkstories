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
out vec3 normalPassed;
out vec4 vertexPassed;
out vec2 texCoordPassed; // Coordinate
out vec3 eyeDirection; // eyeDirection-position
out vec4 lightMapCoords; //Computed in vertex shader
out float fresnelTerm;

//Lighthing
uniform float sunIntensity;
uniform vec3 sunPos;

//Shadow shit
uniform float time;

uniform mat4 objectMatrix;
uniform mat3 objectMatrixNormal;

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

float getFogI(vec3 position, float fogDistance)
{
	float dist = clamp(length(position)-fogDistance,0,10000);
	const float LOG2 = 1.442695;
	float density = 0.0025;
	float fogFactor = exp2( -density * 
					   density * 
					   dist * 
					   dist * 
					   LOG2 );
	float fogI = clamp(fogFactor, 0.0, 1.0);
	return fogI;
}

void main(){
	//Usual variable passing
	texCoordPassed = texCoordIn;
	texCoordPassed /= 32768.0;
	normalPassed = (normalIn.xyz-0.5)*2.0;
	
	vec4 vertex = objectMatrix * vec4(vertexIn.xyz, 1);
	//Move vertex if needed
	
	vertexPassed = vertex;
	
	fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(vertex.xyz - camPos), vec3(0, 1.0 , 0)), 0.0, 1.0);
	
	//Compute lightmap coords
	lightMapCoords = vec4(colorIn.r * 16.0, colorIn.g * 16.0, colorIn.b * 16.0, colorIn.a);
	
	gl_Position = modelViewProjectionMatrix * vertex;
	
	//eyeDirection transform
	eyeDirection = vertex.xyz-camPos;
}