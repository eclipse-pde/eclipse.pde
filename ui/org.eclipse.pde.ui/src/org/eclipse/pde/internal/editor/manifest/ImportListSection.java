package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;

import java.util.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.custom.*;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.model.plugin.ImportObject;
import org.eclipse.pde.internal.preferences.BuildpathPreferencePage;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.pde.internal.elements.DefaultTableProvider;
import org.eclipse.pde.internal.parts.TablePart;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.pde.internal.wizards.imports.PluginImportWizard;
import org.eclipse.pde.model.*;
import org.eclipse.pde.internal.model.plugin.PluginImport;

public class ImportListSection
	extends TableSection
	implements IModelChangedListener, IModelProviderListener {
	private TableViewer importTable;
	private FormWidgetFactory factory;
	public static final String SECTION_TITLE =
		"ManifestEditor.ImportListSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.ImportListSection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.ImportListSection.fdesc";
	public static final String SECTION_NEW = "ManifestEditor.ImportListSection.new";
	public static final String POPUP_NEW = "Menus.new.label";
	public static final String POPUP_OPEN = "Actions.open.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String KEY_UPDATING_BUILD_PATH =
		"ManifestEditor.ImportListSection.updatingBuildPath";
	public static final String KEY_COMPUTE_BUILD_PATH =
		"ManifestEditor.ImportListSection.updateBuildPath";
	private Vector imports;
	private Action openAction;
	private Action newAction;
	private Action deleteAction;
	private Action buildpathAction;

	class ImportContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (imports == null) {
				createImportObjects();
			}
			return imports.toArray();
		}
		private void createImportObjects() {
			imports = new Vector();
			IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
			IPluginImport[] iimports = model.getPluginBase().getImports();
			for (int i = 0; i < iimports.length; i++) {
				IPluginImport iimport = iimports[i];
				imports.add(new ImportObject(iimport));
			}
		}
	}

	public ImportListSection(ManifestDependenciesPage page) {
		super(page, new String[] { PDEPlugin.getResourceString(SECTION_NEW)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment =
			((ManifestEditor) getFormPage().getEditor()).isFragmentEditor();
		if (fragment)
			setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		Composite container = createClientContainer(parent, 2, factory);
		createViewerPartControl(container, SWT.MULTI, 2, factory);
		TablePart tablePart = getTablePart();
		importTable = tablePart.getTableViewer();

		importTable.setContentProvider(new ImportContentProvider());
		importTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		factory.paintBordersFor(container);
		makeActions();
		return container;
	}

	protected void selectionChanged(IStructuredSelection sel) {
		Object item = sel.getFirstElement();
		fireSelectionNotification(item);
		getFormPage().setSelection(sel);
	}
	protected void handleDoubleClick(IStructuredSelection sel) {
		handleOpen(sel);
	}

	protected void buttonSelected(int index) {
		if (index == 0) {
			handleNew();
		}
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		PDEPlugin.getDefault().getWorkspaceModelManager().removeModelProviderListener(
			this);
		PDEPlugin.getDefault().getExternalModelManager().removeModelProviderListener(
			this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.CUT)) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(IWorkbenchActionConstants.PASTE)) {
			doPaste();
			return true;
		}
		return false;
	}

	public void expandTo(Object object) {
		if (object instanceof IPluginImport) {
			ImportObject iobj = new ImportObject((IPluginImport) object);
			importTable.setSelection(new StructuredSelection(iobj), true);
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = importTable.getSelection();
		manager.add(newAction);
		manager.add(new Separator());
		if (!selection.isEmpty()) {
			manager.add(openAction);
			manager.add(deleteAction);
			manager.add(new Separator());
		}
		((DependenciesForm) getFormPage().getForm()).fillContextMenu(manager);
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) importTable.getSelection();

		if (ssel.isEmpty())
			return;
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase pluginBase = model.getPluginBase();

		try {
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				ImportObject iobj = (ImportObject) iter.next();
				pluginBase.remove(iobj.getImport());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleNew() {
		final IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		BusyIndicator.showWhile(importTable.getTable().getDisplay(), new Runnable() {
			public void run() {
				NewDependencyWizard wizard = new NewDependencyWizard(model);
				WizardDialog dialog =
					new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				dialog.getShell().setSize(500, 500);
				dialog.open();
			}
		});
	}

	private void handleOpen(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) sel;
			if (ssel.size() == 1) {
				handleOpen(ssel.getFirstElement());
			}
		}
	}

	private void handleOpen(Object obj) {
		if (obj instanceof ImportObject) {
			IPlugin plugin = ((ImportObject) obj).getPlugin();
			if (plugin != null)
				 ((ManifestEditor) getFormPage().getEditor()).openPluginEditor(plugin);
		}
	}

	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		importTable.setInput(model.getPluginBase());
		setReadOnly(!model.isEditable());
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
		PDEPlugin.getDefault().getWorkspaceModelManager().addModelProviderListener(
			this);
		PDEPlugin.getDefault().getExternalModelManager().addModelProviderListener(this);
	}

	private void makeActions() {
		newAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newAction.setText(PDEPlugin.getResourceString(POPUP_NEW));

		openAction = new Action() {
			public void run() {
				handleOpen(importTable.getSelection());
			}
		};
		openAction.setText(PDEPlugin.getResourceString(POPUP_OPEN));

		deleteAction = new Action() {
			public void run() {
				handleDelete();
			}
		};
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
		buildpathAction = new Action() {
			public void run() {
				IPluginModel model = (IPluginModel) getFormPage().getModel();
				computeBuildPath(model, true);
			}
		};
		buildpathAction.setText(PDEPlugin.getResourceString(KEY_COMPUTE_BUILD_PATH));
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			imports = null;
			importTable.refresh();
			return;
		}

		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginImport) {
			IPluginImport iimport = (IPluginImport) changeObject;
			if (event.getChangeType() == event.INSERT) {
				ImportObject iobj = new ImportObject(iimport);
				imports.add(iobj);
				importTable.add(iobj);
				importTable.setSelection(new StructuredSelection(iobj), true);
				importTable.getTable().setFocus();
			} else {
				ImportObject iobj = findImportObject(iimport);
				if (iobj != null) {
					if (event.getChangeType() == event.REMOVE) {
						imports.remove(iobj);
						importTable.remove(iobj);
					} else {
						importTable.update(iobj, null);
					}
				}
			}
			setDirty(true);
		}
	}

	public void modelsChanged(IModelProviderEvent e) {
		imports = null;
		importTable.refresh();
	}

	private ImportObject findImportObject(IPluginImport iimport) {
		if (imports == null)
			return null;
		for (int i = 0; i < imports.size(); i++) {
			ImportObject iobj = (ImportObject) imports.get(i);
			if (iobj.getImport().equals(iimport))
				return iobj;
		}
		return null;
	}

	public void commitChanges(boolean onSave) {
		if (onSave) {
			boolean shouldUpdate = BuildpathPreferencePage.isManifestUpdate();
			if (shouldUpdate)
				updateBuildPath();
		}
		setDirty(false);
	}

	private void updateBuildPath() {
		computeBuildPath((IPluginModelBase) getFormPage().getModel(), false);
	}

	Action getBuildpathAction() {
		return buildpathAction;
	}

	private void computeBuildPath(final IPluginModelBase model, final boolean save) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				monitor.beginTask(PDEPlugin.getResourceString(KEY_UPDATING_BUILD_PATH), 1);
				try {
					if (save && getFormPage().getEditor().isDirty()) {
						getFormPage().getEditor().doSave(monitor);
					}
					BuildPathUtil.setBuildPath(model, monitor);
					monitor.worked(1);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};

		ProgressMonitorDialog pm =
			new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
		try {
			pm.run(false, false, op);
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e.getTargetException());
		}
	}

	public void setFocus() {
		if (importTable != null)
			importTable.getTable().setFocus();
	}

	protected void doPaste(Object target, Object[] objects) {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();

		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof ImportObject) {
					ImportObject iobj = (ImportObject)obj;
					PluginImport iimport = (PluginImport)iobj.getImport();
					iimport.setModel(model);
					iimport.setParent(plugin);
					plugin.add(iimport);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	protected boolean canPaste(Object target, Object[] objects) {
		if (objects[0] instanceof ImportObject) return true;
		return false;
	}
}