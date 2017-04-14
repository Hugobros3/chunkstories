#version 150
// Copyright 2015 XolioWare Interactive

//General data
varying vec4 proc_texcoord; // Coordinate
varying vec4 texcoord; // Coordinate
varying vec3 eye; // eye-position

//Diffuse colors
uniform sampler2D diffuseTexture; // diffuse texture
uniform sampler2D normalTexture; // diffuse texture
uniform vec3 blockColor;

uniform sampler2D normalMap; // Blocks normal texture atlas

//Block and sun Lightning
varying vec2 lightMapCoords; //Computed in vertex shader
uniform sampler2D colorTempSampler;

//Normal mapping
varying vec4 varyingVertex;

uniform sampler2D diffuseGBuffer;

out vec4 shadedFramebufferOut;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

vec4 textureGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture(sampler, coords), vec4(gamma));
}

void main(){
	vec4 source = textureGammaIn(diffuseTexture, proc_texcoord.xy);
	
	source *= textureGammaIn(colorTempSampler, vec2(texcoord.x / 12000.0, 0.0));
	
	source.rgb *= source.a * clamp((texcoord.x - 11000)/100, 2.0, 50.0);
	
	//if(alpha < 1.0)
	//	discard;
	
	//Diffuse G-Buffer
	shadedFramebufferOut = source;
}
