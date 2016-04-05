package utils.source.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utils.ESConfig;

public class FileMediaRoots
{
	private static String fixedRoot = null;

	public static void setFixedRoot(String root)
	{
		fixedRoot = root;
	}

	private static ArrayList<String> defaultMediaRoots = new ArrayList<String>();

	private static List<String> MEDIA_ROOTS = defaultMediaRoots;

	private static boolean defaultUsageWarningGiven = false;

	/** crazy defaults not for real use by anyone but the laziest devs
	 * 
	 */
	static
	{
		defaultMediaRoots.add("f:/game_media/morrowind/");
		defaultMediaRoots.add("f:/game_media/oblivion/");
		defaultMediaRoots.add("f:/game_media/fallout3/");
		defaultMediaRoots.add("f:/game_media/falloutnv/");
		defaultMediaRoots.add("f:/game_media/skyrim/");
		defaultMediaRoots.add("f:/game_media/fallout4/");
		defaultMediaRoots.add("f:/game_media/x3tc/");
		defaultMediaRoots.add("f:/game_media/black prophecy/");
	}

	public static void setMediaRoots(String[] mediaRoots)
	{
		MEDIA_ROOTS = new ArrayList<String>();
		for (String path : mediaRoots)
		{
			MEDIA_ROOTS.add(path.replace("\\", "/").toLowerCase());
		}

	}

	public static void setMediaRoots(List<String> mediaRoots)
	{
		MEDIA_ROOTS = new ArrayList<String>();
		for (String path : mediaRoots)
		{
			MEDIA_ROOTS.add(path.replace("\\", "/").toLowerCase());
		}

	}

	public static List<String> getMediaRoots()
	{
		return MEDIA_ROOTS;
	}

	/**
	 * Helper return media root in element 0 and media name in element 1 
	 * @param modelFileName
	 * @return
	 */
	public static String[] splitOffMediaRoot(String mediaFileName)
	{
		//for Android/linux
		mediaFileName = mediaFileName.replace("\\", "/");
		
		String[] ret = new String[2];		
		if (fixedRoot != null)
		{

			File f = new File(mediaFileName);
			if (f.exists())
			{
				//System.out.println("No root found to split " + mediaFileName);
				ret[0] = "";
				ret[1] = mediaFileName;
				return ret;
			}
			else
			{
				// try with the word mesh
				f = new File(fixedRoot + File.separator + ESConfig.TES_MESH_PATH + mediaFileName);

				if (f.exists())
				{
					ret[0] = fixedRoot;
					ret[1] = File.separator + ESConfig.TES_MESH_PATH + mediaFileName;
					return ret;

				}
				else
				{
					// add texture
					f = new File(fixedRoot + File.separator + ESConfig.TES_TEXTURE_PATH + mediaFileName);

					if (f.exists())
					{
						ret[0] = fixedRoot;
						ret[1] = File.separator + ESConfig.TES_TEXTURE_PATH + mediaFileName;
						return ret;

					}
					else
					{
						// add sound
						f = new File(fixedRoot + File.separator + ESConfig.TES_SOUND_PATH + mediaFileName);

						if (f.exists())
						{
							ret[0] = fixedRoot;
							ret[1] = File.separator + ESConfig.TES_SOUND_PATH + mediaFileName;
							return ret;
						}
						else
						{
							f = new File(fixedRoot + File.separator + mediaFileName);

							if (f.exists())
							{
								ret[0] = fixedRoot;
								ret[1] = File.separator + mediaFileName;
								return ret;
							}
							
						}

					}
				}
			}

		}
		else
		{
			if (MEDIA_ROOTS == defaultMediaRoots && !defaultUsageWarningGiven)
			{
				System.err.println("Warning! Warning! Usage of default media roots not recommended!");
				defaultUsageWarningGiven = true;
			}

			// test to see if it is a complete file name "as is"
			File f = new File(mediaFileName);
			if (f.exists())
			{
				mediaFileName = mediaFileName.replace("\\", "/").toLowerCase();
				for (String path : MEDIA_ROOTS)
				{
					if (mediaFileName.startsWith(path))
					{
						ret[0] = path;
						ret[1] = mediaFileName.substring(path.length());
						return ret;
					}
				}

				//System.out.println("No root found to split " + mediaFileName);
				ret[0] = "";
				ret[1] = mediaFileName;
				return ret;
			}
			else
			{
				for (String path : MEDIA_ROOTS)
				{

					// try with the word mesh
					f = new File(path + File.separator + ESConfig.TES_MESH_PATH + mediaFileName);

					if (f.exists())
					{
						ret[0] = path;
						ret[1] = File.separator + ESConfig.TES_MESH_PATH + mediaFileName;
						return ret;

					}
					else
					{
						// add texture
						f = new File(path + File.separator + ESConfig.TES_TEXTURE_PATH + mediaFileName);

						if (f.exists())
						{
							ret[0] = path;
							ret[1] = File.separator + ESConfig.TES_TEXTURE_PATH + mediaFileName;
							return ret;

						}
						else
						{
							// add sound
							f = new File(path + File.separator + ESConfig.TES_SOUND_PATH + mediaFileName);

							if (f.exists())
							{
								ret[0] = path;
								ret[1] = File.separator + ESConfig.TES_SOUND_PATH + mediaFileName;
								return ret;
							}
							else
							{
								f = new File(path + File.separator + mediaFileName);

								if (f.exists())
								{
									ret[0] = path;
									ret[1] = File.separator + mediaFileName;
									return ret;
								}
								//note no return roll around to check next path	
							}

						}
					}

				}
			}
		}
		//System.out.println("No root found to split " + mediaFileName);
		ret[0] = "";
		ret[1] = mediaFileName;
		return ret;
	}


}
