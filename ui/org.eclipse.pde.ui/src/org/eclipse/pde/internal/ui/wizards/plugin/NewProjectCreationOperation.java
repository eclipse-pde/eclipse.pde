package org.eclipse.pde.internal.ui.wizards.plugin;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.pluginconversion.*;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.codegen.*;
import org.eclipse.pde.internal.ui.editor.plugin.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.osgi.util.tracker.ServiceTracker;
/**
 * @author melhem
 *  
 */
public class NewProjectCreationOperation extends WorkspaceModifyOperation {
	private IFieldData fData;
	private IProjectProvider fProjectProvider;
	private WorkspacePluginModelBase fModel;
	private PluginClassCodeGenerator fGenerator;
	private IPluginContentWizard fContentWizard;
	private boolean result;
	
	
	public NewProjectCreationOperation(IFieldData data, IProjectProvider provider, IPluginContentWizard contentWizard) {
		fData = data;
		fProjectProvider = provider;
		fContentWizard = contentWizard;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		monitor.beginTask(PDEPlugin.getResourceString("NewProjectCreationOperation.creating"), getNumberOfWorkUnits());
		
		monitor.subTask(PDEPlugin.getResourceString("NewProjectCreationOperation.project"));
		IProject project = createProject();
		monitor.worked(1);
	
		if (project.hasNature(JavaCore.NATURE_ID)) {
			monitor.subTask(PDEPlugin.getResourceString("NewProjectCreationOperation.setClasspath"));
			setClasspath(project, fData);
			monitor.worked(1);
		}
		
		if (fData instanceof IPluginFieldData
				&& ((IPluginFieldData) fData).doGenerateClass()) {
			generateTopLevelPluginClass(project, new SubProgressMonitor(monitor, 1));
		}
		
		monitor.subTask(PDEPlugin.getResourceString("NewProjectCreationOperation.manifestFile"));
		createManifest(project);
		monitor.worked(1);
		
		monitor.subTask(PDEPlugin.getResourceString("NewProjectCreationOperation.buildPropertiesFile"));
		createBuildPropertiesFile(project);
		monitor.worked(1);
		
		boolean contentWizardResult=true;
		
		if (fContentWizard!=null) {
			contentWizardResult = fContentWizard.performFinish(project, fModel, new SubProgressMonitor(monitor, 1));
		}
		fModel.save();
		
		if (fData.hasBundleStructure()) {
			convertToOSGIFormat(project);
			monitor.worked(1);
		}
		monitor.worked(1);
		openFile();
		result = contentWizardResult;
	}
	
	public boolean getResult() {
		return result;
	}
	
	private void generateTopLevelPluginClass(IProject project,
			IProgressMonitor monitor) throws CoreException {
		IFolder folder = project.getFolder(fData.getSourceFolderName());
		IPluginFieldData data = (IPluginFieldData)fData;
		fGenerator = new PluginClassCodeGenerator(folder, data.getClassname(), data);
		fGenerator.generate(monitor);
		monitor.done();
	}
	
	private int getNumberOfWorkUnits() {
		int numUnits = 4;
		if (fData.hasBundleStructure())
			numUnits++;		
		if (fData instanceof IPluginFieldData) {
			IPluginFieldData data = (IPluginFieldData)fData;
			if (data.doGenerateClass())
				numUnits++;
			if (fContentWizard!=null)
				numUnits++;
		}
		return numUnits;
	}
	
	private IProject createProject() throws CoreException {
		IProject project = fProjectProvider.getProject();
		if (!project.exists()) {
			CoreUtility.createProject(project, fProjectProvider
					.getLocationPath(), null);
			project.open(null);
		}
		if (!project.hasNature(PDE.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, null);
		if (!fData.isSimple() && !project.hasNature(JavaCore.NATURE_ID))
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, null);
		if (!fData.isSimple()) {
			IFolder folder = project.getFolder(fData.getSourceFolderName());
			if (!folder.exists())
				CoreUtility.createFolder(folder, true, true, null);
		}
		return project;
	}
	
	private void createManifest(IProject project) throws CoreException {
		if (fData instanceof IFragmentFieldData) {
			fModel = new WorkspaceFragmentModel(project.getFile("fragment.xml"));
		} else {
			fModel = new WorkspacePluginModel(project.getFile("plugin.xml"));
		}
		
		IPluginBase pluginBase = fModel.getPluginBase();
		if (!fData.isLegacy())
			pluginBase.setSchemaVersion("3.0");
		pluginBase.setId(fData.getId());
		pluginBase.setVersion(fData.getVersion());
		pluginBase.setName(fData.getName());
		pluginBase.setProviderName(fData.getProvider());
		if (pluginBase instanceof IFragment) {
			IFragment fragment = (IFragment) pluginBase;
			FragmentFieldData data = (FragmentFieldData) fData;
			fragment.setPluginId(data.getPluginId());
			fragment.setPluginVersion(data.getPluginVersion());
			fragment.setRule(data.getMatch());
		} else {
			if (((IPluginFieldData) fData).doGenerateClass())
				((IPlugin) pluginBase).setClassName(((IPluginFieldData) fData)
						.getClassname());
		}
		if (!fData.isSimple()) {
			IPluginLibrary library = fModel.getPluginFactory().createLibrary();
			library.setName(fData.getLibraryName());
			library.setExported(true);
			pluginBase.add(library);
		}
		
		IPluginReference[] dependencies = getDependencies();
		for (int i = 0; i < dependencies.length; i++) {
			IPluginReference ref = dependencies[i];
			IPluginImport iimport = fModel.getPluginFactory().createImport();
			iimport.setId(ref.getId());
			iimport.setVersion(ref.getVersion());
			iimport.setMatch(ref.getMatch());
			pluginBase.add(iimport);
		}
	}
	
	private void createBuildPropertiesFile(IProject project)
			throws CoreException {
		IFile file = project.getFile("build.properties");
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildModelFactory factory = model.getFactory();
			IBuildEntry binEntry = factory
					.createEntry(IBuildEntry.BIN_INCLUDES);
			binEntry.addToken(fData instanceof IFragmentFieldData
					? "fragment.xml"
					: "plugin.xml");
			if (fData.hasBundleStructure())
				binEntry.addToken("META-INF/");
			if (!fData.isSimple()) {
				binEntry.addToken(fData.getLibraryName());
				if (fContentWizard!=null) {
				String[] files = fContentWizard.getNewFiles();
					for (int j = 0; j < files.length; j++) {
						if (!binEntry.contains(files[j]))
							binEntry.addToken(files[j]);
					}
				}
				
				IBuildEntry entry = factory.createEntry(IBuildEntry.JAR_PREFIX + fData.getLibraryName());
				entry.addToken(new Path(fData.getSourceFolderName()).addTrailingSeparator().toString());
				model.getBuild().add(entry);
				
				entry = factory.createEntry(IBuildEntry.OUTPUT_PREFIX + fData.getLibraryName());
				entry.addToken(new Path(fData.getOutputFolderName()).addTrailingSeparator().toString());
				model.getBuild().add(entry);
			}
			model.getBuild().add(binEntry);
			model.save();
		}
	}
	
	private void convertToOSGIFormat(IProject project) throws CoreException {
		try {
			String filename = (fData instanceof IFragmentFieldData)
					? "fragment.xml"
					: "plugin.xml";
			File outputFile = new File(project.getLocation().append(
					"META-INF/MANIFEST.MF").toOSString());
			File inputFile = new File(project.getLocation().append(filename).toOSString());
			ServiceTracker tracker = new ServiceTracker(PDEPlugin.getDefault()
					.getBundleContext(), PluginConverter.class.getName(), null);
			tracker.open();
			PluginConverter converter = (PluginConverter) tracker.getService();
			converter.convertManifest(inputFile, outputFile, false, null);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			tracker.close();
		} catch (PluginConversionException e) {
		} catch (CoreException e) {
		}
	}
	
	private void setClasspath(IProject project, IFieldData data)
			throws JavaModelException, CoreException {
		// Set output folder
		IJavaProject javaProject = JavaCore.create(project);
		IPath path = project.getFullPath().append(data.getOutputFolderName());
		javaProject.setOutputLocation(path, null);
		// Set classpath
		IClasspathEntry[] entries = new IClasspathEntry[3];
		path = project.getFullPath().append(data.getSourceFolderName());
		entries[0] = JavaCore.newSourceEntry(path);
		entries[1] = ClasspathUtilCore.createContainerEntry();
		entries[2] = ClasspathUtilCore.createJREEntry();
		javaProject.setRawClasspath(entries, null);
	}
	
	private IPluginReference[] getDependencies() {
		ArrayList result = new ArrayList();
		if (fGenerator != null) {
			IPluginReference[] refs = fGenerator.getDependencies();
			for (int i = 0; i < refs.length; i++) {
				result.add(refs[i]);
			}
		}
		if (fContentWizard!=null) {
			IPluginReference[] refs = fContentWizard.getDependencies(fData.isLegacy() ? null : "3.0");
			for (int j = 0; j < refs.length; j++) {
				if (!result.contains(refs[j]))
					result.add(refs[j]);
			}
		}
		return (IPluginReference[]) result.toArray(new IPluginReference[result
				.size()]);
	}
	
	private void openFile() {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
		final IWorkbenchPage page = ww.getActivePage();
		if (page == null)
			return;

		final IWorkbenchPart focusPart = page.getActivePart();
		 ww.getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IFile file = (IFile) fModel.getUnderlyingResource();
				if (focusPart instanceof ISetSelectionTarget) {
					ISelection selection = new StructuredSelection(file);
					((ISetSelectionTarget) focusPart)
							.selectReveal(selection);
				}
				ManifestEditor.openPluginEditor(fModel.getPluginBase());
			}
		});
	}
}
