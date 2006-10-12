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
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.build.Constants;
import org.eclipse.pde.build.IFetchFactory;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.pde.internal.build.fetch.CVSFetchTaskFactory;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.FeatureExecutableFactory;

/**
 * Generates Ant scripts with a repository specific factory
 * to retrieve plug-ins and features from a repository.
 */
public class FetchScriptGenerator extends AbstractScriptGenerator {
	private static final String FETCH_TASK_FACTORY = "internal.factory"; //$NON-NLS-1$

	// flag saying if we want to recursively generate the scripts	
	protected boolean recursiveGeneration = true;

	// Points to the map files containing references to repository
	protected String directoryLocation;
	protected Properties directory;

	// The location of the CVS password file.
	protected String cvsPassFileLocation;

	protected boolean fetchChildren = true;

	protected Properties fetchTags;

	// The element (an entry of the map file) for which we create the script 
	protected String element;
	// The feature object representing the element
	protected IFeature feature;
	//The map infos (of the element) processed
	protected Map mapInfos;
	// The content of the build.properties file associated with the feature
	protected Properties featureProperties;
	// Variables to control is a mkdir to a specific folder was already.
	protected List mkdirLocations = new ArrayList(5);
	// A property table containing the association between the plugins and the version from the map  
	protected Properties repositoryPluginTags = new Properties();
	protected Properties repositoryFeatureTags = new Properties();

	//The registry of the task factories
	private FetchTaskFactoriesRegistry fetchTaskFactories;
	//Set of all the used factories while generating the fetch script for the top level element
	private Set encounteredTypeOfRepo = new HashSet();

	public static final String FEATURE_ONLY = "featureOnly"; //$NON-NLS-1$
	public static final String FEATURE_AND_PLUGINS = "featureAndPlugins"; //$NON-NLS-1$
	public static final String FEATURES_RECURSIVELY = "featuresRecursively"; //$NON-NLS-1$
	public static final String FETCH_FILE_PREFIX = "fetch_"; //$NON-NLS-1$

	private String scriptName;

	public FetchScriptGenerator() {
		super();
	}

	public FetchScriptGenerator(String element) {
		setElement(element);
	}

	public void setElement(String element) {
		this.element = element;
	}

	private void initializeFactories() {
		fetchTaskFactories = new FetchTaskFactoriesRegistry();
	}
	
	/**
	 * @see AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		initializeFactories();
		mapInfos = processMapFileEntry(element);
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

	private void saveRepositoryTags() throws CoreException {
		saveRepositoryTags(repositoryPluginTags, DEFAULT_PLUGIN_REPOTAG_FILENAME_DESCRIPTOR);
		saveRepositoryTags(repositoryFeatureTags, DEFAULT_FEATURE_REPOTAG_FILENAME_DESCRIPTOR);
	}

	/**
	 * Method generateFetchFilesForRequiredFeatures.
	 */
	private void generateFetchFilesForIncludedFeatures() throws CoreException {
		IIncludedFeatureReference[] referencedFeatures = ((Feature) feature).getFeatureIncluded();
		for (int i = 0; i < referencedFeatures.length; i++) {
			String featureId = referencedFeatures[i].getVersionedIdentifier().getIdentifier();
			if (featureProperties.containsKey(GENERATION_SOURCE_FEATURE_PREFIX + featureId))
				continue;

			FetchScriptGenerator generator = new FetchScriptGenerator("feature@" + featureId); //$NON-NLS-1$
			generator.setDirectoryLocation(directoryLocation);
			generator.setFetchChildren(fetchChildren);
			generator.setCvsPassFileLocation(cvsPassFileLocation);
			generator.setRecursiveGeneration(recursiveGeneration);
			generator.setFetchTag(fetchTags);
			generator.setDirectory(directory);
			generator.setBuildSiteFactory(siteFactory);
			generator.repositoryPluginTags = repositoryPluginTags;
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
		if(!mapInfos.get(IFetchFactory.KEY_ELEMENT_NAME).equals(IPDEBuildConstants.CONTAINER_FEATURE))
			script.printAntCallTask(TARGET_FETCH_ELEMENT, true, null);
		if (mapInfos.get(IFetchFactory.KEY_ELEMENT_TYPE).equals(IFetchFactory.ELEMENT_TYPE_FEATURE)) {
			script.printAntCallTask(TARGET_FETCH_PLUGINS, true, null);
			script.printAntCallTask(TARGET_FETCH_RECURSIVELY, true, null);
		}
		script.printTargetEnd();
	}

	protected void generateFetchElementTarget() {
		//don't try to fetch a generated container feature
		if(mapInfos.get(IFetchFactory.KEY_ELEMENT_NAME).equals(IPDEBuildConstants.CONTAINER_FEATURE))
			return;
		script.printTargetDeclaration(TARGET_FETCH_ELEMENT, null, FEATURE_ONLY, null, null);
		try {
			generateFetchEntry(element, false);
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
	private Map processMapFileEntry(String entry) throws CoreException {
		Map entryInfos = new HashMap(5);

		// extract type and element from entry
		int index = entry.indexOf('@');
		String type = entry.substring(0, index);
		String currentElement = entry.substring(index + 1);

		// read and validate the repository info for the entry
		String repositoryInfo = getRepositoryInfo(entry);
		if (repositoryInfo == null) {
			if(IPDEBuildConstants.CONTAINER_FEATURE.equals(currentElement)){
				entryInfos.put(IFetchFactory.KEY_ELEMENT_TYPE, type);
				entryInfos.put(IFetchFactory.KEY_ELEMENT_NAME, currentElement);
				return entryInfos;
			}			
			String message = NLS.bind(Messages.error_missingDirectoryEntry, entry);
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
		if (! fetchTaskFactories.getFactoryIds().contains(repoIdentifier)) {
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
		fetchTaskFactory.parseMapFileEntry(repoSpecificSegment, fetchTags, entryInfos);

		// store builder
		entryInfos.put(FETCH_TASK_FACTORY, fetchTaskFactory);

		return entryInfos;
	}

	protected void generateFetchRecusivelyTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_FETCH_RECURSIVELY, null, FEATURES_RECURSIVELY, null, null);

		IIncludedFeatureReference[] compiledFeatures = ((Feature) feature).getFeatureIncluded();
		for (int i = 0; i < compiledFeatures.length; i++) {
			String featureId = compiledFeatures[i].getVersionedIdentifier().getIdentifier();
			if (featureProperties.containsKey(GENERATION_SOURCE_FEATURE_PREFIX + featureId)) {
				String[] extraElementsToFetch = Utils.getArrayFromString(featureProperties.getProperty(GENERATION_SOURCE_FEATURE_PREFIX + featureId), ","); //$NON-NLS-1$
				for (int j = 1; j < extraElementsToFetch.length; j++) {
					String [] elements = Utils.getArrayFromString(extraElementsToFetch[j], ";"); //$NON-NLS-1$
					generateFetchEntry(elements[0], false);
				}
				continue;
			}

			//Included features can be available in the baseLocation.
			if (getRepositoryInfo(IFetchFactory.ELEMENT_TYPE_FEATURE + '@' + featureId) != null)
				script.printAntTask(Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + FETCH_FILE_PREFIX + featureId + ".xml", null, TARGET_FETCH, null, null, null); //$NON-NLS-1$
			else if (getSite(false).findFeature(featureId, null, false) == null) {
				String message = NLS.bind(Messages.error_cannotFetchNorFindFeature, featureId);
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}
		}
		script.printTargetEnd();
	}

	protected boolean generateFetchEntry(String entry, boolean manifestFileOnly) throws CoreException {
		Map mapFileEntry = mapInfos;
		if (!entry.equals(element)) {
			mapFileEntry = processMapFileEntry(entry);
			if (mapFileEntry == null)
				return false;
		}

		IFetchFactory factory = (IFetchFactory) mapFileEntry.get(FETCH_TASK_FACTORY);
		String elementToFetch = (String) mapFileEntry.get(IFetchFactory.KEY_ELEMENT_NAME);
		String type = (String) mapFileEntry.get(IFetchFactory.KEY_ELEMENT_TYPE);
		if (! manifestFileOnly)
			factory.generateRetrieveElementCall(mapFileEntry, computeFinalLocation(type, elementToFetch), script);
		else {
			String[] files;
			if (type.equals(IFetchFactory.ELEMENT_TYPE_FEATURE)) {
				files = new String[] { Constants.FEATURE_FILENAME_DESCRIPTOR };
			} else if (type.equals(IFetchFactory.ELEMENT_TYPE_PLUGIN)) {
				files = new String[] { Constants.PLUGIN_FILENAME_DESCRIPTOR, Constants.BUNDLE_FILENAME_DESCRIPTOR };
			} else if (type.equals(IFetchFactory.ELEMENT_TYPE_FRAGMENT)) {
				files = new String[] { Constants.FRAGMENT_FILENAME_DESCRIPTOR, Constants.BUNDLE_FILENAME_DESCRIPTOR };
			} else if (type.equals(IFetchFactory.ELEMENT_TYPE_BUNDLE)) {
				files = new String[] { Constants.BUNDLE_FILENAME_DESCRIPTOR }; 
			} else {
				files = new String[0];
			}
			factory.generateRetrieveFilesCall(mapFileEntry, computeFinalLocation(type, elementToFetch), files, script);
		}
		
		//Keep track of the element that are being fetched
		Properties tags = null;
		if (type.equals(IFetchFactory.ELEMENT_TYPE_FEATURE))
			tags = repositoryFeatureTags;
		else 
			tags = repositoryPluginTags;
		if (mapFileEntry.get(IFetchFactory.KEY_ELEMENT_TAG) != null)
			tags.put(elementToFetch, mapFileEntry.get(IFetchFactory.KEY_ELEMENT_TAG));
		
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
		IPluginEntry[] allChildren = feature.getRawPluginEntries();
		IPluginEntry[] compiledChildren = feature.getPluginEntries();

		for (int i = 0; i < allChildren.length; i++) {
			String elementId = allChildren[i].getVersionedIdentifier().getIdentifier();
			// We are not fetching the elements that are said to be generated, but we are fetching some elements that can be associated
			if (featureProperties.containsKey(GENERATION_SOURCE_PLUGIN_PREFIX + elementId)) {
				String[] extraElementsToFetch = Utils.getArrayFromString(featureProperties.getProperty(GENERATION_SOURCE_PLUGIN_PREFIX + elementId), ","); //$NON-NLS-1$
				for (int j = 1; j < extraElementsToFetch.length; j++) {
					generateFetchEntry(extraElementsToFetch[j], false);
				}
				continue;
			}

			boolean generated = true;
			if (allChildren[i].isFragment())
				generated = generateFetchEntry(IFetchFactory.ELEMENT_TYPE_FRAGMENT + '@' + elementId, !Utils.isIn(compiledChildren, allChildren[i]));
			else
				generated = generateFetchEntry(IFetchFactory.ELEMENT_TYPE_PLUGIN + '@' + elementId, !Utils.isIn(compiledChildren, allChildren[i]));
			if (generated == false)
				generateFetchEntry(IFetchFactory.ELEMENT_TYPE_BUNDLE + '@' + elementId, !Utils.isIn(compiledChildren, allChildren[i]));
		}
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
		if(elementName.equals(IPDEBuildConstants.CONTAINER_FEATURE)) {
			FeatureExecutableFactory factory = new FeatureExecutableFactory();
			File featuresFolder = new File(root, DEFAULT_FEATURE_LOCATION);
			File featureLocation = new File(featuresFolder, elementName);
			try {
				feature = factory.createFeature(featureLocation.toURL(), null, null);
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
				IFetchFactory factory = (IFetchFactory) elementInfos.get(FETCH_TASK_FACTORY);
				factory.generateRetrieveFilesCall(elementInfos, destination, files, retrieve);

				retrieve.printTargetEnd();
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
			AntRunner runner = new AntRunner();
			runner.setBuildFileLocation(target.getAbsolutePath());
			runner.run();
			FeatureExecutableFactory factory = new FeatureExecutableFactory();
			File featureFolder = new File(destination.toString());
			feature = factory.createFeature(featureFolder.toURL(), null, null);

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

	protected IPath computeFinalLocation(String type, String elementName) {
		IPath location = new Path(Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY));
		if (type.equals(IFetchFactory.ELEMENT_TYPE_FEATURE))
			location = location.append(DEFAULT_FEATURE_LOCATION);
		else
			location = location.append(DEFAULT_PLUGIN_LOCATION);
		return location.append(elementName);
	}

	/**
	 * Get information stored in the directory file.
	 * 
	 * @param elementName
	 * @return String
	 * @throws CoreException
	 */
	protected String getRepositoryInfo(String elementName) throws CoreException {
		if (directory == null)
			directory = readProperties(directoryLocation, "", IStatus.ERROR); //$NON-NLS-1$
		return directory.getProperty(elementName);
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

	/**
	 * Sets the CVS tag to use when fetching.  This overrides whatever is 
	 * in the directory database.  This is typically used when doing a nightly
	 * build by setting the tag to HEAD.
	 * 
	 * @param value a string CVS tag
	 */
	public void setFetchTagAsString(String value) {
		fetchTags = new Properties();
		String[] entries = Utils.getArrayFromString(value);
		//Backward compatibility
		if (entries.length == 1 && (entries[0].indexOf('=') == -1)) {
			fetchTags.put(CVSFetchTaskFactory.OVERRIDE_TAG, entries[0]);
			return;
		}
		for (int i = 0; i < entries.length; i++) {
			String[] valueForRepo = Utils.getArrayFromString(entries[i], "="); //$NON-NLS-1$
			if (valueForRepo == null || valueForRepo.length != 2)
				throw new IllegalArgumentException("FetchTag " + entries[i]); //$NON-NLS-1$
			fetchTags.put(valueForRepo[0], valueForRepo[1]);
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

	public void setDirectory(Properties dir) {
		directory = dir;
	}
}
