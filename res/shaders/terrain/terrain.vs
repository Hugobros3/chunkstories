#version 130
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec4 vertexIn;
in int voxelDataIn;
in vec4 normalIn;

//Passed variables
out vec3 vertexPassed;
out vec3 normalPassed;
out vec2 lightMapCoords;
out vec2 textureCoord;
out vec3 eyeDirection;
out float fogIntensity;
out float fresnelTerm;

//Complements vertexIn
uniform vec2 chunkPosition;

//Sky
uniform float sunIntensity;
uniform vec3 sunPos;

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
uniform float viewDistance;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

//Unused
//flat out int voxelData;

void main()
{
	//Displacement from texture & position
	vec4 vertice = vec4(vertexIn.xyz, 1.0);
	vertice.y -= 0.2;
	
	textureCoord = (vertice.zx + vec2(0.5))/256.0;
	
	//Normals decoding and passing
	normalPassed = normalIn.xyz * 0.5 - vec3(0.5);
	normalPassed = normalize(normalPassed);
	
	vertice.xz += chunkPosition.xy;
	
	//Fresnel equation for water
	fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(vertice.xyz - camPos), normalPassed), 0.0, 1.0);
	
	//Pass data
	vertexPassed = vertice.xyz;
	eyeDirection = vertice.xyz-camPos;
	lightMapCoords = vec2(0.0, sunIntensity);
	
	//Computes fog
	vec3 sum = (modelViewMatrix * vertice).xyz;
	float dist = length(sum)-fogStartDistance;
	const float LOG2 = 1.442695;
	float density = 0.0025;
	float fogFactor = exp2( -density * 
					   density * 
					   dist * 
					   dist * 
					   LOG2 );
	fogFactor = (dist) / (fogEndDistance-fogStartDistance);
	fogIntensity = clamp(fogFactor, 0.0, 1.0);
	
	//Output position
    gl_Position = modelViewProjectionMatrix * vertice;
}
