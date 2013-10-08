package nif.j3d.particles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.OrientedShape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;

import nif.basic.NifRef;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiObject;
import nif.niobject.bs.BSFadeNode;
import nif.niobject.controller.NiTimeController;
import nif.niobject.particle.NiPSysData;
import nif.niobject.particle.NiPSysModifier;
import nif.niobject.particle.NiPSysModifierCtlr;
import nif.niobject.particle.NiParticleSystem;
import utils.PerTimeUpdateBehavior;
import utils.source.TextureSource;

public class J3dNiParticleSystem extends J3dNiGeometry implements GeometryUpdater
{
	private ArrayList<J3dNiPSysModifier> modifiersInOrder = new ArrayList<J3dNiPSysModifier>();

	private HashMap<String, J3dNiPSysModifier> modifiersByName = new HashMap<String, J3dNiPSysModifier>();

	private HashMap<NiPSysModifierCtlr, J3dNiPSysModifierCtlr> j3dNiPSysModiferCtlrsByNi = new HashMap<NiPSysModifierCtlr, J3dNiPSysModifierCtlr>();

	public J3dPSysData j3dPSysData;

	private J3dNiAVObject particleRoot = null;

	private NiParticleSystem niParticleSystem;

	public J3dNiParticleSystem(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{

		// note this is an oriented shape super call
		super(niParticleSystem, niToJ3dData, textureSource, new OrientedShape3D());
		this.niParticleSystem = niParticleSystem;

		niToJ3dData.put(niParticleSystem, this);

		NiPSysData niPSysData = (NiPSysData) niToJ3dData.get(niParticleSystem.data);

		OrientedShape3D orientedShape = (OrientedShape3D) getShape();
		orientedShape.setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_POINT);

		if (niParticleSystem.worldSpace)
		{
			// let's see if we can find the BSFade node at teh top of the model tree? then world space can attach to that.
			for (NiObject o : niToJ3dData.getNiObjects())
			{
				if (o instanceof BSFadeNode)
				{
					particleRoot = niToJ3dData.get((BSFadeNode) o);
				}
			}
		}

		if (particleRoot == null)
		{
			particleRoot = this;
		}

		// because we handed in a custom shape will not be attached yet

		particleRoot.addChild(getShape());

		j3dPSysData = new J3dPSysData(niPSysData);

		orientedShape.setGeometry(j3dPSysData.ga);

		// get updated every 50 milliseconds
		addChild(new PerTimeUpdateBehavior(50L, new PerTimeUpdateBehavior.CallBack()
		{
			@Override
			public void update()
			{
				// set this as the geom updater and do the updates when called back (again)
				j3dPSysData.ga.updateData(J3dNiParticleSystem.this);
			}
		}));

	}

	/**
	 * Note this override the NiObjectNET method completely
	 * @see nif.j3d.J3dNiObjectNET#setupController(nif.j3d.NiToJ3dData)
	 */
	@Override
	public void setupController(NiToJ3dData niToJ3dData)
	{
		setUpModifers(niParticleSystem, niToJ3dData);
		setupControllers(niParticleSystem, niToJ3dData);
	}

	@Override
	public void updateData(Geometry geometry)
	{
		//System.out.println("frame update");
		for (J3dNiPSysModifier j3dNiPSysModifier : modifiersInOrder)
		{
			//TODO: this is hard coded to the PerTime behaviour above, needs to work out real time?
			if (j3dNiPSysModifier.active)
			{
				j3dNiPSysModifier.updatePSys(50L);
			}
		}

		// now we tell teh particles to update the nett effects
		j3dPSysData.recalcAllGaCoords();
	}

	//deburner
	private Transform3D rootToPSysTrans = new Transform3D();

	public void transformPosition(Point3f pos)
	{
		if (niParticleSystem.worldSpace && particleRoot != this)
		{
			// now to work out where this particle system is relative to the root of the model tree (if it's world coords)
			// the PSYS will attch the particles to the root, the emitter just gives x,y,z values but this PSys may have been translated relative to root
			// so I need to add the trnasform between bsfadenode root and this
			this.getTreeTransform(rootToPSysTrans, particleRoot);
			// I need to go back to root space
			rootToPSysTrans.invert();
			rootToPSysTrans.transform(pos);
		}
	}

	public void particleCreated(int newParticleId)
	{
		if (newParticleId != -1)
		{
			// now tell all modifiers about the new particles so they can make updates to it (like add rotation etc)
			for (J3dNiPSysModifier j3dNiPSysModifier : modifiersInOrder)
			{
				j3dNiPSysModifier.particleCreated(newParticleId);
			}
		}
	}

	private void setUpModifers(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData)
	{
		// for all referenced mods
		for (NifRef nr : niParticleSystem.modifiers)
		{
			NiPSysModifier niPSysModifier = (NiPSysModifier) niToJ3dData.get(nr);
			// ensure it is created
			getJ3dNiPSysModifier(niPSysModifier, niToJ3dData);
		}

		// sort by the order number
		modifiersInOrder.clear();
		modifiersInOrder.addAll(modifiersByName.values());
		Collections.sort(modifiersInOrder, new Comparator<J3dNiPSysModifier>()
		{
			@Override
			public int compare(J3dNiPSysModifier o1, J3dNiPSysModifier o2)
			{
				return o1.order < o2.order ? -1 : o1.order == o2.order ? 0 : 1;
			}
		});
	}

	public J3dNiPSysModifier getJ3dNiPSysModifier(NiPSysModifier niPSysModifier, NiToJ3dData niToJ3dData)
	{
		J3dNiPSysModifier j3dNiPSysModifier = modifiersByName.get(niPSysModifier.name);
		if (j3dNiPSysModifier == null)
		{
			j3dNiPSysModifier = J3dNiPSysModifier.createJ3dNiPSysModifier(niPSysModifier, niToJ3dData);
			if (j3dNiPSysModifier != null)
			{
				modifiersByName.put(niPSysModifier.name, j3dNiPSysModifier);
			}
		}
		return j3dNiPSysModifier;
	}

	public J3dNiPSysModifierCtlr getJ3dNiPSysModifierCtlr(NiPSysModifierCtlr niPSysModifierCtlr, NiToJ3dData niToJ3dData)
	{
		// I need to ensure all modifers are created as the controllers refer to them only by name
		setUpModifers(niParticleSystem, niToJ3dData);

		J3dNiPSysModifierCtlr j3dNiPSysModifierCtlr = j3dNiPSysModiferCtlrsByNi.get(niPSysModifierCtlr);
		if (j3dNiPSysModifierCtlr == null)
		{
			j3dNiPSysModifierCtlr = J3dNiPSysModifierCtlr.createJ3dNiPSysModifierCtlr(this, niPSysModifierCtlr, niToJ3dData);
			if (j3dNiPSysModifierCtlr != null)
			{
				j3dNiPSysModiferCtlrsByNi.put(niPSysModifierCtlr, j3dNiPSysModifierCtlr);
				niToJ3dData.put(niPSysModifierCtlr, j3dNiPSysModifierCtlr);
			}
		}
		return j3dNiPSysModifierCtlr;
	}

	// create controllers
	private void setupControllers(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData)
	{
		NiTimeController cont = (NiTimeController) niToJ3dData.get(niParticleSystem.controller);

		while (cont != null && cont instanceof NiPSysModifierCtlr)
		{
			NiPSysModifierCtlr niPSysModifierCtlr = (NiPSysModifierCtlr) cont;
			getJ3dNiPSysModifierCtlr(niPSysModifierCtlr, niToJ3dData);
			cont = (NiTimeController) niToJ3dData.get(cont.nextController);
		}
	}

	public J3dNiPSysModifier getJ3dNiPSysModifier(String modifierName)
	{
		J3dNiPSysModifier j3dNiPSysModifier = modifiersByName.get(modifierName);
		if (j3dNiPSysModifier == null)
			System.out.println("J3dNiParticleSystem - modifierName " + modifierName + " not found in " + this);
		return j3dNiPSysModifier;
	}

}