package nif.j3d.particles.tes3;

import java.util.ArrayList;

import javax.media.j3d.Alpha;
import javax.media.j3d.Bounds;
import javax.media.j3d.Group;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.compound.NifParticle;
import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.j3dinterp.J3dNiFloatInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.niobject.NiAVObject;
import nif.niobject.particle.NiParticleSystemController;
import tools3d.utils.Utils3D;
import utils.convert.ConvertFromNif;

public class J3dNiParticleSystemController extends J3dNiTimeController
{
	protected ArrayList<J3dNiParticleModifier> modifiersInOrder = new ArrayList<J3dNiParticleModifier>();

	private J3dNiInterpolator j3dNiInterpolator;

	private Alpha baseAlpha;// created as looping, so never needs resetting

	private J3dNiParticleSystemController nextJ3dNiParticleSystemController;

	private J3dNiNode emmitter;

	public J3dNiParticleSystemController(NiParticleSystemController niParticleSystemController,
			J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niParticleSystemController, null);

		// only one call back this, which hands float off to all modifiers
		J3dNiInterpolator j3dNiInterpolator = new J3dNiFloatInterpolator(niParticleSystemController.startTime,
				niParticleSystemController.stopTime, this);
		Alpha baseAlpha = J3dNiTimeController.createLoopingAlpha(niParticleSystemController.startTime, niParticleSystemController.stopTime);
		setInterpolator(j3dNiInterpolator, baseAlpha);

		emmitter = (J3dNiNode) niToJ3dData.get((NiAVObject) niToJ3dData.get(niParticleSystemController.emitter));
		emmitter.setCapability(Group.ALLOW_LOCAL_TO_VWORLD_READ);

		// now add initial data to the particles
		for (int indx = 0; indx < niParticleSystemController.numValid; indx++)
		{
			NifParticle p = niParticleSystemController.particles[indx];
			Vector3f vel = ConvertFromNif.toJ3d(p.velocity);
			j3dNiAutoNormalParticlesData.particleVelocity[indx * 3 + 0] = vel.x;
			j3dNiAutoNormalParticlesData.particleVelocity[indx * 3 + 1] = vel.y;
			j3dNiAutoNormalParticlesData.particleVelocity[indx * 3 + 2] = vel.z;

			j3dNiAutoNormalParticlesData.particleAge[indx] = (long) (p.lifetime * 1000);
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

	@Override
	public void update(float value)
	{

		// do spawn age and death now, spawn needs to use the mitter location
		// IN fact controller needs to do all the spawn age death type operations too

		for (J3dNiParticleModifier j3dNiParticleModifier : modifiersInOrder)
		{
			//TODO: this is hard coded to the PerTime behaviour above, needs to work out real time?
			j3dNiParticleModifier.updatePSys(50L);
		}
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
