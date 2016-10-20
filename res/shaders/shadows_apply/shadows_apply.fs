#version 150 core
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D depthBuffer;
uniform sampler2D metaBuffer;
uniform sampler2D albedoBuffer;
uniform sampler2D normalBuffer;

//Reflections stuff
uniform samplerCube environmentCubemap;

//Passed variables
in vec2 screenCoord;

//Sky data
uniform sampler2D sunSetRiseTexture;
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform vec3 sunPos;
uniform float overcastFactor;

uniform sampler2D lightColors;
uniform sampler2D blockLightmap;
uniform sampler2D ssaoBuffer;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;
uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;
uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;
uniform mat4 untranslatedMV;
uniform mat4 untranslatedMVInv;
uniform vec3 camPos;

//Shadow mapping
uniform float shadowVisiblity; // Used for night transitions, hides shadows
uniform sampler2DShadow shadowMap;
uniform mat4 shadowMatrix;

uniform float time;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

//Gamma constants
<include ../lib/gamma.glsl>

uniform vec3 shadowColor;
uniform vec3 sunColor;
uniform float shadowStrength;

out vec4 fragColor;

<include ../sky/sky.glsl>
<include ../lib/transformations.glsl>
<include ../lib/shadowTricks.glsl>
<include ../lib/normalmapping.glsl>

vec4 computeLight(vec4 inputColor, vec3 normal, vec4 worldSpacePosition, vec4 meta, float specular)
{
	inputColor.rgb = pow(inputColor.rgb, vec3(gamma));

	float NdotL = clamp(dot(normalize(normal), normalize(normalMatrix * sunPos )), -1.0, 1.0);

	float opacity = 0.0;
	//Shadows sampling
	vec4 coordinatesInShadowmap = accuratizeShadow(shadowMatrix * (untranslatedMVInv * worldSpacePosition));

	//Declaration here
	vec3 finalLight = vec3(1.0, 1.0, 0.0);
	
	//Block light input, modified linearly according to time of day
	vec3 baseLight = textureGammaIn(blockLightmap, vec2(0.0, meta.y)).rgb;
	baseLight *= textureGammaIn(lightColors, vec2(time, 1.0)).rgb;
	
	<ifdef shadows>
	float clamped = 10 * clamp(NdotL, 0.0, 0.1);
	
	//How much in shadows's brightness the object is
	float shadowIllumination = 0.0;
	
	//How much in shadow influence's zone the object is
	float edgeSmoother = 0.0;
	
	//How much does the pixel is lit by directional light
	float directionalLightning = clamp((NdotL) + (1 - meta.a), 0.0, 1.0);
	
	if(!(coordinatesInShadowmap.x <= 0.0 || coordinatesInShadowmap.x >= 1.0 || coordinatesInShadowmap.y <= 0.0 || coordinatesInShadowmap.y >= 1.0  || coordinatesInShadowmap.z >= 1.0 || coordinatesInShadowmap.z <= -1.0))
	{
		//Bias to avoid shadow acne
		float bias = (1.0 - meta.a) * 0.0010 + clamp(0.0035*pow(tan(acos(NdotL)), 2.0) - 0.01075, 0.0005,0.0025 ) * clamp(16.0 * pow(length(coordinatesInShadowmap.xy - vec2(0.5)), 2.0), 1.0, 16.0); // * (0.0 + 1.0 * clamp(2.0 * coordinatesInShadowmap.w - 1.0, 1.0, 100.0));
		//Are we inside the shadowmap zone edge ?
		edgeSmoother = 1.0-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5) - 0.45)*20.0+max(0,abs(coordinatesInShadowmap.y-0.5) - 0.45)*20.0, 1.0), 0.0, 1.0);
		//
		shadowIllumination += clamp((texture(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0.0) * 1.5 - 0.25), 0.0, 1.0);
	}
	
	float sunlightAmount = ( directionalLightning * ( mix( shadowIllumination, meta.y, 1-edgeSmoother) ) ) * shadowVisiblity;
	
	finalLight = mix(pow(sunColor, vec3(gamma)), baseLight * pow(shadowColor, vec3(gamma)), (1.0 - sunlightAmount) * shadowStrength);
	
	<endif shadows>
	<ifdef !shadows>
		// Simple lightning for lower end machines
		float opacityModified = 0.0;
		vec3 shadingDir = normalize(normalMatrixInv * normal);
		opacityModified += 0.25 * abs(dot(vec3(1.0, 0.0, 0.0), shadingDir));
		opacityModified += 0.45 * abs(dot(vec3(0.0, 0.0, 1.0), shadingDir));
		opacityModified += 0.6 * clamp(dot(vec3(0.0, -1.0, 0.0), shadingDir), 0.0, 1.0);
		
		opacity = mix(opacity, opacityModified, meta.a);
		finalLight = mix(baseLight, vec3(0.0), opacity);
		//finalLight = pow(finalLight, vec3(gamma));
	<endif !shadows>
	
	finalLight += textureGammaIn(blockLightmap, vec2(meta.x, 0.0)).rgb;
	float ssao = 1.0-meta.z;
	<ifdef ssao>
		//If SSAO is disabled, we use the crappy free vertex AO ( byproduct of block/sunlight merging in code )
		ssao *= texture(ssaoBuffer, screenCoord).x;
	<endif ssao>
	
	//SSAO * 0.5 + 0.5
	//(1-Z) * 0.5 + 0.5
	//0.5 - Z * 0.5 + 0.5
	//1.0 - Z * 0.5
	
	finalLight *= clamp(ssao * 0.5 + 0.5, 0.0, 1.0);
	inputColor.rgb *= finalLight;
	
	return inputColor;
}
<include ../lib/ssr.glsl>

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, depthBuffer);
	
	vec4 normalBufferData = texture(normalBuffer, screenCoord);
	vec4 pixelMeta = texture(metaBuffer, screenCoord);
	
	vec3 pixelNormal = decodeNormal(normalBufferData);
	
	vec4 shadingColor = texture(albedoBuffer, screenCoord);
	//shadingColor.rgb = normalize(shadingColor.rgb);
	
	if(shadingColor.a > 0.0)
	{
		float spec = pow(normalBufferData.z, 1.0);
		shadingColor = computeLight(shadingColor, pixelNormal, cameraSpacePosition, pixelMeta, spec);
		if(spec > 0.0)
			shadingColor.rgb = mix(shadingColor.rgb, computeReflectedPixel(screenCoord, cameraSpacePosition.xyz, pixelNormal, pixelMeta.y).rgb, spec);
		//shadingColor = vec4(1.0, 0.0, 0.0, 0.0);
	}
	else
		discard;
	
	// Apply fog
	vec3 sum = (cameraSpacePosition.xyz);
	float dist = length(sum)-fogStartDistance;
	float fogFactor = (dist) / (fogEndDistance-fogStartDistance);
	float fogIntensity = clamp(fogFactor, 0.0, 1.0);
	
	vec3 fogColor = getSkyColorWOSun(time, normalize(((modelViewMatrixInv * cameraSpacePosition).xyz - camPos).xyz));
	
	fragColor = mix(shadingColor, vec4(fogColor,shadingColor.a), fogIntensity);
}
