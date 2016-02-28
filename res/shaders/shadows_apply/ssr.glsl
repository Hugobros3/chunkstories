vec3 convertCameraSpaceToScreenSpace(vec3 cameraSpace) {
    vec4 clipSpace = projectionMatrix * vec4(cameraSpace, 1.0);
    vec3 NDCSpace = clipSpace.xyz / clipSpace.w;
    vec3 screenSpace = 0.5 * NDCSpace + 0.5;
		 screenSpace.z = 0.1f;
    return screenSpace;
}

vec4 computeReflectedPixel(vec2 screenSpaceCoords, vec3 cameraSpacePosition, vec3 pixelNormal, float showSkybox)
{
    vec2 screenSpacePosition2D = screenSpaceCoords;
	
    vec3 cameraSpaceViewDir = normalize(cameraSpacePosition);
    vec3 cameraSpaceVector = normalize(reflect(cameraSpaceViewDir, pixelNormal));
	vec3 oldPosition = cameraSpacePosition;
    vec3 cameraSpaceVectorPosition = oldPosition + cameraSpaceVector;
    vec3 currentPosition = convertCameraSpaceToScreenSpace(cameraSpaceVectorPosition);
    
	// Is the reflection pointing in the right direction ?

	
	vec4 color = vec4(0.0);// texture2D(comp_diffuse, screenSpacePosition2D); // vec4(pow(texture2D(gcolor, screenSpacePosition2D).rgb, vec3(3.0f + 1.2f)), 0.0);
   
	const int maxRefinements = 3;
	int numRefinements = 0;
    	int count = 0;
	vec2 finalSamplePos = screenSpacePosition2D;
	
	int numSteps = 0;
	
	bool outOfViewport = true;
	
	<ifdef doRealtimeReflections>
    for (int i = 0; i < 40; i++)
    {
        if((currentPosition.x < 0.0) || (currentPosition.x > 1.0) ||
           (currentPosition.y < 0.0) || (currentPosition.y > 1.0) ||
           (currentPosition.z <= 0.0) || (currentPosition.z > 1.0) ||
           (-cameraSpaceVectorPosition.z > 512.0) ||
           (-cameraSpaceVectorPosition.z < 0.0f))
			
        { 
			outOfViewport = true;
			break; 
		}

        vec2 samplePos = currentPosition.xy;
        float sampleDepth = convertScreenSpaceToWorldSpace(samplePos).z;

        float currentDepth = cameraSpaceVectorPosition.z;
        float diff = sampleDepth - currentDepth;
        float error = length(cameraSpaceVector / pow(2.0f, numRefinements));

        //If a collision was detected, refine raymarch
        if(diff >= 0.0 && diff <= error * 2.00f && numRefinements <= maxRefinements) 
        {
			/*if( -cameraSpaceVectorPosition.z > 100)
				break;*/
			outOfViewport = false;
        	//Step back
        	cameraSpaceVectorPosition -= cameraSpaceVector / pow(2.0f, numRefinements);
        	++numRefinements;
		//If refinements run out
		} 
		else if (diff >= 0.0 && diff <= error * 4.0f && numRefinements > maxRefinements)
		{
			outOfViewport = false;
			finalSamplePos = samplePos;
			break;
		}
		else if(diff > 0.0)
		{
			
		}
		
        cameraSpaceVectorPosition += cameraSpaceVector / pow(2.0f, numRefinements);

        if (numSteps > 1)
			cameraSpaceVector *= 1.275f;	//Each step gets bigger

		currentPosition = convertCameraSpaceToScreenSpace(cameraSpaceVectorPosition);
        count++;
        numSteps++;
    }
	<endif doRealtimeReflections>
	
	color = texture2D(albedoBuffer, finalSamplePos);
	
	if(numSteps > 38)
		color.a = 0.0f;
	if(texture2D(depthBuffer, screenSpaceCoords, 0.0).x == 1.0)
		color.a = 0.0f;
	if(outOfViewport)
		color.a = 0.0f;
	
	vec3 normSkyDirection = normalMatrixInv * cameraSpaceVector;
		
	vec3 skyColor = getSkyColor(time, normSkyDirection);
		
	//float specular = clamp(pow(dot(normalize(normSkyDirection),normalize(sunPos)),16.0),0.0,1.0);
		
	float sunSpecular = pow(clamp(dot(normalize(normSkyDirection),normalize(sunPos)), 0.0, 1.0),1750.0);
	<ifdef doDynamicCubemaps>
	skyColor = textureCube(environmentCubemap, vec3(normSkyDirection.x, -normSkyDirection.y, -normSkyDirection.z)).rgb;
	<endif doDynamicCubemaps>
	
	skyColor += vec3(100.0) * sunSpecular;
		
	skyColor *= showSkybox;//texture2D(blocklights, lightMapUV).rgb;
	
	if(color.a == 0.0)
	{
		color.rgb = skyColor;
	}
	else
	{
		vec4 cameraSpacePosition = convertScreenSpaceToWorldSpace(finalSamplePos);
		vec4 pixelNormal = texture2D(normalBuffer, finalSamplePos);
		pixelNormal.rgb * 2.0 - vec3(1.0);
		vec4 pixelMeta = texture2D(metaBuffer, finalSamplePos);
		//color = vec4(1.0, 0.0, 1.0, 1.0);
		color = computeLight(color, pixelNormal.xyz, cameraSpacePosition, pixelMeta, pixelNormal.w);
		//color.rgb = mix(color, skyColor, pixelNormal.w);
	}
	return color;
}

