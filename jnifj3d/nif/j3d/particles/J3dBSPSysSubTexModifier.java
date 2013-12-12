package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysSubTexModifier;

public class J3dBSPSysSubTexModifier extends J3dNiPSysModifier
{
	private BSPSysSubTexModifier bSPSysSubTexModifier;

	public J3dBSPSysSubTexModifier(BSPSysSubTexModifier bSPSysSubTexModifier, NiToJ3dData niToJ3dData)
	{
		super(bSPSysSubTexModifier, niToJ3dData);
		this.bSPSysSubTexModifier = bSPSysSubTexModifier;
	}

	public void particleCreated(int pId)
	{
		//TODO: the bSPSysSubTexModifier has lots of data
	}

	private long accumTime = 0;

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		accumTime += elapsedMillisec;
		if (accumTime > 300)
		{
			accumTime -= 300;
			//TODO: lots to be done
			J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
			int[] iis = j3dPSysData.particleImageIds;
			for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
			{
				iis[i] = iis[i] + 1;
				// wrap
				iis[i] = iis[i] >= j3dPSysData.atlasAnimatedTexture.getSubImageCount() ? 0 : iis[i];
			}

			j3dPSysData.updateAllTexCoords();
		}
	}
}
