package nif.j3d.particles;

import utils.convert.ConvertFromNif;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysPlanarCollider;

public class J3dNiPSysPlanarCollider extends J3dNiPSysCollider {

	/**
	 
	 <niobject name="NiPSysPlanarCollider" abstract="0" inherit="NiPSysCollider" ver1="10.2.0.0">
	
	 Particle Collider object which particles will interact with.
	 
	 <add name="Width" type="float">Defines the width of the plane.</add>
	 <add name="Height" type="float">Defines the height of the plane.</add>
	 <add name="X Axis" type="Vector3">Defines Orientation.</add>
	 <add name="Y Axis" type="Vector3">Defines Orientation.</add>
	 </niobject>
	 */

	public float		width;

	public float		height;

	private Vector3f	xAxis;

	private Vector3f	yAxis;

	public J3dNiPSysPlanarCollider(	NiPSysPlanarCollider niPSysPlanarCollider, NiToJ3dData niToJ3dData,
									J3dNiParticleSystem j3dNiParticleSystem) {
		super(niPSysPlanarCollider, niToJ3dData, j3dNiParticleSystem);

		width = ConvertFromNif.toJ3d(niPSysPlanarCollider.width);
		height = ConvertFromNif.toJ3d(niPSysPlanarCollider.height);
		xAxis = ConvertFromNif.toJ3dNoScale(niPSysPlanarCollider.xAxis);
		yAxis = ConvertFromNif.toJ3dNoScale(niPSysPlanarCollider.yAxis);
		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysPlanarCollider");
			System.out.print(" width " + width);
			System.out.print(" height " + height);
			System.out.print(" xAxis " + xAxis);
			System.out.println(" yAxis " + yAxis);
		}
	}

	@Override
	protected boolean checkCollision(Point3f loc, Vector3f vel, Vector3f newVel) {
		//TODO: this collison
		//		System.out.println("J3dNiPSysPlanarCollider collidey ");
		//blah blah retunr true;
		
		collLoc.set(0, 0, 0);
		getCurrentNiNodeTransform().transform(collLoc);
		//FIXME: umm planes etc 2 axis and the size should be mathsable

		if (nextCollider != null) {
			return nextCollider.checkCollision(loc, vel, newVel);
		} else {
			return false;
		}
	}
}
