/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.engine.SimpleProfileRegistry;
import org.eclipse.equinox.internal.p2.publisher.eclipse.ProductFile;
import org.eclipse.equinox.p2.core.IAgentLocation;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.builder.BuildDirector;
import org.eclipse.pde.internal.build.site.*;
import org.eclipse.pde.internal.build.site.compatibility.SiteManager;
import org.osgi.framework.Version;

/**
 * Generic super-class for all script generator classes. 
 * It contains basic informations like the script, the configurations, and a location 
 */
public abstract class AbstractScriptGenerator implements IXMLConstants, IPDEBuildConstants, IBuildPropertiesConstants {
	private static final FilenameFilter METADATA_REPO_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.startsWith("content.") || name.startsWith("compositeContent.") || //$NON-NLS-1$ //$NON-NLS-2$
					name.endsWith(".profile") || name.endsWith(".profile.gz"); //$NON-NLS-1$//$NON-NLS-2$
		}
	};

	private static final FilenameFilter ARTIFACT_REPO_FILTER = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.startsWith("artifacts.") || name.startsWith("compositeArtifacts."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	};

	private static Properties immutableAntProperties = null;
	protected static boolean embeddedSource = false;
	protected static boolean forceUpdateJarFormat = false;
	private static List configInfos;
	protected static String workingDirectory;
	protected static boolean buildingOSGi = true;
	protected URI[] contextMetadata = null;
	protected URI[] contextArtifacts = null;
	protected AntScript script;
	protected Properties platformProperties;
	protected String productQualifier;

	private static PDEUIStateWrapper pdeUIState;

	/** Location of the plug-ins and fragments. */
	protected String[] sitePaths;
	protected String[] pluginPath;
	protected BuildTimeSiteFactory siteFactory;

	/**
	 * Indicate whether the content of the pdestate should only contain the plugins that are in the transitive closure of the features being built
	 */
	protected boolean filterState = false;
	protected List featuresForFilterRoots = new ArrayList();
	protected List pluginsForFilterRoots = new ArrayList();
	protected boolean filterP2Base = false;

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

	protected static void setStaticAntProperties(Properties properties) {
		if (properties == null) {
			immutableAntProperties = new Properties();
			BuildDirector.p2Gathering = false;
		} else
			immutableAntProperties = properties;
		if (getImmutableAntProperty(IBuildPropertiesConstants.PROPERTY_PACKAGER_MODE) == null) {
			immutableAntProperties.setProperty(IBuildPropertiesConstants.PROPERTY_PACKAGER_MODE, "false"); //$NON-NLS-1$
		}
		//When we are generating build scripts, the normalization needs to be set, and when doing packaging the default is to set normalization to true for backward compatibility 
		if (!getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_PACKAGER_MODE) || getImmutableAntProperty(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER) == null) {
			immutableAntProperties.setProperty(IBuildPropertiesConstants.PROPERTY_PACKAGER_AS_NORMALIZER, "true"); //$NON-NLS-1$
		}

		if (getPropertyAsBoolean(IBuildPropertiesConstants.PROPERTY_P2_GATHERING))
			BuildDirector.p2Gathering = true;
	}

	public static String getImmutableAntProperty(String key) {
		return getImmutableAntProperty(key, null);
	}

	public static boolean getPropertyAsBoolean(String key) {
		String booleanValue = getImmutableAntProperty(key, null);
		if ("true".equalsIgnoreCase(booleanValue)) //$NON-NLS-1$
			return true;
		return false;
	}

	public static String getImmutableAntProperty(String key, String defaultValue) {
		if (immutableAntProperties == null || !immutableAntProperties.containsKey(key))
			return defaultValue;
		Object obj = immutableAntProperties.get(key);
		return (obj instanceof String) ? (String) obj : null;
	}

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
		SiteManager.setArch(Utils.getStringFromArray(archs, ",")); //$NON-NLS-1$
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
		if (location == null) {
			if (errorLevel != IStatus.INFO && errorLevel != IStatus.OK) {
				String message = NLS.bind(Messages.exception_missingFile, fileName);
				BundleHelper.getDefault().getLog().log(new Status(errorLevel, PI_PDEBUILD, EXCEPTION_READING_FILE, message, null));
			}
			return MissingProperties.getInstance();
		}

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
		script = newAntScript(scriptLocation, scriptName);
	}

	protected static AntScript newAntScript(String scriptLocation, String scriptName) throws CoreException {
		AntScript result = null;
		try {
			OutputStream scriptStream = new BufferedOutputStream(new FileOutputStream(scriptLocation + '/' + scriptName));
			try {
				result = new AntScript(scriptStream);
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
		return result;
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

	protected static boolean loadP2Class() {
		try {
			BundleHelper.getDefault().getClass().getClassLoader().loadClass("org.eclipse.equinox.p2.publisher.Publisher"); //$NON-NLS-1$
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * Return a build time site referencing things to be built.   
	 * @param refresh : indicate if a refresh must be performed. Although this flag is set to true, a new site is not rebuild if the urls of the site did not changed 
	 * @return BuildTimeSite
	 * @throws CoreException
	 */
	public BuildTimeSite getSite(boolean refresh) throws CoreException {
		if (siteFactory != null && refresh == false)
			return siteFactory.createSite();

		//If there is an exception from createSite(), we will discard the factory
		BuildTimeSiteFactory factory = new BuildTimeSiteFactory();
		factory.setFilterState(filterState);
		factory.setFilterRoots(featuresForFilterRoots, pluginsForFilterRoots);
		factory.setReportResolutionErrors(reportResolutionErrors);
		factory.setFilterP2Base(filterP2Base);
		factory.setSitePaths(getPaths());
		factory.setEESources(getEESources());
		factory.setInitialState(pdeUIState);

		BuildTimeSite result = factory.createSite();
		siteFactory = factory;

		if (platformProperties != null)
			result.setPlatformPropeties(platformProperties);

		File baseProfile = result.getSiteContentProvider().getBaseProfile();
		if (baseProfile != null) {
			List repos = getAssociatedRepositories(baseProfile);
			if (repos.size() > 0) {
				addContextRepos((URI[]) repos.toArray(new URI[repos.size()]));
			}
		}

		return result;
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

	protected String[] getEESources() {
		return null;
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
		setStateExtraData(classpath, patchData, null);
	}

	public void setStateExtraData(HashMap classpath, Map patchData, Map outputFolders) {
		ensurePDEUIStateNotNull();
		pdeUIState.setExtraData(classpath, patchData, outputFolders);
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

	public ProductFile loadProduct(String product) throws CoreException {
		//the ProductFile uses the OS to determine which icons to return, we don't care so can use null
		//this is better since this generator may be used for multiple OS's
		return loadProduct(product, null);
	}

	public ProductFile loadProduct(String product, String os) throws CoreException {
		if (product == null || product.startsWith("${") || product.length() == 0) { //$NON-NLS-1$
			return null;
		}
		String productPath = findFile(product, false);
		File f = null;
		if (productPath != null) {
			f = new File(productPath);
		} else {
			// couldn't find productFile, try it as a path directly
			f = new File(product);
			if (!f.exists() || !f.isFile()) {
				// doesn't exist, try it as a path relative to the working directory
				f = new File(getWorkingDirectory(), product);
				if (!f.exists() || !f.isFile()) {
					f = new File(getWorkingDirectory() + "/" + DEFAULT_PLUGIN_LOCATION, product); //$NON-NLS-1$
					if (!f.exists() || !f.isFile()) {
						f = new File(getWorkingDirectory() + '/' + DEFAULT_FEATURE_LOCATION, product);
					}
				}
			}
		}
		return new ProductFile(f.getAbsolutePath(), os);
	}

	//Find a file in a bundle or a feature.
	//location is assumed to be structured like : /<featureId | pluginId>/path.to.the.file
	protected String findFile(String location, boolean makeRelative) {
		if (location == null || location.length() == 0)
			return null;

		//shortcut building the site if we don't need to
		if (new File(location).exists())
			return location;

		PDEState state;
		try {
			state = getSite(false).getRegistry();
		} catch (CoreException e) {
			BundleHelper.getDefault().getLog().log(e.getStatus());
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
		BuildTimeFeature feature = null;
		try {
			feature = getSite(false).findFeature(id, null, false);
		} catch (CoreException e) {
			BundleHelper.getDefault().getLog().log(e.getStatus());
		}
		if (feature == null)
			return null;

		String featureRoot = feature.getRootLocation();
		if (featureRoot != null)
			return checkFile(new Path(featureRoot), path, makeRelative);
		return null;
	}

	protected String findConfigFile(ProductFile productFile, String os) {
		String path = productFile.getConfigIniPath(os);
		if (path == null)
			return null;

		String result = findFile(path, false);
		if (result != null)
			return result;

		// couldn't find productFile, try it as a path directly
		File f = new File(path);
		if (f.exists() && f.isFile())
			return f.getAbsolutePath();

		// relative to the working directory
		f = new File(getWorkingDirectory(), path);
		if (f.exists() && f.isFile())
			return f.getAbsolutePath();

		// relative to the working directory/plugins
		f = new File(getWorkingDirectory() + "/" + DEFAULT_PLUGIN_LOCATION, path); //$NON-NLS-1$
		if (f.exists() && f.isFile())
			return f.getAbsolutePath();

		//relative to .product file
		f = new File(productFile.getLocation().getParent(), path);
		if (f.exists() && f.isFile())
			return f.getAbsolutePath();

		return null;
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

	public void setFilterState(boolean filter) {
		filterState = filter;
	}

	public void setFilterP2Base(boolean filter) {
		filterP2Base = filter;
	}

	static private URI getDownloadCacheLocation(IProvisioningAgent agent) {
		IAgentLocation location = (IAgentLocation) agent.getService(IAgentLocation.SERVICE_NAME);
		if (location == null)
			return null;
		return URIUtil.append(location.getDataArea("org.eclipse.equinox.p2.core"), "cache/"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void setContextArtifacts(URI[] uris) {
		contextArtifacts = uris;
	}

	protected void setContextMetadata(URI[] uris) {
		contextMetadata = uris;
	}

	public void setContextMetadataRepositories(URI[] uris) {
		Set uriSet = new HashSet();
		uriSet.addAll(Arrays.asList(uris));

		for (int i = 0; i < uris.length; i++) {
			//try and find additional repos associated with a profile
			File uriFile = URIUtil.toFile(uris[i]);
			uriSet.addAll(getAssociatedRepositories(uriFile));
		}

		addContextRepos((URI[]) uriSet.toArray(new URI[uriSet.size()]));
	}

	protected void addContextRepos(URI[] repos) {
		List metadata = filterRepos(repos, METADATA_REPO_FILTER);
		List artifacts = filterRepos(repos, ARTIFACT_REPO_FILTER);

		if (contextMetadata != null) {
			Set uriSet = new HashSet();
			uriSet.addAll(Arrays.asList(contextMetadata));
			uriSet.addAll(metadata);
			contextMetadata = (URI[]) uriSet.toArray(new URI[uriSet.size()]);
		} else {
			contextMetadata = (URI[]) metadata.toArray(new URI[metadata.size()]);
		}

		if (contextArtifacts != null) {
			Set uriSet = new HashSet();
			uriSet.addAll(Arrays.asList(contextArtifacts));
			uriSet.addAll(artifacts);
			contextArtifacts = (URI[]) uriSet.toArray(new URI[uriSet.size()]);
		} else {
			contextArtifacts = (URI[]) artifacts.toArray(new URI[artifacts.size()]);
		}
	}

	//return only the metadata repos, and also the ones we aren't sure about
	private List filterRepos(URI[] contexts, FilenameFilter repoFilter) {
		if (contexts == null)
			return null;
		ArrayList result = new ArrayList();
		for (int i = 0; i < contexts.length; i++) {
			File repo = URIUtil.toFile(contexts[i]);
			if (repo == null) {
				//remote, not sure, just use it
				result.add(contexts[i]);
			} else {
				String[] list = repo.list(repoFilter);
				if (list != null && list.length > 0)
					result.add(contexts[i]);
			}
		}
		return result;
	}

	private List getAssociatedRepositories(File profileFile) {
		if (profileFile == null || !profileFile.exists() || !profileFile.getName().endsWith(".profile")) //$NON-NLS-1$
			return Collections.EMPTY_LIST;

		ArrayList result = new ArrayList();
		URI profileURI = profileFile.toURI();
		result.add(profileURI);

		Map profileInfo = extractProfileInformation(profileFile);
		if (profileInfo == null)
			return result;

		File areaFile = new File((String) profileInfo.get(PROFILE_DATA_AREA));
		if (areaFile.exists()) {
			IProvisioningAgent agent = BundleHelper.getDefault().getProvisioningAgent(areaFile.toURI());
			if (agent != null) {
				IProfileRegistry registry = new SimpleProfileRegistry(agent, (File) profileInfo.get(PROFILE_REGISTRY), null, false);
				try {
					long timestamp = ((Long) profileInfo.get(PROFILE_TIMESTAMP)).longValue();
					String profileId = (String) profileInfo.get(PROFILE_ID);
					if (timestamp == -1L) {
						long[] timestamps = registry.listProfileTimestamps(profileId);
						if (timestamps.length > 0)
							timestamp = timestamps[timestamps.length - 1];
					}

					//specifying the timestamp avoids attempting to lock the profile registry
					//which could be a problem if it is read only.
					if (timestamp > 0) {
						IProfile profile = registry.getProfile(profileId, timestamp);
						if (profile != null) {
							String cache = profile.getProperty(IProfile.PROP_CACHE);
							if (cache != null) {
								File cacheFolder = new File(cache);
								if (cacheFolder.exists()) {
									result.add(cacheFolder.toURI());
								} else {
									//if cache does not exist, this could be a roaming profile that has not
									//been run yet, lets guess and use the parent of the p2 data area
									result.add(areaFile.getParentFile().toURI());
								}
							}
							String sharedCache = profile.getProperty(IProfile.PROP_SHARED_CACHE);
							if (sharedCache != null)
								result.add(new File(cache).toURI());
							String dropinRepositories = profile.getProperty("org.eclipse.equinox.p2.cache.extensions"); //$NON-NLS-1$
							if (dropinRepositories != null) {
								// #filterRepos will remove any dropin folders that require synchronization
								StringTokenizer tokenizer = new StringTokenizer(dropinRepositories, "|"); //$NON-NLS-1$
								while (tokenizer.hasMoreTokens()) {
									try {
										result.add(new URI(tokenizer.nextToken()));
									} catch (URISyntaxException e) {
										//skip
									}
								}
							}
						}
					}
				} catch (IllegalStateException e) {
					//unable to read profile, may be read only
					result.add(areaFile.getParentFile().toURI());
				}

				//download cache
				URI download = getDownloadCacheLocation(agent);
				if (URIUtil.toFile(download).exists())
					result.add(download);
			}
		}
		return result;
	}

	private static String PROFILE_TIMESTAMP = "timestamp"; //$NON-NLS-1$
	private static String PROFILE_ID = "profileId"; //$NON-NLS-1$
	private static String PROFILE_DATA_AREA = "dataArea"; //$NON-NLS-1$
	private static String PROFILE_REGISTRY = "registry"; //$NON-NLS-1$

	private static Map extractProfileInformation(File target) {
		if (target == null || !target.exists())
			return null;

		IPath path = new Path(target.getAbsolutePath());
		if (!path.lastSegment().endsWith(PROFILE) && !path.lastSegment().endsWith(PROFILE_GZ))
			return null;

		//expect at least "p2/org.eclipse.equinox.p2.engine/profileRegistry/Profile.profile"
		if (path.segmentCount() < 4)
			return null;

		Map results = new HashMap();
		results.put(PROFILE_TIMESTAMP, new Long(-1));

		String profileId = null;
		if (target.isFile()) {
			//p2/org.eclipse.equinox.p2.engine/profileRegistry/Profile.profile/123456.profile.gz
			if (path.segmentCount() < 5)
				return null;

			String timestamp = path.lastSegment();
			int idx = timestamp.indexOf('.');
			if (idx > 0) {
				timestamp = timestamp.substring(0, idx);
				try {
					results.put(PROFILE_TIMESTAMP, new Long(timestamp));
				} catch (NumberFormatException e) {
					//not a timestamp?
				}
			}

			path = path.removeLastSegments(1);
			profileId = path.removeFileExtension().lastSegment();
		} else {
			//target is the profile folder
			profileId = path.removeFileExtension().lastSegment();
		}

		profileId = SimpleProfileRegistry.unescape(profileId);
		results.put(PROFILE_ID, profileId);

		//remove Profile.profile to get the registry folder
		path = path.removeLastSegments(1);
		results.put(PROFILE_REGISTRY, path.toFile());

		//removing "org.eclipse.equinox.p2.engine/profileRegistry"
		path = path.removeLastSegments(2);
		results.put(PROFILE_DATA_AREA, path.toOSString());

		return results;
	}

	public URI[] getContextMetadata() {
		return contextMetadata;
	}

	public URI[] getContextArtifacts() {
		return contextArtifacts;
	}

	public void setProductQualifier(String value) {
		productQualifier = value;
	}

	/*
	 * If the user has specified a platform properties then load it.
	 */
	public void setPlatformProperties(String filename) {
		if (filename == null || filename.trim().length() == 0)
			return;
		File file = new File(filename);
		if (!file.exists())
			return;
		platformProperties = new Properties();
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(file));
			platformProperties.load(input);
		} catch (IOException e) {
			platformProperties = null;
			String message = NLS.bind(Messages.error_loading_platform_properties, filename);
			IStatus status = new Status(IStatus.WARNING, IPDEBuildConstants.PI_PDEBUILD, message, e);
			BundleHelper.getDefault().getLog().log(status);
		} finally {
			if (input != null)
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
		}
	}

	protected void generateProductReplaceTask(ProductFile product, String productFilePath, AssemblyInformation assemblyInfo) {
		if (product == null)
			return;

		BuildTimeSite site = null;
		try {
			site = getSite(false);
		} catch (CoreException e1) {
			return;
		}

		String version = product.getVersion();
		if (version.endsWith(PROPERTY_QUALIFIER)) {
			Version oldVersion = new Version(version);
			version = oldVersion.getMajor() + "." + oldVersion.getMinor() + "." + oldVersion.getMicro() + "." + Utils.getPropertyFormat(PROPERTY_P2_PRODUCT_QUALIFIER); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		List productEntries = product.getProductEntries();
		String mappings = Utils.getEntryVersionMappings((FeatureEntry[]) productEntries.toArray(new FeatureEntry[productEntries.size()]), site, assemblyInfo);

		script.println("<eclipse.idReplacer productFilePath=\"" + AntScript.getEscaped(productFilePath) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		script.println("                    selfVersion=\"" + version + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
		if (product.useFeatures())
			script.println("                    featureIds=\"" + mappings + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		else
			script.println("                    pluginIds=\"" + mappings + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ 

		return;
	}
}
