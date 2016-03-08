#version 130
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

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

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
    vec3 map = texture2D(normalMap, texcoord ).xyz;
    map = map*2.0 - 1.0;
	mat3 TBN = cotangent_frame(N, -V, texcoord);
    return normalize(TBN * map);
}

varying float back;

void main(){
	//Basic texture color
	vec3 baseColor = texture2D(diffuseTexture, texcoord.st).rgb;
	
	//light coloring
	vec3 finalLight = texture2D(lightColors,lightMapCoords).rgb;
	
	//Texture transparency
	float alpha = texture2D(diffuseTexture, texcoord.st).a;
	
	//Rain makes shit glint
	float specular = 0.0;
	//specular+=1;
	//Shadow
	
	vec4 source = texture2D(diffuseGBuffer, gl_FragCoord.xy);
	
	if(alpha < 1)
		discard;
	
	//Diffuse G-Buffer
	gl_FragData[0] = vec4(baseColor,alpha);
	//Normal G-Buffer
	gl_FragData[1] = vec4(0.5,0.5,1,1);
	//Light color G-buffer
	gl_FragData[2] = vec4(finalLight,alpha);
	//Specular G-Buffer
	gl_FragData[3] = vec4(specular, specular, specular, 1);
	//Modelview buffer (discard)
	//gl_FragData[4] = vec4(modelview.rgb,modelview.a);
}
