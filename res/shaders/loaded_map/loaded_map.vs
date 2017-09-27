#version 330
//Entry attributes
in vec3 vertexIn;

out float height;

void main(){
	//XY -> XZ position relative to camera's chunk
	//Z : Absolute Y coordinate in world-chunk-space
	height = vertexIn.z;
	gl_Position = vec4(((vertexIn.xy + vec2(gl_VertexID * 0.0, 0.0)) / vec2(64.0)) * 2, 0.0, 1.0);
}