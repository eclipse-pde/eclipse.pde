package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.model.build.*;
import org.eclipse.core.runtime.model.*;
import java.net.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.model.build.*;
import java.io.File;


public abstract class ExternalPluginModelBase extends AbstractPluginModelBase {
	private String installLocation;
	private IPath eclipseHomeRelativePath;
	private IBuildModel buildModel;
	private long timeStamp;

public ExternalPluginModelBase() {
	super();
}
protected NLResourceHelper createNLResourceHelper() {
	try {
		String installLocation = getInstallLocation();
		if (installLocation.startsWith("file:")==false)
		   installLocation = "file:"+installLocation;
		URL url = new URL(installLocation+"/");
		String name = isFragmentModel() ? "fragment" : "plugin";
		return new NLResourceHelper(name, url);
	} catch (MalformedURLException e) {
		return null;
	}
}
public IBuildModel getBuildModel() {
	if (buildModel == null) {
		buildModel = new ExternalBuildModel(getInstallLocation());
		((ExternalBuildModel) buildModel).load();
	}
	return buildModel;
}
public org.eclipse.core.runtime.IPath getEclipseHomeRelativePath() {
	return eclipseHomeRelativePath;
}
public String getInstallLocation() {
	return installLocation;
}
public boolean isEditable() {
	return false;
}
public void load() {}
public void load(PluginModel descriptorModel) {
	PluginBase pluginBase = (PluginBase)getPluginBase();
	if (pluginBase == null) {
		pluginBase = (PluginBase)createPluginBase();
		this.pluginBase = pluginBase;
	} else {
		pluginBase.reset();
	}
	pluginBase.load(descriptorModel);
	updateTimeStamp();
	loaded=true;
}

public boolean isInSync() {
	return isInSync(getLocalFile());
}

private File getLocalFile() {
	String manifest = isFragmentModel()?"fragment.xml":"plugin.xml";
	return new File(getInstallLocation()+File.separator+manifest);
}

protected void updateTimeStamp() {
	updateTimeStamp(getLocalFile());
}

public void setEclipseHomeRelativePath(IPath newEclipseHomeRelativePath) {
	eclipseHomeRelativePath = newEclipseHomeRelativePath;
}
public void setInstallLocation(String newInstallLocation) {
	installLocation = newInstallLocation;
}
}
