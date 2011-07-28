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
import java.util.*;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.build.IFetchFactory;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.ant.IScriptRunner;
import org.eclipse.pde.internal.build.fetch.CVSFetchTaskFactory;
import org.eclipse.pde.internal.build.site.*;
import org.osgi.framework.Version;

/**
 * Generates Ant scripts with a repository specific factory
 * to retrieve plug-ins and features from a repository.
 */
public class FetchScriptGenerator extends AbstractScriptGenerator {
	private static final Object SAVE_LOCK = new Object();
	private static final String FETCH_TASK_FACTORY = "internal.factory"; //$NON-NLS-1$
	private static final String FETCH_TASK_FACTORY_ID = "internal.factory.id"; //$NON-NLS-1$
	private static final String MATCHED_VERSION = "internal.matchedVersion"; //$NON-NLS-1$

	// flag saying if we want to recursively generate the scripts	
	protected boolean recursiveGeneration = true;

	// Points to the map files containing references to repository
	protected Properties directoryFile;
	protected String directoryLocation;
	protected SortedMap directory;

	protected String fetchCache;

	// The location of the CVS password file.
	protected String cvsPassFileLocation;

	protected boolean fetchChildren = true;

	protected Properties fetchTags = null;
	protected Map fetchOverrides = null;

	// The element (an entry of the map file) for which we create the script 
	protected String element;
	protected Version elementVersion;

	// The feature object representing the element
	protected BuildTimeFeature feature;
	//The map infos (of the element) processed
	protected Map mapInfos;
	// The content of the build.properties file associated with the feature
	protected Properties featureProperties;
	// Variables to control is a mkdir to a specific folder was already.
	protected List mkdirLocations = new ArrayList(5);
	// A property table containing the association between the plugins and the version from the map  
	protected Properties repositoryPluginTags = new Properties();
	protected Properties repositoryFeatureTags = new Properties();
	protected Properties sourceReferences = new Properties();

	//The registry of the task factories
	private FetchTaskFactoriesRegistry fetchTaskFactories;
	//Set of all the used factories while generating the fetch script for the top level element
	private final Set encounteredTypeOfRepo = new HashSet();

	public static final String FEATURE_ONLY = "featureOnly"; //$NON-NLS-1$
	public static final String FEATURE_AND_PLUGINS = "featureAndPlugins"; //$NON-NLS-1$
	public static final String FEATURES_RECURSIVELY = "featuresRecursively"; //$NON-NLS-1$
	public static final String FETCH_FILE_PREFIX = "fetch_"; //$NON-NLS-1$

	private String scriptName;
	private IScriptRunner scriptRunner;

	public FetchScriptGenerator() {
		super();
	}

	public FetchScriptGenerator(String element) {
		setElement(element);
	}

	public void setElement(String element) {
		Object[] split = splitElement(element);
		this.element = (String) split[0];
		this.elementVersion = (Version) split[1];
	}

	private Object[] splitElement(String elt) {
		int comma = elt.indexOf(',');
		if (comma == -1) {
			return new Object[] {elt, Version.emptyVersion};
		}
		return new Object[] {elt.substring(0, comma), new Version(elt.substring(comma + 1))};
	}

	private void initializeFactories() {
		fetchTaskFactories = new FetchTaskFactoriesRegistry();
	}

	/**
	 * @see AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		initializeFactories();
		mapInfos = processMapFileEntry(element, elementVersion);
		if (mapInfos == null) {
			IStatus warning = new Status(IStatus.WARNING, PI_PDEBUILD, WARNING_ELEMENT_NOT_FETCHED, NLS.bind(Messages.error_fetchingFailed, element), null);
			BundleHelper.getDefault().getLog().log(warning);
			return;
		}

		scriptName = FETCH_FILE_PREFIX + mapInfos.get(IFetchFactory.KEY_ELEMENT_NAME) + ".xml"; //$NON-NLS-1$
		openScript(workingDirectory, scriptName);
		try {
			generateFetchScript();
		} finally {
			closeScript();
		}

		if (recursiveGeneration && mapInfos.get(IFetchFactory.KEY_ELEMENT_TYPE).equals(IFetchFactory.ELEMENT_TYPE_FEATURE))
			generateFetchFilesForIncludedFeatures();

		saveRepositoryTags();
	}

	private void saveRepositoryTags(Properties properties, String fileName) throws CoreException {
		synchronized (SAVE_LOCK) {
			try {
				InputStream input = new BufferedInputStream(new FileInputStream(workingDirectory + '/' + fileName));
				try {
					properties.load(input);
				} finally {
					input.close();
				}
			} catch (IOException e) {
				//ignore the exception, the same may not exist
			}

			try {
				OutputStream os = new BufferedOutputStream(new FileOutputStream(workingDirectory + '/' + fileName));
				try {
					properties.store(os, null);
				} finally {
					os.close();
				}
			} catch (IOException e) {
				String message = NLS.bind(Messages.exception_writingFile, workingDirectory + '/' + fileName);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, null));
			}
		}
	}

	private void saveRepositoryTags() throws CoreException {
		saveRepositoryTags(repositoryPluginTags, DEFAULT_PLUGIN_REPOTAG_FILENAME_DESCRIPTOR);
		saveRepositoryTags(repositoryFeatureTags, DEFAULT_FEATURE_REPOTAG_FILENAME_DESCRIPTOR);
		saveRepositoryTags(sourceReferences, DEFAULT_SOURCE_REFERENCES_FILENAME_DESCRIPTOR);
	}

	/**
	 * Method generateFetchFilesForRequiredFeatures.
	 */
	private void generateFetchFilesForIncludedFeatures() throws CoreException {
		FeatureEntry[] referencedFeatures = feature.getIncludedFeatureReferences();
		for (int i = 0; i < referencedFeatures.length; i++) {
			String featureId = referencedFeatures[i].getId();
			if (featureProperties.containsKey(GENERATION_SOURCE_FEATURE_PREFIX + featureId))
				continue;

			FetchScriptGenerator generator = new FetchScriptGenerator("feature@" + featureId + ',' + referencedFeatures[i].getVersion()); //$NON-NLS-1$
			generator.setDirectoryLocation(directoryLocation);
			generator.setFetchChildren(fetchChildren);
			generator.setFetchCache(fetchCache);
			generator.setCvsPassFileLocation(cvsPassFileLocation);
			generator.setRecursiveGeneration(recursiveGeneration);
			generator.setFetchTag(fetchTags);
			generator.setFetchOverrides(fetchOverrides);
			generator.setDirectory(directory);
			generator.setDirectoryFile(directoryFile);
			generator.setBuildSiteFactory(siteFactory);
			generator.repositoryPluginTags = repositoryPluginTags;
			generator.setSourceReferences(sourceReferences);
			generator.setScriptRunner(scriptRunner);
			generator.generate();
		}
	}

	/**
	 * Main call for generating the script.
	 * 
	 * @throws CoreException
	 */
	protected void generateFetchScript() throws CoreException {
		generatePrologue();
		generateFetchTarget();
		generateFetchElementTarget();
		if (mapInfos.get(IFetchFactory.KEY_ELEMENT_TYPE).equals(IFetchFactory.ELEMENT_TYPE_FEATURE)) {
			generateFetchPluginsTarget();
			generateFetchRecusivelyTarget();
		}
		generateAdditionalTargets();
		generateEpilogue();
	}

	protected void generateFetchTarget() {
		//CONDITION Here we could check the values contained in the header of the file.
		// However it will require to have either generic value or to be sure that the value can be omitted
		// This would be necessary for the top level feature
		script.println();
		script.printTargetDeclaration(TARGET_FETCH, null, null, null, null);
		//don't do a fetch element for the generated container feature
		if (!mapInfos.get(IFetchFactory.KEY_ELEMENT_NAME).equals(IPDEBuildConstants.CONTAINER_FEATURE))
			script.printAntCallTask(TARGET_FETCH_ELEMENT, true, null);
		if (mapInfos.get(IFetchFactory.KEY_ELEMENT_TYPE).equals(IFetchFactory.ELEMENT_TYPE_FEATURE)) {
			script.printAntCallTask(TARGET_FETCH_PLUGINS, true, null);
			script.printAntCallTask(TARGET_FETCH_RECURSIVELY, true, null);
		}
		script.printTargetEnd();
	}

	protected void generateFetchElementTarget() {
		//don't try to fetch a generated container feature
		if (mapInfos.get(IFetchFactory.KEY_ELEMENT_NAME).equals(IPDEBuildConstants.CONTAINER_FEATURE))
			return;
		script.printTargetDeclaration(TARGET_FETCH_ELEMENT, null, FEATURE_ONLY, null, null);
		try {
			generateFetchEntry(element, elementVersion, false);
		} catch (CoreException e) {
			IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, WARNING_ELEMENT_NOT_FETCHED, NLS.bind(Messages.error_fetchingFailed, element), null);
			BundleHelper.getDefault().getLog().log(status);
		}
		script.printTargetEnd();
	}

	protected void generateFetchPluginsTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_FETCH_PLUGINS, null, FEATURE_AND_PLUGINS, null, null);
		retrieveFeature((String) mapInfos.get(IFetchFactory.KEY_ELEMENT_NAME), (String) mapInfos.get(IFetchFactory.KEY_ELEMENT_TYPE), mapInfos);
		generateChildrenFetchScript();
		script.printTargetEnd();
	}

	/**
	 * Decompose the elements constituting a Map file entry. The values are returned
	 * in a Map. <code>null</code> is returned if the entry does not exist.
	 * @param entry
	 * @return Map
	 * @throws CoreException
	 */
	private Map processMapFileEntry(String entry, Version version) throws CoreException {
		Map entryInfos = new HashMap(5);

		// extract type and element from entry
		int index = entry.indexOf('@');
		String type = entry.substring(0, index);
		String currentElement = entry.substring(index + 1);

		// read and validate the repository info for the entry
		Object[] match = getRepositoryInfo(entry, version);
		String repositoryInfo = match == null ? null : (String) match[0];
		if (repositoryInfo == null) {
			if (IPDEBuildConstants.CONTAINER_FEATURE.equals(currentElement)) {
				entryInfos.put(IFetchFactory.KEY_ELEMENT_TYPE, type);
				entryInfos.put(IFetchFactory.KEY_ELEMENT_NAME, currentElement);
				return entryInfos;
			}
			String message = NLS.bind(Messages.error_missingDirectoryEntry, Version.emptyVersion.equals(version) ? entry : entry + ',' + version.toString());
			BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
			return null;
		}

		// Get the repo identifier
		int idx = repositoryInfo.indexOf(',');
		if (idx == -1) {
			String message = NLS.bind(Messages.error_incorrectDirectoryEntry, currentElement);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
		}
		String repoIdentifier = repositoryInfo.substring(0, idx).trim();

		// Get the repo factory corresponding
		IFetchFactory fetchTaskFactory = null;
		String repoSpecificSegment = null;
		// if the type can not be found it is probably because it is an old style map file
		if (!fetchTaskFactories.getFactoryIds().contains(repoIdentifier)) {
			repoIdentifier = CVSFetchTaskFactory.ID;
			repoSpecificSegment = repositoryInfo;
		} else {
			repoSpecificSegment = repositoryInfo.substring(idx + 1, repositoryInfo.length()); //TODO Need to see if we can go out idx + 1
		}

		fetchTaskFactory = fetchTaskFactories.getFactory(repoIdentifier);
		if (fetchTaskFactory == null) {
			String message = NLS.bind(Messages.error_noCorrespondingFactory, currentElement);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
		}

		encounteredTypeOfRepo.add(fetchTaskFactory);
		// add general infos (will override builder specific infos)
		entryInfos.put(IFetchFactory.KEY_ELEMENT_TYPE, type);
		entryInfos.put(IFetchFactory.KEY_ELEMENT_NAME, currentElement);

		// add infos from registered builder
		fetchTaskFactory.parseMapFileEntry(repoSpecificSegment, getOverrideTags(repoIdentifier), entryInfos);

		// store builder
		entryInfos.put(FETCH_TASK_FACTORY, fetchTaskFactory);
		entryInfos.put(FETCH_TASK_FACTORY_ID, repoIdentifier);

		// keep track of the version of the element as found in the map file
		entryInfos.put(MATCHED_VERSION, match[1]);
		return entryInfos;
	}

	public Properties getOverrideTags(String repoIdentifier) {
		if (fetchOverrides != null && fetchOverrides.containsKey(repoIdentifier)) {
			Properties overrides = new Properties();
			overrides.putAll(fetchTags);
			overrides.putAll((Map) fetchOverrides.get(repoIdentifier));
			return overrides;
		}
		return fetchTags;
	}

	protected void generateFetchRecusivelyTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_FETCH_RECURSIVELY, null, FEATURES_RECURSIVELY, null, null);

		FeatureEntry[] compiledFeatures = feature.getIncludedFeatureReferences();
		for (int i = 0; i < compiledFeatures.length; i++) {
			String featureId = compiledFeatures[i].getId();
			if (featureProperties.containsKey(GENERATION_SOURCE_FEATURE_PREFIX + featureId)) {
				String[] extraElementsToFetch = Utils.getArrayFromString(featureProperties.getProperty(GENERATION_SOURCE_FEATURE_PREFIX + featureId), ","); //$NON-NLS-1$
				for (int j = 1; j < extraElementsToFetch.length; j++) {
					Map infos = Utils.parseExtraBundlesString(extraElementsToFetch[j], false);
					generateFetchEntry((String) infos.get(Utils.EXTRA_ID), (Version) infos.get(Utils.EXTRA_VERSION), false);
				}
				continue;
			}

			//Included features can be available in the baseLocation.
			if (getRepositoryInfo(IFetchFactory.ELEMENT_TYPE_FEATURE + '@' + featureId, new Version(compiledFeatures[i].getVersion())) != null)
				script.printAntTask(Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + FETCH_FILE_PREFIX + featureId + ".xml", null, TARGET_FETCH, null, null, null); //$NON-NLS-1$
			else if (getSite(false).findFeature(featureId, null, false) == null) {
				String message = NLS.bind(Messages.error_cannotFetchNorFindFeature, featureId);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}
		}
		script.printTargetEnd();
	}

	protected boolean generateFetchEntry(String entry, Version version, boolean manifestFileOnly) throws CoreException {
		Map mapFileEntry = mapInfos;
		if (!entry.equals(element)) {
			mapFileEntry = processMapFileEntry(entry, version);
			if (mapFileEntry == null)
				return false;
		}

		IFetchFactory factory = (IFetchFactory) mapFileEntry.get(FETCH_TASK_FACTORY);
		String elementToFetch = (String) mapFileEntry.get(IFetchFactory.KEY_ELEMENT_NAME);
		String type = (String) mapFileEntry.get(IFetchFactory.KEY_ELEMENT_TYPE);
		if (!manifestFileOnly)
			factory.generateRetrieveElementCall(mapFileEntry, computeFinalLocation(type, elementToFetch, (Version) mapFileEntry.get(MATCHED_VERSION)), script);
		else {
			String[] files;
			if (type.equals(IFetchFactory.ELEMENT_TYPE_FEATURE)) {
				files = new String[] {Constants.FEATURE_FILENAME_DESCRIPTOR};
			} else if (type.equals(IFetchFactory.ELEMENT_TYPE_PLUGIN)) {
				files = new String[] {Constants.PLUGIN_FILENAME_DESCRIPTOR, Constants.BUNDLE_FILENAME_DESCRIPTOR};
			} else if (type.equals(IFetchFactory.ELEMENT_TYPE_FRAGMENT)) {
				files = new String[] {Constants.FRAGMENT_FILENAME_DESCRIPTOR, Constants.BUNDLE_FILENAME_DESCRIPTOR};
			} else if (type.equals(IFetchFactory.ELEMENT_TYPE_BUNDLE)) {
				files = new String[] {Constants.BUNDLE_FILENAME_DESCRIPTOR};
			} else {
				files = new String[0];
			}
			factory.generateRetrieveFilesCall(mapFileEntry, computeFinalLocation(type, elementToFetch, (Version) mapFileEntry.get(MATCHED_VERSION)), files, script);
		}

		//key to use for version and source references properties files
		String key = null;
		if (version.getQualifier().endsWith(PROPERTY_QUALIFIER))
			key = QualifierReplacer.getQualifierKey(elementToFetch, version.toString());
		else
			key = elementToFetch + ',' + new Version(version.getMajor(), version.getMinor(), version.getMicro()).toString();
		//Keep track of the element that are being fetched. To simplify the lookup in the qualifier replacer, the versioned that was initially looked up is used as key in the file
		Properties tags = null;
		if (type.equals(IFetchFactory.ELEMENT_TYPE_FEATURE))
			tags = repositoryFeatureTags;
		else
			tags = repositoryPluginTags;
		if (mapFileEntry.get(IFetchFactory.KEY_ELEMENT_TAG) != null) {
			tags.put(key, mapFileEntry.get(IFetchFactory.KEY_ELEMENT_TAG));
		}

		if (!type.equals(IFetchFactory.ELEMENT_TYPE_FEATURE)) {
			String sourceURLs = (String) mapFileEntry.get(Constants.KEY_SOURCE_REFERENCES);
			if (sourceURLs != null)
				sourceReferences.put(key, sourceURLs);
		}
		return true;
	}

	/**
	 * Helper method to control for what locations a mkdir Ant task was already
	 * generated so we can reduce replication.
	 * 
	 * @param location
	 */
	protected void generateMkdirs(String location) {
		if (mkdirLocations.contains(location))
			return;
		mkdirLocations.add(location);
		script.printMkdirTask(location);
	}

	/**
	 * 
	 * @throws CoreException
	 */
	protected void generateChildrenFetchScript() throws CoreException {
		FeatureEntry[] allChildren = feature.getRawPluginEntries();
		FeatureEntry[] compiledChildren = feature.getPluginEntries();

		String elementId;
		for (int i = 0; i < allChildren.length; i++) {
			elementId = allChildren[i].getId();
			Version versionId = new Version(allChildren[i].getVersion());
			// We are not fetching the elements that are said to be generated, but we are fetching some elements that can be associated
			if (featureProperties.containsKey(GENERATION_SOURCE_PLUGIN_PREFIX + elementId)) {
				String[] extraElementsToFetch = Utils.getArrayFromString(featureProperties.getProperty(GENERATION_SOURCE_PLUGIN_PREFIX + elementId), ","); //$NON-NLS-1$
				for (int j = 1; j < extraElementsToFetch.length; j++) {
					Map infos = Utils.parseExtraBundlesString(extraElementsToFetch[j], false);
					generateFetchEntry((String) infos.get(Utils.EXTRA_ID), (Version) infos.get(Utils.EXTRA_VERSION), false);
				}
				continue;
			}

			boolean generated = true;
			if (allChildren[i].isFragment())
				generated = generateFetchEntry(IFetchFactory.ELEMENT_TYPE_FRAGMENT + '@' + elementId, versionId, !Utils.isIn(compiledChildren, allChildren[i]));
			else
				generated = generateFetchEntry(IFetchFactory.ELEMENT_TYPE_PLUGIN + '@' + elementId, versionId, !Utils.isIn(compiledChildren, allChildren[i]));
			if (generated == false)
				generateFetchEntry(IFetchFactory.ELEMENT_TYPE_BUNDLE + '@' + elementId, versionId, !Utils.isIn(compiledChildren, allChildren[i]));
		}

		elementId = feature.getLicenseFeature();
		if (elementId == null || elementId.length() == 0) {
			return;
		}

		String version = feature.getLicenseFeatureVersion();
		if (version == null)
			version = IPDEBuildConstants.GENERIC_VERSION_NUMBER;
		generateFetchEntry(IFetchFactory.ELEMENT_TYPE_FEATURE + '@' + elementId, new Version(version), false);
	}

	/**
	 * Return the feature object for the feature with the given info. Generate an Ant script
	 * which will retrieve the "feature.xml" file from CVS, and then call the feature object
	 * constructor from Update.
	 * 
	 * @param elementName the feature to retrieve
	 * @param elementType the element type
	 * @param elementInfos the element information
	 * @throws CoreException
	 */
	protected void retrieveFeature(String elementName, String elementType, Map elementInfos) throws CoreException {
		// Generate a temporary Ant script which retrieves the feature.xml for this
		// feature from CVS
		File root = new File(workingDirectory);

		//generated container feature should already exist on disk
		if (elementName.equals(IPDEBuildConstants.CONTAINER_FEATURE)) {
			BuildTimeFeatureFactory factory = BuildTimeFeatureFactory.getInstance();
			File featuresFolder = new File(root, DEFAULT_FEATURE_LOCATION);
			File featureLocation = new File(featuresFolder, elementName);
			try {
				feature = factory.createFeature(featureLocation.toURL(), null);
				featureProperties = new Properties();
				InputStream featureStream = new BufferedInputStream(new FileInputStream(new File(featureLocation, PROPERTIES_FILE)));
				featureProperties.load(featureStream);
				featureStream.close();
				return;
			} catch (Exception e) {
				String message = NLS.bind(Messages.exception_missingFeature, elementName);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, e));
			}
		}

		File target = new File(root, DEFAULT_RETRIEVE_FILENAME_DESCRIPTOR);
		IPath destination = new Path(root.getAbsolutePath()).append("tempFeature/"); //$NON-NLS-1$
		try {
			AntScript retrieve = new AntScript(new BufferedOutputStream(new FileOutputStream(target)));
			try {
				retrieve.printProjectDeclaration("RetrieveFeature", "main", "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				retrieve.printTargetDeclaration(TARGET_MAIN, null, null, null, null);

				String[] files = new String[] {Constants.FEATURE_FILENAME_DESCRIPTOR, PROPERTIES_FILE};
				String factoryId = (String) elementInfos.get(FETCH_TASK_FACTORY_ID);
				IFetchFactory factory = fetchTaskFactories.newFactory(factoryId);
				if (factory == null) {
					String message = NLS.bind(Messages.error_noCorrespondingFactory, elementName, factoryId);
					throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
				}
				factory.generateRetrieveFilesCall(elementInfos, destination, files, retrieve);

				retrieve.printTargetEnd();
				factory.addTargets(retrieve);
				retrieve.printProjectEnd();
			} finally {
				retrieve.close();
			}
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writeScript, target);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, message, e));
		}

		// Run the Ant script to go to and retrieve the feature.xml. Call the Update
		// code to construct the feature object to return.
		try {
			Map retrieveProp = new HashMap();
			retrieveProp.put("fetch.failonerror", "true"); //$NON-NLS-1$//$NON-NLS-2$
			retrieveProp.put("buildDirectory", getWorkingDirectory()); //$NON-NLS-1$
			if (fetchCache != null)
				retrieveProp.put(IBuildPropertiesConstants.PROPERTY_FETCH_CACHE, fetchCache);
			if (scriptRunner != null) {
				scriptRunner.runScript(target, TARGET_MAIN, retrieveProp);
			} else {
				AntRunner runner = new AntRunner();
				runner.setBuildFileLocation(target.getAbsolutePath());
				runner.addUserProperties(retrieveProp);
				//This has to be hardcoded here because of the way AntRunner stipulates that 
				//loggers are passed in. Otherwise this would be a Foo.class.getName()
				runner.addBuildLogger("org.eclipse.pde.internal.build.tasks.SimpleBuildLogger"); //$NON-NLS-1$

				runner.run();
			}
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, NLS.bind(Messages.error_retrieveFailed, elementName), e));
		}
		try {
			BuildTimeFeatureFactory factory = BuildTimeFeatureFactory.getInstance();
			File featureFolder = new File(destination.toString());
			feature = factory.createFeature(featureFolder.toURL(), null);

			//We only delete here, so if an exception is thrown the user can still see the retrieve.xml 
			target.delete();

			featureProperties = new Properties();
			InputStream featureStream = new BufferedInputStream(new FileInputStream(new File(featureFolder, PROPERTIES_FILE)));
			featureProperties.load(featureStream);
			featureStream.close();
			clear(featureFolder);
			if (feature == null) {
				String message = NLS.bind(Messages.exception_missingFeature, elementName);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}
		} catch (Exception e) {
			String message = NLS.bind(Messages.error_fetchingFeature, elementName);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, e));
		}
	}

	/**
	 * Deletes all the files and directories from the given root down (inclusive).
	 * Returns false if we could not delete some file or an exception occurred
	 * at any point in the deletion.
	 * Even if an exception occurs, a best effort is made to continue deleting.
	 * 
	 * @param root
	 * @return boolean
	 */
	public static boolean clear(File root) {
		boolean result = true;
		if (root.isDirectory()) {
			String[] list = root.list();
			// for some unknown reason, list() can return null.  
			// Just skip the children If it does.
			if (list != null)
				for (int i = 0; i < list.length; i++)
					result &= clear(new java.io.File(root, list[i]));
		}
		try {
			if (root.exists())
				result &= root.delete();
		} catch (Exception e) {
			// ignore any exceptions
			result = false;
		}
		return result;
	}

	protected IPath computeFinalLocation(String type, String elementName, Version version) {
		IPath location = new Path(Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY));
		if (type.equals(IFetchFactory.ELEMENT_TYPE_FEATURE))
			location = location.append(DEFAULT_FEATURE_LOCATION);
		else
			location = location.append(DEFAULT_PLUGIN_LOCATION);
		return location.append(elementName + (version.equals(Version.emptyVersion) ? "" : '_' + version.toString())); //$NON-NLS-1$
	}

	/**
	 * Get information stored in the directory file.
	 * 
	 * @param elementName
	 * @return String
	 * @throws CoreException
	 */
	//There are 3 cases described by the following "table"
	// what is being asked   -->   what should be returned (what is in the map file)
	//  1) id -->  id   (map: id, id@version)
	//  2) id + version -->  id@version (map: id@version,  id@version2)
	//  3) id --> highest version for the id (map: id@version1, id@version2)
	//  4) id + version --> id (map: id)
	// The first two cases are straight lookup cases
	// The third case is a "fallback case"
	// The fourth is the backward compatibility case.
	protected Object[] getRepositoryInfo(String elementName, Version version) throws CoreException {
		//TODO Need to see if the element name contains plugin, bundle, etc...
		if (directoryFile == null) {
			directoryFile = readProperties(directoryLocation, "", IStatus.ERROR); //$NON-NLS-1$
		}

		String result = null;
		Version matchedVersion = null;
		//Here we deal with the simple cases: the looked up element exists as is in the map (cases 1 and 2).
		if (Version.emptyVersion.equals(version)) {
			result = (String) directoryFile.get(elementName);
			matchedVersion = Version.emptyVersion;
		} else {
			result = (String) directoryFile.get(elementName + ',' + version.getMajor() + '.' + version.getMinor() + '.' + version.getMicro());
			matchedVersion = new Version(version.getMajor(), version.getMinor(), version.getMicro());
			if (result == null) {
				result = (String) directoryFile.get(elementName); //case 4
				matchedVersion = Version.emptyVersion;
				if (result != null && version.getQualifier().endsWith(IBuildPropertiesConstants.PROPERTY_QUALIFIER)) {
					String message = NLS.bind(Messages.warning_fallBackVersion, elementName + ',' + version.toString(), elementName);
					BundleHelper.getDefault().getLog().log(new Status(IStatus.WARNING, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
				}
			}
		}
		if (result != null)
			return new Object[] {result, matchedVersion};

		//Here we start dealing with the case #3.
		initializeSortedDirectory();
		//Among all the plug-ins, find all the ones for the given elementName
		SortedMap candidates = directory.subMap(new MapFileEntry(elementName, Version.emptyVersion), new MapFileEntry(elementName, versionMax));
		if (candidates.size() == 0)
			return null;

		Map.Entry bestMatch = null;
		for (Iterator iterator = candidates.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			MapFileEntry aCandidate = (MapFileEntry) entry.getKey();
			//Find the exact match
			if (aCandidate.v.equals(version))
				return new Object[] {(String) entry.getValue(), version};

			if (bestMatch != null) {
				if (((MapFileEntry) bestMatch.getKey()).v.compareTo(((MapFileEntry) entry.getKey()).v) < 1) {
					bestMatch = entry;
				}
			} else {
				bestMatch = entry;
			}
		}
		if (!Version.emptyVersion.equals(version)) //The request was for a particular version number and it has not been found
			return null;
		return new Object[] {(String) bestMatch.getValue(), ((MapFileEntry) bestMatch.getKey()).v};
	}

	private static final Version versionMax = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

	private void initializeSortedDirectory() {
		if (directory != null)
			return;
		directory = new TreeMap();
		for (Iterator iter = directoryFile.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			String[] entryInfo = Utils.getArrayFromString((String) entry.getKey());
			if (entryInfo.length == 0)
				continue;
			directory.put(new MapFileEntry(entryInfo[0], entryInfo.length == 2 ? new Version(entryInfo[1]) : Version.emptyVersion), entry.getValue());
		}
	}

	public static class MapFileEntry implements Comparable {
		String id;
		Version v;

		public MapFileEntry(String id, Version v) {
			this.id = id;
			this.v = v;
		}

		public int compareTo(Object o) {
			if (o instanceof MapFileEntry) {
				MapFileEntry entry = (MapFileEntry) o;
				int result = id.compareTo(entry.id);
				if (result != 0)
					return result;
				return v.compareTo(entry.v);
			}
			return -1;
		}

		public boolean equals(Object o) {
			if (o instanceof MapFileEntry) {
				MapFileEntry entry = (MapFileEntry) o;
				return id.equals(entry.id) && v.equals(entry.v);
			}
			return false;
		}

		public int hashCode() {
			return id.hashCode() + v.hashCode();
		}
	}

	/**
	 * Defines, the XML declaration and Ant project.
	 */
	protected void generatePrologue() {
		script.println();
		script.printComment("Fetch script for " + element); //$NON-NLS-1$
		script.println();
		script.printProjectDeclaration("FetchScript", TARGET_FETCH, null); //$NON-NLS-1$ 
		script.printProperty(PROPERTY_QUIET, "true"); //$NON-NLS-1$
	}

	/**
	 * Just ends the script.
	 */
	protected void generateEpilogue() {
		script.println();
		script.printProjectEnd();
	}

	/**
	 * Generates additional targets submitted by the fetch task factory.
	 */
	private void generateAdditionalTargets() {
		for (Iterator iter = encounteredTypeOfRepo.iterator(); iter.hasNext();) {
			((IFetchFactory) iter.next()).addTargets(script);
		}
	}

	/**
	 * Set the directory location to be the given value.
	 * 
	 * @param directoryLocation
	 */
	public void setDirectoryLocation(String directoryLocation) {
		this.directoryLocation = directoryLocation;
	}

	/**
	 * Sets whether children of the current element should be fetched.
	 * 
	 * @param fetchChildren
	 */
	public void setFetchChildren(boolean fetchChildren) {
		this.fetchChildren = fetchChildren;
	}

	/**
	 * Sets the CVS tag to use when fetching.  This overrides whatever is 
	 * in the directory database.  This is typically used when doing a nightly
	 * build by setting the tag to HEAD.
	 * 
	 * @param value a string CVS tag
	 */
	public void setFetchTag(Properties value) {
		fetchTags = value;
	}

	public void setFetchOverrides(Map value) {
		fetchOverrides = value;
	}

	public void setSourceReferences(Properties sourceReferences) {
		this.sourceReferences = sourceReferences;
	}

	/**
	 * Sets the CVS tag to use when fetching.  This overrides whatever is 
	 * in the directory database.  This is typically used when doing a nightly
	 * build by setting the tag to HEAD.
	 * 
	 * @param value a string CVS tag
	 */
	public void setFetchTagAsString(String value) {
		fetchOverrides = new HashMap();
		fetchTags = new Properties();

		String[] entries = Utils.getArrayFromString(value);
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] == null)
				continue;

			String[] elements = Utils.getArrayFromString(entries[i], ";"); //$NON-NLS-1$

			// REPO=tag;project=otherTag;project2=tag3
			if (elements.length > 0) {

				String repoElement = elements[0];
				int idx = repoElement.indexOf('=');

				String repoKey = (idx == -1) ? CVSFetchTaskFactory.OVERRIDE_TAG : repoElement.substring(0, idx);

				Properties overrides = null;
				if (fetchOverrides.containsKey(repoKey)) {
					overrides = (Properties) fetchOverrides.get(repoKey);
				} else {
					overrides = new Properties();
					fetchOverrides.put(repoKey, overrides);
				}

				fetchTags.setProperty(repoKey, repoElement.substring(idx + 1, repoElement.length()).trim());

				for (int j = 1; j < elements.length; j++) {
					String projectOverride = elements[j];
					idx = projectOverride.indexOf('=');
					if (idx != -1)
						overrides.setProperty(projectOverride.substring(0, idx), projectOverride.substring(idx + 1, projectOverride.length()).trim());
					else
						throw new IllegalArgumentException("FetchTag " + entries[i]); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Sets the CVS password file location to be the given value.
	 * 
	 * @param cvsPassFileLocation the CVS password file location
	 */
	public void setCvsPassFileLocation(String cvsPassFileLocation) {
		this.cvsPassFileLocation = cvsPassFileLocation;
	}

	public void setRecursiveGeneration(boolean recursiveGeneration) {
		this.recursiveGeneration = recursiveGeneration;
	}

	public void setScriptRunner(IScriptRunner runner) {
		this.scriptRunner = runner;
	}

	private void setDirectory(SortedMap dir) {
		directory = dir;
	}

	private void setDirectoryFile(Properties dir) {
		directoryFile = dir;
	}

	public void setFetchCache(String cache) {
		fetchCache = cache;
	}
}
