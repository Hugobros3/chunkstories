//Entry attributes
attribute vec3 vertexIn;

//Lighthing
uniform float sunIntensity;
uniform vec3 sunPos; 

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

varying vec4 calculatedLight;
varying float blend;

void main(){
	blend = dot(normalize(sunPos), vec3(0.0, -1.0, 0.0));
	
	calculatedLight = vec4(5.0) * blend;
	
	gl_PointSize = 150.0;
	gl_Position = projectionMatrix * modelViewMatrix * vec4(vertexIn, 1.0);
}