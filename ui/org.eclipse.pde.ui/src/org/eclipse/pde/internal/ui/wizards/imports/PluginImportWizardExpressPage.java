package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

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
			WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
			for (int i = 0; i < projects.length; i++) {
				if (projects[i].isOpen()
					&& WorkspaceModelManager.isPluginProject(projects[i])
					&& !WorkspaceModelManager.isBinaryPluginProject(projects[i])) {
					result.add(manager.getWorkspaceModel(projects[i]));
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
			FormWidgetFactory factory) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, factory);
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
		setTitle(PDEPlugin.getResourceString("ImportWizard.expressPage.title"));
		setMessage(PDEPlugin.getResourceString("ImportWizard.expressPage.desc"));
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

		implicitButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageChanged();
			}
		});
		
		addFragments.addSelectionListener(new SelectionAdapter() {
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
				PDEPlugin.getResourceString("ImportWizard.expressPage.nonBinary"),
				new String[] {
					PDEPlugin.getResourceString(TablePart.KEY_SELECT_ALL),
					PDEPlugin.getResourceString(TablePart.KEY_DESELECT_ALL)});
		tablePart.createControl(container);
		tablePart.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		
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
		container.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		createImportList(container);
		counterLabel = new Label(container, SWT.NONE);
		counterLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
	}
	
	private void initialize() {
		Object[] items = initialSelection.toArray();
		ArrayList list = new ArrayList();
		WorkspaceModelManager manager = PDECore.getDefault().getWorkspaceModelManager();
		
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
					list.add(manager.getWorkspaceModel(project));
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
			addDependencies(model, result, true);
			addExtraPrerequisites(model, result);
		}
		
		if (wModels.length > 0) {
			if (!(wModels.length == 1
				&& ((IPluginModelBase) wModels[0]).getPluginBase().getId().equals(
					"org.eclipse.core.boot"))
				&& implicitButton.isVisible()
				&& implicitButton.getSelection())
				addImplicitDependencies(result);
			removeCheckedModels(result);
		}
		
		importListViewer.add(result.toArray());
	}
	
	private void removeCheckedModels(ArrayList result) {
		HashSet set = new HashSet();
		Object[] wModels = tablePart.getSelection();
		for (int i = 0; i < wModels.length; i++) {
			set.add(((IPluginModelBase)wModels[i]).getPluginBase().getId());
		}
		IPluginModelBase[] smodels = (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
		for (int i = 0; i < smodels.length; i++) {
			if (set.contains(smodels[i].getPluginBase().getId()))
				result.remove(smodels[i]);
		}
	}

	private void addExtraPrerequisites(IPluginModelBase model, ArrayList result) {
		try {
			IBuildModel buildModel = model.getBuildModel();
			if (buildModel == null) {
				IFile buildFile = model.getUnderlyingResource().getProject().getFile("build.properties");
				if (buildFile.exists()) {
					buildModel = new WorkspaceBuildModel(buildFile);
					buildModel.load();
				}
			}
			if (buildModel == null)
				return;
				
			IBuildEntry entry = buildModel.getBuild().getEntry("jars.extra.classpath");
			if (entry == null)
				return;
				
			String[] tokens = entry.getTokens();
			for (int i = 0; i < tokens.length; i++) {
				Path path = new Path(tokens[i]);
				if (path.segmentCount() >= 2 && path.segment(0).equals("..")) {
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
				"ImportWizard.expressPage.total",
				new Integer(importListViewer.getTable().getItemCount()).toString()));
		counterLabel.getParent().layout();
	}


}
