package org.eclipse.pde.internal.ui.wizards.exports;

//import java.io.File;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.apache.tools.ant.Project;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.FragmentBuildScriptGenerator;
import org.eclipse.pde.internal.build.ModelBuildScriptGenerator;
import org.eclipse.pde.internal.build.PluginBuildScriptGenerator;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;

/**
 * Insert the type's description here.
 * @see Wizard
 */
public class PluginExportWizard extends BaseExportWizard {
	private static final String KEY_WTITLE = "ExportWizard.Plugin.wtitle";
	private static final String STORE_SECTION = "PluginExportWizard";

	/**
	 * The constructor.
	 */
	public PluginExportWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXPRJ_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	protected BaseExportWizardPage createPage1() {
		return new PluginExportWizardPage(getSelection());
	}

	/**
	 * 
	 */
	protected void doExport(
		boolean exportZip,
		String destination,
		IModel model,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		try {
			IPluginModelBase modelBase = (IPluginModelBase) model;

			String label =
				PDEPlugin.getDefault().getLabelProvider().getObjectText(
					modelBase.getPluginBase());
			monitor.subTask(label);
			String scriptName = MainPreferencePage.getBuildScriptName();
			makeScript(modelBase);
			monitor.worked(1);
			runScript(modelBase, destination, exportZip, monitor);
			monitor.worked(1);
		} finally {
			File file = new File(destination + Path.SEPARATOR + "build_result");
			cleanup(model.getUnderlyingResource().getProject(), file);
			monitor.done();

		}

	}

	private void makeScript(IPluginModelBase model)
		throws CoreException {
		ModelBuildScriptGenerator generator = null;
		if (model.isFragmentModel())
			generator = new FragmentBuildScriptGenerator();
		else
			generator = new PluginBuildScriptGenerator();

		generator.setBuildScriptName(MainPreferencePage.getBuildScriptName());
		generator.setScriptTargetLocation(model.getInstallLocation());
		generator.setInstallLocation(model.getInstallLocation());

		IProject project = model.getUnderlyingResource().getProject();
		if (project.hasNature(JavaCore.NATURE_ID)) {
			IPath path =
				JavaCore.create(project).getOutputLocation().removeFirstSegments(1);
			generator.setDevEntries(new String[] { path.toOSString()});
		} else {
			generator.setDevEntries(new String[] { "bin" });
		}

		generator.setPluginPath(TargetPlatform.createPluginPath());

		generator.setModelId(model.getPluginBase().getId());
		generator.generate();
	}
	
	private void runScript(
		IPluginModelBase model,
		String destination,
		boolean exportZip,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		AntRunner runner = new AntRunner();
		if (exportZip) {
			runner.setExecutionTargets(new String[] { "zip.plugin" });
		} else {
			runner.setExecutionTargets(new String[] { "build.update.jar" });
		}
		runner.setMessageOutputLevel(Project.MSG_ERR);
		runner.addBuildLogger("org.apache.tools.ant.DefaultLogger");
		runner.addUserProperties(createProperties(destination));
		runner.setAntHome(model.getInstallLocation());
		runner.setBuildFileLocation(
			model.getInstallLocation()
				+ Path.SEPARATOR
				+ MainPreferencePage.getBuildScriptName());
		runner.run(monitor);
		monitor.worked(1);

	}
	
	private HashMap createProperties(String destination) {
		HashMap map = new HashMap(3);
		map.put("build.result.folder", destination + Path.SEPARATOR + "build_result");
		map.put("temp.folder", destination + Path.SEPARATOR + "temp.folder");
		map.put("plugin.destination", destination);
		return map;
	}
	
	private void cleanup(IProject project, File resultFolder) throws CoreException {
		IResource buildFile = project.findMember(MainPreferencePage.getBuildScriptName());
		if (buildFile != null)
			buildFile.delete(true, null);
			
		if (resultFolder.exists() && resultFolder.isDirectory()) {
			File[] files = resultFolder.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					files[i].delete();
				}
			}
			resultFolder.delete();
		}

	}
}
