package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;import org.eclipse.core.runtime.model.*;

public class FragmentBuildScriptGenerator extends ModelBuildScriptGenerator {

public FragmentBuildScriptGenerator() {
	super();
}
public FragmentBuildScriptGenerator(PluginModel modelsToGenerate[],PluginRegistryModel registry) {
	super(modelsToGenerate,registry);
}

protected String getComponentDirectoryName() {
	return "fragments/${fragment}_${version}";
}

protected String getModelTypeName() {
	return "fragment";
}
public static void main(String[] args) throws Exception {
	new FragmentBuildScriptGenerator().run(args);
}
public static void main(String argString) throws Exception {
	main(tokenizeArgs(argString));
}
protected void printUsage(PrintWriter out) {
	out.println("\tjava FragmentBuildScriptGenerator -install <targetDir> {-fragment <fragmentId>}* [-dev <devEntries>]");
}
protected PluginModel retrieveModelNamed(String modelName) {
	return getRegistry().getFragment(modelName);
}
}
