package org.eclipse.pde.internal.ui.wizards.plugin;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.pluginconversion.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.manifest.*;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.part.*;
import org.osgi.util.tracker.*;
/**
 * @author melhem
 *  
 */
public class NewProjectCreationOperation extends WorkspaceModifyOperation {
	private IFieldData fData;
	private IProjectProvider fProjectProvider;
	private WorkspacePluginModelBase fModel;
	private PluginClassCodeGenerator fGenerator;
	private ITemplateSection[] fTemplateSections = new ITemplateSection[0];
	
	
	public NewProjectCreationOperation(IFieldData data, IProjectProvider provider) {
		fData = data;
		fProjectProvider = provider;
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
		
		if (fData instanceof IPluginFieldData) {
			fTemplateSections = ((IPluginFieldData) fData).getTemplateSections();
		}
		
		monitor.subTask(PDEPlugin.getResourceString("NewProjectCreationOperation.manifestFile"));
		createManifest(project);
		monitor.worked(1);
		
		monitor.subTask(PDEPlugin.getResourceString("NewProjectCreationOperation.buildPropertiesFile"));
		createBuildPropertiesFile(project);
		monitor.worked(1);
		
		executeTemplates(project, monitor);
		fModel.save();
		
		if (fData.hasBundleStructure()) {
			convertToOSGIFormat(project);
			monitor.worked(1);
		}
		monitor.worked(1);
		openFile();
	}
	
	private void executeTemplates(IProject project, IProgressMonitor monitor)
			throws CoreException {
		for (int i = 0; i < fTemplateSections.length; i++) {
			fTemplateSections[i].execute(project, fModel,
					new SubProgressMonitor(monitor, 1));
		}
		saveTemplateFile(project, null);
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
			numUnits += 1;		
		if (fData instanceof IPluginFieldData) {
			IPluginFieldData data = (IPluginFieldData)fData;
			if (data.doGenerateClass())
				numUnits += 1;
			numUnits += data.getTemplateSections().length;
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
				for (int i = 0; i < fTemplateSections.length; i++) {
					String[] folders = fTemplateSections[i].getFoldersToInclude();
					for (int j = 0; j < folders.length; j++) {
						if (!binEntry.contains(folders[j]))
							binEntry.addToken(folders[j]);
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
		converter.convertManifest(inputFile, outputFile, false);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
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
		for (int i = 0; i < fTemplateSections.length; i++) {
			IPluginReference[] refs = fTemplateSections[i]
					.getDependencies(fData.isLegacy() ? null : "3.0");
			for (int j = 0; j < refs.length; j++) {
				if (!result.contains(refs[j]))
					result.add(refs[j]);
			}
		}
		return (IPluginReference[]) result.toArray(new IPluginReference[result
				.size()]);
	}
	
	private void writeTemplateFile(PrintWriter writer) {
		String indent = "   ";
		// open
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<form>");
		if (fTemplateSections.length > 0) {
			// add the standard prolog
			writer
					.println(indent
							+ PDEPlugin
									.getResourceString("ManifestEditor.TemplatePage.intro"));
			// add template section descriptions
			for (int i = 0; i < fTemplateSections.length; i++) {
				ITemplateSection section = fTemplateSections[i];
				String list = "<li style=\"text\" value=\"" + (i + 1) + ".\">";
				writer.println(indent + list + "<b>" + section.getLabel()
						+ ".</b>" + section.getDescription() + "</li>");
			}
		}
		// add the standard epilogue
		writer
				.println(indent
						+ PDEPlugin
								.getResourceString("ManifestEditor.TemplatePage.common"));
		// close
		writer.println("</form>");
	}
	
	private void saveTemplateFile(IProject project, IProgressMonitor monitor) {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		writeTemplateFile(writer);
		writer.flush();
		try {
			swriter.close();
		} catch (IOException e) {
		}
		String contents = swriter.toString();
		IFile file = project.getFile(".template");
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(contents
					.getBytes("UTF8"));
			if (file.exists()) {
				file.setContents(stream, false, false, null);
			} else {
				file.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} catch (IOException e) {
		}
	}
	
	private void openFile() {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
		final IWorkbenchPage page = ww.getActivePage();
		if (page == null)
			return;

		final IWorkbenchPart focusPart = page.getActivePart();
		 ww.getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					IFile file = (IFile) fModel.getUnderlyingResource();
					if (focusPart instanceof ISetSelectionTarget) {
						ISelection selection = new StructuredSelection(file);
						((ISetSelectionTarget) focusPart)
								.selectReveal(selection);
					}
					String editorId = fData instanceof IFragmentFieldData
							? PDEPlugin.FRAGMENT_EDITOR_ID
							: PDEPlugin.MANIFEST_EDITOR_ID;
					ww.getActivePage().openEditor(
							new TemplateEditorInput(file,
									ManifestEditor.TEMPLATE_PAGE), editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
}
