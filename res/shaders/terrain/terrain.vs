#version 150 core
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec4 vertexIn;
//in int voxelDataIn;
in vec4 normalIn;
in vec4 displacementIn;
in ivec4 indexIn;

//Passed variables
out vec3 vertexPassed;
out vec3 normalPassed;
out vec2 lightMapCoords;
out vec2 textureCoord;
out vec3 eyeDirection;
out float fogIntensity;
out float fresnelTerm;
out ivec4 indexPassed;

//Complements vertexIn
uniform vec2 visualOffset;

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

uniform usampler2DArray heights;
uniform int lodLevel;
uniform int maskPresence;

//Unused
//flat out int voxelData;

uint access(usampler2DArray tex, vec2 coords) {
	if(coords.x <= 1.0) {
		if(coords.y <= 1.0) {
			return texture(tex, vec3(coords, indexIn.x)).r;
		}
		else {
			return texture(tex, vec3(coords - vec2(0.0, 1.0), indexIn.y)).r;
		}
	}
	else {
		if(coords.y <= 1.0) {
			return texture(tex, vec3(coords - vec2(1.0, 0.0), indexIn.z)).r;
		}
		else {
			return texture(tex, vec3(coords - vec2(1.0), indexIn.w)).r;
		}
	}
}

void main()
{
	//Displacement from texture & position
	vec4 vertice = vec4(vertexIn.xyz, 1.0);
	vertice.y -= 0.2;
	
	vec2 inMeshCoords = (vertice.zx + mod(displacementIn.yx, 256.0) + vec2(0.5))/256.0;
	textureCoord = inMeshCoords;
	
	//Normals decoding and passing
	//normalPassed = normalIn.xyz * 0.5 - vec3(0.5);
	//normalPassed = normalize(normalPassed);
	normalPassed = vec3(0.0, 1.0, 0.0);
	
	indexPassed = indexIn;
	vertice.xz += displacementIn.xy;
	vertice.y += access(heights, inMeshCoords);//texture(heights, vec3(inMeshCoords, indexIn.x)).r;
	
	//vertice.xz += visualOffset.xy;
	
	//Fresnel equation for water
	fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(vertice.xyz - camPos), normalPassed), 0.0, 1.0);
	
	//Pass data
	vertexPassed = vertice.xyz;
	eyeDirection = vertice.xyz-camPos;
	
	<ifdef hqTerrain>
	if(lodLevel ==0 && maskPresence == 1) {
		vertice.y -= (1.0 - clamp(length(eyeDirection), 0.0, 512) / 512.0) * 32;
	}
	<endif hqTerrain>
	
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
    gl_Position = modelViewProjectionMatrix * vertice + vec4(0.0, 0.0, 0.001, 0.0);
}
