#version 130
// Disabled int textures for Intel IGP compatibility, instead it's floats in GL_NEAREST interpolation
//#extension GL_EXT_gpu_shader4 : require

varying vec3 vertex;
varying vec4 color;

varying float fogI;

uniform sampler2D normalTexture;
uniform vec3 sunPos; // Sun position
uniform vec3 camPos;
uniform float time;
varying vec3 eye;
uniform samplerCube skybox;
varying float fresnelTerm;

uniform float waterLevel;
uniform sampler2D heightMap;
uniform sampler2D groundTexture;
uniform sampler1D blocksTexturesSummary;
uniform samplerCube environmentCubemap;
uniform sampler2D vegetationColorTexture; // Blocks material texture atlas
varying vec2 textureCoord;

varying float chunkFade;

varying vec2 lightMapCoords;

uniform float sunIntensity;
varying vec3 normalHeightmap;
uniform sampler2D lightColors; // Sampler to lightmap
uniform sampler2D blockLightmap;

varying float lowerFactor;

uniform vec3 shadowColor;
uniform vec3 sunColor;
uniform float shadowStrength;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float shadowVisiblity;

uniform sampler2D glowSampler;
uniform float isRaining;
uniform sampler2D colorSampler;

const float gamma = 2.2;
const float gammaInv = 1/2.2;

uniform float mapSize;

varying vec2 chunkPositionFrag;
uniform sampler2D loadedChunksMap;
uniform vec2 playerCurrentChunk;

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

<include ../sky/sky.glsl>

void main()
{
	float id = texture2D(groundTexture, textureCoord).r;
	vec3 finalColor = vec3(0.0+id*500, 0.0, 0.0);
	
	vec4 bs = texture1D(blocksTexturesSummary, id/512.0);
	finalColor = bs.rgb;
	if(bs.a < 1)
		finalColor *= texture2D(vegetationColorTexture, vertex.xz / vec2(mapSize)).rgb;
	
	finalColor.rgb = pow(finalColor.rgb, vec3(gamma));
	
	float spec = 0.0;
	
	vec3 normal = normalHeightmap;
	<ifdef hqTerrain>
	float baseHeight = texture2D(heightMap,textureCoord).r;
	
	//Normal computation, brace yourselves
	vec3 normalHeightmap2 = vec3(0.0, 1.0, 0.0); //Start with an empty vector
	
	float xtrem = 2;
	
	float normalXplusHeight = texture2D(heightMap,textureCoord+vec2(1.0/256.0, 0.0)).r;
	float alpha = atan((normalXplusHeight-baseHeight)*xtrem);
	vec3 normalXplus = vec3(0.0, cos(alpha), -sin(alpha));
	
	normalHeightmap2 += normalXplus;
	
	float normalZplusHeight = texture2D(heightMap,textureCoord+vec2(0.0, 1.0/256.0)).r;
	alpha = atan((normalZplusHeight-baseHeight)*xtrem);
	vec3 normalZplus = vec3(-sin(alpha), cos(alpha), 0.0);
	
	normalHeightmap2 += normalZplus;
	
	float normalXminusHeight = texture2D(heightMap,textureCoord-vec2(1.0/256.0, 0.0)).r;
	alpha = atan((normalXminusHeight-baseHeight)*xtrem);
	vec3 normalXminus = vec3(0.0, cos(alpha), sin(alpha));
	
	normalHeightmap2 += normalXminus;
	
	float normalZminusHeight = texture2D(heightMap,textureCoord-vec2(0.0, 1.0/256.0)).r;
	alpha = atan((normalZminusHeight-baseHeight)*xtrem);
	vec3 normalZminus = vec3(sin(alpha), cos(alpha), 0.0);
	
	normalHeightmap2 += normalZminus;
	
	//I'm happy to say I came up with the maths by myself :)
	
	//normal = normalize(normalHeightmap2);
	<endif hqTerrain>
	
	float specular = 0.0;
	
	if(id == 128)
	{
		
		vec3 nt = 1.0*(texture2D(normalTexture,(vertex.xz/5.0+vec2(0.0,time)/50.0)/15.0).rgb*2.0-1.0);
		nt += 1.0*(texture2D(normalTexture,(vertex.xz/2.0+vec2(-time,-2.0*time)/150.0)/2.0).rgb*2.0-1.0);
		nt += 0.5*(texture2D(normalTexture,(vertex.zx*0.8+vec2(400.0, sin(-time/5.0)+time/25.0)/350.0)/10.0).rgb*2.0-1.0);
		nt += 0.25*(texture2D(normalTexture,(vertex.zx*0.1+vec2(400.0, sin(-time/5.0)-time/25.0)/250.0)/15.0).rgb*2.0-1.0);
		
		nt = normalize(nt);
		
		float i = 0.5;
		
		normal.x += nt.r*i;
		normal.z += nt.g*i;
		normal.y += nt.b*i;
		
		normal = normalize(normal);
		
		//specular = max(pow(dot(normalize(reflect(normalMatrix * eye,normalMatrix * normal)),normalize(normalMatrix * sunPos)),150.0),0.0);
		
		spec = pow(fresnelTerm, gamma);
		
		//spec = fresnelTerm;
		
		specular = spec * pow(clamp(dot(normalize(reflect(normalMatrix * eye,normalMatrix * normal)),normalize(normalMatrix * sunPos)), 0.0, 1.0),750.0);
	
		//vec3 reflection = texture(skybox, reflect(eye, normal)).rgb;
		
	}
	else if(id == 0)
	{
		finalColor = vec3(1, 1, 0);
	}

	
	vec3 baseLight = texture2DGammaIn(blockLightmap, vec2(0.0, 1.0)).rgb;
	baseLight *= texture2DGammaIn(lightColors, vec2(time, 1.0)).rgb;
	
	vec3 blockLight = texture2DGammaIn(blockLightmap,vec2(lightMapCoords.x, 0)).rgb;
	vec3 sunLight = texture2DGammaIn(blockLightmap,vec2(0, lightMapCoords.y)).rgb;
	
	float opacity = 0.0;
	float NdotL = clamp(dot(normal, normalize(sunPos)), -1.0, 1.0);
	
	//opacity += NdotL;
	
	float clamped = clamp(NdotL, 0.0, 0.1);
	if(NdotL < 0.1)
	{
		opacity += 1-(10*clamped);
	}
	
	opacity = clamp(opacity, 0, 0.52);
	
	sunLight *= mix(pow(shadowColor, vec3(gamma)),  pow(sunColor, vec3(gamma)), (1-opacity) * 1);
	
	vec3 finalLight = blockLight;// * (1-sunLight);
	finalLight += sunLight;
	
	<ifdef !shadows>
		// Simple lightning for lower end machines
		float opacityModified = 0.0;
		vec3 shadingDir = normal;//normalize(normalMatrixInv * normal);
		opacityModified += 0.25 * abs(dot(vec3(1.0, 0.0, 0.0), shadingDir));
		opacityModified += 0.45 * abs(dot(vec3(0.0, 0.0, 1.0), shadingDir));
		opacityModified += 0.6 * clamp(dot(vec3(0.0, -1.0, 0.0), shadingDir), 0.0, 1.0);
		
		//opacity = mix(opacity, opacityModified, meta.a);
		finalLight = mix(baseLight, vec3(0.0), opacityModified);
		//finalLight = pow(finalLight, vec3(gamma));
	<endif !shadows>
	
	//vec3 finalLight = baseLight * mix(pow(shadowColor, vec3(gamma)), pow(sunColor, vec3(gamma)), (1 - opacity * pow(shadowStrength, gammaInv)) * shadowVisiblity);
	//finalLight = mix(finalLight, finalLight*shadowColor, opacity * 1.0);
	//finalColor*=finalLight;
	
	

	finalColor = finalColor * finalLight;
	
	vec3 reflectionVector = normalize(reflect(vec3(eye.x, eye.y, eye.z), normal));
	if(spec >0)
	{	
		vec3 reflected = getSkyColor(time, normalize(reflect(eye, normal)));
		
		<ifdef doDynamicCubemaps>
		reflected = textureCube(environmentCubemap, vec3(reflectionVector.x, -reflectionVector.y, -reflectionVector.z)).rgb;
		<endif doDynamicCubemaps>
		finalColor = mix(finalColor, reflected , spec);
	}
	vec3 fogColor = gl_Fog.color.rgb;
	fogColor = getSkyColorWOSun(time, normalize(eye));
	//fogColor.rgb = pow(fogColor.rgb, vec3(gamma));
	
	finalColor += vec3(100.0) * specular;
	
	//finalColor = vec3(1.0);
	
	vec4 compositeColor = mix(vec4(finalColor, 1.0),vec4(fogColor,1.0), fogI);
	
	//compositeColor.rgb = pow(compositeColor.rgb, vec3(gamma));
	
	float covered = texture2D(loadedChunksMap,  ( ( ( ( vertex.xz - floor(camPos.xz/32.0)*32.0) / 32.0) - vec2(0.0) )/ 32.0) * 0.5 + 0.5 ).r;
	
	if(covered > 0.0)
		discard;
	
	gl_FragColor = compositeColor;
}
