/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.scanner.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.UIJob;

import com.ibm.icu.text.MessageFormat;

/**
 * The main page for the {@link ApiToolingSetupWizard}
 * 
 * @since 1.0.0
 */
public class ApiToolingSetupWizardPage extends UserInputWizardPage {
	
	private static final String SETTINGS_SECTION = "ApiToolingSetupWizardPage"; //$NON-NLS-1$
	private static final String SETTINGS_REMOVECXML = "remove_componentxml"; //$NON-NLS-1$
	
	private CheckboxTableViewer tableviewer = null;
	private Button removecxml = null;
	private HashSet fProjectsToUpdate = new HashSet();
	
	/**
	 * Constructor
	 * @param pageName
	 */
	protected ApiToolingSetupWizardPage() {
		super(WizardMessages.UpdateJavadocTagsWizardPage_4);
		setTitle(WizardMessages.UpdateJavadocTagsWizardPage_4);
		setMessage(WizardMessages.UpdateJavadocTagsWizardPage_6);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.API_TOOLING_SETUP_WIZARD_PAGE);
		SWTFactory.createWrapLabel(comp, WizardMessages.UpdateJavadocTagsWizardPage_7, 1, 100);
		SWTFactory.createVerticalSpacer(comp, 1);
		SWTFactory.createWrapLabel(comp, WizardMessages.UpdateJavadocTagsWizardPage_8, 1, 50);
		Table table = new Table(comp, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		table.setLayoutData(gd);
		tableviewer =  new CheckboxTableViewer(table);
		tableviewer.setLabelProvider(new WorkbenchLabelProvider());
		tableviewer.setContentProvider(new ArrayContentProvider());
		tableviewer.setInput(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		tableviewer.setComparator(new ViewerComparator());
		tableviewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if(element instanceof IProject) {
					IProject project  = (IProject) element;
					try {
						return (project.hasNature(JavaCore.NATURE_ID) && project.hasNature("org.eclipse.pde.PluginNature"))  //$NON-NLS-1$
						&& !project.hasNature(ApiPlugin.NATURE_ID);
					}
					catch(CoreException ce) {}
				}
				return false;
			}
		});
		tableviewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setPageComplete(pageValid());
			}
		});
		Composite bcomp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_HORIZONTAL | GridData.END, 0, 0);
		Button button = SWTFactory.createPushButton(bcomp, WizardMessages.UpdateJavadocTagsWizardPage_10, null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tableviewer.setAllChecked(true);
				setPageComplete(pageValid());
			}
		});
		button = SWTFactory.createPushButton(bcomp, WizardMessages.UpdateJavadocTagsWizardPage_11, null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tableviewer.setAllChecked(false);
				setPageComplete(pageValid());
			}
		});
		tableviewer.setCheckedElements(getWorkbenchSelection());
		setPageComplete(tableviewer.getCheckedElements().length > 0);
		
		SWTFactory.createVerticalSpacer(comp, 1);
		removecxml = SWTFactory.createCheckButton(comp, WizardMessages.ApiToolingSetupWizardPage_0, 
				null, true, 1);
		
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if(settings != null) {
			removecxml.setSelection(settings.getBoolean(SETTINGS_REMOVECXML));
		}
	}

	/**
	 * @return the current selection from the workbench as an array of objects
	 */
	protected Object[] getWorkbenchSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if(window != null) {
			IWorkbenchPage page  = window.getActivePage();
			if(page != null) {
				IWorkbenchPart part = page.getActivePart();
				if(part != null) {
					IWorkbenchSite site = part.getSite();
					if(site != null) {
						ISelectionProvider provider = site.getSelectionProvider();
						if(provider != null) {
							ISelection selection = provider.getSelection();
							if(selection instanceof IStructuredSelection) {
								Object[] jps = ((IStructuredSelection)provider.getSelection()).toArray();
								ArrayList pjs = new ArrayList();
								for(int i = 0; i < jps.length; i++) {
									if(jps[i] instanceof IAdaptable) {
										IAdaptable adapt = (IAdaptable) jps[i];
										IProject pj = (IProject) adapt.getAdapter(IProject.class);
										if(pj != null) {
											pjs.add(pj);
										}
									}
								}
								return pjs.toArray();
							}
						}
					}
				}
			}
		}
		return new Object[0];
	}
	
	/**
	 * @return if the page is valid or not, this method also sets error messages
	 */
	protected boolean pageValid() {
		if(tableviewer.getCheckedElements().length < 1) {
			setErrorMessage(WizardMessages.UpdateJavadocTagsWizardPage_12);
			return false;
		}
		setErrorMessage(null);
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		//TODO if the user goes back and forth (and makes changes to the selected listing of projects) 
		//we should remove those files from the mapping
		JavadocTagRefactoring refactoring = (JavadocTagRefactoring) getRefactoring();
		refactoring.setChangeInput(collectTagUpdates());
		return super.getNextPage();
	}
	
	/**
	 * @return the mapping of text edits to the IFile they occur on
	 */
	private HashMap collectTagUpdates() {
		final HashMap map = new HashMap();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				Object[] projects = tableviewer.getCheckedElements();
				fProjectsToUpdate.clear();
				fProjectsToUpdate.addAll(Arrays.asList(projects));
				IProject project = null;
				monitor.beginTask(WizardMessages.ApiToolingSetupWizardPage_7, fProjectsToUpdate.size());
				for(Iterator iter = fProjectsToUpdate.iterator(); iter.hasNext();) {
					try {
						project = (IProject) iter.next();
						monitor.subTask(MessageFormat.format(WizardMessages.ApiToolingSetupWizardPage_4, new String[] {project.getName()}));
						IResource cxml = project.findMember(ApiDescriptionProcessor.COMPONENT_XML_NAME);
						if(cxml != null) {
							ApiDescriptionProcessor.collectTagUpdates(JavaCore.create(project), new File(cxml.getLocationURI()), map);
							if(monitor.isCanceled()) {
								break;
							}
							monitor.worked(1);
						}
					}
					catch (CoreException e) {
						ApiUIPlugin.log(e);
					} 
					catch (IOException e) {
						ApiUIPlugin.log(e);
					}
				}
			}
		};
		try {
			getContainer().run(false, false, op);
		} catch (InvocationTargetException e) {
			ApiUIPlugin.log(e);
		} catch (InterruptedException e) {
			ApiUIPlugin.log(e);
		}
		return map;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#performFinish()
	 */
	protected boolean performFinish() {
		if(fProjectsToUpdate.isEmpty()) {
			collectTagUpdates();
		}
		return super.performFinish();
	}
	
	/**
	 * Converts a single {@link IProject} to have an Api nature
	 * @param projectToConvert
	 * @param monitor
	 * @throws CoreException
	 */
	private void convertProject(IProject projectToConvert, IProgressMonitor monitor) throws CoreException {
		// Do early checks to make sure we can get out fast if we're not setup
		// properly
		if (projectToConvert == null || !projectToConvert.exists()) {
			return;
		}
		// Nature check - do we need to do anything at all?
		if (projectToConvert.hasNature(ApiPlugin.NATURE_ID)) {
			return;
		}
		if(!monitor.isCanceled()) {
			addNatureToProject(projectToConvert, ApiPlugin.NATURE_ID, monitor);
		}
	}
	
	/**
	 * Adds the Api project nature to the given {@link IProject}
	 * @param proj
	 * @param natureId
	 * @param monitor
	 * @throws CoreException
	 */
	private void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures = description.getNatureIds();
		String[] newNatures = new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length] = natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
		if(!monitor.isCanceled()) {
			monitor.worked(1);
		}
	}
	
	/**
	 * Called by the {@link ApiToolingSetupWizard} when finishing the wizard
	 * 
	 * @return true if the page finished normally, false otherwise
	 */
	public boolean finish() {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				boolean remove = removecxml.getSelection();
				IProject project = null;
				for(Iterator iter = fProjectsToUpdate.iterator(); iter.hasNext();) {
					try {
						project = (IProject) iter.next();
						convertProject(project, SubMonitor.convert(monitor, WizardMessages.ApiToolingSetupWizardPage_5, 1));
						if(remove) {
							IResource cxml = project.findMember(ApiDescriptionProcessor.COMPONENT_XML_NAME);
							if(cxml != null) {
								cxml.delete(true, SubMonitor.convert(monitor, WizardMessages.ApiToolingSetupWizardPage_6, 1));
							}
						}
					}
					catch (CoreException e) {
						ApiUIPlugin.log(e);
					} 
				}
				IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
				settings.put(SETTINGS_REMOVECXML, remove);
				notifyNoDefaultProfile();
			}
		};
		try {
			getContainer().run(false, false, op);
		} catch (InvocationTargetException e) {
			ApiUIPlugin.log(e);
		} catch (InterruptedException e) {
			ApiUIPlugin.log(e);
		}
		return true;
	}
	
	/**
	 * Notifies the user that they have no default API profile
	 */
	private void notifyNoDefaultProfile() {
		if(ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile() == null) {
			UIJob job = new UIJob("No default API profile detected")  { //$NON-NLS-1$
				/* (non-Javadoc)
				 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
				 */
				public IStatus runInUIThread(IProgressMonitor monitor) {
					boolean doit = MessageDialog.openQuestion(getShell(), WizardMessages.ApiToolingSetupWizardPage_1, WizardMessages.ApiToolingSetupWizardPage_2 +
					WizardMessages.ApiToolingSetupWizardPage_3);
					if(doit) {
						SWTFactory.showPreferencePage(IApiToolsConstants.ID_PROFILES_PREF_PAGE, null);
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}
	}
}
