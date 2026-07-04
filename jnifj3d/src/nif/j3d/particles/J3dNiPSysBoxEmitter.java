package nif.j3d.particles;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysBoxEmitter;
import utils.convert.ConvertFromNif;

/**
 * <niobject name="NiPSysBoxEmitter" abstract="0" inherit="NiPSysVolumeEmitter" ver1="10.1.0.0">
 * 
 * Particle emitter that uses points within a defined Box shape to emit from..
 * 
 * <add name="Width" type="float">Defines the Width of the box area.</add> <add name="Height" type="float">Defines the
 * Height of the box area.</add> <add name="Depth" type="float">Defines the Depth of the box area.</add> </niobject>
 */
public class J3dNiPSysBoxEmitter extends J3dNiPSysVolumeEmittter {

	private float	width;
	private float	height;
	private float	depth;

	public J3dNiPSysBoxEmitter(NiPSysBoxEmitter niPSysBoxEmitter, NiToJ3dData niToJ3dData) {
		super(niPSysBoxEmitter, niToJ3dData);

		this.width = ConvertFromNif.toJ3d(niPSysBoxEmitter.width);
		this.height = ConvertFromNif.toJ3d(niPSysBoxEmitter.height);
		this.depth = ConvertFromNif.toJ3d(niPSysBoxEmitter.depth);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysBoxEmitter");
			System.out.print(" width " + width);
			System.out.print(" height " + height);
			System.out.println(" depth " + depth);
		}
	}


	@Override
	protected void getCreationPoint(Point3f pos, Vector3f vel) {
		float x = varHalf(width);
		float y = varHalf(height);
		float z = varHalf(depth);

		pos.set(x, y, z);

		getCurrentNiNodeTransform().transform(pos);
		getCurrentNiNodeTransform().transform(vel);
	}
}
