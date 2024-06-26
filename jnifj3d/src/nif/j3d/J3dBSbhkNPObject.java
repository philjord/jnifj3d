package nif.j3d;

import java.util.Iterator;

import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.IndexedLineArray;
import org.jogamp.java3d.J3DBuffer;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.java3d.TriangleStripArray;
import org.jogamp.java3d.geom.CylinderGenerator;
import org.jogamp.java3d.geom.GeometryData;
import org.jogamp.java3d.geom.SphereGenerator;
import org.jogamp.java3d.utils.geometry.GeometryInfo;
import org.jogamp.java3d.utils.shader.SimpleShaderAppearance;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Matrix4f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Tuple3f;
import org.jogamp.vecmath.Vector3d;
import org.jogamp.vecmath.Vector3f;
import org.jogamp.vecmath.Vector4f;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.collision.shapes.ShapeHull;
import com.bulletphysics.util.IntArrayList;
import com.bulletphysics.util.ObjectArrayList;

import nif.NifVer;
import nif.niobject.bs.BSbhkNPObject;
import nif.niobject.hkx.hkAabb;
import nif.niobject.hkx.hkBaseObject;
import nif.niobject.hkx.hkcdStaticMeshTreeBasePrimitive;
import nif.niobject.hkx.hknpBodyCinfo;
import nif.niobject.hkx.hknpCapsuleShape;
import nif.niobject.hkx.hknpCompoundShape;
import nif.niobject.hkx.hknpCompressedMeshShape;
import nif.niobject.hkx.hknpCompressedMeshShapeData;
import nif.niobject.hkx.hknpCompressedMeshShapeTree;
import nif.niobject.hkx.hknpConvexPolytopeShape;
import nif.niobject.hkx.hknpDynamicCompoundShape;
import nif.niobject.hkx.hknpPhysicsSystemData;
import nif.niobject.hkx.hknpScaledConvexShape;
import nif.niobject.hkx.hknpShape;
import nif.niobject.hkx.hknpShapeInstance;
import nif.niobject.hkx.hknpSphereShape;
import nif.niobject.hkx.hknpStaticCompoundShape;
import nif.niobject.hkx.reader.HKXContents;
import tools3d.utils.PhysAppearance;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromHavok;

/**
 * NOTE for trivial render only now, bullet does the hard work!
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
		if(contents != null) {
		
			Iterator<hkBaseObject> iter = contents.getContentCollection().iterator();
			if(iter.hasNext()) {
				// the first one had better be a system
				hknpPhysicsSystemData hknpPhysicsSystemData = (hknpPhysicsSystemData)iter.next();
					
				//physics bodies are here
				hknpBodyCinfo[] bodyCinfos = hknpPhysicsSystemData.bodyCinfos;
				for(int b = 0 ; b < bodyCinfos.length; b++) {
					hknpBodyCinfo bodyCinfo = bodyCinfos[b];
					
					long shapeId = bodyCinfo.shape;
					if(shapeId > 0) {
						hknpShape hknpShape = (hknpShape)contents.get(shapeId);
						if(hknpShape instanceof hknpConvexPolytopeShape) {
							Transform3D t = new Transform3D();						
							t.setRotation(ConvertFromHavok.toJ3d(bodyCinfo.orientation)); 	
			
							Vector3f pos = ConvertFromHavok.toJ3d(bodyCinfo.position, niToJ3dData.nifVer);
							// ok so the position wants to be the center of the polytopeshape, but my polytopeshape seem to be offset from 0,0,0
							// so I tell them to cetner at 0,0,0, but NOTE! not if the pos is 0,0,0
							
							addChild(createDebugPointShape(new Vector3f[]{pos}, new Color3f(1f,1f,1f)));
	
							t.setTranslation(pos);
							 
							TransformGroup lowerGroup = new TransformGroup(t);					
							lowerGroup.addChild(hknpConvexPolytopeShape((hknpConvexPolytopeShape)hknpShape, contents, pos.lengthSquared() != 0));
							addChild(lowerGroup);
							 
						} else {				
							addChild(processHknpShape(hknpShape, contents));						
						}
					}
				}
			} else {
				System.out.println("HKXContents contents is empty? odd");
			}
		}		
	}
	private static Node processHknpShape(hknpShape hknpShape, HKXContents contents) {
		return processHknpShape(hknpShape, contents, false);
	}
	private static Node processHknpShape(hknpShape hknpShape, HKXContents contents, boolean centerAtOrgin)
	{
		if (hknpShape instanceof hknpSphereShape) {
			return hknpSphereShape((hknpSphereShape) hknpShape, contents);
		} else if (hknpShape instanceof hknpCapsuleShape) {
			return hknpCapsuleShape((hknpCapsuleShape) hknpShape, contents);
		} else	if (hknpShape instanceof hknpDynamicCompoundShape) {
			return hknpCompoundShape((hknpDynamicCompoundShape) hknpShape, contents);
		} else	if (hknpShape instanceof hknpStaticCompoundShape) {
			return hknpCompoundShape((hknpDynamicCompoundShape) hknpShape, contents);
		} else if (hknpShape instanceof hknpScaledConvexShape) {
			return hknpScaledConvexShape((hknpScaledConvexShape) hknpShape, contents);
		} else if (hknpShape instanceof hknpConvexPolytopeShape) {
			return hknpConvexPolytopeShape((hknpConvexPolytopeShape)hknpShape, contents, centerAtOrgin);
		} else if (hknpShape instanceof hknpCompressedMeshShape) {
			hknpCompressedMeshShape hknpCompressedMeshShape = (hknpCompressedMeshShape)hknpShape;

			if (hknpCompressedMeshShape.data > 0) {
				hknpCompressedMeshShapeData hknpCompressedMeshShapeData = (hknpCompressedMeshShapeData)contents
						.get(hknpCompressedMeshShape.data);
				return hknpCompressedMeshShapeData(hknpCompressedMeshShapeData, contents);
			} else {
				//Meshes\Interiors\Utility\Doors\UtilMetalDbDoor01.nif
				System.out.println("no shape data for hknpCompressedMeshShape ");
			}
		} else {
			System.out.println("J3dbhkCollisionObject - unknown bhkShape " + hknpShape);
		}
		return null;
	}
	
	private static Node hknpScaledConvexShape(hknpScaledConvexShape data,  HKXContents contents)
	{

		long shapeId = data.coreShape;
		if(shapeId > 0) {
			hknpShape hknpShape = (hknpShape)contents.get(shapeId);
			TransformGroup transformGroup = new TransformGroup();
			Transform3D t3d = new Transform3D();
			Vector3f pos = ConvertFromHavok.toJ3d(data.position, nifVer);
			t3d.set(pos);
			
			//Note no ConvertFromHavok as these are just straight multipliers 
			t3d.setScale(new Vector3d(data.scale.x, data.scale.z, data.scale.y));

			transformGroup.setTransform(t3d);
			
			transformGroup.addChild(processHknpShape(hknpShape, contents, pos.lengthSquared() != 0));
			return transformGroup;
			
		}	
		return null;
		
	}
	
	private static Node hknpCompoundShape(hknpCompoundShape data, HKXContents contents)
	{
		Group g = new Group();
		for(int i = 0 ; i < data.instances.elements.length;i++) {
			hknpShapeInstance s = data.instances.elements[i];
			long shapeId = s.shape;
			if(shapeId > 0) {
				TransformGroup transformGroup = new TransformGroup();
				Transform3D t3d = new Transform3D();

				Matrix4f m = ConvertFromHavok.toJ3dM4(s.transform, nifVer);
				t3d.set(m);
				
				//Note no ConvertFromHavok as these are just straight multipliers 
				t3d.setScale(new Vector3d(s.scale.x, s.scale.z, s.scale.y));

				transformGroup.setTransform(t3d);
				
				hknpShape hknpShape = (hknpShape)contents.get(shapeId);
				transformGroup.addChild(processHknpShape(hknpShape, contents));
				g.addChild(transformGroup);				
			}			
		}
		return g;
	}
	
	private static Shape3D hknpSphereShape(hknpSphereShape data, HKXContents contents)
	{
		//System.out.println("just out of interest vertices for sphere " +data.convexRadius+ " " + data.vertices[0]+ " " + data.vertices[1]+ " " + data.vertices[2]);
		//just out of interest vertices for sphere 0.29049572 [NPVector4] x:2.3510652E-38 y:0.29049572 z:2.664476E24 w:0.0 [NPVector4] x:0.0 y:0.0 z:0.0 w:0.0 [NPVector4] x:1.469374E-39 y:0.0 z:0.0 w:0.0
		// so vertices[0].y is the radius, and the others are weird numbers
		
		float radius = ConvertFromHavok.toJ3d(data.convexRadius, nifVer);
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



	private static Group hknpCapsuleShape(hknpCapsuleShape data, HKXContents contents)
	{
		//TODO: try JBullet CapsuleShapeX
		Group g = new Group();

		float radius = ConvertFromHavok.toJ3d(data.convexRadius, nifVer);
		Vector3f v1 = ConvertFromHavok.toJ3d(data.a, nifVer);
		float radius1 = ConvertFromHavok.toJ3d(data.convexRadius, nifVer);
		Vector3f v2 = ConvertFromHavok.toJ3d(data.b, nifVer);
		float radius2 = ConvertFromHavok.toJ3d(data.convexRadius, nifVer);

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

	public static Node hknpConvexPolytopeShape(hknpConvexPolytopeShape data, HKXContents contents) {
		return hknpConvexPolytopeShape(data, contents, false);		
	}
	public static Node hknpConvexPolytopeShape(hknpConvexPolytopeShape data, HKXContents contents, boolean centerAtOrgin) {		
		Group group = new Group();	
				
		/*Point3f[] vertices = new Point3f[data.vertices.length];
		for (int i = 0; i < data.vertices.length; i++) {
			vertices[i] = new Point3f(ConvertFromHavok.toJ3dP3f(data.vertices[i], nifVer));
		}		
		group.addChild(createDebugPointShape(vertices, new Color3f(0.0f, 0.5f, 0.3f)));
		Point3f[] vertices2 = new Point3f[data.planes.length];
		for (int i = 0; i < data.planes.length; i++) {
			vertices2[i] = new Point3f(ConvertFromHavok.toJ3dP3f(data.planes[i], nifVer));
		}	
		group.addChild(createDebugPointShape(vertices2, new Color3f(1.0f,0.5f,0.3f)));
		*/
		
		// ok vertices and planes, I'm see for a box which wants 8 vertexs I see oddly, very oddly, 
		//vertices 3 oddly close to 0,0,0 then 4 good verts, planes, 4 that look like verts almost and 3 that are
		// like a plane, normal and distance (e.g. 0,0,1,-0.16)		
		// like 3 odds to start and 3 odds to finish
		// so this 3 thingy below works
		
		
		// super suspect these first 3 are a bunch of flags, notice very diff poly topes (boxes) same numbers 
		//ftoi xyzw 0: 0, 0, 0, 0
		//ftoi xyzw 1: 001000000000000000001000, 0, 0, 0
		//ftoi xyzw 2: 100100000000000000001000, 1000011000000000000000110, 1001010000000000000011000, 0
		//and 
		//ftoi xyzw 0 0, 0, 0, 0
		//ftoi xyzw 1 001000000000000000001000, 0, 0, 0
		//ftoi xyzw 2 100100000000000000001000, 1000011000000000000000110, 1001010000000000000011000, 0
		
		/*for (int i = 0; i < 3; i++) {
			//System.out.println("ftoi "+i+" " + Float.floatToIntBits(data.vertices[i].x) );
			System.out.println("ftoi xyzw "+i+": " + Integer.toBinaryString( Float.floatToIntBits(data.vertices[i].x))
			+ ", "+ Integer.toBinaryString( Float.floatToIntBits(data.vertices[i].y))
			+ ", "+ Integer.toBinaryString( Float.floatToIntBits(data.vertices[i].z))
			+ ", "+ Integer.toBinaryString( Float.floatToIntBits(data.vertices[i].w)));
		}*/

		/*float hs = ConvertFromHavok.getHavokScale(nifVer);
		for (int i = 0; i < data.vertices.length; i++) {			
			 Vector4f v= new Vector4f(data.vertices[i].x * hs ,   data.vertices[i].z * hs ,  -data.vertices[i].y * hs, data.vertices[i].w );
			System.out.println("vi " + i + "" + v);
		}for (int i = 0; i < data.planes.length; i++) {
			Vector4f p= new Vector4f(data.planes[i].x * hs ,   data.planes[i].z * hs ,  -data.planes[i].y * hs, data.planes[i].w );
			System.out.println("pi " + i + "" + p);
		} */
	
		
		ObjectArrayList<Vector3f> points = new ObjectArrayList<Vector3f>();
		
		// used if the centering is required below
		Vector3f min = new Vector3f(Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE);
		Vector3f max = new Vector3f(Float.MIN_VALUE,Float.MIN_VALUE,Float.MIN_VALUE);
		if( data.vertices.length > 3 && data.planes.length > 3) { 			
			for (int i = 3; i < data.vertices.length; i++) {
				Vector3f v = new Vector3f(ConvertFromHavok.toJ3dP3f(data.vertices[i], nifVer));
				if(centerAtOrgin) {
					min.x = v.x < min.x ? v.x : min.x;
					max.x = v.x > max.x ? v.x : max.x;
					min.y = v.y < min.y ? v.y : min.y;
					max.y = v.y > max.y ? v.y : max.y;
					min.z = v.z < min.z ? v.z : min.z;
					max.z = v.z > max.z ? v.z : max.z;
				}
				points.add(v);
			}
			for (int i = 0; i < data.planes.length - 3; i++) {
				Vector3f p = new Vector3f(ConvertFromHavok.toJ3dP3f(data.planes[i], nifVer));
				if(centerAtOrgin) {
					min.x = p.x < min.x ? p.x : min.x;
					max.x = p.x > max.x ? p.x : max.x;
					min.y = p.y < min.y ? p.y : min.y;
					max.y = p.y > max.y ? p.y : max.y;
					min.z = p.z < min.z ? p.z : min.z;
					max.z = p.z > max.z ? p.z : max.z;
				}
				points.add(p);
			}
		} else {
			System.out.println("Interesting hknpConvexPolytopeShape " + data.vertices.length + " " + data.planes.length);
		}

		if(centerAtOrgin) {
			Vector3f mod = new Vector3f(((max.x+min.x)/2f), ((max.y+min.y)/2f), ((max.z+min.z)/2f));		
			for(int  i = 0; i < points.size(); i++) {
				points.get(i).sub(mod);
			}
		}
	
		
		ConvexHullShape convexShape = new ConvexHullShape(points);
		// create a hull approximation
		ShapeHull hull = new ShapeHull(convexShape);
		float margin = convexShape.getMargin();
		hull.buildHull(margin);

		if (hull.numTriangles() > 0) {

			IntArrayList idx = hull.getIndexPointer();
			ObjectArrayList<Vector3f> vtx = hull.getVertexPointer();

			int[] coordIndices = new int[hull.numIndices()];
			for (int i = 0; i < hull.numIndices(); i++) {
				coordIndices[i] = idx.get(i);
			}

			Point3f[] coords = new Point3f[hull.numVertices()];
			for (int i = 0; i < hull.numVertices(); i++) {
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
			for (int pvi = 0; pvi < meshTree.sharedVertices.length; pvi++) {			
				long pv = meshTree.sharedVertices[pvi];			
			
				//z value are what I'm calling x, so is this is in zyx format, 10, 11, 11 bit			 
				//21bit x, 21 bit y, 22 bit z this time!
				float fx = (((pv >> 0) & 0x1FFFFF)/(float)0x1FFFFF * (meshTreehkAabb.max.x-meshTreehkAabb.min.x)) + meshTreehkAabb.min.x;
				float fy = (((pv >> 21) & 0x1FFFFF)/(float)0x1FFFFF * (meshTreehkAabb.max.y-meshTreehkAabb.min.y)) + meshTreehkAabb.min.y;
				float fz = (((pv >> 42) & 0x3FFFFF)/(float)0x3FFFFF * (meshTreehkAabb.max.z-meshTreehkAabb.min.z)) + meshTreehkAabb.min.z;
			
				sharedVertices[pvi] = ConvertFromHavok.toJ3dP3f(fx, fy, fz, nifVer);	
			}		
		}
		
		for (int s = 0; s < meshTree.sections.length; s++) {			
			// the current value is the lowest byte and the previous in the next short(?) up	
			int primitivesCount = meshTree.sections[s].primitives.data & 0xff;
			int primitivesOffset = (meshTree.sections[s].primitives.data) >> 8 & 0xffff;
			//int sharedCount = meshTree.sections[s].sharedVertices.data & 0xff;
			int sharedOffset = (meshTree.sections[s].sharedVertices.data) >> 8 & 0xffff;
						
			
			int firstPackedVertex = meshTree.sections[s].firstPackedVertex;
			int numPackedVertices = meshTree.sections[s].numPackedVertices;
			//int numSharedIndices = meshTree.sections[s].numSharedIndices;			
			//hkAabb currentSectionhkAabb = meshTree.sections[currentSectionIdx].domain;	
			float[] codecParms = meshTree.sections[s].codecParms;// parallel Algebraic Recursive Multilevel Solvers?   
			
			Point3f[] vertices = new Point3f[numPackedVertices + sharedVertices.length];
			
			for(int pvi = 0; pvi < numPackedVertices; pvi++) {

				int pv = meshTree.packedVertices[firstPackedVertex + pvi];			
			
				//z value are what I'm calling x, so is this is in zyx format, 10, 11, 11 bit			
				
				// Normalized would look like this, but as the normalization dividor is accounted for in the param scale we don't need it
				//float fx = (pv & 0x7FF) / 2047.0f; // 11bit
				//float fy = ((pv >> 11) & 0x7FF) / 2047.0f; // 11bit
				//float fz = ((pv >> 22) & 0x3FF) / 1023.0f; // 10bit
				// multiplised by the param scale, and offset added 
				float fx = (((pv >> 0) & 0x7FF) * codecParms[3]) + codecParms[0];
				float fy = (((pv >> 11) & 0x7FF) * codecParms[4]) + codecParms[1];
				float fz = (((pv >> 22) & 0x3FF) * codecParms[5]) + codecParms[2];
				
				vertices[pvi] = ConvertFromHavok.toJ3dP3f(fx, fy, fz, nifVer);		
			}
						
			//TODO: all shared are copied to the end (if any), bit poor in efficiency, shorten later
			for(int i =0 ; i< sharedVertices.length;i++) {
				vertices[numPackedVertices+i] = sharedVertices[i];					 
			}		
			
			//group.addChild(createDebugPointShape(vertices, new Color3f(0.0f,1f-((float)s/(float)meshTree.primitiveDataRuns.length), ((float)s/(float)meshTree.primitiveDataRuns.length))));
			
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

								 				
				try {
					
					int[] sharedVerticesIndex = meshTree.sharedVerticesIndex;	
					if(sharedVerticesIndex != null) {
						// if any of the indices go beyond numPackedVertices then the distance beyond
						// is used as an index into the sharedVerticesIndex, starting at the <sharedOffset> for this section
						// each section has numSharedIndices of the sharedVerticesIndex as it's own
						// the index found is then used as an index into the shared vertex area of the vertices, which for now is at the end starting at 
						// numPackedVertices, but when a single packed and shared vertex array is used will be at the end of all packed vertices
															 				
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
					} else {
						//quad?
						//FIXME: what does an over sized index but no shared vertexs mean?
						if(indices[2] != indices[3]) {
							listPoints[idx++] = indices[0] < numPackedVertices ? indices[0] : 0;
							listPoints[idx++] = indices[1] < numPackedVertices ? indices[1] : 0;
							listPoints[idx++] = indices[2] < numPackedVertices ? indices[2] : 0;
							listPoints[idx++] = indices[2] < numPackedVertices ? indices[2] : 0;
							listPoints[idx++] = indices[3] < numPackedVertices ? indices[3] : 0;
							listPoints[idx++] = indices[0] < numPackedVertices ? indices[0] : 0;
						} else {//just a tri					
							listPoints[idx++] = indices[0] < numPackedVertices ? indices[0] : 0;
							listPoints[idx++] = indices[1] < numPackedVertices ? indices[1] : 0;
							listPoints[idx++] = indices[2] < numPackedVertices ? indices[2] : 0;
						}
					}				
					
				} catch(ArrayIndexOutOfBoundsException e) {
					System.out.println("J3dBSbhkNPObject ArrayIndexOutOfBoundsException " + e.getMessage());
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
	
	
	public static Shape3D createDebugPointShape(Tuple3f[] vertices, Color3f color) {
		///////////////////////////////////////////////////////////////Lovely little point drawer thing!
		int gaVertexCount = vertices.length * 6;// 4 points a crossed pair of lines
		IndexedLineArray ga = new IndexedLineArray(gaVertexCount,
		GeometryArray.BY_REFERENCE | GeometryArray.COORDINATES  
		| GeometryArray.BY_REFERENCE_INDICES | GeometryArray.USE_COORD_INDEX_ONLY,
		gaVertexCount);
		
		int[] gaCoordIndices = new int[gaVertexCount];
		//fixed for all time, recall these are points
		for (int i = 0; i < gaVertexCount; i++) {
		gaCoordIndices[i] = i;
		}
		
		float[] gaCoords = new float[gaVertexCount * 3];
		for (int i = 0; i < gaVertexCount/6; i++) {
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
