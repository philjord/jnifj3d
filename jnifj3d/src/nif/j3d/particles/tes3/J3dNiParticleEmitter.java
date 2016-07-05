package nif.j3d.particles.tes3;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.particle.NiParticleSystemController;
import utils.convert.ConvertFromNif;

/**
 * Wrapper around the single NiNode emitter system
 * @author phil
 *
 */
public class J3dNiParticleEmitter
{
	private J3dNiParticleSystemController j3dNiParticleSystemController;

	private J3dNiParticlesData j3dNiParticlesData;

	private J3dNiParticles parent;

	private J3dNiNode emitter;

	private J3dNiAVObject root;

	private float emitStart = 0;

	private float emitStop = 0;

	private boolean autoAdjust = true;

	private float birthRate = 0;

	private float speed;

	private float speedVariation;

	private float declination;

	private float declinationVariation;

	private float planarAngle;

	private float planarAngleVariation;

	private Color4f initialColor;

	private float initialRadius;

	private float radiusVariation;

	private float lifeSpan;

	private float lifeSpanVariation;

	public J3dNiParticleEmitter(NiParticleSystemController niParticleSystemController, J3dNiParticles parent,
			J3dNiParticleSystemController j3dNiParticleSystemController, J3dNiParticlesData j3dNiParticlesData, NiToJ3dData niToJ3dData)
	{
		this.parent = parent;
		this.j3dNiParticleSystemController = j3dNiParticleSystemController;
		this.j3dNiParticlesData = j3dNiParticlesData;

		emitter = (J3dNiNode) niToJ3dData.get((NiAVObject) niToJ3dData.get(niParticleSystemController.emitter));
		emitter.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

		//try to find nif root
		root = emitter.topOfParent;
		root.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		while (root.topOfParent != null)
		{
			root = root.topOfParent;
			root.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		}

		// parent is parent of particle system node, so we can emit in particle system space
		parent.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		J3dNiAVObject pt = parent.topOfParent;
		while (pt != null)
		{
			pt.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
			pt = pt.topOfParent;
		}
		emitStart = niParticleSystemController.emitStartTime;
		emitStop = niParticleSystemController.emitStopTime;
		autoAdjust = niParticleSystemController.emitFlags == 0;
		birthRate = niParticleSystemController.emitRate;
		speed = niParticleSystemController.speed;
		speedVariation = niParticleSystemController.speedRandom;
		declination = niParticleSystemController.verticalDirection;
		declinationVariation = niParticleSystemController.verticalAngle;
		planarAngle = niParticleSystemController.horizontalDirection;
		planarAngleVariation = niParticleSystemController.horizontalAngle;
		initialColor = new Color4f(1, 1, 1, 1);// maybe reset by the color modifier
		initialRadius = niParticleSystemController.size;
		radiusVariation = 0;
		lifeSpan = niParticleSystemController.lifetime;
		lifeSpanVariation = niParticleSystemController.lifetimeRandom;

	}

	public void updateBirthRate(float value)
	{
		this.birthRate = value;
	}

	public void updateLifeSpan(float value)
	{
		lifeSpan = value;
	}

	public void updateSpeed(float value)
	{
		speed = value;
	}

	public void updateDeclination(float value)
	{
		declination = value;
	}

	public void updateDeclinationVar(float value)
	{
		declinationVariation = value;
	}

	public void updatePlanarAngle(float value)
	{
		planarAngle = value;
	}

	public void updateInitialRadius(float value)
	{
		initialRadius = value;
	}

	//deburner
	private Vector3f v1 = new Vector3f();
	private Vector3f v2 = new Vector3f();
	private Transform3D t3 = new Transform3D();

	/**
	 * return 3 float x,y,z
	 * @return
	 */
	protected void getCreationPoint(Point3f pos)
	{
		// Notice this is location of the node, but I'm going to add to this node so I need to
		// find the relative position
		// which means using the getTreeTransfom and the root node for both the emitter
		// and the particle system location
		emitter.getTreeTransform(t3, root);
		t3.get(v1);

		parent.getTreeTransform(t3, root);
		t3.get(v2);
		v1.sub(v2);// get the diff
		pos.set(v1);
	}

	private AxisAngle4f aaZ = new AxisAngle4f(0, 0, 1, 0);

	private AxisAngle4f aaY = new AxisAngle4f(0, 1, 0, 0);

	private Vector3f vel = new Vector3f();

	private Point3f pos = new Point3f();

	private Color4f col = new Color4f();

	private Transform3D t = new Transform3D();

	private Transform3D t2 = new Transform3D();

	/**
	 * Note not the same interface as a modifier
	 */
	public void update(float timeSec, long elapsedMillisec)
	{
		if (timeSec >= emitStart && timeSec <= emitStop)
		{
			if (autoAdjust)
			{
				// just fill up to the max
				while (j3dNiParticlesData.activeParticleCount < j3dNiParticlesData.maxParticleCount)
				{
					addParticle();
				}
			}
			else
			{
				float birthRatePerUpdateTime = (birthRate / 1000f) * elapsedMillisec;

				while (birthRatePerUpdateTime > 0)
				{
					addParticle();
					birthRatePerUpdateTime -= 1;
				}
			}
		}

	}

	private void addParticle()
	{
		float particleSpeed = speed;
		particleSpeed += J3dNiParticleModifier.var(speedVariation);
		particleSpeed = ConvertFromNif.toJ3d(particleSpeed);

		float dec = declination;
		dec += J3dNiParticleModifier.var(declinationVariation * 2);

		float pa = planarAngle;
		pa += J3dNiParticleModifier.var(planarAngleVariation * 2);

		// calculate the velocity vector
		aaZ.setAngle(dec);
		aaY.setAngle(pa);
		t.set(aaZ);
		t2.set(aaY);
		t.mul(t2, t);
		vel.set(0, 1, 0);
		t.transform(vel);
		vel.scale(particleSpeed);

		col.set(initialColor);

		float radius = ConvertFromNif.toJ3d(initialRadius/2f);
		radius += J3dNiParticleModifier.var(ConvertFromNif.toJ3d(radiusVariation) * 2);

		float particleLifeSpan = lifeSpan;
		particleLifeSpan += J3dNiParticleModifier.var(lifeSpanVariation);
		particleLifeSpan *= 1000;// it's in seconds, convert to ms

		getCreationPoint(pos);

		int generation = 0;

		int newParticleId = j3dNiParticlesData.addActive(radius, (long) particleLifeSpan, generation, pos.x, pos.y, pos.z, col.x, col.y,
				col.z, col.w, vel.x, vel.y, vel.z);

		j3dNiParticleSystemController.particleCreated(newParticleId);
	}

}
