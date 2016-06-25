#version 120
attribute vec2 vertexIn;
attribute vec2 texCoordIn;

varying vec2 texCoordPassed;
void main(void) {

  texCoordPassed = vec2(texCoordIn);
  gl_Position = vec4(vertexIn, 0.0, 1.0);
  
}