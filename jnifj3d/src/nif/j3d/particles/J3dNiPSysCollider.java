package nif.j3d.particles;

import org.jogamp.java3d.Group;
import org.jogamp.java3d.Transform3D;
import org.jogamp.java3d.TransformGroup;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiNode;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.NiObject;
import nif.niobject.particle.NiPSysCollider;
import nif.niobject.particle.NiPSysPlanarCollider;
import nif.niobject.particle.NiPSysSpawnModifier;
import nif.niobject.particle.NiPSysSphericalCollider;

public abstract class J3dNiPSysCollider {

	/**
	 * <niobject name="NiPSysCollider" abstract="1" inherit="NiObject">
	 * 
	 * Particle system collider.
	 * 
	 * <add name="Bounce" type="float">Defines amount of bounce the collider object has.</add>
	 * <add name="Spawn on Collide" type="bool">Unknown.</add>
	 * <add name="Die on Collide" type="bool">Kill particles on impact if set to yes.</add> 
	 * <add name="Spawn Modifier" type="Ref" template="NiPSysSpawnModifier">Link to NiPSysSpawnModifier object?</add> 
	 * <add name="Parent" type="Ptr" template="NiObject">Link to parent.</add>
	 * <add name="Next Collider" type="Ref" template="NiObject">The next collider.</add>
	 * <add name="Collider Object" type="Ptr" template="NiNode"> Links to a NiNode that will define where in object
	 * space the collider is located/oriented. </add> </niobject>
	 * 
	 */

	public float					bounce;

	public boolean					spawnonCollide;

	public boolean					dieonCollide;

	private J3dNiPSysSpawnModifier	spawnModifier;

	public J3dNiPSysColliderManager	parent;

	protected J3dNiPSysCollider		nextCollider;

	protected J3dNiNode				j3dNiNode;
	private J3dNiAVObject			root;

	public J3dNiPSysCollider(	NiPSysCollider niPSysCollider, NiToJ3dData niToJ3dData,
								J3dNiParticleSystem j3dNiParticleSystem) {

		this.bounce = niPSysCollider.bounce;
		this.spawnonCollide = niPSysCollider.spawnonCollide;
		this.dieonCollide = niPSysCollider.dieonCollide;

		NiObject niObject = niToJ3dData.get(niPSysCollider.spawnModifier);
		if (niObject instanceof NiPSysSpawnModifier) {
			spawnModifier = (J3dNiPSysSpawnModifier)j3dNiParticleSystem
					.getJ3dNiPSysModifier((NiPSysSpawnModifier)niObject, niToJ3dData);
		}

		//NiObject parentNiObject = niToJ3dData.get(niPSysCollider.parent);
		//oops can't do this as we are in the create of J3dNiPSysColliderManager and so we will recurse forever
		/*if (parentNiObject instanceof NiPSysColliderManager) {
			parent = (J3dNiPSysColliderManager)j3dNiParticleSystem
					.getJ3dNiPSysModifier((NiPSysColliderManager)parentNiObject, niToJ3dData);
		}*/

		NiObject nextColliderNiObject = niToJ3dData.get(niPSysCollider.nextCollider);
		if (niObject instanceof NiPSysCollider) {
			nextCollider = j3dNiParticleSystem.getJ3dNiPSysCollider((NiPSysCollider)nextColliderNiObject, niToJ3dData);
		}

		//TODO: this should be common to the emitter 2 class that look the same, and bomb modifer
		j3dNiNode = (J3dNiNode)niToJ3dData.get((NiAVObject)niToJ3dData.get(niPSysCollider.colliderObject));

		//make the caps correct up to the root
		root = niToJ3dData.getJ3dRoot();
		Group g = j3dNiNode;
		while (g != null) {
			g.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
			g.setCapability(Group.ALLOW_PARENT_READ);
			if (g == niToJ3dData.getJ3dRoot())
				break;
			g = (Group)g.getParent();
		}

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.println("J3dNiPSysCollider abstract parent of the next one");
			System.out.print(" bounce " + bounce);
			System.out.print(" spawnonCollide " + spawnonCollide);
			System.out.print(" dieonCollide " + dieonCollide);
			System.out.print(" spawnModifier " + spawnModifier);
			System.out.print(" parent " + parent);
			System.out.println(" nextCollider " + nextCollider);
		}
	}

	public void particleCreated(int pId) {
		if (spawnModifier != null)
			spawnModifier.particleCreated(pId);
		if (nextCollider != null)
			nextCollider.particleCreated(pId);
	}

	public void particleDeath(int pId) {
		if (spawnModifier != null)
			spawnModifier.particleDeath(pId);
		if (nextCollider != null)
			nextCollider.particleDeath(pId);
	}

	protected Point3f	collLoc		= new Point3f();

	protected Point3f	velApplied	= new Point3f();

	protected Vector3f	partLoc		= new Vector3f();
	
	//deburner
	private Transform3D	trans		= new Transform3D();
	
	protected Transform3D getCurrentNiNodeTransform() {
		j3dNiNode.getTreeTransform(trans, root);
		return trans;
	}

	protected abstract boolean checkCollision(Point3f loc, Vector3f vel, Vector3f newVel);

	public static J3dNiPSysCollider createJ3dNiPSysCollider(NiPSysCollider niPSysCollider, NiToJ3dData niToJ3dData,
															J3dNiParticleSystem j3dNiParticleSystem) {
		if (niPSysCollider instanceof NiPSysPlanarCollider) {
			return new J3dNiPSysPlanarCollider((NiPSysPlanarCollider)niPSysCollider, niToJ3dData, j3dNiParticleSystem);
		} else if (niPSysCollider instanceof NiPSysSphericalCollider) {
			return new J3dNiPSysSphericalCollider((NiPSysSphericalCollider)niPSysCollider, niToJ3dData,
					j3dNiParticleSystem);
		} else {
			System.out.println("Eh new unknown NiPSysCollider " + niPSysCollider);
		}
		return null;
	}

	@Override
	public String toString() {
		return "[" + this.getClass().getSimpleName() + "] ";
	}
}
