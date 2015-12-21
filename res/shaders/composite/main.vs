#version 120
attribute vec2 vertexIn;
varying vec2 f_texcoord;
varying vec4 texcoord;

varying vec2 scaledPixel;

varying vec3 eyeDirection;

uniform float viewWidth;
uniform float viewHeight;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;
 
void main(void) {

  gl_Position = vec4(-1.0+vertexIn*2.0, 0.0, 1.0);
  
  f_texcoord = vertexIn;
  
	vec4 transformedSS = vec4(vertexIn.x, vertexIn.y, -1.0, 1.0);
	
	eyeDirection = (modelViewMatrixInv * projectionMatrixInv * transformedSS ).xyz;
	
  texcoord = vec4(f_texcoord,0,0);
  
  scaledPixel = vec2(1.0/viewWidth,1.0/viewHeight);
}