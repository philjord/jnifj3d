package nif.j3d.particles;

import org.jogamp.java3d.Group;
import org.jogamp.java3d.Node;
import org.jogamp.java3d.Transform3D;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.particle.NiPSysGravityModifier;
import utils.convert.ConvertFromNif;

/**
 * <niobject name="NiPSysGravityModifier" abstract="0" inherit="NiPSysModifier" ver1="10.1.0.0">
 * 
 * Adds gravity to a particle system, when linked to a NiNode to use as a Gravity Object.
 * 
 * <add name="Gravity Object" type="Ptr" template="NiNode">Refers to a NiNode for gravity location.</add>
 * <add name="Gravity Axis" type="Vector3">Orientation of gravity.</add> 
 * <add name="Decay" type="float">Falloff range.</add> 
 * <add name="Strength" type="float">The strength of gravity.</add>
 * <add name="Force Type" type="ForceType">Planar or Spherical type</add> 
 * <add name="Turbulence" type="float">Adds a degree of randomness.</add> 
 * <add name="Turbulence Scale" type="float" default="1.0">Range for turbulence.</add>
 * <add name="Unknown Byte" type="byte" ver1="20.2.0.7" userver="11">Unknown</add> </niobject>
 */
public class J3dNiPSysGravityModifier extends J3dNiPSysModifier {

	private J3dNiNode	gravityJ3dNiNode;

	// we use this so all the transforms on the NiNode above are taken into account properly
	private Group		gravityNode	= new Group();

	private Vector3f	gravityAxis;

	private float		decay;						//Decay = NiExp(Decay* Distance), not can be 0 if so just constant strength 

	private float		strength;

	private int			forceType;

	private float		turbulence;

	private float		turbulenceScale;

	public J3dNiPSysGravityModifier(NiPSysGravityModifier niPSysGravityModifier, NiToJ3dData niToJ3dData) {
		super(niPSysGravityModifier, niToJ3dData);
		gravityJ3dNiNode = (J3dNiNode)niToJ3dData.get((NiAVObject)niToJ3dData.get(niPSysGravityModifier.gravityObject));
		gravityAxis = ConvertFromNif.toJ3dNoScale(niPSysGravityModifier.gravityAxis); // normal no scale 
		decay = ConvertFromNif.toJ3d(niPSysGravityModifier.decay);
		strength = ConvertFromNif.toJ3d(niPSysGravityModifier.strength);
		forceType = niPSysGravityModifier.forceType;
		turbulence = niPSysGravityModifier.turbulence;
		turbulenceScale = niPSysGravityModifier.turbulenceScale;

		//we'll need this later
		gravityJ3dNiNode.addChild(gravityNode);
		gravityNode.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysGravityModifier");
			System.out.print(" gravityJ3dNiNode " + gravityJ3dNiNode.getName());
			System.out.print(" gravityNode " + gravityNode.getName());
			System.out.print(" gravityAxis " + gravityAxis);
			System.out.print(" decay " + decay);
			System.out.print(" strength " + strength);
			System.out.print(" forceType " + forceType);
			System.out.print(" turbulence " + turbulence);
			System.out.println(" turbulenceScale " + turbulenceScale);
		}

	}

	public void updateStrength(float value) {
		strength = value;
	}

	//deburner
	private Transform3D	trans			= new Transform3D();

	private Point3f		gravityLoc		= new Point3f();

	private Vector3f	gravityApplied	= new Vector3f();
	private boolean nodeIssueReported = false;
	@Override
	public void updatePSys(long elapsedMillisec) {
		if (forceType == NiPSysGravityModifier.FORCE_PLANAR) {
			gravityLoc.set(0, 0, 0);
			if (gravityNode.isCompiled() && !gravityNode.isLive()) {
				if(!nodeIssueReported)
				System.out.println("gravityNode that can't be used "	+ gravityJ3dNiNode.getName() + " "
									+ gravityJ3dNiNode.getNiAVObject().nVer.fileName);
				nodeIssueReported = true;
			} else {
				gravityNode.getLocalToVworld(trans);
			}

			gravityApplied.set(gravityAxis);
			trans.transform(gravityApplied);
			trans.transform(gravityLoc);

			gravityApplied.normalize();

			//FIXME: turbulence and turbulenceScale
			// values like J3dNiPSysGravityModifier turbulence 12.0 turbulenceScale 0.02
			if (J3dNiParticleSystem.DEBUG_DATA && turbulence != 0) {
				//	System.err.println(
				//			"J3dNiPSysGravityModifier turbulence " + turbulence + " turbulenceScale " + turbulenceScale);
			}

			Point3f loc = new Point3f();

			J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
			float fractionOfSec = elapsedMillisec / 1000f;

			float[] vs = j3dPSysData.particleVelocity;
			float[] ts = j3dPSysData.particleTranslation;

			for (int i = 0; i < j3dPSysData.activeParticleCount; i++) {
				loc.set(ts[i * 3 + 0], ts[i * 3 + 1], ts[i * 3 + 2]);
				float distFromGravity = gravityLoc.distance(loc);
				float actualDecay = (decay * distFromGravity) * (decay * distFromGravity);
				float actualStrength = strength - actualDecay;
				actualStrength = actualStrength < 0 ? 0 : actualStrength;

				/*if (J3dNiParticleSystem.DEBUG_DATA) {
					System.out.println("distFromGravity " + distFromGravity);
					System.out.println("decay " + decay);
					System.out.println("actualDecay " + actualDecay);
					System.out.println("strength " + strength);
					System.out.println("actualStrength " + actualStrength);
					System.out.println("gravityApplied " + gravityApplied);
					System.out.println("fractionOfSec * actualStrength " + (fractionOfSec * actualStrength));
				}*/

				vs[i * 3 + 0] += gravityApplied.x * fractionOfSec * actualStrength;
				vs[i * 3 + 1] += gravityApplied.y * fractionOfSec * actualStrength;
				vs[i * 3 + 2] += gravityApplied.z * fractionOfSec * actualStrength;
			}
		} else if (forceType == NiPSysGravityModifier.FORCE_SPHERICAL) {
			//FIXME: not done!!
			//System.err.println("NiPSysGravityModifier FORCE_SPHERICAL nt done ");
		} else {
			System.err.println("bad force NiPSysGravityModifier " + forceType);
		}

	}
}
