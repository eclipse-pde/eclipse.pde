package org.eclipse.pde.internal.core;

import java.io.*;
import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.model.*;
import org.eclipse.core.runtime.model.PluginRegistryModel;

/**
 *
 */
public class TargetPlatformRegistryLoader {
	private static final String CACHE_FILE = ".registry";
	private static final String KEY_SCANNING_PROBLEMS =
		"ExternalModelManager.scanningProblems";
	private PluginRegistryModel registryModel;
	private static boolean DEBUG = Platform.getDebugOption("org.eclipse.pde.core/cache").equals("true");;
	private long code;
	
	public MultiStatus load(URL[] urls, boolean resolve, boolean useCache) {
		MultiStatus errors =
			new MultiStatus(
				PDECore.getPluginId(),
				1,
				PDECore.getResourceString(KEY_SCANNING_PROBLEMS),
				null);
		Factory factory = new Factory(errors);
		
		long start = System.currentTimeMillis();

		if (resolve && useCache) {
			code = computePluginsTimestamp(urls);
			loadFromCache(urls, factory, errors);
			if (registryModel != null) {
				return errors;
			}
		}

		registryModel = Platform.parsePlugins(urls, factory);
		IStatus resolveStatus = null;
		if (resolve) {
			resolveStatus = registryModel.resolve(true, false);
			if (resolveStatus != null)
				errors.merge(resolveStatus);
			if (useCache) {
				saveCache(errors);
			}
		}
		if (DEBUG) {
			System.out.println("Total time elapsed: " + (System.currentTimeMillis() - start) + "ms");
		}
		return errors;
	}
	
	public PluginRegistryModel getRegistry() {
		return registryModel;
	}
	
	private void loadFromCache(URL [] urls, Factory factory, MultiStatus errors) {
		File cacheFile = getCacheFile();
		if (!cacheFile.exists()) return;
		try {
			DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(cacheFile)));
			try {
				long start = System.currentTimeMillis();
				PDERegistryCacheReader cacheReader = new PDERegistryCacheReader(factory, code);
				registryModel = cacheReader.readPluginRegistry(input, urls, DEBUG);
				if (DEBUG)
					System.out.println("Read registry cache: " + (System.currentTimeMillis() - start) + "ms");
			} finally {
				input.close();
			}
		} catch (IOException ioe) {
			IStatus status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Platform.PLUGIN_ERROR, "Unable to read plug-in cache", ioe);
			errors.merge(status);
		}
		if (registryModel==null) {
			// Cache existed but we could not load from it
			// Remove it so that a fresh one can be written.
			cacheFile.delete();
		}
	}
	
	private void saveCache(MultiStatus errors) {
		try {
			File file = getCacheFile();
			if (file.exists()) {
				// The registry cache file exists.  Assume it is fine and
				// we don't need to re-write it.
				return;
			}
			DataOutputStream output = null;
			try {
				output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			} catch (IOException ioe) {
				String message = "Unable to create cache.";
				IStatus status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.OK, message, ioe);
				errors.merge(status);
				return;
			}
			try {
				long start = System.currentTimeMillis();
				PDERegistryCacheWriter cacheWriter = new PDERegistryCacheWriter(code);
				cacheWriter.writePluginRegistry(registryModel, output);
				if (DEBUG)
					System.out.println("Wrote registry: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			} finally {
				output.close();
			}
		} catch (IOException e) {
			String message = "Unable to write registry."; //$NON-NLS-1$
			IStatus status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Platform.PLUGIN_ERROR, message, e);
			errors.merge(status);
			if (DEBUG)
				System.out.println(status.getMessage());
		}
		
	}

	private File getCacheFile() {
		IPath location = PDECore.getDefault().getStateLocation();
		IPath filePath = location.append(CACHE_FILE);
		return new File(filePath.toOSString());
	}

	private long computePluginsTimestamp(URL[] urls) {
		long result = 0;
		for (int i = 0; i < urls.length; i++) {
			File directory = new File(urls[i].getFile().toString().replace('/',File.separatorChar));
			if (directory.exists() && directory.isDirectory()) {
				File[] files = directory.listFiles();
				for (int j = 0; j < files.length; j++) {
					File manifest = new File(files[j].getAbsolutePath() + File.separatorChar + "plugin.xml");
					if (!manifest.exists())
						manifest = new File(files[j].getAbsolutePath() + File.separatorChar + "fragment.xml");
					if (manifest.exists())
						result ^= manifest.getAbsolutePath().hashCode() ^ manifest.lastModified() ^ manifest.length();
				}
			}
		}
		return result;
	}
	
}
