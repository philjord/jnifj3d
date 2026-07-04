package nif.j3d.particles;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysArrayEmitter;

public class J3dBSPSysArrayEmitter extends J3dNiPSysVolumeEmittter {
	/**
	 <niobject name="BSPSysArrayEmitter" abstract="0" inherit="NiPSysVolumeEmitter" ver1="20.0.0.5">

	 Particle emitter that uses a node, its children and subchildren to emit from. 
	 Emission will be evenly spread along points from nodes leading to their direct parents/children only.
	 
	 </niobject>
	 */

	public J3dBSPSysArrayEmitter(BSPSysArrayEmitter bSPSysArrayEmitter, NiToJ3dData niToJ3dData) {
		super(bSPSysArrayEmitter, niToJ3dData);
		
		for (int i = 0; i < j3dNiNode.numChildren(); i++) {
			J3dNiNode child = (J3dNiNode)j3dNiNode.getChild(i);  
			
			//TODO:
			// put it in a list and then get it's childrne then... some thing?
			
			
		}
		 
		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dBSPSysArrayEmitter");			 
		}
	}

	@Override
	protected void getCreationPoint(Point3f pos, Vector3f vel) {
		//FIXME: j3dNiNode is the node to start from find out what children it's got and...  

		//System.out.println("J3dBSPSysArrayEmitter emitted a particle");

		float x = varHalf(1f);
		float y = varHalf(1f);
		float z = varHalf(1f);

		pos.set(x, y, z);
		//TODO: just to put it near the emitter node
		getCurrentNiNodeTransform().transform(pos);
		getCurrentNiNodeTransform().transform(vel);		
	}

}
