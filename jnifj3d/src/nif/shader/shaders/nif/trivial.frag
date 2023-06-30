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

in vec3 ViewVec;
in vec4 C;

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
		
	color.rgb = albedo;
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
