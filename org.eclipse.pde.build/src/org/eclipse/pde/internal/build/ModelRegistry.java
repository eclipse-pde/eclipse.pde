package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.plugins.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.core.Feature;
import org.eclipse.update.core.model.PluginEntryModel;
import org.eclipse.update.internal.core.FeatureExecutableFactory;
import org.xml.sax.InputSource;
	
class ModelRegistry {
	private Map features = new HashMap(9);

	// constants	
	private static final String FILENAME_FEATURE = "feature.xml";
	private static final String PATH_FEATURES = "install/features";
	
public Feature getFeature(String identifier) {
	return (Feature)features.get(identifier);
}

/**
 * Returns a feature which consists of all the plugins/fragments in all of the 
 * given features as known by this registry.  The returned feature has only a 
 * list of plugins/fragment (i.e., no id, no other setttings).
 */
public Feature merge(String[] features) {
	HashSet plugins = new HashSet(10);
	for (int i = 0; i < features.length; i++) {
		Feature feature = getFeature(features[i]);
		if (feature != null)
			plugins.addAll(Arrays.asList(feature.getPluginEntries()));
	}
	IPluginEntry[] entries = (IPluginEntry[])plugins.toArray(new IPluginEntry[plugins.size()]);
	Feature result = new Feature();
	result.setPluginEntryModels((PluginEntryModel[]) entries);
	return result;
}
protected File searchIndividualDir(File directory,String filename) {
	if (!directory.isDirectory()) 
		return null;
	String children[] = directory.list();
	for (int i = 0; i < children.length; i++) {
		if (children[i].equalsIgnoreCase(filename))
			return new File(directory,filename);
	}
	return null;
}
public Feature readFeature(File file) {
	if (file == null) {
		IStatus status = new Status(IStatus.ERROR, PluginTool.PI_PDECORE, ScriptGeneratorConstants.EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingComponentId"), null);
		PluginTool.getPluginLog().log(status);
		return null;
	}
	try {
		FeatureExecutableFactory factory = new FeatureExecutableFactory();
		Feature result = (Feature) factory.createFeature(file.toURL(), null);
		registerFeature(result);
		return result;
	} catch (Exception e) {
		e.printStackTrace();
		return null;
	}
}

public void registerFeature(Feature feature) {
	features.put(feature.getFeatureIdentifier(), feature);
}

protected File[] searchInstallDir(File directory, String filename) {
	ArrayList result = new ArrayList();
	String children[] = directory.list();
	if (children != null) {
		for (int i = 0; i < children.length; i++) {
			File targetFile = searchIndividualDir(new File(directory, children[i]), filename);
			if (targetFile != null)
				result.add(targetFile);
		}
	}
	return (File[])result.toArray(new File[result.size()]);
}


public void seekFeatures(String baseDir) {
	IPath basePath = new Path(baseDir).append(PATH_FEATURES);
	File files[] = searchInstallDir(new File(basePath.toString()), FILENAME_FEATURE);
	for (int i = 0; i < files.length; i++)
		readFeature(files[i]);
}

}
	
