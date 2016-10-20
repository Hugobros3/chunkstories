#version 150 core
in vec2 vertexIn;
in vec2 texCoordIn;
in vec4 colorIn;

//Simply transfer data
out vec2 texCoordPassed;
out vec4 colorPassed;

void main(void) {

  texCoordPassed = vec2(texCoordIn);
  colorPassed = colorIn;
  gl_Position = vec4(vertexIn, 0.0, 1.0);
  
}