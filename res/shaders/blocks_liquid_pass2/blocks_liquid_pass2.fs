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

out vec4 outDiffuseColor;
out vec4 outNormalColor;
out vec4 outMaterialColor;

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
	vec3 normal = vec3(0.0, 0.0, 1.0);
	
	vec3 nt = 1.0*(mixedTextures(lightMapCoords.a, (vertexPassed.xz/5.0+vec2(0.0,time)/50.0)/15.0).rgb*2.0-1.0);
	nt += 1.0*(mixedTextures(lightMapCoords.a, (vertexPassed.xz/2.0+vec2(-time,-2.0*time)/150.0)/2.0).rgb*2.0-1.0);
	nt += 0.5*(mixedTextures(lightMapCoords.a, (vertexPassed.zx*0.8+vec2(400.0, sin(-time/5.0)+time/25.0)/350.0)/10.0).rgb*2.0-1.0);
	nt += 0.25*(mixedTextures(lightMapCoords.a, (vertexPassed.zx*0.1+vec2(400.0, sin(-time/5.0)-time/25.0)/250.0)/15.0).rgb*2.0-1.0);
	
	nt = normalize(nt);
	
	float i = 1.0;
	
	normal.x += nt.r*i;
	normal.y += nt.g*i;
	normal.z += nt.b*i;
	
	normal = normalize(normal);
	
	normal = perturb_normal(normalPassed, eyeDirection, texCoordPassed, normal);
	normal = normalize(normalMatrix * normal);
	
	//Basic texture color
	vec2 coords = (gl_FragCoord.xy)/screenSize;
	
	vec4 baseColor = texture(diffuseTexture, texCoordPassed);
	
	float spec = fresnelTerm;
	vec4 worldspaceFragment = convertScreenSpaceToCameraSpace(coords, gl_FragCoord.z);
	
	<ifdef perPixelFresnel>
	float dynamicFresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(worldspaceFragment.xyz), normal), 0.0, 1.0);
	spec = dynamicFresnelTerm;
	<endif perPixelFresnel>
	
	baseColor = texture(readbackShadedBufferTemp, gl_FragCoord.xy / screenSize);
	
	spec *= 1-underwater;
	
	spec = pow(spec, gamma);
	
	//if(mod((gl_FragCoord.x + gl_FragCoord.y), 2) == 0)
	//	discard;
	
	if(baseColor.a < 1.0)
		discard;
	
	outDiffuseColor = vec4(baseColor);
	outNormalColor = vec4(encodeNormal(normal).xy, spec, 1.0);
	outMaterialColor = vec4(lightMapCoords.xyz, 1);
}
