package org.eclipse.pde.internal.build.tasks;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Policy;
/**
 * Set's a property defining the location of the template.xml file.
 */
public class InitTemplate extends Task implements IPDEBuildConstants, IXMLConstants {

public InitTemplate() {
	super();
}

public void execute() throws BuildException {
	IPluginDescriptor plugin = Platform.getPluginRegistry().getPluginDescriptor(PI_PDEBUILD);
	if (plugin == null)
		return;
	try {
		String location = Platform.resolve(plugin.getInstallURL()).getFile();
		location = new File(location, "template.xml").getAbsolutePath();
		project.setProperty(PROPERTY_TEMPLATE, location);
	} catch (IOException e) {
		throw new BuildException(Policy.bind("exception.missingTemplate"));
	}
}

}
