package org.eclipse.pde.internal.core.tasks;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.update.core.Feature;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.internal.core.FeatureExecutableFactory;
/**
 * Used to create a build manifest file describing what plug-ins and versions
 * were included in a build. It has to be executed after a fetch task.
 */
public class BuildManifestTask extends Task implements IPDECoreConstants, IXMLConstants {
	private String buildId;
	protected String buildName;
	private String buildQualifier;
	private String buildType;
	protected boolean children = true;
	protected String destination;
	protected Properties directory;
	protected String directoryLocation;
	protected String[] elements;
	protected String installLocation;

public void execute() throws BuildException {
	try {
		if (this.elements == null)
			throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_ELEMENT_MISSING, Policy.bind("error.missingElement"), null));
		readDirectory();
		PrintWriter output = new PrintWriter(new FileOutputStream(destination));
		try {
			List entries = new ArrayList(20);
			for (int i = 0; i < elements.length; i++)
				collectEntries(entries, elements[i]);
			generatePrologue(output);
			generateEntries(output, entries);
		} finally {
			output.close();
		}
	} catch (Exception e) {
		throw new BuildException(e);
	}
}

protected void generatePrologue(PrintWriter output) {
	output.print("# Build Manifest for ");
	output.println(buildName);
	output.println();
	output.println("# The format of this file is:");
	output.println("# <type>@<element>=<CVS tag>");
	output.println();
	String id = getBuildId();
	if (id != null) {
		output.print(PROPERTY_BUILD_ID + "=");
		output.println(id);
	}
	String type = getBuildType();
	if (type != null) {
		output.print(PROPERTY_BUILD_TYPE + "=");
		output.println(type);
	}
	String qualifier = getBuildQualifier();
	if (qualifier != null) {
		output.print(PROPERTY_BUILD_QUALIFIER + "=");
		output.println(qualifier);
	}
	output.println();
}

protected String getBuildId() {
	if (buildId == null)
		buildId = getProject().getProperty(PROPERTY_BUILD_ID);
	return buildId;
}

protected String getBuildQualifier() {
	if (buildQualifier == null)
		buildQualifier = getProject().getProperty(PROPERTY_BUILD_QUALIFIER);
	return buildQualifier;
}

protected String getBuildType() {
	if (buildType == null)
		buildType = getProject().getProperty(PROPERTY_BUILD_TYPE);
	return buildType;
}
protected void generateEntries(PrintWriter output, List entries) throws CoreException {
	Collections.sort(entries);
	for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
		String entry = (String) iterator.next();
		output.println(entry);
	}
}

/**
 * Collects all the elements that are part of this build.
 */
protected void collectEntries(List entries, String entry) throws CoreException {
	String cvsInfo = directory.getProperty(entry);
	if (cvsInfo == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_ENTRY_MISSING, Policy.bind("error.missingDirectoryEntry", entry), null));

	int index = entry.indexOf('@');
	String type = entry.substring(0, index);
	String element = entry.substring(index + 1);
	if (type.equals("plugin") || type.equals("fragment")) {
		String[] cvsFields = Utils.getArrayFromString(cvsInfo);
		String tag = cvsFields[0];
		StringBuffer sb = new StringBuffer();
		sb.append(entry);
		sb.append("=");
		sb.append(tag);
		entries.add(sb.toString());
	} else if (children && type.equals("feature")) {
		Feature feature = readFeature(element);
		collectChildrenEntries(entries, feature);
	}
}

protected void collectChildrenEntries(List entries, Feature feature) throws CoreException {
	IPluginEntry[] children = feature.getPluginEntries();
	for (int i = 0; i < children.length; i++) {
		String elementId = children[i].getVersionedIdentifier().getIdentifier();
		if (children[i].isFragment())
			collectEntries(entries, "fragment@" + elementId);
		else
			collectEntries(entries, "plugin@" + elementId);
	}
}

protected Feature readFeature(String element) throws CoreException {
	IPath root = new Path(installLocation);
	root = root.append(DEFAULT_FEATURE_LOCATION);
	root = root.append(element);
	try {
		FeatureExecutableFactory factory = new FeatureExecutableFactory();
		return (Feature) factory.createFeature(root.toFile().toURL(), null);
	} catch (Exception e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDECORE, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {element}), e));
	}	
}

/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	this.installLocation = installLocation;
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

public void setDirectory(String directory) {
	directoryLocation = directory;
}

public void setElements(String value) {
	elements = Utils.getArrayFromString(value);
}

/**
 * Sets the full location of the manifest file.
 */
public void setDestination(String value) {
	destination = value;
}

/**
 * Whether children of this element should be taken into account.
 */
public void setChildren(boolean children) {
	this.children = children;
}

public void setBuildName(String value) {
	buildName = value;
}

/**
 * Sets the buildId.
 */
public void setBuildId(String buildId) {
	this.buildId = buildId;
}

/**
 * Sets the buildQualifier.
 */
public void setBuildQualifier(String buildQualifier) {
	this.buildQualifier = buildQualifier;
}

/**
 * Sets the buildType.
 */
public void setBuildType(String buildType) {
	this.buildType = buildType;
}

}