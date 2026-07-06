#version 140

precision mediump float;
 
//End of FFP inputs
in vec2 glTexCoord0; // represents only the start of the uv, does not vary across the particle

uniform float transparencyAlpha;	

uniform sampler2D BaseMap;
 
in mediump vec2 TextureSize;

in vec4 C;
in mediump mat3 v_rotationMatrix;

out vec4 glFragColor;

float sqrt2 = sqrt(2.0);

void main( void )
{	

							//glFragColor = vec4(vec2(1)-gl_PointCoord.xy,0,1);return;
							//if(gl_PointCoord.x < gl_PointCoord.y){glFragColor = vec4(0,1,1,1);return;}
	 
	//rotate the point coord in the space of one sub texture
//	mediump vec3 rotCoord = v_rotationMatrix * vec3((gl_PointCoord * TextureSize), 1.0); 

 
	// we need to increase the texcoord so it's a smaller square inside the unit square
	mediump vec2  unrotatedUV = gl_PointCoord; //unit 0-1	
	// reduce the texcoords to be a smaller square by sampling a larger range	
	unrotatedUV *= (sqrt2 * TextureSize);
	
										
	// now recenter that new smaller square
	unrotatedUV += (TextureSize * 0.5) - ((sqrt2 * TextureSize) * 0.5); 
	mediump vec3 rotCoord = v_rotationMatrix * vec3((unrotatedUV), 1.0); 	
		
	// move across to the actual in use sub texture
 	mediump vec2 realTexCoord = glTexCoord0 + rotCoord.st;   
 
 	//rotated textures go outside the bounds of 0-1 so discard anything else
 	//If it falls outside the sub texture range discard it, sub texture start at glTexCoord0 and has stride of TextureSize
 	//if( realTexCoord.s < glTexCoord0.s || realTexCoord.s > glTexCoord0 + TextureSize.s 
 	// || realTexCoord.t < glTexCoord0.t || realTexCoord.t > glTexCoord0 + TextureSize.t) {
 	// but it seems clearer to check the rotCoord adjustment values
 	if(rotCoord.s < 0.0 || rotCoord.t < 0.0 ||  rotCoord.s > TextureSize.s  || rotCoord.t > TextureSize.t) { 
 		discard;		
	}
 	
 	// get the color	
    mediump vec4 fragColor = texture(BaseMap, realTexCoord.st); 
			
			
																		// takign roation out of it
																		//texture(BaseMap, glTexCoord0); 
			
    glFragColor = fragColor * C;
    glFragColor.a *= transparencyAlpha;   
   																		//if(glTexCoord0.t == 0 )
   																		//glFragColor = vec4(glTexCoord0 * 100,1,1);
   																		 
   																		glFragColor.a *= 10; // just so we can see it a bit better
   																		 
   																		    																		 
   																		 
   																		 
   																		// if(C == vec4(1,0,1,1))
   																		// 	glFragColor = C;
   																		 
   																		// glFragColor = vec4(1,0,1,1);
   																		
   																		// if(rotCoord.s == realTexCoord.s)
   																		//	glFragColor = vec4(1,0,1,1);
   																		
   																		//glFragColor.yz=texture(BaseMap, vec2(0.5,0.5)).yz;
   																		
   																		//glFragColor.x=0.1;
}
