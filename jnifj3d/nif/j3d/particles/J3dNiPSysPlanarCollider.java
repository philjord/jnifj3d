package nif.j3d.particles;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import utils.convert.ConvertFromNif;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysPlanarCollider;

public class J3dNiPSysPlanarCollider extends J3dNiPSysCollider
{
	public float width;

	public float height;

	private Vector3f xAxis;

	private Vector3f yAxis;

	public J3dNiPSysPlanarCollider(NiPSysPlanarCollider niPSysPlanarCollider, NiToJ3dData niToJ3dData)
	{
		super(niPSysPlanarCollider, niToJ3dData);

		width = ConvertFromNif.toJ3d(niPSysPlanarCollider.width);
		height = ConvertFromNif.toJ3d(niPSysPlanarCollider.height);
		xAxis = ConvertFromNif.toJ3dNoScale(niPSysPlanarCollider.xAxis);
		yAxis = ConvertFromNif.toJ3dNoScale(niPSysPlanarCollider.yAxis);
	}

	@Override
	protected boolean checkCollision(Point3f loc, Vector3f vel, Vector3f newVel)
	{
		//TODO: this collison
		return false;
	}
}
