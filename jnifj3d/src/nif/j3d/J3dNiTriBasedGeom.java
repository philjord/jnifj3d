package nif.j3d;

import utils.source.TextureSource;
import nif.niobject.NiTriBasedGeom;

//public abstract class J3dNiTriBasedGeom extends J3dNiGeometryShader
public abstract class J3dNiTriBasedGeom extends J3dNiGeometry
{
	public J3dNiTriBasedGeom(NiTriBasedGeom niTriBasedGeom, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriBasedGeom, niToJ3dData, textureSource);
	}
}
