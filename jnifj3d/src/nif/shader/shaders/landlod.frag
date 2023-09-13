#version 120

precision mediump float;

uniform sampler2D baseMap;

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

varying vec2 glTexCoord0;

varying vec3 ViewVec;
 
varying vec4 A;
varying vec4 C;
varying vec4 D;

varying vec4 lodPos;
uniform vec2 minXYRemoval;
uniform vec2 maxXYRemoval;

void main( void )
{
	vec4 baseMapTex = texture2D( baseMap, glTexCoord0.st );
	
	vec3 albedo = baseMapTex.rgb;	
	
	albedo = albedo * C.rgb;
	
	vec3 diffuse = A.rgb + D.rgb;

	vec4 color;
	color.rgb = albedo * diffuse ;
	color.a = 1.0;
	

	
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
			
	if(lodPos.x > minXYRemoval.x && lodPos.z < -minXYRemoval.y 
	&& lodPos.x < maxXYRemoval.x && lodPos.z > -maxXYRemoval.y )
		discard;
 		
	gl_FragColor = color;	 
}