package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.model.*;

public class PluginBuildScriptGenerator extends ModelBuildScriptGenerator {

public PluginBuildScriptGenerator() {
	super();
}
public PluginBuildScriptGenerator(PluginModel modelsToGenerate[],PluginRegistryModel registry) {
	super(modelsToGenerate,registry);
}

protected String getComponentDirectoryName() {
	return "plugins/${plugin}_${version}";
}
protected String getModelTypeName() {
	return "plugin";
}
public static void main(String[] args) throws Exception {
	new PluginBuildScriptGenerator().run(args);
}
public static void main(String argString) throws Exception {
	main(tokenizeArgs(argString));
}
protected PluginModel retrieveModelNamed(String modelName) {
	return getRegistry().getPlugin(modelName);
}

}
