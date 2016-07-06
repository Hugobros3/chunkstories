#version 120
//Entry attributes
attribute vec4 vertexIn;
attribute vec2 texCoordIn;
attribute vec4 colorIn;
attribute vec4 normalIn;

varying vec2 texcoord;
varying vec4 lightMapCoords;

//Lighthing
uniform float sunIntensity;
uniform vec3 sunPos;
varying vec3 fastColor;
varying float NdotL; // Face luminosity

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
varying float fresnelTerm;

uniform vec3 camPos;
uniform vec3 objectPosition;

uniform float vegetation;

uniform float yAngle;

varying float chunkFade;
uniform float viewDistance;

varying vec4 modelview;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform mat4 modelViewProjectionMatrix;
uniform mat4 modelViewProjectionMatrixInv;

varying float waterFogI;

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
	texcoord = texCoordIn;
	texcoord /= 32768.0;
	varyingNormal = (normalIn.xyz-0.5)*2.0;
	vec4 v = vec4(vertexIn.xyz, 1);
	//Move vertex if needed
	
	v+=vec4(objectPosition,0);
	
	//v.y -= colorIn.a / 15.0;
	
	varyingVertex = v;
	
	fresnelTerm = 0.1 + 0.6 * clamp(0.7 + dot(normalize(v.xyz - camPos), vec3(0, 1.0 , 0)), 0.0, 1.0);
	
	//Compute lightmap coords
	lightMapCoords = vec4(colorIn.r, colorIn.g, colorIn.b, 0);
	
	gl_Position = modelViewProjectionMatrix * v;
	
	//Eye transform
	eye = v.xyz-camPos;
	
	//Compute NdotL
	NdotL = max(dot(normalize(normalMatrix * varyingNormal), normalize(normalMatrix * sunPos)), 0.0);
	
	//fresnelTerm = clamp(cos(yAngle),0,0.8);
	
	waterFogI = length(eye)/(viewDistance/2.0-16);
	
	//Fog calculation
	
}