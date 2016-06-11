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

	private J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData;

	private J3dNiAutoNormalParticles parent;

	private J3dNiNode emmitter;
	private J3dNiAVObject root;

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

	public J3dNiParticleEmitter(NiParticleSystemController niParticleSystemController, J3dNiAutoNormalParticles parent,
			J3dNiParticleSystemController j3dNiParticleSystemController, J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData,
			NiToJ3dData niToJ3dData)
	{
		this.parent = parent;
		this.j3dNiParticleSystemController = j3dNiParticleSystemController;
		this.j3dNiAutoNormalParticlesData = j3dNiAutoNormalParticlesData;

		emmitter = (J3dNiNode) niToJ3dData.get((NiAVObject) niToJ3dData.get(niParticleSystemController.emitter));

		//try to find nif root
		root = emmitter.topOfParent;
		root.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		while (root.topOfParent != null)
		{
			root = root.topOfParent;
			root.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		}

		parent.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		J3dNiAVObject pt = parent.topOfParent;
		while (pt != null)
		{
			pt.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
			pt = pt.topOfParent;
		}

		autoAdjust = niParticleSystemController.emitFlags == 0;
		birthRate = niParticleSystemController.emitRate;
		speed = niParticleSystemController.speed;
		speedVariation = niParticleSystemController.speedRandom;
		declination = niParticleSystemController.verticalDirection;
		declinationVariation = niParticleSystemController.verticalAngle;
		planarAngle = niParticleSystemController.horizontalDirection;
		planarAngleVariation = niParticleSystemController.horizontalAngle;
		initialColor = new Color4f(0, 0, 0, 0);// set by the color modifier
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
		//find the relative position
		// which means using the getTreeTransfom and the root node for both the emitter
		// and the particle system location
		emmitter.getTreeTransform(t3, root);
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

	public void update(long elapsedMillisec)
	{
		if (autoAdjust)
		{
			if (j3dNiAutoNormalParticlesData.activeParticleCount < j3dNiAutoNormalParticlesData.maxParticleCount)
				addParticle();
		}
		else
		{
			float birthRatePerUpdateTime = (birthRate / 1000f) * elapsedMillisec;

			while (birthRatePerUpdateTime > 1)
			{
				addParticle();
				birthRatePerUpdateTime -= 1;
			}

			boolean shouldAdd = Math.random() < birthRatePerUpdateTime;

			//See file:///C:/Emergent/Gamebryo-LightSpeed-Binary/Documentation/HTML/Art/Max/Particles.htm
			if (shouldAdd)
			{
				addParticle();
			}
		}

	}

	private void addParticle()
	{
		//NOTE NOTE, possibly it's just rotate around Z then pivot that point around Z??
		// yes confirmed, starts pointing up, declination rotates around Z (toward either + or - X) then PA pivots around Z

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
		t.mul(t2);
		vel.set(0, 1, 0);
		t.transform(vel);
		vel.scale(particleSpeed);

		col.set(initialColor);

		float radius = initialRadius;
		radius += J3dNiParticleModifier.var(radiusVariation * 2);
		radius = ConvertFromNif.toJ3d(radius);

		float particleLifeSpan = lifeSpan;
		particleLifeSpan += J3dNiParticleModifier.var(lifeSpanVariation);
		particleLifeSpan *= 1000;// it's in seconds, convert to ms

		getCreationPoint(pos);

		int generation = 0;

		int newParticleId = j3dNiAutoNormalParticlesData.addActive(radius, (long) particleLifeSpan, generation, pos.x, pos.y, pos.z, col.x,
				col.y, col.z, col.w, vel.x, vel.y, vel.z);

		j3dNiParticleSystemController.particleCreated(newParticleId);

	}

}
