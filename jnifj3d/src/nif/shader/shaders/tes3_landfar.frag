#version 120
//https://www.khronos.org/files/opengles_shading_language.pdf
precision mediump float;

uniform sampler2D baseMap;

uniform sampler2D sampler0;
uniform sampler2D sampler1;
uniform sampler2D sampler2;
uniform sampler2D sampler3;
uniform sampler2D sampler4;
uniform sampler2D sampler5;
uniform sampler2D sampler6;
uniform sampler2D sampler7;
uniform sampler2D sampler8;
uniform sampler2D sampler9;
uniform sampler2D sampler10;
uniform sampler2D sampler11;
uniform sampler2D sampler12;
uniform sampler2D sampler13;
uniform sampler2D sampler14;
uniform sampler2D sampler15;

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

//glsles requires highp for a shared uniform
//uniform highp int layerCount;

varying vec2 glTexCoord0;

varying vec4 fragSamplers0;
varying vec4 fragSamplers1;
varying vec4 fragSamplers2;
varying vec4 fragSamplers3;



void main( void )
{
	vec4 baseMapTex = texture2D( baseMap, glTexCoord0.st );
	
	vec3 albedo = baseMapTex.rgb;		
	
	albedo = mix(albedo, texture2D( sampler0, glTexCoord0.st ).rgb, fragSamplers0.x);
	albedo = mix(albedo, texture2D( sampler1, glTexCoord0.st ).rgb, fragSamplers0.y);
	albedo = mix(albedo, texture2D( sampler2, glTexCoord0.st ).rgb, fragSamplers0.z);
	albedo = mix(albedo, texture2D( sampler3, glTexCoord0.st ).rgb, fragSamplers0.w);
	albedo = fragSamplers1.x > 0.0 ? mix(albedo, texture2D( sampler4, glTexCoord0.st ).rgb, fragSamplers1.x) : albedo;
	albedo = fragSamplers1.y > 0.0 ? mix(albedo, texture2D( sampler5, glTexCoord0.st ).rgb, fragSamplers1.y) : albedo;
	albedo = fragSamplers1.z > 0.0 ? mix(albedo, texture2D( sampler6, glTexCoord0.st ).rgb, fragSamplers1.z) : albedo;
	albedo = fragSamplers1.w > 0.0 ? mix(albedo, texture2D( sampler7, glTexCoord0.st ).rgb, fragSamplers1.w) : albedo;
	albedo = fragSamplers2.x > 0.0 ? mix(albedo, texture2D( sampler8, glTexCoord0.st ).rgb, fragSamplers2.x) : albedo;
	albedo = fragSamplers2.y > 0.0 ? mix(albedo, texture2D( sampler9, glTexCoord0.st ).rgb, fragSamplers2.y) : albedo;
	albedo = fragSamplers2.z > 0.0 ? mix(albedo, texture2D( sampler10, glTexCoord0.st ).rgb, fragSamplers2.z) : albedo;
	albedo = fragSamplers2.w > 0.0 ? mix(albedo, texture2D( sampler11, glTexCoord0.st ).rgb, fragSamplers2.w) : albedo;
	albedo = fragSamplers3.x > 0.0 ? mix(albedo, texture2D( sampler8, glTexCoord0.st ).rgb, fragSamplers3.x) : albedo;
	albedo = fragSamplers3.y > 0.0 ? mix(albedo, texture2D( sampler9, glTexCoord0.st ).rgb, fragSamplers3.y) : albedo;
	albedo = fragSamplers3.z > 0.0 ? mix(albedo, texture2D( sampler10, glTexCoord0.st ).rgb, fragSamplers3.z) : albedo;
	albedo = fragSamplers3.w > 0.0 ? mix(albedo, texture2D( sampler11, glTexCoord0.st ).rgb, fragSamplers3.w) : albedo;
	
	
	
	
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
	

	gl_FragColor = color;	
	//gl_FragColor = baseMapTex;
	//gl_FragColor = vec4(float(layerCount)/6.0,layerAlpha4.x,layerAlpha4.y,1);
 
}