/**********************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
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
import org.eclipse.update.core.Feature;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.internal.core.FeatureExecutableFactory;

/**
 * Generates Ant scripts which will use the CVS "fetch" element
 * to retrieve plug-ins and features from the CVS repository.
 */
public class FetchScriptGenerator extends AbstractScriptGenerator implements IPDEBuildConstants {

	// The resulting script that we are generating.
	protected AntScript script;

	protected boolean fetchChildren = true;

	protected String element;

	protected String installLocation;

	protected String directoryLocation;

	// The location of the CVS password file.
	protected String cvsPassFileLocation;

	// The default name for the fetch script
	protected String scriptName = DEFAULT_FETCH_SCRIPT_FILENAME;

	protected Properties directory;

	// Variables to control is a mkdir to a specific folder was already.
	protected List mkdirLocations = new ArrayList(5);

/**
 * @see AbstractScriptGenerator#generate()
 */
public void generate() throws CoreException {
	try {
		File root = new File(installLocation);
		File target = new File(scriptName);
		// if scriptName is not absolute, make it relative to the installLocation
		if (!target.isAbsolute())
			target = new File(root, scriptName);
		script = new AntScript(new FileOutputStream(target));
		try {
			generateFetchScript();
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e)); //$NON-NLS-1$
	}
}

/**
 * Main call for generating the script.
 *  * @throws CoreException */
protected void generateFetchScript() throws CoreException {
	generatePrologue();
	generateFetchTarget();
	generateEpilogue();
}

/**
 *  * @throws CoreException */
protected void generateFetchTarget() throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_FETCH, null, null, null, null);
	generateFetchEntry(++tab, element);
	script.printTargetEnd(--tab);
}

/**
 *  * @param tab * @param entry * @throws CoreException */
protected void generateFetchEntry(int tab, String entry) throws CoreException {
	String cvsInfo = getCVSInfo(entry);
	if (cvsInfo == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, Policy.bind("error.missingDirectoryEntry", entry), null)); //$NON-NLS-1$

	int index = entry.indexOf('@');
	String type = entry.substring(0, index);
	String element = entry.substring(index + 1);
	String location = getElementLocation(type);
	String[] cvsFields = Utils.getArrayFromString(cvsInfo);
	if (cvsFields.length < 2)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ENTRY_MISSING, Policy.bind("error.incorrectDirectoryEntry", entry), null)); //$NON-NLS-1$
		
	String tag = cvsFields[0];
	String cvsRoot = cvsFields[1];
	String password = (cvsFields.length > 2) ? cvsFields[2] : null;

	if (password != null)
		script.printCVSPassTask(tab, cvsRoot, password, cvsPassFileLocation);

	generateMkdirs(tab, location);
	script.printCVSTask(tab, null, cvsRoot, location, element, tag, getPropertyFormat(PROPERTY_QUIET), cvsPassFileLocation);

	if (fetchChildren && type.equals("feature")) { //$NON-NLS-1$
		Feature feature = retrieveFeature(element, cvsRoot, tag, password);
		generateChildrenFetchScript(tab, feature);
	}
}

/**
 * Helper method to control for what locations a mkdir Ant task was already
 * generated so we can reduce replication.
 *  * @param tab * @param location */
protected void generateMkdirs(int tab, String location) {
	if (mkdirLocations.contains(location))
		return;
	mkdirLocations.add(location);
	script.printMkdirTask(tab, location);
}

/**
 *  * @param tab * @param feature * @throws CoreException */
protected void generateChildrenFetchScript(int tab, Feature feature) throws CoreException {
	IPluginEntry[] children = feature.getPluginEntries();
	for (int i = 0; i < children.length; i++) {
		String elementId = children[i].getVersionedIdentifier().getIdentifier();
		if (children[i].isFragment())
			generateFetchEntry(tab, "fragment@" + elementId); //$NON-NLS-1$
		else
			generateFetchEntry(tab, "plugin@" + elementId); //$NON-NLS-1$
	}
}

/**
 * Return the feature object for the feature with the given info. Generate an Ant script
 * which will retrieve the "feature.xml" file from CVS, and then call the feature object
 * constructor from Update.
 *  * @param element the feature to retrieve * @param cvsRoot the root in CVS * @param tag the CVS tag * @param password the CVS password * @return Feature * @throws CoreException */
protected Feature retrieveFeature(String element, String cvsRoot, String tag, String password) throws CoreException {
	
	// Generate a temporary Ant script which retrieves the feature.xml for this
	// feature from CVS
	File root = new File(installLocation);
	File target = new File(root, "retrieve.xml"); //$NON-NLS-1$
	try {
		AntScript retrieve = new AntScript(new FileOutputStream(target));
		try {
			retrieve.printProjectDeclaration("RetrieveFeature", "main", "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			retrieve.printTargetDeclaration(0, TARGET_MAIN, null, null, null, null); //$NON-NLS-1$
			IPath module = new Path(element).append("feature.xml"); //$NON-NLS-1$
			if (password != null)
				retrieve.printCVSPassTask(0, cvsRoot, password, cvsPassFileLocation);
			retrieve.printCVSTask(0, null, cvsRoot, null, module.toString(), tag, "true", cvsPassFileLocation); //$NON-NLS-1$
			retrieve.printTargetEnd(0);
			retrieve.printProjectEnd();
		} finally {
			retrieve.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e)); //$NON-NLS-1$
	}
	
	// Run the Ant script to go to CVS and retrieve the feature.xml. Call the Update
	// code to construct the feature object to return.
	try {
		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(target.getAbsolutePath());
		runner.run();
		target.delete();
		FeatureExecutableFactory factory = new FeatureExecutableFactory();
		File featureFolder = new File(root, element);
		Feature feature = (Feature) factory.createFeature(featureFolder.toURL(), null);
		clear(featureFolder);
		if (feature == null)
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("exception.missingFeature", new String[] {element}), null)); //$NON-NLS-1$
		return feature;
	} catch (Exception e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {element}), e)); //$NON-NLS-1$
	}
}

/**
 * Deletes all the files and directories from the given root down (inclusive).
 * Returns false if we could not delete some file or an exception occurred
 * at any point in the deletion.
 * Even if an exception occurs, a best effort is made to continue deleting.
 *  * @param root * @return boolean */
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
 *  * @param type * @return String */
protected String getElementLocation(String type) {
	IPath location = new Path(getPropertyFormat(PROPERTY_INSTALL));
	if (type.equals("feature")) //$NON-NLS-1$
		location = location.append(DEFAULT_FEATURE_LOCATION);
	else
		location = location.append(DEFAULT_PLUGIN_LOCATION);
	return location.toString();
}

/**
 * Get information stored in the directory file.
 *  * @param element * @return String * @throws CoreException */
protected String getCVSInfo(String element) throws CoreException {
	if (directory == null)
		readDirectory();
	return directory.getProperty(element);
}

/**
 * Reads directory file at the directoryLocation.
 *  * @throws CoreException if there is an IOException when reading the file */
protected void readDirectory() throws CoreException {
	try {
		directory = new Properties();
		File file = new File(directoryLocation);
		InputStream is = new FileInputStream(file);
		try {
			directory.load(is);
		} finally {
			is.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_READ_DIRECTORY, Policy.bind("error.readingDirectory"), e)); //$NON-NLS-1$
	}
}

/**
 * Defines, the XML declaration and Ant project.
 */
protected void generatePrologue() {
	script.println();
	script.printComment(0, "Fetch script for " + element); //$NON-NLS-1$
	script.println();
	script.printProjectDeclaration("FetchScript", TARGET_FETCH, "."); //$NON-NLS-1$ //$NON-NLS-2$
	script.printProperty(1,PROPERTY_QUIET, "true"); //$NON-NLS-1$
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
 *  * @param directoryLocation */
public void setDirectoryLocation(String directoryLocation) {
	this.directoryLocation = directoryLocation;
}

/**
 * Sets the element to generate fetch script from.
 *  * @param element */
public void setElement(String element) {
	this.element = element;
}

/**
 * Sets whether children of the current element should be fetched.
 *  * @param fetchChildren */
public void setFetchChildren(boolean fetchChildren) {
	this.fetchChildren = fetchChildren;
}

/**
 * Sets the install location to be the given value.
 *  * @param installLocation */
public void setInstallLocation(String installLocation) {
	this.installLocation = installLocation;
}

/**
 * Sets the CVS password file location to be the given value.
 *  * @param cvsPassFileLocation the CVS password file location */
public void setCvsPassFileLocation(String cvsPassFileLocation) {
	this.cvsPassFileLocation = cvsPassFileLocation;
}

/**
 * Sets the script name to be the given value. If <code>null</code> is
 * passed as the argument, then <code>IPDEBuildConstants.DEFAULT_FETCH_SCRIPT_FILENAME</code>
 * is used.
 *  * @param scriptName the name of the script or <code>null</code>
 * @see IPDEBuildConstants.DEFAULT_FETCH_SCRIPT_FILENAME */
public void setScriptName(String scriptName) {
	if (scriptName == null)
		this.scriptName = DEFAULT_FETCH_SCRIPT_FILENAME;
	else
		this.scriptName = scriptName;
}

}