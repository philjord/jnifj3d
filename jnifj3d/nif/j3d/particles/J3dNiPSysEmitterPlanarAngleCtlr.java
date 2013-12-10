package nif.j3d.particles;

import nif.niobject.particle.NiPSysEmitterPlanarAngleCtlr;

public class J3dNiPSysEmitterPlanarAngleCtlr extends J3dNiPSysModifierCtlr
{
	private J3dNiPSysEmitter j3dNiPSysEmitter;

	public J3dNiPSysEmitterPlanarAngleCtlr(NiPSysEmitterPlanarAngleCtlr niPSysEmitterPlanarAngleCtlr, J3dNiPSysEmitter j3dNiPSysEmitter)
	{
		super(niPSysEmitterPlanarAngleCtlr, j3dNiPSysEmitter);
		this.j3dNiPSysEmitter = j3dNiPSysEmitter;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysEmitter.updatePlanarAngle(value);
	}

}
