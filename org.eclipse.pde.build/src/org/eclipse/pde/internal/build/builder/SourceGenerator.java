/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.builder;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.*;
import java.util.jar.Attributes.Name;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator.CompiledEntry;
import org.eclipse.pde.internal.build.site.*;
import org.osgi.framework.Version;

public class SourceGenerator implements IPDEBuildConstants, IBuildPropertiesConstants {
	private static final String COMMENT_START_TAG = "<!--"; //$NON-NLS-1$
	private static final String COMMENT_END_TAG = "-->"; //$NON-NLS-1$
	private static final String PLUGIN_START_TAG = "<plugin"; //$NON-NLS-1$
	private static final String FEATURE_START_TAG = "<feature";//$NON-NLS-1$
	private static final String FRAGMENT_START_TAG = "<fragment"; //$NON-NLS-1$
	private static final String VERSION = "version";//$NON-NLS-1$
	private static final String PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	private static final String TEMPLATE = "data"; //$NON-NLS-1$

	private String featureRootLocation;
	private String sourceFeatureId;
	private String brandingPlugin;
	private Properties buildProperties;
	private boolean individualSourceBundles = false;

	private BuildDirector director;
	private String[] extraEntries;
	private Map excludedEntries;

	public void setSourceFeatureId(String id) {
		sourceFeatureId = id;
	}

	public void setExtraEntries(String[] extraEntries) {
		this.extraEntries = extraEntries;
	}

	public void setDirector(BuildDirector director) {
		this.director = director;
	}

	public void setIndividual(boolean individual) {
		this.individualSourceBundles = individual;
	}

	private void initialize(BuildTimeFeature feature, String sourceFeatureName) throws CoreException {
		featureRootLocation = feature.getRootLocation();
		setSourceFeatureId(sourceFeatureName);
		collectSourceEntries(feature);
	}

	private BuildTimeSite getSite() throws CoreException {
		return director.getSite(false);
	}

	private String getWorkingDirectory() {
		return AbstractScriptGenerator.getWorkingDirectory();
	}

	private Properties getBuildProperties() throws CoreException {
		if (buildProperties == null)
			buildProperties = AbstractScriptGenerator.readProperties(featureRootLocation, PROPERTIES_FILE, IStatus.OK);
		return buildProperties;
	}

	protected Properties getBuildProperties(BundleDescription model) throws CoreException {
		return AbstractScriptGenerator.readProperties(model.getLocation(), PROPERTIES_FILE, IStatus.OK);
	}

	private String getSourcePluginName(FeatureEntry plugin, boolean versionSuffix) {
		return plugin.getId() + (versionSuffix ? "_" + plugin.getVersion() : ""); //$NON-NLS-1$	//$NON-NLS-2$
	}

	private void collectSourceEntries(BuildTimeFeature feature) throws CoreException {
		FeatureEntry[] pluginList = feature.getPluginEntries();
		for (int i = 0; i < pluginList.length; i++) {
			FeatureEntry entry = pluginList[i];
			BundleDescription model;
			if (director.selectConfigs(entry).size() == 0)
				continue;

			String versionRequested = entry.getVersion();
			model = getSite().getRegistry().getResolvedBundle(entry.getId(), versionRequested);
			if (model == null)
				continue;

			collectSourcePlugins(feature, pluginList[i], model);
		}
	}

	private void collectSourcePlugins(BuildTimeFeature feature, FeatureEntry pluginEntry, BundleDescription model) throws CoreException {
		//don't gather if we are doing individual source bundles
		if (individualSourceBundles)
			return;

		// The generic entry may not be part of the configuration we are building however,
		// the code for a non platform specific plugin still needs to go into a generic source plugin
		String sourceId = computeSourceFeatureName(feature, false);
		if (pluginEntry.getOS() == null && pluginEntry.getWS() == null && pluginEntry.getArch() == null) {
			director.sourceToGather.addElementEntry(sourceId, model);
			return;
		}
		// Here we fan the plugins into the source fragment where they should go
		List correctConfigs = director.selectConfigs(pluginEntry);
		for (Iterator iter = correctConfigs.iterator(); iter.hasNext();) {
			Config configInfo = (Config) iter.next();
			director.sourceToGather.addElementEntry(sourceId + "." + configInfo.toString("."), model); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Method generateSourceFeature.
	 * @throws CoreException 
	 */
	public BuildTimeFeature generateSourceFeature(BuildTimeFeature feature, String sourceFeatureName) throws CoreException {
		initialize(feature, sourceFeatureName);
		BuildTimeFeature sourceFeature = createSourceFeature(feature);

		associateExtraEntries(sourceFeature);

		FeatureEntry sourcePlugin;
		if (individualSourceBundles) {
			/* individual source bundles */

			// branding plugin for source feature will be the source bundle generated
			//from the original branding plugin.
			brandingPlugin = feature.getBrandingPlugin();
			if (brandingPlugin != null) {
				brandingPlugin += ".source"; //$NON-NLS-1$
				sourceFeature.setBrandingPlugin(brandingPlugin);
			} else {
				brandingPlugin = sourceFeature.getId();
			}

			FeatureEntry[] plugins = feature.getPluginEntries();
			for (int i = 0; i < plugins.length; i++) {
				if (director.selectConfigs(plugins[i]).size() == 0)
					continue;
				createSourceBundle(sourceFeature, plugins[i]);
			}
		} else {
			/* one source bundle + platform fragments */
			if (AbstractScriptGenerator.isBuildingOSGi())
				sourcePlugin = create30SourcePlugin(sourceFeature);
			else
				sourcePlugin = createSourcePlugin(sourceFeature);

			generateSourceFragments(sourceFeature, sourcePlugin);
		}

		writeSourceFeature(sourceFeature);

		return sourceFeature;
	}

	// Add extra plugins into the given feature.
	private void associateExtraEntries(BuildTimeFeature sourceFeature) throws CoreException {
		BundleDescription model;
		FeatureEntry entry;

		for (int i = 1; i < extraEntries.length; i++) {
			Map items = Utils.parseExtraBundlesString(extraEntries[i], true);
			String id = (String) items.get(Utils.EXTRA_ID);
			Version version = (Version) items.get(Utils.EXTRA_VERSION);

			// see if we have a plug-in or a fragment
			if (extraEntries[i].startsWith("feature@")) { //$NON-NLS-1$
				entry = new FeatureEntry(id, version.toString(), false);
				if (items.containsKey(Utils.EXTRA_OPTIONAL))
					entry.setOptional(((Boolean) items.get(Utils.EXTRA_OPTIONAL)).booleanValue());
				entry.setEnvironment((String) items.get(Utils.EXTRA_OS), (String) items.get(Utils.EXTRA_WS), (String) items.get(Utils.EXTRA_ARCH), null);
				sourceFeature.addEntry(entry);
			} else if (extraEntries[i].startsWith("plugin@")) { //$NON-NLS-1$
				model = getSite().getRegistry().getResolvedBundle((String) items.get(Utils.EXTRA_ID), ((Version) items.get(Utils.EXTRA_VERSION)).toString());
				if (model == null) {
					IStatus status = getSite().missingPlugin(id, version.toString(), null, false);
					BundleHelper.getDefault().getLog().log(status);
					continue;
				}
				entry = new FeatureEntry(model.getSymbolicName(), model.getVersion().toString(), true);
				entry.setUnpack(((Boolean) items.get(Utils.EXTRA_UNPACK)).booleanValue());
				entry.setEnvironment((String) items.get(Utils.EXTRA_OS), (String) items.get(Utils.EXTRA_WS), (String) items.get(Utils.EXTRA_ARCH), null);
				sourceFeature.addEntry(entry);
			} else if (extraEntries[i].startsWith("exclude@")) { //$NON-NLS-1$
				if (excludedEntries == null)
					excludedEntries = new HashMap();

				if (excludedEntries.containsKey(id)) {
					((List) excludedEntries.get(id)).add(version);
				} else {
					List versionList = new ArrayList();
					versionList.add(version);
					excludedEntries.put(id, versionList);
				}
			}
		}
	}

	private void generateSourceFragments(BuildTimeFeature sourceFeature, FeatureEntry sourcePlugin) throws CoreException {
		Map fragments = director.sourceToGather.getElementEntries();
		for (Iterator iter = AbstractScriptGenerator.getConfigInfos().iterator(); iter.hasNext();) {
			Config configInfo = (Config) iter.next();
			if (configInfo.equals(Config.genericConfig()))
				continue;
			String sourceFragmentId = sourceFeature.getId() + "." + configInfo.toString("."); //$NON-NLS-1$ //$NON-NLS-2$
			Set fragmentEntries = (Set) fragments.get(sourceFragmentId);
			if (fragmentEntries == null || fragmentEntries.size() == 0)
				continue;
			FeatureEntry sourceFragment = new FeatureEntry(sourceFragmentId, sourceFeature.getVersion(), true);
			sourceFragment.setEnvironment(configInfo.getOs(), configInfo.getWs(), configInfo.getArch(), null);
			sourceFragment.setFragment(true);
			//sourceFeature.addPluginEntryModel(sourceFragment);
			if (AbstractScriptGenerator.isBuildingOSGi())
				create30SourceFragment(sourceFragment, sourcePlugin);
			else
				createSourceFragment(sourceFragment, sourcePlugin);

			sourceFeature.addEntry(sourceFragment);
		}
	}

	private String computeSourceFeatureName(Feature featureForName, boolean withNumber) throws CoreException {
		String sourceFeatureName = getBuildProperties().getProperty(PROPERTY_SOURCE_FEATURE_NAME);
		if (sourceFeatureName == null)
			sourceFeatureName = sourceFeatureId;
		if (sourceFeatureName == null)
			sourceFeatureName = featureForName.getId() + ".source"; //$NON-NLS-1$
		return sourceFeatureName + (withNumber ? "_" + featureForName.getVersion() : ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// Create a feature object representing a source feature based on the featureExample
	private BuildTimeFeature createSourceFeature(Feature featureExample) throws CoreException {
		String id = computeSourceFeatureName(featureExample, false);
		String version = featureExample.getVersion();
		BuildTimeFeature result = new BuildTimeFeature(id, version);

		result.setLabel(featureExample.getLabel());
		result.setProviderName(featureExample.getProviderName());
		result.setImage(featureExample.getImage());
		result.setInstallHandler(featureExample.getInstallHandler());
		result.setInstallHandlerLibrary(featureExample.getInstallHandlerLibrary());
		result.setInstallHandlerURL(featureExample.getInstallHandlerURL());
		result.setDescription(featureExample.getDescription());
		result.setDescriptionURL(featureExample.getDescriptionURL());
		result.setCopyright(featureExample.getCopyright());
		result.setCopyrightURL(featureExample.getCopyrightURL());
		result.setLicense(featureExample.getLicense());
		result.setLicenseURL(featureExample.getLicenseURL());
		result.setLicenseFeature(featureExample.getLicenseFeature());
		result.setLicenseFeatureVersion(featureExample.getLicenseFeatureVersion());
		result.setUpdateSiteLabel(featureExample.getUpdateSiteLabel());
		result.setUpdateSiteURL(featureExample.getUpdateSiteURL());

		URLEntry[] siteEntries = featureExample.getDiscoverySites();
		for (int i = 0; i < siteEntries.length; i++) {
			result.addDiscoverySite(siteEntries[i].getAnnotation(), siteEntries[i].getURL());
		}

		result.setEnvironment(featureExample.getOS(), featureExample.getWS(), featureExample.getArch(), null);

		int contextLength = featureExample instanceof BuildTimeFeature ? ((BuildTimeFeature) featureExample).getContextQualifierLength() : -1;
		result.setContextQualifierLength(contextLength);
		return result;
	}

	/**
	 * Method createSourcePlugin.
	 */
	private FeatureEntry createSourcePlugin(BuildTimeFeature sourceFeature) throws CoreException {
		//Create an object representing the plugin
		FeatureEntry result = new FeatureEntry(sourceFeature.getId(), sourceFeature.getVersion(), true);
		sourceFeature.addEntry(result);
		// create the directory for the plugin
		IPath sourcePluginDirURL = new Path(getWorkingDirectory() + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(result, true));
		File sourcePluginDir = sourcePluginDirURL.toFile();
		sourcePluginDir.mkdirs();

		// Create the plugin.xml
		StringBuffer buffer;
		Path templatePluginXML = new Path(TEMPLATE + "/21/plugin/" + Constants.PLUGIN_FILENAME_DESCRIPTOR); //$NON-NLS-1$
		URL templatePluginURL = BundleHelper.getDefault().find(templatePluginXML);
		if (templatePluginURL == null) {
			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, templatePluginXML), null);
			BundleHelper.getDefault().getLog().log(status);
			return null;
		}
		try {
			buffer = Utils.readFile(templatePluginURL.openStream());
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_readingFile, templatePluginURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}
		int beginId = Utils.scan(buffer, 0, REPLACED_PLUGIN_ID);
		buffer.replace(beginId, beginId + REPLACED_PLUGIN_ID.length(), result.getId());
		//set the version number
		beginId = Utils.scan(buffer, beginId, REPLACED_PLUGIN_VERSION);
		buffer.replace(beginId, beginId + REPLACED_PLUGIN_VERSION.length(), result.getVersion());
		try {
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourcePluginDirURL.append(Constants.PLUGIN_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_writingFile, templatePluginURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}
		Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplatePlugin", sourcePluginDir.getAbsolutePath()); //$NON-NLS-1$
		if (copiedFiles.contains(Constants.PLUGIN_FILENAME_DESCRIPTOR)) {
			replaceXMLAttribute(sourcePluginDirURL.append(Constants.PLUGIN_FILENAME_DESCRIPTOR).toOSString(), PLUGIN_START_TAG, VERSION, result.getVersion());
		}
		//	If a build.properties file already exist then we use it supposing it is correct.
		File buildProperty = sourcePluginDirURL.append(PROPERTIES_FILE).toFile();
		if (!buildProperty.exists()) {
			copiedFiles.add(Constants.PLUGIN_FILENAME_DESCRIPTOR); //Because the plugin.xml is not copied, we need to add it to the file
			copiedFiles.add("src/**/*.zip"); //$NON-NLS-1$
			Properties sourceBuildProperties = new Properties();
			sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
			sourceBuildProperties.put(SOURCE_PLUGIN_ATTRIBUTE, "true"); //$NON-NLS-1$
			try {
				Utils.writeProperties(sourceBuildProperties, buildProperty, null);
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
			}
		}
		PDEState state = getSite().getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(result.getId());
		if (oldBundle != null)
			state.getState().removeBundle(oldBundle);
		state.addBundle(sourcePluginDir);
		return result;
	}

	private void create30SourceFragment(FeatureEntry fragment, FeatureEntry plugin) throws CoreException {
		// create the directory for the plugin
		Path sourceFragmentDirURL = new Path(getWorkingDirectory() + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(fragment, true));
		File sourceFragmentDir = new File(sourceFragmentDirURL.toOSString());
		new File(sourceFragmentDir, "META-INF").mkdirs(); //$NON-NLS-1$
		try {
			// read the content of the template file
			Path fragmentPath = new Path(TEMPLATE + "/30/fragment/" + Constants.BUNDLE_FILENAME_DESCRIPTOR);//$NON-NLS-1$
			URL templateLocation = BundleHelper.getDefault().find(fragmentPath);
			if (templateLocation == null) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, fragmentPath), null);
				BundleHelper.getDefault().getLog().log(status);
				return;
			}

			//Copy the fragment.xml
			try {
				InputStream fragmentXML = BundleHelper.getDefault().getBundle().getEntry(TEMPLATE + "/30/fragment/fragment.xml").openStream(); //$NON-NLS-1$
				Utils.transferStreams(fragmentXML, new FileOutputStream(sourceFragmentDirURL.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR).toOSString()));
			} catch (IOException e1) {
				String message = NLS.bind(Messages.exception_readingFile, TEMPLATE + "/30/fragment/fragment.xml"); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e1));
			}

			StringBuffer buffer = Utils.readFile(templateLocation.openStream());
			//Set the Id of the fragment
			int beginId = Utils.scan(buffer, 0, REPLACED_FRAGMENT_ID);
			buffer.replace(beginId, beginId + REPLACED_FRAGMENT_ID.length(), fragment.getId());
			//		set the version number
			beginId = Utils.scan(buffer, beginId, REPLACED_FRAGMENT_VERSION);
			buffer.replace(beginId, beginId + REPLACED_FRAGMENT_VERSION.length(), fragment.getVersion());
			// Set the Id of the plugin for the fragment
			beginId = Utils.scan(buffer, beginId, REPLACED_PLUGIN_ID);
			buffer.replace(beginId, beginId + REPLACED_PLUGIN_ID.length(), plugin.getId());
			//		set the version number of the plugin to which the fragment is attached to
			BundleDescription effectivePlugin = getSite().getRegistry().getResolvedBundle(plugin.getId(), plugin.getVersion());
			beginId = Utils.scan(buffer, beginId, REPLACED_PLUGIN_VERSION);
			buffer.replace(beginId, beginId + REPLACED_PLUGIN_VERSION.length(), effectivePlugin.getVersion().toString());
			// Set the platform filter of the fragment
			beginId = Utils.scan(buffer, beginId, REPLACED_PLATFORM_FILTER);
			buffer.replace(beginId, beginId + REPLACED_PLATFORM_FILTER.length(), "(& (osgi.ws=" + fragment.getWS() + ") (osgi.os=" + fragment.getOS() + ") (osgi.arch=" + fragment.getArch() + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourceFragmentDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString()));
			Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplateFragment", sourceFragmentDir.getAbsolutePath()); //$NON-NLS-1$
			if (copiedFiles.contains(Constants.BUNDLE_FILENAME_DESCRIPTOR)) {
				//make sure the manifest.mf has the versions we want
				replaceManifestValue(sourceFragmentDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString(), org.osgi.framework.Constants.BUNDLE_VERSION, fragment.getVersion());
				String host = plugin.getId() + ';' + org.osgi.framework.Constants.BUNDLE_VERSION + '=' + effectivePlugin.getVersion().toString();
				replaceManifestValue(sourceFragmentDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString(), org.osgi.framework.Constants.FRAGMENT_HOST, host);
			}
			File buildProperty = sourceFragmentDirURL.append(PROPERTIES_FILE).toFile();
			if (!buildProperty.exists()) { //If a build.properties file already exist  then we don't override it.
				copiedFiles.add(Constants.FRAGMENT_FILENAME_DESCRIPTOR); //Because the fragment.xml is not copied, we need to add it to the file
				copiedFiles.add("src/**"); //$NON-NLS-1$
				copiedFiles.add(Constants.BUNDLE_FILENAME_DESCRIPTOR);
				Properties sourceBuildProperties = new Properties();
				sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
				sourceBuildProperties.put("sourcePlugin", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					Utils.writeProperties(sourceBuildProperties, buildProperty, null);
				} catch (IOException e) {
					String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				}
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writingFile, sourceFragmentDir.getName());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, null));
		}
		PDEState state = getSite().getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(fragment.getId());
		if (oldBundle != null)
			state.getState().removeBundle(oldBundle);
		state.addBundle(sourceFragmentDir);
	}

	private void createSourceFragment(FeatureEntry fragment, FeatureEntry plugin) throws CoreException {
		// create the directory for the plugin
		Path sourceFragmentDirURL = new Path(getWorkingDirectory() + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(fragment, false));
		File sourceFragmentDir = new File(sourceFragmentDirURL.toOSString());
		sourceFragmentDir.mkdirs();
		try {
			// read the content of the template file
			Path fragmentPath = new Path(TEMPLATE + "/21/fragment/" + Constants.FRAGMENT_FILENAME_DESCRIPTOR);//$NON-NLS-1$
			URL templateLocation = BundleHelper.getDefault().find(fragmentPath);
			if (templateLocation == null) {
				IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, fragmentPath), null);
				BundleHelper.getDefault().getLog().log(status);
				return;
			}

			StringBuffer buffer = Utils.readFile(templateLocation.openStream());
			//Set the Id of the fragment
			int beginId = Utils.scan(buffer, 0, REPLACED_FRAGMENT_ID);
			buffer.replace(beginId, beginId + REPLACED_FRAGMENT_ID.length(), fragment.getId());
			//		set the version number
			beginId = Utils.scan(buffer, beginId, REPLACED_FRAGMENT_VERSION);
			buffer.replace(beginId, beginId + REPLACED_FRAGMENT_VERSION.length(), fragment.getVersion());
			// Set the Id of the plugin for the fragment
			beginId = Utils.scan(buffer, beginId, REPLACED_PLUGIN_ID);
			buffer.replace(beginId, beginId + REPLACED_PLUGIN_ID.length(), plugin.getId());
			//		set the version number of the plugin to which the fragment is attached to
			beginId = Utils.scan(buffer, beginId, REPLACED_PLUGIN_VERSION);
			buffer.replace(beginId, beginId + REPLACED_PLUGIN_VERSION.length(), plugin.getVersion());
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourceFragmentDirURL.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR).toOSString()));
			Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplateFragment", sourceFragmentDir.getAbsolutePath()); //$NON-NLS-1$
			if (copiedFiles.contains(Constants.FRAGMENT_FILENAME_DESCRIPTOR)) {
				replaceXMLAttribute(sourceFragmentDirURL.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR).toOSString(), FRAGMENT_START_TAG, VERSION, fragment.getVersion());
				replaceXMLAttribute(sourceFragmentDirURL.append(Constants.FRAGMENT_FILENAME_DESCRIPTOR).toOSString(), FRAGMENT_START_TAG, PLUGIN_VERSION, plugin.getVersion());
			}
			File buildProperty = sourceFragmentDirURL.append(PROPERTIES_FILE).toFile();
			if (!buildProperty.exists()) { //If a build.properties file already exist  then we don't override it.
				copiedFiles.add(Constants.FRAGMENT_FILENAME_DESCRIPTOR); //Because the fragment.xml is not copied, we need to add it to the file
				copiedFiles.add("src/**"); //$NON-NLS-1$
				Properties sourceBuildProperties = new Properties();
				sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
				sourceBuildProperties.put("sourcePlugin", "true"); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					Utils.writeProperties(sourceBuildProperties, buildProperty, null);
				} catch (IOException e) {
					String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
				}
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writingFile, sourceFragmentDir.getName());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, null));
		}
		PDEState state = getSite().getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(fragment.getId());
		if (oldBundle != null)
			state.getState().removeBundle(oldBundle);
		state.addBundle(sourceFragmentDir);
	}

	private void writeSourceFeature(BuildTimeFeature sourceFeature) throws CoreException {
		String sourceFeatureDir = getWorkingDirectory() + '/' + DEFAULT_FEATURE_LOCATION + '/' + sourceFeatureId;
		File sourceDir = new File(sourceFeatureDir);
		sourceDir.mkdirs();
		// write the source feature to the feature.xml
		File file = new File(sourceFeatureDir + '/' + Constants.FEATURE_FILENAME_DESCRIPTOR);
		try {
			SourceFeatureWriter writer = new SourceFeatureWriter(new BufferedOutputStream(new FileOutputStream(file)), sourceFeature, getSite());
			try {
				writer.printFeature();
			} finally {
				writer.close();
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.error_creatingFeature, sourceFeature.getId());
			throw new CoreException(new Status(IStatus.OK, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
		Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + "sourceTemplateFeature", sourceFeatureDir); //$NON-NLS-1$
		if (copiedFiles.contains(Constants.FEATURE_FILENAME_DESCRIPTOR)) {
			//we overwrote our feature.xml with a template, replace the version
			replaceXMLAttribute(sourceFeatureDir + '/' + Constants.FEATURE_FILENAME_DESCRIPTOR, FEATURE_START_TAG, VERSION, sourceFeature.getVersion());
		}
		File buildProperty = new File(sourceFeatureDir + '/' + PROPERTIES_FILE);
		if (buildProperty.exists()) {//If a build.properties file already exist then we don't override it.
			getSite().addFeatureReferenceModel(sourceDir);
			return;
		}
		copiedFiles.add(Constants.FEATURE_FILENAME_DESCRIPTOR); //Because the feature.xml is not copied, we need to add it to the file
		Properties sourceBuildProperties = new Properties();
		sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, Utils.getStringFromCollection(copiedFiles, ",")); //$NON-NLS-1$
		try {
			Utils.writeProperties(sourceBuildProperties, buildProperty, null);
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
		getSite().addFeatureReferenceModel(sourceDir);
	}

	private void replaceXMLAttribute(String location, String tag, String attr, String newValue) {
		File featureFile = new File(location);
		if (!featureFile.exists())
			return;

		StringBuffer buffer = null;
		try {
			buffer = Utils.readFile(featureFile);
		} catch (IOException e) {
			return;
		}

		int startComment = Utils.scan(buffer, 0, COMMENT_START_TAG);
		int endComment = startComment > -1 ? Utils.scan(buffer, startComment, COMMENT_END_TAG) : -1;
		int startTag = Utils.scan(buffer, 0, tag);
		while (startComment != -1 && startTag > startComment && startTag < endComment) {
			startTag = Utils.scan(buffer, endComment, tag);
			startComment = Utils.scan(buffer, endComment, COMMENT_START_TAG);
			endComment = startComment > -1 ? Utils.scan(buffer, startComment, COMMENT_END_TAG) : -1;
		}
		if (startTag == -1)
			return;
		int endTag = Utils.scan(buffer, startTag, ">"); //$NON-NLS-1$
		boolean attrFound = false;
		while (!attrFound) {
			int startAttributeWord = Utils.scan(buffer, startTag, attr);
			if (startAttributeWord == -1 || startAttributeWord > endTag)
				return;
			if (!Character.isWhitespace(buffer.charAt(startAttributeWord - 1))) {
				startTag = startAttributeWord + attr.length();
				continue;
			}
			//Verify that the word found is the actual attribute
			int endAttributeWord = startAttributeWord + attr.length();
			while (Character.isWhitespace(buffer.charAt(endAttributeWord)) && endAttributeWord < endTag) {
				endAttributeWord++;
			}
			if (endAttributeWord > endTag) { //attribute  has not been found
				return;
			}

			if (buffer.charAt(endAttributeWord) != '=') {
				startTag = endAttributeWord;
				continue;
			}

			int startVersionId = Utils.scan(buffer, startAttributeWord + 1, "\""); //$NON-NLS-1$
			int endVersionId = Utils.scan(buffer, startVersionId + 1, "\""); //$NON-NLS-1$
			buffer.replace(startVersionId + 1, endVersionId, newValue);
			attrFound = true;
		}
		if (attrFound) {
			try {
				Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(featureFile));
			} catch (IOException e) {
				//ignore
			}
		}
	}

	private FeatureEntry createSourceBundle(BuildTimeFeature sourceFeature, FeatureEntry pluginEntry) throws CoreException {
		BundleDescription bundle = getSite().getRegistry().getBundle(pluginEntry.getId(), pluginEntry.getVersion(), true);
		if (bundle == null) {
			getSite().missingPlugin(pluginEntry.getId(), pluginEntry.getVersion(), null, true);
		}

		if (excludedEntries != null && excludedEntries.containsKey(bundle.getSymbolicName())) {
			List excludedVersions = (List) excludedEntries.get(bundle.getSymbolicName());
			for (Iterator iterator = excludedVersions.iterator(); iterator.hasNext();) {
				Version version = (Version) iterator.next();
				if (Utils.matchVersions(bundle.getVersion().toString(), version.toString()))
					return null;
			}
		}

		Properties bundleProperties = getBuildProperties(bundle);
		if (!Boolean.valueOf(bundleProperties.getProperty(PROPERTY_GENERATE_SOURCE_BUNDLE, TRUE)).booleanValue()) {
			return null;
		}

		FeatureEntry sourceEntry = new FeatureEntry(pluginEntry.getId() + ".source", bundle.getVersion().toString(), true); //$NON-NLS-1$
		sourceEntry.setEnvironment(pluginEntry.getOS(), pluginEntry.getWS(), pluginEntry.getArch(), pluginEntry.getNL());
		sourceEntry.setUnpack(false);

		if (Utils.isBinary(bundle)) {
			//binary, don't generate a source bundle.  But we can add the source entry if we can find an already existing one
			BundleDescription sourceBundle = getSite().getRegistry().getResolvedBundle(sourceEntry.getId(), sourceEntry.getVersion());
			if (sourceBundle != null) {
				if (Utils.isSourceBundle(sourceBundle)) {
					//it is a source bundle, check that it is for bundle
					Map headerMap = Utils.parseSourceBundleEntry(sourceBundle);
					Map entryMap = (Map) headerMap.get(bundle.getSymbolicName());
					if (entryMap != null && bundle.getVersion().toString().equals(entryMap.get(VERSION))) {
						sourceEntry.setUnpack(new File(sourceBundle.getLocation()).isDirectory());

						FeatureEntry existingEntry = sourceFeature.findPluginEntry(sourceEntry.getId(), sourceEntry.getVersion());
						if (existingEntry == null || existingEntry.getVersion() == GENERIC_VERSION_NUMBER) {
							if (existingEntry != null)
								sourceFeature.removeEntry(existingEntry);
							sourceFeature.addEntry(sourceEntry);
							return sourceEntry;
						}
						return existingEntry;
					}
				}
			}
			return null;
		}

		sourceFeature.addEntry(sourceEntry);

		generateSourcePlugin(sourceEntry, bundle);

		return sourceEntry;
	}

	private String getSourceRoot(CompiledEntry entry) {
		String jarName = entry.getName(false);
		if (jarName.equals(ModelBuildScriptGenerator.DOT))
			return jarName;
		String srcName = ModelBuildScriptGenerator.getSRCName(entry.getName(false));
		return srcName.substring(0, srcName.length() - 4); //remove .zip
	}

	public void generateSourcePlugin(FeatureEntry sourceEntry, BundleDescription originalBundle) throws CoreException {
		IPath sourcePluginDirURL = new Path(getWorkingDirectory() + '/' + DEFAULT_PLUGIN_LOCATION + '/' + sourceEntry.getId() + '_' + originalBundle.getVersion());

		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.put(Name.MANIFEST_VERSION, "1.0"); //$NON-NLS-1$
		attributes.put(new Name(org.osgi.framework.Constants.BUNDLE_MANIFESTVERSION), "2"); //$NON-NLS-1$
		attributes.put(new Name(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME), sourceEntry.getId());
		attributes.put(new Name(org.osgi.framework.Constants.BUNDLE_VERSION), originalBundle.getVersion().toString());

		if (originalBundle.getPlatformFilter() != null)
			attributes.put(new Name(ECLIPSE_PLATFORM_FILTER), originalBundle.getPlatformFilter());

		Properties origBuildProperties = getBuildProperties(originalBundle);
		String extraRoots = (String) origBuildProperties.get(PROPERTY_SRC_ROOTS);
		String sourceHeader = originalBundle.getSymbolicName() + ";version=\"" + originalBundle.getVersion().toString() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
		CompiledEntry[] entries = ModelBuildScriptGenerator.extractEntriesToCompile(origBuildProperties, originalBundle);
		if (entries.length > 0 || extraRoots != null) {
			sourceHeader += ";roots:=\""; //$NON-NLS-1$
			for (int i = 0; i < entries.length; i++) {
				if (i > 0)
					sourceHeader += ',';
				sourceHeader += getSourceRoot(entries[i]);
			}
			if (extraRoots != null) {
				if (entries.length > 0)
					sourceHeader += ',';
				sourceHeader += extraRoots;
			}
			sourceHeader += '\"';
		}
		attributes.put(new Name(ECLIPSE_SOURCE_BUNDLE), sourceHeader);

		//bundle Localization
		String localizationEntry = null;
		String localization = null;
		String vendor = null;
		String name = null;

		Properties bundleProperties = (Properties) originalBundle.getUserObject();
		if (bundleProperties != null) {
			localization = (String) bundleProperties.get(org.osgi.framework.Constants.BUNDLE_LOCALIZATION);
			vendor = (String) bundleProperties.get(org.osgi.framework.Constants.BUNDLE_VENDOR);
			name = (String) bundleProperties.get(org.osgi.framework.Constants.BUNDLE_NAME);
		}

		String vendorKey = (vendor != null && vendor.startsWith("%")) ? vendor.substring(1) : null; //$NON-NLS-1$
		String nameKey = (name != null && name.startsWith("%")) ? name.substring(1) : null; //$NON-NLS-1$;

		if (localization == null)
			localization = PLUGIN;
		else {
			//read the localization properties from original bundle
			Properties localizationProperties = null;
			File localizationFile = new File(originalBundle.getLocation(), localization + ".properties"); //$NON-NLS-1$
			if (!localizationFile.exists() && originalBundle.getHost() != null) {
				// properties file does not exist,  we are a fragment, check the host
				BundleDescription host = (BundleDescription) originalBundle.getHost().getSupplier();
				localizationProperties = AbstractScriptGenerator.readProperties(host.getLocation(), localization + ".properties", IStatus.OK); //$NON-NLS-1$
			} else if (localizationFile.exists()) {
				localizationProperties = AbstractScriptGenerator.readProperties(originalBundle.getLocation(), localization + ".properties", IStatus.OK); //$NON-NLS-1$
			}

			if (localizationProperties != null) {
				if (vendorKey != null)
					vendor = localizationProperties.getProperty(vendorKey);
				if (nameKey != null)
					name = localizationProperties.getProperty(nameKey);
			}
		}

		// name not specified, use the source bundle id and externalize it anyway
		if (name == null)
			name = sourceEntry.getId();
		else
			name += " Source"; //$NON-NLS-1$
		if (nameKey == null)
			nameKey = "pluginName"; //$NON-NLS-1$
		// if vendor is not specified, we don't know what to put there
		if (vendor != null && vendorKey == null)
			vendorKey = "providerName"; //$NON-NLS-1$

		attributes.put(new Name(org.osgi.framework.Constants.BUNDLE_LOCALIZATION), localization);
		attributes.put(new Name(org.osgi.framework.Constants.BUNDLE_NAME), "%" + nameKey); //$NON-NLS-1$
		Properties localizationProperties = new Properties();
		localizationProperties.put(nameKey, name);
		if (vendorKey != null && vendor != null) {
			attributes.put(new Name(org.osgi.framework.Constants.BUNDLE_VENDOR), "%" + vendorKey); //$NON-NLS-1$
			localizationProperties.put(vendorKey, vendor);
		}

		localizationEntry = localization + ".properties"; //$NON-NLS-1$
		File localizationFile = new File(sourcePluginDirURL.toFile(), localizationEntry);
		try {
			Utils.writeProperties(localizationProperties, localizationFile, "#Source Bundle Localization"); //$NON-NLS-1$
		} catch (IOException e) {
			//	what?
		}

		File manifestFile = new File(sourcePluginDirURL.toFile(), Constants.BUNDLE_FILENAME_DESCRIPTOR);
		manifestFile.getParentFile().mkdirs();
		try {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(manifestFile));
			try {
				manifest.write(out);
			} finally {
				out.close();
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writingFile, manifestFile.getAbsolutePath());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}

		// if this source bundle  will be the branding plug-in for the source feature, use the old plug-in template directory
		String template = sourceEntry.getId().equals(brandingPlugin) ? "sourceTemplatePlugin" : "sourceTemplateBundle"; //$NON-NLS-1$ //$NON-NLS-2$
		generateSourceFiles(sourcePluginDirURL, sourceEntry, template, localizationEntry, originalBundle);

		PDEState state = getSite().getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(sourceEntry.getId(), sourceEntry.getVersion());
		if (oldBundle != null)
			state.getState().removeBundle(oldBundle);
		state.addBundle(sourcePluginDirURL.toFile());

		director.sourceToGather.addElementEntry(sourceEntry.getId(), originalBundle);
	}

	private FeatureEntry create30SourcePlugin(BuildTimeFeature sourceFeature) throws CoreException {
		//Create an object representing the plugin
		FeatureEntry result = new FeatureEntry(sourceFeature.getId(), sourceFeature.getVersion(), true);
		sourceFeature.addEntry(result);

		// create the directory for the plugin
		IPath sourcePluginDirURL = new Path(getWorkingDirectory() + '/' + DEFAULT_PLUGIN_LOCATION + '/' + getSourcePluginName(result, true));
		File sourcePluginDir = sourcePluginDirURL.toFile();
		new File(sourcePluginDir, "META-INF").mkdirs(); //$NON-NLS-1$

		// Create the MANIFEST.MF
		StringBuffer buffer;
		Path templateManifest = new Path(TEMPLATE + "/30/plugin/" + Constants.BUNDLE_FILENAME_DESCRIPTOR); //$NON-NLS-1$
		URL templateManifestURL = BundleHelper.getDefault().find(templateManifest);
		if (templateManifestURL == null) {
			IStatus status = new Status(IStatus.WARNING, PI_PDEBUILD, IPDEBuildConstants.EXCEPTION_READING_FILE, NLS.bind(Messages.error_readingDirectory, templateManifest), null);
			BundleHelper.getDefault().getLog().log(status);
			return null;
		}
		try {
			buffer = Utils.readFile(templateManifestURL.openStream());
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_readingFile, templateManifestURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}
		int beginId = Utils.scan(buffer, 0, REPLACED_PLUGIN_ID);
		buffer.replace(beginId, beginId + REPLACED_PLUGIN_ID.length(), result.getId());
		//set the version number
		beginId = Utils.scan(buffer, beginId, REPLACED_PLUGIN_VERSION);
		buffer.replace(beginId, beginId + REPLACED_PLUGIN_VERSION.length(), result.getVersion());
		try {
			Utils.transferStreams(new ByteArrayInputStream(buffer.toString().getBytes()), new FileOutputStream(sourcePluginDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_writingFile, templateManifestURL.toExternalForm());
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READING_FILE, message, e1));
		}

		//Copy the plugin.xml
		try {
			InputStream pluginXML = BundleHelper.getDefault().getBundle().getEntry(TEMPLATE + "/30/plugin/plugin.xml").openStream(); //$NON-NLS-1$
			Utils.transferStreams(pluginXML, new FileOutputStream(sourcePluginDirURL.append(Constants.PLUGIN_FILENAME_DESCRIPTOR).toOSString()));
		} catch (IOException e1) {
			String message = NLS.bind(Messages.exception_readingFile, TEMPLATE + "/30/plugin/plugin.xml"); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e1));
		}

		//Copy the other files
		generateSourceFiles(sourcePluginDirURL, result, "sourceTemplatePlugin", null, null); //$NON-NLS-1$

		PDEState state = getSite().getRegistry();
		BundleDescription oldBundle = state.getResolvedBundle(result.getId());
		String oldBundleLocation = null;
		if (oldBundle != null) {
			oldBundleLocation = oldBundle.getLocation();
			state.getState().removeBundle(oldBundle);
		}
		state.addBundle(sourcePluginDir);

		if (oldBundleLocation != null) {
			state.getState().resolve(true);
			BundleDescription newBundle = state.getResolvedBundle(result.getId(), result.getVersion());
			//the old location is only interesting if it is different from the new one
			if (newBundle != null && !newBundle.getLocation().equals(oldBundleLocation)) {
				Properties bundleProperties = (Properties) newBundle.getUserObject();
				if (bundleProperties == null) {
					bundleProperties = new Properties();
					newBundle.setUserObject(bundleProperties);
				}

				bundleProperties.setProperty(OLD_BUNDLE_LOCATION, oldBundleLocation);
			}
		}

		return result;
	}

	private void generateSourceFiles(IPath sourcePluginDirURL, FeatureEntry sourceEntry, String templateDir, String extraFiles, BundleDescription originalBundle) throws CoreException {
		Collection copiedFiles = Utils.copyFiles(featureRootLocation + '/' + templateDir, sourcePluginDirURL.toFile().getAbsolutePath());
		if (copiedFiles.contains(Constants.BUNDLE_FILENAME_DESCRIPTOR)) {
			//make sure the manifest.mf has the version we want
			replaceManifestValue(sourcePluginDirURL.append(Constants.BUNDLE_FILENAME_DESCRIPTOR).toOSString(), org.osgi.framework.Constants.BUNDLE_VERSION, sourceEntry.getVersion());
		}

		String original = originalBundle != null ? originalBundle.getSymbolicName() + ';' + originalBundle.getVersion().toString() : "true"; //$NON-NLS-1$

		//	If a build.properties file already exist then we use it supposing it is correct.
		File buildProperty = sourcePluginDirURL.append(PROPERTIES_FILE).toFile();
		if (!buildProperty.exists()) {
			copiedFiles.add(Constants.PLUGIN_FILENAME_DESCRIPTOR); //Because the plugin.xml is not copied, we need to add it to the file
			copiedFiles.add("src/**"); //$NON-NLS-1$
			copiedFiles.add(Constants.BUNDLE_FILENAME_DESCRIPTOR);//Because the manifest.mf is not copied, we need to add it to the file
			Properties sourceBuildProperties = new Properties();
			String binIncludes = Utils.getStringFromCollection(copiedFiles, ","); //$NON-NLS-1$
			if (extraFiles != null)
				binIncludes += "," + extraFiles; //$NON-NLS-1$
			sourceBuildProperties.put(PROPERTY_BIN_INCLUDES, binIncludes);
			sourceBuildProperties.put(SOURCE_PLUGIN_ATTRIBUTE, original);
			try {
				Utils.writeProperties(sourceBuildProperties, buildProperty, null);
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_writingFile, buildProperty.getAbsolutePath());
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
			}
		} else if (originalBundle != null) {
			Properties props = AbstractScriptGenerator.readProperties(sourcePluginDirURL.toOSString(), PROPERTIES_FILE, IStatus.OK);
			props.put(SOURCE_PLUGIN_ATTRIBUTE, original);
			try {
				Utils.writeProperties(props, buildProperty, null);
			} catch (IOException e) {
				//ignore
			}
		}
	}

	private void replaceManifestValue(String location, String attribute, String newVersion) {
		Manifest manifest = null;
		try {
			//work around for bug 256787 
			InputStream is = new SequenceInputStream(new BufferedInputStream(new FileInputStream(location)), new ByteArrayInputStream(new byte[] {'\n'}));
			try {
				manifest = new Manifest(is);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			return;
		}

		manifest.getMainAttributes().put(new Attributes.Name(attribute), newVersion);

		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(location));
			try {
				manifest.write(os);
				os.write(new byte[] {'\n'});
			} finally {
				os.close();
			}
		} catch (IOException e1) {
			//ignore
		}
	}
}
