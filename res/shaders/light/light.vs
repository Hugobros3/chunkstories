#version 120
varying vec4 f_texcoord;
varying vec2 screenCoord;
 
attribute vec2 vertexIn;
void main(void) {
  gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
  screenCoord = vertexIn.xy*0.5+0.5;
}