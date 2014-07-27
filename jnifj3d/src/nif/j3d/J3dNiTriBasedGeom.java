package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;

import nif.niobject.NiTriBasedGeom;
import tools.WeakValueHashMap;
import utils.source.TextureSource;

import com.sun.j3d.utils.geometry.GeometryInfo;

//public abstract class J3dNiTriBasedGeom extends J3dNiGeometryShader
public abstract class J3dNiTriBasedGeom extends J3dNiGeometry
{
	protected GeometryArray baseGeometryArray;

	public J3dNiTriBasedGeom(NiTriBasedGeom niTriBasedGeom, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriBasedGeom, niToJ3dData, textureSource);
	}

	public abstract void makeMorphable();

	public GeometryArray getBaseGeometryArray()
	{
		return baseGeometryArray;
	}

	//Note self expunging cache
	private static WeakValueHashMap<Object, IndexedGeometryArray> sharedIGAs = new WeakValueHashMap<Object, IndexedGeometryArray>();

	/** Note if compact the return will be a strips array 
	 * 
	 * @param geometryInfo
	 * @param compact and make sharable
	 * @return
	 */
	public static GeometryArray makeGeometry(GeometryInfo geometryInfo, boolean notMorphable, Object cacheKey)
	{
		if (notMorphable)
		{
			IndexedGeometryArray iga = sharedIGAs.get(cacheKey);

			if (iga != null)
			{
				return iga;
			}
			else
			{
				geometryInfo.compact();
				IndexedGeometryArray ita = geometryInfo.getIndexedGeometryArray(true, false, true, true, false);
				sharedIGAs.put(cacheKey, ita);
				return ita;
			}
		}
		else
		{
			IndexedGeometryArray ita = geometryInfo.getIndexedGeometryArray(false, true, false, true, false);
			ita.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ita.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
			return ita;
		}

	}
}
