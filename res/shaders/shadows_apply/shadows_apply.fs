//#version 120

uniform sampler2D depthBuffer;
uniform sampler2D metaBuffer;
uniform sampler2D albedoBuffer;
uniform sampler2D normalBuffer;

uniform sampler2D glowSampler;
uniform sampler2D colorSampler;

uniform sampler2D lightColors;

uniform samplerCube environmentCubemap;

uniform float isRaining;

uniform sampler2D blockLightmap;

uniform sampler2D ssaoBuffer;

varying vec2 screenCoord;

uniform vec2 screenViewportSize;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform mat4 untranslatedMVP;
uniform mat4 untranslatedMVPInv;

uniform float shadowVisiblity; // Used for night transitions, hides shadows

uniform sampler2DShadow shadowMap;
uniform mat4 shadowMatrix;

uniform float time;
uniform vec3 camPos;

uniform float powFactor;

const float distScale = 0.9;

uniform float pass;
uniform float sunIntensity;
uniform vec3 sunPos;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

uniform vec3 shadowColor;
uniform vec3 sunColor;
uniform float shadowStrength;

varying float shadowMapBiasMultiplier;

uniform float brightnessMultiplier;

<include ../sky/sky.glsl>

vec4 accuratizeShadow(vec4 shadowMap)
{
	shadowMap.xy /= ( (1.0f - distScale) + sqrt(shadowMap.x * shadowMap.x + shadowMap.y * shadowMap.y) * distScale );
	shadowMap.w = length(shadowMap.xy);
	//Transformation for screen-space
	shadowMap.xyz = shadowMap.xyz * 0.5 + 0.5;
	return shadowMap;
}

float linearizeDepth(float depth)
{
    float near = 0.1;//Camera.NearPlane;
    float far = 3000.0;//Camera.FarPlane;
    float linearDepth = (2.0 * near) / (far + near - depth * (far - near));
    return linearDepth;
}

vec4 convertScreenSpaceToWorldSpace(vec2 co) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2.0-1.0, texture2D(depthBuffer, co, 0.0).x * 2.0 - 1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition;
}

vec4 computeLight(vec4 inputColor, vec3 normal, vec4 worldSpacePosition, vec4 meta, float specular)
{
	inputColor.rgb = pow(inputColor.rgb, vec3(gamma));

	float NdotL = clamp(dot(normalize(normal), normalize(normalMatrix * sunPos )), -1.0, 1.0);

	float opacity = 0.0;
	//Shadows sampling
	vec4 coordinatesInShadowmap = accuratizeShadow(shadowMatrix * (untranslatedMVPInv * worldSpacePosition));

	//Declaration here
	vec3 finalLight = vec3(1.0, 1.0, 0.0);
	
	//Block light input, modified linearly according to time of day
	vec3 baseLight = texture2DGammaIn(blockLightmap, vec2(0.0, meta.y)).rgb;
	baseLight *= texture2DGammaIn(lightColors, vec2(time, 1.0)).rgb;
	
	<ifdef shadows>
	float clamped = clamp(NdotL, 0.0, 0.1);
	
	//How much in shadows's brightness the object is
	float shadowIllumination = 0.0;
	
	//How much in shadow influence's zone the object is
	float edgeSmoother = 0.0;
	
	//How much does the pixel is lit by directional light
	float directionalLightning = clamp((10.0*clamped) + (1 - meta.a), 0.0, 1.0);
	
	//opacity = mix(opacity, clamp(1.0-(10.0*clamped), 0.0, 1.0), meta.a);
	if(!(coordinatesInShadowmap.x <= 0.0 || coordinatesInShadowmap.x >= 1.0 || coordinatesInShadowmap.y <= 0.0 || coordinatesInShadowmap.y >= 1.0  || coordinatesInShadowmap.z >= 1.0 || coordinatesInShadowmap.z <= -1.0))
	{
		//Bias to avoid shadow acne
		float bias = (1.0 - meta.a) * 0.0010 + clamp(0.0035*tan(acos(NdotL)) - 0.01075, 0.0005,0.0025 ) * (1.0 + 1.0 * clamp(2.0 * coordinatesInShadowmap.w - 1.0, 0.0, 100.0));
		//Are we inside the shadowmap zone edge ?
		edgeSmoother = 1-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5)-0.25)*4.0+max(0,abs(coordinatesInShadowmap.y-0.5)-0.25)*4.0, 3.0), 0.0, 1.0);
		//
		shadowIllumination += clamp((shadow2D(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0.0).r * 1.5 - 0.25), 0.0, 1.0);
	}
	
	//float sunSpec = specular * pow(clamp(dot(normalize(reflect(worldSpacePosition.xyz, normal)),normalize(normalMatrix * sunPos)), 0.0, 1.0),750.0);
	
	float sunlightAmount = ( directionalLightning * ( mix( shadowIllumination, meta.y, 1-edgeSmoother) ) ) * shadowStrength * shadowVisiblity;
	
	finalLight = mix(baseLight * pow(shadowColor, vec3(gamma)), pow(sunColor, vec3(gamma)), sunlightAmount);
	
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
	
	finalLight += texture2DGammaIn(blockLightmap, vec2(meta.x, 0.0)).rgb;
	float ssao = 1.0-meta.z;
	<ifdef ssao>
		//If SSAO is disabled, we use the crappy free vertex AO ( byproduct of block/sunlight merging in code )
		ssao *= texture2D(ssaoBuffer, screenCoord).x;
	<endif ssao>
	
	//SSAO * 0.5 + 0.5
	//(1-Z) * 0.5 + 0.5
	//0.5 - Z * 0.5 + 0.5
	//1.0 - Z * 0.5
	
	finalLight *= clamp(ssao * 0.5 + 0.5, 0.0, 1.0);
	inputColor.rgb *= finalLight;
	
	return inputColor;
}

<include ssr.glsl>

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToWorldSpace(screenCoord);
	
	vec4 pixelNormal = texture2D(normalBuffer, screenCoord);
	vec4 pixelMeta = texture2D(metaBuffer, screenCoord);
	
	pixelNormal.rgb = pixelNormal.rgb * 2.0 - vec3(1.0);
	
	vec4 shadingColor = texture2D(albedoBuffer, screenCoord);
	//shadingColor.rgb = normalize(shadingColor.rgb);
	
	if(shadingColor.a > 0.0)
	{
		float spec = pow(pixelNormal.w, 1.0);
		shadingColor = computeLight(shadingColor, pixelNormal.xyz, cameraSpacePosition, pixelMeta, spec);
		if(spec > 0.0)
			shadingColor.rgb = mix(shadingColor.rgb, computeReflectedPixel(screenCoord, cameraSpacePosition.xyz, pixelNormal.xyz, pixelMeta.y).rgb, spec);
		//shadingColor = vec4(1.0, 0.0, 0.0, 0.0);
	}
	else
		discard;
	
	//shadingColor.rgb *= brightnessMultiplier;
	
	//shadingColor.a *= 0.5;
	
	// Apply fog
	//vec3 sum = (modelViewMatrixInv * projectionMatrixInv * vec4(vec3(screenCoord*2.0-1.0, texture2D(depthBuffer, screenCoord, 0.0).x * 2.0 - 1.0), 1.0)).xyz;
	vec3 sum = (cameraSpacePosition.xyz);
	float dist = length(sum)-gl_Fog.start;
	float fogFactor = (dist) / (gl_Fog.end-gl_Fog.start);
	float fogI = clamp(fogFactor, 0.0, 0.9);
	
	vec3 fogColor = gl_Fog.color.rgb;
	fogColor = getSkyColorWOSun(time, normalize(((modelViewMatrixInv * cameraSpacePosition).xyz + camPos).xyz));
	//fogColor.rgb = pow(fogColor.rgb, vec3(gamma));
	
	//gl_FragColor = shadingColor;
	gl_FragColor = mix(shadingColor, vec4(fogColor,shadingColor.a), fogI);
}
