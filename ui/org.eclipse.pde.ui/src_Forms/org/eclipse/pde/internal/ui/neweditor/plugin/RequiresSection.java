/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.neweditor.TableSection;
import org.eclipse.pde.internal.ui.newparts.TablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class RequiresSection
	extends TableSection
	implements IModelChangedListener, IModelProviderListener {
	private TableViewer importTable;
	public static final String SECTION_TITLE =
		"ManifestEditor.ImportListSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.ImportListSection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.ImportListSection.fdesc";
	public static final String SECTION_NEW = "ManifestEditor.ImportListSection.new";
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
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IPluginImport[] iimports = model.getPluginBase().getImports();
			for (int i = 0; i < iimports.length; i++) {
				IPluginImport iimport = iimports[i];
				imports.add(new ImportObject(iimport));
			}
		}
	}

	public RequiresSection(DependenciesPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] { PDEPlugin.getResourceString(SECTION_NEW)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment = ((IPluginModelBase)getPage().getModel()).isFragmentModel();
		if (fragment)
			getSection().setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		importTable = tablePart.getTableViewer();

		importTable.setContentProvider(new ImportContentProvider());
		importTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		importTable.setSorter(ListUtil.PLUGIN_SORTER);
		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		initialize();
	}

	protected void selectionChanged(IStructuredSelection sel) {
		Object item = sel.getFirstElement();
		getForm().fireSelectionChanged(this, sel);
		//getFormPage().setSelection(sel);
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
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.removeModelChangedListener(this);
		PDECore.getDefault().getWorkspaceModelManager().removeModelProviderListener(
			this);
		PDECore.getDefault().getExternalModelManager().removeModelProviderListener(
			this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		/*
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		*/
		return false;
	}

	public void setFormInput(Object object) {
		if (object instanceof IPluginImport) {
			ImportObject iobj = new ImportObject((IPluginImport) object);
			importTable.setSelection(new StructuredSelection(iobj), true);
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = importTable.getSelection();
		manager.add(newAction);
		if (!selection.isEmpty()) {
			manager.add(openAction);
		}
		manager.add(new Separator());
		/*
		
		//((DependenciesForm) getFormPage().getForm()).fillContextMenu(manager);
		
		if (!selection.isEmpty())
			manager.add(deleteAction);
		getPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(new ActionContext(selection));
		actionGroup.fillContextMenu(manager);
		if (getFormPage().getModel() instanceof WorkspacePluginModelBase) {
			manager.add(new UnusedDependenciesAction((WorkspacePluginModelBase) getFormPage().getModel()));
		}
		*/
	}

	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) importTable.getSelection();

		if (ssel.isEmpty())
			return;
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
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
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		PluginSelectionDialog dialog =
			new PluginSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				getAvailablePlugins(model),
				true);
		dialog.create();
		if (dialog.open() == PluginSelectionDialog.OK) {
			Object[] models = dialog.getResult();
			for (int i = 0; i < models.length; i++) {
				try {
					IPluginModel candidate = (IPluginModel) models[i];
					IPluginImport importNode = model.getPluginFactory().createImport();
					importNode.setId(candidate.getPlugin().getId());
					model.getPluginBase().add(importNode);
				} catch (CoreException e) {
				}
			}
		}
	}
	
	private IPluginModelBase[] getAvailablePlugins(IPluginModelBase model) {
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPluginsOnly();
		HashSet existingImports = PluginSelectionDialog.getExistingImports(model.getPluginBase());
		ArrayList result = new ArrayList();
		for (int i = 0; i < plugins.length; i++) {
			if (!existingImports.contains(plugins[i].getPluginBase().getId())) {
				result.add(plugins[i]);
			}
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
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
		/*
		if (obj instanceof ImportObject) {
			IPlugin plugin = ((ImportObject) obj).getPlugin();
			if (plugin != null)
				 ManifestEditor.openPluginEditor(plugin);
		}
		*/
	}

	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		importTable.setInput(model.getPluginBase());
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
		PDECore.getDefault().getWorkspaceModelManager().addModelProviderListener(
			this);
		PDECore.getDefault().getExternalModelManager().addModelProviderListener(this);
		newAction.setEnabled(model.isEditable());
		deleteAction.setEnabled(model.isEditable());
		buildpathAction.setEnabled(model.isEditable());
	}

	private void makeActions() {
		newAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newAction.setText(PDEPlugin.getResourceString("ManifestEditor.ImportListSection.new"));

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
				Object model = getPage().getModel();
				if (model instanceof IPluginModelBase)
					computeBuildPath((IPluginModelBase)model, true);
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
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				ImportObject iobj = new ImportObject(iimport);
				imports.add(iobj);
				importTable.add(iobj);
				importTable.setSelection(new StructuredSelection(iobj), true);
				importTable.getTable().setFocus();
			} else {
				ImportObject iobj = findImportObject(iimport);
				if (iobj != null) {
					if (event.getChangeType() == IModelChangedEvent.REMOVE) {
						imports.remove(iobj);
						importTable.remove(iobj);
					} else {
						importTable.update(iobj, null);
					}
				}
			}
			//setDirty(true);
		}
	}

	public void modelsChanged(IModelProviderEvent e) {
		imports = null;
		importTable.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!importTable.getControl().isDisposed())
					importTable.refresh();
			}
		});
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

	public void commit(boolean onSave) {
		if (onSave) {
			IResource resource =
				((IPluginModelBase) getPage().getModel())
					.getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (WorkspaceModelManager.isJavaPluginProject(project)) {
					/*
					PDESourcePage sourcePage =
						(PDESourcePage) getFormPage().getEditor().getPage(
							ManifestEditor.SOURCE_PAGE);
					if (!sourcePage.containsError())
						updateBuildPath();
						*/
				}
			}
		}
		super.commit(onSave);
	}

	private void updateBuildPath() {
		computeBuildPath((IPluginModelBase) getPage().getModel(), false);
	}

	Action getBuildpathAction() {
		return buildpathAction;
	}

	private void computeBuildPath(
		final IPluginModelBase model,
		final boolean save) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				monitor.beginTask(PDEPlugin.getResourceString(KEY_UPDATING_BUILD_PATH), 1);
				try {
					if (save && getPage().getEditor().isDirty()) {
						getPage().getEditor().doSave(monitor);
					}
					ClasspathUtilCore.setClasspath(model, monitor);
					monitor.worked(1);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
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
/*
	protected void doPaste(Object target, Object[] objects) {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();

		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof ImportObject) {
					ImportObject iobj = (ImportObject) obj;
					PluginImport iimport = (PluginImport) iobj.getImport();
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
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof ImportObject))
				return false;
		}
		return true;
	}
	public boolean canPaste(Clipboard clipboard) {
		Object [] objects = (Object[])clipboard.getContents(ModelDataTransfer.getInstance());
		if (objects!=null && objects.length>0) {
			return canPaste(null, objects);
		}
		return false;
	}
	protected void doPaste() {
		Clipboard clipboard = getFormPage().getEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object [] objects = (Object[])clipboard.getContents(modelTransfer);
		if (objects!=null) {
			doPaste(null, objects);
		}
	}
*/
}
