#version 140

precision mediump float;
 
//End of FFP inputs
in vec2 glTexCoord0;

uniform float transparencyAlpha;	

uniform sampler2D BaseMap;
 
in mediump vec2 TextureSize;

in vec4 C;
in mat3 v_rotationMatrix;

out vec4 glFragColor;

void main( void )
{	 	
	//rotate the point coord in the space of one sub texture
	mediump vec3 rotCoord = v_rotationMatrix * vec3((gl_PointCoord * TextureSize), 1.0); 
	// move across to the actual in use sub texture
 	mediump vec2 realTexCoord = glTexCoord0 + rotCoord.st;
 	// get the color	
    mediump vec4 fragColor = texture(BaseMap, realTexCoord.st); 

    glFragColor = fragColor * C;
    glFragColor.a *= transparencyAlpha;   
}
