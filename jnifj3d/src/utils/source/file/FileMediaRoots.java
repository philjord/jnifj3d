package utils.source.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utils.ESConfig;

public class FileMediaRoots
{
	private static ArrayList<String> defaultMediaRoots = new ArrayList<String>();

	private static List<String> MEDIA_ROOTS = defaultMediaRoots;

	private static boolean defaultUsageWarningGiven = false;

	/** crazy defaults not for real use by anyone but the laziest devs
	 * 
	 */
	static
	{
		defaultMediaRoots.add("f:/game media/morrowind/");
		defaultMediaRoots.add("f:/game media/oblivion/");
		defaultMediaRoots.add("f:/game media/fallout/");
		defaultMediaRoots.add("f:/game media/falloutnv/");
		defaultMediaRoots.add("f:/game media/skyrim/");
		defaultMediaRoots.add("f:/game media/x3tc/");
		defaultMediaRoots.add("f:/game media/black prophecy/");
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
	 * Helper return media root in element 0 and media name in element 1, note this differs VERY much from
	 * the method above which attaches teh meshes to teh root, this method attaches that to the media name
	 * @param modelFileName
	 * @return
	 */
	public static String[] splitOffMediaRoot(String mediaFileName)
	{
		String[] ret = new String[2];
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

			System.out.println("No root found to split " + mediaFileName);
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

			System.out.println("No root found to split " + mediaFileName);
			ret[0] = "";
			ret[1] = mediaFileName;
			return ret;
		}
	}
	/**
	 * To force a particular root for say characters etc use this to find the root
	 * @param rootContains
	 * @return
	 */
	/*public static String getMediaRootOfType(String rootContains)
	{
		for (String path : MEDIA_ROOTS)
		{
			if (path.contains(rootContains))
			{
				return path;
			}
		}

		System.out.println("getMediaRootOfType failed for " + rootContains);
		return "";
	}*/

	/**
	 * This will return the media root in the file name or the media root that makes the
	 * file point to an existant one. Note if you give an absolute path to this method
	 * you will get a media root back out, which you should not then prefix to the path
	 * @param modelFileName
	 * @return
	 */
	/*public static String findMediaRoot(String mediaFileName)
	{
		//System.out.println("findMediaRoot " + mediaFileName);
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
					return path;
				}
			}

			return "";
		}
		else
		{
			for (String path : MEDIA_ROOTS)
			{
				f = new File(path + File.separator + mediaFileName);

				if (f.exists())
				{
					return path + File.separator;
				}
				else
				{
					// try with the word mesh
					f = new File(path + File.separator + ESConfig.TES_MESH_PATH + mediaFileName);

					if (f.exists())
					{
						return path + File.separator + ESConfig.TES_MESH_PATH;
					}
					else
					{
						// add texture
						f = new File(path + File.separator + ESConfig.TES_TEXTURE_PATH + mediaFileName);

						if (f.exists())
						{
							return path + File.separator + ESConfig.TES_TEXTURE_PATH;
						}
						else
						{
							// add sound
							f = new File(path + File.separator + ESConfig.TES_SOUND_PATH + mediaFileName);

							if (f.exists())
							{
								return path + File.separator + ESConfig.TES_SOUND_PATH;
							}

						}
					}
				}

			}

			return "";
		}
	}*/

	/*public static String stripExtras(String inMediaRoot)
	{
		//System.out.println("stripExtras " + inMediaRoot);
		if (inMediaRoot.endsWith(ESConfig.TES_MESH_PATH))
		{
			return inMediaRoot.substring(0, inMediaRoot.length() - (ESConfig.TES_MESH_PATH).length());
		}
		else if (inMediaRoot.endsWith(ESConfig.TES_SOUND_PATH))
		{
			return inMediaRoot.substring(0, inMediaRoot.length() - (ESConfig.TES_SOUND_PATH).length());
		}
		else if (inMediaRoot.endsWith(ESConfig.TES_TEXTURE_PATH))
		{
			return inMediaRoot.substring(0, inMediaRoot.length() - (ESConfig.TES_TEXTURE_PATH).length());
		}
		else
		{
			return inMediaRoot;
		}
	}*/

	/*public static String findFullPath(String modelFileName)
	{
		//System.out.println("findFullPath " + modelFileName);
		File f = new File(modelFileName);
		if (f.exists())
		{
			return modelFileName;
		}
		else
		{
			return findMediaRoot(modelFileName) + modelFileName;
		}
	}*/

}
