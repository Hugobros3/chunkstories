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

// I suck at maths, so I used this code in the meanwhile I get how it works
// http://www.thetenthplanet.de/archives/1180
mat3 cotangent_frame(vec3 N, vec3 p, vec2 uv)
{
    // récupère les vecteurs du triangle composant le pixel
    vec3 dp1 = dFdx( p );
    vec3 dp2 = dFdy( p );
    vec2 duv1 = dFdx( uv );
    vec2 duv2 = dFdy( uv );

    // résout le système linéaire
    vec3 dp2perp = cross( dp2, N );
    vec3 dp1perp = cross( N, dp1 );
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;

    // construit une trame invariante à l'échelle 
    float invmax = inversesqrt( max( dot(T,T), dot(B,B) ) );
    return mat3( T * invmax, B * invmax, N );
}

vec3 perturb_normal( vec3 N, vec3 V, vec2 texcoord )
{
    // N, la normale interpolée et
    // V, le vecteur vue (vertex dirigé vers l'œil)
    vec3 map = texture2D(normalTexture, texcoord ).xyz;
    map = map*2.0 - 1.0;
	mat3 TBN = cotangent_frame(N, -V, texcoord);
    return normalize(TBN * map);
}

void main(){
	
	vec3 normal = varyingNormal;
	
	if(useNormalIn < 1.0)
		normal = vec3(0.0, 1.0, 0.0);
		
	normal = perturb_normal(normal, eye, texcoord);
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
	gl_FragData[1] = vec4(normal*0.5+0.5, spec);
	//Metadata color G-buffer
	
	gl_FragData[2] = vec4(lightMapCoords, 1.0f);
}