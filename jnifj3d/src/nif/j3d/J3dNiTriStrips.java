package nif.j3d;

import javax.media.j3d.GeometryArray;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;

import nif.niobject.NiTriStrips;
import nif.niobject.NiTriStripsData;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

/**
 * NOTE! Skyrim appears to not use these any more! only trishape
 * @author philip
 *
 */
public class J3dNiTriStrips extends J3dNiTriBasedGeom
{
	private NiTriStripsData data;

	public J3dNiTriStrips(NiTriStrips niTriStrips, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriStrips, niToJ3dData, textureSource);
		niToJ3dData.put(niTriStrips, this);
		data = (NiTriStripsData) niToJ3dData.get(niTriStrips.data);

		getShape().setGeometry(makeGeometry(makeGeometryInfo(data), true, data));
	}

	/**
	 * Note expensive re-create should be optomised one day
	 */
	public void makeMorphable()
	{
		GeometryArray newGeom = makeGeometry(makeGeometryInfo(data), false, null);
		getShape().setGeometry(newGeom);
		baseGeometryArray = newGeom;
	}

	private static GeometryInfo makeGeometryInfo(NiTriStripsData data)
	{
		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);

		if (data.hasVertices)
		{
			Point3f[] vertices = new Point3f[data.numVertices];
			for (int i = 0; i < data.numVertices; i++)
			{
				vertices[i] = ConvertFromNif.toJ3dP3f(data.vertices[i]);
			}
			gi.setCoordinates(vertices);

		}

		if (data.hasNormals)
		{
			Vector3f[] normals = new Vector3f[data.numVertices];
			for (int i = 0; i < data.numVertices; i++)
			{
				normals[i] = ConvertFromNif.toJ3dNoScale(data.normals[i]);
			}
			gi.setNormals(normals);

		}

		Color4f[] colors = new Color4f[data.numVertices];
		for (int i = 0; i < data.numVertices; i++)
		{
			if (data.hasVertexColors)
			{
				colors[i] = ConvertFromNif.toJ3d(data.vertexColors[i]);
			}
			else
			{
				colors[i] = new Color4f(1, 1, 1, 1);
			}
		}
		gi.setColors(colors);

		// process UVsets hasUV or UVset2?? Num UV Sets 2
		int actNumUVSets = data.actNumUVSets;
		if (actNumUVSets > 0)
		{
			gi.setTextureCoordinateParams(actNumUVSets, 2);

			for (int i = 0; i < actNumUVSets; i++)
			{
				TexCoord2f[] texCoords = new TexCoord2f[data.uVSets[i].length];
				for (int j = 0; j < data.uVSets[i].length; j++)
				{
					texCoords[j] = ConvertFromNif.toJ3d(data.uVSets[i][j]);
				}
				gi.setTextureCoordinates(i, texCoords);

			}

		}

		int numStrips = data.numStrips;
		int[] stripLengths = data.stripLengths;
		int[] points = null;
		if (data.hasPoints)
		{
			// get full length
			int length = 0;
			for (int i = 0; i < numStrips; i++)
			{
				length += data.points[i].length;
			}

			gi.setStripCounts(stripLengths);
			points = new int[length];
			int idx = 0;
			for (int i = 0; i < numStrips; i++)
			{
				for (int j = 0; j < stripLengths[i]; j++)
				{
					points[idx] = data.points[i][j];
					idx++;
				}
			}

			gi.setCoordinateIndices(points);
			gi.setUseCoordIndexOnly(true);
		}

		if (!data.hasNormals)
		{
			NormalGenerator normalGenerator = new NormalGenerator();
			normalGenerator.generateNormals(gi);
		}

		return gi;

	}
}
