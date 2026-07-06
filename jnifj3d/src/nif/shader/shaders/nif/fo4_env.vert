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

varying vec3 N;
varying vec3 t;
varying vec3 b;
varying vec3 v;

varying vec4 A;
varying vec4 C;
varying vec4 D;

// this is not env but it has the same vert code
//https://github.com/niftools/nifskope/blob/develop/res/shaders/fo4_default.vert
void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0.0,1.0)).st;	
	
	N = normalize(glNormalMatrix * glNormal);
	t = normalize(glNormalMatrix * tangent);
	b = normalize(glNormalMatrix * binormal);
	
				//notice with glNormalMatrix taken out, my lighting calcs are still wrong so that's not the problem
				//N = normalize( glNormal);
				//t = normalize( tangent);
				//b = normalize( binormal);
	
	// No this doens't seem to fix nothinf in FO4 but I did it because of FO76?
	//b = cross( N, t ); // my binormal attribute data seems corrupt
	
	// NOTE: b<->t versus the nifskope source
	mat3 tbnMatrix = mat3(t.x, b.x, N.x,
                          t.y, b.y, N.y,
                          t.z, b.z, N.z);
   // ok so there's also this constructor
   // tbnMatrix = mat3(t,b,N);               
                          
                          
                         //this version has consistent lighting across edges N,t,t
                         
                         //N,t,b
                         //N,b,t
                         //t,n,b
                         //b,n,t
                         //t,b,n
                         //b,t,n
                         
                         // ok so there's also this constructor
                        // b = cross( N, t );
                        //  tbnMatrix = mat3( N,t,b);
						  
	v = vec3(glModelViewMatrix * glVertex);
	
	ViewVec = tbnMatrix * -v.xyz;
	LightDir = tbnMatrix * glLightSource[0].position.xyz;
	
								//LightDir = glNormalMatrix * glLightSource[0].position.xyz;
	
	A = glLightModelambient;
	if( ignoreVertexColors != 0) 
		C = glFrontMaterial.diffuse; 
	else 
		C = glColor;
	D = glLightSource[0].diffuse;
}