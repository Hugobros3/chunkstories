//#version 120

uniform sampler2D depthBuffer;
uniform sampler2D metaBuffer;
uniform sampler2D albedoBuffer;
uniform sampler2D normalBuffer;

uniform sampler2D glowSampler;
uniform sampler2D colorSampler;
uniform float isRaining;

uniform sampler2D blockLightmap;

uniform sampler2D ssaoBuffer;

varying vec2 screenCoord;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float shadowVisiblity; // Used for night transitions, hides shadows

uniform sampler2DShadow shadowMap;
uniform mat4 shadowMatrix;

uniform float time;
uniform vec3 camPos;

uniform float powFactor;

const float distScale = 0.5;

uniform float pass;
uniform float sunIntensity;
uniform vec3 sunPos;

const float gamma = 2.2;
const float gammaInv = 1/2.2;

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

uniform vec3 shadowColor;
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

vec4 computeLight(vec4 inputColor, vec3 normal, vec4 worldSpacePosition, vec3 lightmapCoordinates, float specular)
{
	inputColor.rgb = pow(inputColor.rgb, vec3(gamma));

	float NdotL = clamp(dot(normalize(normal), normalize(normalMatrix * sunPos )), -1.0, 1.0);

	float opacity = 0.0;
	//Shadows sampling
	vec4 coordinatesInShadowmap = accuratizeShadow(shadowMatrix * modelViewMatrixInv * worldSpacePosition);

	float ragix = 0.8f;
	<ifdef shadows>
	float edgeSmoother = 0.0;
	ragix = 1;
	if(!(coordinatesInShadowmap.x <= 0 || coordinatesInShadowmap.x >= 1 || coordinatesInShadowmap.y <= 0 || coordinatesInShadowmap.y >= 1  || coordinatesInShadowmap.z >= 1 || coordinatesInShadowmap.z <= -1))
	{
		float bias = clamp(0.00035*tan(acos(NdotL)) * clamp(shadowMapBiasMultiplier, 1.0, 2.0) ,0.000,0.00125 )*(1+2*coordinatesInShadowmap.w);
		edgeSmoother = 1-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5)-0.25)*4+max(0,abs(coordinatesInShadowmap.y-0.5)-0.25)*4,3),0,1);
		opacity += edgeSmoother * (1-shadow2D(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0).r);
	}
	
	float clamped = clamp(NdotL, 0.0, 0.1);
	if(NdotL < 0.1)
	{
		opacity += 1-(10*clamped);
	}
	
	//opacity += 1-NdotL;
	
	<endif shadows>
	//vec4 light = texture2D(comp_light, screenCoord);
	float sunLightMultiplier = lightmapCoordinates.y;
	
	<ifdef !shadows>
	opacity = 0.0;
	vec3 shadingDir = normalize(normalMatrixInv * normal);
	opacity += 0.25 * abs(dot(vec3(1,0,0), shadingDir));
	opacity += 0.45 * abs(dot(vec3(0,0,1), shadingDir));
	opacity += 0.6 * clamp(dot(vec3(0,-1,0), shadingDir), 0.0, 1.0);
	<endif !shadows>
	opacity = clamp(opacity, 0.0, 1.0);
	
	float sunSpec = specular * pow(clamp(dot(normalize(reflect(worldSpacePosition.xyz, normal)),normalize(normalMatrix * sunPos)), 0.0, 1.0),1750.0);
	
	vec3 baseLight = texture2DGammaIn(blockLightmap, vec2(0, lightmapCoordinates.y * sunIntensity)).rgb;
	vec3 finalLight = mix(pow(shadowColor, vec3(gamma)) * baseLight, baseLight, (1 - opacity * shadowStrength) * shadowVisiblity);
	<ifdef !shadows>
	finalLight = pow(finalLight, vec3(gamma));
	<endif !shadows>
	
	finalLight += texture2DGammaIn(blockLightmap, vec2(lightmapCoordinates.x, 0)).rgb;
	float ssao = 1-lightmapCoordinates.z;
	<ifdef ssao>
		//If SSAO is disabled, we use the crappy free vertex AO ( byproduct of block/sunlight merging in code )
		ssao *= texture2D(ssaoBuffer, screenCoord).x;
	<endif ssao>
	
	finalLight *= clamp(ssao, 0.0, 1.0);
	inputColor.rgb *= finalLight;
	
	return inputColor;
}

<include ssr.glsl>

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToWorldSpace(screenCoord);
	
	vec4 pixelNormal = texture2D(normalBuffer, screenCoord);
	vec3 pixelMeta = texture2D(metaBuffer, screenCoord).xyz;
	
	pixelNormal.rgb = pixelNormal.rgb * 2.0 - vec3(1.0);
	
	vec4 shadingColor = texture2D(albedoBuffer, screenCoord);
	
	if(shadingColor.a > 0.0)
	{
		float spec = pow(pixelNormal.w, 1.0);
		shadingColor = computeLight(shadingColor, pixelNormal.xyz, cameraSpacePosition, pixelMeta, spec);
		if(spec > 0)
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
	fogColor.rgb = pow(fogColor.rgb, vec3(gamma));
	
	//gl_FragColor = shadingColor;
	gl_FragColor = mix(shadingColor, vec4(fogColor,shadingColor.a), fogI);
}
