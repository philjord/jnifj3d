#version 120

attribute vec4 glVertex;
attribute vec4 glColor;
attribute vec2 glMultiTexCoord0; 

attribute vec4 alphas04;
attribute vec4 alphas58;
attribute vec4 alphas912;


//uniform mat4 glProjectionMatrix;
uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;
//uniform mat3 glNormalMatrix;

 
uniform int ignoreVertexColors;

uniform vec4 glLightModelambient;

struct material
{
	int lightEnabled;
 	vec4 ambient;
 	vec4 diffuse;
 	vec4 emission; 
 	vec3 specular;
 	float shininess;
};
uniform material glFrontMaterial;

struct lightSource
{
	 vec4 position;
	 vec4 diffuse;
	 vec4 specular;
	 float constantAttenuation, linearAttenuation, quadraticAttenuation;
	 float spotCutoff, spotExponent;
	 vec3 spotDirection;
};

uniform int numberOfLights;
const int maxLights = 2;
uniform lightSource glLightSource[maxLights];

varying vec2 glTexCoord0;

//TODO: my land needs to have lighting!
//varying vec3 LightDir;
varying vec3 ViewVec;

varying vec4 A;
varying vec4 C;
varying vec4 D;

varying vec4 layerAlpha4;
varying vec4 layerAlpha8;
varying vec4 layerAlpha12;
 

void main( void )
{	
	gl_Position = glModelViewProjectionMatrix * glVertex;
	glTexCoord0 = glMultiTexCoord0.st; 
	
	A = glLightModelambient;
	if( ignoreVertexColors != 0 ) 
		C = glFrontMaterial.diffuse; 
	else 
		C = glColor;
	D = glLightSource[0].diffuse * glFrontMaterial.diffuse;
	 
	
	layerAlpha4 = alphas04;
	layerAlpha8 = alphas58;
	layerAlpha12 = alphas912; 	
	
	vec3 v = vec3(glModelViewMatrix * glVertex);
	ViewVec = -v.xyz;// do not normalize also used for view dist
}
