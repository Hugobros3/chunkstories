#version 150

//Entry attributes
in vec4 vertexIn;

out vec4 interpolatedColor;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float time;
uniform float sunTime;

uniform sampler2D lightmap;

uniform float sunIntensity;

const float gamma = 2.2;
const float gammaInv = 1/2.2;

vec4 textureGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

void main(){
	interpolatedColor = vec4(0.3, 0.3, 0.4, 0.0) * textureGammaIn(lightmap, vec2(sunTime, 1.0));
	
	float maxHeight = vertexIn.w;
	vec3 vertexPosition = vertexIn.xyz;
	
	interpolatedColor.a = 1.0 - clamp((maxHeight - vertexPosition.y - 20.0) / 1.0, 0.0, 1.0);
	//Falling at 20m/s
	vertexPosition.y -= time * 20;
	if(vertexPosition.y < maxHeight)
	{
		vertexPosition = vec3(0.0);
		interpolatedColor = vec4(1.0, 0.0, 0.0, 1.0);
	}
	
	vec4 projected = projectionMatrix * modelViewMatrix * vec4(vertexPosition, 1.0);
	gl_PointSize = 200.0f;
	gl_Position = projected;
}
