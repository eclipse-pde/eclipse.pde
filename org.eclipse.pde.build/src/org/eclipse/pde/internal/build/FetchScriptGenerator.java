/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.FeatureExecutableFactory;

/**
 * Generates Ant scripts which will use the CVS "fetch" element
 * to retrieve plug-ins and features from the CVS repository.
 * TODO  Add the support for the case of 0.0.0 plugin that are not into the config we are building
 */
public class FetchScriptGenerator extends AbstractScriptGenerator {
	// flag saying if we want to recursively generate the scripts	
	protected boolean recursiveGeneration = true;

	// Points to the map files containing references to cvs repository
	protected String directoryLocation;
	protected static Properties directory;

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

	public static final String FEATURE_ONLY = "featureOnly"; //$NON-NLS-1$
	public static final String FEATURE_AND_PLUGINS = "featureAndPlugins"; //$NON-NLS-1$
	public static final String FEATURES_RECURSIVELY = "featuresRecursively"; //$NON-NLS-1$
	public static final String FETCH_FILE_PREFIX = "fetch_"; //$NON-NLS-1$

	private String scriptName;

	public FetchScriptGenerator() {
		super();
	}

	public FetchScriptGenerator(String element) throws CoreException {
		setElement(element);
	}

	public void setElement(String element) throws CoreException {
		this.element = element;
	}

	/**
	 * @see AbstractScriptGenerator#generate()
	 */
	public void generate() throws CoreException {
		mapInfos = processMapFileEntry(element);
		if (mapInfos == null)
			return;

		scriptName = FETCH_FILE_PREFIX + mapInfos.get("element") + ".xml"; //$NON-NLS-1$ 	//$NON-NLS-2$
		openScript(workingDirectory, scriptName);
		try {
			generateFetchScript();
		} finally {
			closeScript();
		}

		if (recursiveGeneration && mapInfos.get("type").equals("feature")) //$NON-NLS-1$ 	//$NON-NLS-2$
			generateFetchFilesForIncludedFeatures();
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
		if (mapInfos.get("type").equals("feature")) { //$NON-NLS-1$ 	//$NON-NLS-2$
			generateFetchPluginsTarget();
			generateFetchRecusivelyTarget();
		}
		generateGetFromCVSTarget();
		generateEpilogue();
	}

	protected void generateFetchTarget() throws CoreException {
		//CONDITION Here we could check the values contained in the header of the file.
		// However it will require to have either generic value or to be sure that the value can be omitted
		// This would be necessary for the top level feature
		script.println();
		script.printTargetDeclaration(TARGET_FETCH, null, null, null, null);
		script.printAntCallTask(TARGET_FETCH_ELEMENT, null, null);
		if (mapInfos.get("type").equals("feature")) { //$NON-NLS-1$ 	//$NON-NLS-2$
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
			// FIXME: is this ok to ignore?
		}
		script.printTargetEnd();
	}

	protected void generateFetchPluginsTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_FETCH_PLUGINS, null, FEATURE_AND_PLUGINS, null, null);
		retrieveFeature((String) mapInfos.get("element"), (String) mapInfos.get("cvsRoot"), (String) mapInfos.get("tag"), (String) mapInfos.get("password"), (String) mapInfos.get("path")); //$NON-NLS-1$ 	//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-1$ //$NON-NLS-5$
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
			String message = Policy.bind("error.missingDirectoryEntry", entry); //$NON-NLS-1$
			Platform.getPlugin(PI_PDEBUILD).getLog().log(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
			return null;
		}

		String[] cvsFields = Utils.getArrayFromStringWithBlank(cvsInfo, ","); //$NON-NLS-1$
		if (cvsFields.length < 2) {
			String message = Policy.bind("error.incorrectDirectoryEntry", element); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, message, null));
		}

		entryInfos.put("tag", fetchTag.length() == 0 ? cvsFields[0] : fetchTag); //$NON-NLS-1$
		entryInfos.put("cvsRoot", cvsFields[1]); //$NON-NLS-1$
		entryInfos.put("password", (cvsFields.length > 2 && !cvsFields[2].equals("")) ? cvsFields[2] : null); //$NON-NLS-1$ //$NON-NLS-2$

		entryInfos.put("path", (cvsFields.length > 3 && !cvsFields[3].equals("")) ? cvsFields[3] : null); //$NON-NLS-1$ //$NON-NLS-2$

		int index = entry.indexOf('@'); //$NON-NLS-1$
		entryInfos.put("type", entry.substring(0, index)); //$NON-NLS-1$
		entryInfos.put("element", entry.substring(index + 1)); //$NON-NLS-1$
		return entryInfos;
	}

	protected void generateFetchRecusivelyTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_FETCH_RECURSIVELY, null, FEATURES_RECURSIVELY, null, null);

		IIncludedFeatureReference[] compiledFeatures = ((Feature) feature).getFeatureIncluded(); //TODO Check if the features can be selected on the config
		for (int i = 0; i < compiledFeatures.length; i++) {
			String featureId = compiledFeatures[i].getVersionedIdentifier().getIdentifier();
			if (featureProperties.containsKey(GENERATION_SOURCE_FEATURE_PREFIX + featureId)) {
				String[] extraElementsToFetch = Utils.getArrayFromString(featureProperties.getProperty(GENERATION_SOURCE_FEATURE_PREFIX + featureId), ","); //$NON-NLS-1$
				for (int j = 1; j < extraElementsToFetch.length; j++) {
					generateFetchEntry(extraElementsToFetch[j], false);
				}
				continue;
			}
			script.printAntTask("${buildDirectory}/" + FETCH_FILE_PREFIX + featureId + ".xml", null, TARGET_FETCH, null, null, null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		script.printTargetEnd();
	}

	protected void generateFetchEntry(String entry, boolean xmlFileOnly) throws CoreException {
		Map mapFileEntry = mapInfos;
		if (!entry.equals(element)) {
			mapFileEntry = processMapFileEntry(entry);
			if (mapFileEntry == null)
				return;
		}

		String password = (String) mapFileEntry.get("password"); //$NON-NLS-1$
		if (password != null)
			script.printCVSPassTask((String) mapFileEntry.get("cvsRoot"), password, cvsPassFileLocation); //$NON-NLS-1$

		String type = (String) mapFileEntry.get("type"); //$NON-NLS-1$
		String location = getElementLocation(type);
		Map params = new HashMap(5);

		//We directly export the CVS content into the correct directory 
		params.put("destination", mapFileEntry.get("element")); //$NON-NLS-1$ //$NON-NLS-2$
		params.put("tag", mapFileEntry.get("tag")); //$NON-NLS-1$ //$NON-NLS-2$
		params.put("cvsRoot", mapFileEntry.get("cvsRoot")); //$NON-NLS-1$ //$NON-NLS-2$
		params.put("quiet", "${quiet}"); //$NON-NLS-1$ //$NON-NLS-2$

		String cvsPackage = ((String) mapFileEntry.get("path") == null ? (String) mapFileEntry.get("element") : (String) mapFileEntry.get("path")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String fullLocation = null;
		if (type.equals("feature")) { //$NON-NLS-1$
			fullLocation = location + "/" + (String) mapFileEntry.get("element") + "/" + DEFAULT_FEATURE_FILENAME_DESCRIPTOR; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			params.put("fileToCheck", fullLocation); //$NON-NLS-1$
			cvsPackage += xmlFileOnly ? "/" + DEFAULT_FEATURE_FILENAME_DESCRIPTOR : ""; //$NON-NLS-1$ //$NON-NLS-2$
		} else if (type.equals("plugin")) { //$NON-NLS-1$
			fullLocation = location + "/" + (String) mapFileEntry.get("element") + "/" + DEFAULT_PLUGIN_FILENAME_DESCRIPTOR; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			params.put("fileToCheck", fullLocation); //$NON-NLS-1$
			cvsPackage += xmlFileOnly ? "/" + DEFAULT_PLUGIN_FILENAME_DESCRIPTOR : ""; //$NON-NLS-1$ //$NON-NLS-2$
		} else if (type.equals("fragment")) { //$NON-NLS-1$
			fullLocation = location + "/" + (String) mapFileEntry.get("element") + "/" + DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			params.put("fileToCheck", fullLocation); //$NON-NLS-1$
			cvsPackage += xmlFileOnly ? "/" + DEFAULT_FRAGMENT_FILENAME_DESCRIPTOR : ""; //$NON-NLS-1$ //$NON-NLS-2$
		}
		params.put("package", cvsPackage); //$NON-NLS-1$

		// This call create a new property for every feature, plugins or fragments that we must check the existence of 
		script.printAvailableTask(fullLocation, fullLocation);
		script.printAntTask("../" + scriptName, "${buildDirectory}/" + (type.equals("feature") ? DEFAULT_FEATURE_LOCATION : DEFAULT_PLUGIN_LOCATION), TARGET_GET_FROM_CVS, null, null, params); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	protected void generateGetFromCVSTarget() {
		script.printTargetDeclaration(TARGET_GET_FROM_CVS, null, null, "${fileToCheck}", null); //$NON-NLS-1$
		script.printCVSTask("export -d ${destination} -r ${tag} ${package}", "${cvsRoot}", null, null, null, "${quiet}", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		script.printTargetEnd();
	}

	/**
	 * Helper method to control for what locations a mkdir Ant task was already
	 * generated so we can reduce replication.
	 * 
	 * @param tab
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
	 * @param tab
	 * @param feature
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

			//TODO Improve the management of the flag: the flag should be set to false if the version number is not the generic number.
			if (allChildren[i].isFragment())
				generateFetchEntry("fragment@" + elementId, !Utils.isIn(compiledChildren, allChildren[i])); //$NON-NLS-1$
			else
				generateFetchEntry("plugin@" + elementId, !Utils.isIn(compiledChildren, allChildren[i])); //$NON-NLS-1$
		}
	}

	/**
	 * Return the feature object for the feature with the given info. Generate an Ant script
	 * which will retrieve the "feature.xml" file from CVS, and then call the feature object
	 * constructor from Update.
	 * 
	 * @param element the feature to retrieve
	 * @param cvsRoot the root in CVS
	 * @param tag the CVS tag
	 * @param password the CVS password
	 * @return Feature
	 * @throws CoreException
	 */
	protected void retrieveFeature(String elementName, String cvsRoot, String tag, String password, String path) throws CoreException {
		// Generate a temporary Ant script which retrieves the feature.xml for this
		// feature from CVS
		File root = new File(workingDirectory);
		File target = new File(root, DEFAULT_RETRIEVE_FILENAME_DESCRIPTOR); //$NON-NLS-1$
		try {
			AntScript retrieve = new AntScript(new FileOutputStream(target));
			try {
				retrieve.printProjectDeclaration("RetrieveFeature", "main", "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				retrieve.printTargetDeclaration(TARGET_MAIN, null, null, null, null); //$NON-NLS-1$

				IPath moduleFeatureFile;
				IPath moduleFeatureProperties;
				if (path != null) {
					moduleFeatureFile = new Path(path).append(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
					moduleFeatureProperties = new Path(path).append(PROPERTIES_FILE);
				} else {
					moduleFeatureFile = new Path(elementName).append(DEFAULT_FEATURE_FILENAME_DESCRIPTOR);
					moduleFeatureProperties = new Path(path).append(PROPERTIES_FILE);
				}

				if (password != null)
					retrieve.printCVSPassTask(cvsRoot, password, cvsPassFileLocation);

				retrieve.printCVSTask("export -r " + tag + " " + moduleFeatureFile.toString(), cvsRoot, null, null, null, "true", null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				retrieve.printCVSTask("export -r " + tag + " " + moduleFeatureProperties.toString(), cvsRoot, null, null, null, "true", null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

				//	retrieve.printCVSTask(0, null, cvsRoot, null, moduleFeatureFile.toString(), tag, "true", cvsPassFileLocation); //$NON-NLS-1$
				//retrieve.printCVSTask(0, null, cvsRoot, null, moduleFeatureProperties.toString(), tag, "true", cvsPassFileLocation ); //$NON-NLS-1$
				retrieve.printTargetEnd();
				retrieve.printProjectEnd();
			} finally {
				retrieve.close();
			}
		} catch (IOException e) {
			String message = Policy.bind("exception.writeScript"); //$NON-NLS-1$
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
			feature = (Feature) factory.createFeature(featureFolder.toURL(), null, null);

			//We only delete here, so if an exception is thrown the user can still see the retrieve.xml 
			target.delete();
			featureProperties = new Properties();
			InputStream featureStream = new FileInputStream(root + "/" + (path == null ? elementName : path) + "/" + PROPERTIES_FILE); //$NON-NLS-1$ //$NON-NLS-2$
			featureProperties.load(featureStream);
			featureStream.close();
			clear(featureFolder);
			if (feature == null) {
				String message = Policy.bind("exception.missingFeature", elementName); //$NON-NLS-1$
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, message, null));
			}
		} catch (Exception e) {
			String message = Policy.bind("error.fetchingFeature", elementName); //$NON-NLS-1$
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
		IPath location = new Path(getPropertyFormat("buildDirectory")); //$NON-NLS-1$
		if (type.equals("feature")) //$NON-NLS-1$
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
			directory = readProperties(directoryLocation, ""); //$NON-NLS-1$
		return directory.getProperty(elementName);
	}

	/**
	 * Defines, the XML declaration and Ant project.
	 */
	protected void generatePrologue() {
		script.println();
		script.printComment("Fetch script for " + element); //$NON-NLS-1$
		script.println();
		script.printProjectDeclaration("FetchScript", TARGET_FETCH, null); //$NON-NLS-1$ //$NON-NLS-2$
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

}