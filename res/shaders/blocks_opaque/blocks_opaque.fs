#version 130
// Copyright 2015 XolioWare Interactive

//General data
varying vec2 texcoord; // Coordinate
varying vec3 eye; // eye-position

//Diffuse colors
uniform sampler2D diffuseTexture; // Blocks diffuse texture atlas
uniform sampler2D normalTexture; // Blocks normal texture atlas
uniform sampler2D materialTexture; // Blocks material texture atlas
uniform sampler2D vegetationColorTexture; // Blocks material texture atlas
uniform vec3 blockColor;


//Debug
uniform vec3 blindFactor; // can white-out all the colors
varying vec4 modelview;

//Block and sun Lightning
varying vec4 vertexColor; // Vertex color : red is for blocklight, green is sunlight
varying vec3 lightMapCoords; //Computed in vertex shader
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform sampler2D lightColors; // Sampler to lightmap
uniform vec3 sunPos; // Sun position

//Normal mapping
varying vec3 varyingNormal;
varying vec4 varyingVertex;

//Shadow shit
uniform float shadowVisiblity; // Used for night transitions, hides shadows
uniform sampler2D shadowMap;
uniform sampler2D shadowMap2;
varying vec4 coordinatesInShadowmap;
varying vec4 coordinatesInShadowmap2;

//Weather
uniform float wetness;
varying float rainWetness;

//Matrices

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

varying float fresnelTerm;

uniform vec2 screenSize;

//Gamma constants
<include ../lib/gamma.glsl>
<include ../lib/transformations.glsl>

uniform float mapSize;

<include ../lib/normalmapping.glsl>

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);
const float shadowStrength = 0.75;

void main(){
	
	vec3 normal = varyingNormal;
	
	//Check normal was given
	float normalGiven = length(varyingNormal);
	
	//Grabs normal from texture and corrects the format
	vec3 normalMapped = texture2D(normalTexture, texcoord).xyz;
    normalMapped = normalMapped*2.0 - 1.0;
	normalMapped.x = -normalMapped.x;
	
	//Apply it
	normal = perturb_normal(normal, eye, texcoord, normalMapped);
	normal = normalize(normalMatrix * normal);
	
	//If no normal given, face camera
	normal = mix(vec3(0,0,1), normal, normalGiven);
	//Basic texture color
	vec3 baseColor = texture2D(diffuseTexture, texcoord).rgb;
	
	//Texture transparency
	float alpha = texture2D(diffuseTexture, texcoord).a;
	
	//alpha = 1;
	//baseColor = vec3(1, 0.5, 0.5);
	
	if(alpha <= 0.0)
		discard;
	
	else if(alpha < 1)
		baseColor *= texture2D(vegetationColorTexture, varyingVertex.xz / vec2(mapSize)).rgb;
	
	//Gamma correction
	//baseColor.rgb = pow(baseColor.rgb, vec3(gamma));
	
	//Rain makes shit glint
	float spec = 0;
	
	vec4 material = texture2D(materialTexture, texcoord);
	
	spec = material.r*rainWetness + (material.g + rainWetness) * fresnelTerm + material.b;
	<ifdef perPixelFresnel>
	vec3 coords = (gl_FragCoord.xyz);
	coords.xy/=screenSize;
	vec4 worldspaceFragment = convertScreenSpaceToCameraSpace(coords);
	float dynamicFresnelTerm = 0.0 + 1.0 * clamp(0.7 + dot(normalize(eye), vec3(varyingNormal)), 0.0, 1.0);
	spec = material.r*rainWetness + (material.g + rainWetness) * dynamicFresnelTerm + material.b;
	<endif perPixelFresnel>
	
	vec3 finalColor = baseColor;
	
	//Diffuse G-Buffer
	
	gl_FragData[0] = vec4(finalColor,1);
	//Normal G-Buffer
	gl_FragData[1] = vec4(normal*0.5+0.5, spec);
	
	gl_FragData[2] = vec4(lightMapCoords, material.a);
}