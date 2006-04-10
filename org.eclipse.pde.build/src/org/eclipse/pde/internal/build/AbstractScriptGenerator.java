/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.site.*;
import org.eclipse.update.core.*;

/**
 * Generic super-class for all script generator classes. 
 * It contains basic informations like the script, the configurations, and a location 
 */
public abstract class AbstractScriptGenerator implements IXMLConstants, IPDEBuildConstants, IBuildPropertiesConstants {
	protected static boolean embeddedSource = false;
	protected static boolean forceUpdateJarFormat = false;
	private static List configInfos;
	protected static String workingDirectory;
	protected static boolean buildingOSGi = true;
	protected AntScript script;

	private static PDEUIStateWrapper pdeUIState;

	/** Location of the plug-ins and fragments. */
	protected String[] sitePaths;
	protected String[] pluginPath;
	protected BuildTimeSiteFactory siteFactory;

	protected boolean reportResolutionErrors;

	static {
		// By default, a generic configuration is set
		configInfos = new ArrayList(1);
		configInfos.add(Config.genericConfig());
	}

	public static List getConfigInfos() {
		return configInfos;
	}

	/**
	 * Starting point for script generation. See subclass implementations for
	 * individual comments.
	 * 
	 * @throws CoreException
	 */
	public abstract void generate() throws CoreException;

	public static void setConfigInfo(String spec) throws CoreException {
		configInfos.clear();
		String[] configs = Utils.getArrayFromStringWithBlank(spec, "&"); //$NON-NLS-1$
		configInfos = new ArrayList(configs.length);
		String[] os = new String[configs.length];
		String[] ws = new String[configs.length];
		String[] archs = new String[configs.length];
		for (int i = 0; i < configs.length; i++) {
			String[] configElements = Utils.getArrayFromStringWithBlank(configs[i], ","); //$NON-NLS-1$
			if (configElements.length != 3) {
				IStatus error = new Status(IStatus.ERROR, IPDEBuildConstants.PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_CONFIG_FORMAT, NLS.bind(Messages.error_configWrongFormat, configs[i]), null);
				throw new CoreException(error);
			}
			Config aConfig = new Config(configs[i]);
			if (aConfig.equals(Config.genericConfig()))
				configInfos.add(Config.genericConfig());
			else
				configInfos.add(aConfig);

			// create a list of all ws, os and arch to feed the SiteManager
			os[i] = aConfig.getOs();
			ws[i] = aConfig.getWs();
			archs[i] = aConfig.getArch();
		}
		SiteManager.setOS(Utils.getStringFromArray(os, ",")); //$NON-NLS-1$
		SiteManager.setWS(Utils.getStringFromArray(ws, ",")); //$NON-NLS-1$
		SiteManager.setOSArch(Utils.getStringFromArray(archs, ",")); //$NON-NLS-1$
	}

	public void setWorkingDirectory(String location) {
		workingDirectory = location;
	}

	/**
	 * Return the file system location for the given plug-in model object.
	 * 
	 * @param model the plug-in
	 * @return String
	 */
	public String getLocation(BundleDescription model) {
		return model.getLocation();
	}

	static public class MissingProperties extends Properties {
		private static final long serialVersionUID = 3546924667060303927L;
		private static MissingProperties singleton;

		private MissingProperties() {
			//nothing to do;
		}

		public synchronized Object setProperty(String key, String value) {
			throw new UnsupportedOperationException();
		}

		public synchronized Object put(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		public static MissingProperties getInstance() {
			if (singleton == null)
				singleton = new MissingProperties();
			return singleton;
		}
	}

	public static Properties readProperties(String location, String fileName, int errorLevel) throws CoreException {
		Properties result = new Properties();
		File file = new File(location, fileName);
		try {
			InputStream input = new BufferedInputStream(new FileInputStream(file));
			try {
				result.load(input);
			} finally {
				input.close();
			}
		} catch (FileNotFoundException e) {
			if (errorLevel != IStatus.INFO && errorLevel != IStatus.OK) {
				String message = NLS.bind(Messages.exception_missingFile, file);
				BundleHelper.getDefault().getLog().log(new Status(errorLevel, PI_PDEBUILD, EXCEPTION_READING_FILE, message, null));
			}
			result = MissingProperties.getInstance();
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_readingFile, file);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e));
		}
		return result;
	}

	public void openScript(String scriptLocation, String scriptName) throws CoreException {
		if (script != null)
			return;

		try {
			OutputStream scriptStream = new BufferedOutputStream(new FileOutputStream(scriptLocation + '/' + scriptName)); 
			try {
				script = new AntScript(scriptStream);
			} catch (IOException e) {
				try {
					scriptStream.close();
					String message = NLS.bind(Messages.exception_writingFile, scriptLocation + '/' + scriptName);
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				} catch (IOException e1) {
					// Ignored		
				}
			}
		} catch (FileNotFoundException e) {
			String message = NLS.bind(Messages.exception_writingFile, scriptLocation + '/' + scriptName);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
	}

	public void closeScript() {
		script.close();
	}

	public void setBuildingOSGi(boolean b) {
		buildingOSGi = b;
	}

	public static boolean isBuildingOSGi() {
		return buildingOSGi;
	}

	public static String getWorkingDirectory() {
		return workingDirectory;
	}

	public static String getDefaultOutputFormat() {
		return "zip"; //$NON-NLS-1$
	}

	public static boolean getDefaultEmbeddedSource() {
		return false;
	}

	public static void setEmbeddedSource(boolean embed) {
		embeddedSource = embed;
	}

	public static boolean getForceUpdateJarFormat() {
		return false;
	}

	public static void setForceUpdateJar(boolean force) {
		forceUpdateJarFormat = force;
	}

	public static String getDefaultConfigInfos() {
		return "*, *, *"; //$NON-NLS-1$
	}

	public static boolean getDefaultBuildingOSGi() {
		return true;
	}

	/**
	 * Return a build time site referencing things to be built.   
	 * @param refresh : indicate if a refresh must be performed. Although this flag is set to true, a new site is not rebuild if the urls of the site did not changed 
	 * @return BuildTimeSite
	 * @throws CoreException
	 */
	public BuildTimeSite getSite(boolean refresh) throws CoreException {
		if (siteFactory != null && refresh == false)
			return (BuildTimeSite) siteFactory.createSite();

		if (siteFactory == null || refresh == true) {
			siteFactory = new BuildTimeSiteFactory();
			siteFactory.setReportResolutionErrors(reportResolutionErrors);
		}

		siteFactory.setSitePaths(getPaths());
		siteFactory.setInitialState(pdeUIState);
		return (BuildTimeSite) siteFactory.createSite();
	}

	/**
	 * Method getPaths.  These are the paths used for the BuildTimeSite
	 * @return URL[]
	 */
	private String[] getPaths() {
		if (sitePaths == null) {
			if (pluginPath != null) {
				sitePaths = new String[pluginPath.length + 1];
				System.arraycopy(pluginPath, 0, sitePaths, 0, pluginPath.length);
				sitePaths[sitePaths.length - 1] = workingDirectory;
			} else {
				sitePaths = new String[] {workingDirectory};
			}
		}

		return sitePaths;
	}

	public void setBuildSiteFactory(BuildTimeSiteFactory siteFactory) {
		this.siteFactory = siteFactory;
	}

	/**
	 * Return the path of the plugins		//TODO Do we need to add support for features, or do we simply consider one list of URL? It is just a matter of style/
	 * @return URL[]
	 */
	public String[] getPluginPath() {
		return pluginPath;
	}

	/**
	 * Sets the pluginPath.
	 * 
	 * @param path
	 */
	public void setPluginPath(String[] path) {
		pluginPath = path;
	}

	public void setPDEState(State state) {
		ensurePDEUIStateNotNull();
		pdeUIState.setState(state);
	}

	public void setStateExtraData(HashMap classpath, Map patchData) {
		ensurePDEUIStateNotNull();
		pdeUIState.setExtraData(classpath, patchData);
	}

	public void setNextId(long nextId) {
		ensurePDEUIStateNotNull();
		pdeUIState.setNextId(nextId);
	}

	protected void flushState() {
		pdeUIState = null;
	}

	private void ensurePDEUIStateNotNull() {
		if (pdeUIState == null)
			pdeUIState = new PDEUIStateWrapper();
	}
	
	protected boolean havePDEUIState() {
		return pdeUIState != null;
	}
	
	//Find a file in a bundle or a feature.
	//location is assumed to be structured like : /<featureId | pluginId>/path.to.the.file
	protected String findFile(String location, boolean makeRelative) {
		if (location == null)
			return null;
		PDEState state;
		try {
			state = getSite(false).getRegistry();
		} catch (CoreException e) {
			return null;
		}
		Path path = new Path(location);
		String id = path.segment(0);
		BundleDescription[] matches = state.getState().getBundles(id);
		if (matches != null && matches.length != 0) {
			BundleDescription bundle = matches[0];
			if (bundle != null) {
				String result = checkFile(new Path(bundle.getLocation()), path, makeRelative);
				if (result != null)
					return result;
			}
		}
		// Couldn't find the file in any of the plugins, try in a feature.
		IFeature feature = null;
		try {
			feature = getSite(false).findFeature(id, null, false);
		} catch (CoreException e) {
			//Ignore
		}
		if (feature == null) 
			return null;
		ISiteFeatureReference ref = feature.getSite().getFeatureReference(feature);
		IPath featureBase = new Path(ref.getURL().getFile()).removeLastSegments(1);
		return checkFile(featureBase, path, makeRelative);
	}

	private String checkFile(IPath base, Path target, boolean makeRelative) {
		IPath path = base.append(target.removeFirstSegments(1));
		String result = path.toOSString();
		if (!new File(result).exists())
			return null;
		if (makeRelative)
			return Utils.makeRelative(path, new Path(workingDirectory)).toOSString();
		return result;
	}
}
