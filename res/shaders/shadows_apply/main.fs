//#version 120

uniform sampler2D lightTexture;
uniform sampler2D lightBuffer;
uniform sampler2D comp_depth;
uniform sampler2D comp_spec;
uniform sampler2D comp_light;
uniform sampler2D comp_normal;

varying vec2 screenCoord;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float shadowVisiblity; // Used for night transitions, hides shadows
uniform sampler2DShadow shadowMap;
uniform mat4 shadowMatrix;

uniform int lightsToRender;
uniform vec3 camPos;

uniform float powFactor;

const float distScale = 0.5;

uniform float pass;
uniform vec3 sunPos;

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);
const float shadowStrength = 0.75;

varying float shadowMapBiasMultiplier;

uniform float brightnessMultiplier;

vec4 accuratizeShadow(vec4 shadowMap)
{
	shadowMap.xy /= ( (1.0f - distScale) + sqrt(shadowMap.x * shadowMap.x + shadowMap.y * shadowMap.y) * distScale );
	
	shadowMap.w = length(shadowMap.xy);
	//Transformation for screen-space
	shadowMap.xyz = shadowMap.xyz * 0.5 + 0.5;
	
	
	return shadowMap;
}

float linearizeDepth(float depth)
{
    float near = 0.1;//Camera.NearPlane;
    float far = 3000.0;//Camera.FarPlane;
    float linearDepth = (2.0 * near) / (far + near - depth * (far - near));

    return linearDepth;
}

vec4 convertScreenSpaceToWorldSpace(vec2 co) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2.0-1.0, texture2D(comp_depth, co, 0.0).x * 2.0 - 1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition;
}

vec4 computeLight(vec3 normal, vec4 worldSpacePosition)
{
	float NdotL = clamp(dot(normalize(normal), normalize(normalMatrix * sunPos )), -1.0, 1.0);

	float opacity = 0.0;
	//Shadows sampling
	vec4 coordinatesInShadowmap = accuratizeShadow(shadowMatrix * modelViewMatrixInv * worldSpacePosition);

	
	float ragix = 0.8f;
	<ifdef shadows>
	float edgeSmoother = 0.0;
	ragix = 1;
	if(!(coordinatesInShadowmap.x <= 0 || coordinatesInShadowmap.x >= 1 || coordinatesInShadowmap.y <= 0 || coordinatesInShadowmap.y >= 1  || coordinatesInShadowmap.z >= 1 || coordinatesInShadowmap.z <= -1))
	{
		float bias = clamp(0.00025*tan(acos(NdotL)) * clamp(shadowMapBiasMultiplier, 1.0, 2.0) ,0,0.005 )*(1+coordinatesInShadowmap.w);
		edgeSmoother = 1-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5)-0.25)*4+max(0,abs(coordinatesInShadowmap.y-0.5)-0.25)*4,3),0,1);
		opacity += edgeSmoother * (1-shadow2D(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0).r);
	}
	
	//No extreme shadowing
	//opacity = clamp(opacity, 0.52, 1.0);
	<endif shadows>
	vec3 finalLight = vec3(1.0);
	
	
	float clamped = clamp(NdotL, 0.0, 0.1);
	if(NdotL < 0.1)
	{
		opacity += 1-(10*clamped);
	}
	
	opacity = clamp(opacity, 0.0, 1.0);
	//finalLight*=opacity;
	
	float sunLightMultiplier = texture2D(comp_spec, screenCoord).b;
	
	finalLight = mix(vec3(0), finalLight-shadowColor, (1-opacity) * shadowVisiblity * sunLightMultiplier);
	
	//finalLight -= shadowColor * shadowVisiblity * (opacity);
	
	/*if(finalLight.x > 1)
		finalLight.x = 1.0 + log(finalLight.x)/1.0;
	if(finalLight.y > 1)
		finalLight.y = 1.0 + log(finalLight.y)/1.0;
	if(finalLight.z > 1)
		finalLight.z = 1.0 + log(finalLight.z)/1.0;*/
	
	return vec4(finalLight, 1.0);
}

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToWorldSpace(screenCoord);
	
	//vec4 pixelColor = texture2D(comp_light, screenCoord);
	vec4 pixelNormal = texture2D(comp_normal, screenCoord)*2.0 - 1.0;
	
	vec4 shadingColor = vec4(0.0);
	
	//if(length(texture2D(comp_normal, screenCoord).xyz) > 0.0)
	{
		shadingColor = vec4(0);
		//for(int i = 0; i < KERNEL_SIZE; i++)
			shadingColor += computeLight(pixelNormal.xyz, cameraSpacePosition);
	}
	
	shadingColor.rgb *= brightnessMultiplier;
	
	gl_FragColor = shadingColor;
}
