#version 330
//Entry attributes
in vec3 vertexIn;

//Lighthing
uniform float sunIntensity;
uniform vec3 sunPos; 

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 untranslatedMV;
uniform mat4 untranslatedMVInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

out vec4 calculatedLight;
out float blend;

void main(){
	blend = dot(normalize(sunPos), vec3(0.0, -1.0, 0.0));
	
	calculatedLight = vec4(5.0) * blend;
	
	gl_PointSize = 1.0;
	gl_Position = projectionMatrix * untranslatedMV * vec4(vertexIn, 1.0);
}