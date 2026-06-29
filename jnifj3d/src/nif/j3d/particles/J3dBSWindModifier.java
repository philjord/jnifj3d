package nif.j3d.particles;

import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSWindModifier;

public class J3dBSWindModifier extends J3dNiPSysModifier {
	//wind from the right towards the left
	Vector3f	windDir	= new Vector3f(-1, 0, 0);
	float		strength;

	public J3dBSWindModifier(BSWindModifier bSWindModifier, NiToJ3dData niToJ3dData) {
		super(bSWindModifier, niToJ3dData);
		strength = bSWindModifier.strength;

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dBSWindModifier");
			System.out.print(" strength " + bSWindModifier.strength);
			System.out.println(" plus apparently I need game data value for direction of wind ");
		}
	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
		float fractionOfSec = elapsedMillisec / 1000f;

		float[] vs = j3dPSysData.particleVelocity;

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++) {
			// build to strength but no more, strength is the max m/s effect (I'd say)
			if (vs[i * 3 + 0] < windDir.x * strength)
				vs[i * 3 + 0] += windDir.x * fractionOfSec * strength;
			if (vs[i * 3 + 1] < windDir.y * strength)
				vs[i * 3 + 1] += windDir.y * fractionOfSec * strength;
			if (vs[i * 3 + 2] < windDir.z * strength)
				vs[i * 3 + 2] += windDir.z * fractionOfSec * strength;
		}
	}
}
