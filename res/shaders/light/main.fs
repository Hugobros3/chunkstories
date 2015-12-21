//#version 120

uniform sampler2D lightTexture;
uniform sampler2D lightBuffer;
uniform sampler2D comp_depth;
uniform sampler2D comp_spec;
uniform sampler2D comp_normal;

varying vec2 screenCoord;

uniform mat4 modelViewMatrixInv;
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrixInv;

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

float linearizeDepth(float depth)
{
    float near = 0.1;//Camera.NearPlane;
    float far = 3000.0;//Camera.FarPlane;
    float linearDepth = (2.0 * near) / (far + near - depth * (far - near));

    return linearDepth;
}

vec3 convertScreenSpaceToWorldSpace(vec2 co) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2.0-1.0, texture2D(comp_depth, co, 0.0).x * 2.0 - 1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition.xyz;
}

void main() {
	if(texture2D(lightTexture, screenCoord).a <= 0)
		discard;

	vec4 totalLight = vec4(0.0);
	//Get normal from g-buffer
	vec3 normal = texture2D(comp_normal, screenCoord).rgb*2.0-1.0;
	float spec = texture2D(comp_spec, screenCoord).x;
	
	vec3 worldspace = convertScreenSpaceToWorldSpace(screenCoord);
	
	if(length(worldspace) > 500)
		discard;
		
	vec3 modelView = ( modelViewMatrixInv * vec4(worldspace, 1.0) ).xyz;
	
	for(int i = 0; i < lightsToRender; i++)
	{
		vec3 lightP = lightPos[i];
		float distance = length(modelView-lightP);
		//Distance influence
		if(distance > lightDecay[i])
			continue;
		
		vec4 lightAmount = vec4(lightColor[i], 1.0);
		lightAmount*=clamp(1.0-distance/(lightDecay[i]), 0.0, 10.0);
		
		vec3 lightRay = normalize((modelViewMatrix * vec4(modelView-(lightP-camPos), 1.0)).xyz);
		//Normal influence
		float dotL = dot(-normal, lightRay);
		/*if(spec > 0)
			spec = clamp(pow(dot(reflect(-lightRay, normal), vec3(0,0,0)) , powFactor), 0.0, 10.0);*/
		lightAmount *= dotL+spec;
		//Optional : cone light, view direction influence
		if(lightAngle[i] > 0)
		{
			//float dotCone = dot(lightRay, normalize((modelViewMatrix * vec4(lightDir[i], 1.0)).xyz));
			float dotCone = dot(lightRay, -normalize((modelViewMatrix * vec4(lightDir[i], 0.0)).xyz));
			float cosAngle = cos(lightAngle[i]);
			if(dotCone < cosAngle)
				lightAmount = vec4(0.0);//mix(lightAmount, vec4(0.0, 0.0, 0.0, 0.0),  (cosAngle-dotCone)+0.5);
			lightAmount *= clamp(60.0*abs(cosAngle-dotCone), 0.0, 1.0);
			
		}
		
		totalLight += max(lightAmount, 0.0);
	}
	
	gl_FragColor = totalLight;
}
