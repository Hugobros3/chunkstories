#version 330
//Entry attributes
in vec3 vertexIn;

uniform vec4 colorIn;

out vec4 interpolatedColor;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat4 transform;
uniform int doTransform;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main(){
	interpolatedColor = colorIn;
	
	gl_Position = projectionMatrix * (modelViewMatrix * vec4(vertexIn, 1.0) + vec4(0.0, 0.0, 0.01, 0.0));
	
	if(doTransform == 1)
		gl_Position = projectionMatrix * (modelViewMatrix * transform * vec4(vertexIn, 1.0) + vec4(0.0, 0.0, 0.01, 0.0));
}