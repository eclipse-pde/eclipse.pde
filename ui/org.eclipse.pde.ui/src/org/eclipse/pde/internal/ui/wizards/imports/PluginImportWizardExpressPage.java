/*******************************************************************************
 *  Copyright (c) 2003, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 526283
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

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
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class PluginImportWizardExpressPage extends BaseImportWizardSecondPage {

	private TablePart fTablePart;
	private IStructuredSelection fInitialSelection;
	private Label fCounterLabel;

	class PluginContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
			ArrayList<IPluginModelBase> result = new ArrayList<>();
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

		@Override
		public void updateCounter(int count) {
			super.updateCounter(count);
		}

		@Override
		protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
			StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
			return viewer;
		}

		@Override
		protected void elementChecked(Object element, boolean checked) {
			super.elementChecked(element, checked);
			pageChanged();
		}

		@Override
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

	@Override
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

		fAddFragmentsButton.addSelectionListener(widgetSelectedAdapter(e -> pageChanged()));

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
		selectAll.addSelectionListener(widgetSelectedAdapter(e -> {
			fTablePart.handleSelectAll(true);
			pageChanged();
		}));
		Button deselectAll = new Button(buttonComp, SWT.PUSH);
		deselectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		deselectAll.setText(PDEUIMessages.WizardCheckboxTablePart_deselectAll);
		deselectAll.addSelectionListener(widgetSelectedAdapter(e -> {
			fTablePart.handleSelectAll(false);
			pageChanged();
		}));

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
		ArrayList<IPluginModelBase> list = new ArrayList<>();
		for (Object item : items) {
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

		ArrayList<IPluginModelBase> result = new ArrayList<>();
		Object[] wModels = fTablePart.getSelection();
		for (Object wModel : wModels) {
			IPluginModelBase model = (IPluginModelBase) wModel;
			addDependencies(model, result, fAddFragmentsButton.getSelection());
			addExtraPrerequisites(model, result);
			addUnresolvedImportPackagesModels(model, result, fAddFragmentsButton.getSelection());
		}

		if (wModels.length > 0) {
			removeSharedModels(result);
		}

		fImportListViewer.add(result.toArray());
	}

	private void removeSharedModels(ArrayList<IPluginModelBase> result) {
		IPluginModelBase[] smodels = result.toArray(new IPluginModelBase[result.size()]);
		for (IPluginModelBase smodel : smodels) {
			String id = smodel.getPluginBase().getId();
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model != null) {
				IResource resource = model.getUnderlyingResource();
				if (resource != null) {
					IProject project = resource.getProject();
					if (!WorkspaceModelManager.isUnsharedProject(project)) {
						result.remove(smodel);
					}
				}
			}
		}
	}

	private void addExtraPrerequisites(IPluginModelBase model, ArrayList<IPluginModelBase> result) {
		try {
			IBuildModel buildModel = PluginRegistry.createBuildModel(model);
			if (buildModel == null)
				return;

			IBuildEntry entry = buildModel.getBuild().getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
			if (entry == null)
				return;

			String[] tokens = entry.getTokens();
			for (String token : tokens) {
				Path path = new Path(token);
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

	@Override
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
		fCounterLabel.setText(NLS.bind(PDEUIMessages.ImportWizard_expressPage_total,
				Integer.toString(fImportListViewer.getTable().getItemCount())));
		fCounterLabel.getParent().layout();
	}

}
