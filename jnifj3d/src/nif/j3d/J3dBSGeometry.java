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
// no morphing meshes ata ll


public class J3dBSGeometry extends J3dNiAVObject implements Fadable
{

	private Appearance normalApp;

	private Shape3D shape;

	private TransparencyAttributes normalTA = null;
	
	protected GeometryArray baseGeometryArray;
	
	
	/**
	 *  so BSGeom that I've seen has a shader prop but the one I saw had no alpha
	 *  none f te ones I saw had a MeshData they all pointed at meshpath
	 *  examples
	 *  
	 *  ShaderProperty 10 AlphaProperty -1
MeshPAth = ce04a383d77ed8024962\88240de2a57125a07f5d
MeshPAth = 0d1f52439e4be388b685\6086a00fa49e1ceaf40a
MeshPAth = eb0e4b7e5a7a3ff560be\45507f1921761cef1eaa

ShaderProperty 30 AlphaProperty -1
MeshPAth = dae59cbdace4fb083392\6e2370be5cb8b5024bb9
MeshPAth = dae59cbdace4fb083392\6e2370be5cb8b5024bb9
MeshPAth = dae59cbdace4fb083392\6e2370be5cb8b5024bb9
MeshPAth = 68cc3f60d3352211498b\0fdcd0c123d31ea0eff3
	 * 
	 * ShaderProperty 6 AlphaProperty -1
MeshPAth = 5c4368417859d8d7357e\24c2f45b93e75e5ffefc
MeshPAth = fd81ab9e3dc21482837e\cddd93c1297ff38efee4
MeshPAth = fd81ab9e3dc21482837e\cddd93c1297ff38efee4
MeshPAth = 8a08403691e164f4493b\5742fed8402ad3f82dbe
h lods
	 * 
	 * ShaderProperty 24 AlphaProperty -1
MeshPAth = 569caf4c101df55098d9\8925d29384ee10a719dd


	 * I see lots with 3 or 2 repeats, so perhaps each one means something? LOd or something?
	 * always 1 first then 2 etc, nne skipped only foreshortened 
	 * going down in verts super suggests Lodding
	 * 
	 * small count of verts less liekly to have more mes




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
				getShape().setBounds(new BoundingSphere(ConvertFromNif.toJ3dP3d(bsGeometry.BoundingSphere.Center), ConvertFromNif.toJ3d(bsGeometry.BoundingSphere.Radius)));
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

		if (data.uVSetOptBuf != null) {
			ga.setTexCoordRefBuffer(0, new J3DBuffer(data.uVSetOptBuf));
		}

		if (data.normalsOptBuf != null && data.tangentsOptBuf != null && BSMeshData.TANGENTS) {
			ga.setVertexAttrRefBuffer(0, new J3DBuffer(data.tangentsOptBuf));
			//ga.setVertexAttrRefBuffer(1, new J3DBuffer(data.binormalsOptBuf));
		}

	}

 
	public static IndexedGeometryArray createGeometry(BSGeometry bsGeometry) {

		IndexedGeometryArray iga = sharedIGAs.get(bsGeometry);

		if (iga != null) {
			return iga;
		}

		try {
			//TODO: I'm just going to load up mesh[0] which is closest LOD and should always be HsaMesh==1
			BSMeshData data;

			if (bsGeometry.Meshes[0].Mesh.MeshData == null) {
				data = new BSMeshData(
						MeshSource.meshSource.getByteBuffer("geometries\\" + bsGeometry.Meshes[0].Mesh.MeshPath));
				// must assign it so the shader factory can examine the contents
				bsGeometry.Meshes[0].Mesh.MeshData = data;
			} else {
				data = bsGeometry.Meshes[0].Mesh.MeshData;
			}

			if (data.IndicesSize > 0) {
				// All tex units use the 0ith , all others are ignored
				int[] texMap = new int[9];
				for (int i = 0; i < 9; i++)
					texMap[i] = 0;

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
					iga = new IndexedTriangleArray(data.NumVerts, vertexFormat, 1, texMap, 1, new int[] {3},
							data.IndicesSize);
				} else {
					iga = new IndexedTriangleArray(data.NumVerts, vertexFormat, 1, texMap, data.IndicesSize);
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