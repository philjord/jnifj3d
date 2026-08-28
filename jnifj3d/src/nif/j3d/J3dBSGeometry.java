package nif.j3d;

import java.io.IOException;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.GeometryArray;
import org.jogamp.java3d.IndexedGeometryArray;
import org.jogamp.java3d.IndexedTriangleArray;
import org.jogamp.java3d.J3DBuffer;
import org.jogamp.java3d.Shape3D;
import org.jogamp.java3d.TransparencyAttributes;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;

import nif.appearance.NiGeometryAppearance;
import nif.niobject.bs.BSGeometry;
import nif.niobject.bs.BSMeshData;
import tools.WeakValueHashMap;
import tools3d.utils.AppearanceFactory;
import tools3d.utils.scenegraph.Fadable;
import utils.convert.ConvertFromNif;
import utils.source.MeshSource;
import utils.source.TextureSource;


//This guy looks like a mesh from star field, but it has weights so morphable to coming back at me!

//this is a copy of J3dBSTriShape extends J3dNiTriBasedGeom

// No fader appearance, just dummy interfaces
// no outlines at all, just dummy interfaces
// no morphing meshes at all


public class J3dBSGeometry extends J3dNiAVObject implements Fadable
{

	private Appearance normalApp;

	private Shape3D shape;

	private TransparencyAttributes normalTA = null;
	
	protected GeometryArray baseGeometryArray;
	
	
	/**
		J3dLODNode is the lod node guy
	 * @param bsTriShape
	 * @param niToJ3dData
	 * @param textureSource
	 */
	public J3dBSGeometry(BSGeometry bsGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(bsGeometry, niToJ3dData);
	
		shape = new Shape3D();
		addChild(shape);
		
		shape.setName("" + this.getClass().getSimpleName() + ":" + bsGeometry.name);
		
		niToJ3dData.put(bsGeometry, this);
		if (textureSource != null)	{
			normalApp = ((NiGeometryAppearance) AppearanceFactory.currentAppearanceFactory).configureAppearance(bsGeometry, niToJ3dData,
					textureSource, shape, this);
		}

		//BSgeom has bone weights but I can't see skin
		//am I a skin shape in which case I need to be uncompacted ready for animation
		if (bsGeometry.Skin.ref != -1)
		{
			System.out.println("woah skin");
	//		makeMorphable();
		}
	//	else
		{
			baseGeometryArray = createGeometry();
			getShape().setGeometry(baseGeometryArray);
			if (J3dNiTriBasedGeom.USE_FIXED_BOUNDS)
			{
				getShape().setBoundsAutoCompute(false);// expensive to do regularly so animated node just get one
				//TODO: possibly bounding box rather than bounding sphere?
				
				Point3d c = ConvertFromNif.toJ3dP3d(bsGeometry.BoundingSphere.Center);
				c.scale(STF_TRANS_SCALE);
				float r = ConvertFromNif.toJ3d(bsGeometry.BoundingSphere.Radius);
				r *= STF_TRANS_SCALE;
				
				getShape().setBounds(new BoundingSphere(c, r));
			}
		}
	}
	

	public Shape3D getShape()
	{
		return shape;
	}
		
	public GeometryArray getBaseGeometryArray()
	{
		return baseGeometryArray;
	}
	
	protected IndexedGeometryArray createGeometry()
	{
		return createGeometry((BSGeometry) this.niAVObject);
	}
	

	//Note self expunging cache
	protected static WeakValueHashMap<Object, IndexedGeometryArray> sharedIGAs = new WeakValueHashMap<Object, IndexedGeometryArray>();


	private static void fillIn(GeometryArray ga, BSMeshData data) {

		
		ga.setCoordRefBuffer(new J3DBuffer(data.verticesOptBuf));

		if (data.normalsOptBuf != null) 
			ga.setNormalRefBuffer(new J3DBuffer(data.normalsOptBuf));

		if (data.colorsOptBuf != null)
			ga.setColorRefBuffer(new J3DBuffer(data.colorsOptBuf));

		if (data.uVSetOptBuf != null) 
			ga.setTexCoordRefBuffer(0, new J3DBuffer(data.uVSetOptBuf));
		
		if (data.uVSet2OptBuf != null) 
			ga.setTexCoordRefBuffer(1, new J3DBuffer(data.uVSet2OptBuf));
		

		if (data.normalsOptBuf != null && data.tangentsOptBuf != null && BSMeshData.TANGENTS) 
			ga.setVertexAttrRefBuffer(0, new J3DBuffer(data.tangentsOptBuf));

	}

 
	public static IndexedGeometryArray createGeometry(BSGeometry bsGeometry) {

		IndexedGeometryArray iga = sharedIGAs.get(bsGeometry);

		if (iga != null) {
			return iga;
		}

		try {
			//TODO: I'm just going to load up mesh[0] which is closest LOD and should always be HasMesh==1
			BSMeshData data;

			if (bsGeometry.Meshes[0].Mesh.MeshData == null) {
				data = new BSMeshData(
						MeshSource.meshSource.getByteBuffer("geometries\\" + bsGeometry.Meshes[0].Mesh.MeshPath + ".mesh"));
				// must assign it so the shader factory can examine the contents
				bsGeometry.Meshes[0].Mesh.MeshData = data;
			} else {
				data = bsGeometry.Meshes[0].Mesh.MeshData;
			}
			

			if (data.IndicesSize > 0) {
				// All tex units use the 0ith , all others are ignored
				int texCoordCount = data.uVSet2OptBuf != null ? 2 : 1;
				int[] texMap = new int[9];
				for (int i = 0; i < 9; i++)
					texMap[i] = (data.uVSet2OptBuf != null && i == 1) ? 1 : 0;

				int vertexFormat = 0;

				vertexFormat = GeometryArray.COORDINATES //
								| (data.normalsOptBuf != null ? GeometryArray.NORMALS : 0) //
								| (data.uVSetOptBuf != null ? GeometryArray.TEXTURE_COORDINATE_2 : 0) //
								| (data.colorsOptBuf != null ? GeometryArray.COLOR_4 : 0) //
								| GeometryArray.USE_COORD_INDEX_ONLY //
								| ((J3dNiTriBasedGeom.BUFFERS) ? GeometryArray.BY_REFERENCE_INDICES : 0)//				
								| ((J3dNiTriBasedGeom.BUFFERS) ? GeometryArray.BY_REFERENCE : 0)//
								| ((J3dNiTriBasedGeom.BUFFERS) ? GeometryArray.USE_NIO_BUFFER : 0) //
								| ((data.normalsOptBuf != null
									&& data.tangentsOptBuf != null) ? GeometryArray.VERTEX_ATTRIBUTES : 0);

				if (data.normalsOptBuf != null && data.tangentsOptBuf != null && BSMeshData.TANGENTS) {
					iga = new IndexedTriangleArray(data.NumVerts, vertexFormat, texCoordCount, 
							texMap, 1, new int[] {3},
							data.IndicesSize);
				} else {
					iga = new IndexedTriangleArray(data.NumVerts, vertexFormat, texCoordCount,
							texMap, data.IndicesSize);
				}

				if (J3dNiTriBasedGeom.BUFFERS)
					iga.setCoordIndicesRef(data.trianglesOpt);
				else
					iga.setCoordinateIndices(0, data.trianglesOpt);

				fillIn(iga, data);

				sharedIGAs.put(bsGeometry, iga);

				return iga;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	

	@Override
	public void fade(float percent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOutline(Color3f c) {
		// TODO Auto-generated method stub
		
	}

}