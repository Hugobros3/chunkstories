vec4 computeReflectedPixel(vec2 screenSpaceCoords, vec3 cameraSpacePosition, vec3 pixelNormal, float showSkybox)
{
    vec2 screenSpacePosition2D = screenSpaceCoords;
	
    vec3 cameraSpaceViewDir = normalize(cameraSpacePosition);
    vec3 cameraSpaceVector = normalize(reflect(cameraSpaceViewDir, pixelNormal));
	vec3 oldPosition = cameraSpacePosition;
    vec3 cameraSpaceVectorPosition = oldPosition + cameraSpaceVector;
    vec3 currentPosition = convertCameraSpaceToScreenSpace(cameraSpaceVectorPosition);
    
	// Is the reflection pointing in the right direction ?
	
	
	vec4 color = vec4(0.0);// texture(comp_diffuse, screenSpacePosition2D); // vec4(pow(texture(gcolor, screenSpacePosition2D).rgb, vec3(3.0f + 1.2f)), 0.0);
   
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
        float sampleDepth = convertScreenSpaceToCameraSpace(samplePos, depthBuffer).z;

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
	
	color = texture(albedoBuffer, finalSamplePos);
	
	if(numSteps > 38)
		color.a = 0.0f;
	if(texture(depthBuffer, screenSpaceCoords, 0.0).x == 1.0)
		color.a = 0.0f;
	if(outOfViewport)
		color.a = 0.0f;
	
	vec3 normSkyDirection = normalMatrixInv * cameraSpaceVector;
		
	vec3 skyColor = getSkyColor(time, normSkyDirection);
		
	//float specular = clamp(pow(dot(normalize(normSkyDirection),normalize(sunPos)),16.0),0.0,1.0);
		
	float sunSpecular = 100.0 * clamp(1.0-overcastFactor * 2.0, 0.0, 1.0) * pow(clamp(dot(normalize(normSkyDirection),normalize(sunPos)), 0.0, 1.0),750.0);
	
	
	skyColor += vec3(sunSpecular);
		
	skyColor *= showSkybox;//texture(blocklights, lightMapUV).rgb;
	
	if(color.a == 0.0)
	{
		<ifdef doDynamicCubemaps>
		skyColor = texture(environmentCubemap, vec3(normSkyDirection.x, -normSkyDirection.y, -normSkyDirection.z)).rgb;
		
		//skyColor = pow(skyColor.rgb, vec3(gamma));
		skyColor *= showSkybox;
		
		return vec4(skyColor, 1.0);
		<endif doDynamicCubemaps>
	
		color.rgb = skyColor;
	}
	else
	{
		vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(finalSamplePos, depthBuffer);
		vec4 pixelNormal = texture(normalBuffer, finalSamplePos);
		vec4 pixelMeta = texture(metaBuffer, finalSamplePos);
		
		color = computeLight(color, decodeNormal(pixelNormal), cameraSpacePosition, pixelMeta, pixelNormal.z);
		
		// Apply fog
		vec3 sum = (cameraSpacePosition.xyz);
		float dist = length(sum)-fogStartDistance;
		float fogFactor = (dist) / (fogEndDistance-fogStartDistance);
		float fogIntensity = clamp(fogFactor, 0.0, 1.0);
		
		vec3 fogColor = getSkyColorWOSun(time, normalize(((modelViewMatrixInv * cameraSpacePosition).xyz - camPos).xyz));
		
		color = mix(color, vec4(fogColor,color.a), fogIntensity);
	}
	return color;
}

