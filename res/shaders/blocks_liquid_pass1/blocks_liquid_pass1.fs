#version 150 core
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D diffuseTexture; // Blocks texture atlas
uniform sampler2D normalTexture;

//Passed variables
in vec4 vertexPassed;
in vec3 normalPassed;
in vec2 texCoordPassed;
in vec3 eyeDirection;
in vec4 lightMapCoords;
in float fresnelTerm;
in float waterFogI;

out vec4 fragColor;

//Block and sun Lightning
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform vec3 sunPos; // Sun position
uniform sampler2D lightColors; // Sampler to lightmap

//Shadow shit
uniform float shadowVisiblity; // Used for night transitions ( w/o shadows as you know )

//Water
uniform float time;
// Screen space reflections
uniform vec2 screenSize;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);
const float shadowStrength = 0.75;

uniform sampler2D readbackAlbedoBufferTemp;
uniform sampler2D readbackMetaBufferTemp;
uniform sampler2D readbackDepthBufferTemp;

uniform vec2 shadedBufferDimensions;
uniform float viewDistance;

uniform float underwater;

//Gamma constants
<include ../lib/gamma.glsl>
<include ../lib/transformations.glsl>

void main(){

	//Basic texture color
	vec2 coords = (gl_FragCoord.xy)/screenSize;
	
	vec4 baseColor = texture(diffuseTexture, texCoordPassed);
	
	vec4 worldspaceFragment = convertScreenSpaceToCameraSpace(coords, readbackDepthBufferTemp);
	
	//Pass 1
	vec4 meta = texture(readbackMetaBufferTemp, coords);
	
	vec3 blockLight = textureGammaIn(lightColors,vec2(meta.x, 0)).rgb;
	vec3 sunLight = textureGammaIn(lightColors,vec2(0, meta.y)).rgb;
	
	sunLight = mix(sunLight, sunLight * shadowColor, shadowVisiblity * 0.75);
	
	vec3 finalLight = blockLight;// * (1-sunLight);
	finalLight += sunLight;
	finalLight *= (1-meta.z);

	//coords += 15.0 * (1 - length(worldspaceFragment) / viewDistance) * vec2( normal.xz ) / screenSize;
	vec4 refracted = texture(readbackAlbedoBufferTemp, coords);
	
	float waterFogI2 = length(worldspaceFragment) / viewDistance;
	refracted.rgb *= pow(finalLight + vec3(1.0) * (1-refracted.a*lightMapCoords.g), vec3(gammaInv));
	
	baseColor.rgb = mix(refracted.rgb, baseColor.rgb, clamp(waterFogI2*(1.0-underwater), 0.0, 1.0));
	
	fragColor = vec4(baseColor);
}
