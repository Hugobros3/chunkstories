#version 130
// Copyright 2015 XolioWare Interactive

uniform sampler2D diffuseTexture; // Blocks texture atlas
varying vec2 texcoord; // Coordinate
varying vec3 eye; // eye-position

//Chunk loading
uniform float chunkTransparency;

//Debug
uniform vec3 blindFactor; // can white-out all the colors

//Block and sun Lightning
varying vec4 vertexColor; // Vertex color : red is for blocklight, green is sunlight
varying vec4 lightMapCoords; //Computed in vertex shader
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform sampler2D lightColors; // Sampler to lightmap
uniform vec3 sunPos; // Sun position
varying float NdotL; // Face luminosity

//Normal mapping
varying vec3 varyingNormal;
varying vec4 varyingVertex;
uniform sampler2D normalTextureShallow;
uniform sampler2D normalTextureDeep;

varying vec3 normalV;
varying vec3 lightV;
uniform float shadowVisiblity; // Used for night transitions ( w/o shadows as you know )

//Water
uniform float time;
// Screen space reflections
varying vec2 fragPos;
uniform vec2 screenSize;

varying float fresnelTerm;

uniform samplerCube skybox;

varying float chunkFade;

//Fog

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

varying float waterFogI;

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);
const float shadowStrength = 0.75;

uniform sampler2D readbackShadedBufferTemp;

uniform vec2 shadedBufferDimensions;
uniform float viewDistance;

uniform float underwater;

//Gamma constants
<include ../lib/gamma.glsl>
<include ../lib/transformations.glsl>

<include ../lib/normalmapping.glsl>

vec3 mixedTextures(float blend, vec2 coords)
{
	return mix(texture2D(normalTextureShallow, coords).rgb, texture2D(normalTextureDeep, coords * 0.125).rgb, 0);
}

void main(){
	//if(mod((gl_FragCoord.x + gl_FragCoord.y), 2) == 0)

	vec3 normal = vec3(0.0, 0.0, 1.0);

	//vec3 normalTexture = normalTextureShallow
	
	vec3 nt = 1.0*(mixedTextures(lightMapCoords.a, (varyingVertex.xz/5.0+vec2(0.0,time)/50.0)/15.0).rgb*2.0-1.0);
	nt += 1.0*(mixedTextures(lightMapCoords.a, (varyingVertex.xz/2.0+vec2(-time,-2.0*time)/150.0)/2.0).rgb*2.0-1.0);
	nt += 0.5*(mixedTextures(lightMapCoords.a, (varyingVertex.zx*0.8+vec2(400.0, sin(-time/5.0)+time/25.0)/350.0)/10.0).rgb*2.0-1.0);
	nt += 0.25*(mixedTextures(lightMapCoords.a, (varyingVertex.zx*0.1+vec2(400.0, sin(-time/5.0)-time/25.0)/250.0)/15.0).rgb*2.0-1.0);
	
	nt = normalize(nt);
	
	float i = 1.0;
	
	normal.x += nt.r*i;
	normal.y += nt.g*i;
	normal.z += nt.b*i;
	
	normal = normalize(normal);
	
	normal = perturb_normal(varyingNormal, eye, texcoord, normal);
	normal = normalize(normalMatrix * normal);
	
	//Basic texture color
	vec2 coords = (gl_FragCoord.xy)/screenSize;
	
	//coords+=10.0 * vec2(floor(sin(coords.x*100.0+time/5.0))/screenSize.x,floor(cos(coords.y*100.0+time/5.0))/screenSize.y);
	
	vec4 baseColor = texture2D(diffuseTexture, texcoord);
	
	float spec = fresnelTerm;
	vec4 worldspaceFragment = convertScreenSpaceToCameraSpace(coords, gl_FragCoord.z);
	
	<ifdef perPixelFresnel>
	float dynamicFresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(worldspaceFragment.xyz), normal), 0.0, 1.0);
	spec = dynamicFresnelTerm;
	<endif perPixelFresnel>
	
	
	baseColor = texture2D(readbackShadedBufferTemp, gl_FragCoord.xy / screenSize);
	//baseColor.rgb *= lightMapCoords.a;
	//baseColor.a = 1.0;
	
	spec *= 1-underwater;
	
	spec = pow(spec, gamma);
	
	if(baseColor.a < 1.0)
		discard;
	
	gl_FragData[0] = vec4(baseColor);
	
	gl_FragData[1] = vec4(normalize(normal)*0.5+0.5, spec);

	gl_FragData[2] = vec4(lightMapCoords.xyz, 1);

}
