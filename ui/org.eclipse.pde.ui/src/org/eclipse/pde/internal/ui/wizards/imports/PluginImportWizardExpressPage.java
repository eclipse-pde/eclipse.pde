/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.ArrayList;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class PluginImportWizardExpressPage extends BaseImportWizardSecondPage {

	private TablePart fTablePart;
	private IStructuredSelection fInitialSelection;
	private Label fCounterLabel;

	class PluginContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
			ArrayList result = new ArrayList();
			for (int i = 0; i < projects.length; i++) {
				if (WorkspaceModelManager.isPluginProject(projects[i]) && !WorkspaceModelManager.isBinaryProject(projects[i])) {
					IPluginModelBase model = PluginRegistry.findModel(projects[i]);
					if (model != null && model.getBundleDescription() != null)
						result.add(model);
				}
			}
			return result.toArray();
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel, String[] buttonLabels) {
			super(mainLabel, buttonLabels);
		}

		public void updateCounter(int count) {
			super.updateCounter(count);
		}

		protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
			StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
			return viewer;
		}

		protected void elementChecked(Object element, boolean checked) {
			super.elementChecked(element, checked);
			pageChanged();
		}

		protected void handleSelectAll(boolean select) {
			super.handleSelectAll(select);
			pageChanged();
		}
	}

	public PluginImportWizardExpressPage(String pageName, PluginImportWizardFirstPage page, IStructuredSelection selection) {
		super(pageName, page);
		this.fInitialSelection = selection;
		setTitle(PDEUIMessages.ImportWizard_expressPage_title);
		setMessage(PDEUIMessages.ImportWizard_expressPage_desc);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTablePart(container);
		createImportPart(container);

		createButtons(container);

		Composite optionsComp = SWTFactory.createComposite(container, 1, 2, GridData.FILL_HORIZONTAL, 5, 0);
		createComputationsOption(optionsComp);

		fAddFragmentsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});

		initialize();
		setControl(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.PLUGIN_IMPORT_EXPRESS_PAGE);
		Dialog.applyDialogFont(container);
	}

	private void createButtons(Composite container) {
		Composite buttonComp = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginBottom = 10;
		layout.verticalSpacing = 0;
		layout.marginRight = 4;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button selectAll = new Button(buttonComp, SWT.PUSH);
		selectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectAll.setText(PDEUIMessages.WizardCheckboxTablePart_selectAll);
		selectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTablePart.handleSelectAll(true);
				pageChanged();
			}
		});
		Button deselectAll = new Button(buttonComp, SWT.PUSH);
		deselectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		deselectAll.setText(PDEUIMessages.WizardCheckboxTablePart_deselectAll);
		deselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTablePart.handleSelectAll(false);
				pageChanged();
			}
		});

	}

	private Composite createTablePart(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		fTablePart = new TablePart(PDEUIMessages.ImportWizard_expressPage_nonBinary, new String[] {});
		fTablePart.createControl(container);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 225;
		gd.heightHint = 200;
		fTablePart.getControl().setLayoutData(gd);

		CheckboxTableViewer viewer = fTablePart.getTableViewer();
		viewer.setLabelProvider(new PluginImportLabelProvider());
		viewer.setContentProvider(new PluginContentProvider());
		viewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
		viewer.setInput(PDEPlugin.getWorkspace().getRoot());

		return container;
	}

	private void createImportPart(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createImportList(container);
		fCounterLabel = new Label(container, SWT.NONE);
		fCounterLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}

	private void initialize() {
		Object[] items = fInitialSelection.toArray();
		ArrayList list = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			Object item = items[i];
			if (item instanceof IJavaProject) {
				item = ((IJavaProject) item).getProject();
			}
			if (item instanceof IProject) {
				IProject project = (IProject) item;
				if (WorkspaceModelManager.isPluginProject(project) && !WorkspaceModelManager.isBinaryProject(project)) {
					IPluginModelBase model = PluginRegistry.findModel(project);
					if (model != null)
						list.add(model);
				}
			}
		}
		fTablePart.setSelection(list.toArray());
	}

	private void computeModelsToImport() {
		fImportListViewer.getTable().removeAll();

		ArrayList result = new ArrayList();
		Object[] wModels = fTablePart.getSelection();
		for (int i = 0; i < wModels.length; i++) {
			IPluginModelBase model = (IPluginModelBase) wModels[i];
			addDependencies(model, result, fAddFragmentsButton.getSelection());
			addExtraPrerequisites(model, result);
		}

		if (wModels.length > 0) {
			removeSharedModels(result);
		}

		fImportListViewer.add(result.toArray());
	}

	private void removeSharedModels(ArrayList result) {
		IPluginModelBase[] smodels = (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
		for (int i = 0; i < smodels.length; i++) {
			String id = smodels[i].getPluginBase().getId();
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model != null) {
				IResource resource = model.getUnderlyingResource();
				if (resource != null) {
					IProject project = resource.getProject();
					if (!WorkspaceModelManager.isUnsharedProject(project)) {
						result.remove(smodels[i]);
					}
				}
			}
		}
	}

	private void addExtraPrerequisites(IPluginModelBase model, ArrayList result) {
		try {
			IBuildModel buildModel = PluginRegistry.createBuildModel(model);
			if (buildModel == null)
				return;

			IBuildEntry entry = buildModel.getBuild().getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
			if (entry == null)
				return;

			String[] tokens = entry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				Path path = new Path(tokens[i]);
				if (path.segmentCount() >= 2 && path.segment(0).equals("..")) { //$NON-NLS-1$
					for (int j = 0; j < fModels.length; j++) {
						if (fModels[j].getPluginBase().getId().equals(path.segment(1)) && !result.contains(fModels[j])) {
							result.add(fModels[j]);
						}
					}
				}
			}
		} catch (CoreException e) {
		}
	}

	protected void refreshPage() {
		pageChanged();
	}

	protected void pageChanged() {
		computeModelsToImport();
		updateCount();
		setPageComplete(fImportListViewer.getTable().getItemCount() > 0);
		setMessage(PDEUIMessages.ImportWizard_expressPage_desc);
		checkRepositoryAvailability();
	}

	private void updateCount() {
		fCounterLabel.setText(NLS.bind(PDEUIMessages.ImportWizard_expressPage_total, new Integer(fImportListViewer.getTable().getItemCount()).toString()));
		fCounterLabel.getParent().layout();
	}

}
