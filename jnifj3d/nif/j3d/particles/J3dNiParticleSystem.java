package nif.j3d.particles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.media.j3d.Billboard;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3f;

import nif.basic.NifRef;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiTimeController;
import nif.niobject.bs.BSStripParticleSystem;
import nif.niobject.controller.NiTimeController;
import nif.niobject.particle.NiMeshParticleSystem;
import nif.niobject.particle.NiPSysData;
import nif.niobject.particle.NiPSysModifier;
import nif.niobject.particle.NiPSysModifierCtlr;
import nif.niobject.particle.NiParticleSystem;
import tools3d.utils.Utils3D;
import utils.PerTimeUpdateBehavior;
import utils.PhysAppearance;
import utils.source.TextureSource;

public class J3dNiParticleSystem extends J3dNiGeometry implements GeometryUpdater
{
	private ArrayList<J3dNiPSysModifier> modifiersInOrder = new ArrayList<J3dNiPSysModifier>();

	private HashMap<String, J3dNiPSysModifier> modifiersByName = new HashMap<String, J3dNiPSysModifier>();

	public HashMap<NiPSysModifierCtlr, J3dNiPSysModifierCtlr> j3dNiPSysModiferCtlrsByNi = new HashMap<NiPSysModifierCtlr, J3dNiPSysModifierCtlr>();

	public J3dPSysData j3dPSysData;

	private J3dNiAVObject particleRoot = null;

	private J3dNiPSysModifierCtlr rootJ3dNiPSysModifierCtlr = null;

	private NiParticleSystem niParticleSystem;

	public J3dNiParticleSystem(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{

		// the shape will not be added so we can choose to add it to a root we like in a moment
		super(niParticleSystem, niToJ3dData, textureSource, new Shape3D());
		this.niParticleSystem = niParticleSystem;

		niToJ3dData.put(niParticleSystem, this);

		NiPSysData niPSysData = (NiPSysData) niToJ3dData.get(niParticleSystem.data);

		if (niParticleSystem.worldSpace)
		{
			// transformPosition sorts out a decent world space		
			particleRoot = niToJ3dData.getJ3dRoot();
		}
		else
		{
			particleRoot = this;
		}
		particleRoot.addChild(getShape());

		// bill board to orient every quad to cameras proper like
		TransformGroup billTrans = new TransformGroup();
		billTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		billTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		particleRoot.addChild(billTrans);
		Billboard billBehave = new Billboard(billTrans, Billboard.ROTATE_ABOUT_POINT, new Point3f(0, 0, 0));
		billBehave.setEnable(true);
		billBehave.setSchedulingBounds(Utils3D.defaultBounds);
		addChild(billBehave);

		j3dPSysData = new J3dPSysData(niPSysData, billTrans);

		getShape().setGeometry(j3dPSysData.ga);

		//for debug
		//getShape().setAppearance(new PhysAppearance());

		// get updated every 50 milliseconds
		addChild(new PerTimeUpdateBehavior(50, new PerTimeUpdateBehavior.CallBack()
		{
			@Override
			public void update()
			{
				// set this as the geom updater and do the updates when called back (again)
				j3dPSysData.ga.updateData(J3dNiParticleSystem.this);
			}
		}));

		//2 types of sub classes with no extra data
		if (niParticleSystem instanceof BSStripParticleSystem)
		{
			//TODO: do I care?
		}
		else if (niParticleSystem instanceof NiMeshParticleSystem)
		{
			//TODO: do I care?
		}

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
		if (rootJ3dNiPSysModifierCtlr != null)
		{
			rootJ3dNiPSysModifierCtlr.process();
		}

		//System.out.println("frame update");
		for (J3dNiPSysModifier j3dNiPSysModifier : modifiersInOrder)
		{
			//TODO: this is hard coded to the PerTime behaviour above, needs to work out real time?
			if (j3dNiPSysModifier.active)
			{
				j3dNiPSysModifier.updatePSys(50L);
			}
		}

		// now we tell the particles to update the nett effects
		j3dPSysData.recalcAllGaCoords();

	}

	//deburner
	private Transform3D rootToPSysTrans = new Transform3D();

	public void transformPosition(Point3f pos)
	{
		if (niParticleSystem.worldSpace && particleRoot != this)
		{
			// now to work out where this particle system is relative to the root of the model tree (if it's world coords)
			// the PSYS will attach the particles to the root, the emitter just gives x,y,z values but this PSys may 
			// have been translated relative to root so I need to add the transform between  root and this
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

	private boolean modifiersSetup = false;

	private void setUpModifers(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData)
	{
		if (!modifiersSetup)
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
			modifiersSetup = true;
		}
	}

	// create controllers
	// I need to ensure all modifers are created as the controllers refer to them only by name
	private void setupControllers(NiParticleSystem niParticleSystem, NiToJ3dData niToJ3dData)
	{
		setUpModifers(niParticleSystem, niToJ3dData);
		NiTimeController cont = (NiTimeController) niToJ3dData.get(niParticleSystem.controller);
		if (cont != null)
		{
			rootJ3dNiPSysModifierCtlr = j3dNiPSysModiferCtlrsByNi.get(cont);
			if (rootJ3dNiPSysModifierCtlr == null)
			{
				rootJ3dNiPSysModifierCtlr = J3dNiPSysModifierCtlr.createJ3dNiPSysModifierCtlr(this, cont, niToJ3dData);
			}
		}
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

	public J3dNiPSysModifier getJ3dNiPSysModifier(String modifierName)
	{
		J3dNiPSysModifier j3dNiPSysModifier = modifiersByName.get(modifierName);
		if (j3dNiPSysModifier == null)
			System.out.println("J3dNiParticleSystem - modifierName " + modifierName + " not found in " + this);
		return j3dNiPSysModifier;
	}

	public J3dNiTimeController getJ3dNiPSysModifierCtlr(NiPSysModifierCtlr niPSysModifierCtlr, NiToJ3dData niToJ3dData)
	{
		// the controlled modifer will need to be ready
		setUpModifers(niParticleSystem, niToJ3dData);

		J3dNiTimeController j3dNiTimeController = j3dNiPSysModiferCtlrsByNi.get(niPSysModifierCtlr);
		// sometimes (always?) it's external to the particle system
		if (j3dNiTimeController == null)
		{
			j3dNiTimeController = J3dNiPSysModifierCtlr.createJ3dNiPSysModifierCtlr(this, niPSysModifierCtlr, niToJ3dData);
		}

		return j3dNiPSysModiferCtlrsByNi.get(niPSysModifierCtlr);
	}

}
