#version 120

uniform sampler2D comp_diffuse;
uniform sampler2D comp_normal;
uniform sampler2D comp_depth;
uniform sampler2D comp_light;
uniform sampler2D comp_specular;
//uniform sampler2D comp_worldpos;
uniform sampler2D comp_background;

//For sky reflections

uniform sampler2D glowSampler;
uniform sampler2D colorSampler;
uniform vec3 sunPos;
uniform float time;

varying vec3 eyeDirection;

<include ../sky/sky.glsl>

//Shadows
uniform float shadowVisiblity; // Used for night transitions, hides shadows
uniform sampler2DShadow shadowMap;
uniform sampler2DShadow shadowMap2;

uniform mat4 shadowMatrix;
uniform mat4 shadowMatrix2;

uniform samplerCube skybox;

uniform sampler2D blocklights; // Sampler to lightmap

varying vec2 f_texcoord;

varying vec2 scaledPixel;

uniform vec3 skyColor;

uniform float pass;

uniform float viewWidth;
uniform float viewHeight;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform int isUnderwater;

const float distScale = 0.5;

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);
const float shadowStrength = 0.75;

vec4 accuratizeShadow(vec4 shadowMap)
{
	shadowMap.xy /= ( (1.0f - distScale) + sqrt(shadowMap.x * shadowMap.x + shadowMap.y * shadowMap.y) * distScale );
	
	shadowMap.w = length(shadowMap.xy);
	//Transformation for screen-space
	shadowMap.xyz = shadowMap.xyz * 0.5 + 0.5;
	
	
	return shadowMap;
}

vec3 convertCameraSpaceToScreenSpace(vec3 cameraSpace) {
    vec4 clipSpace = projectionMatrix * vec4(cameraSpace, 1.0);
    vec3 NDCSpace = clipSpace.xyz / clipSpace.w;
    vec3 screenSpace = 0.5 * NDCSpace + 0.5;
		 screenSpace.z = 0.1f;
    return screenSpace;
}

vec4 convertScreenSpaceToWorldSpace(vec2 co) {
    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2.0 - 1.0 , texture2D(comp_depth, co, 0).x * 2.0 - 1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition;
}

const float far = 3000.0;

vec4 getProcessedPixel(vec2 screenSpaceCoords);
vec4 getProcessedPixelWithReflections(vec2 screenSpaceCoords);

// Screen-space reflections code

vec4 computeReflectedPixel(vec2 screenSpaceCoords, vec3 cameraSpacePosition, vec3 pixelNormal, float showSkybox)
{
    vec2 screenSpacePosition2D = screenSpaceCoords;
	
    vec3 cameraSpaceViewDir = normalize(cameraSpacePosition);
    vec3 cameraSpaceVector = normalize(reflect(cameraSpaceViewDir, pixelNormal));
	vec3 oldPosition = cameraSpacePosition;
    vec3 cameraSpaceVectorPosition = oldPosition + cameraSpaceVector;
    vec3 currentPosition = convertCameraSpaceToScreenSpace(cameraSpaceVectorPosition);
    vec4 color = texture2D(comp_diffuse, screenSpacePosition2D); // vec4(pow(texture2D(gcolor, screenSpacePosition2D).rgb, vec3(3.0f + 1.2f)), 0.0);
    const int maxRefinements = 3;
	int numRefinements = 0;
    int count = 0;
	vec2 finalSamplePos = screenSpacePosition2D;
	
	int numSteps = 0;
	
	bool outOfViewport = true;
	
	<ifdef doRealtimeReflections>
    for (int i = 0; i < 40; i++)
    {
        if(currentPosition.x < 0 || currentPosition.x > 1 ||
           currentPosition.y < 0 || currentPosition.y > 1 ||
           currentPosition.z <= 0.0 || currentPosition.z > 1.0 ||
           -cameraSpaceVectorPosition.z > 512 ||
           -cameraSpaceVectorPosition.z < 0.0f)
			
        { 
			outOfViewport = true;
			break; 
		}

        vec2 samplePos = currentPosition.xy;
        float sampleDepth = convertScreenSpaceToWorldSpace(samplePos).z;

        float currentDepth = cameraSpaceVectorPosition.z;
        float diff = sampleDepth - currentDepth;
        float error = length(cameraSpaceVector / pow(2.0f, numRefinements));

        //If a collision was detected, refine raymarch
        if(diff >= 0 && diff <= error * 2.00f && numRefinements <= maxRefinements) 
        {
			/*if( -cameraSpaceVectorPosition.z > 100)
				break;*/
			outOfViewport = false;
        	//Step back
        	cameraSpaceVectorPosition -= cameraSpaceVector / pow(2.0f, numRefinements);
        	++numRefinements;
		//If refinements run out
		} 
		else if (diff >= 0 && diff <= error * 4.0f && numRefinements > maxRefinements)
		{
			outOfViewport = false;
			finalSamplePos = samplePos;
			break;
		}
		else if(diff > 0)
		{
			
		}
		
        cameraSpaceVectorPosition += cameraSpaceVector / pow(2.0f, numRefinements);

        if (numSteps > 1)
			cameraSpaceVector *= 1.275f;	//Each step gets bigger

		currentPosition = convertCameraSpaceToScreenSpace(cameraSpaceVectorPosition);
        count++;
        numSteps++;
    }
	<endif doRealtimeReflections>
	
	color = getProcessedPixel(finalSamplePos);
	
	if(numSteps > 38)
		color.a = 0.0f;
	if(texture2D(comp_depth, screenSpaceCoords, 0).x == 1)
		color.a = 0.0f;
	if(outOfViewport)
		color.a = 0.0f;
	
	if(color.a == 0)
	{
		vec3 normSkyDirection = normalMatrixInv * cameraSpaceVector;
		
		color = vec4(getSkyColor(time, normSkyDirection), 1.0);
		
		//color.rgb = getSkyColor(time, normSkyDirection);
		
		float specular = clamp(pow(dot(normalize(normSkyDirection),normalize(sunPos)),16.0),0.0,1.0);
		//color.rgb += vec3(1,1,1)*clamp(pow((dot(normalize(normSkyDirection), normalize(sunPos))+1.055)/4.0,16.0),0.0,1.0);
		//color.rgb += vec3(1,1,1)*specular;
		
		
		color.rgb *= showSkybox;//texture2D(blocklights, lightMapUV).rgb;
	}
    return color;
}

vec4 getFog(vec4 color, vec3 position, float fogDistance, vec3 lightColor)
{
	float dist = clamp(length(position)-fogDistance,0,10000);
	const float LOG2 = 1.442695;
	float density = 0.0025;
	float fogFactor = exp2( -density * 
					   density * 
					   dist * 
					   dist * 
					   LOG2 );
	float fogI = clamp(fogFactor, 0.0, 1.0);
	return mix(vec4(gl_Fog.color.rgb,1), color, fogI);
}


vec4 getProcessedPixel(vec2 screenSpaceCoords)
{
	//vec4 lightMapCoordinates = texture2D(comp_light, screenSpaceCoords).rgba;
    vec4 cameraSpacePosition = convertScreenSpaceToWorldSpace(screenSpaceCoords);
	
	vec4 pixelColor = texture2D(comp_diffuse, screenSpaceCoords);
	vec4 pixelNormal = texture2D(comp_normal, screenSpaceCoords)*2.0 - 1.0;
	
	vec4 light = clamp(texture2D(comp_light, screenSpaceCoords).rgba, 0.0, 1.0);
	
	//light = computeLight(lightMapCoordinates, pixelNormal.xyz, cameraSpacePosition);
	//texture2D(comp_light, screenSpaceCoords).rgba;
	
	pixelColor.rgb*=light.rgb;// + pixelColor.rgb*(1-clamp(light.a, 0.0, 1.0));
	
	//pixelColor = mix(pixelColor,texture2D(comp_background, screenSpaceCoords),1-pixelColor.a);
	pixelColor.a = 1;
	
	float spec = texture2D(comp_specular, screenSpaceCoords).r;
	if(spec > 0)
	{
		vec3 cameraSpaceViewDir = normalize(cameraSpacePosition.xyz);
		vec3 cameraSpaceVector = normalize(reflect(cameraSpaceViewDir, pixelNormal.xyz));
	
		vec3 normSkyDirection = normalMatrixInv * cameraSpaceVector;
		//reflection = textureCube(skybox, normSkyDirection);
		vec3 reflection = getSkyColor(time, normSkyDirection);
		pixelColor.rgb = mix(pixelColor.rgb,reflection,spec);
	}
	
	
	return pixelColor;
}

vec4 getProcessedPixelWithReflections(vec2 screenSpaceCoords)
{
	//vec4 lightMapCoordinates = texture2D(comp_light, screenSpaceCoords).rgba;
    vec4 cameraSpacePosition = convertScreenSpaceToWorldSpace(screenSpaceCoords);

	vec4 pixelColor = texture2D(comp_diffuse, screenSpaceCoords);
	vec4 pixelNormal = texture2D(comp_normal, screenSpaceCoords)*2.0 - 1.0;
	
	vec4 light = clamp(texture2D(comp_light, screenSpaceCoords).rgba, 0.0, 1.0);
	
	if(pass == 1)
		light.a = 1;
	
	//light = computeLight(light, pixelNormal.xyz, cameraSpacePosition);
	//computeLight(lightMapCoordinates, pixelNormal.xyz, cameraSpacePosition);
	//texture2D(comp_light, screenSpaceCoords).rgba;
	
	pixelColor.rgb = pixelColor.rgb*light.rgb + pixelColor.rgb*(1-clamp(light.a, 0.0, 1.0));
	//pixelColor.a = 1;
	
	//float replace = pixelColor.a;
	//pixelColor = mix(pixelColor,texture2D(comp_background, screenSpaceCoords), 1-replace);
	
	
	vec3 specData = texture2D(comp_specular, screenSpaceCoords).rgb;
	float spec = specData.r;
	
	//spec = clamp(spec, 1, 1);
	
	//Do reflections, dynamic or cubemap
	vec4 reflection = vec4(0,0,0,0);
	if(pass < 2 && spec > 0 )
	{
		reflection = computeReflectedPixel(screenSpaceCoords, cameraSpacePosition.xyz, pixelNormal.xyz, specData.b);
	}
	if(pass == 2 && spec > 0 )
	{
		vec3 cameraSpaceViewDir = normalize(cameraSpacePosition.xyz);
		vec3 cameraSpaceVector = normalize(reflect(cameraSpaceViewDir, pixelNormal.xyz));
	
		vec3 normSkyDirection = normalMatrixInv * cameraSpaceVector;
		//reflection = textureCube(skybox, normSkyDirection);
		reflection = vec4(getSkyColor(time, normSkyDirection), 1.0);
		
		//color.rgb *= skyColor;
		
		//float specular = clamp(pow(dot(normalize(normSkyDirection),normalize(sunPos)),16.0),0.0,10.0);
		//color.rgb += vec3(1,1,1)*clamp(pow((dot(normalize(normSkyDirection), normalize(sunPos))+1.055)/4.0,16.0),0.0,1.0);
		//reflection.rgb += vec3(1,1,1)*specular;
		reflection.rgb *= specData.b;
	}
	
	pixelColor.rgb = mix(pixelColor.rgb,reflection.rgb,spec);
	
	/*if(replace == 0)
		discard;*/
	
	//Apply fog
	if(texture2D(comp_depth, screenSpaceCoords).x < 1)
		pixelColor = getFog(pixelColor, cameraSpacePosition.xyz, 64, light.rgb);
		
	//Draw sky
	//if(pixelColor.a == 0)
	//	pixelColor = vec4(0,0,1,1);
	return pixelColor;
}

void main() {	
	vec4 baseColor = vec4(0);
	gl_FragColor = getProcessedPixelWithReflections(f_texcoord);
}
