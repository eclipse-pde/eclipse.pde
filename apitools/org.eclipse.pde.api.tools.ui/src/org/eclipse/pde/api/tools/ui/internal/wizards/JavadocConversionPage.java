/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * The wizard page for performing the conversion
 *
 * @since 1.0.500
 */
public class JavadocConversionPage extends UserInputWizardPage {

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
		 *
		 * @param filter
		 */
		public synchronized void setFilter(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (tableviewer != null) {
				try {
					tableviewer.getTable().setRedraw(false);
					synchronized (this) {
						filter.setPattern(pattern + '*');
					}
					tableviewer.refresh(true);
					tableviewer.setCheckedElements(checkedset.toArray());
				} finally {
					tableviewer.getTable().setRedraw(true);
				}
			}
			return Status.OK_STATUS;
		}

	}

	private static final String SETTINGS_SECTION = "JavadocTagConversionWizardPage"; //$NON-NLS-1$
	private static final String SETTINGS_REMOVE_TAGS = "remove_tags"; //$NON-NLS-1$

	Button removetags = null;
	CheckboxTableViewer tableviewer = null;
	HashSet<Object> checkedset = new HashSet<>();
	UpdateJob updatejob = new UpdateJob();
	StringFilter filter = new StringFilter();
	private Text checkcount = null;

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public JavadocConversionPage() {
		super(WizardMessages.JavadocConversionWizard_0);
		setTitle(WizardMessages.JavadocConversionWizard_0);
		setDescription(WizardMessages.JavadocConversionPage_convert_tags_to_annotations_description);
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.JAVADOC_CONVERSION_WIZARD_PAGE);
		SWTFactory.createWrapLabel(comp, WizardMessages.JavadocConversionPage_select_pjs_to_convert, 1, 100);
		SWTFactory.createVerticalSpacer(comp, 1);
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiToolingSetupWizardPage_6, 1, 50);

		final Text text = SWTFactory.createText(comp, SWT.BORDER, 1);
		text.addModifyListener(e -> {
			updatejob.setFilter(text.getText().trim());
			updatejob.cancel();
			updatejob.schedule();
		});
		text.addKeyListener(KeyListener.keyPressedAdapter(e -> {
			if (e.keyCode == SWT.ARROW_DOWN) {
				if (tableviewer != null) {
					tableviewer.getTable().setFocus();
				}
			}
		}));

		SWTFactory.createWrapLabel(comp, WizardMessages.UpdateJavadocTagsWizardPage_8, 1, 50);

		Table table = new Table(comp, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK | SWT.MULTI);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 150;
		table.setLayoutData(gd);
		tableviewer = new CheckboxTableViewer(table);
		tableviewer.setLabelProvider(new WorkbenchLabelProvider());
		tableviewer.setContentProvider(ArrayContentProvider.getInstance());
		IProject[] input = Util.getApiProjectsMinSourceLevel(JavaCore.VERSION_1_5);
		if (input == null) {
			setMessage(WizardMessages.JavadocConversionPage_0, IMessageProvider.WARNING);
		} else {
			tableviewer.setInput(input);
		}
		tableviewer.setComparator(new ViewerComparator());
		tableviewer.addFilter(filter);
		tableviewer.addCheckStateListener(event -> {
			if (event.getChecked()) {
				checkedset.add(event.getElement());
			} else {
				checkedset.remove(event.getElement());
			}
			setPageComplete(pageValid());
		});
		Composite bcomp = SWTFactory.createComposite(comp, 3, 1, GridData.FILL_HORIZONTAL | GridData.END, 0, 0);
		Button button = SWTFactory.createPushButton(bcomp, WizardMessages.UpdateJavadocTagsWizardPage_10, null);
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			tableviewer.setAllChecked(true);
			checkedset.addAll(Arrays.asList(tableviewer.getCheckedElements()));
			setPageComplete(pageValid());
		}));
		button = SWTFactory.createPushButton(bcomp, WizardMessages.UpdateJavadocTagsWizardPage_11, null);
		button.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			tableviewer.setAllChecked(false);
			TableItem[] items = tableviewer.getTable().getItems();
			for (TableItem item : items) {
				checkedset.remove(item.getData());
			}
			setPageComplete(pageValid());
		}));

		checkcount = SWTFactory.createText(bcomp, SWT.FLAT | SWT.READ_ONLY, 1, GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		checkcount.setBackground(bcomp.getBackground());

		Object[] selected = getWorkbenchSelection();
		if (selected.length > 0) {
			tableviewer.setCheckedElements(selected);
			checkedset.addAll(Arrays.asList(selected));
		}
		setPageComplete(tableviewer.getCheckedElements().length > 0);

		SWTFactory.createVerticalSpacer(comp, 1);
		removetags = SWTFactory.createCheckButton(comp, WizardMessages.JavadocConversionPage_delete_tags_during_conversion, null, true, 1);

		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
		if (settings != null) {
			removetags.setSelection(settings.getBoolean(SETTINGS_REMOVE_TAGS));
		}
	}

	/**
	 * @return if the page is valid or not, this method also sets error messages
	 */
	protected boolean pageValid() {
		if (checkedset.size() < 1) {
			setErrorMessage(WizardMessages.UpdateJavadocTagsWizardPage_12);
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	/**
	 * Called by the {@link ApiToolingSetupWizard} when finishing the wizard
	 *
	 * @return true if the page finished normally, false otherwise
	 */
	public boolean finish() {
		IDialogSettings settings = ApiUIPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
		settings.put(SETTINGS_REMOVE_TAGS, removetags.getSelection());
		return true;
	}

	@Override
	public IWizardPage getNextPage() {
		// always have to collect changes again in the event the user goes back
		// and forth,
		// as a change cannot ever have more than one parent - EVER
		JavadocConversionRefactoring refactoring = (JavadocConversionRefactoring) getRefactoring();
		HashSet<IProject> projects = new HashSet<>();
		for (Object object : checkedset) {
			IProject current = (IProject) object;
			projects.add(current);
		}
		refactoring.setProjects(projects);
		refactoring.setRemoveTags(removetags.getSelection());
		IWizardPage page = super.getNextPage();
		if (page != null) {
			page.setDescription(WizardMessages.JavadocConversionPage_changes_required_for_conversion);
		}
		return page;
	}

	@Override
	protected boolean performFinish() {
		// always have to collect changes again in the event the user goes back
		// and forth,
		// as a change cannot ever have more than one parent - EVER
		JavadocConversionRefactoring refactoring = (JavadocConversionRefactoring) getRefactoring();
		HashSet<IProject> projects = new HashSet<>();
		for (Object object : checkedset) {
			IProject current = (IProject) object;
			projects.add(current);
		}
		refactoring.setProjects(projects);
		refactoring.setRemoveTags(removetags.getSelection());
		return super.performFinish();
	}

	/**
	 * @return the current selection from the workbench as an array of objects
	 */
	protected Object[] getWorkbenchSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				IWorkbenchPart part = page.getActivePart();
				if (part != null) {
					IWorkbenchSite site = part.getSite();
					if (site != null) {
						ISelectionProvider provider = site.getSelectionProvider();
						if (provider != null) {
							ISelection selection = provider.getSelection();
							if (selection instanceof IStructuredSelection) {
								Object[] jps = ((IStructuredSelection) provider.getSelection()).toArray();
								ArrayList<IProject> pjs = new ArrayList<>();
								for (Object jp : jps) {
									if (jp instanceof IAdaptable) {
										IAdaptable adapt = (IAdaptable) jp;
										IProject pj = adapt.getAdapter(IProject.class);
										if (Util.isApiProject(pj)) {
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
}
