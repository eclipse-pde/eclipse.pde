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
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXPRJ_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}

	private void cleanup(IFeatureModel model, String destination, boolean exportChildren) throws CoreException {
		deleteBuildFolders(destination);
		deleteBuildScript(model);
		if (exportChildren) {
			IFeaturePlugin[] plugins = model.getFeature().getPlugins();
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			for (int i = 0; i < plugins.length; i++) {
				ModelEntry entry =
					manager.findEntry(plugins[i].getId(), plugins[i].getVersion());
				if (entry != null) {
					IPluginModelBase modelBase = entry.getActiveModel();
					if (modelBase.getUnderlyingResource() != null) {
						deleteBuildScript(modelBase);
					}

				}

			}
		}
	}

	protected BaseExportWizardPage createPage1() {
		return new FeatureExportWizardPage(getSelection());
	}

	protected void doExport(
		boolean exportZip,
		boolean exportChildren,
		String destination,
		IModel model,
		IProgressMonitor monitor) {
		IFeatureModel feature = (IFeatureModel) model;
		String label = PDEPlugin.getDefault().getLabelProvider().getObjectText(feature);

		monitor.beginTask("", 3);
		monitor.subTask(label);
		try {
			makeScript(feature, exportChildren);
			monitor.worked(1);
			runScript(feature, exportZip, destination, monitor);
			monitor.worked(1);
		} catch (Exception e) {
			if (writer != null)
				writer.write(e.getMessage());
		} finally {
			try {
				cleanup(feature, destination, exportChildren);
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

	private void makeScript(IFeatureModel model, boolean exportChildren) throws CoreException {
		FeatureBuildScriptGenerator generator = new FeatureBuildScriptGenerator();

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

		generator.setGenerateChildrenScript(exportChildren);
		generator.setPluginPath(TargetPlatform.createPluginPath());

		generator.setFeature(model.getFeature().getId());
		generator.generate();

	}

	private void runScript(
		IFeatureModel model,
		boolean exportZip,
		String destination,
		IProgressMonitor monitor)
		throws InvocationTargetException, CoreException {
		AntRunner runner = new AntRunner();
		if (exportZip) {
			runner.setExecutionTargets(new String[] { "build.jars", "zip.distribution" });
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
		runner.run(monitor);

	}

}
