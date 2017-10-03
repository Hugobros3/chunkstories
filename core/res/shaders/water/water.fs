#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D diffuseTexture; // Blocks texture atlas

//Passed variables
in vec3 normalPassed;
in vec4 vertexPassed;
in vec2 texCoordPassed; // Coordinate
in vec3 eyeDirection; // eyeDirection-position
in vec4 lightMapCoords; //Computed in vertex shader
in float fresnelTerm;

out vec4 outColor;

//Block and sun Lightning
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform sampler2D lightColors; // Sampler to lightmap
uniform vec3 sunPos; // Sun position

//Normal mapping
uniform sampler2D normalTextureShallow;
uniform sampler2D normalTextureDeep;

uniform float shadowVisiblity; // Used for night transitions ( w/o shadows as you know )

//Water
uniform float time;
// Screen space reflections
uniform vec2 screenSize;

//Fog
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform sampler2D readbackShadedBufferTemp;

uniform float underwater;

//Gamma constants
<include ../lib/gamma.glsl>
<include ../lib/transformations.glsl>

<include ../lib/normalmapping.glsl>

vec3 mixedTextures(float blend, vec2 coords)
{
	return mix(texture(normalTextureShallow, coords).rgb, texture(normalTextureDeep, coords * 0.125).rgb, 0);
}

void main(){
	
	vec4 baseColor = texture(diffuseTexture, texCoordPassed);
	
	//if(baseColor.a < 1.0)
	//	discard;
	
	outColor = vec4(0.0, 0.0, 1.0, 0.5);
}
