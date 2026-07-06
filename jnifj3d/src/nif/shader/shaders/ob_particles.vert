#version 150 
//#version 120 is not optional, trouble otherwise

//Note don't put if else constructs on one line or trouble

in vec4 glVertex;         
in vec4 glColor;       
//in vec3 glNormal;     
in vec2 glMultiTexCoord0; 

uniform mat4 glProjectionMatrix;
//uniform mat4 glProjectionMatrixInverse;
uniform mat4 glViewMatrix;
uniform mat4 glModelMatrix;

uniform int ignoreVertexColors;
	
uniform mat4 textureTransform;
//End of FFP inputs

in float Size;
in float rCos;
in float rSin;

out mat3 v_rotationMatrix;
out vec4 C;

// The size of the sprite being rendered as a sub texture for an atlas. 
in vec2 SubTextureSize;

out vec2 glTexCoord0;
out vec2 TextureSize;

 
uniform float screenWidth;      //screen width in pixels

float sqrt2 = sqrt(2.0);

void main( void )
{
	mat4 glModelViewMatrix = glViewMatrix * glModelMatrix;
	gl_Position = glProjectionMatrix * glModelViewMatrix * glVertex;//glModelViewProjectionMatrix * glVertex;
	
	glTexCoord0 = (textureTransform * vec4(glMultiTexCoord0,0.0,1.0)).xy;	
	TextureSize = SubTextureSize;
	
	vec4 v2 =  glModelViewMatrix * glVertex;
	vec4 projCorner = glProjectionMatrix * vec4(0.5*Size, 0.5*Size, v2.z, v2.w);
	gl_PointSize = screenWidth * projCorner.x / projCorner.w;
	
	//rotated or not we need to increase the size to fit the rotateers into the square shape
	//https://stackoverflow.com/questions/57619285/calculate-how-much-smaller-a-square-would-have-to-be-to-fit-after-rotated-45-deg
	//A square of edge length a has a diagonal of length d = sqrt(2)*a 
	gl_PointSize = sqrt2*gl_PointSize;
		
	// Also we need to reduce the texcoord so it's a smaller  square inside the new unit square
	// but this is done in the frag as the uv are no longer axis aligned
	
	
	if(ignoreVertexColors == 0)
		C = glColor; 
	else
		C = vec4(1.0, 1.0, 1.0, 1.0);
	

    
    // rotation matrix is just for the UV coords, so mix the atlas sub texture size in
    //(sqrt(2) * TextureSize).t*0.5 to cause it to rotate around teh center of the larger size tex coords
    v_rotationMatrix = mat3(rCos, rSin, 0.0,
                        -rSin, rCos, 0.0,
                        (rSin-rCos+1.0)*((( TextureSize) * 0.5)).s, 
                        (-rSin-rCos+1.0)*((( TextureSize) * 0.5)).t,
                        1.0);
 
}
