package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.build.ant.AntScript;
/**
 * 
 */
public class BuildAntScript extends AntScript implements IXMLConstants {

	protected boolean jarExternal = false;
	protected boolean zipExternal = false;
	protected String zipExecutable;
	protected String zipArgument;

public BuildAntScript(OutputStream output) {
	super(output);
}

protected void printExternalZipTask(int tab, String zipFile, String basedir) {
	List args = new ArrayList(1);
	args.add(zipArgument);
	printExecTask(tab, zipExecutable, basedir, args);
}

/**
 * If the user has specified an external zip program in the build.properties file,
 * use it. Otherwise use the default Ant jar task.
 */
public void printJarTask(int tab, String zipFile, String basedir) {
	if (jarExternal)
		printExternalZipTask(tab, zipFile, basedir);
	else
		super.printJarTask(tab, zipFile, basedir);
}

/**
 * If the user has specified an external zip program in the build.properties file,
 * use it. Otherwise use the default Ant zip task.
 */
public void printZipTask(int tab, String zipFile, String basedir) {
	if (zipExternal)
		printExternalZipTask(tab, zipFile, basedir);
	else
		super.printZipTask(tab, zipFile, basedir);
}

public void printPluginLocationDeclaration(int tab, String entry, String property) {
	printTab(tab);
	output.print("<pluginLocation");
	int i = entry.indexOf("@") + 1;
	String pluginId = entry.substring(i);
	boolean fragment = entry.startsWith("fragment@");
	printAttribute(fragment ? "fragment" : "plugin", pluginId, true);
	printAttribute("property", property, true);
	output.println("/>");
}

/**
 * Sets the jarExternal.
 */
public void setJarExternal(boolean jarExternal) {
	this.jarExternal = jarExternal;
}

/**
 * Sets the zipExternal.
 */
public void setZipExternal(boolean zipExternal) {
	this.zipExternal = zipExternal;
}

/**
 * Sets the zipArgument.
 */
public void setZipArgument(String zipArgument) {
	this.zipArgument = zipArgument;
}

/**
 * Sets the zipExecuteble.
 */
public void setZipExecutable(String zipExecutable) {
	this.zipExecutable = zipExecutable;
}

}