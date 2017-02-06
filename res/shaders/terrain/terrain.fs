#version 150 core
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

// Disabled int textures for Intel IGP compatibility, instead it's floats in GL_NEAREST interpolation, works everywhere including nvidia if you add a few tricks
//#extension GL_EXT_gpu_shader4 : require

//Passed variables
in vec3 vertexPassed;
in vec3 normalPassed;
in vec2 lightMapCoords;
in vec2 textureCoord;
in vec3 eyeDirection;
in float fresnelTerm;
in float fogIntensity;

//Framebuffer outputs
out vec4 shadedFramebufferOut;

//Textures
uniform sampler2D normalTexture; // Water surface
uniform sampler2D heightMap; // Heightmap
uniform sampler2D groundTexture; // Block ids
uniform sampler1D blocksTexturesSummary; // Atlas ids -> diffuse rgb
uniform sampler2D vegetationColorTexture; //Vegetation

//Reflections
uniform samplerCube environmentCubemap;

//Block lightning
uniform sampler2D lightColors;
uniform sampler2D blockLightmap;
uniform vec3 shadowColor;
uniform vec3 sunColor;
uniform float shadowStrength;
uniform float shadowVisiblity;

//World general information
uniform float mapSize;
uniform float time;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec3 camPos;

//Sky data
uniform sampler2D sunSetRiseTexture;
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform vec3 sunPos;
uniform float overcastFactor;

//Gamma constants
<include ../lib/gamma.glsl>

//World mesh culling
uniform sampler2D loadedChunksMapTop;
uniform sampler2D loadedChunksMapBot;
uniform float ignoreWorldCulling;

<include ../sky/sky.glsl>

void main()
{
	//Computes the zone covered by actual chunks
	float heightCoveredStart = texture(loadedChunksMapBot,  ( ( floor( ( vertexPassed.xz - floor(camPos.xz/32.0)*32.0) / 32.0) )/ 32.0) * 0.5 + 0.5 ).r * 1024.0 - 1.0;
	float heightCoveredEnd = texture(loadedChunksMapTop,  ( ( floor( ( vertexPassed.xz - floor(camPos.xz/32.0)*32.0) / 32.0) )/ 32.0) * 0.5 + 0.5 ).r * 1024.0 + 33.0;
	
	//Discards the fragment if it is within
	if(vertexPassed.y-1.5 > heightCoveredStart && vertexPassed.y-0.0-32.0 < heightCoveredEnd && ignoreWorldCulling < 1.0)
		discard;

	//int voxelDataActual = voxelData;
	float voxelId = texture(groundTexture, textureCoord).r;
	
	//512-voxel types summary... not best
	vec4 diffuseColor = texture(blocksTexturesSummary, voxelId/512.0);
	
	//Apply plants color if alpha is < 1.0
	if(diffuseColor.a < 1.0)
		diffuseColor.rgb *= texture(vegetationColorTexture, vertexPassed.xz / vec2(mapSize)).rgb;
	
	//Apply gamma then
	diffuseColor.rgb = pow(diffuseColor.rgb, vec3(gamma));
	
	float specularity = 0.0;
	vec3 normal = normalPassed;
	
	//Water case
	if(voxelId > 511.0)
	{
		diffuseColor.rgb = pow(vec3(51 / 255.0, 105 / 255.0, 110 / 255.0), vec3(gamma));
	
		//Build water texture
		vec3 nt = 1.0*(texture(normalTexture,(vertexPassed.xz/5.0+vec2(0.0,time)/50.0)/15.0).rgb*2.0-1.0);
		nt += 1.0*(texture(normalTexture,(vertexPassed.xz/2.0+vec2(-time,-2.0*time)/150.0)/2.0).rgb*2.0-1.0);
		nt += 0.5*(texture(normalTexture,(vertexPassed.zx*0.8+vec2(400.0, sin(-time/5.0)+time/25.0)/350.0)/10.0).rgb*2.0-1.0);
		nt += 0.25*(texture(normalTexture,(vertexPassed.zx*0.1+vec2(400.0, sin(-time/5.0)-time/25.0)/250.0)/15.0).rgb*2.0-1.0);
		
		nt = normalize(nt);
		
		//Merge it a bit with the usual direction
		float i = 0.5;
		normal.x += nt.r*i;
		normal.z += nt.g*i;
		normal.y += nt.b*i;
		
		normal = normalize(normal);
		
		//Set wet
		specularity = pow(fresnelTerm, gamma);
	}
	
	//Computes blocky light
	vec3 baseLight = textureGammaIn(blockLightmap, vec2(0.0, 1.0)).rgb;
	baseLight *= textureGammaIn(lightColors, vec2(time, 1.0)).rgb;
	
	//Compute side illumination by sun
	float NdotL = clamp(dot(normal, normalize(sunPos)), 0.0, 1.0);
	float sunlightAmount = NdotL * shadowVisiblity;
	vec3 finalLight = mix(pow(sunColor, vec3(gamma)), baseLight * pow(shadowColor, vec3(gamma)), (1.0 - sunlightAmount) * shadowStrength);
	
	// Simple lightning for lower end machines
	<ifdef !shadows>
		float faceDarkening = 0.0;
		vec3 shadingDir = normal;
		
		//Some face darken more than others
		faceDarkening += 0.25 * abs(dot(vec3(1.0, 0.0, 0.0), shadingDir));
		faceDarkening += 0.45 * abs(dot(vec3(0.0, 0.0, 1.0), shadingDir));
		faceDarkening += 0.6 * clamp(dot(vec3(0.0, -1.0, 0.0), shadingDir), 0.0, 1.0);
		
		finalLight = mix(baseLight, vec3(0.0), faceDarkening);
	<endif !shadows>

	//Merges diffuse and lightning
	vec3 finalColor = diffuseColor.rgb * finalLight;
	
	//Do basic reflections
	vec3 reflectionVector = normalize(reflect(vec3(eyeDirection.x, eyeDirection.y, eyeDirection.z), normal));
	if(specularity > 0.0)
	{	
		//Basic sky colour
		vec3 reflected = getSkyColor(time, normalize(reflect(eyeDirection, normal)));
		
		//Sample cubemap if enabled
		<ifdef doDynamicCubemaps>
		reflected = texture(environmentCubemap, vec3(reflectionVector.x, -reflectionVector.y, -reflectionVector.z)).rgb;
		<endif doDynamicCubemaps>
		
		//Add sunlight reflection
		float sunSpecularReflection = specularity * 100.0 * pow(clamp(dot(normalize(reflect(normalMatrix * eyeDirection,normalMatrix * normal)),normalize(normalMatrix * sunPos)), 0.0, 1.0),750.0);
		finalColor += vec3(sunSpecularReflection);
		
		//Mix them to obtain final colour
		finalColor = mix(finalColor, reflected , specularity);
	}
	
	//Get per-fragment fog color
	vec3 fogColor = getSkyColorWOSun(time, normalize(eyeDirection));
	
	//Mix in fog
	shadedFramebufferOut = mix(vec4(finalColor, 1.0),vec4(fogColor,1.0), fogIntensity);
}
