package org.eclipse.pde.internal.ui.wizards.exports;

import java.lang.reflect.InvocationTargetException;

import org.apache.tools.ant.Project;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IProject;
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

	private void cleanup(IModel model, String destination) throws CoreException {
		deleteBuildScript(model);
		deleteBuildFolders(destination);
	}

	protected BaseExportWizardPage createPage1() {
		return new PluginExportWizardPage(getSelection());
	}

	protected void doExport(
		boolean exportZip,
		boolean exportChildren,
		String destination,
		IModel model,
		IProgressMonitor monitor) {
		try {
			IPluginModelBase modelBase = (IPluginModelBase) model;

			String label =
				PDEPlugin.getDefault().getLabelProvider().getObjectText(
					modelBase.getPluginBase());
			monitor.setTaskName(
				PDEPlugin.getResourceString("ExportWizard.exporting") + " " + label);
			monitor.beginTask("", 10);
			makeScript(modelBase);
			monitor.worked(1);
			runScript(
				modelBase,
				destination,
				exportZip,
				new SubProgressMonitor(monitor, 9));
		} catch (Exception e) {
			if (writer != null && e.getMessage() != null)
				writer.write(e.getMessage() + System.getProperty("line.separator"));
		} finally {
			try {
				cleanup(model, destination);
			} catch (CoreException e) {
			}
			monitor.done();
		}

	}

	public IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	private void makeScript(IPluginModelBase model) throws CoreException {
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
		runner.addBuildListener("org.eclipse.pde.internal.ui.ant.ExportBuildListener");
		runner.addUserProperties(createProperties(destination));
		runner.setAntHome(model.getInstallLocation());
		runner.setBuildFileLocation(
			model.getInstallLocation()
				+ Path.SEPARATOR
				+ MainPreferencePage.getBuildScriptName());
		runner.setMessageOutputLevel(Project.MSG_ERR);
		runner.run(monitor);
	}

}
