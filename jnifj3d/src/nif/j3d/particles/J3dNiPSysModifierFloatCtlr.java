package nif.j3d.particles;

import nif.niobject.particle.NiPSysModifierFloatCtlr;

public abstract class J3dNiPSysModifierFloatCtlr extends J3dNiPSysModifierCtlr
{
	/**
	 * Class simlpy to disallow boolean interp controll
	 * @param niPSysModifierFloatCtlr
	 * @param niToJ3dData
	 * @param j3dNiPSysModifier
	 */
	public J3dNiPSysModifierFloatCtlr(NiPSysModifierFloatCtlr niPSysModifierFloatCtlr, J3dNiPSysModifier j3dNiPSysModifier)
	{
		super(niPSysModifierFloatCtlr, j3dNiPSysModifier);
	}

	@Override
	public void update(boolean value)
	{
		new Throwable("J3dNiPSysModifierFloatCtlr can't be controlled by a boolean interp").printStackTrace();
	}

}
