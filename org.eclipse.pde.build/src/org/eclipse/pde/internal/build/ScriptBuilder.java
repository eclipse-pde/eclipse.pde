package org.eclipse.pde.internal.core;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class ScriptBuilder extends Task {
	String script;
	String element;
	String elements;
	String install;
	String directory;

public void execute() throws BuildException {
	try {
		if (script.equals("plugin")) {
			PluginBuildScriptGenerator.main(new String[] {"-install ", install, " -" + element, elements});
			return;
		}
		if (script.equals("fragment")) {
			FragmentBuildScriptGenerator.main(new String[] {"-install ", install, " -" + element, elements});
			return;
		}
		if (script.equals("component")) {
			ComponentBuildScriptGenerator.main(new String[] {"-install ", install, " -" + element, elements});
			return;
		}
		if (script.equals("configuration")) {
			ConfigurationBuildScriptGenerator.main(new String[] {"-install ", install, " -" + element, elements});
			return;
		}
	} catch (Exception e) {
		throw new BuildException(script + ": Unable to generate build.xml", e);
	}
}
public void setDirectory(String value) {
	directory = value;
}
public void setElement(String value) {
	element = value;
}
public void setElements(String value) {
	elements = value;
}
public void setInstall(String value) {
	install = value;
}
public void setScript(String value) {
	script = value;
}
}
