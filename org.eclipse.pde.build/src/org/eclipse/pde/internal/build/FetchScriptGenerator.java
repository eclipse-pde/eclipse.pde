/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.FeatureExecutableFactory;

/**
 * Generates Ant scripts which will use the CVS "fetch" element
 * to retrieve plug-ins and features from the CVS repository.
 */
public class FetchScriptGenerator extends AbstractScriptGenerator {
	private static final String BUNDLE = "bundle"; //$NON-NLS-1$
	private static final String FRAGMENT = "fragment"; //$NON-NLS-1$
	private static final String PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String FEATURE = "feature"; //$NON-NLS-1$
	private static final String ELEMENT = "element"; //$NON-NLS-1$
	private static final String TYPE = "type"; //$NON-NLS-1$
	private static final String PATH = "path"; //$NON-NLS-1$
	private static final String PASSWORD = "password"; //$NON-NLS-1$
	private static final String CVSROOT = "cvsRoot"; //$NON-NLS-1$
	private static final String TAG = "tag"; //$NON-NLS-1$

	// flag saying if we want to recursively generate the scripts	
	protected boolean recursiveGeneration = true;

	// Points to the map files containing references to cvs repository
	protected String directoryLocation;
	protected Properties directory;

	// The location of the CVS password file.
	protected String cvsPassFileLocation;

	protected boolean fetchChildren = true;

	protected String fetchTag = ""; //$NON-NLS-1$

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

	/**
	 * @see AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		mapInfos = processMapFileEntry(element);
		if (mapInfos == null) {
			IStatus warning = new Status(IStatus.WARNING, PI_PDEBUILD, WARNING_ELEMENT_NOT_FETCHED, NLS.bind(Messages.error_fetchingFailed, element), null);
			BundleHelper.getDefault().getLog().log(warning);
			return;
		}

		scriptName = FETCH_FILE_PREFIX + mapInfos.get(ELEMENT) + ".xml"; //$NON-NLS-1$
		openScript(workingDirectory, scriptName);
		try {
			generateFetchScript();
		} finally {
			closeScript();
		}

		if (recursiveGeneration && mapInfos.get(TYPE).equals(FEATURE)) 
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
			generator.setFetchTag(fetchTag);
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
		if (mapInfos.get(TYPE).equals(FEATURE)) { 
			generateFetchPluginsTarget();
			generateFetchRecusivelyTarget();
		}
		generateGetFromCVSTarget();
		generateEpilogue();
	}

	protected void generateFetchTarget() {
		//CONDITION Here we could check the values contained in the header of the file.
		// However it will require to have either generic value or to be sure that the value can be omitted
		// This would be necessary for the top level feature
		script.println();
		script.printTargetDeclaration(TARGET_FETCH, null, null, null, null);
		script.printAntCallTask(TARGET_FETCH_ELEMENT, null, null);
		if (mapInfos.get(TYPE).equals(FEATURE)) { 
			script.printAntCallTask(TARGET_FETCH_PLUGINS, null, null);
			script.printAntCallTask(TARGET_FETCH_RECURSIVELY, null, null);
		}
		script.printTargetEnd();
	}

	protected void generateFetchElementTarget() {
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
		retrieveFeature((String) mapInfos.get(ELEMENT), (String) mapInfos.get(CVSROOT), (String) mapInfos.get(TAG), (String) mapInfos.get(PASSWORD), (String) mapInfos.get(PATH)); 
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

		String cvsInfo = getCVSInfo(entry);
		if (cvsInfo == null) {
			String message = NLS.bind(Messages.error_missingDirectoryEntry, entry);
			BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
			return null;
		}

		String[] cvsFields = Utils.getArrayFromStringWithBlank(cvsInfo, ","); //$NON-NLS-1$
		if (cvsFields.length < 2) {
			String message = NLS.bind(Messages.error_incorrectDirectoryEntry, element);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
		}

		entryInfos.put(TAG, fetchTag.length() == 0 ? cvsFields[0] : fetchTag); 
		entryInfos.put(CVSROOT, cvsFields[1]); 
		entryInfos.put(PASSWORD, (cvsFields.length > 2 && !cvsFields[2].equals("")) ? cvsFields[2] : null); //$NON-NLS-1$ 

		entryInfos.put(PATH, (cvsFields.length > 3 && !cvsFields[3].equals("")) ? cvsFields[3] : null); //$NON-NLS-1$ 

		int index = entry.indexOf('@'); 
		entryInfos.put(TYPE, entry.substring(0, index)); 
		entryInfos.put(ELEMENT, entry.substring(index + 1)); 
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
			if (getCVSInfo(FEATURE + '@' + featureId) != null)
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

		String password = (String) mapFileEntry.get(PASSWORD);
		if (password != null)
			script.printCVSPassTask((String) mapFileEntry.get(CVSROOT), password, cvsPassFileLocation); 

		String type = (String) mapFileEntry.get(TYPE);
		String location = getElementLocation(type);
		Map params = new HashMap(5);

		//We directly export the CVS content into the correct directory 
		params.put("destination", mapFileEntry.get(ELEMENT)); //$NON-NLS-1$ 
		params.put(TAG, mapFileEntry.get(TAG)); 
		params.put(CVSROOT, mapFileEntry.get(CVSROOT)); 
		params.put("quiet", "${quiet}"); //$NON-NLS-1$ //$NON-NLS-2$

		String cvsPackage = ((String) mapFileEntry.get(PATH) == null ? (String) mapFileEntry.get(ELEMENT) : (String) mapFileEntry.get(PATH)); 
		String fullLocation = null;
		if (type.equals(FEATURE)) {
			fullLocation = location + '/' + (String) mapFileEntry.get(ELEMENT) + '/' + DEFAULT_FEATURE_FILENAME_DESCRIPTOR; 
			params.put("fileToCheck", fullLocation); //$NON-NLS-1$
			cvsPackage += manifestFileOnly ? '/' + DEFAULT_FEATURE_FILENAME_DESCRIPTOR : ""; //$NON-NLS-1$
			repositoryFeatureTags.put(mapFileEntry.get(ELEMENT), mapFileEntry.get(TAG));
		} else if (type.equals(PLUGIN)) {
			fullLocation = location + '/' + (String) mapFileEntry.get(ELEMENT) + '/' + DEFAULT_PLUGIN_FILENAME_DESCRIPTOR; 
			params.put("fileToCheck", fullLocation); //$NON-NLS-1$
			cvsPackage += manifestFileOnly ? '/' + DEFAULT_PLUGIN_FILENAME_DESCRIPTOR : ""; //$NON-NLS-1$
			repositoryPluginTags.put(mapFileEntry.get(ELEMENT), mapFileEntry.get(TAG));
		} else if (type.equals(FRAGMENT)) {
			fullLocation = location + '/' + (String) mapFileEntry.get(ELEMENT) + '/' + DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR; 
			params.put("fileToCheck", fullLocation); //$NON-NLS-1$
			cvsPackage += manifestFileOnly ? '/' + DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR : ""; //$NON-NLS-1$
			repositoryPluginTags.put(mapFileEntry.get(ELEMENT), mapFileEntry.get(TAG));
		} else if (type.equals(BUNDLE)) {
			fullLocation = location + '/' + (String) mapFileEntry.get(ELEMENT) + '/' + DEFAULT_BUNDLE_FILENAME_DESCRIPTOR;
			params.put("fileToCheck", fullLocation); //$NON-NLS-1$
			cvsPackage += manifestFileOnly ? '/' + DEFAULT_BUNDLE_FILENAME_DESCRIPTOR : "";//$NON-NLS-1$ 
			repositoryPluginTags.put(mapFileEntry.get(ELEMENT), mapFileEntry.get(TAG));   
		}
		params.put("package", cvsPackage); //$NON-NLS-1$

		// This call create a new property for every feature, plugins or fragments that we must check the existence of 
		script.printAvailableTask(fullLocation, fullLocation);
		if (type.equals(PLUGIN) || type.equals(FRAGMENT)) {
			script.printAvailableTask(fullLocation, location + '/' + (String) mapFileEntry.get(ELEMENT) + '/' + DEFAULT_BUNDLE_FILENAME_DESCRIPTOR);
		}
		script.printAntTask("../" + scriptName, Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY) + '/' + (type.equals(FEATURE) ? DEFAULT_FEATURE_LOCATION : DEFAULT_PLUGIN_LOCATION), TARGET_GET_FROM_CVS, null, null, params); //$NON-NLS-1$ 
		return true;
	}

	protected void generateGetFromCVSTarget() {
		script.printTargetDeclaration(TARGET_GET_FROM_CVS, null, null, "${fileToCheck}", "{destination}"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printCVSTask("export -d ${destination} -r ${tag} ${package}", "${cvsRoot}", null, null, null, "${quiet}", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printTargetEnd();
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
				generated = generateFetchEntry(FRAGMENT + '@' + elementId, !Utils.isIn(compiledChildren, allChildren[i])); 
			else
				generated = generateFetchEntry(PLUGIN + '@' + elementId, !Utils.isIn(compiledChildren, allChildren[i])); 
			if (generated == false)
				generateFetchEntry(BUNDLE + '@' + elementId, !Utils.isIn(compiledChildren, allChildren[i])); 
		}
	}

	/**
	 * Return the feature object for the feature with the given info. Generate an Ant script
	 * which will retrieve the "feature.xml" file from CVS, and then call the feature object
	 * constructor from Update.
	 * 
	 * @param elementName the feature to retrieve
	 * @param cvsRoot the root in CVS
	 * @param tag the CVS tag
	 * @param password the CVS password
	 * @throws CoreException
	 */
	protected void retrieveFeature(String elementName, String cvsRoot, String tag, String password, String path) throws CoreException {
		// Generate a temporary Ant script which retrieves the feature.xml for this
		// feature from CVS
		File root = new File(workingDirectory);
		File target = new File(root, DEFAULT_RETRIEVE_FILENAME_DESCRIPTOR); 
		try {
			AntScript retrieve = new AntScript(new FileOutputStream(target));
			try {
				retrieve.printProjectDeclaration("RetrieveFeature", "main", "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				retrieve.printTargetDeclaration(TARGET_MAIN, null, null, null, null); 

				IPath moduleFeatureFile;
				IPath moduleFeatureProperties;
				if (path != null) {
					moduleFeatureFile = new Path(path).append(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
					moduleFeatureProperties = new Path(path).append(PROPERTIES_FILE);
				} else {
					moduleFeatureFile = new Path(elementName).append(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
					moduleFeatureProperties = new Path(elementName).append(PROPERTIES_FILE);
				}

				if (password != null)
					retrieve.printCVSPassTask(cvsRoot, password, cvsPassFileLocation);

				retrieve.printCVSTask("export -r " + tag + " " + moduleFeatureFile.toString(), cvsRoot, null, null, null, "true", null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				retrieve.printCVSTask("export -r " + tag + " " + moduleFeatureProperties.toString(), cvsRoot, null, null, null, "true", null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

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
			File featureFolder = new File(root, (path == null ? elementName : path));
			feature = factory.createFeature(featureFolder.toURL(), null, null);

			//We only delete here, so if an exception is thrown the user can still see the retrieve.xml 
			target.delete();
			featureProperties = new Properties();
			InputStream featureStream = new FileInputStream(new File(root, (path == null ? elementName : path) + '/' + PROPERTIES_FILE));
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

	/**
	 * 
	 * @param type
	 * @return String
	 */
	protected String getElementLocation(String type) {
		IPath location = new Path(Utils.getPropertyFormat(PROPERTY_BUILD_DIRECTORY));
		if (type.equals(FEATURE)) 
			location = location.append(DEFAULT_FEATURE_LOCATION);
		else
			location = location.append(DEFAULT_PLUGIN_LOCATION);
		return location.toString();
	}

	/**
	 * Get information stored in the directory file.
	 * 
	 * @param elementName
	 * @return String
	 * @throws CoreException
	 */
	protected String getCVSInfo(String elementName) throws CoreException {
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
	public void setFetchTag(String value) {
		fetchTag = value;
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
