#version 330
in vec2 vertexIn;

out vec2 texCoord;
out vec2 pauseOverlayCoords;

uniform vec2 screenViewportSize;
 
const vec2 backgroundSize = vec2(1024);
	
void main(void) {
	gl_Position = vec4(vertexIn, 0.0, 1.0);

	texCoord = vertexIn.xy * 0.5 + vec2(0.5);
	pauseOverlayCoords = ( ( vertexIn.xy * 0.5 + vec2(0.5) ) * screenViewportSize ) / backgroundSize;
}