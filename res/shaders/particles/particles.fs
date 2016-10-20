#version 150
// Copyright 2015 XolioWare Interactive

//General data
varying vec4 texcoord; // Coordinate
varying vec3 eye; // eye-position

//Diffuse colors
uniform sampler2D diffuseTexture; // diffuse texture
uniform vec3 blockColor;

uniform sampler2D normalMap; // Blocks normal texture atlas

//Block and sun Lightning
varying vec2 lightMapCoords; //Computed in vertex shader
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform sampler2D lightColors; // Sampler to lightmap

//Normal mapping
varying vec3 varyingNormal;
varying vec4 varyingVertex;

uniform sampler2D diffuseGBuffer;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

vec4 textureGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

<include ../lib/normalmapping.glsl>

varying float back;

void main(){
	//Basic texture color
	vec3 baseColor = texture(diffuseTexture, texcoord.st).rgb;
	
	//light coloring
	vec3 finalLight = texture(lightColors,lightMapCoords).rgb;
	
	//Texture transparency
	float alpha = texture(diffuseTexture, texcoord.st).a;
	
	//Rain makes shit glint
	float specular = 0.0;
	//specular+=1;
	//Shadow
	
	vec4 source = texture(diffuseGBuffer, gl_FragCoord.xy);
	
	if(alpha < 1.0)
		discard;
	
	//Diffuse G-Buffer
	gl_FragData[0] = vec4(baseColor,alpha);
	//Normal G-Buffer + reflections
	gl_FragData[1] = vec4(encodeNormal(vec3(0.0, 0.0, 1.0)).xy, 0.0, 0.0);
	//Light color G-buffer
	gl_FragData[2] = vec4(lightMapCoords, 0.0,1.0);
}
