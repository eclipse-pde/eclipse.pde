package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author Wassim Melhem
 */
public class PluginImportWizardExpressPage extends BaseImportWizardSecondPage {

	private TablePart tablePart;
	private IStructuredSelection initialSelection;
	private Label counterLabel;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
			ArrayList result = new ArrayList();
			PluginModelManager manager = PDECore.getDefault().getModelManager();
			for (int i = 0; i < projects.length; i++) {
				if (projects[i].isOpen()
					&& WorkspaceModelManager.isPluginProject(projects[i])
					&& !WorkspaceModelManager.isBinaryPluginProject(projects[i])) {
					IPluginModelBase model = manager.findModel(projects[i]);
					if (model != null)
						result.add(model);
				}
			}
			return result.toArray();
		}
	}
	
	
	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel, String[] buttonLabels) {
			super(mainLabel, buttonLabels);
			setSelectAllIndex(0);
			setDeselectAllIndex(1);
		}
		public void updateCounter(int count) {
			super.updateCounter(count);
		}
		public void buttonSelected(Button button, int index) {
			if (index == 0 || index == 1)
				super.buttonSelected(button, index);
		}
		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormToolkit toolkit) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, toolkit);
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
		this.initialSelection = selection;
		setTitle(PDEPlugin.getResourceString("ImportWizard.expressPage.title")); //$NON-NLS-1$
		setMessage(PDEPlugin.getResourceString("ImportWizard.expressPage.desc")); //$NON-NLS-1$
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 20;
		layout.verticalSpacing = 10;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createTablePart(container);
		createImportPart(container);

		Composite options = createComputationsOption(container);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		options.setLayoutData(gd);

		addFragmentsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
	}
	
	private Composite createTablePart(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		tablePart =
			new TablePart(
				PDEPlugin.getResourceString("ImportWizard.expressPage.nonBinary"), //$NON-NLS-1$
				new String[] {
					PDEPlugin.getResourceString(TablePart.KEY_SELECT_ALL),
					PDEPlugin.getResourceString(TablePart.KEY_DESELECT_ALL)});
		tablePart.createControl(container);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 200;
		gd.widthHint = 225;
		tablePart.getControl().setLayoutData(gd);
		
		CheckboxTableViewer viewer = tablePart.getTableViewer();
		viewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		viewer.setContentProvider(new PluginContentProvider());
		viewer.setSorter(ListUtil.PLUGIN_SORTER);
		viewer.setInput(PDEPlugin.getWorkspace().getRoot());
		
		return container;
	}
	
	private void createImportPart(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createImportList(container);
		counterLabel = new Label(container, SWT.NONE);
		counterLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
	}
	
	private void initialize() {
		Object[] items = initialSelection.toArray();
		ArrayList list = new ArrayList();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		
		for (int i = 0; i < items.length; i++) {
			Object item = items[i];
			if (item instanceof IJavaProject) {
				item = ((IJavaProject)item).getProject();
			}
			if (item instanceof IProject) {
				IProject project = (IProject) item;
				if (project.isOpen()
					&& WorkspaceModelManager.isPluginProject(project)
					&& !WorkspaceModelManager.isBinaryPluginProject(project)) {
					IPluginModelBase model = manager.findModel(project);
					if (model != null)
						list.add(model);
				}
			}
		}
		tablePart.setSelection(list.toArray());
	}
	
	
	private void computeModelsToImport() {
		importListViewer.getTable().removeAll();
		
		ArrayList result = new ArrayList();
		Object[] wModels = tablePart.getSelection();
		for (int i = 0; i < wModels.length; i++) {
			IPluginModelBase model = (IPluginModelBase)wModels[i];
			addDependencies(model, result, addFragmentsButton.getSelection());
			addExtraPrerequisites(model, result);
		}
		
		if (wModels.length > 0) {
			removeSharedModels(result);
		}
		
		importListViewer.add(result.toArray());
	}
	
	private void removeSharedModels(ArrayList result) {
		IPluginModelBase[] smodels = (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		for (int i = 0; i < smodels.length; i++) {
			String id = smodels[i].getPluginBase().getId();
			IPluginModelBase model = manager.findModel(id);
			if (model != null) {
				IResource resource = model.getUnderlyingResource();
				if (resource != null) {
					IProject project = resource.getProject();
					if (!WorkspaceModelManager.isUnsharedPluginProject(project)) {
						result.remove(smodels[i]);
					}
				}
			}
		}
	}

	private void addExtraPrerequisites(IPluginModelBase model, ArrayList result) {
		try {
			IBuildModel buildModel = model.getBuildModel();
			if (buildModel == null) {
				IFile buildFile = model.getUnderlyingResource().getProject().getFile("build.properties"); //$NON-NLS-1$
				if (buildFile.exists()) {
					buildModel = new WorkspaceBuildModel(buildFile);
					buildModel.load();
				}
			}
			if (buildModel == null)
				return;
				
			IBuildEntry entry = buildModel.getBuild().getEntry(IBuildEntry.JARS_EXTRA_CLASSPATH);
			if (entry == null)
				return;
				
			String[] tokens = entry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				Path path = new Path(tokens[i]);
				if (path.segmentCount() >= 2 && path.segment(0).equals("..")) { //$NON-NLS-1$
					for (int j = 0; j < models.length; j++) {
						if (models[j].getPluginBase().getId().equals(path.segment(1))
							&& !result.contains(models[j])) {
							result.add(models[j]);
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
		setPageComplete(importListViewer.getTable().getItemCount() > 0);	
	}

	private void updateCount() {
		counterLabel.setText(
			PDEPlugin.getFormattedMessage(
				"ImportWizard.expressPage.total", //$NON-NLS-1$
				new Integer(importListViewer.getTable().getItemCount()).toString()));
		counterLabel.getParent().layout();
	}


}
