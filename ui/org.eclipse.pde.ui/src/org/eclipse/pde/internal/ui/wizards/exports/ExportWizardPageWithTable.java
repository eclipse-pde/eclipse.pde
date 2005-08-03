/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

public abstract class ExportWizardPageWithTable extends BaseExportWizardPage {
	
	protected ExportPart fExportPart;
	private IStructuredSelection fSelection;

	class ExportListProvider extends DefaultContentProvider implements
			IStructuredContentProvider {
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
			case 0:
				handleSelectAll(true);
				break;
			case 1:
				handleSelectAll(false);
				break;
			case 3:
				handleWorkingSets();
			}
		}
	}

	public ExportWizardPageWithTable(IStructuredSelection selection, String name, String choiceLabel) {
		super(name);
		fSelection = selection;
		fExportPart =
			new ExportPart(
				choiceLabel,
				new String[] {
					PDEUIMessages.WizardCheckboxTablePart_selectAll,
					PDEUIMessages.WizardCheckboxTablePart_deselectAll,
					null,
					PDEUIMessages.ExportWizard_workingSet }); 
		setDescription(PDEUIMessages.ExportWizard_Plugin_description); 
	}
	
	protected void createTopSection(Composite parent) {
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
		viewer.setSorter(ListUtil.PLUGIN_SORTER);
		fExportPart.getTableViewer().setInput(PDECore.getDefault().getWorkspaceModelManager());
	}
	
	protected void initializeTopSection() {
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
	
	protected abstract boolean isValidModel(IModel model);
	
	protected abstract IModel findModelFor(IAdaptable object);
	
	public Object[] getSelectedItems() {
		return fExportPart.getSelection();
	}
	
	protected void validateTopSection() {
		setMessage(fExportPart.getSelectionCount() > 0 
						? null 
						: PDEUIMessages.ExportWizard_status_noselection); 
	}
	
	public abstract Object[] getListElements();
	
	protected void pageChanged() {
		super.pageChanged();
		String message = fExportPart.getSelectionCount() > 0 
							? null 
				            : PDEUIMessages.ExportWizard_status_noselection; 
		if (message == null) {
			message = validateBottomSections();
		}
		setMessage(message);
		setPageComplete(message == null);
	}

}
