#version 120
// Copyright 2015 XolioWare Interactive

//General data
varying vec2 texcoord; // Coordinate
varying vec3 eye; // eye-position
varying float chunkFade;
varying vec3 varyingNormal;
varying vec4 varyingVertex;
varying vec4 colorPassed;
varying float fresnelTerm;
varying float rainWetness;
varying vec4 vertexColor; // Vertex color : red is for blocklight, green is sunlight
varying vec3 lightMapCoords; //Computed in vertex shader
varying vec4 modelview;

uniform float useColorIn;
uniform float useNormalIn;

//Diffuse colors
uniform sampler2D diffuseTexture; // Blocks diffuse texture atlas
uniform vec3 blockColor;

uniform sampler2D normalTexture; // Blocks normal texture atlas

//Chunk fading into view
uniform float chunkTransparency;

//Debug
uniform vec3 blindFactor; // can white-out all the colors

//Block and sun Lightning
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform sampler2D lightColors; // Sampler to lightmap
uniform vec3 sunPos; // Sun position

//Normal mapping

//Shadow shit
uniform float shadowVisiblity; // Used for night transitions, hides shadows

//Weather
uniform float wetness;

//Matrices

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec3 vegetationColor;

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);
const float shadowStrength = 0.75;


<include ../lib/normalmapping.glsl>

void main(){
	
	vec3 normal = varyingNormal;
	
	if(useNormalIn < 1.0)
		normal = vec3(0.0, 1.0, 0.0);
		
	vec3 normalMapped = texture2D(normalTexture, texcoord).xyz;
    normalMapped = normalMapped * 2.0 - 1.0;
	//normalMapped.x = -normalMapped.x;
	
	normal = perturb_normal(normal, eye, texcoord, normalMapped);
	normal = normalize(normalMatrix * normal);
		
	//Basic texture color
	vec3 baseColor = texture2D(diffuseTexture, texcoord).rgb;
	
	if(useColorIn > 0.0)
		baseColor = colorPassed.rgb;
	
	//Texture transparency
	float alpha = texture2D(diffuseTexture, texcoord).a;
	
	//if(useColorIn > 0.0)
	//	alpha *= colorPassed.a;
	
	if(alpha < 0.5)
		discard;
	else if(alpha < 1)
		baseColor *= vegetationColor;
	
	//Rain makes shit glint
	float spec = rainWetness * fresnelTerm;
	<ifdef perPixelFresnel>
	float dynamicFresnelTerm = 0.0 + 1.0 * clamp(0.7 + dot(normalize(eye), vec3(varyingNormal)), 0.0, 1.0);
	spec = rainWetness * dynamicFresnelTerm;
	<endif perPixelFresnel>
	
	//vec3 finalLight = texture2D(lightColors,lightMapCoords.xy).rgb;
	
	vec3 blockLight = texture2D(lightColors,vec2(lightMapCoords.x, 0)).rgb;
	vec3 sunLight = texture2D(lightColors,vec2(0, lightMapCoords.y)).rgb;
	
	sunLight = mix(sunLight, sunLight * shadowColor, shadowVisiblity * 0.75);
	
	vec3 finalLight = blockLight * (1-sunLight);
	finalLight += sunLight;
	
	//ao term
	//finalLight *= vec3(1,1,1)*clamp(1-lightMapCoords.z, 0.0, 1.0);
	
	//finalLight+=1.0;
	
	vec3 finalColor = baseColor*blockColor;
	
	//Diffuse G-Buffer
	gl_FragData[0] = vec4(finalColor,chunkFade+1);
	//Normal G-Buffer
	gl_FragData[1] = vec4(encodeNormal(normal).xyz, spec);
	//Metadata color G-buffer
	
	gl_FragData[2] = vec4(lightMapCoords, 1.0f);
}