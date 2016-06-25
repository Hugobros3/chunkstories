#version 120
attribute vec2 vertexIn;
varying vec2 f_texcoord;
varying vec4 texcoord;

varying vec2 scaledPixel;

uniform float viewWidth;
uniform float viewHeight;
 
void main(void) {

  gl_Position = vec4(-1.0+vertexIn*2.0, 0.0, 1.0);
  
  f_texcoord = (vertexIn);
  
  texcoord = vec4(f_texcoord,0,0);
  
  scaledPixel = vec2(1.0/viewWidth,1.0/viewHeight);
}