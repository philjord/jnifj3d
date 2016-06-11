package nif.j3d.particles.tes3;

import java.util.ArrayList;

import javax.media.j3d.Alpha;
import javax.media.j3d.Bounds;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.compound.NifParticle;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.j3dinterp.J3dNiFloatInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.j3d.particles.J3dPSysData;
import nif.niobject.particle.NiParticleSystemController;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromNif;

public class J3dNiParticleSystemController extends J3dNiTimeController
{
	protected ArrayList<J3dNiParticleModifier> modifiersInOrder = new ArrayList<J3dNiParticleModifier>();

	private J3dNiInterpolator j3dNiInterpolator;

	private Alpha baseAlpha;// created as looping, so never needs resetting

	private J3dNiParticleSystemController nextJ3dNiParticleSystemController;

	private J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData;

	private J3dNiParticleEmitter j3dNiParticleEmitter;

	public J3dNiParticleSystemController(NiParticleSystemController niParticleSystemController, J3dNiAutoNormalParticles parent,
			J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niParticleSystemController, null);

		this.j3dNiAutoNormalParticlesData = j3dNiAutoNormalParticlesData;

		// only one call back this, which hands float off to all modifiers
		J3dNiInterpolator j3dNiInterpolator = new J3dNiFloatInterpolator(niParticleSystemController.startTime,
				niParticleSystemController.stopTime, this);
		Alpha baseAlpha = J3dNiTimeController.createLoopingAlpha(niParticleSystemController.startTime, niParticleSystemController.stopTime);
		setInterpolator(j3dNiInterpolator, baseAlpha);

		j3dNiParticleEmitter = new J3dNiParticleEmitter(niParticleSystemController, parent, this, j3dNiAutoNormalParticlesData,
				niToJ3dData);

		// now add initial data to the particles
		for (int indx = 0; indx < niParticleSystemController.numValid; indx++)
		{
			NifParticle p = niParticleSystemController.particles[indx];
			Vector3f vel = ConvertFromNif.toJ3d(p.velocity);// notice scale as we are meters per second
			j3dNiAutoNormalParticlesData.particleVelocity[indx * 3 + 0] = vel.x;
			j3dNiAutoNormalParticlesData.particleVelocity[indx * 3 + 1] = vel.y;
			j3dNiAutoNormalParticlesData.particleVelocity[indx * 3 + 2] = vel.z;

			j3dNiAutoNormalParticlesData.particleAge[indx] = (long) (p.lifetime * 1000);// secs to milli secs
			j3dNiAutoNormalParticlesData.particleLifeSpan[indx] = (long) (p.lifespan * 1000);
		}

	}

	/**
	 * hard to work this out
	 * @see nif.j3d.animation.J3dNiTimeController#getBounds()
	 */
	@Override
	public Bounds getBounds()
	{
		return Utils3D.defaultBounds;
	}

	public void setInterpolator(J3dNiInterpolator j3dNiInterpolator2, Alpha baseAlpha2)
	{
		j3dNiInterpolator = j3dNiInterpolator2;
		baseAlpha = baseAlpha2;
	}

	public void setNextController(J3dNiParticleSystemController nextJ3dNiParticleSystemController)
	{
		this.nextJ3dNiParticleSystemController = nextJ3dNiParticleSystemController;
	}

	public void process()
	{
		if (j3dNiInterpolator != null)
		{
			j3dNiInterpolator.process(baseAlpha.value());

		}

		// fire the next controller
		if (nextJ3dNiParticleSystemController != null)
		{
			nextJ3dNiParticleSystemController.process();
		}
	}

	public void particleCreated(int newParticleId)
	{
		if (newParticleId != -1)
		{
			// now tell all modifiers about the new particles so they can make updates to it (like add rotation etc)
			for (J3dNiParticleModifier j3dNiParticleModifier : modifiersInOrder)
			{
				j3dNiParticleModifier.particleCreated(newParticleId);
			}
		}
	}

	@Override
	public void update(float value)
	{
		// the float received here is time (from 0-2 for the example)
		// but I'm not sure that matters much?

		// do spawn age and death now, spawn needs to use the emitter location
		// IN fact controller needs to do all the spawn age death type operations too
		// it also should update the location based on speed

		long fixedTime = 50L;

		updateAge(fixedTime);
		j3dNiParticleEmitter.update(fixedTime);

		for (J3dNiParticleModifier j3dNiParticleModifier : modifiersInOrder)
		{
			//TODO: this is hard coded to the PerTime behaviour above, needs to work out real time?
			j3dNiParticleModifier.updateParticles(fixedTime);
		}
	}

	public void updateAge(long elapsedMillisec)
	{

		long[] as = j3dNiAutoNormalParticlesData.particleAge; // in milliseconds
		long[] lss = j3dNiAutoNormalParticlesData.particleLifeSpan; // in ms

		for (int i = 0; i < j3dNiAutoNormalParticlesData.activeParticleCount; i++)
		{
			as[i] += elapsedMillisec;
			// is the particle past it's lifespan?
			if (lss[i] < as[i])
			{
				j3dNiAutoNormalParticlesData.inactivateParticle(i);
			}
		}

	}

	public void updatePosition(long elapsedMillisec)
	{
		// simply grab the velocity for an active particle and add it on to the translation
		// velocitys are in meters per second

		float fractionOfSec = elapsedMillisec / 1000f;

		float[] vs = j3dNiAutoNormalParticlesData.particleVelocity;
		float[] ts = j3dNiAutoNormalParticlesData.particleTranslation;
		for (int i = 0; i < j3dNiAutoNormalParticlesData.activeParticleCount; i++)
		{
			ts[i * 3 + 0] += vs[i * 3 + 0] * fractionOfSec;
			ts[i * 3 + 1] += vs[i * 3 + 1] * fractionOfSec;
			ts[i * 3 + 2] += vs[i * 3 + 2] * fractionOfSec;
		}

		// note j3dPSysData.recalcAllGaCoords(); will be called once by the particle system after all modifiers have run
	}

	@Override
	public void update(boolean value)
	{
		new Throwable("J3dNiPSysModifierCtlr can't be controlled by a boolean interp").printStackTrace();
	}

	@Override
	public void update(Point3f value)
	{
		new Throwable("J3dNiPSysModifierCtlr can't be controlled by a Point3f interp").printStackTrace();
	}

}
