package nif.j3d;

import javax.media.j3d.Appearance;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedQuadArray;
import javax.media.j3d.IndexedTriangleArray;
import javax.media.j3d.J3DBuffer;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;

import nif.basic.NifRef;
import nif.compound.BSVertexData;
import nif.compound.NifMatrix33;
import nif.niobject.bs.BSPackedCombinedSharedGeomDataExtra;
import nif.niobject.bs.BSPackedCombinedSharedGeomDataExtra.Combined;
import nif.niobject.bs.BSPackedCombinedSharedGeomDataExtra.Data;
import nif.niobject.bs.BSTriShape;
import tools3d.utils.Utils3D;
import tools3d.utils.YawPitch;
import tools3d.utils.leafnode.Cube;
import utils.ESConfig;
import utils.convert.ConvertFromNif;
import utils.source.TextureSource;

public class J3dBSTriShape extends J3dNiTriBasedGeom
{

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

		if (bsTriShape.dataSize == 0)
		{
			//  parent rotr as this is the center average so floor has this half way up, which is crazy talk
			// attach the madness below directly to the root of everything
			J3dNiAVObject root = niToJ3dData.getJ3dRoot();
			Appearance app = getShape().getAppearance();

			app.getPolygonAttributes().setCullFace(PolygonAttributes.CULL_NONE);
			app.getTextureUnitState(0).getTextureAttributes().setTextureMode(TextureAttributes.REPLACE);

			Shape3D shape = new Shape3D();
			shape.setAppearance(app);
			root.addChild(shape);

			// find a BSPackedCombinedSharedGeomDataExtra child (always first one?)
			NifRef[] extraDataList = bsTriShape.extraDataList;
			BSPackedCombinedSharedGeomDataExtra packed = (BSPackedCombinedSharedGeomDataExtra) niToJ3dData.get(extraDataList[0]);

			Data[] datas = packed.data;
			//System.out.println("set of data below");
			for (int da = 0; da < datas.length; da++)
			{
				Data data = datas[da];
				//System.out.println("data " + da);

				//System.out.println("set of combined below");
				for (int c = 0; c < data.NumCombined; c++)
				{
					//	System.out.println("combined " + c);
					Combined combined = data.Combined[c];

					// reverse it all!
					NifMatrix33 m = combined.rot;

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
					YawPitch yp = new YawPitch(q2);
					System.out.println("yp = " + yp);

					Quat4f q = ConvertFromNif.toJ3d(m);
					//q= new Quat4f(0,0,0,1);// I think my q is not forming as I would have it form
					// I think I need to round the values off or something like nifskope

					Vector3f t = ConvertFromNif.toJ3d(combined.trans);
					float s = combined.scale;

					Transform3D t4p = new Transform3D(q2, t, s);

					float x = 5.12f;
					float y = 0;
					float z = 5.12f;

					if (Math.abs(packed.fs[da][2]) < 0.01)
					{
						y = 5.12f;
						z = 0;
					}

					Point3f p0 = new Point3f(x, y, z);
					t4p.transform(p0);
					Point3f p1 = new Point3f(0.0f, y, z);
					t4p.transform(p1);
					Point3f p2 = new Point3f(0.0f, 0.0f, 0);
					t4p.transform(p2);
					Point3f p3 = new Point3f(x, 0.0f, 0);
					t4p.transform(p3);

					shape.addGeometry(createQuad(p0, p1, p2, p3));

					TransformGroup tg = new TransformGroup();
					Transform3D t3d = new Transform3D(new Quat4f(0, 0, 0, 1), ConvertFromNif.toJ3d(combined.bounds.center), 1);

					tg.setTransform(t3d);

					tg.addChild(new Cube(0.25, (combined.trans.x / 100) % 1, (combined.trans.y / 100) % 1, (combined.trans.z / 100) % 1));
					root.addChild(tg);

				}
			}

		}
	}

	@Override
	protected IndexedGeometryArray createGeometry(boolean morphable)
	{
		return createGeometry((BSTriShape) this.niAVObject, morphable);
	}

	private static IndexedQuadArray createQuad(Point3f p0, Point3f p1, Point3f p2, Point3f p3)
	{
		//	QuadArray quads = new QuadArray(4, GeometryArray.COORDINATES | GeometryArray.NORMALS | GeometryArray.TEXTURE_COORDINATE_2
		//			| GeometryArray.COLOR_4);

		IndexedQuadArray quads = new IndexedQuadArray(4,
				GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2 | GeometryArray.COLOR_3 | GeometryArray.USE_COORD_INDEX_ONLY,
				4);

		quads.setCoordinate(0, p0);
		quads.setCoordinate(1, p1);
		quads.setCoordinate(2, p2);
		quads.setCoordinate(3, p3);
		/*	quads.setNormal(0, new Vector3f(0f, 1f, 0f));
			quads.setNormal(1, new Vector3f(0f, 1f, 0f));
			quads.setNormal(2, new Vector3f(0f, 1f, 0f));
			quads.setNormal(3, new Vector3f(0f, 1f, 0f));*/
		quads.setTextureCoordinate(0, 0, new TexCoord2f(1f, 1f));
		quads.setTextureCoordinate(0, 1, new TexCoord2f(0f, 1f));
		quads.setTextureCoordinate(0, 2, new TexCoord2f(0f, 0f));
		quads.setTextureCoordinate(0, 3, new TexCoord2f(1f, 0f));
		quads.setColor(0, new Color3f(p0));
		quads.setColor(1, new Color3f(p1));
		quads.setColor(2, new Color3f(p2));
		quads.setColor(3, new Color3f(p3));

		quads.setCoordinateIndices(0, new int[] { 0, 1, 2, 3 });

		return quads;
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
						| (bsTriShape.normalsOpt != null ? GeometryArray.NORMALS : 0) //
						| (bsTriShape.uVSetOpt != null ? GeometryArray.TEXTURE_COORDINATE_2 : 0) //
						| (bsTriShape.colorsOpt != null ? GeometryArray.COLOR_4 : 0) //
						| GeometryArray.USE_COORD_INDEX_ONLY //
						| ((morphable || BUFFERS) ? GeometryArray.BY_REFERENCE_INDICES : 0)//				
						| ((morphable || BUFFERS) ? GeometryArray.BY_REFERENCE : 0)//
						| ((!morphable && BUFFERS) ? GeometryArray.USE_NIO_BUFFER : 0) //
						| ((bsTriShape.normalsOpt != null && bsTriShape.tangentsOpt != null && TANGENTS_BITANGENTS)
								? GeometryArray.VERTEX_ATTRIBUTES : 0);
			}
			else
			{
				//TODO: non optomized version of a format
			}

			IndexedGeometryArray iga;
			if (bsTriShape.normalsOpt != null && bsTriShape.tangentsOpt != null && TANGENTS_BITANGENTS)
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

			fillIn(iga, bsTriShape, morphable);

			if (!morphable)
			{
				sharedIGAs.put(bsTriShape, iga);
			}
			return iga;
		}

		return null;
	}

	private static void fillIn(GeometryArray ga, BSTriShape bsTriShape, boolean morphable)
	{
		BSVertexData[] vertexData = bsTriShape.vertexData;

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
		}

		if (!morphable)
		{
			if (!BUFFERS)
			{
				ga.setCoordinates(0, verticesOpt);

				if (normalsOpt != null)
					ga.setNormals(0, normalsOpt);

				if (vertexColorsOpt != null)
					ga.setColors(0, vertexColorsOpt);

				if (uVSetOpt != null)
				{
					ga.setTextureCoordinates(0, 0, uVSetOpt);
				}

				if (normalsOpt != null && tangentsOpt != null)
				{
					//TODO: here https://www.opengl.org/sdk/docs/tutorials/ClockworkCoders/attributes.php
					// says 6 and 7 are spare, I'm assuming java3d and openlGL sort this out?
					// must test on nvidia hardware
					ga.setVertexAttrs(0, 0, tangentsOpt);
					ga.setVertexAttrs(1, 0, binormalsOpt);
				}
			}
			else
			{
				ga.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(verticesOpt)));

				if (normalsOpt != null)
					ga.setNormalRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(normalsOpt)));

				if (vertexColorsOpt != null)
					ga.setColorRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(vertexColorsOpt)));

				if (uVSetOpt != null)
				{
					ga.setTexCoordRefBuffer(0, new J3DBuffer(Utils3D.makeFloatBuffer(uVSetOpt)));
				}

				if (normalsOpt != null && tangentsOpt != null)
				{
					ga.setVertexAttrRefBuffer(0, new J3DBuffer(Utils3D.makeFloatBuffer(tangentsOpt)));
					ga.setVertexAttrRefBuffer(1, new J3DBuffer(Utils3D.makeFloatBuffer(binormalsOpt)));
				}
			}

		}
		else
		{
			// copy as we are by ref and people will morph these coords later on
			float[] coords = new float[verticesOpt.length];
			System.arraycopy(verticesOpt, 0, coords, 0, verticesOpt.length);
			ga.setCoordRefFloat(coords);

			if (normalsOpt != null)
				ga.setNormalRefFloat(normalsOpt);

			if (vertexColorsOpt != null)
				ga.setColorRefFloat(vertexColorsOpt);

			if (uVSetOpt != null)
			{
				ga.setTexCoordRefFloat(0, uVSetOpt);
			}

			if (normalsOpt != null && tangentsOpt != null)
			{
				ga.setVertexAttrRefFloat(0, tangentsOpt);
				ga.setVertexAttrRefFloat(1, binormalsOpt);
			}

			ga.setCapability(GeometryArray.ALLOW_REF_DATA_READ);
			ga.setCapability(GeometryArray.ALLOW_REF_DATA_WRITE);

		}
	}

}