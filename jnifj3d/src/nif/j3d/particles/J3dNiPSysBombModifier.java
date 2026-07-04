package nif.j3d.particles;

import org.jogamp.java3d.Group;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Vector3f;

import nif.enums.DecayType;
import nif.enums.SymmetryType;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.particle.NiPSysBombModifier;
import utils.convert.ConvertFromNif;

public class J3dNiPSysBombModifier extends J3dNiPSysModifier {
	/**
	 <niobject name="NiPSysBombModifier" abstract="0" inherit="NiPSysModifier" ver1="20.0.0.5">
	
	 Particle modifier that uses a NiNode to use as a "Bomb Object" to alter the path of particles.
	 
	 <add name="Bomb Object" type="Ptr" template="NiNode">Link to a NiNode for bomb to function.</add>
	 <add name="Bomb Axis" type="Vector3">Orientation of bomb object.</add>
	 <add name="Decay" type="float">Falloff rate of the bomb object.</add>
	 <add name="Delta V" type="float">DeltaV /  Strength?</add>
	 <add name="Decay Type" type="DecayType">Decay type</add>
	 <add name="Symmetry Type" type="SymmetryType">Shape/symmetry of the bomb object.</add>
	 </niobject>
	 */

	protected J3dNiNode		j3dNiNode;
	private J3dNiAVObject	root;

	public Vector3f			bombAxis;

	public float			decay;

	public float			deltaV;

	public DecayType		decayType;

	public SymmetryType		symmetryType;

	public J3dNiPSysBombModifier(NiPSysBombModifier niPSysBombModifier, NiToJ3dData niToJ3dData) {
		super(niPSysBombModifier, niToJ3dData);

		j3dNiNode = (J3dNiNode)niToJ3dData.get((NiAVObject)niToJ3dData.get(niPSysBombModifier.bombObject));

		//make the caps correct up to the root
		//TODO the whiole root system should be decided by the particle system and handed out from there, no one should be checking world
		if (j3dNiParticleSystem.worldSpace) {
			root = niToJ3dData.getJ3dRoot();
		} else {
			root = this.j3dNiParticleSystem;
		}
		Group g = j3dNiNode;
		while (g != null) {
			g.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
			g.setCapability(Group.ALLOW_PARENT_READ);
			if (g == niToJ3dData.getJ3dRoot())
				break;
			g = (Group)g.getParent();
		}

		this.bombAxis = ConvertFromNif.toJ3dNoScale(niPSysBombModifier.bombAxis);
		this.decay = niPSysBombModifier.decay;
		this.deltaV = niPSysBombModifier.deltaV;
		this.decayType = niPSysBombModifier.decayType;
		this.symmetryType = niPSysBombModifier.symmetryType;

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysBombModifier");
			System.out.print(" j3dNiNode " + j3dNiNode);
			System.out.print(" bombAxis " + bombAxis);
			System.out.print(" decay " + decay);
			System.out.print(" deltaV " + deltaV);
			System.out.print(" decayType " + decayType);
			System.out.println(" symmetryType " + symmetryType);

		}

	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		//TODO: this
	}
}
