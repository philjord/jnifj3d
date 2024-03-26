package nif.j3d;

import java.util.Vector;

import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.IndexedLineArray;
import org.jogamp.java3d.IndexedPointArray;
import org.jogamp.java3d.J3DBuffer;
import org.jogamp.java3d.PointAttributes;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.TriangleArray;
import org.jogamp.java3d.TriangleStripArray;
import org.jogamp.java3d.geom.CylinderGenerator;
import org.jogamp.java3d.geom.GeometryData;
import org.jogamp.java3d.geom.SphereGenerator;
import org.jogamp.java3d.utils.geometry.GeometryInfo;
import org.jogamp.java3d.utils.shader.SimpleShaderAppearance;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.collision.shapes.ShapeHull;
import com.bulletphysics.util.IntArrayList;
import com.bulletphysics.util.ObjectArrayList;

import nif.NifVer;
import nif.compound.NifSphereBV;
import nif.niobject.NiTriStripsData;
import nif.niobject.bhk.bhkBoxShape;
import nif.niobject.bhk.bhkCapsuleShape;
import nif.niobject.bhk.bhkConvexVerticesShape;
import nif.niobject.bhk.bhkMultiSphereShape;
import nif.niobject.bhk.bhkNiTriStripsShape;
import nif.niobject.bhk.bhkSphereShape;
import nif.niobject.bhk.hkPackedNiTriStripsData;
import nif.niobject.bs.BSbhkNPObject;
import nif.niobject.hkx.hkAabb;
import nif.niobject.hkx.hkcdStaticMeshTreeBasePrimitive;
import nif.niobject.hkx.hknpBodyCinfo;
import nif.niobject.hkx.hknpCompressedMeshShape;
import nif.niobject.hkx.hknpCompressedMeshShapeData;
import nif.niobject.hkx.hknpCompressedMeshShapeTree;
import nif.niobject.hkx.hknpPhysicsSystemData;
import nif.niobject.hkx.hknpShape;
import nif.niobject.hkx.reader.HKXContents;
import nif.tools.MiniFloat;
import tools3d.utils.PhysAppearance;
import tools3d.utils.Utils3D;
import utils.ESConfig;
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
				t.setRotation(ConvertFromHavok.toJ3d(bodyCinfo.orientation));//FIXME: is the nifquat the same as nifquatxyzw?	
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
		else if (bhkShape instanceof bhkTransformShape)
		{
			bhkTransformShape((bhkTransformShape) bhkShape, group, niToJ3dData);
		}
		else if (bhkShape instanceof bhkConvexListShape)
		{
			//	bhkConvexListShape((bhkConvexListShape) bhkShape, group, blocks);
			//TODO: bhkConvexListShape
		}
		else */if (hknpShape instanceof hknpCompressedMeshShape)
		{
			hknpCompressedMeshShape hknpCompressedMeshShape = (hknpCompressedMeshShape) hknpShape;

			if (hknpCompressedMeshShape.data > 0)
			{
				hknpCompressedMeshShapeData hknpCompressedMeshShapeData = (hknpCompressedMeshShapeData) contents.get(hknpCompressedMeshShape.data);
				group.addChild(hknpCompressedMeshShapeData(hknpCompressedMeshShapeData, contents));
			} else {
				System.out.println("no shape data for  hknpCompressedMeshShape");
			}
		}
		else
		{
			System.out.println("J3dbhkCollisionObject - unknown bhkShape " + hknpShape);
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

	public static NifVer nifVer = new NifVer("FO4", NifVer.VER_20_2_0_7, 12, 130);

	public static Group hknpCompressedMeshShapeData(hknpCompressedMeshShapeData data, HKXContents contents)
	{
 		// we are in fact dealing only with the meshTree not the simdTree
		hknpCompressedMeshShapeTree meshTree = data.meshTree;

		Group group = new Group();
		
		hkAabb meshTreehkAabb = meshTree.domain; // full AABB of all sections
			
		Point3f[] sharedVertices = null;
		if(meshTree.sharedVertices != null) {
			sharedVertices = new Point3f[meshTree.sharedVertices.length];
			for (int pvi = 0; pvi < meshTree.sharedVertices.length; pvi++)
			{			
				long pv = meshTree.sharedVertices[pvi];			
			
				//z value are what I'm calling x, so is this is in zyx format, 10, 11, 11 bit			
	 
	
				// 64 bits in a  long, possibly signed? probably mini floats
			/*	float fx2 = MiniFloat.float22bits(pv, 0); 
				float fy2 = MiniFloat.float21bits(pv, 22);  
				float fz2 = MiniFloat.float21bits(pv, 43);  
				System.out.println("minifloat  fxyz2 " + fx2 + ", " + fy2+ ", " +fz2);*/
					
				//21bit x, 21 bit y, 22 bit z this time!
				float fx = (((pv >> 0) & 0x1FFFFF)/(float)0x1FFFFF * (meshTreehkAabb.max.x-meshTreehkAabb.min.x)) + meshTreehkAabb.min.x;
				float fy = (((pv >> 21) & 0x1FFFFF)/(float)0x1FFFFF * (meshTreehkAabb.max.y-meshTreehkAabb.min.y)) + meshTreehkAabb.min.y;
				float fz = (((pv >> 42) & 0x3FFFFF)/(float)0x3FFFFF * (meshTreehkAabb.max.z-meshTreehkAabb.min.z)) + meshTreehkAabb.min.z;
				//System.out.println("long:" + String.format("%64s", Long.toBinaryString(pv)).replace(" ", "0"));	
				//System.out.println("minifloat  fxyz " + fx + ", " + fy+ ", " +fz);
			
				sharedVertices[pvi] = ConvertFromHavok.toJ3dP3f( -fx,-fy,fz, nifVer);	
			}		
		}


		// keep track of where we are in the primitives array
		int startIndex = 0;
		for (int s = 0; s < meshTree.sections.length; s++)
		{
			int firstPackedVertex = meshTree.sections[s].firstPackedVertex;
			int numPackedVertices = meshTree.sections[s].numPackedVertices;
			int numSharedIndices = meshTree.sections[s].numSharedIndices;			
			//hkAabb currentSectionhkAabb = meshTree.sections[currentSectionIdx].domain;	
			float[] codecParms = meshTree.sections[s].codecParms;// parallel Algebraic Recursive Multilevel Solvers?   
			
			Point3f[] vertices = new Point3f[numPackedVertices + numSharedIndices];
			
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
				
				vertices[pvi] = ConvertFromHavok.toJ3dP3f( -fx,-fy,fz, nifVer);		
			}
			
			
			if(meshTree.sharedVertices != null) {
				for(int i =0 ; i< numSharedIndices;i++)
					vertices[numPackedVertices+i] = sharedVertices[i];
			}
						
			
			addDebugPoints(vertices, new Color3f(0.0f,1f-((float)s/(float)meshTree.primitiveDataRuns.length), ((float)s/(float)meshTree.primitiveDataRuns.length)), group);
			
			//int index = meshTree.primitiveDataRuns[s].index;
			int count = meshTree.primitiveDataRuns[s].count;
			
			// bum have to precount so we can allocate a index array
			int pointCount = 0;
			for (int p = startIndex; p < startIndex + count; p++)
			{		
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
			int i = 0;
			for (int p = startIndex; p < startIndex + count; p++)
			{		
				
				hkcdStaticMeshTreeBasePrimitive primitive = meshTree.primitives[p]; 
				int[] indices = primitive.indices;
				
				//TODO: notice the sharedIndices need to be run through 
				//int[] sharedVerticesIndex = meshTree.sharedVerticesIndex;
				//0 1 2 3 4 5 6 7 8 9 10 11 12 2 1 0 4 3 12 7 6 5 10 9 8 11
				
												 				
				//quad?
				if(indices[2] != indices[3]) {
					listPoints[i++] = indices[0];
					listPoints[i++] = indices[1];
					listPoints[i++] = indices[2];
					listPoints[i++] = indices[2];
					listPoints[i++] = indices[3];
					listPoints[i++] = indices[0];
				} else {//just a tri					
					listPoints[i++] = indices[0];
					listPoints[i++] = indices[1];
					listPoints[i++] = indices[2];
				}
			}	
					
					
			GeometryInfo gi = new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
			gi.setCoordinates(vertices);
			gi.setCoordinateIndices(listPoints);
			gi.setUseCoordIndexOnly(true);

			try {
			Shape3D shape = new Shape3D();
			shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
			shape.setAppearance(PhysAppearance.makeAppearance(new Color3f(0.75f,1f-((float)s/(float)meshTree.primitiveDataRuns.length), ((float)s/(float)meshTree.primitiveDataRuns.length))));
			group.addChild(shape);
			}catch (ArrayIndexOutOfBoundsException e) {
				e.printStackTrace();
			}

			
			startIndex += count;//prep for next iter, needs to be by index above
		}

			
			
			
			
		return group;
	}
	
	
	private static void addDebugPoints(Point3f[] vertices, Color3f color, Group parent) {
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
		parent.addChild(shape);
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
