package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysModifierBoolCtlr;

public abstract class J3dNiPSysModifierBoolCtlr extends J3dNiPSysModifierCtlr
{
	/**
	 * Class simply to disallow float interp controll
	 * @param niPSysModifierBoolCtlr
	 * @param niToJ3dData
	 * @param j3dNiPSysModifier
	 */
	public J3dNiPSysModifierBoolCtlr(NiPSysModifierBoolCtlr niPSysModifierBoolCtlr, NiToJ3dData niToJ3dData,
			J3dNiPSysModifier j3dNiPSysModifier)
	{
		super(niPSysModifierBoolCtlr, niToJ3dData, j3dNiPSysModifier);
	}

	@Override
	public void update(float value)
	{
		new Throwable("J3dNiPSysModifierBoolCtlr can't be controlled by a float interp").printStackTrace();
	}

}
