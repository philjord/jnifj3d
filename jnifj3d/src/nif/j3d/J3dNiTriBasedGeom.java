package nif.j3d;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.J3DBuffer;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;

import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriBasedGeomData;
import tools.WeakValueHashMap;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

/**
 * This class has a base geometry and a current to allow skin instances to deform the base
 * @author philip
 *
 */
public abstract class J3dNiTriBasedGeom extends J3dNiGeometry
{
	//morrowind wants true false false
	//oblivion wants true false false
	//skyrim wants true false false
	public static boolean INTERLEAVE = true;

	public static boolean STRIPIFY = false;// relavant to shape only

	public static boolean BUFFERS = false;

	public static boolean OUTLINE_MORPHS_DEMO = true;

	public static int OUTLINE_STENCIL_MASK = 0x01;

	protected GeometryArray baseGeometryArray;

	protected GeometryArray currentGeometryArray;

	protected NiTriBasedGeomData data;

	protected boolean isMorphable = false;

	public J3dNiTriBasedGeom(NiTriBasedGeom niTriBasedGeom, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niTriBasedGeom, niToJ3dData, textureSource);
		data = (NiTriBasedGeomData) niToJ3dData.get(niTriBasedGeom.data);
	}

	protected abstract IndexedGeometryArray createGeometry(boolean morphable);

	/**
	 * Note expensive re-create should be optomised one day
	 */
	public void makeMorphable()
	{
		if (!isMorphable)
		{
			getShape().setBoundsAutoCompute(false);// expensive to do regularly so animated node just get one
			getShape().setBounds(new BoundingSphere(ConvertFromNif.toJ3dP3d(data.center), ConvertFromNif.toJ3d(data.radius)));

			baseGeometryArray = createGeometry(true);
			currentGeometryArray = createGeometry(true);
			getShape().setGeometry(currentGeometryArray);
			isMorphable = true;

			if (OUTLINE_MORPHS_DEMO)
			{

				//-Dj3d.stencilClear=true still required??

				Appearance sapp = getShape().getAppearance();
				RenderingAttributes ra1 = new RenderingAttributes();
				ra1.setStencilEnable(true);
				ra1.setStencilWriteMask(OUTLINE_STENCIL_MASK);
				ra1.setStencilFunction(RenderingAttributes.ALWAYS, 1, OUTLINE_STENCIL_MASK);
				ra1.setStencilOp(RenderingAttributes.STENCIL_REPLACE, //
						RenderingAttributes.STENCIL_REPLACE,//
						RenderingAttributes.STENCIL_REPLACE);
				sapp.setRenderingAttributes(ra1);

				Shape3D outliner = new Shape3D();
				outliner.setGeometry(currentGeometryArray);
				Appearance app = new Appearance();
				LineAttributes la = new LineAttributes(4, LineAttributes.PATTERN_SOLID, true);
				app.setLineAttributes(la);
				PolygonAttributes pa = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0.0f, false, 0.0f);
				app.setPolygonAttributes(pa);
				ColoringAttributes colorAtt = new ColoringAttributes(1.0f, 1.0f, 0.0f, ColoringAttributes.FASTEST);
				app.setColoringAttributes(colorAtt);

				RenderingAttributes ra2 = new RenderingAttributes();
				ra2.setStencilEnable(true);
				ra2.setStencilWriteMask(OUTLINE_STENCIL_MASK);
				ra2.setStencilFunction(RenderingAttributes.NOT_EQUAL, 1, OUTLINE_STENCIL_MASK);
				ra2.setStencilOp(RenderingAttributes.STENCIL_KEEP, //
						RenderingAttributes.STENCIL_KEEP,//
						RenderingAttributes.STENCIL_KEEP);

				ra2.setDepthBufferEnable(false);

				app.setRenderingAttributes(ra2);
				outliner.setAppearance(app);
				addChild(outliner);
			}
		}
	}

	public GeometryArray getBaseGeometryArray()
	{
		return baseGeometryArray;
	}

	public GeometryArray getCurrentGeometryArray()
	{
		return currentGeometryArray;
	}

	//Note self expunging cache
	protected static WeakValueHashMap<Object, IndexedGeometryArray> sharedIGAs = new WeakValueHashMap<Object, IndexedGeometryArray>();

	protected static int getFormat(NiTriBasedGeomData data, boolean morphable, boolean interleave)
	{
		int vertexFormat = (data.hasVertices ? GeometryArray.COORDINATES : 0) //
				| (data.hasNormals ? GeometryArray.NORMALS : 0) //
				| (data.actNumUVSets > 0 ? GeometryArray.TEXTURE_COORDINATE_2 : 0) //
				| (data.vertexColorsOpt != null ? GeometryArray.COLOR_4 : 0) //
				| GeometryArray.USE_COORD_INDEX_ONLY //
				| ((morphable || interleave || BUFFERS) ? GeometryArray.BY_REFERENCE_INDICES : 0)//				
				| ((morphable || interleave || BUFFERS) ? GeometryArray.BY_REFERENCE : 0)//
				| ((!morphable && interleave) ? GeometryArray.INTERLEAVED : 0)//
				| ((!morphable && BUFFERS) ? GeometryArray.USE_NIO_BUFFER : 0);
		return vertexFormat;
	}

	protected static void fillIn(GeometryArray ga, NiTriBasedGeomData data, boolean morphable, boolean interleave)
	{
		//Note consistency type in nif file also dictates morphable
		float[] normals = null;
		if (data.hasNormals)
		{
			normals = data.normalsOpt;
		}

		float[] colors4 = null;
		if (data.hasVertexColors)
		{
			colors4 = data.vertexColorsOpt;
		}
		else
		{
			//TODO: do I really need a default white color set on everything?
			/*colors4 = new float[data.numVertices * 4];
			for (int i = 0; i < data.numVertices; i++)
			{
				colors4[i * 4 + 0] = 1;
				colors4[i * 4 + 1] = 1;
				colors4[i * 4 + 2] = 1;
				colors4[i * 4 + 3] = 1;
			}*/
		}

		int texCoordDim = 2;
		float[][] texCoordSets = null;
		// process UVsets hasUV or UVset2?? Num UV Sets 2
		int texCoordSetCount = data.actNumUVSets;

		if (texCoordSetCount > 0)
		{
			texCoordSets = data.uVSetsOpt;
		}

		if (!morphable)
		{
			if (interleave)
			{
				float[] vertexData = J3dNiTriBasedGeom.interleave(texCoordSetCount, texCoordDim, texCoordSets, null, colors4, normals,
						data.verticesOpt);
				if (!BUFFERS)
				{
					ga.setInterleavedVertices(vertexData);
				}
				else
				{
					ga.setInterleavedVertexBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(vertexData)));
				}
			}
			else
			{
				if (!BUFFERS)
				{
					ga.setCoordinates(0, data.verticesOpt);

					if (data.hasNormals)
						ga.setNormals(0, normals);

					if (data.hasVertexColors)
						ga.setColors(0, colors4);

					for (int i = 0; i < texCoordSetCount; i++)
					{
						ga.setTextureCoordinates(i, 0, texCoordSets[i]);
					}
				}
				else
				{
					ga.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(data.verticesOpt)));

					ga.setNormalRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(normals)));
					ga.setColorRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(colors4)));
					for (int i = 0; i < texCoordSetCount; i++)
					{
						ga.setTexCoordRefBuffer(i, new J3DBuffer(Utils3D.makeFloatBuffer(texCoordSets[i])));
					}
				}
			}
		}
		else
		{
			// copy as we are by ref and people will morph these coords later on
			float[] coords = new float[data.verticesOpt.length];
			System.arraycopy(data.verticesOpt, 0, coords, 0, data.verticesOpt.length);
			ga.setCoordRefFloat(coords);

			if (data.hasNormals)
				ga.setNormalRefFloat(normals);

			ga.setColorRefFloat(colors4);

			for (int i = 0; i < texCoordSetCount; i++)
			{
				ga.setTexCoordRefFloat(i, texCoordSets[i]);
			}

			ga.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);
		}

	}

	/** 
	 * Copied basically from
	 * //GeometryInfo.fillIn(GeometryArray ga, boolean byRef, boolean interleaved, boolean nio)
	 * 
	 */
	public static float[] interleave(int texCoordSetCount, int texCoordDim, float[][] texCoordSets, float[] colors3, float[] colors4,
			float[] normals, float[] coordinates)
	{
		// Calculate number of words per vertex
		int wpv = 3; // Always have coordinate data
		if (normals != null)
			wpv += 3;
		if (colors3 != null)
			wpv += 3;
		else if (colors4 != null)
			wpv += 4;
		wpv += (texCoordSetCount * texCoordDim);

		int coordCount = coordinates.length / 3;
		// Build array of interleaved data
		float[] d = new float[wpv * coordCount];

		// Fill in the array
		int offset = 0;
		for (int i = 0; i < coordCount; i++)
		{
			if (texCoordDim == 2)
			{
				for (int j = 0; j < texCoordSetCount; j++)
				{
					d[offset++] = texCoordSets[j][i * 2 + 0];
					d[offset++] = texCoordSets[j][i * 2 + 1];
				}
			}
			else if (texCoordDim == 3)
			{
				for (int j = 0; j < texCoordSetCount; j++)
				{
					d[offset++] = texCoordSets[j][i * 3 + 0];
					d[offset++] = texCoordSets[j][i * 3 + 1];
					d[offset++] = texCoordSets[j][i * 3 + 2];
				}
			}
			else if (texCoordDim == 4)
			{
				for (int j = 0; j < texCoordSetCount; j++)
				{
					d[offset++] = texCoordSets[j][i * 4 + 0];
					d[offset++] = texCoordSets[j][i * 4 + 1];
					d[offset++] = texCoordSets[j][i * 4 + 2];
					d[offset++] = texCoordSets[j][i * 4 + 3];
				}
			}

			if (colors3 != null)
			{
				d[offset++] = colors3[i * 3 + 0];
				d[offset++] = colors3[i * 3 + 1];
				d[offset++] = colors3[i * 3 + 2];
			}
			else if (colors4 != null)
			{
				d[offset++] = colors4[i * 4 + 0];
				d[offset++] = colors4[i * 4 + 1];
				d[offset++] = colors4[i * 4 + 2];
				d[offset++] = colors4[i * 4 + 3];
			}

			if (normals != null)
			{
				d[offset++] = normals[i * 3 + 0];
				d[offset++] = normals[i * 3 + 1];
				d[offset++] = normals[i * 3 + 2];
			}

			d[offset++] = coordinates[i * 3 + 0];
			d[offset++] = coordinates[i * 3 + 1];
			d[offset++] = coordinates[i * 3 + 2];
		}

		return d;
	}
}
