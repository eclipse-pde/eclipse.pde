package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.model.PluginModel;

/**
 * Generates build.xml script for features.
 */
public class FragmentBuildScriptGenerator extends ModelBuildScriptGenerator {

protected PluginModel getModel(String modelId) throws CoreException {
	return getRegistry().getFragment(modelId);
}

protected String getModelTypeName() {
	return "fragment";
}

protected String getDirectoryName() {
	return "plugins/${fragment}_${version}";
}
}
