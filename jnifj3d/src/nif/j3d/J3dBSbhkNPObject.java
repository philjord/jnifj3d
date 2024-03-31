package nif.j3d;

import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.IndexedLineArray;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.utils.geometry.GeometryInfo;
import org.jogamp.java3d.utils.shader.SimpleShaderAppearance;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Tuple3f;
import org.jogamp.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.collision.shapes.ShapeHull;
import com.bulletphysics.util.IntArrayList;
import com.bulletphysics.util.ObjectArrayList;

import nif.NifVer;
import nif.niobject.bs.BSbhkNPObject;
import nif.niobject.hkx.hkAabb;
import nif.niobject.hkx.hkcdStaticMeshTreeBasePrimitive;
import nif.niobject.hkx.hknpBodyCinfo;
import nif.niobject.hkx.hknpCompressedMeshShape;
import nif.niobject.hkx.hknpCompressedMeshShapeData;
import nif.niobject.hkx.hknpCompressedMeshShapeTree;
import nif.niobject.hkx.hknpConvexPolytopeShape;
import nif.niobject.hkx.hknpDynamicCompoundShape;
import nif.niobject.hkx.hknpPhysicsSystemData;
import nif.niobject.hkx.hknpShape;
import nif.niobject.hkx.reader.HKXContents;
import tools3d.utils.PhysAppearance;
import utils.convert.ConvertFromHavok;

/**
 * NOTE for trival render only now, bullet does teh hard work!
 * normals ignored as slow and cause huge indexify operations due to diff indeices counts
 * @author philip
 *
 */
public class J3dBSbhkNPObject extends Group
{
	private static final boolean COMPACT = false;
	private static final boolean BY_REF = true;
	private static final boolean INTERLEAVED = false;
	private static final boolean NIO = true;

	private static final int defaultFormat = GeometryArray.COORDINATES | GeometryArray.BY_REFERENCE | GeometryArray.USE_NIO_BUFFER;
	

	public static NifVer nifVer = new NifVer("FO4", NifVer.VER_20_2_0_7, 12, 130);

	//TODO: many more JBullet style conversions

	public J3dBSbhkNPObject(BSbhkNPObject object, NiToJ3dData niToJ3dData)
	{
		HKXContents contents = object.hkxContents;
		// the first one had better be a system
		hknpPhysicsSystemData hknpPhysicsSystemData = (hknpPhysicsSystemData)contents.getContentCollection().iterator().next();
			
		//physics bodies are here
		hknpBodyCinfo[] bodyCinfos = hknpPhysicsSystemData.bodyCinfos;
		for(int  i= 0 ; i < bodyCinfos.length; i++) {
			hknpBodyCinfo bodyCinfo = bodyCinfos[i];
			
			long shapeId = bodyCinfo.shape;
			if(shapeId > 0) {
				hknpShape hknpShape = (hknpShape)contents.get(shapeId);
				 
				Transform3D t = new Transform3D();	
				t.setRotation(ConvertFromHavok.toJ3d(bodyCinfo.orientation)); 	
				t.setTranslation(ConvertFromHavok.toJ3d(bodyCinfo.position, niToJ3dData.nifVer));	
				Group lowerGroup = new TransformGroup(t);			 
	
				processHknpShape(hknpShape, lowerGroup, contents);
	
				addChild(lowerGroup);
			}
		}
	}

	private static void processHknpShape(hknpShape hknpShape, Group group, HKXContents contents)
	{
		/*if (bhkShape instanceof bhkListShape)
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
		else */if (hknpShape instanceof hknpDynamicCompoundShape)
		{
			//TODO:
			//bhkTransformShape((bhkTransformShape) bhkShape, group, niToJ3dData);
		}
		else if (hknpShape instanceof hknpConvexPolytopeShape)
		{
			group.addChild(hknpConvexPolytopeShape((hknpConvexPolytopeShape) hknpShape, contents));
		}
		else if (hknpShape instanceof hknpCompressedMeshShape)
		{
			hknpCompressedMeshShape hknpCompressedMeshShape = (hknpCompressedMeshShape) hknpShape;

			if (hknpCompressedMeshShape.data > 0)
			{
				hknpCompressedMeshShapeData hknpCompressedMeshShapeData = (hknpCompressedMeshShapeData) contents.get(hknpCompressedMeshShape.data);
				group.addChild(hknpCompressedMeshShapeData(hknpCompressedMeshShapeData, contents));
			} else {
				//Meshes\Interiors\Utility\Doors\UtilMetalDbDoor01.nif
				System.out.println("no shape data for  hknpCompressedMeshShape " );
			}
		}
		else
		{
			System.out.println("J3dbhkCollisionObject - unknown bhkShape " + hknpShape);
		}

	}

/*	private static void bhkNiTriStripsShape(bhkNiTriStripsShape data, Group g, NiToJ3dData niToJ3dData)
	{
		for (int i = 0; i < data.numStripsData; i++)
		{
			NiTriStripsData niTriStripsData = (NiTriStripsData) niToJ3dData.get(data.stripsData[i]);
			g.addChild(processNiTriStripsData(niTriStripsData));
		}
	}*/

/*	private static void bhkMoppBvTreeShape(bhkMoppBvTreeShape data, Group g, NiToJ3dData niToJ3dData)
	{
		if (data.shape.ref != -1)
		{
			bhkShape bhkShape = (bhkShape) niToJ3dData.get(data.shape);
			processBhkShape(bhkShape, g, niToJ3dData);
		}
	}*/

/*	private static void bhkTransformShape(bhkTransformShape data, Group g, NiToJ3dData niToJ3dData)
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
	}*/

	/*private static Shape3D bhkSphereShape(bhkSphereShape data, NifVer nifVer)
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
	}*/

	/*private static Group bhkMultiSphereShape(bhkMultiSphereShape data, NifVer nifVer)
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

	}*/

	/*private static Group bhkCapsuleShape(bhkCapsuleShape data, NifVer nifVer)
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

	}*/

	/*private static Shape3D bhkBoxShape(bhkBoxShape data, NifVer nifVer)
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
	}*/

	public static Node hknpConvexPolytopeShape(hknpConvexPolytopeShape data, HKXContents contents)
	{				
		Group group = new Group();	
				
		Point3f[] vertices = new Point3f[data.vertices.length];
		for (int i = 0; i < data.vertices.length; i++)
		{
			vertices[i] = new Point3f(ConvertFromHavok.toJ3dP3f(data.vertices[i], nifVer));
		}
		group.addChild(createDebugPointShape(vertices, new Color3f(0.0f,0.5f,0.3f)));
		Point3f[] vertices2 = new Point3f[data.planes.length];
		for (int i = 0; i < data.planes.length; i++)
		{			
			vertices2[i] = new Point3f(ConvertFromHavok.toJ3dP3f(data.planes[i], nifVer));
		}
		
		group.addChild(createDebugPointShape(vertices2, new Color3f(1.0f,0.5f,0.3f)));
		
		
		// ok vertices and planes, I'm see for a box which wants 8 vertexs I see oddly, very oddly, 
		//vertices 3 oddly close to 0,0,0 4 good verts, planes, 4 that look like verts almost and 3 that are
		// like a plane, normal and distance (e.g. 0,0,1,-0.16)
		
		
		// this last 3 might be axis angle rotations perhaps?
		// BOSSteps needs both a rotatin and a translation it looks like, and I have 3x2 so maybe
		
		// so this 3 thingy below works, possibly this is a flags thing? I have flags of 323 which is
		//<enumitem name='USE_SMALL_FACE_INDICES' value='256'/>
		//<enumitem name='SUPPORTS_COLLISIONS_WITH_INTERIOR_TRIANGLES' value='64'/>
		//<enumitem name='IS_CONVEX_SHAPE' value='1'/>
		//<enumitem name='IS_CONVEX_POLYTOPE_SHAPE' value='2'/>
		 
		
		
		ObjectArrayList<Vector3f> points = new ObjectArrayList<Vector3f>();
		
		if( data.vertices.length == 7 && data.planes.length == 7){ 
			for (int i = 0; i < 3; i++)
			{
				System.out.println("i "+ i + ""  +ConvertFromHavok.toJ3dP3f(data.vertices[i], nifVer));
			}
			for (int i = 3; i < data.vertices.length; i++)
			{
				points.add(new Vector3f(ConvertFromHavok.toJ3dP3f(data.vertices[i], nifVer)));
			}
			for (int i = 0; i < data.planes.length-3; i++)
			{
				points.add(new Vector3f(ConvertFromHavok.toJ3dP3f(data.planes[i], nifVer)));
			}
		} else {
			System.out.println("Bad hknpConvexPolytopeShape ");
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
			group.addChild(shape);

		}

		return group;
	}

/*	public static Shape3D hkPackedNiTriStripsData(hkPackedNiTriStripsData data, NifVer nifVer)
	{
		int[] coordIndices = new int[data.numTriangles * 3];

		//Vector3f[] normals = new Vector3f[data.numTriangles];
		//int[] normIndices = new int[data.numTriangles * 3];
		for (int i = 0; i < data.numTriangles; i++)
		{
			coordIndices[i * 3 + 0] = data.triangles[i].triangle.v1;
			coordIndices[i * 3 + 1] = data.triangles[i].triangle.v2;
			coordIndices[i * 3 + 2] = data.triangles[i].triangle.v3;
				//if (nifVer.LOAD_VER <= NifVer.VER_20_0_0_5)	{
				//	normals[i] = ConvertFromHavok.toJ3dNoScale(data.triangles[i].normal);
				//	normIndices[i * 3 + 0] = i;
				//	normIndices[i * 3 + 1] = i;
				//	normIndices[i * 3 + 2] = i;
				//}
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

			//if (nifVer.LOAD_VER <= NifVer.VER_20_0_0_5) {
			//	gi.setNormalIndices(normIndices);
			//	gi.setNormals(normals);
			//}

		// Put geometry into Shape3d
		Shape3D shape = new Shape3D();
		shape.setName("hkPackedNiTriStripsData:");
		shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
		shape.setAppearance(PhysAppearance.makeAppearance());
		return shape;
	}*/

/*	public static Shape3D processNiTriStripsData(NiTriStripsData data)
	{
		GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_STRIP_ARRAY);

		if (data.hasVertices)
		{
			//OPTOMIZATION
			
			//Point3f[] vertices = new Point3f[data.numVertices];
			//for (int i = 0; i < data.numVertices; i++) {
			//	vertices[i] = ConvertFromNif.toJ3dP3f(data.vertices[i]);
			//}
			gi.setCoordinates(vertices);
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
	}*/


	public static Node hknpCompressedMeshShapeData(hknpCompressedMeshShapeData data, HKXContents contents)
	{
 		// we are in fact dealing only with the meshTree not the simdTree
		hknpCompressedMeshShapeTree meshTree = data.meshTree;

		Group group = new Group();		
		
		// first decompress shared Vertices against the full AABB
		hkAabb meshTreehkAabb = meshTree.domain; // full AABB of all sections
			
		Point3f[] sharedVertices = new Point3f[0];// avoid null pointer checks
		if(meshTree.sharedVertices != null) {
			sharedVertices = new Point3f[meshTree.sharedVertices.length];
			for (int pvi = 0; pvi < meshTree.sharedVertices.length; pvi++)
			{			
				long pv = meshTree.sharedVertices[pvi];			
			
				//z value are what I'm calling x, so is this is in zyx format, 10, 11, 11 bit			 
				//21bit x, 21 bit y, 22 bit z this time!
				float fx = (((pv >> 0) & 0x1FFFFF)/(float)0x1FFFFF * (meshTreehkAabb.max.x-meshTreehkAabb.min.x)) + meshTreehkAabb.min.x;
				float fy = (((pv >> 21) & 0x1FFFFF)/(float)0x1FFFFF * (meshTreehkAabb.max.y-meshTreehkAabb.min.y)) + meshTreehkAabb.min.y;
				float fz = (((pv >> 42) & 0x3FFFFF)/(float)0x3FFFFF * (meshTreehkAabb.max.z-meshTreehkAabb.min.z)) + meshTreehkAabb.min.z;
			
				sharedVertices[pvi] = ConvertFromHavok.toJ3dP3f( -fx,fy,fz, nifVer);	
			}		
		}
		
		for (int s = 0; s < meshTree.sections.length; s++) {			
			// the current value is the lowest byte and the previous in the next short(?) up	
			int primitivesCount = meshTree.sections[s].primitives.data & 0xff;
			int primitivesOffset = (meshTree.sections[s].primitives.data) >> 8 & 0xffff;
			int sharedCount = meshTree.sections[s].sharedVertices.data & 0xff;
			int sharedOffset = (meshTree.sections[s].sharedVertices.data) >> 8 & 0xffff;
						
			
			int firstPackedVertex = meshTree.sections[s].firstPackedVertex;
			int numPackedVertices = meshTree.sections[s].numPackedVertices;
			int numSharedIndices = meshTree.sections[s].numSharedIndices;			
			//hkAabb currentSectionhkAabb = meshTree.sections[currentSectionIdx].domain;	
			float[] codecParms = meshTree.sections[s].codecParms;// parallel Algebraic Recursive Multilevel Solvers?   
			
			Point3f[] vertices = new Point3f[numPackedVertices + sharedVertices.length];//numSharedIndices];
			
			for(int pvi = 0; pvi < numPackedVertices; pvi++) {

				int pv = meshTree.packedVertices[firstPackedVertex + pvi];			
			
				//z value are what I'm calling x, so is this is in zyx format, 10, 11, 11 bit			
				
				// Normalized would look like this, but as the noramlization dividor is accounted for in the param scale we don't need it
				//float fx = (pv & 0x7FF) / 2047.0f; // 11bit
				//float fy = ((pv >> 11) & 0x7FF) / 2047.0f; // 11bit
				//float fz = ((pv >> 22) & 0x3FF) / 1023.0f; // 10bit
				// multiplised by the param scale, and offset added 
				float fx = (((pv >> 0) & 0x7FF) * codecParms[3]) + codecParms[0];
				float fy = (((pv >> 11) & 0x7FF) * codecParms[4]) + codecParms[1];
				float fz = (((pv >> 22) & 0x3FF) * codecParms[5]) + codecParms[2];
				
				vertices[pvi] = ConvertFromHavok.toJ3dP3f( -fx,fy,fz, nifVer);		
			}
						
			// all shared are copied to the end (if any), bit poor in efficiency, shorten later
			for(int i =0 ; i< sharedVertices.length;i++) {
				vertices[numPackedVertices+i] = sharedVertices[i];					 
			}		
			
			group.addChild(createDebugPointShape(vertices, new Color3f(0.0f,1f-((float)s/(float)meshTree.primitiveDataRuns.length), ((float)s/(float)meshTree.primitiveDataRuns.length))));
			
			// bum have to precount so we can allocate a index array
			int pointCount = 0;
			for (int p = primitivesOffset; p < primitivesOffset + primitivesCount; p++) {		
				hkcdStaticMeshTreeBasePrimitive primitive = meshTree.primitives[p]; 
				int[] indices = primitive.indices;
				//quad?
				if(indices[2] != indices[3]) {
					pointCount += 6;
				} else {//just a tri					
					pointCount += 3;
				}
			}						
			
			int[] listPoints = new int[pointCount];
			int idx = 0;
			for (int i = 0; i < primitivesCount; i++) {				
				int p = primitivesOffset + i;
				hkcdStaticMeshTreeBasePrimitive primitive = meshTree.primitives[p]; 
				
				int[] indices = primitive.indices;	

				// if any of the indices go beyond numPackedVertices then the distance beyond
				// is used as an index into the sharedVerticesIndex, starting at the <sharedOffset> for this section
				// each section has numSharedIndices of the sharedVerticesIndex as it's own
				// the index found is then used as an index into the shared vertex area of the vertices, which for now is at the end starting at 
				// numPackedVertices, but when a single packed and shared vertex array is used will be at the end of all packed vertices
								 				
				int[] sharedVerticesIndex = meshTree.sharedVerticesIndex;										 				
				//quad?
				if(indices[2] != indices[3]) {
					listPoints[idx++] = indices[0] < numPackedVertices ? indices[0] : sharedVerticesIndex[(indices[0]-numPackedVertices)+sharedOffset]+numPackedVertices;
					listPoints[idx++] = indices[1] < numPackedVertices ? indices[1] : sharedVerticesIndex[(indices[1]-numPackedVertices)+sharedOffset]+numPackedVertices;
					listPoints[idx++] = indices[2] < numPackedVertices ? indices[2] : sharedVerticesIndex[(indices[2]-numPackedVertices)+sharedOffset]+numPackedVertices;
					listPoints[idx++] = indices[2] < numPackedVertices ? indices[2] : sharedVerticesIndex[(indices[2]-numPackedVertices)+sharedOffset]+numPackedVertices;
					listPoints[idx++] = indices[3] < numPackedVertices ? indices[3] : sharedVerticesIndex[(indices[3]-numPackedVertices)+sharedOffset]+numPackedVertices;
					listPoints[idx++] = indices[0] < numPackedVertices ? indices[0] : sharedVerticesIndex[(indices[0]-numPackedVertices)+sharedOffset]+numPackedVertices;
				} else {//just a tri					
					listPoints[idx++] = indices[0] < numPackedVertices ? indices[0] : sharedVerticesIndex[(indices[0]-numPackedVertices)+sharedOffset]+numPackedVertices;
					listPoints[idx++] = indices[1] < numPackedVertices ? indices[1] : sharedVerticesIndex[(indices[1]-numPackedVertices)+sharedOffset]+numPackedVertices;
					listPoints[idx++] = indices[2] < numPackedVertices ? indices[2] : sharedVerticesIndex[(indices[2]-numPackedVertices)+sharedOffset]+numPackedVertices;
				}
			}	
					
					
			GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
			gi.setCoordinates(vertices);
			gi.setCoordinateIndices(listPoints);
			gi.setUseCoordIndexOnly(true);

			Shape3D shape = new Shape3D();
			shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
			shape.setAppearance(PhysAppearance.makeAppearance(new Color3f(0.75f,1f-((float)s/(float)meshTree.primitiveDataRuns.length), ((float)s/(float)meshTree.primitiveDataRuns.length))));
			group.addChild(shape);
		}			
			
		return group;
	}
	
	
	private static Shape3D createDebugPointShape(Tuple3f[] vertices, Color3f color) {
		///////////////////////////////////////////////////////////////Lovely little point drawer thing!
		int gaVertexCount = vertices.length * 6;// 4 points a crossed pair of lines
		IndexedLineArray ga = new IndexedLineArray(gaVertexCount,
		GeometryArray.BY_REFERENCE | GeometryArray.COORDINATES  
		| GeometryArray.BY_REFERENCE_INDICES | GeometryArray.USE_COORD_INDEX_ONLY,
		gaVertexCount);
		
		int[] gaCoordIndices = new int[gaVertexCount];
		//fixed for all time, recall these are points
		for (int i = 0; i < gaVertexCount; i++)
		{
		gaCoordIndices[i] = i;
		}
		
		float[] gaCoords = new float[gaVertexCount * 3];
		for (int i = 0; i < gaVertexCount/6; i++)
		{
			if(vertices[i]!=null) {
				gaCoords[i*3*6+0] = vertices[i].x;
				gaCoords[i*3*6+1] = vertices[i].y-0.01f;
				gaCoords[i*3*6+2] = vertices[i].z;
				gaCoords[i*3*6+3] = vertices[i].x;
				gaCoords[i*3*6+4] = vertices[i].y+0.01f;;
				gaCoords[i*3*6+5] = vertices[i].z;
				gaCoords[i*3*6+6] = vertices[i].x-0.01f;
				gaCoords[i*3*6+7] = vertices[i].y;
				gaCoords[i*3*6+8] = vertices[i].z;
				gaCoords[i*3*6+9] = vertices[i].x+0.01f;
				gaCoords[i*3*6+10] = vertices[i].y;
				gaCoords[i*3*6+11] = vertices[i].z;
				gaCoords[i*3*6+12] = vertices[i].x;
				gaCoords[i*3*6+13] = vertices[i].y;
				gaCoords[i*3*6+14] = vertices[i].z-0.01f;
				gaCoords[i*3*6+15] = vertices[i].x;
				gaCoords[i*3*6+16] = vertices[i].y;
				gaCoords[i*3*6+17] = vertices[i].z+0.01f;
			}
		}
		
		ga.setCoordRefFloat(gaCoords);
		ga.setCoordIndicesRef(gaCoordIndices);
		
		
		Shape3D shape = new Shape3D();
		shape.setGeometry(ga);
		shape.setAppearance(new SimpleShaderAppearance(color));
		return shape;
	}
}
