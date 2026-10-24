#version 120
//https://www.khronos.org/files/opengles_shading_language.pdf
precision mediump float;

uniform sampler2D baseMap;

uniform sampler2D layerMap0;
uniform sampler2D layerMap1;
uniform sampler2D layerMap2;
uniform sampler2D layerMap3;
uniform sampler2D layerMap4;

varying vec4 A;
varying vec4 C;
varying vec4 D;

//glsles requires highp for a shared uniform
//uniform highp int layerCount;

varying vec2 glTexCoord0;
	
varying vec3 ViewVec;

varying vec4 layerAlpha4;
varying float layerAlpha5;

void main( void )
{
	vec3 albedo = texture2D(baseMap, glTexCoord0.st).rgb;
	
	albedo = mix(albedo, texture2D(layerMap0, glTexCoord0.st).rgb, layerAlpha4.x);
	albedo = mix(albedo, texture2D(layerMap1, glTexCoord0.st).rgb, layerAlpha4.y);
	albedo = mix(albedo, texture2D(layerMap2, glTexCoord0.st).rgb, layerAlpha4.z);
	albedo = mix(albedo, texture2D(layerMap3, glTexCoord0.st).rgb, layerAlpha4.w);
	
	//FIXME: this guy used to always be black
	// now I've fixed the black issue, it never seems to have a texture attached
	// is it just wildly underused? 
	//albedo = mix(albedo, texture2D(layerMap4, glTexCoord0.st).rgb, layerAlpha5);
	
	albedo = albedo * C.rgb;
	
	vec3 diffuse = A.rgb + D.rgb;

	vec4 color;
	color.rgb = albedo * diffuse;
	color.a = 1.0;
	 	
	gl_FragColor = color;	
}