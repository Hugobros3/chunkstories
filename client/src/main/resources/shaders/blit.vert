#version 450

in vec2 vertexIn;
out vec2 texCoord;

//in int gl_VertexID; 

out vec3 color;

void main()
{
	color = vec3(gl_VertexIndex / 4.0, gl_VertexIndex / 8.0, gl_VertexIndex / 16.0);

	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	texCoord = vertexIn.xy*0.5+0.5;
}