package org.eclipse.pde.internal.core;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IPluginDescriptor;import org.eclipse.core.runtime.Platform;
import java.io.File;import java.io.IOException;import java.net.URL;


public class InitTemplate extends Task {

public InitTemplate() {
	super();
}

public void execute() throws BuildException {
	IPluginDescriptor plugin = Platform.getPluginRegistry().getPluginDescriptor("org.eclipse.pde.core");
	if (plugin == null)
		return;
	try {
		String location = Platform.resolve(plugin.getInstallURL()).getFile();
		location = new File(location, "template.xml").getAbsolutePath();
		project.setProperty("template", location);
	} catch (IOException e) {
		throw new BuildException ("cannot locate plugin build template");
	}
}

}
