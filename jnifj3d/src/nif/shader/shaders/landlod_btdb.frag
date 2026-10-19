#version 120
//https://www.khronos.org/files/opengles_shading_language.pdf
precision mediump float;

varying vec4 A;
varying vec4 C;
varying vec4 D;

//glsles requires highp for a shared uniform
//uniform highp int layerCount;

varying vec3 ViewVec;

varying vec4 lodPos;
uniform vec2 minXYRemoval;
uniform vec2 maxXYRemoval;

void main( void )
{
	vec3 albedo = C.rgb;
	
	vec3 diffuse = A.rgb + D.rgb;

	vec4 color;
	color.rgb = albedo * diffuse;
	color.a = 1.0;
	 	
	gl_FragColor = color;	
	
	
	if(lodPos.x > minXYRemoval.x && lodPos.z < -minXYRemoval.y 
	&& lodPos.x < maxXYRemoval.x && lodPos.z > -maxXYRemoval.y )
		discard; //gl_FragColor = vec4(1,0,1,1);//

}