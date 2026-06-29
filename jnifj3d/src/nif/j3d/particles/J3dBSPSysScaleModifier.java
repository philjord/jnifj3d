package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysScaleModifier;

public class J3dBSPSysScaleModifier extends J3dNiPSysModifier {

	public int		numFloats;

	public float[]	floats;

	public J3dBSPSysScaleModifier(BSPSysScaleModifier niPSysModifier, NiToJ3dData niToJ3dData) {
		super(niPSysModifier, niToJ3dData);

		numFloats = niPSysModifier.numFloats;
		floats = niPSysModifier.floats;

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dBSPSysScaleModifier");
			System.out.println(" numFloats " + numFloats);

		}
	}

	@Override
	public void updatePSys(long elapsedMillisec) {

		//ok 0 to 119 floats 120 num, 0 starts at value 1 119 = value 2 
		// so proably chop life up in lifespan as knots and alpha teh scale value

		if (numFloats > 0) {
			J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
			long[] as = j3dPSysData.particleAge; // in milliseconds
			long[] lss = j3dPSysData.particleLifeSpan; // in ms

			float[] ps = j3dPSysData.particleSize;

			for (int pId = 0; pId < j3dPSysData.activeParticleCount; pId++) {

				float age = as[pId];
				float lifeSpan = lss[pId];
				float alphaValue = age / lifeSpan; // and normalised value from 0 to 1
				alphaValue = alphaValue < 0 ? 0 : alphaValue > 1 ? 1 : alphaValue;

				//let's try trivial 
				int idx = (int)((numFloats - 1) * alphaValue);

				ps[pId] = floats[idx];

			}

		}

	}

}
