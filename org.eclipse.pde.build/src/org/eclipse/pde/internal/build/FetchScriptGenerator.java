package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.util.*;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.ant.AntScript;
import org.eclipse.update.core.Feature;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.internal.core.FeatureExecutableFactory;

/**
 * 
 */
public class FetchScriptGenerator extends AbstractScriptGenerator {

	/**
	 * 
	 */
	protected AntScript script;

	/**
	 * 
	 */
	protected boolean fetchChildren = true;

	/**
	 * 
	 */
	protected String element;

	/**
	 * 
	 */
	protected String installLocation;

	/**
	 * 
	 */
	protected String directoryLocation;

	/**
	 * 
	 */
	protected String cvsPassFileLocation;

	/**
	 * 
	 */
	protected String scriptName = DEFAULT_FETCH_SCRIPT_FILENAME;

	/**
	 * 
	 */
	protected Properties directory;

	/**
	 * Variables to control is a mkdir to a specific folder was already.
	 */
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
		script = new BuildAntScript(new FileOutputStream(target));
		try {
			generateFetchScript();
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e));
	}
}

/**
 * Main call for generating the script.
 */
protected void generateFetchScript() throws CoreException {
	generatePrologue();
	generateFetchTarget();
	generateEpilogue();
}


protected void generateFetchTarget() throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_FETCH, null, null, null, null);
	tab++;
	generateFetchEntry(tab, element);
	tab--;
	script.printString(tab, "</target>");
}

protected void generateFetchEntry(int tab, String entry) throws CoreException {
	String cvsInfo = getCVSInfo(entry);
	if (cvsInfo == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_ENTRY_MISSING, Policy.bind("error.missingDirectoryEntry", entry), null));

	int index = entry.indexOf('@');
	String type = entry.substring(0, index);
	String element = entry.substring(index + 1);
	String location = getElementLocation(type);
	String[] cvsFields = Utils.getArrayFromString(cvsInfo);
	String tag = cvsFields[0];
	String cvsRoot = cvsFields[1];
	String password = (cvsFields.length > 2) ? cvsFields[2] : null;

	if (password != null)
		script.printCVSPassTask(tab, cvsRoot, password, cvsPassFileLocation);

	generateMkdirs(tab, location);
	script.printCVSTask(tab, null, cvsRoot, location, element, tag, getPropertyFormat(PROPERTY_QUIET), cvsPassFileLocation);

	if (fetchChildren && type.equals("feature")) {
		Feature feature = retrieveFeature(element, cvsRoot, tag, password);
		generateChildrenFetchScript(tab, feature);
	}
}

/**
 * Helper method to control for what locations a mkdir Ant task was already
 * generated so we can reduce replication.
 */
protected void generateMkdirs(int tab, String location) {
	if (mkdirLocations.contains(location))
		return;
	mkdirLocations.add(location);
	script.printMkdirTask(tab, location);
}

protected void generateChildrenFetchScript(int tab, Feature feature) throws CoreException {
	IPluginEntry[] children = feature.getPluginEntries();
	for (int i = 0; i < children.length; i++) {
		String elementId = children[i].getVersionedIdentifier().getIdentifier();
		if (children[i].isFragment())
			generateFetchEntry(tab, "fragment@" + elementId);
		else
			generateFetchEntry(tab, "plugin@" + elementId);
	}
}

protected Feature retrieveFeature(String element, String cvsRoot, String tag, String password) throws CoreException {
	File root = new File(installLocation);
	File target = new File(root, "retrieve.xml");
	try {
		AntScript retrieve = new AntScript(new FileOutputStream(target));
		try {
			retrieve.printProjectDeclaration("RetrieveFeature", "main", ".");
			retrieve.printTargetDeclaration(0, "main", null, null, null, null);
			IPath module = new Path(element).append("feature.xml");
			if (password != null)
				retrieve.printCVSPassTask(0, cvsRoot, password, cvsPassFileLocation);
			retrieve.printCVSTask(0, null, cvsRoot, null, module.toString(), tag, "true", cvsPassFileLocation);
			retrieve.printString(0, "</target>");
			retrieve.printString(0, "</project>");
		} finally {
			retrieve.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e));
	}
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
			throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_FEATURE_MISSING, Policy.bind("exception.missingFeature", new String[] {element}), null));
		return feature;
	} catch (Exception e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {element}), e));
	}
}

/**
 * Deletes all the files and directories from the given root down (inclusive).
 * Returns false if we could not delete some file or an exception occurred
 * at any point in the deletion.
 * Even if an exception occurs, a best effort is made to continue deleting.
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
		result = false;
	}
	return result;
}

protected String getElementLocation(String type) {
	IPath location = new Path(getPropertyFormat(PROPERTY_INSTALL));
	if (type.equals("feature"))
		location = location.append(DEFAULT_FEATURE_LOCATION);
	else
		location = location.append(DEFAULT_PLUGIN_LOCATION);
	return location.toString();
}

/**
 * Get information stored in the directory file.
 */
protected String getCVSInfo(String element) throws CoreException {
	if (directory == null)
		readDirectory();
	return directory.getProperty(element);
}

/**
 * Reads directory file at the directoryLocation.
 */
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
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_READ_DIRECTORY, Policy.bind("error.readingDirectory"), e));
	}
}


/**
 * Defines, the XML declaration and Ant project.
 */
protected void generatePrologue() {
	script.println();
	script.printComment(0, "Fetch script for " + element);
	script.println();
	script.printProjectDeclaration("FetchScript", TARGET_FETCH, ".");
	script.printProperty(1,PROPERTY_QUIET, "true");
}

/**
 * Just ends the script.
 */
protected void generateEpilogue() {
	script.println();
	script.printString(0, "</project>");
}

	
/**
 * Sets the directory location.
 */
public void setDirectoryLocation(String directoryLocation) {
	this.directoryLocation = directoryLocation;
}

	
/**
 * Sets the element to generate fetch script from.
 */
public void setElement(String element) {
	this.element = element;
}

	
/**
 * Sets whether children of the current element should be fetch.
 */
public void setFetchChildren(boolean fetchChildren) {
	this.fetchChildren = fetchChildren;
}

	
/**
 * Sets the install location.
 */
public void setInstallLocation(String installLocation) {
	this.installLocation = installLocation;
}

	
/**
 * Sets the cvsPassFileLocation.
 */
public void setCvsPassFileLocation(String cvsPassFileLocation) {
	this.cvsPassFileLocation = cvsPassFileLocation;
}

	
/**
 * Sets the scriptName.
 */
public void setScriptName(String scriptName) {
	if (scriptName == null)
		this.scriptName = DEFAULT_FETCH_SCRIPT_FILENAME;
	else
		this.scriptName = scriptName;
}

}