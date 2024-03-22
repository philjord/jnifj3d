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
import tools3d.utils.PhysAppearance;
import tools3d.utils.Utils3D;
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

	//public static float CMD_VERT_SCALE = 1f / 1000f;
	public static NifVer nifVer = new NifVer("FO4", NifVer.VER_20_2_0_7, 12, 130);

	public static Group hknpCompressedMeshShapeData(hknpCompressedMeshShapeData data, HKXContents contents)
	{
		// we are in fact dealing only with the meshTree not the simdTree
		hknpCompressedMeshShapeTree meshTree = data.meshTree;

		Group group = new Group();
		
		//possibly get the domain out as I imagine all the vertices are normalized   into that range
		hkAabb meshTreehkAabb = meshTree.domain;
		
/*		Point3f[][] vertices = new Point3f[meshTree.sections.length][];

		for (int s = 0; s < meshTree.sections.length; s++)
		{
			hkcdStaticMeshTreeBaseSection section = meshTree.sections[s];
			
			
			hkAabb sectionhkAabb = section.domain;
			
			vertices[s] = new Point3f[section.nodes.length];
			for (int i = 0; i < section.nodes.length; i++)
			{
				hkcdStaticTreeCodec3Axis4 hkcdStaticTreeCodec3Axis4 = section.nodes[i];
				
				
				
				//interesting parts of the section
				// len vertices in section 0 = 109 section 1 = 153
				int firstPackedVertex = section.firstPackedVertex; //0 in my section[0] and 43 in my section[1]
				int sharedVertices = section.sharedVertices.data; 	//43 and 3391
				int primitives = section.primitives.data; 			//55 and 14157 (I'm expecting 77 here actually)
				int dataRuns = section.dataRuns.data;				//1 and 257 
				int numPackedVertices  = section.numPackedVertices; //43 and 63
				int numSharedIndices = section.numSharedIndices;	//13 and 13
				// section 1 has leafIndex=1 and 1 has leafIndex=2
				
							
				
				//FIXME: presumably I need to multiply the bytesworth into the range max.x-min.x?? and possibly drop the CMD_VERT_SCALE?
				float CMD_VERT_SCALE_X = (sectionhkAabb.max.x - sectionhkAabb.min.x)/255; 
				float CMD_VERT_SCALE_Y = (sectionhkAabb.max.y - sectionhkAabb.min.y)/255; 
				float CMD_VERT_SCALE_Z = (sectionhkAabb.max.z - sectionhkAabb.min.z)/255; 
				vertices[s][i] = ConvertFromHavok.toJ3dP3f(//
						((hkcdStaticTreeCodec3Axis4.xyz[0]) * CMD_VERT_SCALE_X) + sectionhkAabb.min.x, //
						((hkcdStaticTreeCodec3Axis4.xyz[1]) * CMD_VERT_SCALE_Y) + sectionhkAabb.min.y, //
						((hkcdStaticTreeCodec3Axis4.xyz[2]) * CMD_VERT_SCALE_Z) + sectionhkAabb.min.z, nifVer);
			}
		}*/
		
	/*	int startIndex = 0;
		for (int pdr = 0; pdr < meshTree.primitiveDataRuns.length; pdr++)
		{
			int index = meshTree.primitiveDataRuns[pdr].index;
			int count = meshTree.primitiveDataRuns[pdr].count;
			
			
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
			gi.setCoordinates(vertices[pdr]);
			gi.setCoordinateIndices(listPoints);
			gi.setUseCoordIndexOnly(true);

			Shape3D shape = new Shape3D();
			shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
			shape.setAppearance(PhysAppearance.makeAppearance(new Color3f(0.75f, 1f, 1f)));
			group.addChild(shape);

			
			startIndex += count;//prep for next iter, needs to be by index above
		}
	*/

		//BOSBarricadePoles01.nif
		
		//primitives (only 1 but the run count is 2 so that may allocate them out) has length 132 of int[4] (enough for each run below)
		//why 4? are we always flat quads?, some of them are denigerate with repeated last index, so quads or tris, no worries
		//min index is 0 and max looks like 75
		//looks like I index them in 2? but how would that work?,  how do I index the odd verts?
				
				 
				 
		int[] sharedVerticesIndex = meshTree.sharedVerticesIndex;// then sharedVertexIndice of len 26 (which is 13+13?)
		//0 1 2 3 4 5 6 7 8 9 10 11 12 2 1 0 4 3 12 7 6 5 10 9 8 11
		
		
				
		//pack vertices(?) of len 106 (just a bunch of encoded ints)
		//sharedVertice len of 13 bunch of encoded ints
		//primitive data runs = 2 with (note this adds to the total of 132)
		//data run 0: index = 0, count = 55 <- this is half (-1) of the section 1 vert count of 109?
		//data run 1: index = 0, count = 77 <- this is half (-1) of the section 2 vert count of 153?

		
		Point3f[] vertices = new Point3f[meshTree.packedVertices.length];
		
		// start with section 0 aabb
		int currentSectionIdx = 0;
		int sectionNumPackedVertices = meshTree.sections[currentSectionIdx].numPackedVertices;
		hkAabb currentSectionhkAabb = meshTree.sections[currentSectionIdx].domain;		
		float[] codecParms = meshTree.sections[currentSectionIdx].codecParms;// parallel Algebraic Recursive Multilevel Solvers   
		
		
		// ok ok looks like a pole that need 16 verts has 19! in the sections world
		// but it definitely has 16 in packedVertices, so time to try to unpack that mystery
		for (int pvi = 0; pvi < meshTree.packedVertices.length; pvi++)
		{
			// time for the next section?
			if(pvi >= sectionNumPackedVertices) {
				currentSectionIdx++;
				sectionNumPackedVertices += meshTree.sections[currentSectionIdx].numPackedVertices;// notice += so we can do easy idx checks
				currentSectionhkAabb = meshTree.sections[currentSectionIdx].domain;
				
				// I notice a section codecParams appear to tbe the minimum for a single section, but somethign lese for the double
				// somehting like the min offsetty part?
				codecParms = meshTree.sections[currentSectionIdx].codecParms;
			}			
			
			int pv = meshTree.packedVertices[pvi];
			
			// definitely 10,11,11 no doubt!
			// values seen
			// x 0's , 1's
			// y 0's , 1's
			// z 0's , 1's, 00001101110=110dec, 11110010001=1937dec
			
			// this suggests the z value are what I'm calling x?? so is this zyx?

			
			
			//0x7FF or 0x3FF then bitshift			
//			int x = pv & 0x7FF; // 11bit
//			int y = (pv >> 11) & 0x7FF; // 11bit
//			int z = (pv >> 22) & 0x3FF; // 10bit
//			System.out.println("x " + x + " y " + y + " z " + z);
			float fx = (pv & 0x7FF) / 2047.0f; // 11bit
			float fy = ((pv >> 11) & 0x7FF) / 2047.0f; // 11bit
			float fz = ((pv >> 22) & 0x3FF) / 1023.0f; // 10bit
//			System.out.println("fx " + fx + " fy " + fy + " fz " + fz);
			 		
		
			
			
			float CMD_VERT_SCALE_X = (currentSectionhkAabb.max.x - currentSectionhkAabb.min.x); 
			float CMD_VERT_SCALE_Y = (currentSectionhkAabb.max.y - currentSectionhkAabb.min.y); 
			float CMD_VERT_SCALE_Z = (currentSectionhkAabb.max.z - currentSectionhkAabb.min.z); 
			vertices[pvi] = ConvertFromHavok.toJ3dP3f(//
					-((fx * CMD_VERT_SCALE_X) + currentSectionhkAabb.min.x), //
					-((fy * CMD_VERT_SCALE_Y) + currentSectionhkAabb.min.y), // notice -ve
					(fz * CMD_VERT_SCALE_Z) + currentSectionhkAabb.min.z, nifVer);
			
			
			// this is the single domain/aabb code using the overall aabb
/*			float CMD_VERT_SCALE_X = (meshTreehkAabb.max.x - meshTreehkAabb.min.x); 
			float CMD_VERT_SCALE_Y = (meshTreehkAabb.max.y - meshTreehkAabb.min.y); 
			float CMD_VERT_SCALE_Z = (meshTreehkAabb.max.z - meshTreehkAabb.min.z); 
			vertices[pvi] = ConvertFromHavok.toJ3dP3f(//
					(fx * CMD_VERT_SCALE_X) + codecParms[0],//meshTreehkAabb.min.x, //
					-((fy * CMD_VERT_SCALE_Y) + codecParms[1]),//meshTreehkAabb.min.y), // notice -ve
					(fz * CMD_VERT_SCALE_Z) + codecParms[2], nifVer);//meshTreehkAabb.min.z, nifVer);
	*/	
			
		}
		//TODO: the shared vertices appear to be very similar only it has longs rather than ints

		
		//I see my section domain are borken up into a sliver and then most of it
		// not base plate as 2 dims would differ
		/*
		aabb
        <hkparam name="domain">
            <hkobject class="hkAabb" name="domain" signature="0x4a948b16">
                <hkparam name="min">(-1.6002000570297241 -0.5715000033378601 -0.11430008709430695 1.0)</hkparam>
                <hkparam name="max">(1.6002000570297241 0.5715000629425049 1.1430000066757202 1.0)</hkparam>
            </hkobject>
        </hkparam>
	
sec 1
        <hkparam name="domain">
            <hkobject class="hkAabb" name="domain" signature="0x4a948b16">
                <hkparam name="min">(-1.6002000570297241 -0.5715000033378601 -0.11430008709430695 1.0)</hkparam>
                <hkparam name="max">(-1.1429998874664307 0.5715000629425049 1.1430000066757202 1.0)</hkparam>
            </hkobject>
        </hkparam>

sec 2
        <hkobject class="hkAabb" name="domain" signature="0x4a948b16">
            <hkparam name="min">(-1.4287506341934204 -0.5715000033378601 -0.11430008709430695 1.0)</hkparam>
            <hkparam name="max">(1.6002000570297241 0.5715000629425049 1.1430000066757202 1.0)</hkparam>
        </hkobject>
*/
	
 
			System.out.println("meshTree.sections.lengthmeshTree.sections.lengthmeshTree.sections.length " + meshTree.sections.length);
			
		// bum have to precount so we can allocate a index array
		int pointCount = 0;
		for (int p = 0; p < meshTree.primitives.length; p++)
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
		for (int p = 0; p < meshTree.primitives.length; p++)
		{		
			
			// for debug to only index one section
			//if(p>=meshTree.primitiveDataRuns[0].count)continue;
			
			
			//BOSBarricadePlate01.nif p34 is a degenerate non tri?
			//if(p==34 || p==87) {			System.out.println("87");		continue; //87 = 49,50,51,51			
			//}
			
			
			hkcdStaticMeshTreeBasePrimitive primitive = meshTree.primitives[p]; 
			int[] indices = primitive.indices;
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

		Shape3D shape = new Shape3D();
		shape.setGeometry(gi.getIndexedGeometryArray(COMPACT, BY_REF, INTERLEAVED, true, NIO));
		shape.setAppearance(PhysAppearance.makeAppearance(new Color3f(0.70f, 1f, 1f)));
		group.addChild(shape);

			
			 

		

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
