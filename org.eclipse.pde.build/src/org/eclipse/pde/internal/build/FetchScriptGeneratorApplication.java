package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.List;

import org.eclipse.core.runtime.*;
/**
 * 
 */
public class FetchScriptGeneratorApplication extends AbstractApplication {

	/**
	 * 
	 */
	protected boolean children = true;

	/**
	 * 
	 */
	protected String[] elements;

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
	protected String scriptName;

public FetchScriptGeneratorApplication() {
}

public void run() throws CoreException {
	if (this.elements == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null));
	FetchScriptGenerator generator = new FetchScriptGenerator();
	generator.setDirectoryLocation(directoryLocation);
	generator.setInstallLocation(installLocation);
	generator.setFetchChildren(children);
	generator.setCvsPassFileLocation(cvsPassFileLocation);
	generator.setScriptName(scriptName);
	for (int i = 0; i < elements.length; i++) {
		generator.setElement(elements[i]);
		generator.generate();
	}
}

/**
 * @see AbstractApplication#processCommandLine
 */
protected void processCommandLine(List commands) {
	super.processCommandLine(commands);

	// looks for flag-like commands
	if (commands.remove(ARG_NO_CHILDREN)) 
		children = false;

	// looks for param/arg-like commands
	elements = getArguments(commands, ARG_ELEMENTS);
	String[] arguments = getArguments(commands, ARG_INSTALL_LOCATION);
	installLocation = arguments[0]; // only consider one location
	arguments = getArguments(commands, ARG_DIRECTORY_LOCATION);
	directoryLocation = arguments[0]; // only consider one location
	arguments = getArguments(commands, ARG_CVS_PASSFILE_LOCATION);
	if (arguments != null)
		cvsPassFileLocation = arguments[0]; // only consider one location
	arguments = getArguments(commands, ARG_SCRIPT_NAME);
	if (arguments != null)
		scriptName = arguments[0]; // only consider one name
}

/**
 * Sets the children.
 */
public void setChildren(boolean children) {
	this.children = children;
}

/**
 * Sets the cvsPassFileLocation.
 */
public void setCvsPassFile(String cvsPassFileLocation) {
	this.cvsPassFileLocation = cvsPassFileLocation;
}

/**
 * Sets the directoryLocation.
 */
public void setDirectory(String directoryLocation) {
	this.directoryLocation = directoryLocation;
}

/**
 * Sets the elements.
 */
public void setElements(String[] elements) {
	this.elements = elements;
}

/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	this.installLocation = installLocation;
}

/**
 * Sets the scriptName.
 */
public void setScriptName(String scriptName) {
	this.scriptName = scriptName;
}
}