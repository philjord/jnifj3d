package nif.j3d.animation;

import javax.media.j3d.Appearance;
import javax.media.j3d.Texture;
import javax.media.j3d.TextureUnitState;

import utils.source.TextureSource;
import nif.j3d.J3dNiAVObject;
import nif.j3d.J3dNiGeometry;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiSourceTexture;
import nif.niobject.controller.NiFlipController;

public class J3dNiFlipController extends J3dNiTimeController
{
	private Appearance app;

	private Texture[] textures;

	public J3dNiFlipController(NiFlipController controller, J3dNiAVObject nodeTarget, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(controller);
		app = ((J3dNiGeometry) nodeTarget).getShape().getAppearance();
		if (app.getTextureUnitCount() > 0)
		{
			textures = new Texture[controller.numSources];

			for (int t = 0; t < controller.numSources; t++)
			{
				String texName = ((NiSourceTexture) niToJ3dData.get(controller.sources[t])).fileName.string;
				textures[t] = loadTexture(texName, textureSource);
			}
			app.getTextureUnitState(0).setCapability(TextureUnitState.ALLOW_STATE_WRITE);
		}
		else
		{
			app.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
		}
	}

	public Texture loadTexture(String texName, TextureSource textureSource)
	{
		if (textureSource != null && texName != null && texName.length() > 0)
		{
			return textureSource.getTexture(texName);
		}

		return null;
	}

	@Override
	public void update(float value)
	{
		/*int idx = (int) Math.floor(value * textures.length);
		idx = (idx == textures.length) ? idx - 1 : idx;
		if (idx >= 0 && idx < textures.length)
		{
			app.setTexture(textures[idx]);
		}
		else
		{
			System.out.println("TextureSwitchInterpolator bad idx");
			System.out.println("idx " + idx);
			System.out.println("value " + value);
			System.out.println("textures.length " + textures.length);
		}*/
		//I think the floats in the interp are in the correct range, the above was possibly me being keen 
		int idx = (int) Math.floor(value);
		if (app.getTextureUnitCount() > 0)
		{			
			app.getTextureUnitState(0).setTexture(textures[idx]);
		}
		else
		{
			app.setTexture(textures[idx]);
		}
	}

}
