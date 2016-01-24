//Entry attributes
attribute vec4 vertexIn;

varying vec4 interpolatedColor;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float time;

uniform sampler2D lightmap;

uniform float sunIntensity;

const float gamma = 2.2;
const float gammaInv = 1/2.2;

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

void main(){
	interpolatedColor = vec4(0.5, 0.5, 0.6, 0.5 + 0.5 * sunIntensity) * texture2DGammaIn(lightmap, vec2(1 * sunIntensity, 0));
	
	float maxHeight = vertexIn.w;
	vec3 vertexPosition = vertexIn.xyz;
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