#version 120
//Entry attributes
attribute vec4 vertexIn;
attribute vec2 texCoordIn;
attribute vec3 colorIn;
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

varying vec4 coordinatesInShadowmap2;

varying float fogI;

uniform float time;
varying vec3 eye;
uniform vec3 camPos;
uniform vec3 objectPosition;

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

uniform mat4 untranslatedMV;
uniform mat4 untranslatedMVInv;

uniform mat4 modelViewProjectionMatrix;
uniform mat4 modelViewProjectionMatrixInv;

//Weather
uniform float wetness;
varying float rainWetness;

void main(){
	//Usual variable passing
	texcoord = texCoordIn;
	
	vec4 v = vec4(vertexIn.xyz, 1.0);
	
	<ifdef dynamicGrass>
	float movingness = normalIn.w;
	if(movingness > 0)
	{
		v.x += sin(time + v.z + v.y / 2.0) * 0.1;
		v.z += cos(time + v.x*1.5 + 0.3) * 0.1;
	}
	<endif dynamicGrass>
	
	v+=vec4(objectPosition,0);
	
	varyingVertex = v + vec4(camPos, 0.0);
	varyingNormal =  (normalIn.xyz-0.5)*2.0;//normalIn;
	
	fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(v.xyz - camPos), vec3(varyingNormal)), 0.0, 1.0);
	
	texcoord /= 32768.0;
	
	//Compute lightmap coords
	rainWetness = wetness*clamp((colorIn.g * 16.0 - 15.0),0,1.0);
	lightMapCoords = vec3(vec2(colorIn.r, colorIn.g)*(1.0 - colorIn.b * 0.15), 0*colorIn.b);
	
	//lightMapCoords = vec3(colorIn.rgb);
	//lightMapCoords.y *= sunIntensity;
	
	gl_Position = projectionMatrix * untranslatedMV * v;
	
	//Eye transform
	eye = v.xyz;
	
	//Fog calculation
	
	//Chunk-aligned linear fog
	
	/*vec3 camPosChunk = camPos;
	camPosChunk.x = floor((camPosChunk.x+16)/32)*32;
	camPosChunk.z = floor((camPosChunk.z+16)/32)*32;
	
	vec3 eyeChunk = v.xyz-camPosChunk;
	
	float fogStartDistance = clamp(floor(viewDistance/32)*32-12,32,512);
	
	chunkFade = clamp((abs(eyeChunk.x)-fogStartDistance)/12,0,1)+
	clamp((abs(eyeChunk.z)-fogStartDistance)/12,0,1);
	
	chunkFade = 1-clamp(chunkFade,0,1);*/
}