package nif.j3d.particles;

import nif.niobject.particle.NiPSysModifierActiveCtlr;

public class J3dNiPSysModifierActiveCtlr extends J3dNiPSysModifierBoolCtlr
{

	public J3dNiPSysModifierActiveCtlr(NiPSysModifierActiveCtlr niPSysModifierActiveCtlr, J3dNiPSysModifier j3dNiPSysModifier)
	{
		super(niPSysModifierActiveCtlr, j3dNiPSysModifier);
	}

	@Override
	public void update(boolean value)
	{
		j3dNiPSysModifier.updateActive(value);
	}

}
