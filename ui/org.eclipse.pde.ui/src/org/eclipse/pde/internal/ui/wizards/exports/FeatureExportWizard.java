package org.eclipse.pde.internal.ui.wizards.exports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

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
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
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


	protected BaseExportWizardPage createPage1() {
		return new FeatureExportWizardPage(getSelection());
	}

	protected HashMap createProperties(String destination) {
		HashMap map = new HashMap(5);
		map.put("build.result.folder", destination + Path.SEPARATOR + "build_result");
		map.put("temp.folder", destination + Path.SEPARATOR + "temp");
		map.put("feature.temp.folder", destination + Path.SEPARATOR + "temp");
		map.put("plugin.destination", destination);
		map.put("feature.destination", destination);
		return map;
	}
	
	protected void doExport(
		boolean exportZip,
		boolean exportSource,
		String destination,
		String zipFileName,
		IModel model,
		IProgressMonitor monitor) {
		IFeatureModel feature = (IFeatureModel) model;
		String label = PDEPlugin.getDefault().getLabelProvider().getObjectText(feature);

		monitor.beginTask("", 10);
		monitor.setTaskName(
			PDEPlugin.getResourceString("ExportWizard.exporting") + " " + label);
		try {
			makeScript(feature);
			monitor.worked(1);
			runScript(
				feature,
				exportZip,
				destination,
				new SubProgressMonitor(monitor, 9));
		} catch (Exception e) {
			if (writer != null && e.getMessage() != null)
				writer.write(e.getMessage() + System.getProperty("line.separator"));
		} finally {
			deleteBuildFiles(feature);
			monitor.done();
		}
	}

	private void deleteBuildFiles(IFeatureModel model) {
		
		deleteBuildFile(model);
		
		IFeatureChild[] children = model.getFeature().getIncludedFeatures();
		for (int i = 0; i < children.length; i++) {
			deleteBuildFiles(children[i].getModel());
		}

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
