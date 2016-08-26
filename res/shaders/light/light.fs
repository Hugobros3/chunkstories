#version 130
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//G-buffer samplers
uniform sampler2D diffuseBuffer;
uniform sampler2D depthBuffer;
uniform sampler2D normalBuffer;

//Pixel texture position
in vec2 screenCoord;

//Common camera matrices & uniforms
uniform mat4 modelViewMatrixInv;
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrixInv;
uniform mat4 projectionMatrix;
uniform mat3 normalMatrixInv;
uniform mat3 normalMatrix;
uniform vec3 camPos;

//Point lights
uniform float lightDecay[64];
uniform vec3 lightPos[64];
uniform vec3 lightColor[64];
//Cone lights
uniform vec3 lightDir[64];
uniform float lightAngle[64];

uniform int lightsToRender;

//Gamma constants
<include ../lib/gamma.glsl>
<include ../lib/transformations.glsl>
<include ../lib/normalmapping.glsl>

void main() {
	//Accumulator buffer
	vec4 totalLight = vec4(0.0);
	
	//Get normal from g-buffer
	vec3 normal = decodeNormal(texture2D(normalBuffer, screenCoord));
	vec3 normalWorld = normalize(normalMatrixInv * normal);
	
	//Get reflectivity of surface
	float spec = texture2D(normalBuffer, screenCoord).z;
	
	vec3 pixelPositionCamera = convertScreenSpaceToCameraSpace(screenCoord, depthBuffer).xyz;
	//Discard if too far from camera
	if(length(pixelPositionCamera) > 500.0)
		discard;
		
	vec3 pixelPositionWorld = ( modelViewMatrixInv * vec4(pixelPositionCamera, 1.0) ).xyz;
	
	//Iterate over every light
	for(int i = 0; i < lightsToRender; i++)
	{
		vec3 lightPositionWorld = lightPos[i];
		float distance = length(pixelPositionWorld-lightPositionWorld);
		
		vec4 lightAmount = vec4(pow(lightColor[i], vec3(gamma)), 1.0);
		lightAmount *= pow(clamp(1.0-distance/(lightDecay[i]), 0.0, 10.0), gamma);
		
		vec3 lightRay = normalize((vec4(lightPositionWorld-pixelPositionWorld, 1.0)).xyz);
		//Normal influence
		
		float dotL = clamp(dot(normalWorld, lightRay), 0.0, 1.0);
		lightAmount.rgb *= dotL;
		
		//Add specular term if light should be reflected by surface
		//Optional : cone light, view direction influence
		if(lightAngle[i] > 0.0)
		{
			float dotCone = dot(lightRay, lightDir[i]);
			float cosAngle = cos(lightAngle[i]);
			
			lightAmount.rgb *= clamp(30.0*(dotCone-cosAngle), 0.0, 1.0);
		}
		if(spec > 0.0)
		{
			//lightAmount.rgb += pow(lightColor[i], vec3(gamma)) * 20 * clamp(pow(clamp(dot(reflect(lightRay, normalWorld), normalize(pixelPositionWorld-camPos)), 0.0, 10.0), 1000), 0.0, 10.0);
		}
		totalLight += max(lightAmount, 0.0);
	}
	gl_FragColor = totalLight * texture2DGammaIn(diffuseBuffer, screenCoord);
}
