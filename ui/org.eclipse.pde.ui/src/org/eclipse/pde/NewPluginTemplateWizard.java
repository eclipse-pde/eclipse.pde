package org.eclipse.pde;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.pde.internal.util.CoreUtility;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.core.resources.*;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.pde.internal.model.plugin.*;
import org.eclipse.pde.internal.wizards.templates.*;
import java.util.*;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.pde.internal.wizards.PluginPathUpdater;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.pde.internal.PDEPluginImages;
import org.eclipse.pde.internal.wizards.project.ProjectStructurePage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.update.ui.forms.internal.*;

/**
 * This wizard should be used as a base class for 
 * wizards that provide new plug-in templates. 
 * These wizards are loaded during new plug-in or fragment
 * creation and are used to provide initial
 * content (Java classes, directory structure and
 * extensions).
 * <p>
 * The wizard provides a common first page that will
 * initialize the plug-in itself. This plug-in will
 * be passed on to the templates to generate additional
 * content. After all templates have executed, 
 * the wizard will use the collected list of required
 * plug-ins to set up Java buildpath so that all the
 * generated Java classes can be resolved during the build.
 */

public abstract class NewPluginTemplateWizard
	extends Wizard
	implements IPluginContentWizard {
	private static final String KEY_WTITLE = "PluginCodeGeneratorWizard.title";
	private static final String KEY_WFTITLE = "PluginCodeGeneratorWizard.ftitle";

	private IProjectProvider provider;
	private IPluginStructureData structureData;
	private boolean fragment;
	private FirstTemplateWizardPage firstPage;
	private ITemplateSection[] sections;

	/**
	 * Creates a new template wizard.
	 */

	public NewPluginTemplateWizard() {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_DEFCON_WIZ);
		setNeedsProgressMonitor(true);
		sections = createTemplateSections();
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
		setWindowTitle(PDEPlugin.getResourceString(fragment?KEY_WFTITLE:KEY_WTITLE));
	}


	public abstract ITemplateSection[] createTemplateSections();

	public ITemplateSection[] getTemplateSections() {
		return sections;
	}

	public void addPages() {
		// add the mandatory first page
		firstPage = new FirstTemplateWizardPage(provider, structureData, fragment);
		addPage(firstPage);
		// add template pages
		for (int i = 0; i < sections.length; i++) {
			sections[i].addPages(this);
		}
	}

	public boolean performFinish() {
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
		int totalWork = 4;

		for (int i=0; i<sections.length; i++) {
			totalWork += sections[i].getNumberOfWorkUnits();
		}
		return totalWork;
	}
	
	protected void doFinish(FieldData data, IProgressMonitor monitor) throws CoreException {
		int totalWork = computeTotalWork();
		monitor.beginTask("Generating content...", totalWork);
		IProject project = provider.getProject();
		ProjectStructurePage.createProject(project, provider, monitor); // one step
		monitor.worked(1);
		ProjectStructurePage.createBuildProperties(project, structureData, monitor);
		monitor.worked(1);
		ArrayList dependencies = getDependencies();
		WorkspacePluginModelBase model = firstPage.createPluginManifest(project, data, dependencies, monitor); // one step
		monitor.worked(1);
		setJavaSettings(model, monitor); // one step
		monitor.worked(1);
		executeTemplates(project, model, monitor); // nsteps
		model.save();
	
		IFile file = (IFile)model.getUnderlyingResource();
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getEditorRegistry().setDefaultEditor(
			file,
			PDEPlugin.MANIFEST_EDITOR_ID);
		openPluginFile(file);
	}
	
	private void setJavaSettings(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		try {
			BuildPathUtil.setBuildPath(model, monitor);
		}
		catch (JavaModelException e) {
			String message = e.getMessage();
			IStatus status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, e);
			throw new CoreException(status);
		}
	}

	private ArrayList getDependencies() {
		ArrayList result = new ArrayList();
		IPluginReference [] list = firstPage.getDependencies();
		addDependencies(list, result);
		for (int i=0; i<sections.length; i++) {
			addDependencies(sections[i].getDependencies(), result);
		}
		return result;
	}
	
	private void addDependencies(IPluginReference [] list, ArrayList result) {
		for (int i=0; i<list.length; i++) {
			IPluginReference reference = list[i];
			if (!result.contains(reference))
				result.add(reference);
		}
	}
	
	private void executeTemplates(IProject project, IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		for (int i=0; i<sections.length; i++) {
			ITemplateSection section = sections[i];
			section.execute(project, model, monitor);
		}
	}
	
	protected IEditorInput createEditorInput(IFile file) {
		return new FileEditorInput(file);
	}
	
	private void openPluginFile(final IFile file) {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();

		Display d = ww.getShell().getDisplay();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					String editorId =
						fragment ? PDEPlugin.FRAGMENT_EDITOR_ID : PDEPlugin.MANIFEST_EDITOR_ID;
					IEditorInput input = createEditorInput(file);
					ww.getActivePage().openEditor(input, editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}


}