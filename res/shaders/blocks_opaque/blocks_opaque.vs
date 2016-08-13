#version 130
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec4 vertexIn;
in vec2 texCoordIn;
in vec3 colorIn;
in vec4 normalIn;

//Passed variables
out vec2 texCoordPassed;
out vec3 lightMapCoords;
out float fresnelTerm;
out vec3 normalPassed;
out vec4 vertexPassed;
out vec3 eyeDirection;
out float rainWetness;

//Lighthing
uniform float sunIntensity;

uniform float wetness;
uniform float time;
uniform vec3 objectPosition;

//Common camera matrices & uniforms
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
uniform vec3 camPos;

void main(){
	//Usual variable passing
	texCoordPassed = texCoordIn;
	
	vec4 v = vec4(vertexIn.xyz, 1.0);
	//v.y += gl_VertexID / 6000.0;
	
	<ifdef dynamicGrass>
	float movingness = normalIn.w;
	if(movingness > 0)
	{
		v.x += sin(time + v.z + v.y / 2.0) * 0.1;
		v.z += cos(time + v.x*1.5 + 0.3) * 0.1;
	}
	<endif dynamicGrass>
	
	v+=vec4(objectPosition,0);
	
	vertexPassed = v;
	normalPassed =  (normalIn.xyz-0.5)*2.0;//normalIn;
	
	fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(v.xyz - camPos), vec3(normalPassed)), 0.0, 1.0);
	
	texCoordPassed /= 32768.0;
	
	//Compute lightmap coords
	rainWetness = wetness*clamp((colorIn.g * 16.0 - 15.0),0,1.0);
	lightMapCoords = vec3(vec2(colorIn.r, colorIn.g)*(1.0 - colorIn.b * 0.15), 0*colorIn.b);
	
	//lightMapCoords = vec3(colorIn.rgb);
	//lightMapCoords.y *= sunIntensity;
	
	gl_Position = modelViewProjectionMatrix * v;
	
	//eyeDirection transform
	eyeDirection = v.xyz;
}