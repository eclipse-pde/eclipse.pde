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
package org.eclipse.pde.internal.ui.osgi.templates;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.IExternalModelManager;
import org.eclipse.pde.core.osgi.bundle.IBundle;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.osgi.bundle.BundlePluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.codegen.JavaCodeGenerator;
import org.eclipse.pde.internal.ui.editor.manifest.ManifestEditor;
import org.eclipse.pde.internal.ui.osgi.wizards.project.BundleProjectStructurePage;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * <p>
 * <b>Note:</b> This class is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */

public abstract class AbstractNewBundleTemplateWizard
	extends Wizard
	implements IPluginContentWizard {
	private static final String KEY_WTITLE = "PluginCodeGeneratorWizard.title";
	private static final String KEY_WFTITLE =
		"PluginCodeGeneratorWizard.ftitle";
	private static final String KEY_MISSING =
		"PluginCodeGeneratorWizard.missing";
	private static final String KEY_FMISSING =
		"PluginCoreGeneratorWizard.fmissing";
	private static final String KEY_GENERATING =
		"PluginCoreGeneratorWizard.generating";

	private IProjectProvider provider;
	private IPluginStructureData structureData;
	private boolean fragment;
	private FirstBundleTemplateWizardPage firstPage;
	private ITemplateSection[] activeSections;
	private IConfigurationElement config;

	/**
	 * Creates a new template wizard.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */

	public AbstractNewBundleTemplateWizard() {
		super();
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWBEXPRJ_WIZ);
		setNeedsProgressMonitor(true);
	}

	/*
	 * 
	 * @see IPluginContentWizard#init(IProjectProvider, IPluginStructureData, boolean)
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void init(
		IProjectProvider provider,
		IPluginStructureData structureData,
		boolean fragment,
		IConfigurationElement config) {
		this.provider = provider;
		this.structureData = structureData;
		this.fragment = fragment;
		this.config = config;
		setWindowTitle(
			PDEPlugin.getResourceString(fragment ? KEY_WFTITLE : KEY_WTITLE));
	}

	/**
	 * Subclasses must implement this method by returning an array
	 * of templates that will contribute pages to this wizard.
	 * @return an array of template sections that will contribute
	 * pages to this wizard.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	protected abstract ITemplateSection[] getTemplateSections();
	/**
	 * This wizard adds a mandatory first page. Subclasses implement
	 * this method to add additional pages to the wizard.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	protected abstract void addAdditionalPages();

	/**
	 * Implements wizard method. Subclasses cannot override it.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public final void addPages() {
		// add the mandatory first page
		firstPage =
			new FirstBundleTemplateWizardPage(provider, structureData, fragment);
		addPage(firstPage);
		addAdditionalPages();
	}
	/**
	 * Implements required wizard method. Subclasses cannot override it.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public final boolean performFinish() {
		activeSections = getTemplateSections();
		final IFieldData data = firstPage.createFieldData(activeSections);
		for (int i =0 ; i<activeSections.length; i++){
			if (activeSections[i].getLabel().equals("Preference Page")){
				((PreferencePageTemplate)activeSections[i]).addDefaultOption(data.isThisCheck());
				continue;
			}
		}	
		
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor)
				throws InterruptedException {
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
			return false;
		}
		return true;
	}

	// private methods

	private int computeTotalWork() {
		int totalWork = 5;

		for (int i = 0; i < activeSections.length; i++) {
			totalWork += activeSections[i].getNumberOfWorkUnits();
		}
		return totalWork;
	}

	private void doFinish(IFieldData data, IProgressMonitor monitor)
		throws CoreException, InterruptedException {
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_GENERATING),
			computeTotalWork());
			
		ArrayList dependencies = getDependencies();
		if (!verifyPluginPath(dependencies))
			throw new InterruptedException();
			
		IProject project = provider.getProject();

		BundleProjectStructurePage.createProject(project, provider, structureData, monitor);
		// one step
		monitor.worked(1);
		
		BundleProjectStructurePage.createBuildProperties(
			project,
			structureData,
			data.isFragment(),
			monitor);
		monitor.worked(1);
		
		BundlePluginModelBase model =
			firstPage.createPluginManifest(
				project,
				data,
				dependencies,
				monitor);
		// one step
		monitor.worked(1);

		if (structureData.getRuntimeLibraryName() != null) {
			setJavaSettings(model, new SubProgressMonitor(monitor, 1)); // one step
		}
		ArrayList generatedPackages = new ArrayList();
		if (data.getClassName()!=null)
			generatedPackages.add(structureData.getPluginId());
		executeTemplates(project, model, generatedPackages, monitor); // nsteps
		setPackages(model, generatedPackages);
		JavaCodeGenerator.ensureFoldersExist(project, "META-INF", "/");
		model.save();
		saveTemplateFile(project, monitor); // one step
		monitor.worked(1);
		IWorkbench workbench = PlatformUI.getWorkbench();
		IFile manifestFile = (IFile)model.getBundleModel().getUnderlyingResource();
		IFile extensionsFile = (IFile)model.getExtensionsModel().getUnderlyingResource();
		IEditorRegistry ereg = workbench.getEditorRegistry();
		IEditorDescriptor meditorId = IDE.getDefaultEditor(manifestFile);
		if (meditorId==null) meditorId = ereg.getDefaultEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		IEditorDescriptor exeditorId = IDE.getDefaultEditor(extensionsFile);
		if (exeditorId==null) exeditorId = ereg.getDefaultEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		if (extensionsFile.exists())
			openFile(extensionsFile, exeditorId.getId(), false);
		openFile(manifestFile, meditorId.getId(), true);
	}

	private void setJavaSettings(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		boolean useContainers = PDEPlugin.getUseClasspathContainers();
		ClasspathUtil.setClasspath(model, useContainers, null, monitor);
	}
	
	private void setPackages(BundlePluginModelBase model, ArrayList generatedPackages) {
		if (generatedPackages.size()==0) return;
		StringBuffer provides = new StringBuffer();
		for (int i=0; i<generatedPackages.size(); i++) {
			String packageName = (String)generatedPackages.get(i);
			if (i>0) provides.append(", ");
			provides.append(packageName);
		}
		IBundle bundle = model.getBundleModel().getBundle();
		try {
			bundle.setHeader(IBundle.KEY_PROVIDE_PACKAGE, provides.toString());
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
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

	private boolean verifyPluginPath(ArrayList dependencies) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ArrayList matches = new ArrayList();
		boolean workspaceModels = false;

		for (int i = 0; i < dependencies.size(); i++) {
			IPluginReference ref = (IPluginReference) dependencies.get(i);
			IPluginModelBase model =
				manager.findPlugin(
					ref.getId(),
					ref.getVersion(),
					ref.getMatch());
			if (model != null) {
				if (model.getUnderlyingResource() != null) {
					workspaceModels = true;
					break;
				} else if (model.isEnabled() == false) {
					// disabled external model
					matches.add(model);
				}
			}
		}
		if (!workspaceModels && matches.size() > 0) {
			// enable
			final boolean[] result = new boolean[1];
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					if (askToEnable()) {
						IExternalModelManager mng =
							PDECore.getDefault().getExternalModelManager();
						mng.enableAll();
						result[0] = true;
					} else
						result[0] = false;
				}
			});
			return result[0];

		}
		return true;
	}

	private boolean askToEnable() {
		String title = getWindowTitle();
		String message =
			PDEPlugin.getResourceString(fragment ? KEY_FMISSING : KEY_MISSING);
			MessageDialog dialog =
				new MessageDialog(
					getShell(),
					title,
					null,
		// accept the default window icon
	message,
		MessageDialog.INFORMATION,
		new String[] {
			IDialogConstants.OK_LABEL,
			/*IDialogConstants.CANCEL_LABEL*/ },
		0);
		// OK is the default
		return dialog.open() == 0;
	}

	private void executeTemplates(
		IProject project,
		IPluginModelBase model,
		ArrayList generatedPackages,
		IProgressMonitor monitor)
		throws CoreException {
		for (int i = 0; i < activeSections.length; i++) {
			ITemplateSection section = activeSections[i];
			section.execute(project, model, monitor);
			if (section instanceof AbstractTemplateSection) {
				AbstractTemplateSection asection = (AbstractTemplateSection)section;
				Object packageValue = asection.getValue(AbstractTemplateSection.KEY_PACKAGE_NAME);
				if (packageValue!=null) {
					String packageName = packageValue.toString();
					if (packageName.length()>0 && !generatedPackages.contains(packageName))
						generatedPackages.add(packageName);
				}
			}
		}
	}

	private void writeTemplateFile(PrintWriter writer) {
		String indent = "   ";
		// open
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<form>");
		if (activeSections.length > 0) {
			// add the standard prolog
			writer.println(
				indent
					+ PDEPlugin.getResourceString(
						"ManifestEditor.TemplatePage.intro"));
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
			indent
				+ PDEPlugin.getResourceString(
					"ManifestEditor.TemplatePage.common"));
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

	private IEditorInput createEditorInput(IFile file) {
		return new TemplateEditorInput(file, ManifestEditor.TEMPLATE_PAGE);
	}

	private void openFile(final IFile file, final String editorId, final boolean selectReveal) {
		BasicNewProjectResourceWizard.updatePerspective(config);		
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();

		final IWorkbenchPage page = ww.getActivePage();
		if (page == null)
			return;
		Display d = ww.getShell().getDisplay();
		final IWorkbenchPart focusPart = page.getActivePart();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					if (focusPart instanceof ISetSelectionTarget) {
						if (selectReveal) {
						ISelection selection = new StructuredSelection(file);
						((ISetSelectionTarget) focusPart).selectReveal(
							selection);
						}
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