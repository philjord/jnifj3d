package nif.j3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.IndexedGeometryArray;
import org.jogamp.java3d.IndexedTriangleArray;
import org.jogamp.java3d.J3DBuffer;
import org.jogamp.java3d.JoglesIndexedTriangleArray;
import org.jogamp.java3d.PolygonAttributes;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.TextureAttributes;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.TriangleArray;
import org.jogamp.vecmath.Matrix3f;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3f;

import nif.basic.NifRef;
import nif.compound.NifMatrix33;
import nif.niobject.NiObject;
import nif.niobject.bs.BSPackedCombinedSharedGeomDataExtra;
import nif.niobject.bs.BSPackedCombinedSharedGeomDataExtra.BSPackedGeomData;
import nif.niobject.bs.BSPackedCombinedSharedGeomDataExtra.BSPackedGeomDataCombined;
import nif.niobject.bs.BSTriShape.VertexFormat;
import nif.niobject.bs.BSTriShape;
import tools3d.utils.Utils3D;
import tools3d.utils.leafnode.Cube;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

public class J3dBSTriShape extends J3dNiTriBasedGeom
{

	private static boolean DISPLAY_FAR_QUADS = false;

	/**
	 * Note BSLODMeshTriShape also arrives here
	 * 
	 * @param bsTriShape
	 * @param niToJ3dData
	 * @param textureSource
	 */
	public J3dBSTriShape(BSTriShape bsTriShape, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(bsTriShape, niToJ3dData, textureSource);
		niToJ3dData.put(bsTriShape, this);

		currentGeometryArray = createGeometry(false);
		getShape().setGeometry(currentGeometryArray);
		if (USE_FIXED_BOUNDS)
		{
			getShape().setBoundsAutoCompute(false);// expensive to do regularly so animated node just get one
			getShape().setBounds(new BoundingSphere(ConvertFromNif.toJ3dP3d(bsTriShape.center), ConvertFromNif.toJ3d(bsTriShape.radius)));
		}

		if (bsTriShape.dataSize == 0 && DISPLAY_FAR_QUADS)
		{
			//  parent rotr as this is the center average so floor has this half way up, which is crazy talk
			// attach the madness below directly to the root of everything
			J3dNiAVObject root = niToJ3dData.getJ3dRoot();
			Appearance app = getShape().getAppearance();

			app.getPolygonAttributes().setCullFace(PolygonAttributes.CULL_NONE);
			if (app.getTextureUnitCount() > 0)
				if (app.getTextureUnitState(0).getTextureAttributes() != null)
					app.getTextureUnitState(0).getTextureAttributes().setTextureMode(TextureAttributes.REPLACE);

			Shape3D shape = new Shape3D();
			shape.setAppearance(app);
			root.addChild(shape);

			// find a BSPackedCombinedSharedGeomDataExtra child (always first one?)
			NifRef[] extraDataList = bsTriShape.extraDataList;
			NiObject extraData = niToJ3dData.get(extraDataList[0]);
			//I have also seen BSPositionData
			if (extraData instanceof BSPackedCombinedSharedGeomDataExtra)
			{
				BSPackedCombinedSharedGeomDataExtra packed = (BSPackedCombinedSharedGeomDataExtra) extraData;

				BSPackedGeomData[] datas = packed.BSPackedGeomData;
				//BSPackedGeomObject[] objs = packed.BSPackedGeomObject;
				//System.out.println("set of data below");
				for (int da = 0; da < datas.length; da++)
				{
					BSPackedGeomData data = datas[da];
					//BSPackedGeomObject obj = objs[da];
					//System.out.println("data " + da);

					//System.out.println("set of combined below");
					for (int c = 0; c < data.NumCombined; c++)
					{
						//	System.out.println("combined " + c);
						BSPackedGeomDataCombined combined = data.BSPackedGeomDataCombined[c];

						// reverse it all!
						NifMatrix33 m = combined.Rotation;

						float[] d = m.data();
						m.m33 = d[0];
						m.m23 = d[1];
						m.m13 = d[2];
						m.m32 = d[3];
						m.m22 = d[4];
						m.m12 = d[5];
						m.m31 = d[6];
						m.m21 = d[7];
						m.m11 = d[8];

						Matrix3f m2 = new Matrix3f(m.data());
						//m2.invert();
						Quat4f q2 = new Quat4f();
						q2.set(m2);
						//YawPitch yp = new YawPitch(q2);
						//System.out.println("yp = " + yp);

						//Quat4f q = ConvertFromNif.toJ3d(m);
						//q= new Quat4f(0,0,0,1);// I think my q is not forming as I would have it form
						// I think I need to round the values off or something like nifskope

						Vector3f t = ConvertFromNif.toJ3d(combined.Translation);
						float s = combined.Scale;

						Transform3D t4p = new Transform3D(q2, t, s);

						float radius = combined.BoundingSphere.radius;
						float radiusAsHalfSquareSize = (float) Math.sqrt((radius * radius) / 2f);

						float x = radiusAsHalfSquareSize;
						float y = 0;
						float z = radiusAsHalfSquareSize;

						Point3f p0 = new Point3f(-x, -y, -z);
						t4p.transform(p0);
						Point3f p1 = new Point3f(x, y, z);
						t4p.transform(p1);

						shape.addGeometry(createQuad(p0, p1));

						TransformGroup tg = new TransformGroup();
						Transform3D t3d = new Transform3D(new Quat4f(0, 0, 0, 1), ConvertFromNif.toJ3d(combined.BoundingSphere.center), 1);

						tg.setTransform(t3d);

						tg.addChild(new Cube(0.25, (combined.Translation.x / 100) % 1, (combined.Translation.y / 100) % 1,
								(combined.Translation.z / 100) % 1));
						root.addChild(tg);

					}
				}
			}

		}
	}

	@Override
	protected IndexedGeometryArray createGeometry(boolean morphable)
	{
		return createGeometry((BSTriShape) this.niAVObject, morphable);
	}

	private static TriangleArray createQuad(Point3f min, Point3f max)
	{
		float[] verts1 = { min.x, min.y, min.z, //1
				max.x, min.y, min.z, //2
				max.x, max.y, min.z, //3
				min.x, min.y, min.z, //1
				max.x, max.y, min.z, //3
				min.x, max.y, min.z//4
		};//4

		float[] texCoords = { 0f, 1f, //1
				0f, 0f, //2
				(1f), 0f, //3
				0f, 1f, //1
				(1f), 0f, //3
				(1f), 1f//4

		};

		//probably should add normals too for speed?otherwise auto generated or something
		float[] normals = { 0f, 0f, 1f, //1
				0f, 0f, 1f, //2
				0f, 0f, 1f, //3
				0f, 0f, 1f, //1
				0f, 0f, 1f, //3
				0f, 0f, 1f, //4

		};

		TriangleArray rect = new TriangleArray(6, GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2 | GeometryArray.NORMALS
				| GeometryArray.USE_NIO_BUFFER | GeometryArray.BY_REFERENCE);

		rect.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(verts1)));
		rect.setNormalRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(normals)));
		rect.setTexCoordRefBuffer(0, new J3DBuffer(Utils3D.makeFloatBuffer(texCoords)));

		return rect;
	}

	//also used by Fo4LODLandscape
	public static IndexedGeometryArray createGeometry(BSTriShape bsTriShape, boolean morphable)
	{
		if (!morphable)
		{
			IndexedGeometryArray iga = sharedIGAs.get(bsTriShape);

			if (iga != null)
			{
				return iga;
			}
		}

		if (morphable && bsTriShape.verticesOptBuf == null)
		{
			// we have laoded optimized but someone wants to morph us now
			// example size 4 btr lod landscape files Commonwealth.4.0.0.BTR
			// extract back out of interleaved!
			//TODO: madness!
			return null;
		}

		if (bsTriShape.dataSize > 0)
		{
			// All tex units use the 0ith , all others are ignored
			int[] texMap = new int[9];
			for (int i = 0; i < 9; i++)
				texMap[i] = 0;

			int vertexFormat = 0;
			if (BSTriShape.LOAD_OPTIMIZED)
			{
				vertexFormat = GeometryArray.COORDINATES //
						| (bsTriShape.normalsOptBuf != null || bsTriShape.geoToNormalsOffset != -1 ? GeometryArray.NORMALS : 0) //
						| (bsTriShape.uVSetOptBuf != null || bsTriShape.geoToTexCoordOffset != -1 ? GeometryArray.TEXTURE_COORDINATE_2 : 0) //
						| (bsTriShape.colorsOptBuf != null || bsTriShape.geoToColorsOffset != -1 ? GeometryArray.COLOR_4 : 0) //
						| GeometryArray.USE_COORD_INDEX_ONLY //
						| ((morphable || BUFFERS) ? GeometryArray.BY_REFERENCE_INDICES : 0)//				
						| ((morphable || BUFFERS) ? GeometryArray.BY_REFERENCE : 0)//
						| ((BUFFERS) ? GeometryArray.USE_NIO_BUFFER : 0) //
						| (((bsTriShape.normalsOptBuf != null && bsTriShape.tangentsOptBuf != null)
								|| (bsTriShape.geoToNormalsOffset != -1 && bsTriShape.geoToTanOffset != -1) && TANGENTS_BITANGENTS)
										? GeometryArray.VERTEX_ATTRIBUTES : 0);
			}
			else
			{
				throw new UnsupportedOperationException();
			}

			IndexedGeometryArray iga = null;
			if (BSTriShape.LOAD_MEGA_OPTIMIZED && !bsTriShape.vertexFormat.isSet(VertexFormat.VF_Skinned)
					&& !bsTriShape.vertexFormat.isSet(VertexFormat.VF_Male_Eyes))
			{
				JoglesIndexedTriangleArray ita;
				if ((bsTriShape.normalsOptBuf != null && bsTriShape.tangentsOptBuf != null)
						|| (bsTriShape.geoToNormalsOffset != -1 && bsTriShape.geoToTanOffset != -1) && TANGENTS_BITANGENTS)
				{
					ita = new JoglesIndexedTriangleArray(bsTriShape.numVertices, vertexFormat, 1, texMap, 2, new int[] { 3, 3 },
							bsTriShape.numTriangles * 3);
				}
				else
				{
					ita = new JoglesIndexedTriangleArray(bsTriShape.numVertices, vertexFormat, 1, texMap, bsTriShape.numTriangles * 3);
				}

				ByteBuffer bb = ByteBuffer.allocateDirect(bsTriShape.trianglesOpt.length * 2);
				bb.order(ByteOrder.nativeOrder());
				ShortBuffer indBuf = bb.asShortBuffer();
				for (int s = 0; s < bsTriShape.trianglesOpt.length; s++)
					indBuf.put(s, (short) bsTriShape.trianglesOpt[s]);
				indBuf.position(0);

				ita.setCoordIndicesRefBuffer(indBuf);
				iga = ita;
			}
			else
			{
				if (bsTriShape.normalsOptBuf != null && bsTriShape.tangentsOptBuf != null && TANGENTS_BITANGENTS)
				{
					iga = new IndexedTriangleArray(bsTriShape.numVertices, vertexFormat, 1, texMap, 2, new int[] { 3, 3 },
							bsTriShape.numTriangles * 3);
				}
				else
				{
					iga = new IndexedTriangleArray(bsTriShape.numVertices, vertexFormat, 1, texMap, bsTriShape.numTriangles * 3);
				}

				if (morphable || BUFFERS)
					iga.setCoordIndicesRef(bsTriShape.trianglesOpt);
				else
					iga.setCoordinateIndices(0, bsTriShape.trianglesOpt);
			}

			fillIn(iga, bsTriShape, morphable);

			if (!morphable)
			{
				sharedIGAs.put(bsTriShape, iga);
			}
			return iga;
		}

		return null;
	}

	private static void fillIn(GeometryArray ga, BSTriShape data, boolean morphable)
	{
		/*BSVertexData[] vertexData = data.vertexData;
		
		float[] verticesOpt = null;
		if (BSTriShape.LOAD_OPTIMIZED)
		{
			verticesOpt = bsTriShape.verticesOpt;
		}
		else
		{
			verticesOpt = new float[bsTriShape.numVertices * 3];
			for (int i = 0; i < bsTriShape.numVertices; i++)
			{
				verticesOpt[i * 3 + 0] = vertexData[i].vertex.x * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 2] = -vertexData[i].vertex.y * ESConfig.ES_TO_METERS_SCALE;
				verticesOpt[i * 3 + 1] = vertexData[i].vertex.z * ESConfig.ES_TO_METERS_SCALE;
			}
		}
		
		float[] uVSetOpt = null;
		if (BSTriShape.LOAD_OPTIMIZED)
		{
			if (bsTriShape.uVSetOpt != null)
			{
				uVSetOpt = bsTriShape.uVSetOpt;
			}
		}
		else
		{
			if (vertexData[0].texCoord != null)
			{
				uVSetOpt = new float[bsTriShape.numVertices * 2];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					uVSetOpt[i * 2 + 0] = vertexData[i].texCoord.u;
					uVSetOpt[i * 2 + 1] = vertexData[i].texCoord.v;
				}
			}
		}
		
		float[] normalsOpt = null;
		
		if (BSTriShape.LOAD_OPTIMIZED)
		{
			if (bsTriShape.normalsOpt != null)
			{
				normalsOpt = bsTriShape.normalsOpt;
		
			}
		}
		else
		{
			if (vertexData[0].normal != null)
			{
				normalsOpt = new float[bsTriShape.numVertices * 3];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					normalsOpt[i * 3 + 0] = vertexData[i].normal.x;
					normalsOpt[i * 3 + 2] = -vertexData[i].normal.y;
					normalsOpt[i * 3 + 1] = vertexData[i].normal.z;
				}
			}
		}
		
		float[] tangentsOpt = null;
		float[] binormalsOpt = null;
		if (TANGENTS_BITANGENTS)
		{
			if (BSTriShape.LOAD_OPTIMIZED)
			{
				if (bsTriShape.normalsOpt != null)
				{
					tangentsOpt = bsTriShape.tangentsOpt;
					binormalsOpt = bsTriShape.binormalsOpt;
				}
			}
			else
			{
				if (vertexData[0].tangent != null)
				{
					tangentsOpt = new float[bsTriShape.numVertices * 3];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						tangentsOpt[i * 3 + 0] = vertexData[i].tangent.x;
						tangentsOpt[i * 3 + 2] = -vertexData[i].tangent.y;
						tangentsOpt[i * 3 + 1] = vertexData[i].tangent.z;
					}
					binormalsOpt = new float[bsTriShape.numVertices * 3];
					for (int i = 0; i < bsTriShape.numVertices; i++)
					{
						binormalsOpt[i * 3 + 0] = vertexData[i].bitangentX;
						binormalsOpt[i * 3 + 2] = -vertexData[i].bitangentY;
						binormalsOpt[i * 3 + 1] = vertexData[i].bitangentZ;
					}
				}
			}
		}
		
		float[] vertexColorsOpt = null;
		if (BSTriShape.LOAD_OPTIMIZED)
		{
			if (bsTriShape.colorsOpt != null)
			{
				vertexColorsOpt = bsTriShape.colorsOpt;
			}
		}
		else
		{
			if (vertexData[0].color != null)
			{
				vertexColorsOpt = new float[bsTriShape.numVertices * 4];
				for (int i = 0; i < bsTriShape.numVertices; i++)
				{
					vertexColorsOpt[i * 4 + 0] = vertexData[i].color.r;
					vertexColorsOpt[i * 4 + 1] = vertexData[i].color.g;
					vertexColorsOpt[i * 4 + 2] = vertexData[i].color.b;
					vertexColorsOpt[i * 4 + 3] = vertexData[i].color.a;
				}
			}
		}*/

		if (!morphable)
		{
			if (BSTriShape.LOAD_MEGA_OPTIMIZED && !data.vertexFormat.isSet(VertexFormat.VF_Skinned)
					&& !data.vertexFormat.isSet(VertexFormat.VF_Male_Eyes))
			{
				int[] geoToVattrOffset = new int[] { data.geoToTanOffset, data.geoToBiTanOffset };

				data.interleavedBuffer.position(0);

				JoglesIndexedTriangleArray gd = (JoglesIndexedTriangleArray) ga;
				gd.setInterleavedVertexBuffer(data.interleavedStride, data.geoToCoordOffset, data.geoToColorsOffset,
						data.geoToNormalsOffset, new int[] { data.geoToTexCoordOffset }, geoToVattrOffset, data.interleavedBuffer, null);

			}
			else
			{

				ga.setCoordRefBuffer(new J3DBuffer(data.verticesOptBuf));

				if (data.normalsOptBuf != null)
					ga.setNormalRefBuffer(new J3DBuffer(data.normalsOptBuf));

				if (data.colorsOptBuf != null)
					ga.setColorRefBuffer(new J3DBuffer(data.colorsOptBuf));

				if (data.uVSetOptBuf != null)
				{
					ga.setTexCoordRefBuffer(0, new J3DBuffer(data.uVSetOptBuf));
				}

				if (data.normalsOptBuf != null && data.tangentsOptBuf != null)
				{
					ga.setVertexAttrRefBuffer(0, new J3DBuffer(data.tangentsOptBuf));
					ga.setVertexAttrRefBuffer(1, new J3DBuffer(data.binormalsOptBuf));
				}
			}

		}
		else
		{

			// copy as we are by ref and people will morph these coords later on
			ga.setCoordRefBuffer(new J3DBuffer(Utils3D.cloneFloatBuffer(data.verticesOptBuf)));

			if (data.normalsOptBuf != null)
				ga.setNormalRefBuffer(new J3DBuffer(data.normalsOptBuf));

			if (data.colorsOptBuf != null)
				ga.setColorRefBuffer(new J3DBuffer(data.colorsOptBuf));

			if (data.uVSetOptBuf != null)
			{
				ga.setTexCoordRefBuffer(0, new J3DBuffer(data.uVSetOptBuf));
			}

			if (data.normalsOptBuf != null && data.tangentsOptBuf != null)
			{
				ga.setVertexAttrRefBuffer(0, new J3DBuffer(data.tangentsOptBuf));
				ga.setVertexAttrRefBuffer(1, new J3DBuffer(data.binormalsOptBuf));
			}

			ga.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

		}
	}

}