package nif.j3d.particles;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Random;

import org.jogamp.java3d.Group;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriBasedGeomData;
import nif.niobject.NiTriShapeData;
import nif.niobject.NiTriStripsData;
import nif.niobject.particle.NiPSysMeshEmitter;
import utils.convert.ConvertFromNif;

/**
 * <niobject name="NiPSysMeshEmitter" abstract="0" inherit="NiPSysEmitter" ver1="20.0.0.5">
 * 
 * Particle emitter that uses points on a specified mesh to emit from.
 * 
 * <add name="Num Emitter Meshes" type="uint"> The number of references to emitter meshes that follow. </add>
 * <add name="Emitter Meshes" type="Ref" template="NiTriBasedGeom" arr1="Num Emitter Meshes">Links to meshes used for emitting.</add> 
 * <add name="Initial Velocity Type" type="VelocityType"> The way the particles get their initial direction and speed. </add> 
 * <add name="Emission Type" type="EmitFrom"> The parts of the mesh that the particles emit from. </add> 
 * <add name="Emission Axis" type="Vector3">The emission axis.</add> </niobject>
 * 
 * EMIT_FROM_VERTICES Emit from a random vertex in the mesh. 
 * EMIT_FROM_FACE_CENTER Emit from the center of a random triangle in the mesh. 
 * EMIT_FROM_EDGE_CENTER Emit from the center of a random edge of a random triangle in the mesh.
 * EMIT_FROM_FACE_SURFACE Emit from a random position inside a random triangle in the mesh. 
 * EMIT_FROM_EDGE_SURFACE Emit from a random position along a random edge of a random triangle in the mesh. 
 * EMIT_MAX An invalid value indicating the maximum number of defined MeshEmissionType enumerations.
 * 
 * VELOCITY_USE_NORMALS Use the vertex/triangle normals to determine the velocity direction. 
 * VELOCITY_USE_RANDOM Use a random velocity direction.
 * VELOCITY_USE_DIRECTION Use the direction specified in the base emitter as the velocity direction. 
 * VELOCITY_MAX An invalid value indicating the maximum number of defined InitialVelocityType enumerations.
 * 
 */

public class J3dNiPSysMeshEmitter extends J3dNiPSysEmitter {

	enum EMIT_TYPE {
		EMIT_FROM_VERTICES, EMIT_FROM_FACE_CENTER, EMIT_FROM_EDGE_CENTER, EMIT_FROM_FACE_SURFACE, EMIT_FROM_EDGE_SURFACE, EMIT_MAX
	};

	enum VELOCITY_TYPE {
		VELOCITY_USE_NORMALS, VELOCITY_USE_RANDOM, VELOCITY_USE_DIRECTION, VELOCITY_MAX
	};

	private J3dNiAVObject					root;

	private ArrayList<J3dNiTriBasedGeom>	j3dNiTriBasedGeoms	= new ArrayList<J3dNiTriBasedGeom>();
	private ArrayList<NiTriBasedGeomData>	datas				= new ArrayList<NiTriBasedGeomData>();

	private int								initialVelocityType;

	private int								emissionType;

	private Vector3f						emissionAxis;

	public J3dNiPSysMeshEmitter(NiPSysMeshEmitter niPSysMeshEmitter, NiToJ3dData niToJ3dData) {
		super(niPSysMeshEmitter, niToJ3dData);

		root = niToJ3dData.getJ3dRoot();

		for (int i = 0; i < niPSysMeshEmitter.numEmitterMeshes; i++) {
			NiTriBasedGeom niTriBasedGeom = (NiTriBasedGeom)niToJ3dData.get(niPSysMeshEmitter.emitterMeshes[i]);
			J3dNiTriBasedGeom j3dNiTriBasedGeom = (J3dNiTriBasedGeom)niToJ3dData.get(niTriBasedGeom);
			NiTriBasedGeomData data = (NiTriBasedGeomData)niToJ3dData.get(niTriBasedGeom.data);

			if ((data instanceof NiTriStripsData) || (data instanceof NiTriShapeData)) {
				j3dNiTriBasedGeoms.add(j3dNiTriBasedGeom);
				datas.add(data);

				//make the caps correct up to the root
				Group g = j3dNiTriBasedGeom;
				while (g != null) {
					g.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
					g.setCapability(Group.ALLOW_PARENT_READ);
					if (g == niToJ3dData.getJ3dRoot())
						break;
					g = (Group)g.getParent();
				}
			} else {
				System.out.println("J3dNiPSysMeshEmitter unhandled emitter geom type " + data);
			}
		}

		this.initialVelocityType = niPSysMeshEmitter.initialVelocityType;
		this.emissionType = niPSysMeshEmitter.emissionType;
		this.emissionAxis = ConvertFromNif.toJ3dNoScale(niPSysMeshEmitter.emissionAxis);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysMeshEmitter");
			System.out.print(" numEmitterMeshes " + j3dNiTriBasedGeoms.size());
			if (j3dNiTriBasedGeoms.size() > 0)
				System.out.print(" j3dNiTriBasedGeoms[0] " + j3dNiTriBasedGeoms.get(0));
			System.out.print(" initialVelocityType " + VELOCITY_TYPE.values()[initialVelocityType].name());
			System.out.print(" emissionType " + EMIT_TYPE.values()[emissionType].name());
			System.out.println(" emissionAxis " + emissionAxis);

			// it might be a delightful thing to show the emitter mesh about now?? like outlined type thing?
		}
	}

	private Random		rnd		= new Random();
	//deburner
	private Transform3D	trans	= new Transform3D();

	@Override
	protected void getCreationPoint(Point3f pos, Vector3f vel) {
		//FIXME:!!!  only first cut vertices

		// example data *********** NEW file Meshes\Furniture\Clutter\BlacksmithSharpeningWheelAnimating.nif
		//J3dNiPSysEmitter parent of next Emitter
		// speed 150.0 speedVariation 135.0 declination 0.0 declinationVariation 0.2617994 planarAngle 0.0 planarAngleVariation 0.2617994 
		//initialColor (1.0, 1.0, 1.0, 1.0) initialRadius 1.0 radiusVariation 0.25 lifeSpan 0.33333334 lifeSpanVariation 0.33333334
		// J3dNiPSysMeshEmitter numEmitterMeshes 1 j3dNiTriBasedGeoms[0] [J3dNiTriShape]  
		//initialVelocityType VELOCITY_USE_NORMALS emissionType EMIT_FROM_FACE_SURFACE 
		//emissionAxis [NifVector3] 1.0 0.0 0.0

		if (!j3dNiParticleSystem.worldSpace) {
			System.err.println("nonono emitter fail not woprldSpace! but can be fixed no panic");
			//root = niToJ3dData.getJ3dRoot(); would need to just be this.j3dNiParticleSystem
		}

		if (datas.size() > 0) {
			int idx = rnd.nextInt(datas.size());
			NiTriBasedGeomData data = datas.get(idx);
			J3dNiTriBasedGeom j3dNiTriBasedGeom = j3dNiTriBasedGeoms.get(idx);

			// these are important for faces and possibly edges but not the trivial vertexes I'm doing now
			if (data instanceof NiTriStripsData) {
				// short numStrips;
				// int[] stripLengths; //potentially over half a short
				// boolean hasPoints;
				// int[][] points;
			} else if (data instanceof NiTriShapeData) {
				// boolean hasTriangles = true;
				// int[] trianglesOpt;
			}

			// get from the emitter up to the to root
			j3dNiTriBasedGeom.getTreeTransform(trans, root);

			FloatBuffer coords = data.verticesOptBuf;

			

			idx = rnd.nextInt(coords.limit() / 3);
			pos.set(coords.get(idx * 3 + 0), coords.get(idx * 3 + 1), coords.get(idx * 3 + 2));
			trans.transform(pos);
			
			FloatBuffer normals = data.normalsOptBuf;
			// TODO: black smith has VELOCITY_USE_NORMALS but null normals
			if (initialVelocityType == VELOCITY_TYPE.VELOCITY_USE_NORMALS.ordinal() && normals != null) {
				
				vel.set(normals.get(idx * 3 + 0), normals.get(idx * 3 + 1), normals.get(idx * 3 + 2));
				
				//TODO: transform of a Vector3f should be fine for normals
				trans.transform(vel);
			}
			// FIXME: testy just for blacksmith
			vel.set(0,1,1f);
			trans.transform(vel);		

		}
	}
}
