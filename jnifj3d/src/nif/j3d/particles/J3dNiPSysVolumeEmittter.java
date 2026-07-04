package nif.j3d.particles;

import org.jogamp.java3d.Group;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;

import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.particle.NiPSysVolumeEmitter;

public abstract class J3dNiPSysVolumeEmittter extends J3dNiPSysEmitter {
	protected J3dNiNode		j3dNiNode;
	//deburner
	private Transform3D		trans	= new Transform3D();

	public J3dNiPSysVolumeEmittter(NiPSysVolumeEmitter niPSysVolumeEmitter, NiToJ3dData niToJ3dData) {
		super(niPSysVolumeEmitter, niToJ3dData);
		j3dNiNode = (J3dNiNode)niToJ3dData.get((NiAVObject)niToJ3dData.get(niPSysVolumeEmitter.emitterObject));

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

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysVolumeEmittter parent of next");
			System.out.println(" j3dNiNode " + j3dNiNode);
		}
	}

	protected Transform3D getCurrentNiNodeTransform() {

		j3dNiNode.getTreeTransform(trans, root);
		return trans;
	}

}
