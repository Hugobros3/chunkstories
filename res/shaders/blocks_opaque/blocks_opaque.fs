#version 120
// Copyright 2015 XolioWare Interactive

//General data
varying vec2 texcoord; // Coordinate
varying vec3 eye; // eye-position

//Diffuse colors
uniform sampler2D diffuseTexture; // Blocks diffuse texture atlas
uniform vec3 blockColor;

uniform sampler2D normalTexture; // Blocks normal texture atlas

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
uniform vec3 vegetationColor;

uniform vec3 givenLightmapCoords;
uniform vec2 screenSize;

const float gamma = 2.2;
const float gammaInv = 1/2.2;



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
	//vec3 map = vec3(nul.x, nul.y, nul.z);
    map = map*2.0 - 1.0;
	mat3 TBN = cotangent_frame(N, -V, texcoord);
    return normalize(TBN * map);
}

vec4 unprojectPixel(vec3 co) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2.0-1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition;
}

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);
const float shadowStrength = 0.75;

void main(){
	
	vec3 normal = varyingNormal;
	
	float normalGiven = length(varyingNormal);
	//texcoord /= 32768.0;
	
	normal = perturb_normal(normal, eye, texcoord);
	normal = normalize(normalMatrix * normal);
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
		baseColor *= vegetationColor;
	
	//Gamma correction
	//baseColor.rgb = pow(baseColor.rgb, vec3(gamma));
	
	//Rain makes shit glint
	float spec = 0;
	
	
	spec = rainWetness * fresnelTerm;
	<ifdef perPixelFresnel>
	vec3 coords = (gl_FragCoord.xyz);
	coords.xy/=screenSize;
	vec4 worldspaceFragment = unprojectPixel(coords);
	float dynamicFresnelTerm = 0.0 + 1.0 * clamp(0.7 + dot(normalize(eye), vec3(varyingNormal)), 0.0, 1.0);
	spec = rainWetness * dynamicFresnelTerm;
	<endif perPixelFresnel>
	
	/*vec3 blockLight = texture2D(lightColors,vec2(lightMapCoords.x, 0)).rgb;
	vec3 sunLight = texture2D(lightColors,vec2(0, lightMapCoords.y)).rgb;
	
	sunLight = mix(sunLight, sunLight * shadowColor, shadowVisiblity * 0.75);
	
	vec3 finalLight = blockLight;// * (1-sunLight);
	finalLight += sunLight;
	*/
	
	
	//ao term
	<ifdef !ssao>
		//If SSAO is disabled, we use the crappy free vertex AO ( byproduct of block/sunlight merging in code )
		//finalLight -= vec3(0.2)*clamp(lightMapCoords.z, 0.0, 1.0);
	<endif ssao>
	
	//spec = 1.0+pow(spec, 1.0);
	
	vec3 finalColor = baseColor;
	
	//Diffuse G-Buffer
	gl_FragData[0] = vec4(finalColor,1);
	//Normal G-Buffer
	gl_FragData[1] = vec4(normal*0.5+0.5, spec);
	
	gl_FragData[2] = vec4(lightMapCoords, 1.0);

	//old crap
	//Light color G-buffer
	//gl_FragData[2] = vec4(finalLight, clamp(spec, 0.0, 1.0));
	
	// Even older crap
	
	//gl_FragData[2] = coordinatesInShadowmap;
	//Specular G-Buffer
	//gl_FragData[3] = vec4(spec, lightMapCoords.xy, 1.0);
}