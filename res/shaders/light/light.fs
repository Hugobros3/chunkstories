#version 130

uniform sampler2D albedoBuffer;
uniform sampler2D depthBuffer;
uniform sampler2D comp_spec;
uniform sampler2D comp_normal;

varying vec2 screenCoord;

uniform mat4 modelViewMatrixInv;
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrixInv;
uniform mat4 projectionMatrix;
uniform mat3 normalMatrixInv;
uniform mat3 normalMatrix;

//Point lights
uniform float lightDecay[64];
uniform vec3 lightPos[64];
uniform vec3 lightColor[64];
//Cone lights
uniform vec3 lightDir[64];
uniform float lightAngle[64];

uniform int lightsToRender;
uniform vec3 camPos;

uniform float powFactor;

//Gamma constants
<include ../lib/gamma.glsl>
<include ../lib/transformations.glsl>

void main() {
	vec4 totalLight = vec4(0.0);
	//Get normal from g-buffer
	vec3 normal = texture2D(comp_normal, screenCoord).rgb*2.0-1.0;
	vec3 normalWorld = normalize(normalMatrixInv * normal);
	float spec = texture2D(comp_normal, screenCoord).a;
	
	vec3 pixelPositionView = convertScreenSpaceToCameraSpace(screenCoord, depthBuffer).xyz;
	
	if(length(pixelPositionView) > 500)
		discard;
		
	vec3 pixelPositionWorld = ( modelViewMatrixInv * vec4(pixelPositionView, 1.0) ).xyz;
	
	for(int i = 0; i < lightsToRender; i++)
	{
		vec3 lightPositionWorld = lightPos[i];
		float distance = length(pixelPositionWorld-lightPositionWorld);
		//Distance influence
		//if(distance > lightDecay[i])
		//	continue;
		
		vec4 lightAmount = vec4(pow(lightColor[i], vec3(gamma)), 1.0);
		lightAmount*=pow(clamp(1.0-distance/(lightDecay[i]), 0.0, 10.0), gamma);
		
		vec3 lightRay = normalize((vec4(lightPositionWorld-pixelPositionWorld, 1.0)).xyz);
		//Normal influence
		
		float dotL = clamp(dot(normalWorld, lightRay), 0.0, 1.0);
		lightAmount.rgb *= dotL;
		
		
		//Optional : cone light, view direction influence
		if(spec > 0)
		{
			lightAmount.rgb += pow(lightColor[i], vec3(gamma))  * 2 * clamp(pow(clamp(dot(reflect(lightRay, normalWorld), normalize(pixelPositionWorld-camPos)), 0.0, 10.0), 1000), 0.0, 10.0);
		}
		if(lightAngle[i] > 0)
		{
			float dotCone = dot(lightRay, lightDir[i]);
			float cosAngle = cos(lightAngle[i]);
			
			//if(dotCone < cosAngle)
			//	lightAmount = vec4(0.0);//mix(lightAmount, vec4(0.0, 0.0, 0.0, 0.0),  (cosAngle-dotCone)+0.5);
			lightAmount.rgb *= clamp(60.0*(dotCone-cosAngle), 0.0, 1.0);
		}
		
		//lightAmount = vec4(vec3(pow(1.0, 2.2)), 1.0);
		
		totalLight += max(lightAmount, 0.0);
	}
	
	//totalLight *= totalLight.a;
	//totalLight.a = 0;
	
	gl_FragColor = totalLight * texture2DGammaIn(albedoBuffer, screenCoord);
}
