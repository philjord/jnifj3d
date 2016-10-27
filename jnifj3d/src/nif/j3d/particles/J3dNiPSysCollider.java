package nif.j3d.particles;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jogamp.java3d.Group;
import org.jogamp.java3d.Node;

import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.particle.NiPSysCollider;
import nif.niobject.particle.NiPSysPlanarCollider;
import nif.niobject.particle.NiPSysSphericalCollider;

public abstract class J3dNiPSysCollider
{
	private J3dNiNode colliderJ3dNiNode;

	// we use this so all teh transforms on teh ninode abvoe are taken into account properly
	protected Group colliderNode = new Group();

	public J3dNiPSysCollider(NiPSysCollider niPSysCollider, NiToJ3dData niToJ3dData)
	{
		colliderJ3dNiNode = (J3dNiNode) niToJ3dData.get((NiAVObject) niToJ3dData.get(niPSysCollider.colliderObject));
		//we'll need this later
		colliderJ3dNiNode.addChild(colliderNode);
		colliderNode.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
	}

	protected abstract boolean checkCollision(Point3f loc, Vector3f vel, Vector3f newVel);

	public static J3dNiPSysCollider createJ3dNiPSysCollider(NiPSysCollider niPSysCollider, NiToJ3dData niToJ3dData)
	{
		if (niPSysCollider instanceof NiPSysPlanarCollider)
		{
			return new J3dNiPSysPlanarCollider((NiPSysPlanarCollider) niPSysCollider, niToJ3dData);
		}
		else if (niPSysCollider instanceof NiPSysSphericalCollider)
		{
			return new J3dNiPSysSphericalCollider((NiPSysSphericalCollider) niPSysCollider, niToJ3dData);
		}
		else
		{
			System.out.println("Eh bad NiPSysCollider " + niPSysCollider);
		}
		return null;
	}
}
