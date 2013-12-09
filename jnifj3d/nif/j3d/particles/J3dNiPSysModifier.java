package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysInheritVelocityModifier;
import nif.niobject.bs.BSPSysLODModifier;
import nif.niobject.bs.BSPSysRecycleBoundModifier;
import nif.niobject.bs.BSPSysScaleModifier;
import nif.niobject.bs.BSPSysSimpleColorModifier;
import nif.niobject.bs.BSPSysStripUpdateModifier;
import nif.niobject.bs.BSPSysSubTexModifier;
import nif.niobject.bs.BSWindModifier;
import nif.niobject.particle.NiPSysAgeDeathModifier;
import nif.niobject.particle.NiPSysBombModifier;
import nif.niobject.particle.NiPSysBoundUpdateModifier;
import nif.niobject.particle.NiPSysBoxEmitter;
import nif.niobject.particle.NiPSysColliderManager;
import nif.niobject.particle.NiPSysColorModifier;
import nif.niobject.particle.NiPSysCylinderEmitter;
import nif.niobject.particle.NiPSysDragModifier;
import nif.niobject.particle.NiPSysGravityModifier;
import nif.niobject.particle.NiPSysGrowFadeModifier;
import nif.niobject.particle.NiPSysMeshEmitter;
import nif.niobject.particle.NiPSysModifier;
import nif.niobject.particle.NiPSysPositionModifier;
import nif.niobject.particle.NiPSysRotationModifier;
import nif.niobject.particle.NiPSysSpawnModifier;
import nif.niobject.particle.NiPSysSphereEmitter;
import nif.niobject.particle.NiParticleSystem;

public abstract class J3dNiPSysModifier
{
	public String name;

	public int order = 0;

	public boolean active;

	protected J3dNiParticleSystem j3dNiParticleSystem;

	public J3dNiPSysModifier(NiPSysModifier niPSysModifier, NiToJ3dData niToJ3dData)
	{
		this.name = niPSysModifier.name;
		this.order = niPSysModifier.order;
		this.active = niPSysModifier.active;
		this.j3dNiParticleSystem = (J3dNiParticleSystem) niToJ3dData.get((NiParticleSystem) niToJ3dData.get(niPSysModifier.target));
	}

	// Called by particle system to get the modifier to apply it's effect, to a newly created particle
	public void particleCreated(int pId)
	{
		//default ignore
	}

	// Called by particle system to get the modifier to apply it's effect
	public abstract void updatePSys(long elapsedMillisec);

	public void updateActive(boolean value)
	{
		this.active = value;
	}

	public static J3dNiPSysModifier createJ3dNiPSysModifier(NiPSysModifier niPSysModifier, NiToJ3dData niToJ3dData)
	{
		if (niPSysModifier instanceof NiPSysBoxEmitter)
		{
			return new J3dNiPSysBoxEmitter((NiPSysBoxEmitter) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysAgeDeathModifier)
		{
			return new J3dNiPSysAgeDeathModifier((NiPSysAgeDeathModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysSpawnModifier)
		{
			return new J3dNiPSysSpawnModifier((NiPSysSpawnModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysRotationModifier)
		{
			return new J3dNiPSysRotationModifier((NiPSysRotationModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysGravityModifier)
		{
			return new J3dNiPSysGravityModifier((NiPSysGravityModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysPositionModifier)
		{
			return new J3dNiPSysPositionModifier((NiPSysPositionModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysGrowFadeModifier)
		{
			return new J3dNiPSysGrowFadeModifier((NiPSysGrowFadeModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysSimpleColorModifier)
		{
			return new J3dBSPSysSimpleColorModifier((BSPSysSimpleColorModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysCylinderEmitter)
		{
			return new J3dNiPSysCylinderEmitter((NiPSysCylinderEmitter) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysSphereEmitter)
		{
			return new J3dNiPSysSphereEmitter((NiPSysSphereEmitter) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysMeshEmitter)
		{
			return new J3dNiPSysMeshEmitter((NiPSysMeshEmitter) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysDragModifier)
		{
			return new J3dNiPSysDragModifier((NiPSysDragModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysColliderManager)
		{
			return new J3dNiPSysColliderManager((NiPSysColliderManager) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysBombModifier)
		{
			return new J3dNiPSysBombModifier((NiPSysBombModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSWindModifier)
		{
			return new J3dBSWindModifier((BSWindModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysStripUpdateModifier)
		{
			//uncommon
		}
		else if (niPSysModifier instanceof NiPSysColorModifier)
		{
			//uncommon
		}
		else if (niPSysModifier instanceof NiPSysBoundUpdateModifier)
		{
			//technical
		}
		else if (niPSysModifier instanceof BSPSysLODModifier)
		{
			//TODO: BSPSysLODModifier
		}
		else if (niPSysModifier instanceof BSPSysInheritVelocityModifier)
		{
			//TODO: BSPSysInheritVelocityModifier
		}
		else if (niPSysModifier instanceof BSPSysSubTexModifier)
		{
			//TODO: BSPSysSubTexModifier
		}
		else if (niPSysModifier instanceof BSPSysScaleModifier)
		{
			//TODO: BSPSysScaleModifier
		}
		else if (niPSysModifier instanceof BSPSysRecycleBoundModifier)
		{
			//TODO: BSPSysRecycleBoundModifier
		}
		else
		{
			System.out.println("J3dNiPSysModifier createJ3dNiPSysModifier unhandled NiPSysModifier " + niPSysModifier);
		}

		return null;
	}

	protected static float var(float range)
	{
		return (float) (Math.random() * range) - range / 2f;
	}
}
