#version 120

attribute vec4 glVertex;         
attribute vec4 glColor;       
attribute vec2 glMultiTexCoord0; 


uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;

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

uniform mat4 textureTransform;
//End of FFP inputs
varying vec2 glTexCoord0;

varying vec3 LightDir;
varying vec3 ViewVec;

varying vec3 v;

varying vec4 A;
varying vec4 C;
varying vec4 D;


void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0.0,1.0)).st;
							  
	v = vec3(glModelViewMatrix * glVertex);

	ViewVec = -v.xyz;
	LightDir = glLightSource[0].position.xyz;
	
	A = glLightModelambient;
	if( ignoreVertexColors != 0) 
		C = glFrontMaterial.diffuse; 
	else 
		C = glColor;
	D = glLightSource[0].diffuse;
}