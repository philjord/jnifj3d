#version 150 

in vec4 glVertex;         
in vec4 glColor;       
in vec3 glNormal;     
in vec2 glMultiTexCoord0; 

uniform mat4 glProjectionMatrix;
//uniform mat4 glProjectionMatrixInverse;
//uniform mat4 glViewMatrix;
uniform mat4 glModelMatrix;
uniform mat4 glModelViewMatrix;
//uniform mat4 glModelViewMatrixInverse;
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
	 vec4 position;// view space
	 vec4 diffuse;
	 vec4 specular;
	 float constantAttenuation, linearAttenuation, quadraticAttenuation;
	 float spotCutoff, spotExponent;
	 vec3 spotDirection;// world space
};

uniform int numberOfLights;// numberOfLights will be set to how many the pipeline can send
const int maxLights = 8;// this is for the shader, it will process no more than this, must be a const
uniform lightSource glLightSource[maxLights];

uniform mat4 textureTransform;

out vec2 glTexCoord0;

out vec3 ViewVec;
out vec4 C;
out vec3 light;


void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0.0,1.0)).st;		

	mat3 glNormalMatrix =  mat3(transpose(inverse(glModelViewMatrix)));
	vec3 N = normalize(glNormalMatrix * glNormal);
		
	vec3 v = vec3(glModelViewMatrix * glVertex);
	ViewVec = -v.xyz;// do not normalize also used for view dist	

	vec4 A = glLightModelambient *  glFrontMaterial.ambient;
			 
	if( ignoreVertexColors != 0) 
	{
		// objectColor should be used if it is no lighting, and reusing material diffuse appears wrong
		C = vec4(1,1,1,1);//glFrontMaterialdiffuse;
	}
	else 
		C = glColor; 
		
	vec3 emissive = glFrontMaterial.emission.rgb;
	float shininess = glFrontMaterial.shininess;
	
	vec3 diffuse = A.rgb;
	vec3 spec = vec3(0,0,0);
	
	
	vec4 vertPos = glModelViewMatrix * glVertex;// vertex position in eye space
	vec3 E = normalize(-vertPos.xyz);// vector from vert to eye in eye space
	//float EdotN = max( dot(N, E), 0.0 );	
		
	//http://www.learnopengles.com/tag/per-vertex-lighting/
	// shows that N is in eye space so must everything else be
	
	//https://github.com/stackgl/glsl-lighting-walkthrough#shaders
	//https://www.opengl.org/sdk/docs/tutorials/ClockworkCoders/lighting.php
	
	for (int index = 0; index < numberOfLights && index < maxLights; index++) // for all light sources
	{ 			
		vec4 Lp = glLightSource[index].position; // in eye space
		
		vec3 Ld;
		if(Lp.w == 0.0 )
			Ld = normalize( Lp.xyz );  //directional store dir in pos
		else
			Ld = normalize( Lp.xyz - vertPos.xyz );	
				
		float NdotL = max( dot(N, Ld), 0.0 );		
		vec3 d = ((glLightSource[index].diffuse * glFrontMaterial.diffuse).rgb * NdotL);
		d = clamp(d, 0.0, 1.0);     		
		
		// 2 versions of s calc
		//vec3 H = normalize( Ld + E ); // the half vector
		//float NdotH = max( dot(N, H), 0.0 );		
		//vec3 s = ((glLightSource[index].specular.rgb * glFrontMaterial.specular) * pow(NdotH, 0.3*shininess));
		
		vec3 R = normalize(-reflect(Ld,N));  
		vec3 s = ((glLightSource[index].specular.rgb * glFrontMaterial.specular) * pow(max(dot(R,E),0.0), 0.3*shininess));
    	s = clamp(s, 0.0, 1.0);     
    
		// Attenuate the light based on distance. (but not for directional!)
		if(Lp.w == 1.0)
		{
   			float dist = length(Lp - vertPos);   
   			float att = (1.0 / (glLightSource[index].constantAttenuation +
				(glLightSource[index].linearAttenuation*dist) +
					(glLightSource[index].quadraticAttenuation*dist*dist)));    
			att = clamp(att, 0.0, 1.0);   
   			d = d * att;
   			s = s * att; 
    	}			 
		
		diffuse = diffuse + d;
        spec = spec + s;	
	}	
	
	light = (diffuse + emissive) + spec;		 
}
