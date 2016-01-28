#version 120
//Entry attributes
attribute vec4 vertexIn;
attribute vec2 texCoordIn;
attribute vec4 normalIn;

varying vec2 texcoord;
varying vec3 lightMapCoords;
varying float fresnelTerm;

//Lighthing
uniform float sunIntensity;
uniform vec3 sunPos; // Sun position
// The normal we're going to pass to the fragment shader.
varying vec3 varyingNormal;
// The vertex we're going to pass to the fragment shader.
varying vec4 varyingVertex;

//Shadow shit
uniform mat4 shadowMatrix;
uniform mat4 shadowMatrix2;
 
varying vec4 coordinatesInShadowmap;
varying vec4 coordinatesInShadowmap2;

varying float fogI;

uniform float time;
varying vec3 eye;
uniform vec3 camPos;
uniform vec3 borderShift;

uniform float vegetation;
varying float chunkFade;
uniform float viewDistance;

varying vec4 modelview;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec3 givenLightmapCoords;

uniform mat4 localTransform;
uniform mat4 localTransformNormal;
uniform mat4 offsetTransform;

//Weather
uniform float wetness;
varying float rainWetness;

void main(){
	//Usual variable passing
	texcoord = texCoordIn;
	vec4 v = localTransform * vec4(vertexIn.xyz, 1.0);
	
	v+=vec4(borderShift,0);
	
	varyingVertex = v;
	varyingNormal = (localTransformNormal * normalIn).xyz;//(normalIn.xyz-0.5)*2.0;//normalIn;
	
	fresnelTerm = 0.0 + 1.0 * clamp(0.7 + dot(normalize(v.xyz - camPos), vec3(varyingNormal)), 0.0, 1.0);
	
	//texcoord /= 32768.0;
	
	//Compute lightmap coords
	rainWetness = wetness;//wetness*clamp((colorIn.g-15.0/16.0)*16,0,0.5);
	lightMapCoords = givenLightmapCoords;
	lightMapCoords.y *= sunIntensity;
	
	//Translate vertex
	modelview = modelViewMatrix * v;
	
	gl_Position = projectionMatrix * modelview;
	
	
	//Eye transform
	eye = v.xyz-camPos;
	
	//Fog calculation
	
	//Chunk-aligned linear fog
	vec3 camPosChunk = camPos;
	camPosChunk.x = floor((camPosChunk.x+16)/32)*32;
	camPosChunk.z = floor((camPosChunk.z+16)/32)*32;
	
	vec3 eyeChunk = v.xyz-camPosChunk;
	
	float fogStartDistance = clamp(floor(viewDistance/32)*32-12,32,512);
	
	chunkFade = clamp((abs(eyeChunk.x)-fogStartDistance)/12,0,1)+
	clamp((abs(eyeChunk.z)-fogStartDistance)/12,0,1);
	
	chunkFade = 1-clamp(chunkFade,0,1);
}