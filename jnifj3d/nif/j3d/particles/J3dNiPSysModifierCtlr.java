package nif.j3d.particles;

import javax.vecmath.Point3f;

import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiTimeController;
import nif.niobject.bs.BSPSysMultiTargetEmitterCtlr;
import nif.niobject.particle.NiPSysEmitterCtlr;
import nif.niobject.particle.NiPSysEmitterDeclinationCtlr;
import nif.niobject.particle.NiPSysEmitterDeclinationVarCtlr;
import nif.niobject.particle.NiPSysEmitterInitialRadiusCtlr;
import nif.niobject.particle.NiPSysEmitterLifeSpanCtlr;
import nif.niobject.particle.NiPSysEmitterPlanarAngleCtlr;
import nif.niobject.particle.NiPSysEmitterSpeedCtlr;
import nif.niobject.particle.NiPSysFieldMagnitudeCtlr;
import nif.niobject.particle.NiPSysGravityStrengthCtlr;
import nif.niobject.particle.NiPSysModifierActiveCtlr;
import nif.niobject.particle.NiPSysModifierCtlr;

public abstract class J3dNiPSysModifierCtlr extends J3dNiTimeController
{
	protected J3dNiPSysModifier j3dNiPSysModifier;

	public J3dNiPSysModifierCtlr(NiPSysModifierCtlr niPSysModifierCtlr, NiToJ3dData niToJ3dData, J3dNiPSysModifier j3dNiPSysModifier)
	{
		super(niPSysModifierCtlr);
		this.j3dNiPSysModifier = j3dNiPSysModifier;
	}

	@Override
	public void update(Point3f value)
	{
		new Throwable("J3dNiPSysModifierCtlr can't be controlled by a Point3f interp").printStackTrace();
	}

	public static J3dNiPSysModifierCtlr createJ3dNiPSysModifierCtlr(J3dNiParticleSystem j3dNiParticleSystem,
			NiPSysModifierCtlr niPSysModifierCtlr, NiToJ3dData niToJ3dData)
	{
		J3dNiPSysModifier j3dNiPSysModifier = j3dNiParticleSystem.getJ3dNiPSysModifier(niPSysModifierCtlr.modifierName);
		if (j3dNiPSysModifier == null)
			return null;

		J3dNiPSysModifierCtlr j3dNiTimeController = null;

		if (niPSysModifierCtlr instanceof NiPSysEmitterCtlr)
		{
			j3dNiTimeController = new J3dNiPSysEmitterCtlr((NiPSysEmitterCtlr) niPSysModifierCtlr, niToJ3dData,
					(J3dNiPSysEmitter) j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysModifierActiveCtlr)
		{
			j3dNiTimeController = new J3dNiPSysModifierActiveCtlr((NiPSysModifierActiveCtlr) niPSysModifierCtlr, niToJ3dData,
					j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysEmitterLifeSpanCtlr)
		{
			j3dNiTimeController = new J3dNiPSysEmitterLifeSpanCtlr((NiPSysEmitterLifeSpanCtlr) niPSysModifierCtlr, niToJ3dData,
					(J3dNiPSysEmitter) j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysEmitterSpeedCtlr)
		{
			j3dNiTimeController = new J3dNiPSysEmitterSpeedCtlr((NiPSysEmitterSpeedCtlr) niPSysModifierCtlr, niToJ3dData,
					(J3dNiPSysEmitter) j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysEmitterDeclinationCtlr)
		{
			j3dNiTimeController = new J3dNiPSysEmitterDeclinationCtlr((NiPSysEmitterDeclinationCtlr) niPSysModifierCtlr, niToJ3dData,
					(J3dNiPSysEmitter) j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysEmitterDeclinationVarCtlr)
		{
			j3dNiTimeController = new J3dNiPSysEmitterDeclinationVarCtlr((NiPSysEmitterDeclinationVarCtlr) niPSysModifierCtlr, niToJ3dData,
					(J3dNiPSysEmitter) j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysEmitterInitialRadiusCtlr)
		{
			j3dNiTimeController = new J3dNiPSysEmitterInitialRadiusCtlr((NiPSysEmitterInitialRadiusCtlr) niPSysModifierCtlr, niToJ3dData,
					(J3dNiPSysEmitter) j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysGravityStrengthCtlr)
		{
			j3dNiTimeController = new J3dNiPSysGravityStrengthCtlr((NiPSysGravityStrengthCtlr) niPSysModifierCtlr, niToJ3dData,
					(J3dNiPSysGravityModifier) j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysFieldMagnitudeCtlr)
		{
			//TODO: System.out.println("J3dNiPSysModifierCtlr NiPSysFieldMagnitudeCtlr " + j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof BSPSysMultiTargetEmitterCtlr)
		{
			//TODO: System.out.println("J3dNiPSysModifierCtlr BSPSysMultiTargetEmitterCtlr " + j3dNiPSysModifier);
		}
		else if (niPSysModifierCtlr instanceof NiPSysEmitterPlanarAngleCtlr)
		{
			//TODO: NiPSysEmitterPlanarAngleCtlr
		}
		else
		{
			System.out.println("J3dNiPSysModiferCtlr createJ3dNiPSysModifierCtlr unhandled NiPSysModifierCtlr " + niPSysModifierCtlr);
		}

		return j3dNiTimeController;
	}
}
