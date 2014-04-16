package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysArrayEmitter;
import nif.niobject.bs.BSPSysHavokUpdateModifier;
import nif.niobject.bs.BSPSysInheritVelocityModifier;
import nif.niobject.bs.BSPSysLODModifier;
import nif.niobject.bs.BSPSysRecycleBoundModifier;
import nif.niobject.bs.BSPSysScaleModifier;
import nif.niobject.bs.BSPSysSimpleColorModifier;
import nif.niobject.bs.BSPSysStripUpdateModifier;
import nif.niobject.bs.BSPSysSubTexModifier;
import nif.niobject.bs.BSParentVelocityModifier;
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
		else if (niPSysModifier instanceof BSPSysArrayEmitter)
		{
			return new J3dBSPSysArrayEmitter((BSPSysArrayEmitter) niPSysModifier, niToJ3dData);
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

		else if (niPSysModifier instanceof NiPSysColliderManager)
		{
			return new J3dNiPSysColliderManager((NiPSysColliderManager) niPSysModifier, niToJ3dData);
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
		else if (niPSysModifier instanceof NiPSysDragModifier)
		{
			return new J3dNiPSysDragModifier((NiPSysDragModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysBombModifier)
		{
			return new J3dNiPSysBombModifier((NiPSysBombModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSWindModifier)
		{
			return new J3dBSWindModifier((BSWindModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysColorModifier)
		{
			return new J3dNiPSysColorModifier((NiPSysColorModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof NiPSysBoundUpdateModifier)
		{
			return new J3dNiPSysBoundUpdateModifier((NiPSysBoundUpdateModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysLODModifier)
		{
			return new J3dBSPSysLODModifier((BSPSysLODModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysInheritVelocityModifier)
		{
			return new J3dBSPSysInheritVelocityModifier((BSPSysInheritVelocityModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysSubTexModifier)
		{
			return new J3dBSPSysSubTexModifier((BSPSysSubTexModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysScaleModifier)
		{
			return new J3dBSPSysScaleModifier((BSPSysScaleModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysRecycleBoundModifier)
		{
			return new J3dBSPSysRecycleBoundModifier((BSPSysRecycleBoundModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysHavokUpdateModifier)
		{
			return new J3dBSPSysHavokUpdateModifier((BSPSysHavokUpdateModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSPSysStripUpdateModifier)
		{
			return new J3dBSPSysStripUpdateModifier((BSPSysStripUpdateModifier) niPSysModifier, niToJ3dData);
		}
		else if (niPSysModifier instanceof BSParentVelocityModifier)
		{
			return new J3dBSParentVelocityModifier((BSParentVelocityModifier) niPSysModifier, niToJ3dData);
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
