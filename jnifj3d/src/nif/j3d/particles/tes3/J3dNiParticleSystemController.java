package nif.j3d.particles.tes3;

import java.util.ArrayList;

import javax.media.j3d.Alpha;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.Node;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.compound.NifParticle;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.SequenceAlpha;
import nif.j3d.animation.j3dinterp.J3dNiFloatInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.niobject.particle.NiParticleSystemController;
import tools3d.utils.Utils3D;
import tools3d.utils.scenegraph.VaryingLODBehaviour;
import utils.convert.ConvertFromNif;

public class J3dNiParticleSystemController extends J3dNiTimeController
{
	protected ArrayList<J3dNiParticleModifier> modifiersInOrder = new ArrayList<J3dNiParticleModifier>();

	private J3dNiInterpolator j3dNiInterpolator;

	private J3dNiParticleSystemController nextJ3dNiParticleSystemController;

	private J3dNiParticlesData j3dNiParticlesData;

	private J3dNiParticleEmitter j3dNiParticleEmitter;

	private SequenceAlpha sequenceAlpha;

	private SequenceBehavior sequenceBehavior = new SequenceBehavior(this);

	protected long lengthMS = 0;

	protected float startTimeS = 0;

	protected float stopTimeS = 0;

	protected float lengthS = 0;

	public J3dNiParticleSystemController(NiParticleSystemController niParticleSystemController, J3dNiParticles parent,
			J3dNiParticlesData j3dNiParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niParticleSystemController, null);

		this.j3dNiParticlesData = j3dNiParticlesData;

		startTimeS = niParticleSystemController.startTime;
		stopTimeS = niParticleSystemController.stopTime;
		lengthS = stopTimeS - startTimeS;

		// only one call back this, which hands float off to all modifiers
		J3dNiInterpolator j3dNiInterpolator = new J3dNiFloatInterpolator(niParticleSystemController.startTime,
				niParticleSystemController.stopTime, this);
		Alpha baseAlpha = J3dNiTimeController.createLoopingAlpha(startTimeS, stopTimeS);
		setInterpolator(j3dNiInterpolator, baseAlpha);

		j3dNiParticleEmitter = new J3dNiParticleEmitter(niParticleSystemController, parent, this, j3dNiParticlesData, niToJ3dData);

		// now add initial data to the particles
		for (int indx = 0; indx < niParticleSystemController.numValid; indx++)
		{
			NifParticle p = niParticleSystemController.particles[indx];
			Vector3f vel = ConvertFromNif.toJ3d(p.velocity);// notice scale as we are meters per second
			j3dNiParticlesData.particleVelocity[indx * 3 + 0] = vel.x;
			j3dNiParticlesData.particleVelocity[indx * 3 + 1] = vel.y;
			j3dNiParticlesData.particleVelocity[indx * 3 + 2] = vel.z;

			j3dNiParticlesData.particleAge[indx] = (long) (p.lifetime * 1000);// secs to milli secs
			j3dNiParticlesData.particleLifeSpan[indx] = (long) (p.lifespan * 1000);
		}

		sequenceBehavior.setEnable(false);
		sequenceBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(sequenceBehavior);
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
	}

	public void setNextController(J3dNiParticleSystemController nextJ3dNiParticleSystemController)
	{
		this.nextJ3dNiParticleSystemController = nextJ3dNiParticleSystemController;
	}

	public void processSequence(float alphaValue)
	{
		if (j3dNiInterpolator != null)
		{
			j3dNiInterpolator.process(alphaValue);
		}

		// fire the next controller
		if (nextJ3dNiParticleSystemController != null)
		{
			nextJ3dNiParticleSystemController.processSequence(alphaValue);
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
		long fixedTime = 50L;

		// there is no modifier to control these 2 aspects so done here
		updateAge(fixedTime);
		updatePosition(fixedTime);
		j3dNiParticleEmitter.update(fixedTime);

		for (J3dNiParticleModifier j3dNiParticleModifier : modifiersInOrder)
		{
			//TODO: this is hard coded to the PerTime behaviour above, needs to work out real time?
			j3dNiParticleModifier.updateParticles(fixedTime);
		}

		// Because updatePosition and  gravity affect this
		j3dNiParticlesData.recalcAllGaCoords();
	}

	public void updateAge(long elapsedMillisec)
	{
		long[] as = j3dNiParticlesData.particleAge; // in milliseconds
		long[] lss = j3dNiParticlesData.particleLifeSpan; // in ms

		for (int i = 0; i < j3dNiParticlesData.activeParticleCount; i++)
		{
			as[i] += elapsedMillisec;
			// is the particle past it's lifespan?
			if (lss[i] < as[i])
			{
				System.out.println("killing " + i);
				j3dNiParticlesData.inactivateParticle(i);
			}
		}
	}

	public void updatePosition(long elapsedMillisec)
	{
		// simply grab the velocity for an active particle and add it on to the translation
		// velocitys are in meters per second

		float fractionOfSec = elapsedMillisec / 1000f;

		float[] vs = j3dNiParticlesData.particleVelocity;
		float[] ts = j3dNiParticlesData.particleTranslation;
		for (int i = 0; i < j3dNiParticlesData.activeParticleCount; i++)
		{
			ts[i * 3 + 0] += vs[i * 3 + 0] * fractionOfSec;
			ts[i * 3 + 1] += vs[i * 3 + 1] * fractionOfSec;
			ts[i * 3 + 2] += vs[i * 3 + 2] * fractionOfSec;
		}
		// note j3dNiAutoNormalParticlesData.recalcAllGaCoords(); will be called once by the particle system after all modifiers have run
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

	/**
	 * Taken from controller sequence to look similar
	 * @return
	 */
	public boolean isNotRunning()
	{
		return sequenceAlpha == null || sequenceAlpha.finished();
	}

	/**
	 * Taken from controller sequence to look similar
	 * This will trigger the sequence looping in not possible
	 */
	public void fireSequence()
	{
		sequenceBehavior.setEnable(false);

		//in theory the start time is working right here right now?
		sequenceAlpha = new SequenceAlpha(startTimeS, stopTimeS, false);
		sequenceAlpha.start();

		sequenceBehavior.setEnable(true);// disables after loop if required
	}

	public float getLengthS()
	{
		return lengthS;
	}

	public class SequenceBehavior extends VaryingLODBehaviour
	{
		public SequenceBehavior(Node node)
		{
			// NOTE!!!! these MUST be active, otherwise the headless locale that might be running physics doesn't continuously render
			super(node, new float[] { 40, 120, 280 }, false, true);
		}

		@Override
		public void process()
		{
			float alphaValue = sequenceAlpha.value();
			processSequence(alphaValue);

			//turn off at the end
			if (sequenceAlpha.finished())
				setEnable(false);
		}
	}

}
