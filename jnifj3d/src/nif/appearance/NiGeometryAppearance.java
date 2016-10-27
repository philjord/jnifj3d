package nif.appearance;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.Shape3D;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiGeometry;
import utils.source.TextureSource;

public interface NiGeometryAppearance
{
	public Appearance configureAppearance(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource, Shape3D shape,
			J3dNiAVObject target);
}
