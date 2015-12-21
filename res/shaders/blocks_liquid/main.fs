// Copyright 2015 XolioWare Interactive

uniform sampler2D diffuseTexture; // Blocks texture atlas
varying vec2 texcoord; // Coordinate
varying vec3 eye; // eye-position

//Chunk loading
uniform float chunkTransparency;

//Debug
uniform vec3 blindFactor; // can white-out all the colors

//Block and sun Lightning
varying vec4 vertexColor; // Vertex color : red is for blocklight, green is sunlight
varying vec4 lightMapCoords; //Computed in vertex shader
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform sampler2D lightColors; // Sampler to lightmap
uniform vec3 sunPos; // Sun position
varying float NdotL; // Face luminosity

//Normal mapping
varying vec3 varyingNormal;
varying vec4 varyingVertex;
uniform sampler2D normalTexture;

//Shadow shit
uniform sampler2D shadowMap;
uniform sampler2D shadowMap2;
varying vec4 coordinatesInShadowmap;
varying vec4 coordinatesInShadowmap2;
varying vec3 normalV;
varying vec3 lightV;
uniform float shadowVisiblity; // Used for night transitions ( w/o shadows as you know )

//Water
uniform float time;
// Screen space reflections
varying vec2 fragPos;
uniform vec2 screenSize;

varying float fresnelTerm;

uniform samplerCube skybox;

varying float chunkFade;

//Fog
varying float fogI;

varying vec4 modelview;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

varying float waterFogI;

float linearizeDepth(float expDepth)
{
	return (2 * 0.1) / (3000 + 0.1 - expDepth * (3000 - 0.1));
}

void main(){
	//discard;

	float opacity = 1.0;
	
	float edgeSmoother = 0.0;
	
	vec3 normal = vec3(0.0, 1.0, 0.0);//varyingNormal;
	
	vec3 nt = 1.0*(texture2D(normalTexture,(varyingVertex.xz/5.0+vec2(0.0,time)/50.0)/15.0).rgb*2.0-1.0);
	
	nt += 1.0*(texture2D(normalTexture,(varyingVertex.xz/2.0+vec2(-time,-2.0*time)/150.0)/2.0).rgb*2.0-1.0);
	nt += 0.5*(texture2D(normalTexture,(varyingVertex.zx*0.8+vec2(400.0, sin(-time/5.0)+time/25.0)/350.0)/10.0).rgb*2.0-1.0);
	nt += 0.5*(texture2D(normalTexture,(varyingVertex.zx*0.2+vec2(400.0, sin(-time/5.0)-time/25.0)/250.0)/15.0).rgb*2.0-1.0);
	
	nt = normalize(nt);
	
	//nt = (gl_ModelViewMatrix * vec4(normalize(nt),0)).xyz;
	
	//normal = nt;
	
	float i = 0.5*0.125;
	
	normal.x += nt.r*i;
	normal.z += nt.g*i;
	normal.y += nt.b*i;
	
	normal = normalize(normal);
	
	
	normal = normalize(normalMatrix * normal);

	//Basic texture color
	vec2 coords = (gl_FragCoord.xy)/screenSize;
	coords+=vec2(floor(sin(coords.x*100.0+time/5.0))/screenSize.x,floor(cos(coords.y*100.0+time/5.0))/screenSize.y);
	
	vec3 baseColor = texture2D(diffuseTexture, texcoord).rgb;
	
	//vec3 reflection = texture(skybox, reflect(eye, normalViewSpace)).rgb;
	//vec3 refraction = texture2D(comp_fp_diffuse,coords).rgb;
	//refraction = mix(refraction, vec3(0.4, 0.4, 1.0), clamp(-30*(linearizeDepth(gl_FragCoord.z)-linearizeDepth(texture2D(comp_fp_depth, (gl_FragCoord.xy)/screenSize).r)), 0.0, 1.1));

	//baseColor = vec3(texcoord, 0);
	
	gl_FragData[0] = vec4(baseColor, chunkTransparency*chunkFade*(0.5+0.5*waterFogI*0));
	
	float spec = 0.5;
	
	gl_FragData[1] = vec4(normalize(normal)*0.5+0.5, 1.0);
	
	gl_FragData[2] = vec4(texture2D(lightColors,lightMapCoords.xy).rgb*opacity, chunkTransparency*chunkFade*(0.5+0.5*waterFogI*0)*0.5); /*+vec3(1.0,1.0,1.0)*clamp(specular,0.0,10.0)*/
	//gl_FragData[2] = vec4(lightMapCoords,0,1);
	
	gl_FragData[3] = vec4(spec, lightMapCoords.xw, 1.0);
	
	gl_FragData[4] = projectionMatrix * modelview;
}