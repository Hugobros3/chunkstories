#version 140
//Entry attributes
attribute vec4 vertexIn;
attribute vec2 texCoordIn;
attribute vec4 colorIn;
attribute vec4 normalIn;

varying vec2 texcoord;
varying vec3 lightMapCoords;
varying float fresnelTerm;
varying float chunkFade;
varying float rainWetness;
varying float fogI;
varying vec4 modelview;
varying vec3 eye;

varying vec3 varyingNormal;
varying vec4 varyingVertex;
varying vec4 colorPassed;

uniform float useColorIn;
uniform float useNormalIn;
uniform float isUsingInstancedData;
uniform sampler2D instancedDataSampler;

//Lighthing
uniform float sunIntensity;
uniform vec3 sunPos; // Sun position

uniform float time;
uniform vec3 camPos;
//TODO get rid of this legacy bs
uniform vec3 objectPosition;

uniform float vegetation;
uniform float viewDistance;
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec2 worldLight;

uniform mat4 localTransform;
uniform mat3 localTransformNormal;

uniform mat4 boneTransform;
uniform mat3 boneTransformNormal;

uniform mat4 offsetTransform;

//Weather
uniform float wetness;

void main(){
	//Usual variable passing
	texcoord = texCoordIn;
	vec4 v = localTransform * boneTransform * vec4(vertexIn.xyz, 1.0);
	
	
	if(isUsingInstancedData > 0)
	{
		mat4 matrixInstanced = mat4(texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 4, 32), (gl_InstanceID * 4) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 4 + 1, 32), (gl_InstanceID * 4 + 1) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 4 + 2, 32), (gl_InstanceID * 4 + 2) / 32), 0),
									texelFetch(instancedDataSampler, ivec2(mod(gl_InstanceID * 4 + 3, 32), (gl_InstanceID * 4 + 3) / 32), 0)
									);
	
		v = matrixInstanced * vec4(vertexIn.xyz, 1.0);
	}
	else
		v+=vec4(objectPosition,0);
	
	varyingVertex = v;
	varyingNormal =  localTransformNormal * boneTransformNormal * (normalIn).xyz;//(normalIn.xyz-0.5)*2.0;//normalIn;
	
	fresnelTerm = 0.0 + 1.0 * clamp(0.7 + dot(normalize(v.xyz - camPos), vec3(varyingNormal)), 0.0, 1.0);
	
	colorPassed = colorIn;
	
	//texcoord /= 32768.0;
	
	//Compute lightmap coords
	rainWetness = wetness;//wetness*clamp((colorIn.g-15.0/16.0)*16,0,0.5);
	lightMapCoords = vec3(worldLight / 15.0, 0);
	//lightMapCoords.y *= sunIntensity;
	
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