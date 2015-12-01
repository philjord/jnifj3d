package nif.appearance;

import javax.media.j3d.Appearance;
import javax.media.j3d.Shape3D;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiGeometry;
import utils.source.TextureSource;

public interface NiGeomteryAppearance
{
	public Appearance configureAppearance(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource, Shape3D shape,
			J3dNiAVObject target);
}
