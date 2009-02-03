/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

public abstract class BaseExportWizardPage extends AbstractExportWizardPage {

	protected ExportPart fExportPart;
	private IStructuredSelection fSelection;
	protected ExportDestinationTab fDestinationTab;
	protected ExportOptionsTab fOptionsTab;
	protected JARSigningTab fJARSiginingTab;
	protected TabFolder fTabFolder;

	class ExportListProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getListElements();
		}
	}

	class ExportPart extends WizardCheckboxTablePart {
		public ExportPart(String label, String[] buttonLabels) {
			super(label, buttonLabels);
		}

		public void updateCounter(int count) {
			super.updateCounter(count);
			pageChanged();
		}

		protected void buttonSelected(Button button, int index) {
			switch (index) {
				case 0 :
					handleSelectAll(true);
					break;
				case 1 :
					handleSelectAll(false);
					break;
				case 2 :
					handleWorkingSets();
			}
		}
	}

	public BaseExportWizardPage(IStructuredSelection selection, String name, String choiceLabel) {
		super(name);
		fSelection = selection;
		fExportPart = new ExportPart(choiceLabel, new String[] {PDEUIMessages.WizardCheckboxTablePart_selectAll, PDEUIMessages.WizardCheckboxTablePart_deselectAll, PDEUIMessages.ExportWizard_workingSet});
		setDescription(PDEUIMessages.ExportWizard_Plugin_description);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		createViewer(container);

		fTabFolder = new TabFolder(container, SWT.NONE);
		fTabFolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		createTabs(fTabFolder, getDialogSettings());

		initializeViewer();
		if (getErrorMessage() != null) {
			setMessage(getErrorMessage());
			setErrorMessage(null);
		}
		setControl(container);
		hookHelpContext(container);
		Dialog.applyDialogFont(container);
	}

	protected void createTabs(TabFolder folder, IDialogSettings settings) {
		createDestinationTab(folder);
		createOptionsTab(folder);

		fDestinationTab.initialize(settings);
		fOptionsTab.initialize(settings);
		if (fOptionsTab.useJARFormat()) {
			createJARSigningTab(folder);
			fJARSiginingTab.initialize(settings);
		}
	}

	protected void createDestinationTab(TabFolder folder) {
		fDestinationTab = new ExportDestinationTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setControl(fDestinationTab.createControl(folder));
		item.setText(PDEUIMessages.ExportWizard_destination);
	}

	protected void createOptionsTab(TabFolder folder) {
		fOptionsTab = new ExportOptionsTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setControl(fOptionsTab.createControl(folder));
		item.setText(PDEUIMessages.ExportWizard_options);
	}

	protected void createJARSigningTab(TabFolder folder) {
		fJARSiginingTab = new JARSigningTab(this);
		TabItem item = new TabItem(folder, SWT.NONE);
		item.setControl(fJARSiginingTab.createControl(folder));
		item.setText(PDEUIMessages.AdvancedPluginExportPage_signJar);
	}

	protected void createViewer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		fExportPart.createControl(composite);
		GridData gd = (GridData) fExportPart.getControl().getLayoutData();
		gd.heightHint = 125;
		gd.widthHint = 150;
		gd.horizontalSpan = 2;

		TableViewer viewer = fExportPart.getTableViewer();
		viewer.setContentProvider(new ExportListProvider());
		viewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		viewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (event.getSource() instanceof CheckboxTableViewer) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					((CheckboxTableViewer) event.getSource()).setChecked(selection.getFirstElement(), !((CheckboxTableViewer) event.getSource()).getChecked(selection.getFirstElement()));
					fExportPart.updateCounter(((CheckboxTableViewer) event.getSource()).getCheckedElements().length);
				}
			}
		});
		fExportPart.getTableViewer().setInput(getInput());
	}

	protected abstract Object getInput();

	protected void initializeViewer() {
		Object[] elems = fSelection.toArray();
		ArrayList checked = new ArrayList(elems.length);

		for (int i = 0; i < elems.length; i++) {
			Object elem = elems[i];
			IProject project = null;

			if (elem instanceof IFile) {
				IFile file = (IFile) elem;
				project = file.getProject();
			} else if (elem instanceof IProject) {
				project = (IProject) elem;
			} else if (elem instanceof IJavaProject) {
				project = ((IJavaProject) elem).getProject();
			}
			if (project != null) {
				IModel model = findModelFor(project);
				if (model != null && !checked.contains(model)) {
					checked.add(model);
				}
			}
		}
		fExportPart.setSelection(checked.toArray());
		if (checked.size() > 0)
			fExportPart.getTableViewer().reveal(checked.get(0));
	}

	private void handleWorkingSets() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(getShell(), true);
		if (dialog.open() == Window.OK) {
			ArrayList models = new ArrayList();
			IWorkingSet[] workingSets = dialog.getSelection();
			for (int i = 0; i < workingSets.length; i++) {
				IAdaptable[] elements = workingSets[i].getElements();
				for (int j = 0; j < elements.length; j++) {
					IModel model = findModelFor(elements[j]);
					if (isValidModel(model)) {
						models.add(model);
					}
				}
			}
			fExportPart.setSelection(models.toArray());
		}
	}

	public Object[] getSelectedItems() {
		return fExportPart.getSelection();
	}

	protected void pageChanged() {
		if (getMessage() != null)
			setMessage(null);
		if (fOptionsTab != null) {
			String path = fOptionsTab.getAntBuildFileName();
			String warningMessage = null;
			if (path != null && path.length() > 0 && "build.xml".equals(new Path(path).lastSegment())) //$NON-NLS-1$
				warningMessage = PDEUIMessages.ExportOptionsTab_antReservedMessage;
			setMessage(warningMessage, IMessageProvider.WARNING);
		}
		String error = fExportPart.getSelectionCount() > 0 ? null : PDEUIMessages.ExportWizard_status_noselection;
		if (error == null)
			error = validateTabs();
		setErrorMessage(error);
		setPageComplete(error == null);
	}

	protected String validateTabs() {
		String message = fDestinationTab.validate();
		if (message == null)
			message = fOptionsTab.validate();
		if (message == null && fTabFolder.getItemCount() > 2)
			message = fJARSiginingTab.validate();
		return message;
	}

	protected abstract void hookHelpContext(Control control);

	protected abstract boolean isValidModel(IModel model);

	public abstract Object[] getListElements();

	protected abstract IModel findModelFor(IAdaptable object);

	protected void saveSettings(IDialogSettings settings) {
		fDestinationTab.saveSettings(settings);
		fOptionsTab.saveSettings(settings);
		if (fJARSiginingTab != null)
			fJARSiginingTab.saveSettings(settings);
	}

	protected boolean doExportToDirectory() {
		return fDestinationTab.doExportToDirectory();
	}

	protected String getFileName() {
		return fDestinationTab.getFileName();
	}

	protected String getDestination() {
		return fDestinationTab.getDestination();
	}

	protected boolean doInstall() {
		return fDestinationTab.doInstall();
	}

	protected boolean doExportSource() {
		return fOptionsTab.doExportSource();
	}

	protected boolean doExportSourceBundles() {
		return fOptionsTab.doExportSourceBundles();
	}

	protected boolean useJARFormat() {
		return fOptionsTab.useJARFormat();
	}

	protected boolean allowBinaryCycles() {
		return fOptionsTab.doBinaryCycles();
	}

	protected boolean useWorkspaceCompiledClasses() {
		return fOptionsTab.useWorkspaceCompiledClasses();
	}

	protected boolean doGenerateAntFile() {
		return fOptionsTab.doGenerateAntFile();
	}

	protected String getAntBuildFileName() {
		return fOptionsTab.getAntBuildFileName();
	}

	protected String getQualifier() {
		return fOptionsTab.getQualifier();
	}

	protected String[] getSigningInfo() {
		if (fJARSiginingTab == null || fTabFolder.getItemCount() < 3)
			return null;
		return fJARSiginingTab.getSigningInfo();
	}

	protected abstract void adjustAdvancedTabsVisibility();

	protected void adjustJARSigningTabVisibility() {
		IDialogSettings settings = getDialogSettings();
		if (useJARFormat()) {
			if (fTabFolder.getItemCount() < 3) {
				createJARSigningTab(fTabFolder);
				fJARSiginingTab.initialize(settings);
			}
		} else {
			if (fTabFolder.getItemCount() >= 3) {
				fJARSiginingTab.saveSettings(settings);
				fTabFolder.getItem(2).dispose();
			}
		}
	}

}
