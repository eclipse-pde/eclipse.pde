/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.FeatureBuildScriptGenerator;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;

/**
 * Insert the type's description here.
 * @see Wizard
 */
public class FeatureExportWizard extends BaseExportWizard {
	private static final String KEY_WTITLE = "ExportWizard.Feature.wtitle";
	private static final String STORE_SECTION = "FeatureExportWizard";

	/**
	 * The constructor.
	 */
	public FeatureExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_FEATURE_EXPORT_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}


	protected BaseExportWizardPage createPage1() {
		return new FeatureExportWizardPage(getSelection());
	}

	protected HashMap createProperties(String destination, boolean exportZip) {		
		HashMap map = new HashMap(5);
		map.put("temp.folder", buildTempLocation + Path.SEPARATOR + "eclipse");
		map.put("feature.temp.folder", buildTempLocation + Path.SEPARATOR + "eclipse");
		if (exportZip) {
			map.put("plugin.destination", destination);
			map.put("feature.destination", destination);
		} else {
			String dest = destination;
			File file = new File(destination,"plugins");
			file.mkdirs();
			if (file.exists()) {
				dest =  file.getAbsolutePath();
			}
			map.put("plugin.destination", dest);
			
			dest = destination;
			file = new File(destination, "features");
			file.mkdirs();
			if (file.exists()) {
				dest = file.getAbsolutePath();
			}
			map.put("feature.destination", dest);
		}
		return map;
	}
	
	protected void doExport(
		boolean exportZip,
		boolean exportSource,
		String destination,
		String zipFileName,
		IModel model,
		IProgressMonitor monitor) throws CoreException, InvocationTargetException {
		IFeatureModel feature = (IFeatureModel) model;
		String label = PDEPlugin.getDefault().getLabelProvider().getObjectText(feature);

		monitor.beginTask("", 10);
		monitor.setTaskName(
			PDEPlugin.getResourceString("ExportWizard.exporting") + " " + label);
		try {
			makeScript(feature);
			monitor.worked(1);
			runScript(
				feature.getInstallLocation(),
				destination,
				exportZip,
				exportSource,
				createProperties(destination, exportZip),
				new SubProgressMonitor(monitor, 9));
		} finally {
			deleteBuildFiles(feature);
			monitor.done();
		}
	}

	private void deleteBuildFiles(IFeatureModel model) {
		
		deleteBuildFile(model);

		IFeaturePlugin[] plugins = model.getFeature().getPlugins();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		for (int i = 0; i < plugins.length; i++) {
			ModelEntry entry =
				manager.findEntry(plugins[i].getId(), plugins[i].getVersion());
			if (entry != null) {
				deleteBuildFile(entry.getActiveModel());
			}

		}

	}
	
	public void deleteBuildFile(IModel model) {
		String scriptName = MainPreferencePage.getBuildScriptName();
		String filename = "";
		if (model instanceof IFeatureModel) {
			filename = ((IFeatureModel)model).getInstallLocation() + Path.SEPARATOR + scriptName; 
		} else {
			filename = ((IPluginModelBase)model).getInstallLocation() + Path.SEPARATOR + scriptName;
		}
		File file = new File(filename);
		if (file.exists()) {
			file.delete();
		}
	}


	public IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	private void makeScript(IFeatureModel model) throws CoreException {
		FeatureBuildScriptGenerator generator = new ExportFeatureBuildScriptGenerator();

		generator.setBuildScriptName(MainPreferencePage.getBuildScriptName());
		generator.setScriptTargetLocation(model.getInstallLocation());
		generator.setFeatureRootLocation(model.getInstallLocation());
		generator.setInstallLocation(model.getInstallLocation());
		
		IProject project = model.getUnderlyingResource().getProject();
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IPath path =
				JavaCore.create(project).getOutputLocation().removeFirstSegments(1);
			generator.setDevEntries(new String[] { path.toOSString()});
		} else {
			generator.setDevEntries(new String[] { "bin" });
		}

		generator.setGenerateChildrenScript(true);
		generator.setPluginPath(TargetPlatform.createPluginPath());

		generator.setFeature(model.getFeature().getId());
		generator.generate();
	}
	
	protected String[] getExecutionTargets(boolean exportZip, boolean exportSource) {
		ArrayList targets = new ArrayList();
		if (!exportZip) {
			targets.add("build.update.jar");	
		} else {
			targets.add("build.jars");
			targets.add("zip.distribution");
			if (exportSource) {
				targets.add("build.sources");
				targets.add("zip.sources");
			}
		}
		return (String[]) targets.toArray(new String[targets.size()]);
	}
	
}
