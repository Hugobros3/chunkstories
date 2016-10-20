
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

float linearizeDepth(float depth)
{
    float near = 0.1;//Camera.NearPlane;
    float far = 3000.0;//Camera.FarPlane;
    float linearDepth = (2.0 * near) / (far + near - depth * (far - near));

    return linearDepth;
}

vec4 convertScreenSpaceToCameraSpace(vec3 screenSpaceCoordinates) {

    vec4 fragposition = projectionMatrixInv * vec4(screenSpaceCoordinates * 2.0 - vec3(1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition;
}

vec4 convertScreenSpaceToCameraSpace(vec2 screenSpaceCoordinates, float depth) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(screenSpaceCoordinates * 2.0 - vec2(1.0), depth), 1.0);
    fragposition /= fragposition.w;
    return fragposition;
}

vec4 convertScreenSpaceToCameraSpace(vec2 screenSpaceCoordinates, sampler2D depthBuffer)
{
    vec4 cameraSpacePosition = projectionMatrixInv * vec4(vec3(screenSpaceCoordinates * 2.0 - vec2(1.0), texture(depthBuffer, screenSpaceCoordinates, 0.0).x * 2.0 - 1.0), 1.0);
    cameraSpacePosition /= cameraSpacePosition.w;
    return cameraSpacePosition;
}

vec3 convertCameraSpaceToScreenSpace(vec3 cameraSpace) {
    vec4 clipSpace = projectionMatrix * vec4(cameraSpace, 1.0);
    vec3 NDCSpace = clipSpace.xyz / clipSpace.w;
	
    vec3 screenSpace = 0.5 * NDCSpace + 0.5;
	screenSpace.z = 0.1f;
	
    return screenSpace;
}