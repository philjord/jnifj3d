#version 120

attribute vec4 glVertex;  
attribute vec4 glColor;        
attribute vec3 glNormal;     
attribute vec2 glMultiTexCoord0; 


uniform mat4 glModelViewMatrix;
uniform mat4 glModelViewProjectionMatrix;
uniform mat3 glNormalMatrix;

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

attribute vec3 tangent;
attribute vec3 binormal;

varying vec3 LightDir;
varying vec3 ViewVec;
varying vec3 HalfVector;

varying vec4 ColorEA;
varying vec4 ColorD;

vec3 N;
vec3 t;
vec3 b;

vec3 tspace( vec3 v )
{
	return vec3( dot( v, b ), dot( v, t ), dot( v, N ) );
}

void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0.0,1.0)).st;	
	
	N = normalize(glNormalMatrix * glNormal);
	t = normalize(glNormalMatrix * tangent);
	b = normalize(glNormalMatrix * binormal);
	
	ViewVec = tspace( ( glModelViewMatrix * glVertex ).xyz );
	LightDir = tspace( glLightSource[0].position.xyz ); // light 0 is directional
	// this differs from nifskope which has a half vector in the gl_lightsource, is it correct?
	HalfVector = ( glModelViewMatrix * glVertex ).xyz - glLightSource[0].position.xyz;
	
	if(ignoreVertexColors != 0)
		ColorEA = glFrontMaterial.emission + (glFrontMaterial.ambient * glLightModelambient);
	else
		ColorEA = glColor + (glFrontMaterial.ambient * glLightModelambient);
	
	ColorD = glFrontMaterial.diffuse * glLightSource[0].diffuse;
}
