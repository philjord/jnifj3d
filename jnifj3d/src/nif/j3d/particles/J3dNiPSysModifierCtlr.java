package nif.j3d.particles;

import javax.media.j3d.Alpha;
import javax.vecmath.Point3f;

import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.interp.J3dNiInterpolator;
import nif.niobject.bs.BSPSysMultiTargetEmitterCtlr;
import nif.niobject.controller.NiTimeController;
import nif.niobject.controller.NiVisController;
import nif.niobject.interpolator.NiInterpolator;
import nif.niobject.particle.NiPSysEmitterCtlr;
import nif.niobject.particle.NiPSysEmitterDeclinationCtlr;
import nif.niobject.particle.NiPSysEmitterDeclinationVarCtlr;
import nif.niobject.particle.NiPSysEmitterInitialRadiusCtlr;
import nif.niobject.particle.NiPSysEmitterLifeSpanCtlr;
import nif.niobject.particle.NiPSysEmitterPlanarAngleCtlr;
import nif.niobject.particle.NiPSysEmitterSpeedCtlr;
import nif.niobject.particle.NiPSysFieldMagnitudeCtlr;
import nif.niobject.particle.NiPSysGravityStrengthCtlr;
import nif.niobject.particle.NiPSysInitialRotSpeedCtlr;
import nif.niobject.particle.NiPSysModifierActiveCtlr;
import nif.niobject.particle.NiPSysModifierCtlr;
import nif.niobject.particle.NiPSysUpdateCtlr;

public abstract class J3dNiPSysModifierCtlr extends J3dNiTimeController
{
	protected J3dNiPSysModifier j3dNiPSysModifier;

	private J3dNiInterpolator j3dNiInterpolator;

	private Alpha baseAlpha;// created as looping, so never needs resetting

	private J3dNiPSysModifierCtlr nextJ3dNiPSysModifierCtlr;

	public J3dNiPSysModifierCtlr(NiPSysModifierCtlr niPSysModifierCtlr, J3dNiPSysModifier j3dNiPSysModifier)
	{
		super(niPSysModifierCtlr);
		this.j3dNiPSysModifier = j3dNiPSysModifier;

	}

	private void setInterpolator(J3dNiInterpolator j3dNiInterpolator2, Alpha baseAlpha2)
	{
		j3dNiInterpolator = j3dNiInterpolator2;
		baseAlpha = baseAlpha2;
	}

	private void setNextController(J3dNiPSysModifierCtlr nextJ3dNiPSysModifierCtlr2)
	{
		nextJ3dNiPSysModifierCtlr = nextJ3dNiPSysModifierCtlr2;
	}

	public void process()
	{
		if (j3dNiInterpolator != null)
		{
			j3dNiInterpolator.process(baseAlpha.value());
		}

		// fire the next controller
		if (nextJ3dNiPSysModifierCtlr != null)
		{
			nextJ3dNiPSysModifierCtlr.process();
		}
	}

	@Override
	public void update(Point3f value)
	{
		new Throwable("J3dNiPSysModifierCtlr can't be controlled by a Point3f interp").printStackTrace();
	}

	public static J3dNiPSysModifierCtlr createJ3dNiPSysModifierCtlr(J3dNiParticleSystem j3dNiParticleSystem,
			NiTimeController niTimeController, NiToJ3dData niToJ3dData)
	{
		if (niTimeController instanceof NiPSysModifierCtlr)
		{
			NiPSysModifierCtlr niPSysModifierCtlr = (NiPSysModifierCtlr) niTimeController;
			J3dNiPSysModifier j3dNiPSysModifier = j3dNiParticleSystem.getJ3dNiPSysModifier(niPSysModifierCtlr.modifierName);
			if (j3dNiPSysModifier == null)
				return null;

			J3dNiPSysModifierCtlr j3dNiTimeController = null;

			// we must construct then set inperpolator then set next in that order! not as one hit in the constructor
			if (niPSysModifierCtlr instanceof NiPSysEmitterCtlr)
			{
				j3dNiTimeController = new J3dNiPSysEmitterCtlr((NiPSysEmitterCtlr) niPSysModifierCtlr, (J3dNiPSysEmitter) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysModifierActiveCtlr)
			{
				j3dNiTimeController = new J3dNiPSysModifierActiveCtlr((NiPSysModifierActiveCtlr) niPSysModifierCtlr, j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysEmitterLifeSpanCtlr)
			{
				j3dNiTimeController = new J3dNiPSysEmitterLifeSpanCtlr((NiPSysEmitterLifeSpanCtlr) niPSysModifierCtlr,
						(J3dNiPSysEmitter) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysEmitterSpeedCtlr)
			{
				j3dNiTimeController = new J3dNiPSysEmitterSpeedCtlr((NiPSysEmitterSpeedCtlr) niPSysModifierCtlr,
						(J3dNiPSysEmitter) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysEmitterDeclinationCtlr)
			{
				j3dNiTimeController = new J3dNiPSysEmitterDeclinationCtlr((NiPSysEmitterDeclinationCtlr) niPSysModifierCtlr,
						(J3dNiPSysEmitter) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysEmitterDeclinationVarCtlr)
			{
				j3dNiTimeController = new J3dNiPSysEmitterDeclinationVarCtlr((NiPSysEmitterDeclinationVarCtlr) niPSysModifierCtlr,
						(J3dNiPSysEmitter) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysEmitterInitialRadiusCtlr)
			{
				j3dNiTimeController = new J3dNiPSysEmitterInitialRadiusCtlr((NiPSysEmitterInitialRadiusCtlr) niPSysModifierCtlr,
						(J3dNiPSysEmitter) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysGravityStrengthCtlr)
			{
				j3dNiTimeController = new J3dNiPSysGravityStrengthCtlr((NiPSysGravityStrengthCtlr) niPSysModifierCtlr,
						(J3dNiPSysGravityModifier) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysEmitterPlanarAngleCtlr)
			{
				j3dNiTimeController = new J3dNiPSysEmitterPlanarAngleCtlr((NiPSysEmitterPlanarAngleCtlr) niPSysModifierCtlr,
						(J3dNiPSysEmitter) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysInitialRotSpeedCtlr)
			{
				j3dNiTimeController = new J3dNiPSysInitialRotSpeedCtlr((NiPSysInitialRotSpeedCtlr) niPSysModifierCtlr,
						(J3dNiPSysRotationModifier) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof NiPSysFieldMagnitudeCtlr)
			{
				System.out.println("J3dNiPSysModifierCtlr NiPSysFieldMagnitudeCtlr " + j3dNiPSysModifier);
				//j3dNiTimeController = new J3dNiPSysFieldMagnitudeCtlr((NiPSysFieldMagnitudeCtlr) niPSysModifierCtlr,
				//		(J3dNiPSysFieldModifer) j3dNiPSysModifier);
			}
			else if (niPSysModifierCtlr instanceof BSPSysMultiTargetEmitterCtlr)
			{
				// not really understood yet
				System.out.println("J3dNiPSysModifierCtlr BSPSysMultiTargetEmitterCtlr " + j3dNiPSysModifier);
			}

			else
			{
				System.out.println("J3dNiPSysModiferCtlr createJ3dNiPSysModifierCtlr unhandled NiPSysModifierCtlr " + niPSysModifierCtlr
						+ " " + j3dNiPSysModifier);
			}

			if (j3dNiTimeController != null)
			{

				j3dNiParticleSystem.j3dNiPSysModiferCtlrsByNi.put(niPSysModifierCtlr, j3dNiTimeController);
				niToJ3dData.put(niPSysModifierCtlr, j3dNiTimeController);

				NiInterpolator nii = (NiInterpolator) niToJ3dData.get(niPSysModifierCtlr.interpolator);
				if (nii != null)
				{
					J3dNiInterpolator j3dNiInterpolator = J3dNiTimeController.createInterpForController(j3dNiTimeController, nii,
							niToJ3dData, niPSysModifierCtlr.startTime, niPSysModifierCtlr.stopTime);
					Alpha baseAlpha = J3dNiTimeController.createLoopingAlpha(niPSysModifierCtlr.startTime, niPSysModifierCtlr.stopTime);
					j3dNiTimeController.setInterpolator(j3dNiInterpolator, baseAlpha);
				}

				NiTimeController nextController = (NiTimeController) niToJ3dData.get(niPSysModifierCtlr.nextController);
				if (nextController != null)
				{
					J3dNiPSysModifierCtlr nextJ3dNiPSysModifierCtlr = createJ3dNiPSysModifierCtlr(j3dNiParticleSystem, nextController,
							niToJ3dData);
					if (nextJ3dNiPSysModifierCtlr != null)
					{
						j3dNiTimeController.setNextController(nextJ3dNiPSysModifierCtlr);
					}
				}
			}

			return j3dNiTimeController;
		}
		else if (niTimeController instanceof NiPSysUpdateCtlr)
		{
			//NiPSysUpdateCtlr niPSysUpdateCtlr = (NiPSysUpdateCtlr) niTimeController;
			// no really interesting data, though the flags suggest how the animation loop should work
			// once all controller run through the update ctlr might say go backwards through them
			// ignore for now
			return null;

		}
		else if (niTimeController instanceof NiVisController)
		{
			//TODO: niTimeController is NiVisController");
			return null;
		}
		else
		{
			System.out.println("TODO: in createJ3dNiPSysModifierCtlr, niTimeController is not expected: " + niTimeController);
			return null;
		}
	}
}
