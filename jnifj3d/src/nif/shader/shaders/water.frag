#version 120

precision mediump float;

uniform sampler2D tex;

varying vec3 eyeNormal;

varying vec3 lightDir;

varying vec2 glTexCoord0;

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
 
varying vec3 ViewVec;

varying vec4 A;
varying vec4 C;
varying vec4 D;

void main() 
{ 
	vec4 color = texture2D(tex, glTexCoord0.st);

   	float intensity,at,af;
    vec3 ct,cf;
    intensity = max(dot(lightDir,normalize(eyeNormal)),0.0); 
    cf = intensity * D.rgb + A.rgb;
    af = D.a;    
     
	ct = color.rgb * C.rgb;
	at = color.a;
	color = vec4((ct * cf),(at * af) ); 
 	
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
     
	gl_FragColor = color; 	
	
}