package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.ui.BuildPathUtil;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.pde.ui.IPluginStructureData;
import org.eclipse.pde.ui.IProjectProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.util.CoreUtility;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.core.resources.*;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import java.util.*;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.ui.wizards.PluginPathUpdater;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.wizards.project.ProjectStructurePage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.manifest.ManifestEditor;

public abstract class AbstractNewPluginTemplateWizard
	extends Wizard
	implements IPluginContentWizard {
	private static final String KEY_WTITLE = "PluginCodeGeneratorWizard.title";
	private static final String KEY_WFTITLE = "PluginCodeGeneratorWizard.ftitle";

	private IProjectProvider provider;
	private IPluginStructureData structureData;
	private boolean fragment;
	private FirstTemplateWizardPage firstPage;
	private ITemplateSection[] activeSections;

	/**
	 * Creates a new template wizard.
	 */

	public AbstractNewPluginTemplateWizard() {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_DEFCON_WIZ);
		setNeedsProgressMonitor(true);
	}

	/*
	 * @see IPluginContentWizard#init(IProjectProvider, IPluginStructureData, boolean)
	 */
	public void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment) {
		this.provider = provider;
		this.structureData = structureData;
		this.fragment = fragment;
		setWindowTitle(
			PDEPlugin.getResourceString(fragment ? KEY_WFTITLE : KEY_WTITLE));
	}

	protected abstract ITemplateSection[] getTemplateSections();
	protected abstract void addAdditionalPages();

	public void addPages() {
		// add the mandatory first page
		firstPage = new FirstTemplateWizardPage(provider, structureData, fragment);
		addPage(firstPage);
		addAdditionalPages();
	}

	public boolean performFinish() {
		activeSections = getTemplateSections();
		final FieldData data = firstPage.createFieldData();
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					doFinish(data, monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}

	private int computeTotalWork() {
		int totalWork = 5;

		for (int i = 0; i < activeSections.length; i++) {
			totalWork += activeSections[i].getNumberOfWorkUnits();
		}
		return totalWork;
	}

	protected void doFinish(FieldData data, IProgressMonitor monitor)
		throws CoreException {
		int totalWork = computeTotalWork();
		monitor.beginTask("Generating content...", totalWork);
		IProject project = provider.getProject();
		ProjectStructurePage.createProject(project, provider, monitor); // one step
		monitor.worked(1);
		ProjectStructurePage.createBuildProperties(project, structureData, monitor);
		monitor.worked(1);
		ArrayList dependencies = getDependencies();
		WorkspacePluginModelBase model =
			firstPage.createPluginManifest(project, data, dependencies, monitor);
		// one step
		monitor.worked(1);
		setJavaSettings(model, monitor); // one step
		monitor.worked(1);
		executeTemplates(project, model, monitor); // nsteps
		model.save();
		saveTemplateFile(project, monitor); // one step
		monitor.worked(1);

		IFile file = (IFile) model.getUnderlyingResource();
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getEditorRegistry().setDefaultEditor(
			file,
			PDEPlugin.MANIFEST_EDITOR_ID);
		openPluginFile(file);
	}

	private void setJavaSettings(IPluginModelBase model, IProgressMonitor monitor)
		throws CoreException {
		try {
			BuildPathUtil.setBuildPath(model, monitor);
		} catch (JavaModelException e) {
			String message = e.getMessage();
			IStatus status =
				new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, e);
			throw new CoreException(status);
		}
	}

	private ArrayList getDependencies() {
		ArrayList result = new ArrayList();
		IPluginReference[] list = firstPage.getDependencies();
		addDependencies(list, result);
		for (int i = 0; i < activeSections.length; i++) {
			addDependencies(activeSections[i].getDependencies(), result);
		}
		return result;
	}

	private void addDependencies(IPluginReference[] list, ArrayList result) {
		for (int i = 0; i < list.length; i++) {
			IPluginReference reference = list[i];
			if (!result.contains(reference))
				result.add(reference);
		}
	}

	private void executeTemplates(
		IProject project,
		IPluginModelBase model,
		IProgressMonitor monitor)
		throws CoreException {
		for (int i = 0; i < activeSections.length; i++) {
			ITemplateSection section = activeSections[i];
			section.execute(project, model, monitor);
		}
	}

	protected void writeTemplateFile(PrintWriter writer) {
		String indent = "   ";
		// open
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<form>");
		if (activeSections.length > 0) {
			// add the standard prolog
			writer.println(
				indent + PDEPlugin.getResourceString("ManifestEditor.TemplatePage.intro"));
			// add template section descriptions
			for (int i = 0; i < activeSections.length; i++) {
				ITemplateSection section = activeSections[i];
				String list = "<li style=\"text\" value=\"" + (i + 1) + ".\">";
				writer.println(
					indent
						+ list
						+ "<b>"
						+ section.getLabel()
						+ ".</b>"
						+ section.getDescription()
						+ "</li>");
			}
		}
		// add the standard epilogue
		writer.println(
			indent + PDEPlugin.getResourceString("ManifestEditor.TemplatePage.common"));
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
			ByteArrayInputStream stream =
				new ByteArrayInputStream(contents.getBytes("UTF8"));
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

	public IEditorInput createEditorInput(IFile file) {
		return new TemplateEditorInput(file, ManifestEditor.TEMPLATE_PAGE);
	}

	private void openPluginFile(final IFile file) {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();

		final IWorkbenchPage page = ww.getActivePage();
		if (page == null)
			return;
		Display d = ww.getShell().getDisplay();
		final IWorkbenchPart focusPart = page.getActivePart();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					String editorId =
						fragment ? PDEPlugin.FRAGMENT_EDITOR_ID : PDEPlugin.MANIFEST_EDITOR_ID;

					if (focusPart instanceof ISetSelectionTarget) {
						ISelection selection = new StructuredSelection(file);
						((ISetSelectionTarget) focusPart).selectReveal(selection);
					}
					IEditorInput input = createEditorInput(file);
					ww.getActivePage().openEditor(input, editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}

}