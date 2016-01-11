package nif.j3d;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.J3DBuffer;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.vecmath.Color3f;

import nif.NifVer;
import nif.basic.NifRef;
import nif.niobject.NiBinaryExtraData;
import nif.niobject.NiObject;
import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriBasedGeomData;
import tools.WeakValueHashMap;
import tools3d.utils.Utils3D;
import utils.source.TextureSource;

/**
 * This class has a base geometry and a current to allow skin instances to deform the base
 * @author philip
 * assa
 */
public abstract class J3dNiTriBasedGeom extends J3dNiGeometry
{
	//morrowind wants true false false
	//oblivion wants true false false
	//skyrim wants true false false

	public static boolean TANGENTS_BITANGENTS = false;//  shader code auto ons 

	public static boolean INTERLEAVE = false;// FALSE if tangents and bitangents on!!!  

	public static boolean STRIPIFY = false; //Relevant to shape only (no advantage with shaders?) doesn't currently work with vert attributes

	public static boolean BUFFERS = true;

	private int outlineStencilMask = -1;

	private GeometryArray baseGeometryArray;

	protected GeometryArray currentGeometryArray;

	protected NiTriBasedGeomData data;

	private boolean isMorphable = false;

	private Shape3D outliner = null;

	private BranchGroup outlinerBG1 = null;

	private BranchGroup outlinerBG2 = null;

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
			// TODO: is there any gain? certainly a loss from being wrong
			//getShape().setBoundsAutoCompute(false);// expensive to do regularly so animated node just get one
			//getShape().setBounds(new BoundingSphere(ConvertFromNif.toJ3dP3d(data.center), ConvertFromNif.toJ3d(data.radius)));

			baseGeometryArray = createGeometry(true);
			currentGeometryArray = createGeometry(true);
			getShape().setGeometry(currentGeometryArray);
			isMorphable = true;
		}
	}

	@Override
	/**
	 * If you will ever call this it must be done at least once before compile time
	 * and it must be called with a color to set a stencil mask (but can be called again with null)
	 * null color disables outlines
	 * @see tools3d.utils.scenegraph.Fadable#setOutline(javax.vecmath.Color3f)
	 */
	public void setOutline(Color3f c)
	{
		// must be called before compile time
		if (outlinerBG1 == null)
		{
			// prepare a root for outline to be added to
			outlinerBG1 = new BranchGroup();
			outlinerBG1.setCapability(Group.ALLOW_CHILDREN_EXTEND);
			outlinerBG1.setCapability(Group.ALLOW_CHILDREN_WRITE);
			addChild(outlinerBG1);

			// prep the real appearance

			//-Dj3d.stencilClear=true  

			// this is generally working now, below are earlier notes, it doesn't play 
			// badly with transparency once everything has renderingattributes
			//possibly issue 249 https://java.net/jira/browse/JAVA3D-224
			// transparency buggers with stencils https://java.net/jira/browse/JAVA3D-314
			//Notice issues with transparent textures not working well, the "filled in" part must be being run after the line 

			Appearance sapp = getShape().getAppearance();

			RenderingAttributes ra1 = sapp.getRenderingAttributes();
			//RAISE_BUG:
			// note ra1 must not be null ever, for stencils all apps should have ras

			outlineStencilMask = (int) (c.x * 255) + (int) (c.y * 255) + (int) (c.z * 255);

			ra1.setStencilEnable(true);
			ra1.setStencilWriteMask(outlineStencilMask);
			ra1.setStencilFunction(RenderingAttributes.ALWAYS, outlineStencilMask, outlineStencilMask);
			ra1.setStencilOp(RenderingAttributes.STENCIL_REPLACE, //
					RenderingAttributes.STENCIL_REPLACE, //
					RenderingAttributes.STENCIL_REPLACE);

			sapp.setRenderingAttributes(ra1);
		}

		if (c == null)
		{
			if (outliner != null)
			{
				outlinerBG2.detach();
				outlinerBG2 = null;
				outliner = null;
			}
		}
		else
		{
			if (outliner == null)
			{
				outliner = new Shape3D();

				////////////////////////////////
				//Outliner gear, note empty geom should be ignored
				Appearance app = new Appearance();
				// lineAntialiasing MUST be true, to force this to be done during rendering pass (otherwise it's hidden)
				LineAttributes la = new LineAttributes(4, LineAttributes.PATTERN_SOLID, true);
				app.setLineAttributes(la);
				PolygonAttributes pa = new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_BACK, 0.0f, true, 0.0f);
				app.setPolygonAttributes(pa);
				ColoringAttributes colorAtt = new ColoringAttributes(c, ColoringAttributes.FASTEST);
				app.setColoringAttributes(colorAtt);

				RenderingAttributes ra2 = new RenderingAttributes();
				ra2.setStencilEnable(true);
				ra2.setStencilWriteMask(outlineStencilMask);
				ra2.setStencilFunction(RenderingAttributes.NOT_EQUAL, outlineStencilMask, outlineStencilMask);
				ra2.setStencilOp(RenderingAttributes.STENCIL_KEEP, //
						RenderingAttributes.STENCIL_KEEP, //
						RenderingAttributes.STENCIL_KEEP);

				//geoms often have colors in verts
				ra2.setIgnoreVertexColors(true);

				// draw it even when hidden
				ra2.setDepthBufferEnable(false);
				ra2.setDepthTestFunction(RenderingAttributes.ALWAYS);

				app.setRenderingAttributes(ra2);

				outliner.setAppearance(app);
				outliner.setGeometry(currentGeometryArray);

				outliner.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
				app.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
				colorAtt.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);

				outlinerBG2 = new BranchGroup();
				outlinerBG2.setCapability(BranchGroup.ALLOW_DETACH);
				outlinerBG2.addChild(outliner);
				outlinerBG1.addChild(outlinerBG2);

			}
			else
			{
				outliner.getAppearance().getColoringAttributes().setColor(c);
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
		//TODO: this is using the optomized arrays so will fail for normal
		int vertexFormat = (data.hasVertices ? GeometryArray.COORDINATES : 0) //
				| (data.hasNormals ? GeometryArray.NORMALS : 0) //
				| (data.actNumUVSets > 0 ? GeometryArray.TEXTURE_COORDINATE_2 : 0) //
				| (data.vertexColorsOpt != null ? GeometryArray.COLOR_4 : 0) //
				| GeometryArray.USE_COORD_INDEX_ONLY //
				| ((morphable || interleave || BUFFERS) ? GeometryArray.BY_REFERENCE_INDICES : 0)//				
				| ((morphable || interleave || BUFFERS) ? GeometryArray.BY_REFERENCE : 0)//
				| ((!morphable && interleave) ? GeometryArray.INTERLEAVED : 0)//
				| ((!morphable && BUFFERS) ? GeometryArray.USE_NIO_BUFFER : 0) //
				| ((data.hasNormals && data.tangentsOpt != null && TANGENTS_BITANGENTS) ? GeometryArray.VERTEX_ATTRIBUTES : 0);
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

		int texCoordDim = 2;
		float[][] texCoordSets = null;

		if (data.actNumUVSets > 0)
		{
			texCoordSets = new float[1][];
			texCoordSets[0] = data.uVSetsOpt[0];//others ignored
		}

		if (!morphable)
		{
			if (interleave)
			{
				float[] vertexData = J3dNiTriBasedGeom.interleave(texCoordDim, texCoordSets, null, colors4, normals, data.verticesOpt);
				if (!BUFFERS)
				{
					ga.setInterleavedVertices(vertexData);
					if (data.hasNormals && data.tangentsOpt != null && TANGENTS_BITANGENTS)
					{

						ga.setVertexAttrs(0, 0, data.tangentsOpt);
						ga.setVertexAttrs(1, 0, data.binormalsOpt);
					}
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

					if (texCoordSets != null)
					{
						for (int i = 0; i < texCoordSets.length; i++)
							ga.setTextureCoordinates(i, 0, texCoordSets[i]);
					}

					if (data.hasNormals && data.tangentsOpt != null && TANGENTS_BITANGENTS)
					{
						//TODO: here https://www.opengl.org/sdk/docs/tutorials/ClockworkCoders/attributes.php
						// says 6 and 7 are spare, I'm assuming java3d and openlGL sort this out?
						// must test on nvidia hardware
						ga.setVertexAttrs(0, 0, data.tangentsOpt);
						ga.setVertexAttrs(1, 0, data.binormalsOpt);
					}
				}
				else
				{

					ga.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(data.verticesOpt)));

					if (data.hasNormals)
						ga.setNormalRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(normals)));

					if (data.hasVertexColors)
						ga.setColorRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(colors4)));

					if (texCoordSets != null)
					{
						for (int i = 0; i < texCoordSets.length; i++)
						{
							ga.setTexCoordRefBuffer(i, new J3DBuffer(Utils3D.makeFloatBuffer(texCoordSets[i])));
						}
					}

					if (data.hasNormals && data.tangentsOpt != null && TANGENTS_BITANGENTS)
					{
						ga.setVertexAttrRefBuffer(0, new J3DBuffer(Utils3D.makeFloatBuffer(data.tangentsOpt)));
						ga.setVertexAttrRefBuffer(1, new J3DBuffer(Utils3D.makeFloatBuffer(data.binormalsOpt)));
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

			if (texCoordSets != null)
			{
				for (int i = 0; i < texCoordSets.length; i++)
				{
					ga.setTexCoordRefFloat(i, texCoordSets[i]);
				}
			}

			if (data.hasNormals && data.tangentsOpt != null && TANGENTS_BITANGENTS)
			{
				ga.setVertexAttrRefFloat(0, data.tangentsOpt);
				ga.setVertexAttrRefFloat(1, data.binormalsOpt);
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
	public static float[] interleave(int texCoordDim, float[][] texCoordSets, float[] colors3, float[] colors4, float[] normals,
			float[] coordinates)
	{
		// Calculate number of words per vertex
		int wpv = 3; // Always have coordinate data
		if (normals != null)
			wpv += 3;
		if (colors3 != null)
			wpv += 3;
		else if (colors4 != null)
			wpv += 4;

		if (texCoordSets != null)
			wpv += (texCoordSets.length * texCoordDim);

		int coordCount = coordinates.length / 3;
		// Build array of interleaved data
		float[] d = new float[wpv * coordCount];

		// Fill in the array
		int offset = 0;
		for (int i = 0; i < coordCount; i++)
		{
			if (texCoordSets != null)
			{
				if (texCoordDim == 2)
				{
					for (int j = 0; j < texCoordSets.length; j++)
					{
						d[offset++] = texCoordSets[j][i * 2 + 0];
						d[offset++] = texCoordSets[j][i * 2 + 1];
					}
				}
				else if (texCoordDim == 3)
				{
					for (int j = 0; j < texCoordSets.length; j++)
					{
						d[offset++] = texCoordSets[j][i * 3 + 0];
						d[offset++] = texCoordSets[j][i * 3 + 1];
						d[offset++] = texCoordSets[j][i * 3 + 2];
					}
				}
				else if (texCoordDim == 4)
				{
					for (int j = 0; j < texCoordSets.length; j++)
					{
						d[offset++] = texCoordSets[j][i * 4 + 0];
						d[offset++] = texCoordSets[j][i * 4 + 1];
						d[offset++] = texCoordSets[j][i * 4 + 2];
						d[offset++] = texCoordSets[j][i * 4 + 3];
					}
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

	public static void mergeOblivionTanBiExtraData(NiTriBasedGeom niTriBasedGeom, NiToJ3dData niToJ3dData)
	{
		boolean isOblivion = ((niTriBasedGeom.nVer.LOAD_VER == NifVer.VER_20_0_0_4 || niTriBasedGeom.nVer.LOAD_VER == NifVer.VER_20_0_0_5)
				&& (niTriBasedGeom.nVer.LOAD_USER_VER == 11));
		if (isOblivion)
		{
			NiTriBasedGeomData data = (NiTriBasedGeomData) niToJ3dData.get(niTriBasedGeom.data);
			// this can be called many times, only set it up the first time
			if (data.tangentsOpt == null)
			{
				NifRef[] properties = niTriBasedGeom.extraDataList;

				for (int i = 0; i < properties.length; i++)
				{
					NiObject prop = niToJ3dData.get(properties[i]);
					if (prop != null && prop instanceof NiBinaryExtraData)
					{
						NiBinaryExtraData niBinaryExtraData = (NiBinaryExtraData) prop;
						if (niBinaryExtraData.name.equals("Tangent space (binormal & tangent vectors)"))
						{
							ByteArrayInputStream stream = new ByteArrayInputStream(niBinaryExtraData.binaryData.data);

							try
							{
								data.loadTangentAndBinormalsFromExtraData(stream, data.nVer);
							}
							catch (IOException e)
							{
								e.printStackTrace();
							}
						}
					}
				}
			}

		}

	}
}
