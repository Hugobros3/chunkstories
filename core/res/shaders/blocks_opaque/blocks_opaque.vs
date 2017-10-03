#version 330
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
out vec2 worldLight;
out float fresnelTerm;
out vec3 normalPassed;
out vec4 vertexPassed;
out vec3 eyeDirection;
out float rainWetness;

//Lighthing
uniform float sunIntensity;

uniform float wetness;
uniform float time;

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

uniform mat4 objectMatrix;
uniform mat3 objectMatrixNormal;

void main(){
	//Usual variable passing
	texCoordPassed = texCoordIn;
	
	vec4 vertex = objectMatrix * vec4(vertexIn.xyz, 1.0);
	
	<ifdef dynamicGrass>
		float movingness = normalIn.w;
		if(movingness > 0)
		{
			vertex.x += sin(time + vertex.z + vertex.y / 2.0) * 0.1;
			vertex.z += cos(time + vertex.x*1.5 + 0.3) * 0.1;
		}
	<endif dynamicGrass>
	
	vertexPassed = vertex;
	normalPassed =  (normalIn.xyz-0.5)*2.0;
	
	fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(vertex.xyz - camPos), vec3(normalPassed)), 0.0, 1.0);
	
	texCoordPassed /= 32768.0;
	
	//Compute lightmap coords
	rainWetness = wetness*clamp((colorIn.g * 16.0 - 0.85)*16,0,1.0);
	worldLight = vec2(colorIn.r * 17.0, colorIn.g * 17.0)*(1.0 - colorIn.b * 65.75 * 0.25);
	
	gl_Position = modelViewProjectionMatrix * vertex;
	
	//eyeDirection transform
	eyeDirection = vertex.xyz - camPos;
}