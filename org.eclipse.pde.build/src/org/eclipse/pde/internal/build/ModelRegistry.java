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
import org.xml.sax.InputSource;
	
class ModelRegistry {
	private Map components = new HashMap(9);
	private Map configurations = new HashMap(9);

	// constants	
	private static final String FILENAME_COMPONENT = "install.xml";
	private static final String FILENAME_CONFIGURATION = "install.xml";
	private static final String PATH_COMPONENTS = "install/components";
	private static final String PATH_CONFIGURATIONS = "install/configurations";
	
public ComponentModel getComponent(String identifier) {
	return (ComponentModel)components.get(identifier);
}
public ConfigurationModel getConfiguration(String identifier) {
	return (ConfigurationModel)configurations.get(identifier);
}
protected File getContainedFile(File directory,String filename) {
	String children[] = directory.list();
	for (int i = 0; i < children.length; i++) {
		if (children[i].equalsIgnoreCase(filename))
			return new File(directory,filename);
	}
	
	return null;
}
protected void readComponent(File componentFile) {
	FileInputStream inStream = null;
	try {
		inStream = new FileInputStream(componentFile);
		MultiStatus problems = new MultiStatus("vajextractor", 13, "component parsing problems", null);
		PluginParser parser = new PluginParser(new Factory(problems));
		ComponentModel result = (ComponentModel)parser.parseInstall(new InputSource(inStream));
		result.setLocation(componentFile.getParent());
		registerComponent(result);
	} catch (FileNotFoundException e) {
		return;
	} catch (Exception e) {
		e.printStackTrace();
		return;
	} finally {
		if (inStream != null) {
			try {
				inStream.close();
			} catch (IOException e) {
			}
		}
	}
}
protected void readConfiguration(File configurationFile) {
	FileInputStream inStream = null;
	try {
		inStream = new FileInputStream(configurationFile);
		MultiStatus problems = new MultiStatus("vajextractor", 13, "configuration parsing problems", null);
		PluginParser parser = new PluginParser(new Factory(problems));
		ConfigurationModel result = (ConfigurationModel)parser.parseInstall(new InputSource(inStream));
		result.setLocation(configurationFile.getParent());
		registerConfiguration(result);
	} catch (FileNotFoundException e) {
		return;
	} catch (Exception e) {
		e.printStackTrace();
		return;
	} finally {
		if (inStream != null) {
			try {
				inStream.close();
			} catch (IOException e) {
			}
		}
	}
}
public void registerComponent(ComponentModel component) {
	components.put(component.getId(),component);
}

public void registerConfiguration(ConfigurationModel configuration) {
	configurations.put(configuration.getId(),configuration);
}

protected File[] searchOneLevel(File directory,String filename) {
	Vector accumulatingResult = new Vector();
	
	String children[] = directory.list();
	if (children != null) {
		for (int i = 0; i < children.length; i++) {
			File currentChild = new File(directory,children[i]);
			if (currentChild.isDirectory()) {
				File targetFile = getContainedFile(currentChild,filename);
				if (targetFile != null)
					accumulatingResult.addElement(targetFile);
			}
		}
	}
	
	File result[] = new File[accumulatingResult.size()];
	accumulatingResult.copyInto(result);
	
	return result;
}


public void seekComponents(String baseDir) {
	IPath basePath = new Path(baseDir).append(PATH_COMPONENTS);
	File componentDefinitionFiles[] = searchOneLevel(new File(basePath.toString()),FILENAME_COMPONENT);
		
	for (int i = 0; i < componentDefinitionFiles.length; i++)
		readComponent(componentDefinitionFiles[i]);
}

public void seekConfigurations(String baseDir) {
	IPath basePath = new Path(baseDir).append(PATH_CONFIGURATIONS);
	File configurationDefinitionFiles[] = searchOneLevel(new File(basePath.toString()),FILENAME_CONFIGURATION);
		
	for (int i = 0; i < configurationDefinitionFiles.length; i++)
		readConfiguration(configurationDefinitionFiles[i]);
}

}
	
