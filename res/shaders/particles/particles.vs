#version 130

varying vec4 texcoord;
varying vec2 lightMapCoords;

//Lighthing
uniform float sunIntensity;

// The normal we're going to pass to the fragment shader.
varying vec3 varyingNormal;
// The vertex we're going to pass to the fragment shader.
varying vec4 varyingVertex;

varying float fogI;

uniform float time;
varying vec3 eye;
uniform vec3 camPos;
uniform vec3 objectPosition;

uniform float vegetation;
varying float chunkFade;
uniform float viewDistance;

varying vec4 modelview;

attribute vec4 particlesPositionIn;
attribute vec2 billboardSquareCoordsIn;
attribute vec2 textureCoordinatesIn;

uniform float areTextureCoordinatesIninatesSupplied;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float billboardSize;

varying float back;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

void main(){
	//Usual variable passing
	
	if(areTextureCoordinatesIninatesSupplied < 0.5)
		texcoord = vec4(billboardSquareCoordsIn*0.5+0.5, 0, 0);
	else
		texcoord = vec4(textureCoordinatesIn, 0, 0);
	
	vec4 v = particlesPositionIn;//vec4(gl_Vertex);
	
	//TODO : Clean this shit up	
	//v+=vec4(objectPosition, 0.0);
	
	//v += modelViewMatrixInv * vec4(billboardSquareCoordsIn,0,0);
	
	varyingVertex = v;
	varyingNormal = gl_Normal;
	
	//Compute lightmap coords
	lightMapCoords = vec2(0.0, 1.0);
	//baseLight *= texture2DGammaIn(lightColors, vec2(time, 1.0)).rgb;
	
	
	modelview = modelViewMatrix * v;
	
	modelview += vec4(billboardSquareCoordsIn*billboardSize, 0.0, 0.0);
	
	vec4 clochard = projectionMatrix * modelview;
	
	gl_Position = clochard;
	//gl_Position = vec4(billboardSquareCoordsIn, 0.0, 1.0);
	
	//Eye transform
	//eye = v.xyz-camPos;
}
