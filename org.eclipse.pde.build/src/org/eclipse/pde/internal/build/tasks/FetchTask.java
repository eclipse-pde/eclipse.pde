package org.eclipse.pde.internal.core.tasks;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;
/**
 * 
 */
public class FetchTask extends Task implements IPDECoreConstants {

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
public void setElements(String elements) {
	this.elements = Utils.getArrayFromString(elements);
}

/**
 * Sets the elements.
 */
public void internalSetElements(String[] elements) {
	this.elements = elements;
}


/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	this.installLocation = installLocation;
}

public void execute() throws BuildException {
	try {
		run();
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

public void run() throws CoreException {
	if (this.elements == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null));
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
 * Sets the scriptName.
 */
public void setScriptName(String scriptName) {
	this.scriptName = scriptName;
}
}