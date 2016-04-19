package nif.j3d;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
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
import tools3d.utils.SimpleShaderAppearance;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

/**
 * This class has a base geometry and a current to allow skin instances to deform the base
 * @author philip
 */
public abstract class J3dNiTriBasedGeom extends J3dNiGeometry
{

	public static boolean USE_FIXED_BOUNDS = true;

	//TODO: these can no longer be turned on, possibly remove fully?
	public static boolean INTERLEAVE = false;// FALSE if tangents and bitangents on!!!  

	public static boolean STRIPIFY = false; //Relevant to shape only (no advantage with shaders?) doesn't currently work with vert attributes

	public static boolean TANGENTS_BITANGENTS = false;//  shader code auto ons 

	public static boolean BUFFERS = true;

	private int outlineStencilMask = -1;

	private GeometryArray baseGeometryArray;

	protected GeometryArray currentGeometryArray;

	//NOT FOR FO4!!!
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
	 * Note expensive re-create should be optimized one day
	 */
	public void makeMorphable()
	{
		// TODO: is there any gain? certainly a loss from being wrong
		if (USE_FIXED_BOUNDS)
		{
			getShape().setBoundsAutoCompute(false);// expensive to do regularly so animated node just get one
			getShape().setBounds(new BoundingSphere(ConvertFromNif.toJ3dP3d(data.center),
					ConvertFromNif.toJ3d(isMorphable ? data.radius * 2 : data.radius)));
		}
		if (!isMorphable)
		{

			baseGeometryArray = createGeometry(true);
			currentGeometryArray = createGeometry(true);
			getShape().setGeometry(currentGeometryArray);
			isMorphable = true;
		}

		/*		//little test 
				Bounds fromShape = getShape().getBounds();
				BoundingSphere fromFile = new BoundingSphere(ConvertFromNif.toJ3dP3d(data.center),
						ConvertFromNif.toJ3d(isMorphable ? data.radius * 2 : data.radius));
		
				System.out.println("fromShape " + fromShape + " fromFile " + fromFile);*/
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
			outlinerBG1.clearCapabilities();
			outlinerBG1.setCapability(Group.ALLOW_CHILDREN_EXTEND);
			outlinerBG1.setCapability(Group.ALLOW_CHILDREN_WRITE);
			addChild(outlinerBG1);

			// prep the real appearance

			//-Dj3d.stencilClear=true  

			// this is generally working now, below are earlier notes, it doesn't play 
			// badly with transparency once everything has renderingattributes
			// possibly issue 249 https://java.net/jira/browse/JAVA3D-224
			// transparency buggers with stencils https://java.net/jira/browse/JAVA3D-314
			// Notice issues with transparent textures not working well, the "filled in" part must be being run after the line 

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
				outliner.clearCapabilities();
				outliner.setPickable(false);
				outliner.setCollidable(false);
				////////////////////////////////
				//Outliner gear, note empty geom should be ignored
				Appearance app = new SimpleShaderAppearance(c);
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
				outlinerBG2.clearCapabilities();
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
				| (data.hasVertexColors ? GeometryArray.COLOR_4 : 0) //
				| GeometryArray.USE_COORD_INDEX_ONLY //
				| ((morphable || interleave || BUFFERS) ? GeometryArray.BY_REFERENCE_INDICES : 0)//				
				| ((morphable || interleave || BUFFERS) ? GeometryArray.BY_REFERENCE : 0)//
				| ((!morphable && interleave) ? GeometryArray.INTERLEAVED : 0)//
				| (BUFFERS ? GeometryArray.USE_NIO_BUFFER : 0) //
				| ((data.hasNormals && data.tangentsOptBuf != null && TANGENTS_BITANGENTS) ? GeometryArray.VERTEX_ATTRIBUTES : 0);
		return vertexFormat;
	}

	protected static void fillIn(GeometryArray ga, NiTriBasedGeomData data, boolean morphable, boolean interleave)
	{
		//Note consistency type in nif file also dictates morphable

		if (!morphable)
		{
			if (interleave)
			{
				throw new UnsupportedOperationException();
			}
			else
			{
				if (!BUFFERS)
				{
					throw new UnsupportedOperationException();
				}
				else
				{

					ga.setCoordRefBuffer(new J3DBuffer(data.verticesOptBuf));

					if (data.hasNormals)
						ga.setNormalRefBuffer(new J3DBuffer(data.normalsOptBuf));

					if (data.hasVertexColors)
						ga.setColorRefBuffer(new J3DBuffer(data.vertexColorsOptBuf));

					if (data.actNumUVSets > 0)
					{
						//only 0 others ignored
						ga.setTexCoordRefBuffer(0, new J3DBuffer(data.uVSetsOptBuf[0]));
					}

					if (data.hasNormals && data.tangentsOptBuf != null && TANGENTS_BITANGENTS)
					{
						ga.setVertexAttrRefBuffer(0, new J3DBuffer(data.tangentsOptBuf));
						ga.setVertexAttrRefBuffer(1, new J3DBuffer(data.binormalsOptBuf));
					}
				}

			}
		}
		else
		{
			// copy as we are by ref and people will morph these coords later on
			ga.setCoordRefBuffer(new J3DBuffer(Utils3D.cloneFloatBuffer(data.verticesOptBuf)));

			if (data.hasNormals)
				ga.setNormalRefBuffer(new J3DBuffer(data.normalsOptBuf));

			if (data.hasVertexColors)
				ga.setColorRefBuffer(new J3DBuffer(data.vertexColorsOptBuf));

			if (data.actNumUVSets > 0)
			{
				//only 0 others ignored
				ga.setTexCoordRefBuffer(0, new J3DBuffer(data.uVSetsOptBuf[0]));
			}

			if (data.hasNormals && data.tangentsOptBuf != null && TANGENTS_BITANGENTS)
			{
				ga.setVertexAttrRefBuffer(0, new J3DBuffer(data.tangentsOptBuf));
				ga.setVertexAttrRefBuffer(1, new J3DBuffer(data.binormalsOptBuf));
			}

			ga.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);//used to tell pipeline about changablness

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
			// don't lets have 2 threads trying to build the tangents at the same time
			synchronized (data)
			{
				// this can be called many times, only set it up the first time
				if (data.tangentsOptBuf == null)
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
								ByteBuffer stream = ByteBuffer.wrap(niBinaryExtraData.binaryData.data);

								try
								{
									data.loadTangentAndBinormalsFromExtraData(stream, data.nVer);
									//This can only ever be called once then it's finished so we can drop the loaded raw bytes
									niBinaryExtraData.binaryData = null;
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
}
