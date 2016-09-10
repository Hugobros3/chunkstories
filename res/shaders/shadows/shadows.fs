#version 130
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

in vec4 texCoordPassed;

uniform sampler2D diffuseTexture;

void main(){
	if(texture2D(diffuseTexture, texCoordPassed.st).a < 0.1)
		discard;
		
	gl_FragDepth = gl_FragCoord.z;
	gl_FragColor = vec4(1.0, gl_FragCoord.z, gl_FragCoord.z, 1.0);
}
