#version 130
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
	//if(mod((gl_FragCoord.x + gl_FragCoord.y), 2) == 0)
	//	discard;

	vec3 normal = vec3(0.0, 1.0, 0.0);

	vec3 nt = 1.0*(texture2D(normalTexture,(vertexPassed.xz/5.0+vec2(0.0,time)/50.0)/15.0).rgb*2.0-1.0);
	nt += 1.0*(texture2D(normalTexture,(vertexPassed.xz/2.0+vec2(-time,-2.0*time)/150.0)/2.0).rgb*2.0-1.0);
	nt += 0.5*(texture2D(normalTexture,(vertexPassed.zx*0.8+vec2(400.0, sin(-time/5.0)+time/25.0)/350.0)/10.0).rgb*2.0-1.0);
	nt += 0.25*(texture2D(normalTexture,(vertexPassed.zx*0.1+vec2(400.0, sin(-time/5.0)-time/25.0)/250.0)/15.0).rgb*2.0-1.0);
	
	nt = normalize(nt);
	
	float i = 0.25;
	
	normal.x += nt.r*i;
	normal.z += nt.g*i;
	normal.y += nt.b*i;
	
	normal = normalize(normal);
	normal = normalMatrix * normal;

	//Basic texture color
	vec2 coords = (gl_FragCoord.xy)/screenSize;
	
	//coords+=10.0 * vec2(floor(sin(coords.x*100.0+time/5.0))/screenSize.x,floor(cos(coords.y*100.0+time/5.0))/screenSize.y);
	
	vec4 baseColor = texture2D(diffuseTexture, texCoordPassed);
	
	float spec = fresnelTerm;
	vec4 worldspaceFragment = convertScreenSpaceToCameraSpace(coords, readbackDepthBufferTemp);
	
	<ifdef perPixelFresnel>
	float dynamicFresnelTerm = 0.1 + 0.6 * clamp(0.7 + dot(normalize(worldspaceFragment.xyz), normal), 0.0, 1.0);
	spec = dynamicFresnelTerm;
	<endif perPixelFresnel>

	//Pass 1
	vec4 meta = texture2D(readbackMetaBufferTemp, coords);
	
	vec3 blockLight = texture2DGammaIn(lightColors,vec2(meta.x, 0)).rgb;
	vec3 sunLight = texture2DGammaIn(lightColors,vec2(0, meta.y)).rgb;
	
	sunLight = mix(sunLight, sunLight * shadowColor, shadowVisiblity * 0.75);
	
	vec3 finalLight = blockLight;// * (1-sunLight);
	finalLight += sunLight;
	finalLight *= (1-meta.z);

	//coords += 15.0 * (1 - length(worldspaceFragment) / viewDistance) * vec2( normal.xz ) / screenSize;
	vec4 refracted = texture2D(readbackAlbedoBufferTemp, coords);
	
	float waterFogI2 = length(worldspaceFragment) / viewDistance;
	refracted.rgb *= pow(finalLight + vec3(1.0) * (1-refracted.a*lightMapCoords.g), vec3(gammaInv));
	
	baseColor.rgb = mix(refracted.rgb, baseColor.rgb, clamp(waterFogI2*(1.0-underwater), 0.0, 1.0));
	
	spec *= 1-underwater;
	
	gl_FragData[0] = vec4(baseColor);
}
