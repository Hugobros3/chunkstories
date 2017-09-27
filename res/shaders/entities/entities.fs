#version 330
/** Very general-purpose shader whoose job is to render (user-defined) entities, both gbuffer and shadow pass */

//General data
in vec2 texcoord; // Coordinate
in vec3 eye; // eye-position
in float chunkFade;
in vec3 inNormal;
in vec4 inVertex;
in vec4 colorPassed;
in float fresnelTerm;
in float rainWetness;
in vec4 vertexColor; // Vertex color : red is for blocklight, green is sunlight
in vec2 worldLight; //Computed in vertex shader
in vec4 modelview;

uniform float useColorIn;
uniform float useNormalIn;

//Diffuse colors
uniform sampler2D diffuseTexture; // Blocks diffuse texture atlas
uniform sampler2D normalTexture; // Blocks normal texture atlas
uniform sampler2D materialTexture; // Blocks normal texture atlas
uniform vec3 blockColor;

//Chunk fading into view
uniform float chunkTransparency;

//Debug
uniform vec3 blindFactor; // can white-out all the colors

//Normal mapping

//Shadow shit
uniform float shadowVisiblity; // Used for night transitions, hides shadows

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

out vec4 outDiffuseColor;
out vec4 outNormalColor;
out vec4 outMaterialColor;

<include ../lib/normalmapping.glsl>

void main(){
	
	vec3 normal = inNormal;
	
	if(useNormalIn < 1.0)
		normal = vec3(0.0, 1.0, 0.0);
		
	vec3 normalMapped = texture(normalTexture, texcoord).xyz;
    normalMapped = normalMapped * 2.0 - 1.0;
	
	normal = perturb_normal(normal, eye, texcoord, normalMapped);
	normal = normalize(normalMatrix * normal);
		
	//Basic texture color
	vec3 baseColor = texture(diffuseTexture, texcoord).rgb;
	
	if(useColorIn > 0.0)
		baseColor = colorPassed.rgb;
	
	//Texture transparency
	float alpha = texture(diffuseTexture, texcoord).a;
	
	vec4 material = texture(materialTexture, texcoord);
	
	//if(useColorIn > 0.0)
	//	alpha *= colorPassed.a;
	
	if(alpha < 0.5)
		discard;
	else if(alpha < 1)
		baseColor *= vegetationColor;
	
	//Rain makes shit glint
	float spec = rainWetness * fresnelTerm;
	<ifdef perPixelFresnel>
	float dynamicFresnelTerm = 0.0 + 1.0 * clamp(0.7 + dot(normalize(eye), vec3(inNormal)), 0.0, 1.0);
	spec = rainWetness * dynamicFresnelTerm;
	<endif perPixelFresnel>
	
	vec3 finalColor = baseColor*blockColor;
	
	//Diffuse G-Buffer
	outDiffuseColor = vec4(finalColor,chunkFade+1);
	//Normal G-Buffer
	outNormalColor = vec4(encodeNormal(normal).xy, spec, 1.0);
	//Metadata color G-buffer
	outMaterialColor = vec4(worldLight, material.r, material.a);
}