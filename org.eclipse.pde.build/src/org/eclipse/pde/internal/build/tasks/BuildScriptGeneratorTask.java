package org.eclipse.pde.internal.core.tasks;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.*;
/**
 * 
 */
public class BuildScriptGeneratorTask extends Task implements IXMLConstants {

	/**
	 * Indicates whether scripts for a feature's children should be generated.
	 */
	protected boolean children = true;

	/**
	 * Source elements for script generation.
	 */
	protected String[] elements;

	/**
	 * Additional dev entries for the compile classpath.
	 */
	protected String[] devEntries = new String[0];

	/**
	 * Where to find the elements.
	 */
	protected String installLocation;
	
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
	this.children = children;
}


/**
 * Sets the devEntries.
 */
public void setDevEntries(String devEntries) {
	this.devEntries = Utils.getArrayFromString(devEntries);
}

/**
 * Sets the devEntries.
 */
public void internalSetDevEntries(String[] devEntries) {
	this.devEntries = devEntries;
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
 * Separate elements by kind.
 */
protected void sortElements(List features, List plugins, List fragments) {
	for (int i = 0; i < elements.length; i++) {
		int index = elements[i].indexOf('@');
		String type = elements[i].substring(0, index);
		String element = elements[i].substring(index + 1);
		if (type.equals("plugin")) 
			plugins.add(element);
		if (type.equals("fragment")) 
			fragments.add(element);
		if (type.equals("feature")) 
			features.add(element);
	}
}

public void execute() throws BuildException {
	try {
		run();
	} catch (CoreException e) {
		throw new BuildException(e);
	}
}

public void run() throws CoreException {
	List plugins = new ArrayList(5);
	List fragments = new ArrayList(5);
	List features = new ArrayList(5);
	sortElements(features, plugins, fragments);
	generateModels(new PluginBuildScriptGenerator(), plugins);
	generateModels(new FragmentBuildScriptGenerator(), fragments);
	generateFeatures(features);
}

protected void generateFeatures(List features) throws CoreException {
	if (features.isEmpty())
		return;
	FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator();
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	generator.setGenerateChildrenScript(children);
	for (int i = 0; i < features.size(); i++) {
		// setFeature has to be called before configurePersistentProperties
		// because it reads the model's properties
		generator.setFeature((String) features.get(i));
		setBuildVariables(generator);
		generator.generate();
	}
}

protected void generateModels(ModelBuildScriptGenerator generator, List models) throws CoreException {
	if (models.isEmpty())
		return;
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	setBuildVariables(generator);
	for (Iterator iterator = models.iterator(); iterator.hasNext();) {
		String model = (String) iterator.next();
		generator.setModelId(model);
		generator.generate();
	}
}

protected void setBuildVariables(AbstractBuildScriptGenerator generator) {
	boolean projectAvailable = getProject() != null;
	if (buildVariableOS == null && projectAvailable)
		buildVariableOS = getProject().getProperty(PROPERTY_OS);
	generator.setBuildVariableOS(buildVariableOS);
	if (buildVariableWS == null && projectAvailable)
		buildVariableWS = getProject().getProperty(PROPERTY_WS);
	generator.setBuildVariableWS(buildVariableWS);
	if (buildVariableNL == null && projectAvailable)
		buildVariableNL = getProject().getProperty(PROPERTY_NL);
	generator.setBuildVariableNL(buildVariableNL);
	if (buildVariableARCH == null && projectAvailable)
		buildVariableARCH = getProject().getProperty(PROPERTY_ARCH);
	generator.setBuildVariableARCH(buildVariableARCH);
}


/**
 * Sets the installLocation.
 */
public void setInstall(String installLocation) {
	this.installLocation = installLocation;
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