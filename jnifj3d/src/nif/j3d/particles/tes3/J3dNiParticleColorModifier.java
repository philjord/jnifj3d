package nif.j3d.particles.tes3;

import org.jogamp.vecmath.Color4f;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiColorData;
import nif.niobject.particle.NiParticleColorModifier;

public class J3dNiParticleColorModifier extends J3dNiParticleModifier
{
	private NiColorData niColorData;

	public J3dNiParticleColorModifier(NiParticleColorModifier niParticleColorModifier, J3dNiParticlesData j3dNiParticlesData,
			NiToJ3dData niToJ3dData)
	{
		super(niParticleColorModifier, j3dNiParticlesData, niToJ3dData);
		niColorData = (NiColorData) niToJ3dData.get(niParticleColorModifier.colorData);
	}

	@Override
	public void updateParticles(long elapsedMillisec)
	{
		for (int i = 0; i < j3dNiParticlesData.activeParticleCount; i++)
		{
			float ageSec = j3dNiParticlesData.particleAge[i] / 1000f;
			Color4f nc = interpolateColor(ageSec);

			float[] cs = j3dNiParticlesData.particleColors;
			cs[i * 4 + 0] = nc.x;
			cs[i * 4 + 1] = nc.y;
			cs[i * 4 + 2] = nc.z;
			cs[i * 4 + 3] = nc.w;
		}

	}

	private Color4f interpolateColor(float ageSec)
	{
		int lastIdx = niColorData.data.time.length - 1;
		Color4f ret = new Color4f(niColorData.data.value[(lastIdx)*4+0],
				niColorData.data.value[(lastIdx)*4+1],
				niColorData.data.value[(lastIdx)*4+2],
				niColorData.data.value[(lastIdx)*4+3]);
		for (int i = 0; i < niColorData.data.time.length; i++) {
			if ((i == 0 && ageSec <= niColorData.data.time[i]) || (i > 0 && ageSec >= niColorData.data.time[i - 1] && ageSec <= niColorData.data.time[i])) {
				if (i == 0) {
					ret.set(niColorData.data.value[0*4+0], niColorData.data.value[0*4+1], niColorData.data.value[0*4+2], niColorData.data.value[0*4+3]);
				} else {
					float currentInterpolationValue = (ageSec - niColorData.data.time[i - 1]) / (niColorData.data.time[i] - niColorData.data.time[i - 1]);

					ret.set((niColorData.data.value[(i - 1)*4+0] * currentInterpolationValue) + (niColorData.data.value[i*4+0] * (1 - currentInterpolationValue)), //
							(niColorData.data.value[(i - 1)*4+1] * currentInterpolationValue) + (niColorData.data.value[i*4+1] * (1 - currentInterpolationValue)), //
							(niColorData.data.value[(i - 1)*4+2] * currentInterpolationValue) + (niColorData.data.value[i*4+2] * (1 - currentInterpolationValue)), //
							(niColorData.data.value[(i - 1)*4+3] * currentInterpolationValue) + (niColorData.data.value[i*4+3] * (1 - currentInterpolationValue)));
				}
				break;
			}
		}

		return ret;
	}

	@Override
	public void particleCreated(int pId)
	{
		float[] cs = j3dNiParticlesData.particleColors;
		cs[pId * 4 + 0] = niColorData.data.value[0*4+0];
		cs[pId * 4 + 1] = niColorData.data.value[0*4+1];
		cs[pId * 4 + 2] = niColorData.data.value[0*4+2];
		cs[pId * 4 + 3] = niColorData.data.value[0*4+3];
	}
}
