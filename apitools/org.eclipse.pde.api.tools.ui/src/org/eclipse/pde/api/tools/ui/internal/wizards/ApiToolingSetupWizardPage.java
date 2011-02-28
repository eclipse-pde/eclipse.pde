/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
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
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.pde.api.tools.internal.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.progress.WorkbenchJob;

import com.ibm.icu.text.MessageFormat;

/**
 * The main page for the {@link ApiToolingSetupWizard}
 * 
 * @since 1.0.0
 */
public class ApiToolingSetupWizardPage extends UserInputWizardPage {
	
	/**
	 * Job for updating the filtering on the table viewer
	 */
	class UpdateJob extends WorkbenchJob {
		
		private String pattern = null;
		
		/**
		 * Constructor
		 */
		public UpdateJob() {
			super(WizardMessages.ApiToolingSetupWizardPage_filter_update_job);
			setSystem(true);
		}

		/**
		 * Sets the current text filter to use
		 * @param filter
		 */
		public synchronized void setFilter(String pattern) {
			this.pattern = pattern;
		}
		
		/**
		 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if(tableviewer != null) {
				try {
					tableviewer.getTable().setRedraw(false);
					synchronized (this) {
						filter.setPattern(pattern + '*');
					}
					tableviewer.refresh(true);
					tableviewer.setCheckedElements(checkedset.toArray());
				}
				finally {
					tableviewer.getTable().setRedraw(true);
				}
			}
			return Status.OK_STATUS;
		}
		
	}
	
	/**
	 * Filter for the viewer, uses a text matcher
	 */
	static class StringFilter extends ViewerFilter {

		private String pattern = null;
		StringMatcher matcher = null;
		
		public void setPattern(String pattern) {
			this.pattern = pattern;
		}
		
		/**
		 * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if(pattern == null) {
				return true;
			}
			if(pattern.trim().length() == 0) {
				return true;
			}
			String name = null;
			if(element instanceof IResource) {
				name = ((IResource)element).getName();
			}
			if(name == null) {
				return false;
			}
			matcher = new StringMatcher(pattern, true, false);
			return matcher.match(name, 0, name.length());
		}
		
	}
	
	private static final String SETTINGS_SECTION = "ApiToolingSetupWizardPage"; //$NON-NLS-1$
	private static final String SETTINGS_REMOVECXML = "remove_componentxml"; //$NON-NLS-1$
	
	CheckboxTableViewer tableviewer = null;
	HashSet checkedset = new HashSet();
	Button removecxml = null;
	UpdateJob updatejob = new UpdateJob();
	StringFilter filter = new StringFilter();
	private Text checkcount = null;
	
	/**
	 * Constructor
	 * @param pageName
	 */
	protected ApiToolingSetupWizardPage() {
		super(WizardMessages.UpdateJavadocTagsWizardPage_4);
		setTitle(WizardMessages.UpdateJavadocTagsWizardPage_4);
		setMessage(WizardMessages.UpdateJavadocTagsWizardPage_7);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.API_TOOLING_SETUP_WIZARD_PAGE);
		SWTFactory.createWrapLabel(comp, WizardMessages.UpdateJavadocTagsWizardPage_6, 1, 100);
		SWTFactory.createVerticalSpacer(comp, 1);
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiToolingSetupWizardPage_6, 1, 50);
		
		final Text text = SWTFactory.createText(comp, SWT.BORDER, 1);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updatejob.setFilter(text.getText().trim());
				updatejob.cancel();
				updatejob.schedule();
			}
		});
		text.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					if(tableviewer != null) {
						tableviewer.getTable().setFocus();
					}
				}
			}
		});
		
		SWTFactory.createWrapLabel(comp, WizardMessages.UpdateJavadocTagsWizardPage_8, 1, 50);
		
		Table table = new Table(comp, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		table.setLayoutData(gd);
		tableviewer = new CheckboxTableViewer(table);
		tableviewer.setLabelProvider(new WorkbenchLabelProvider());
		tableviewer.setContentProvider(new ArrayContentProvider());
		tableviewer.setInput(getInputProjects());
		tableviewer.setComparator(new ViewerComparator());
		tableviewer.addFilter(filter);
		tableviewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if(event.getChecked()) {
					checkedset.add(event.getElement());
				}
				else {
					checkedset.remove(event.getElement());
				}
				setPageComplete(pageValid());
			}
		});
		Composite bcomp = SWTFactory.createComposite(comp, 3, 1, GridData.FILL_HORIZONTAL | GridData.END, 0, 0);
		Button button = SWTFactory.createPushButton(bcomp, WizardMessages.UpdateJavadocTagsWizardPage_10, null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tableviewer.setAllChecked(true);
				checkedset.addAll(Arrays.asList(tableviewer.getCheckedElements()));
				setPageComplete(pageValid());
			}
		});
		button = SWTFactory.createPushButton(bcomp, WizardMessages.UpdateJavadocTagsWizardPage_11, null);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tableviewer.setAllChecked(false);
				TableItem[] items = tableviewer.getTable().getItems();
				for(int i = 0; i < items.length; i++) {
					checkedset.remove(items[i].getData());
				}
				setPageComplete(pageValid());
			}
		});

		checkcount = SWTFactory.createText(bcomp, SWT.FLAT | SWT.READ_ONLY, 1, GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		checkcount.setBackground(bcomp.getBackground());
		
		Object[] selected = getWorkbenchSelection();
		if(selected.length > 0) {
			tableviewer.setCheckedElements(selected);
			checkedset.addAll(Arrays.asList(selected));
		}
		setPageComplete(checkedset.size() > 0);
		
		SWTFactory.createVerticalSpacer(comp, 1);
		removecxml = SWTFactory.createCheckButton(comp, WizardMessages.ApiToolingSetupWizardPage_0, null, true, 1);
		
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if(settings != null) {
			removecxml.setSelection(settings.getBoolean(SETTINGS_REMOVECXML));
		}
	}

	/**
	 * @see org.eclipse.jface.wizard.WizardPage#setPageComplete(boolean)
	 */
	public void setPageComplete(boolean complete) {
		super.setPageComplete(complete);
		updateCheckStatus(checkedset.size());
	}
	
	/**
	 * Updates the number of items that have been checked
	 * @param count
	 */
	private void updateCheckStatus(int count) {
		if(checkcount == null) {
			return;
		}
		checkcount.setText(MessageFormat.format(WizardMessages.ApiToolingSetupWizardPage_n_items_checked, new String[] {Integer.toString(count)}));
	}
	
	/**
	 * @return the complete listing of projects in the workspace that could have API Tools set-up
	 * on them
	 * @throws CoreException
	 */
	private IProject[] getInputProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		ArrayList pjs = new ArrayList();
		for(int i = 0; i < projects.length; i++) {
			try {
				IProject project = projects[i];
				if(acceptProject(project)) {
					pjs.add(project);
				}
			}
			catch(CoreException ce) {}
		}
		return (IProject[]) pjs.toArray(new IProject[pjs.size()]);
	}
	
	private boolean acceptProject(IProject project) throws CoreException {
		if(project == null) {
			return false;
		}
		return (project.hasNature(JavaCore.NATURE_ID) && project.hasNature("org.eclipse.pde.PluginNature")) //$NON-NLS-1$
			&& !project.hasNature(ApiPlugin.NATURE_ID)
			&& !Util.isBinaryProject(project);
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
										try {
											if(acceptProject(pj)) {
												pjs.add(pj);
											}
										}
										catch(CoreException ce){}
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
		if(checkedset.size() < 1) {
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
		//always have to collect changes again in the event the user goes back and forth, 
		//as a change cannot ever have more than one parent - EVER
		collectChanges();
		IWizardPage page = super.getNextPage();
		if (page != null) {
			page.setDescription(WizardMessages.ApiToolingSetupWizardPage_5);
		}
		return page;
	}
	
	/**
	 * Creates all of the text edit changes collected from the processor. The collected edits are arranged as multi-edits 
	 * for the one file that they belong to
	 * @param projectchange
	 * @param project
	 * @param cxml
	 */
	void createTagChanges(CompositeChange projectchange, IJavaProject project, File cxml) {
		try {
			HashMap map = new HashMap();
			ApiDescriptionProcessor.collectTagUpdates(project, cxml, map);
			IFile file = null;
			TextFileChange change = null;
			MultiTextEdit multiedit = null;
			HashSet alledits = null;
			TextEdit edit = null;
			for(Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				file = (IFile) entry.getKey();
				change = new TextFileChange(MessageFormat.format(WizardMessages.JavadocTagRefactoring_2, new String[] {file.getName()}), file);
				multiedit = new MultiTextEdit();
				change.setEdit(multiedit);
				alledits = (HashSet) entry.getValue();
				if(alledits != null) {
					for(Iterator iter2 = alledits.iterator(); iter2.hasNext();) {
						edit = (TextEdit) iter2.next();
						multiedit.addChild(edit);
					}
				}
				if(change != null) {
					projectchange.add(change);
				}
			}
		}
		catch (CoreException e) {
			ApiUIPlugin.log(e);
		} 
		catch (IOException e) {
			ApiUIPlugin.log(e);
		}
	}
	
	/**
	 * @return the mapping of text edits to the IFile they occur on
	 */
	private void collectChanges() {
		final ApiToolingSetupRefactoring refactoring = (ApiToolingSetupRefactoring) getRefactoring();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				Object[] projects = checkedset.toArray(new IProject[checkedset.size()]);
				IProject project = null;
				SubMonitor localmonitor = SubMonitor.convert(monitor);
				localmonitor.beginTask(IApiToolsConstants.EMPTY_STRING, projects.length);
				localmonitor.setTaskName(WizardMessages.ApiToolingSetupWizardPage_7);
				refactoring.resetRefactoring();
				boolean remove = removecxml.getSelection();
				CompositeChange pchange = null;
				for(int i = 0; i < projects.length; i++) {
					project = (IProject) projects[i];
					pchange = new CompositeChange(project.getName());
					refactoring.addChange(pchange);
					pchange.add(new ProjectUpdateChange(project));
					localmonitor.subTask(MessageFormat.format(WizardMessages.ApiToolingSetupWizardPage_4, new String[] {project.getName()}));
					IResource cxml = project.findMember(IApiCoreConstants.COMPONENT_XML_NAME);
					if(cxml != null) {
						//collect the changes for doc
						createTagChanges(pchange, JavaCore.create(project), new File(cxml.getLocationURI()));
						if(remove) {
							pchange.add(new DeleteResourceChange(cxml.getFullPath(), true));
						}
					}
					Util.updateMonitor(localmonitor, 1);
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
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#performFinish()
	 */
	protected boolean performFinish() {
		collectChanges();
		return super.performFinish();
	}
	
	/**
	 * Called by the {@link ApiToolingSetupWizard} when finishing the wizard
	 * 
	 * @return true if the page finished normally, false otherwise
	 */
	public boolean finish() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
		settings.put(SETTINGS_REMOVECXML, removecxml.getSelection());
		notifyNoDefaultProfile();
		return true;
	}
	
	/**
	 * Notifies the user that they have no default API profile
	 */
	private void notifyNoDefaultProfile() {
		if(ApiPlugin.getDefault().getApiBaselineManager().getDefaultApiBaseline() == null) {
			UIJob job = new UIJob("No default API profile detected")  { //$NON-NLS-1$
				public IStatus runInUIThread(IProgressMonitor monitor) {
					boolean doit = MessageDialog.openQuestion(getShell(), WizardMessages.ApiToolingSetupWizardPage_1, WizardMessages.ApiToolingSetupWizardPage_2 +
					WizardMessages.ApiToolingSetupWizardPage_3);
					if(doit) {
						SWTFactory.showPreferencePage(getShell(), IApiToolsConstants.ID_BASELINES_PREF_PAGE, null);
					}
					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}
	}
}
