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
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.builder.FragmentBuildScriptGenerator;
import org.eclipse.pde.internal.build.builder.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.builder.PluginBuildScriptGenerator;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class PluginExportJob extends BaseExportJob {

	public PluginExportJob(
		int exportType,
		boolean exportSource,
		String destination,
		String zipFileName,
		Object[] items) {
		super(exportType, exportSource, destination, zipFileName, items);
	}

	protected HashMap createProperties(
		String destination,
		IPluginBase model,
		int exportType) {
		HashMap map = new HashMap(4);
		String location = buildTempLocation + "/build_result/" + model.getId();
		File dir = new File(location);
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdirs();
		map.put("build.result.folder", location);
		map.put(
			"temp.folder",
			buildTempLocation + "/temp.folder/" + model.getId());
		if (exportType != BaseExportJob.EXPORT_AS_UPDATE_JARS)
			map.put(
				"destination.temp.folder",
				buildTempLocation + "/destination/plugins");
		else
			map.put(
				"destination.temp.folder",
				buildTempLocation + "/temp.folder/" + model.getId());
		map.put("plugin.destination", destination);
		map.put("baseos", TargetPlatform.getOS());
		map.put("basews", TargetPlatform.getWS());
		map.put("basearch", TargetPlatform.getOSArch());
		map.put("basenl", TargetPlatform.getNL());
		map.put("eclipse.running", "true");

		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		map.put("javacFailOnError", "false");
		map.put(
			"javacDebugInfo",
			store.getBoolean(PROP_JAVAC_DEBUG_INFO) ? "on" : "off");
		map.put("javacVerbose", store.getString(PROP_JAVAC_VERBOSE));
		map.put("javacSource", store.getString(PROP_JAVAC_SOURCE));
		map.put("javacTarget", store.getString(PROP_JAVAC_TARGET));
		return map;
	}

	protected void doExport(IModel model, IProgressMonitor monitor)
		throws CoreException, InvocationTargetException {

		IPluginModelBase modelBase = (IPluginModelBase) model;
		try {
			String label =
				PDEPlugin.getDefault().getLabelProvider().getObjectText(
					modelBase.getPluginBase());
			monitor.setTaskName(
				PDEPlugin.getResourceString("ExportJob.exporting")
					+ " "
					+ label);
			monitor.beginTask("", 10);
			makeScript(modelBase);
			monitor.worked(1);
			runScript(
				modelBase.getInstallLocation(),
				destination,
				exportType,
				exportSource,
				createProperties(
					destination,
					modelBase.getPluginBase(),
					exportType),
				new SubProgressMonitor(monitor, 9));
		} finally {
			deleteBuildFile(modelBase);
			monitor.done();
		}

	}

	public void deleteBuildFile(IPluginModelBase model) throws CoreException {
		if (isCustomBuild(model))
			return;
		File file = new File(model.getInstallLocation(), "build.xml");
		if (file.exists())
			file.delete();
	}

	private void makeScript(IPluginModelBase model) throws CoreException {
		ModelBuildScriptGenerator generator = null;
		if (model.isFragmentModel())
			generator = new FragmentBuildScriptGenerator();
		else
			generator = new PluginBuildScriptGenerator();

		generator.setWorkingDirectory(model.getInstallLocation());

		IProject project = model.getUnderlyingResource().getProject();
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IPath path =
				JavaCore
					.create(project)
					.getOutputLocation()
					.removeFirstSegments(
					1);
			generator.setDevEntries(new String[] { path.toOSString()});
		} else {
			generator.setDevEntries(new String[] { "bin" });
		}

		generator.setPluginPath(TargetPlatform.createPluginPath());

		generator.setModelId(model.getPluginBase().getId());
		generator.generate();
	}

}
