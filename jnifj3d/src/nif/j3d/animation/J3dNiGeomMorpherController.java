package nif.j3d.animation;

import java.util.ArrayList;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GeometryUpdater;
import javax.media.j3d.Node;
import javax.vecmath.Point3d;

import nif.NifVer;
import nif.basic.NifRef;
import nif.compound.NifMorph;
import nif.compound.NifMorphWeight;
import nif.j3d.J3dNiTriBasedGeom;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.J3dNiFloatInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.niobject.NiAVObject;
import nif.niobject.NiMorphData;
import nif.niobject.controller.NiGeomMorpherController;
import nif.niobject.interpolator.NiInterpolator;
import tools3d.utils.scenegraph.VaryingLODBehaviour;
import utils.ESConfig;

public class J3dNiGeomMorpherController extends J3dNiTimeController
{
	private GeometryArray currentGeoArray;

	private GeometryArray baseGeoArray;

	private NiMorphData niMorphData;

	private NifMorph currentNifMorph = null;

	private J3dNiInterpolator currentJ3dNiInterpolator = null;

	private SequenceBehavior sequenceBehavior = new SequenceBehavior(this);

	private float startTimeS;

	private float stopTimeS;

	private SequenceAlpha sequenceAlpha;

	private ArrayList<J3dNiInterpolator> j3dNiInterpolators = new ArrayList<J3dNiInterpolator>();

	private boolean vertsResetOffBase = false;

	public J3dNiGeomMorpherController(NiGeomMorpherController controller, NiToJ3dData niToJ3dData)
	{
		super(controller, null);

		niMorphData = (NiMorphData) niToJ3dData.get(controller.data);

		if (niMorphData != null)
		{
			startTimeS = controller.startTime;
			stopTimeS = controller.stopTime;

			NiAVObject target = (NiAVObject) niToJ3dData.get(controller.target);
			nodeTarget = niToJ3dData.get(target);
			if (nodeTarget != null)
			{

				if (nodeTarget instanceof J3dNiTriBasedGeom)
				{
					// this bad boy is not connected to scene graph!
					J3dNiTriBasedGeom j3dNiTriBasedGeom = (J3dNiTriBasedGeom) nodeTarget;
					j3dNiTriBasedGeom.makeMorphable();
					currentGeoArray = j3dNiTriBasedGeom.getCurrentGeometryArray();
					baseGeoArray = j3dNiTriBasedGeom.getBaseGeometryArray();
				}
				else
				{
					System.out.println("J3dNiGeomMorpherController target not J3dNiTriBasedGeom but " + nodeTarget + " " + controller.nVer);
				}

				setupInterps(controller, niToJ3dData);
			}
		}
	}

	public float getLength()
	{
		return stopTimeS - startTimeS;
	}

	public boolean isVertsResetOffBase()
	{
		return vertsResetOffBase;
	}

	public void setVertsResetOffBase(boolean vertsResetOffBase)
	{
		this.vertsResetOffBase = vertsResetOffBase;
	}

	/**
	 *  note process must be called externally from now on,  
	 *   this just preps up teh morph name
	 *   Using an external interp handing updates to this
	 * @param action
	 * @return 
	 */
	public void setFrameName(String action)
	{

		//possibly I've got 2 animations busting on the same char??
		// might explain the shaking of techpods

		// I see it clearly todd land long distance actors and frame updates every second frame
		// look at horsey

		//possibly only when 2 process kick off togehrer?

		// looks like alll geommorph links are firing and causing havok!
		// the idle.kf has a contreoller for all 7 of them! how do I decide which happens?
		// base has no data for float interpolator but all 6 other have data and so will run
		// h2h block also has 7 of them!

		// horse idles show the same randomness, for both head and bridle note :0 appears to indicate something here?

		// now wolf attack?

		// I see geommorphs have a base set of data from which the morph can be taken, but helps not really
		// it does suggest only one morph at a time?

		//only set if different
		if (currentNifMorph == null || !currentNifMorph.frameName.equals(action))
		{
			for (int i = 0; i < niMorphData.numMorphs; i++)
			{
				if (niMorphData.nVer.LOAD_VER <= NifVer.VER_10_1_0_0)
				{
					if (("Frame_" + i).equals(action))
					{
						sequenceBehavior.setEnable(false);
						currentNifMorph = niMorphData.morphs[i];
						sequenceAlpha = null;
						currentJ3dNiInterpolator = null;
						return;
					}
				}
				else
				{
					if (niMorphData.morphs[i].frameName.equals(action))
					{
						sequenceBehavior.setEnable(false);
						currentNifMorph = niMorphData.morphs[i];
						sequenceAlpha = null;
						currentJ3dNiInterpolator = null;
						return;
					}
				}
			}
		}
	}

	@Override
	public void update(float value)
	{
		//the current morph needs to simply interp between start and morph vertex locations
		if (currentNifMorph != null && currentGeoArray != null)
		{
			final float interpValue = value;

			// use a local as the member can be swapped out anytime
			final NifMorph localCurrentNifMorph = currentNifMorph;

			currentGeoArray.updateData(new GeometryUpdater()
			{
				public void updateData(Geometry geometry)
				{
					// Note teh below only works if character attachment constantly resetting for us
					float[] coordRefFloat = currentGeoArray.getCoordRefFloat();
					float[] baseCoords = baseGeoArray.getCoordRefFloat();

					for (int i = 0; i < (coordRefFloat.length / 3); i++)
					{
						float x1 = baseCoords[(i * 3) + 0];
						float y1 = baseCoords[(i * 3) + 1];
						float z1 = baseCoords[(i * 3) + 2];

						// notice ConvertFromNif work here
						float x2 = localCurrentNifMorph.vectors[i].x * ESConfig.ES_TO_METERS_SCALE;
						float y2 = localCurrentNifMorph.vectors[i].z * ESConfig.ES_TO_METERS_SCALE;
						float z2 = -localCurrentNifMorph.vectors[i].y * ESConfig.ES_TO_METERS_SCALE;

						coordRefFloat[(i * 3) + 0] = x1 + (x2 * (interpValue));
						coordRefFloat[(i * 3) + 1] = y1 + (y2 * (interpValue));
						coordRefFloat[(i * 3) + 2] = z1 + (z2 * (interpValue));
					}

					vertsResetOffBase = true;
				}
			});
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////
	// in theory most of the below is just demoish stuff
	// possibly TES3 haed talk/blink needs it maybe

	private void setupInterps(NiGeomMorpherController controller, NiToJ3dData niToJ3dData)
	{
		// in theory most of the below is just demoish stuff, possibly TES3 haed talk/blink needs it maybe
		if (controller.nVer.LOAD_VER >= NifVer.VER_10_1_0_106)
		{
			if (controller.nVer.LOAD_VER <= NifVer.VER_20_0_0_5)
			{
				for (int i = 0; i < controller.interpolators.length; i++)
				{
					// build and attach intrerps for later use, note index order match morph
					NifRef nr = controller.interpolators[i];
					createInterp(nr, niToJ3dData);
				}
			}
			else if (controller.nVer.LOAD_VER >= NifVer.VER_20_1_0_3)
			{
				for (int i = 0; i < controller.interpolatorWeights.length; i++)
				{
					// build and attach interps for later use, note index order match morph
					NifMorphWeight nmw = controller.interpolatorWeights[i];

					NifRef nr = nmw.interpolator;
					if (nmw.weight != 0)
						System.out.println("non 0 nifmorphweight " + nmw.weight + " " + controller.nVer);

					createInterp(nr, niToJ3dData);
				}
			}
		}
		else
		{
			// 0 is base vert, 1 is often no morph, 2 is often all morphs in one track
			// Meshes\r\xKwama Forager.NIF is 10 morphs interesting
			for (int i = 0; i < niMorphData.numMorphs; i++)
			{
				createInterp(niMorphData.morphs[i], niToJ3dData);
			}
		}

		// this and all interp created above should never really be used, tehy are more for demo/debug kf file run this lot
		sequenceBehavior.setEnable(false);
		sequenceBehavior.setSchedulingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), Double.POSITIVE_INFINITY));
		addChild(sequenceBehavior);

	}

	/**
	 * For TES version the interpolators are just stuck in the NifMorph as keys
	 * @param nifMorph
	 * @param niToJ3dData
	 */
	private void createInterp(NifMorph nifMorph, NiToJ3dData niToJ3dData)
	{
		float lengthS = stopTimeS - startTimeS;
		J3dNiFloatInterpolator j3dNiInterpolator = new J3dNiFloatInterpolator(nifMorph, startTimeS, lengthS, this);

		j3dNiInterpolators.add(j3dNiInterpolator);
		addChild(j3dNiInterpolator);

	}

	private void createInterp(NifRef nr, NiToJ3dData niToJ3dData)
	{
		NiInterpolator niInterpolator = (NiInterpolator) niToJ3dData.get(nr);
		if (niInterpolator != null)
		{
			J3dNiInterpolator j3dNiInterpolator = J3dNiTimeController.createInterpForController(this, niInterpolator, niToJ3dData,
					startTimeS, stopTimeS);
			if (j3dNiInterpolator != null)
			{
				j3dNiInterpolators.add(j3dNiInterpolator);
				addChild(j3dNiInterpolator);
			}
		}
	}

	/**
	 * NOTE! it's unlikely to want to do this! kf files manage geomorphs properly
	 * 
	 * Fires off in a single loop of the fire in question
	 * @param action
	 */
	public void fireFrameName(String action)
	{
		//Notice geomorphs are controlled by kf files for above TES3
		// talking head appears to be skin/bones (including eyes/eyelids and teeth too)
		// the .tri adn .egm files possibly run these?

		//TODO: I've got the geomorphs totally wrong for TES3
		// firstly they have a morph 0 which is a baseline one that repeats the base verts
		// morph 1 tends to be a full length no changes morph
		// and morph 2 is the changes; morph 2 is the same length as the toatal kf file
		// and each piece of it corresponds to tke kf animation track
		//(e.g. attack has the mouth open and close at teh right time)

		// there is a fully geomorph animated create r\\xkwama forager.nif it has 10 morphs 
		//and is interesting it has a kf file that has one bone transform track	
		// each fo teh 10 morphs is the full 13 seonds long and each touchs all 370 verts
		// but teh key set seems to be lots of alpha 0's when the track is off
		// so all tracks run and all track modify verts, but only 1 is ever non-0 alpha at
		//a given time

		// but for complex humans (talking blinking heads) there is also
		// b_n_dark_elf_head_06 has a NiTextKeyExtraData 
		// that shows the pieces of the geomorph animation by time slice

		//System.out.println("TES3 only! fireFrameName " + action);
		for (int i = 0; i < niMorphData.numMorphs; i++)
		{
			if (niMorphData.nVer.LOAD_VER <= NifVer.VER_10_1_0_0)
			{

				if (("Frame_" + i).equals(action))
				{
					sequenceBehavior.setEnable(false);
					currentNifMorph = niMorphData.morphs[i];
					currentJ3dNiInterpolator = j3dNiInterpolators.get(i);
					sequenceAlpha = new SequenceAlpha(startTimeS, stopTimeS, false);

					sequenceBehavior.setEnable(true);// disables after loop if required
					return;
				}
			}
			else
			{
				if (niMorphData.morphs[i].frameName.equalsIgnoreCase(action))
				{
					sequenceBehavior.setEnable(false);
					currentNifMorph = niMorphData.morphs[i];
					currentJ3dNiInterpolator = j3dNiInterpolators.get(i);
					sequenceAlpha = new SequenceAlpha(startTimeS, stopTimeS, false);

					sequenceBehavior.setEnable(true);// disables after loop if required
					return;
				}
			}
		}
	}

	public String[] getAllMorphFrameNames()
	{
		String[] strings = new String[niMorphData.numMorphs];
		for (int i = 0; i < niMorphData.numMorphs; i++)
		{
			if (niMorphData.nVer.LOAD_VER <= NifVer.VER_10_1_0_0)
			{
				strings[i] = "Frame_" + i;
			}
			else
			{
				strings[i] = niMorphData.morphs[i].frameName;
			}
		}
		return strings;
	}

	/**
	 * Notice copy of J3dNiCOntrollerSequence
	 * @author philip
	 *
	 */
	public class SequenceBehavior extends VaryingLODBehaviour
	{
		public SequenceBehavior(Node node)
		{
			// NOTE!!!! these MUST be active, otherwise the headless locale that might be running physics doesn't continuously render
			super(node, new float[]
			{ 40, 120, 280 }, true, true);
		}

		@Override
		public void process()
		{

			// will be null when another interp is running this
			if (sequenceAlpha != null)
			{
				float alphaValue = sequenceAlpha.value();

				currentJ3dNiInterpolator.process(alphaValue);

				//turn off at the end
				if (sequenceAlpha.finished())
					setEnable(false);
			}
			else
			{
				setEnable(false);
			}
		}
	}
}
