package nif.j3d;

import java.util.Vector;

import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.J3DBuffer;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.TriangleArray;
import org.jogamp.java3d.TriangleStripArray;
import org.jogamp.java3d.geom.CylinderGenerator;
import org.jogamp.java3d.geom.GeometryData;
import org.jogamp.java3d.geom.SphereGenerator;
import org.jogamp.java3d.utils.geometry.GeometryInfo;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Matrix4f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Quat4f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.collision.shapes.ShapeHull;
import com.bulletphysics.util.IntArrayList;
import com.bulletphysics.util.ObjectArrayList;

import nif.NifVer;
import nif.compound.NifSphereBV;
import nif.compound.NifbhkCMSDChunk;
import nif.compound.NifbhkCMSDTransform;
import nif.niobject.NiAVObject;
import nif.niobject.NiObject;
import nif.niobject.NiTriStripsData;
import nif.niobject.bhk.bhkBoxShape;
import nif.niobject.bhk.bhkCapsuleShape;
import nif.niobject.bhk.bhkCollisionObject;
import nif.niobject.bhk.bhkCompressedMeshShape;
import nif.niobject.bhk.bhkCompressedMeshShapeData;
import nif.niobject.bhk.bhkConvexListShape;
import nif.niobject.bhk.bhkConvexVerticesShape;
import nif.niobject.bhk.bhkListShape;
import nif.niobject.bhk.bhkMoppBvTreeShape;
import nif.niobject.bhk.bhkMultiSphereShape;
import nif.niobject.bhk.bhkNiTriStripsShape;
import nif.niobject.bhk.bhkPackedNiTriStripsShape;
import nif.niobject.bhk.bhkRigidBody;
import nif.niobject.bhk.bhkRigidBodyT;
import nif.niobject.bhk.bhkShape;
import nif.niobject.bhk.bhkSphereShape;
import nif.niobject.bhk.bhkTransformShape;
import nif.niobject.bhk.hkPackedNiTriStripsData;
import tools3d.utils.PhysAppearance;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromHavok;

/**
 * NOTE for trival render only now, bullet does teh hard work!
 * normals ignored as slow and cause huge indexify operations due to diff indeices counts
 * @author philip
 *
 */
public class J3dbhkCollisionObject extends Group
{
	private static final boolean COMPACT = false;
	private static final boolean BY_REF = true;
	private static final boolean INTERLEAVED = false;
	private static final boolean NIO = true;

	private static final int defaultFormat = GeometryArray.COORDINATES | GeometryArray.BY_REFERENCE | GeometryArray.USE_NIO_BUFFER;

	//TODO: many more JBullet style conversions
	public J3dbhkCollisionObject(bhkCollisionObject object, NiToJ3dData niToJ3dData)
	{
		if (object.body.ref != -1)
		{
			if (niToJ3dData.get(object.body) instanceof bhkRigidBody)
			{
				bhkRigidBody bhkRigidBody = (bhkRigidBody) niToJ3dData.get(object.body);
				bhkShape bhkShape = (bhkShape) niToJ3dData.get(bhkRigidBody.shape);

				Group lowerGroup;

				if (bhkRigidBody instanceof bhkRigidBodyT)
				{
					Transform3D t = new Transform3D();

					t.setRotation(ConvertFromHavok.toJ3d(bhkRigidBody.rotation));

					t.setTranslation(ConvertFromHavok.toJ3d(bhkRigidBody.translation, niToJ3dData.nifVer));

					lowerGroup = new TransformGroup(t);
				}
				else
				{
					lowerGroup = new Group();
				}

				processBhkShape(bhkShape, lowerGroup, niToJ3dData);

				NiObject parent = niToJ3dData.get(object.target);

				J3dNiAVObject j3dParent = niToJ3dData.get((NiAVObject) parent);

				// in one file I've found one instance with no parent
				if (j3dParent != null)
				{
					//lowerGroup.setPickable(true);
					lowerGroup.setName("J3dbhkCollisionObject child of " + j3dParent.getName());

					addChild(lowerGroup);
					j3dParent.addChild(this);
				}
				else
				{
					System.out.println("No parent for J3dbhkCollisionObject, oh well");
				}
			}
			else
			{
				if ((object.nVer.LOAD_VER >= NifVer.VER_20_2_0_7 && object.nVer.LOAD_USER_VER == 12 && object.nVer.LOAD_USER_VER2 == 130))
				{
					//TODO: for FO4 physics
					//System.out.println("FO4 skipping child type NiParticleSystem - for now");
				}
				else
				{
					System.out
							.println("J3dbhkCollisionObject - bhkCollisionObject.body is not bhkRigidBody " + niToJ3dData.get(object.body));
				}
			}
		}
	}

	private static void processBhkShape(bhkShape bhkShape, Group group, NiToJ3dData niToJ3dData)
	{
		if (bhkShape instanceof bhkListShape)
		{
			bhkListShape bhkListShape = (bhkListShape) bhkShape;
			for (int i = 0; i < bhkListShape.numSubShapes; i++)
			{
				processBhkShape((bhkShape) niToJ3dData.get(bhkListShape.subShapes[i]), group, niToJ3dData);
			}
		}
		else if (bhkShape instanceof bhkNiTriStripsShape)
		{
			bhkNiTriStripsShape((bhkNiTriStripsShape) bhkShape, group, niToJ3dData);
		}
		else if (bhkShape instanceof bhkPackedNiTriStripsShape)
		{
			bhkPackedNiTriStripsShape bhkPackedNiTriStripsShape = (bhkPackedNiTriStripsShape) bhkShape;

			if (bhkPackedNiTriStripsShape.data.ref != -1)
			{
				hkPackedNiTriStripsData hkPackedNiTriStripsData = (hkPackedNiTriStripsData) niToJ3dData.get(bhkPackedNiTriStripsShape.data);
				group.addChild(hkPackedNiTriStripsData(hkPackedNiTriStripsData, niToJ3dData.nifVer));
			}
		}
		else if (bhkShape instanceof hkPackedNiTriStripsData)
		{
			hkPackedNiTriStripsData hkPackedNiTriStripsData = (hkPackedNiTriStripsData) bhkShape;
			group.addChild(hkPackedNiTriStripsData(hkPackedNiTriStripsData, niToJ3dData.nifVer));
		}
		else if (bhkShape instanceof bhkBoxShape)
		{
			group.addChild(bhkBoxShape((bhkBoxShape) bhkShape, niToJ3dData.nifVer));
		}
		else if (bhkShape instanceof bhkCapsuleShape)
		{
			group.addChild(bhkCapsuleShape((bhkCapsuleShape) bhkShape, niToJ3dData.nifVer));
		}
		else if (bhkShape instanceof bhkSphereShape)
		{
			group.addChild(bhkSphereShape((bhkSphereShape) bhkShape, niToJ3dData.nifVer));
		}
		else if (bhkShape instanceof bhkConvexVerticesShape)
		{
			group.addChild(bhkConvexVerticesShape((bhkConvexVerticesShape) bhkShape, niToJ3dData.nifVer));
		}
		else if (bhkShape instanceof bhkMultiSphereShape)
		{
			group.addChild(bhkMultiSphereShape((bhkMultiSphereShape) bhkShape, niToJ3dData.nifVer));
		}
		else if (bhkShape instanceof bhkMoppBvTreeShape)
		{
			bhkMoppBvTreeShape((bhkMoppBvTreeShape) bhkShape, group, niToJ3dData);
		}
		else if (bhkShape instanceof bhkTransformShape)
		{
			bhkTransformShape((bhkTransformShape) bhkShape, group, niToJ3dData);
		}
		else if (bhkShape instanceof bhkConvexListShape)
		{
			//	bhkConvexListShape((bhkConvexListShape) bhkShape, group, blocks);
			//TODO: bhkConvexListShape
		}
		else if (bhkShape instanceof bhkCompressedMeshShape)
		{
			bhkCompressedMeshShape bhkCompressedMeshShape = (bhkCompressedMeshShape) bhkShape;

			if (bhkCompressedMeshShape.data.ref != -1)
			{
				bhkCompressedMeshShapeData bhkCompressedMeshShapeData = (bhkCompressedMeshShapeData) niToJ3dData
						.get(bhkCompressedMeshShape.data);
				group.addChild(bhkCompressedMeshShape(bhkCompressedMeshShapeData, niToJ3dData.nifVer));
			}
		}
		else
		{
			System.out.println("J3dbhkCollisionObject - unknown bhkShape " + bhkShape);
		}

	}

	private static void bhkNiTriStripsShape(bhkNiTriStripsShape data, Group g, NiToJ3dData niToJ3dData)
	{
		for (int i = 0; i < data.numStripsData; i++)
		{
			NiTriStripsData niTriStripsData = (NiTriStripsData) niToJ3dData.get(data.stripsData[i]);
			g.addChild(processNiTriStripsData(niTriStripsData));
		}
	}

	private static void bhkMoppBvTreeShape(bhkMoppBvTreeShape data, Group g, NiToJ3dData niToJ3dData)
	{
		if (data.shape.ref != -1)
		{
			bhkShape bhkShape = (bhkShape) niToJ3dData.get(data.shape);
			processBhkShape(bhkShape, g, niToJ3dData);
		}
	}

	private static void bhkTransformShape(bhkTransformShape data, Group g, NiToJ3dData niToJ3dData)
	{
		TransformGroup transformGroup = new TransformGroup();
		Transform3D transform = new Transform3D();

		Matrix4f m = ConvertFromHavok.toJ3dM4(data.transform, niToJ3dData.nifVer);
		transform.set(m);

		transformGroup.setTransform(transform);

		if (data.shape.ref != -1)
		{
			bhkShape bhkShape = (bhkShape) niToJ3dData.get(data.shape);
			processBhkShape(bhkShape, transformGroup, niToJ3dData);
		}
		g.addChild(transformGroup);
	}

	private static Shape3D bhkSphereShape(bhkSphereShape data, NifVer nifVer)
	{
		float radius = ConvertFromHavok.toJ3d(data.radius, nifVer);
		SphereGenerator sg = new SphereGenerator(radius);
		GeometryData gd = new GeometryData();
		gd.geometryType = GeometryData.TRIANGLE_STRIPS;
		sg.generate(gd);

		Shape3D shape = new Shape3D();
		TriangleStripArray tsa = new TriangleStripArray(gd.vertexCount, defaultFormat, gd.stripCounts);
		tsa.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(gd.coordinates)));

		shape.setGeometry(tsa);

		shape.setAppearance(PhysAppearance.makeAppearance());
		return shape;
	}

	private static Group bhkMultiSphereShape(bhkMultiSphereShape data, NifVer nifVer)
	{
		Group g = new Group();

		for (int i = 0; i < data.numSpheres; i++)
		{
			NifSphereBV sphere = data.spheres[i];

			float radius = ConvertFromHavok.toJ3d(sphere.radius, nifVer);
			Vector3f v1 = ConvertFromHavok.toJ3d(sphere.center, nifVer);

			SphereGenerator sg = new SphereGenerator(radius);
			GeometryData gd = new GeometryData();
			gd.geometryType = GeometryData.TRIANGLE_STRIPS;
			sg.generate(gd);
			Shape3D shape = new Shape3D();
			TriangleStripArray tsa = new TriangleStripArray(gd.vertexCount, defaultFormat, gd.stripCounts);
			tsa.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(gd.coordinates)));

			shape.setGeometry(tsa);

			shape.setAppearance(PhysAppearance.makeAppearance());
			TransformGroup tg = new TransformGroup();
			Transform3D t = new Transform3D();
			t.setTranslation(v1);
			tg.setTransform(t);
			tg.addChild(shape);
			g.addChild(tg);
		}

		return g;

	}

	private static Group bhkCapsuleShape(bhkCapsuleShape data, NifVer nifVer)
	{
		//TODO: try JBullet CapsuleShapeX
		Group g = new Group();

		float radius = ConvertFromHavok.toJ3d(data.radius, nifVer);
		Vector3f v1 = ConvertFromHavok.toJ3d(data.firstPoint, nifVer);
		float radius1 = ConvertFromHavok.toJ3d(data.radius1, nifVer);
		Vector3f v2 = ConvertFromHavok.toJ3d(data.secondPoint, nifVer);
		float radius2 = ConvertFromHavok.toJ3d(data.radius2, nifVer);

		SphereGenerator sg = new SphereGenerator(radius1);
		GeometryData gd = new GeometryData();
		gd.geometryType = GeometryData.TRIANGLE_STRIPS;
		sg.generate(gd);
		Shape3D shape = new Shape3D();
		TriangleStripArray tsa = new TriangleStripArray(gd.vertexCount, defaultFormat, gd.stripCounts);
		tsa.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(gd.coordinates)));

		shape.setGeometry(tsa);
		shape.setAppearance(PhysAppearance.makeAppearance());
		TransformGroup tg = new TransformGroup();
		Transform3D t = new Transform3D();
		t.setTranslation(v1);
		tg.setTransform(t);
		tg.addChild(shape);
		g.addChild(tg);

		sg = new SphereGenerator(radius2);
		gd = new GeometryData();
		gd.geometryType = GeometryData.TRIANGLE_STRIPS;
		sg.generate(gd);
		shape = new Shape3D();
		tsa = new TriangleStripArray(gd.vertexCount, defaultFormat, gd.stripCounts);
		tsa.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(gd.coordinates)));

		shape.setGeometry(tsa);
		shape.setAppearance(PhysAppearance.makeAppearance());
		tg = new TransformGroup();
		t = new Transform3D();
		t.setTranslation(v2);
		tg.setTransform(t);
		tg.addChild(shape);
		g.addChild(tg);

		float length = new Point3f(v2).distance(new Point3f(v1));
		CylinderGenerator cg = new CylinderGenerator(length, radius);
		gd = new GeometryData();
		gd.geometryType = GeometryData.TRIANGLE_STRIPS;
		cg.generate(gd);
		shape = new Shape3D();
		tsa = new TriangleStripArray(gd.vertexCount, defaultFormat, gd.stripCounts);
		tsa.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(gd.coordinates)));

		shape.setGeometry(tsa);
		shape.setAppearance(PhysAppearance.makeAppearance());
		tg = new TransformGroup();
		Transform3D t1 = new Transform3D();
		Transform3D t2 = new Transform3D();

		Vector3f diff = new Vector3f(v2);

		diff.sub(v1);
		diff.scale(0.5f);
		v2.sub(diff);

		// note up must not be parallel to diff hence crazy up vector
		t1.lookAt(new Point3d(v2), new Point3d(v1), new Vector3d(diff.y, diff.z, diff.x));
		t1.invert();

		t2.rotX(Math.PI / 2);
		t1.mul(t2);

		tg.setTransform(t1);
		tg.addChild(shape);
		g.addChild(tg);

		return g;

	}

	private static Shape3D bhkBoxShape(bhkBoxShape data, NifVer nifVer)
	{
		float x = ConvertFromHavok.toJ3d(data.dimensions.x, nifVer);
		float y = ConvertFromHavok.toJ3d(data.dimensions.z, nifVer);
		float z = ConvertFromHavok.toJ3d(data.dimensions.y, nifVer);

		TriangleArray cube = new TriangleArray(36, defaultFormat);

		float scaledVerts[] = new float[] {
				// front face
				x, -y, z, //1
				x, y, z, //2
				-x, y, z, //3
				x, -y, z, //1
				-x, y, z, //3
				-x, -y, z, //4
				// back face
				-x, -y, -z, //1
				-x, y, -z, //2
				x, y, -z, //3
				-x, -y, -z, //1
				x, y, -z, //3
				x, -y, -z, //4
				// right face
				x, -y, -z, //1
				x, y, -z, //2
				x, y, z, //3
				x, -y, -z, //1
				x, y, z, //3
				x, -y, z, //4
				// left face
				-x, -y, z, //1
				-x, y, z, //2
				-x, y, -z, //3
				-x, -y, z, //1
				-x, y, -z, //3
				-x, -y, -z, //4
				// top face
				x, y, z, //1
				x, y, -z, //2
				-x, y, -z, //3
				x, y, z, //1
				-x, y, -z, //3
				-x, y, z, //4
				// bottom face
				-x, -y, z, //1
				-x, -y, -z, //2
				x, -y, -z, //3
				-x, -y, z, //1
				x, -y, -z, //3
				x, -y, z, };//4

		cube.setCoordRefBuffer(new J3DBuffer(Utils3D.makeFloatBuffer(scaledVerts)));

		// Put geometry into Shape3d
		Shape3D shape = new Shape3D();
		shape.setGeometry(cube);

		shape.setAppearance(PhysAppearance.makeAppearance());
		return shape;
	}

	public static Shape3D bhkConvexVerticesShape(bhkConvexVerticesShape data, NifVer nifVer)
	{
		ObjectArrayList<Vector3f> points = new ObjectArrayList<Vector3f>();

		for (int i = 0; i < data.numVertices; i++)
		{
			points.add(new Vector3f(ConvertFromHavok.toJ3dP3f(data.vertices[i], nifVer)));
		}

		ConvexHullShape convexShape = new ConvexHullShape(points);
		// create a hull approximation
		ShapeHull hull = new ShapeHull(convexShape);
		float margin = convexShape.getMargin();
		hull.buildHull(margin);

		if (hull.numTriangles() > 0)
		{

			IntArrayList idx = hull.getIndexPointer();
			ObjectArrayList<Vector3f> vtx = hull.getVertexPointer();

			int[] coordIndices = new int[hull.numIndices()];
			for (int i = 0; i < hull.numIndices(); i++)
			{
				coordIndices[i] = idx.get(i);
			}

			Point3f[] coords = new Point3f[hull.numVertices()];
			for (int i = 0; i < hull.numVertices(); i++)
			{
				coords[i] = new Point3f(vtx.get(i));
			}

			GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);

			gi.setCoordinates(coords);
			gi.setCoordinateIndices(coordIndices);
			gi.setUseCoordIndexOnly(true);

			// Put geometry into Shape3d
			Shape3D shape = new Shape3D();
			shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));

			shape.setAppearance(PhysAppearance.makeAppearance());
			return shape;

		}

		return null;
	}

	public static Shape3D hkPackedNiTriStripsData(hkPackedNiTriStripsData data, NifVer nifVer)
	{
		int[] coordIndices = new int[data.numTriangles * 3];

		//Vector3f[] normals = new Vector3f[data.numTriangles];
		//int[] normIndices = new int[data.numTriangles * 3];
		for (int i = 0; i < data.numTriangles; i++)
		{
			coordIndices[i * 3 + 0] = data.triangles[i].triangle.v1;
			coordIndices[i * 3 + 1] = data.triangles[i].triangle.v2;
			coordIndices[i * 3 + 2] = data.triangles[i].triangle.v3;
			/*	if (nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)
				{
					normals[i] = ConvertFromHavok.toJ3dNoScale(data.triangles[i].normal);
					normIndices[i * 3 + 0] = i;
					normIndices[i * 3 + 1] = i;
					normIndices[i * 3 + 2] = i;
				}*/
		}
		Point3f[] coords = new Point3f[data.numVertices];
		for (int i = 0; i < data.numVertices; i++)
		{
			coords[i] = ConvertFromHavok.toJ3dP3f(data.vertices[i], nifVer);
		}

		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
		gi.setCoordinates(coords);
		gi.setCoordinateIndices(coordIndices);
		gi.setUseCoordIndexOnly(true);

		/*	if (nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)
			{
				gi.setNormalIndices(normIndices);
				gi.setNormals(normals);
			}*/

		// Put geometry into Shape3d
		Shape3D shape = new Shape3D();
		shape.setName("hkPackedNiTriStripsData:");
		shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
		shape.setAppearance(PhysAppearance.makeAppearance());
		return shape;
	}

	public static Shape3D processNiTriStripsData(NiTriStripsData data)
	{
		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);

		if (data.hasVertices)
		{
			//OPTOMIZATION
			/*
			Point3f[] vertices = new Point3f[data.numVertices];
			for (int i = 0; i < data.numVertices; i++)
			{
				vertices[i] = ConvertFromNif.toJ3dP3f(data.vertices[i]);
			}
			gi.setCoordinates(vertices);*/
			gi.setCoordinates(Utils3D.extractArrayFromFloatBuffer(data.verticesOptBuf));
		}

		int numStrips = data.numStrips;
		int[] stripLengths = data.stripLengths;

		if (data.hasPoints)
		{
			// get full length
			int length = 0;
			for (int i = 0; i < numStrips; i++)
			{
				length += data.points[i].length;
			}

			gi.setStripCounts(stripLengths);
			int[] points = new int[length];
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

		// Put geometry into Shape3d
		Shape3D shape = new Shape3D();
		shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
		shape.setAppearance(PhysAppearance.makeAppearance());
		return shape;
	}

	public static float CMD_VERT_SCALE = 1f / 1000f;

	public static Group bhkCompressedMeshShape(bhkCompressedMeshShapeData data, NifVer nifVer)
	{

		//the masks are just low 17 bits for tri and highest bit for winding
		if (data.BitsPerIndex != 17 || data.BitsPerWindingIndex != 18)
		{
			System.out.println("unexpected bhkCompressedMeshShapeData.BitsPerIndex " + data.BitsPerIndex);
			System.out.println("unexpected bhkCompressedMeshShapeData.BitesPerWindingIndex " + data.BitsPerWindingIndex);
		}

		Group group = new Group();

		if (data.NumBigTris > 0)
		{
			Point3f[] vertices = new Point3f[data.BigVerts.length];
			for (int i = 0; i < data.BigVerts.length; i++)
			{
				vertices[i] = ConvertFromHavok.toJ3dP3f(//
						(data.BigVerts[i].x), //
						(data.BigVerts[i].y), //
						(data.BigVerts[i].z), nifVer);
			}

			int[] listPoints = new int[data.BigTris.length * 3];
			for (int i = 0; i < data.BigTris.length; i++)
			{
				listPoints[(i * 3) + 0] = data.BigTris[i].Triangle1;
				listPoints[(i * 3) + 1] = data.BigTris[i].Triangle2;
				listPoints[(i * 3) + 2] = data.BigTris[i].Triangle3;
			}

			GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
			gi.setCoordinates(vertices);
			gi.setCoordinateIndices(listPoints);
			gi.setUseCoordIndexOnly(true);

			Shape3D shape = new Shape3D();
			shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
			shape.setAppearance(PhysAppearance.makeAppearance(new Color3f(0.5f, 0.5f, 0)));
			group.addChild(shape);
		}

		for (int c = 0; c < data.NumChunks; c++)
		{
			NifbhkCMSDChunk chunk = data.Chunks[c];

			Point3f[] vertices = new Point3f[chunk.Vertices.length / 3];
			for (int i = 0; i < chunk.Vertices.length / 3; i++)
			{
				vertices[i] = ConvertFromHavok.toJ3dP3f(//
						((chunk.Vertices[(i * 3) + 0]) * CMD_VERT_SCALE) + chunk.translation.x, //
						((chunk.Vertices[(i * 3) + 1]) * CMD_VERT_SCALE) + chunk.translation.y, //
						((chunk.Vertices[(i * 3) + 2]) * CMD_VERT_SCALE) + chunk.translation.z, nifVer);
			}

			int numStrips = chunk.NumStrips;
			int[] stripLengths = new int[numStrips];

			// copy and get full length
			int stripsLensIdxCount = 0;
			for (int i = 0; i < numStrips; i++)
			{
				stripLengths[i] = chunk.Strips[i];
				stripsLensIdxCount += chunk.Strips[i];
			}

			//NOTE one indices list hold both strip and list data
			int[] stripPoints = new int[stripsLensIdxCount];
			int idx = 0;
			for (int i = 0; i < numStrips; i++)
			{
				for (int j = 0; j < stripLengths[i]; j++)
				{
					stripPoints[idx] = chunk.Indices[idx];
					idx++;
				}
			}

			//NOTE one indices list hold both strip and list data
			int triListIndicesLength = chunk.NumIndices - stripsLensIdxCount;
			int[] listPoints = new int[triListIndicesLength];
			idx = 0;
			for (int i = stripsLensIdxCount; i < chunk.NumIndices; i++)
			{
				listPoints[idx] = chunk.Indices[i];
				idx++;
			}

			NifbhkCMSDTransform cmsdt = data.ChunkTransforms[chunk.transformIndex];
			Vector3f transformTrans = ConvertFromHavok.toJ3d(cmsdt.Translation, nifVer);
			Quat4f transformRot = ConvertFromHavok.toJ3d(cmsdt.Rotation);
			Transform3D t = new Transform3D(transformRot, transformTrans, 1f);

			TransformGroup tg = new TransformGroup();
			tg.setTransform(t);

			group.addChild(tg);

			// do the strips first 
			if (stripLengths.length > 0)
			{
				GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);
				gi.setCoordinates(vertices);
				gi.setStripCounts(stripLengths);
				gi.setCoordinateIndices(stripPoints);
				gi.setUseCoordIndexOnly(true);

				Shape3D shape = new Shape3D();
				shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
				shape.setAppearance(PhysAppearance.makeAppearance(new Color3f(0.5f, 1f, 0)));
				tg.addChild(shape);

			}

			//now the tri list			
			if (triListIndicesLength > 0)
			{
				GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
				gi.setCoordinates(vertices);
				gi.setCoordinateIndices(listPoints);
				gi.setUseCoordIndexOnly(true);

				Shape3D shape = new Shape3D();
				shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
				shape.setAppearance(PhysAppearance.makeAppearance(new Color3f(0.75f, 1f, 1f)));
				tg.addChild(shape);

			}

		}

		return group;
	}

	/**
	 * Not used! replaced by bullet version above
	 * @param data
	 * @return
	 */
	@Deprecated
	public static Shape3D bhkConvexVerticesShapePreBullet(bhkConvexVerticesShape data, NifVer nifVer)
	{
		// It appears that a convex shape has no triangles to it. It is simply
		// a pile of points on an exterior. so a box is simply 8 points with no triangles defined

		// HOWEVER the havok also contains normals pointing to each face normals

		// so let's find all vertices that lie on the plane of each face, then build tris out of all of them
		// noting that a face might have more than 3 vertices in the plane,
		// just like a box, 8 points and 6 faces (each face is 2 tris or a quad)
		// Any point P = (x,y,z) lies on the plane if it satisfes the following
		// A x + B y + C z + D = 0
		// where ABCD are the equation of the plane

		//TODO: ShapeHull from jbullet does this same job (but compexly)

		// Radius REMoved!! note the radius number below, this is because of the "faces on a sphere" 
		//business this is all about, see jbullet

		//File: C:\game media\skyrim\meshes\landscape\rocks\rockpilem01tundra.nif	

		Vector<Integer> faceIdxs = new Vector<Integer>();
		for (int i = 0; i < data.numNormals; i++)
		{
			float A = data.normals[i].x;
			float B = data.normals[i].y;
			float C = data.normals[i].z;
			float D = data.normals[i].w;

			int coplanarCount = 0;
			int[] coplanar = new int[4];

			for (int j = 0; j < data.numVertices; j++)
			{
				float x = data.vertices[j].x;
				float y = data.vertices[j].y;
				float z = data.vertices[j].z;

				// check for bloody close to co planar due to rounding in the maths function
				if (Math.abs((A * x + B * y + C * z + D) - data.radius) < 0.001)
				{
					coplanar[coplanarCount] = j;
					coplanarCount++;
					// we'll only work with tris or quads
					if (coplanarCount == 4)
					{
						break;
					}
				}

			}

			// now lets add the face idx in
			if (coplanarCount > 2)
			{
				faceIdxs.add(new Integer(coplanar[0]));
				faceIdxs.add(new Integer(coplanar[1]));
				faceIdxs.add(new Integer(coplanar[2]));
				if (coplanarCount > 3)
				{
					faceIdxs.add(new Integer(coplanar[1]));
					faceIdxs.add(new Integer(coplanar[2]));
					faceIdxs.add(new Integer(coplanar[3]));
				}
			}

		}

		//did we find any faces(bug in radius size in source data!)
		if (faceIdxs.size() > 0)
		{
			int[] coordIndices = new int[faceIdxs.size()];
			for (int i = 0; i < faceIdxs.size(); i++)
			{
				coordIndices[i] = faceIdxs.elementAt(i).intValue();
			}

			Point3f[] coords = new Point3f[data.numVertices];
			for (int i = 0; i < data.numVertices; i++)
			{
				coords[i] = ConvertFromHavok.toJ3dP3f(data.vertices[i], nifVer);
			}

			GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);

			gi.setCoordinates(coords);
			gi.setCoordinateIndices(coordIndices);
			gi.setUseCoordIndexOnly(true);

			// Put geometry into Shape3d
			Shape3D shape = new Shape3D();
			shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));

			shape.setAppearance(PhysAppearance.makeAppearance());
			return shape;
		}
		else
		{
			return null;
		}
	}
}
