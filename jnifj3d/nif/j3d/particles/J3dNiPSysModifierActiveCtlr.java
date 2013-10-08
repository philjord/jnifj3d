package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysModifierActiveCtlr;

public class J3dNiPSysModifierActiveCtlr extends J3dNiPSysModifierBoolCtlr
{

	public J3dNiPSysModifierActiveCtlr(NiPSysModifierActiveCtlr niPSysModifierActiveCtlr, NiToJ3dData niToJ3dData,
			J3dNiPSysModifier j3dNiPSysModifier)
	{
		super(niPSysModifierActiveCtlr, niToJ3dData, j3dNiPSysModifier);
	}

	@Override
	public void update(boolean value)
	{
		j3dNiPSysModifier.updateActive(value);
	}

}
