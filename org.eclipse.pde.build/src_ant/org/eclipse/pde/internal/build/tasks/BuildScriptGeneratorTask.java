package org.eclipse.pde.internal.build.tasks;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;
/**
 * 
 */
public class BuildScriptGeneratorTask extends Task implements IXMLConstants, IPDECoreConstants {

	/**
	 * 
	 */
	protected BuildScriptGenerator generator = new BuildScriptGenerator();

	/**
	 * Build variables.
	 */
	protected String buildVariableOS;
	protected String buildVariableWS;
	protected String buildVariableNL;
	protected String buildVariableARCH;

/**
 * Sets the children.
 */
public void setChildren(boolean children) {
	generator.setChildren(children);
}


/**
 * Sets the devEntries.
 */
public void setDevEntries(String devEntries) {
	generator.setDevEntries(Utils.getArrayFromString(devEntries));
}

/**
 * Sets the pluginPath.
 */
public void setPluginPath(String pluginPath) {
	generator.setPluginPath(Utils.getArrayFromString(pluginPath));
}






/**
 * Sets the elements.
 */
public void setElements(String elements) {
	generator.setElements(Utils.getArrayFromString(elements));
}





public void execute() throws BuildException {
	try {
		run();
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

public void run() throws CoreException {
	generator.run();
}









/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	generator.setInstall(installLocation);
}

/**
 * Sets the buildVariableARCH.
 */
public void setARCH(String buildVariableARCH) {
	this.buildVariableARCH = buildVariableARCH;
}

/**
 * Sets the buildVariableNL.
 */
public void setNL(String buildVariableNL) {
	this.buildVariableNL = buildVariableNL;
}

/**
 * Sets the buildVariableOS.
 */
public void setOS(String buildVariableOS) {
	this.buildVariableOS = buildVariableOS;
}

/**
 * Sets the buildVariableWS.
 */
public void setWS(String buildVariableWS) {
	this.buildVariableWS = buildVariableWS;
}

}