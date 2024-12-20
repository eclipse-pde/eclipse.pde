/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.AbstractScriptGenerator;
import org.eclipse.pde.internal.build.BuildScriptGenerator;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.builders.BuildErrorReporter;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * Creates an ant build file for a plugin
 */
public class GeneratePluginBuildFileAction extends BaseBuildAction {

	@Override
	protected void makeScripts(IProgressMonitor monitor) throws InvocationTargetException, CoreException {

		IProject project = fManifestFile.getProject();
		IPluginModelBase model = PluginRegistry.findModel(project);
		BuildErrorReporter buildErrorReporter = new BuildErrorReporter(fManifestFile);
		IResource buildXML = project.findMember("build.xml"); //$NON-NLS-1$
		if (buildXML != null && buildXML.exists() == true && buildErrorReporter.isCustomBuild() == true) {
			throw new CoreException(Status.warning(PDEUIMessages.BuildPluginAction_WarningCustomBuildExists));
		}
		BuildScriptGenerator generator = new BuildScriptGenerator();
		AbstractScriptGenerator.setEmbeddedSource(AbstractScriptGenerator.getDefaultEmbeddedSource());
		AbstractScriptGenerator.setForceUpdateJar(AbstractScriptGenerator.getForceUpdateJarFormat());
		AbstractScriptGenerator.setConfigInfo(AbstractScriptGenerator.getDefaultConfigInfos());

		generator.setWorkingDirectory(project.getLocation().toOSString());
		Path path = ClasspathHelper.getDevEntriesProperties(project.getLocation().addTrailingSeparator().toString() + "dev.properties", false); //$NON-NLS-1$
		generator.setDevEntries(path);
		generator.setPDEState(TargetPlatformHelper.getState());
		generator.setNextId(TargetPlatformHelper.getPDEState().getNextId());
		generator.setStateExtraData(TargetPlatformHelper.getBundleClasspaths(TargetPlatformHelper.getPDEState()), TargetPlatformHelper.getPatchMap(TargetPlatformHelper.getPDEState()));
		// allow binary cycles
		Properties properties = new Properties();
		properties.put(IBuildPropertiesConstants.PROPERTY_ALLOW_BINARY_CYCLES, "true"); //$NON-NLS-1$
		generator.setImmutableAntProperties(properties);
		if (model != null && model.getPluginBase().getId() != null) {
			generator.setBundles(new BundleDescription[] {model.getBundleDescription()});
			generator.generate();
		} else {
			MessageDialog.openError(null, PDEUIMessages.BuildPluginAction_ErrorDialog_Title, PDEUIMessages.BuildPluginAction_ErrorDialog_Message);
		}
	}

}
