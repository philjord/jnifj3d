package nif.j3d.animation;

import javax.media.j3d.Alpha;
import javax.media.j3d.Bounds;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.vecmath.Point3f;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.j3dinterp.J3dNiBSplineCompFloatInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiBoolInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiFloatInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.j3d.animation.j3dinterp.J3dNiPoint3Interpolator;
import nif.j3d.animation.j3dinterp.interp.InterpolatorListener;
import nif.j3d.particles.J3dNiParticleSystem;
import nif.niobject.bhk.bhkBlendController;
import nif.niobject.bs.BSEffectShaderPropertyColorController;
import nif.niobject.bs.BSEffectShaderPropertyFloatController;
import nif.niobject.bs.BSLightingShaderPropertyColorController;
import nif.niobject.bs.BSLightingShaderPropertyFloatController;
import nif.niobject.bs.BSMaterialEmittanceMultController;
import nif.niobject.bs.BSProceduralLightningController;
import nif.niobject.bs.BSRefractionFirePeriodController;
import nif.niobject.bs.BSRefractionStrengthController;
import nif.niobject.controller.NiAlphaController;
import nif.niobject.controller.NiBSBoneLODController;
import nif.niobject.controller.NiExtraDataController;
import nif.niobject.controller.NiFlipController;
import nif.niobject.controller.NiGeomMorpherController;
import nif.niobject.controller.NiLightColorController;
import nif.niobject.controller.NiLightDimmerController;
import nif.niobject.controller.NiLightRadiusController;
import nif.niobject.controller.NiMaterialColorController;
import nif.niobject.controller.NiMultiTargetTransformController;
import nif.niobject.controller.NiTextureTransformController;
import nif.niobject.controller.NiTimeController;
import nif.niobject.controller.NiVisController;
import nif.niobject.interpolator.NiBSplineCompFloatInterpolator;
import nif.niobject.interpolator.NiBlendBoolInterpolator;
import nif.niobject.interpolator.NiBlendFloatInterpolator;
import nif.niobject.interpolator.NiBlendPoint3Interpolator;
import nif.niobject.interpolator.NiBoolInterpolator;
import nif.niobject.interpolator.NiBoolTimelineInterpolator;
import nif.niobject.interpolator.NiFloatInterpolator;
import nif.niobject.interpolator.NiInterpolator;
import nif.niobject.interpolator.NiPoint3Interpolator;
import nif.niobject.particle.NiPSysModifierCtlr;
import utils.source.TextureSource;

public abstract class J3dNiTimeController extends Group implements InterpolatorListener
{
	private J3dNiTimeController j3dNiTimeController;

	protected NiTimeController niTimeController;

	protected Node nodeTarget;

	/**
	 * Just requires sub classes to hand it up for now, no reason
	 * if nodeTarget == null getBounds() MUST be overrriden or nodeTarget must be set
	 * @param niTimeController
	 * @param nodeTarget 
	 */
	public J3dNiTimeController(NiTimeController niTimeController, Node nodeTarget)
	{
		this.niTimeController = niTimeController;
		this.nodeTarget = nodeTarget;
	}

	public J3dNiTimeController getJ3dNiTimeController()
	{
		return j3dNiTimeController;
	}

	public void setJ3dNiTimeController(J3dNiTimeController j3dNiTimeController)
	{
		// TODO: there are cases where this should be fired I think
		this.j3dNiTimeController = j3dNiTimeController;
		addChild(j3dNiTimeController);
	}

	@Override
	public Bounds getBounds()
	{ // Notice this is not called setBoundsAutoCompute(false);
		// it cause NPE because we need to give it a bounds with set bounds, only instead we are just 
		// the exteranl getbounds, not used by pipeline at all
		return nodeTarget.getBounds();
	}

	@Override
	public void update(float value)
	{
		//default to do nothing
	}

	@Override
	public void update(boolean value)
	{
		// default to do nothing
	}

	@Override
	public void update(Point3f value)
	{
		// default to do nothing
	}

	/**
	 * @param startTimeS
	 * @param stopTimeS
	 * @param loopCount means loop forever
	 * @return
	 */
	public static Alpha createLoopingAlpha(float startTimeS, float stopTimeS)
	{
		long startTimeMS = (long) (startTimeS * 1000f);
		long stopTimeMS = (long) (stopTimeS * 1000f);
		long lengthMS = stopTimeMS - startTimeMS;

		// note 0 trigger, see SequenceAlpha for better system
		return new Alpha(-1, 0, startTimeMS, lengthMS, 0, 0);
	}

	/**
	 * NOTE the time controller is NOT used for the alpha creation! It is merely a listener to the 3 types of call back
	 * @param parent
	 * @param callbackListener
	 * @param niInterpolator
	 * @param startTimeS
	 * @param stopTimeS
	 */
	public static J3dNiInterpolator createInterpForController(InterpolatorListener callbackListener, NiInterpolator niInterpolator, NiToJ3dData niToJ3dData, float startTimeS, float stopTimeS)
	{
		if (niInterpolator == null)
		{
			return null;
		}

		float lengthS = stopTimeS - startTimeS;
		J3dNiInterpolator j3dNiInterpolator = null;

		if (niToJ3dData.get(niInterpolator) != null)
		{
			return niToJ3dData.get(niInterpolator);
		}

		if (niInterpolator instanceof NiFloatInterpolator)
		{
			j3dNiInterpolator = new J3dNiFloatInterpolator((NiFloatInterpolator) niInterpolator, niToJ3dData, startTimeS, lengthS, callbackListener);
		}
		else if (niInterpolator instanceof NiBlendFloatInterpolator)
		{
			// don't create indicates that a controller link will create a real one
		}
		else if (niInterpolator instanceof NiBoolInterpolator || niInterpolator instanceof NiBoolTimelineInterpolator)
		{
			j3dNiInterpolator = new J3dNiBoolInterpolator((NiBoolInterpolator) niInterpolator, niToJ3dData, startTimeS, lengthS, callbackListener);
		}
		else if (niInterpolator instanceof NiBlendBoolInterpolator)
		{
			// don't create indicates that a controller link will create a real one
		}
		else if (niInterpolator instanceof NiPoint3Interpolator)
		{
			j3dNiInterpolator = new J3dNiPoint3Interpolator((NiPoint3Interpolator) niInterpolator, niToJ3dData, startTimeS, lengthS, callbackListener);
		}
		else if (niInterpolator instanceof NiBlendPoint3Interpolator)
		{
			// don't create indicates that a controller link will create a real one
		}
		else if (niInterpolator instanceof NiBSplineCompFloatInterpolator)
		{
			j3dNiInterpolator = new J3dNiBSplineCompFloatInterpolator((NiBSplineCompFloatInterpolator) niInterpolator, niToJ3dData, callbackListener);
		}
		else
		{
			System.out.println("Unhandled niInterpolator for J3dNiTimeController " + callbackListener + " of " + niInterpolator);
		}

		if (j3dNiInterpolator != null)
		{
			niToJ3dData.put(niInterpolator, j3dNiInterpolator);
		}
		return j3dNiInterpolator;
	}

	public static J3dNiTimeController createJ3dNiTimeController(NiTimeController controller, NiToJ3dData niToJ3dData, J3dNiAVObject nodeTarget, TextureSource textureSource)
	{
		J3dNiTimeController j3dNiTimeController = null;

		if (niToJ3dData.get(controller) != null)
		{
			return niToJ3dData.get(controller);
		}

		//dont hand in
		if (controller instanceof NiGeomMorpherController || controller instanceof NiMultiTargetTransformController)
		{
			//note NiTransformController won't get past NiTimeController anyway
			new Throwable("Can't hand in " + controller + " to createJ3dNiTimeController").printStackTrace();
			return null;
		}

		if (controller instanceof NiMaterialColorController)
		{
			j3dNiTimeController = new J3dNiMaterialColorController((NiMaterialColorController) controller, nodeTarget);
		}
		else if (controller instanceof NiLightColorController)
		{
			j3dNiTimeController = new J3dNiLightColorController((NiLightColorController) controller, nodeTarget);
		}
		else if (controller instanceof NiLightDimmerController)
		{
			j3dNiTimeController = new J3dNiLightDimmerController((NiLightDimmerController) controller, nodeTarget);
		}
		else if (controller instanceof NiExtraDataController)
		{
			j3dNiTimeController = new J3dNiExtraDataController((NiExtraDataController) controller, nodeTarget);
		}
		else if (controller instanceof NiTextureTransformController)
		{
			j3dNiTimeController = new J3dNiTextureTransformController((NiTextureTransformController) controller, nodeTarget);
		}
		else if (controller instanceof NiAlphaController)
		{
			j3dNiTimeController = new J3dNiAlphaController((NiAlphaController) controller, nodeTarget);
		}
		else if (controller instanceof NiFlipController)
		{
			j3dNiTimeController = new J3dNiFlipController((NiFlipController) controller, nodeTarget, niToJ3dData, textureSource);
		}
		else if (controller instanceof NiVisController)
		{
			j3dNiTimeController = new J3dNiVisController((NiVisController) controller, nodeTarget);
		}
		else if (controller instanceof NiLightRadiusController)
		{
			j3dNiTimeController = new J3dNiLightRadiusController((NiLightRadiusController) controller, nodeTarget);
		}
		else if (controller instanceof BSRefractionStrengthController)
		{
			//TODO:BSRefractionStrengthController
		}
		else if (controller instanceof BSMaterialEmittanceMultController)
		{
			//TODO:BSMaterialEmittanceMultController
		}
		else if (controller instanceof NiBSBoneLODController)
		{
			// TODO:NiBSBoneLODController
		}
		else if (controller instanceof bhkBlendController)
		{
			//TODO: bhkBlendController
		}
		else if (controller instanceof BSLightingShaderPropertyColorController)
		{
			//TODO: BSLightingShaderPropertyColorController
		}
		else if (controller instanceof BSLightingShaderPropertyFloatController)
		{
			//TODO:  BSLightingShaderPropertyFloatController
		}
		else if (controller instanceof BSEffectShaderPropertyColorController)
		{
			//TODO:  BSEffectShaderPropertyColorController
		}
		else if (controller instanceof BSEffectShaderPropertyFloatController)
		{
			//TODO:  BSEffectShaderPropertyFloatController
		}
		else if (controller instanceof BSProceduralLightningController)
		{
			//TODO:  BSProceduralLightningController
		}
		else if (controller instanceof BSRefractionFirePeriodController)
		{
			//TODO:  BSRefractionFirePeriodController
		}
		else if (controller instanceof NiPSysModifierCtlr)
		{
			//TODO: a controller sequence can have a  NiPSysModifierCtlr in it, which will point at a 
			// target Particle system, so presumably the particle system is kicked off
			// when teh animation fires, which sounds fair enough , does it work
			//E:\game media\Oblivion\meshes\effects\se09bodypartsdrop.nif is an example
			NiPSysModifierCtlr niPSysModifierCtlr = (NiPSysModifierCtlr) controller;
			J3dNiParticleSystem j3dNiParticleSystem = (J3dNiParticleSystem) nodeTarget;
			j3dNiTimeController = j3dNiParticleSystem.getJ3dNiPSysModifierCtlr(niPSysModifierCtlr, niToJ3dData);
		}
		else
		{
			System.out.println("J3dNiTimeController.createJ3dNiTimeController - unhandled NiTimeController " + controller);
		}

		if (j3dNiTimeController != null)
		{
			niToJ3dData.put(controller, j3dNiTimeController);
		}

		return j3dNiTimeController;
	}

}
