#version 130
varying vec3 vertex;
varying vec4 color;

varying float fogI;
uniform vec3 camPos;
varying float fresnelTerm;
varying vec3 eye;

uniform sampler2D heightMap;

uniform vec2 chunkPosition;
varying vec2 chunkPositionFrag;
uniform vec2 chunkPositionActual;
uniform vec2 regionPosition;
varying vec2 textureCoord;

varying float chunkFade;

uniform float viewDistance;
varying vec2 lightMapCoords;

uniform float sunIntensity;

varying vec3 normalHeightmap;

uniform vec3 sunPos; // Sun position

// Chunk loading/fade control
uniform float maxYChunkLoaded;
uniform float minYChunkLoaded;

uniform float maxXChunkLoaded;
uniform float minXChunkLoaded;

uniform float maxZChunkLoaded;
uniform float minZChunkLoaded;

uniform float terrainHeight;

varying float lowerFactor;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform mat4 modelViewProjectionMatrix;
uniform mat4 modelViewProjectionMatrixInv;

attribute vec4 vertexIn;
attribute vec4 normalIn;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

void main()
{
	chunkPositionFrag = chunkPosition;

	//Displacement from texture & position
	vec4 v = vec4(vertexIn);
	//v.y += 256 * normalIn.w;
	textureCoord = (v.zx)/256.0;
	
	float packedNormals = normalIn.x;
	
	float baseHeight = texture2D(heightMap,textureCoord).r;
	//baseHeight = 50;
	
	//v.y += baseHeight;
	
	//Normal computation, brace yourselves
	normalHeightmap = vec3(0.0, 1.0, 0.0); //Start with an empty vector
	
	float normalXplusHeight = texture2D(heightMap,textureCoord+vec2(1.0/256.0, 0.0)).r;
	float alpha = atan(normalXplusHeight-baseHeight);
	vec3 normalXplus = vec3(0.0, cos(alpha), -sin(alpha));
	
	normalHeightmap += normalXplus;
	
	float normalZplusHeight = texture2D(heightMap,textureCoord+vec2(0.0, 1.0/256.0)).r;
	alpha = atan(normalZplusHeight-baseHeight);
	vec3 normalZplus = vec3(-sin(alpha), cos(alpha), 0.0);
	
	normalHeightmap += normalZplus;
	
	float normalXminusHeight = texture2D(heightMap,textureCoord-vec2(1.0/256.0, 0.0)).r;
	alpha = atan(normalXminusHeight-baseHeight);
	vec3 normalXminus = vec3(0.0, cos(alpha), sin(alpha));
	
	normalHeightmap += normalXminus;
	
	float normalZminusHeight = texture2D(heightMap,textureCoord-vec2(0.0, 1.0/256.0)).r;
	alpha = atan(normalZminusHeight-baseHeight);
	vec3 normalZminus = vec3(sin(alpha), cos(alpha), 0.0);
	
	normalHeightmap += normalZminus;
	
	normalHeightmap = normalIn.xyz * 0.5 - vec3(0.5);
	//I'm happy and proud to say I came up with the maths by myself :)
	
	//normalHeightmap = vec3(floor(packedNormals / 64.0) - 1.0, floor(mod(packedNormals, 64.0) / 16.0) - 1.0, mod(packedNormals, 16.0) - 1.0);
	
	normalHeightmap = normalize(normalHeightmap);
	
	v.xz += chunkPosition.xy;
	
	//fresnelTerm = 1.0 - clamp(dot(normalHeightmap, normalize(v.xyz)), 0.0, 1.0);
	fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(v.xyz - camPos), normalHeightmap), 0.0, 1.0);
	
	vertex = v.xyz;
	eye = v.xyz-camPos;
	
	//float lowerFactorT = (clamp(viewDistance-length(eye.xz)-16.0, 0.0, viewDistance) / viewDistance) * clamp(2.0-abs(eye.y/32.0), 0.0, 1.0);
	
	float camTerrainDiff = terrainHeight - v.y;
	
	float lowerFactorT = clamp((viewDistance-abs(eye.x)-16.0) / viewDistance, 0.0, 1.0) * clamp((viewDistance-abs(eye.z)-16.0) / viewDistance, 0.0, 1.0) * clamp(2.0-abs(camTerrainDiff/32.0), 0.0, 1.0);
	
	lowerFactor = lowerFactorT;
	//v.y -= 1.0 + lowerFactorT * 2 * lowerFactorT * 2 * 32.0;
	//v.y = mix(v.y, min(v.y, camPos.y-32.0), clamp(lowerFactorT * 3.0, 0.0, 1.0));
	
	vec4 projected = modelViewProjectionMatrix * v;
	
	
	projected.z += 0.1;
	
    gl_Position = projected;
	
	lightMapCoords = vec2(0.0, sunIntensity);
	
	//Distance fog
	
	vec3 sum = (modelViewMatrix * v).xyz;
	float dist = length(sum)-fogStartDistance;
	const float LOG2 = 1.442695;
	float density = 0.0025;
	float fogFactor = exp2( -density * 
					   density * 
					   dist * 
					   dist * 
					   LOG2 );
	fogFactor = (dist) / (fogEndDistance-fogStartDistance);
	fogI = clamp(fogFactor, 0.0, 1.0);
	
	
}
