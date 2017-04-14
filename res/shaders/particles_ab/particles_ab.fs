#version 150
// Copyright 2015 XolioWare Interactive

//General data
varying vec4 texcoord; // Coordinate
varying vec3 eye; // eye-position

//Diffuse colors
uniform sampler2D diffuseTexture; // diffuse texture
uniform sampler2D normalTexture; // diffuse texture
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

out vec4 shadedFramebufferOut;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

void main(){
	vec4 source = texture(diffuseTexture, texcoord.xy);
	
	source.rgb *= source.a;
	
	//if(alpha < 1.0)
	//	discard;
	
	//Diffuse G-Buffer
	shadedFramebufferOut = source;
}
