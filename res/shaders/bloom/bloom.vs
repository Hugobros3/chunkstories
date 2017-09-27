#version 330
in vec2 vertexIn;

out vec2 screenCoord;
 
void main(void) {
  gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
  screenCoord = vertexIn.xy*0.5+0.5;
}