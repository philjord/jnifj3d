package nif.j3d.particles;

import utils.convert.ConvertFromNif;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysSphericalCollider;

public class J3dNiPSysSphericalCollider extends J3dNiPSysCollider
{
	private float radius;

	public J3dNiPSysSphericalCollider(NiPSysSphericalCollider niPSysSphericalCollider, NiToJ3dData niToJ3dData)
	{
		super(niPSysSphericalCollider, niToJ3dData);
		radius = ConvertFromNif.toJ3d(niPSysSphericalCollider.radius);
	}

	@Override
	protected boolean checkCollision(Point3f loc, Vector3f vel, Vector3f newVel)
	{
		//TODO: this collison
		return false;
	}
}
