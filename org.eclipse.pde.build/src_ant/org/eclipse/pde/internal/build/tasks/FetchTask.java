package org.eclipse.pde.internal.build.tasks;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;
/**
 * 
 */
public class FetchTask extends Task implements IPDEBuildConstants {

	/**
	 * 
	 */
	protected FetchScriptGeneratorApplication generator;

/**
 * Constructor for FetchTask.
 */
public FetchTask() {
	generator = new FetchScriptGeneratorApplication();
}

/**
 * Sets the children.
 */
public void setChildren(boolean children) {
	generator.setChildren(children);
}

/**
 * Sets the cvsPassFileLocation.
 */
public void setCvsPassFile(String cvsPassFileLocation) {
	generator.setCvsPassFile(cvsPassFileLocation);
}

/**
 * Sets the directoryLocation.
 */
public void setDirectory(String directoryLocation) {
	generator.setDirectory(directoryLocation);
}

/**
 * Sets the elements.
 */
public void setElements(String elements) {
	generator.setElements(Utils.getArrayFromString(elements));
}


/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	generator.setInstall(installLocation);
}

public void execute() throws BuildException {
	try {
		generator.run();
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}


/**
 * Sets the scriptName.
 */
public void setScriptName(String scriptName) {
	generator.setScriptName(scriptName);
}
}