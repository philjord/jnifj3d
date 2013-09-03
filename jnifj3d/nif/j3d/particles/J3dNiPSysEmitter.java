package nif.j3d.particles;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysEmitter;
import utils.convert.ConvertFromNif;

public abstract class J3dNiPSysEmitter extends J3dNiPSysModifier
{
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

	public J3dNiPSysEmitter(NiPSysEmitter niPSysEmitter, NiToJ3dData niToJ3dData)
	{
		super(niPSysEmitter, niToJ3dData);

		speed = niPSysEmitter.speed;
		speedVariation = niPSysEmitter.speedVariation;
		declination = niPSysEmitter.declination;
		declinationVariation = niPSysEmitter.declinationVariation;
		planarAngle = niPSysEmitter.planarAngle;
		planarAngleVariation = niPSysEmitter.planarAngleVariation;
		initialColor = new Color4f(niPSysEmitter.initialColor.r, niPSysEmitter.initialColor.g, niPSysEmitter.initialColor.b,
				niPSysEmitter.initialColor.a);
		initialRadius = niPSysEmitter.initialRadius;
		radiusVariation = niPSysEmitter.radiusVariation;
		lifeSpan = niPSysEmitter.lifeSpan;
		lifeSpanVariation = niPSysEmitter.lifeSpanVariation;

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

	public void updateInitialRadius(float value)
	{
		initialRadius = value;
	}

	/**
	 * return 3 float x,y,z
	 * @return
	 */
	protected abstract void getCreationPoint(Point3f p);

	private AxisAngle4f aaZ = new AxisAngle4f(0, 0, 1, 0);

	private AxisAngle4f aaY = new AxisAngle4f(0, 1, 0, 0);

	private Vector3f vel = new Vector3f();

	private Point3f pos = new Point3f();

	private Color4f col = new Color4f();

	private Transform3D t = new Transform3D();

	private Transform3D t2 = new Transform3D();

	@Override
	public void updatePSys(long elapsedMillisec)
	{

		// This time controller holds two interpolators: one to control the emission rate (in particles per second) and another to specify when the emitter is active
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

	private void addParticle()
	{
		//NOTE NOTE, possibly it's just rotate around Z then pivot that point around Z??
		// yes confirmed, starts pointing up, declination rotates around Z (toward either + or - X) then PA pivots around Z

		float particleSpeed = speed;
		particleSpeed += var(speedVariation);
		particleSpeed = ConvertFromNif.toJ3d(particleSpeed);

		float dec = declination;
		dec += var(declinationVariation * 2);

		float pa = planarAngle;
		pa += var(planarAngleVariation * 2);

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
		radius += var(radiusVariation * 2);
		radius = ConvertFromNif.toJ3d(radius);

		float particleLifeSpan = lifeSpan;
		particleLifeSpan += var(lifeSpanVariation);
		particleLifeSpan *= 1000;// it's in seconds, convert to ms

		getCreationPoint(pos);

		int generation = 0;

		j3dNiParticleSystem.transformPosition(pos);

		int newParticleId = j3dNiParticleSystem.j3dPSysData.addActive(radius, (long) particleLifeSpan, generation, pos.x, pos.y, pos.z,
				col.x, col.y, col.z, col.w, vel.x, vel.y, vel.z);

		j3dNiParticleSystem.particleCreated(newParticleId);

	}

}
