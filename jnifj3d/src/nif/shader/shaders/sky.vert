#version 120

attribute vec4 glVertex;
attribute vec2 glMultiTexCoord0;

uniform mat4 glModelViewProjectionMatrix;
varying vec2 glTexCoord0;

varying float azi;

void main( void )
{
	gl_Position = glModelViewProjectionMatrix * glVertex;
	glTexCoord0 = glMultiTexCoord0.st;
	
	// TODO work out the azimuth and adjust fog color
	azi = glVertex.y;
}