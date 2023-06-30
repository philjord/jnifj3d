#version 140

precision mediump float;
precision highp int;

uniform float transparencyAlpha;	

uniform int alphaTestEnabled;
uniform int alphaTestFunction;
uniform float alphaTestValue;

struct fogDataStruct
{
	int fogEnabled;
	vec4 expColor;
	float expDensity;
	vec4 linearColor;
	float linearStart;
	float linearEnd;
};
uniform fogDataStruct fogData;

//End of FFP inputs
in vec2 glTexCoord0;

uniform sampler2D BaseMap;
uniform int numberOfLights;

in vec3 ViewVec;
in vec3 N;
in vec4 A;
in vec4 C;
in vec3 emissive;
in float shininess;

//NOTE android might support a very low number of varying attributes as low as 8
// console shows this for now, maxLights of 3 works for a max vectors of 16

const int maxLights = 3;
in vec4 lightsD[maxLights]; 
in vec3 lightsS[maxLights]; 
in vec3 lightsLightDir[maxLights]; 

out vec4 glFragColor;

void main( void )
{
	vec4 baseMap = texture( BaseMap, glTexCoord0.st );
	
	//web says the keyword discard in a shader is bad
	//I could just gl_FragColor=vec(0,0,0,0); return;
	if(alphaTestEnabled != 0)
	{				
	 	if(alphaTestFunction==516)//>
			if(baseMap.a<=alphaTestValue)discard;			
		else if(alphaTestFunction==518)//>=
			if(baseMap.a<alphaTestValue)discard;		
		else if(alphaTestFunction==514)//==
			if(baseMap.a!=alphaTestValue)discard;
		else if(alphaTestFunction==517)//!=
			if(baseMap.a==alphaTestValue)discard;
		else if(alphaTestFunction==513)//<
			if(baseMap.a>=alphaTestValue)discard;
		else if(alphaTestFunction==515)//<=
			if(baseMap.a>alphaTestValue)discard;		
		else if(alphaTestFunction==512)//never	
			discard;			
	}


	vec4 color;
	vec3 albedo = baseMap.rgb * C.rgb;
	vec3 diffuse = A.rgb;
	vec3 spec;
	
	vec3 normal = N;
	vec3 E = normalize(ViewVec);
	float EdotN = max( dot(normal, E), 0.0 );
	// TODO: bring the attenuation across from the per vertex version
	for (int index = 0; index < numberOfLights && index < maxLights; index++) // for all light sources
	{ 	
		vec3 L = normalize( lightsLightDir[index] );		
		//vec3 R = reflect(-L, normal);
		vec3 H = normalize( L + E );		
		float NdotL = max( dot(normal, L), 0.0 );
		float NdotH = max( dot(normal, H), 0.0 );		
		float NdotNegL = max( dot(normal, -L), 0.0 );	
	
		diffuse = diffuse + (lightsD[index].rgb * NdotL);
		spec = spec + (lightsS[index] * pow(NdotH, 0.3*shininess));
	}
	
	// see sphere_motion for multiple lights in phong style, below is blinn phong comparision both in frag
	//https://en.wikipedia.org/wiki/Blinn%E2%80%93Phong_shading_model

	
	color.rgb = albedo * (diffuse + emissive) + spec;
	color.a = C.a * baseMap.a;
	
	if(fogData.fogEnabled == 1)
	{
		//compute distance used in fog equations
		float dist = length(ViewVec);
		float fogFactor = 0.0;  		  
		 
		if(fogData.linearEnd > 0.0)//linear fog
		{
		   fogFactor = 1.0-((fogData.linearEnd - dist)/(fogData.linearEnd - fogData.linearStart));
		   fogFactor = clamp( fogFactor, 0.0, 1.0 );
		   color = mix(color, fogData.linearColor, fogFactor);			    
		}
		else if( fogData.expDensity > 0.0)// exponential fog
		{
		    fogFactor = 1.0-(1.0 /exp(dist * fogData.expDensity));
		    fogFactor = clamp( fogFactor, 0.0, 1.0 );
		    color = mix(color, fogData.expColor, fogFactor);
		}	
		color.a = color.a + fogFactor; 	 
	}
    
    color *= transparencyAlpha;
     
	glFragColor = color; 



}
