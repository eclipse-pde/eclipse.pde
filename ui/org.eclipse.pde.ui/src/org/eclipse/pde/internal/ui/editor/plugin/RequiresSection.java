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
package org.eclipse.pde.internal.ui.editor.plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.ImportObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.*;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;

public class RequiresSection
	extends TableSection
	implements IModelChangedListener, IModelProviderListener {
	private TableViewer importTable;
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
		super(page, parent, Section.DESCRIPTION, new String[] {PDEPlugin.getResourceString("RequiresSection.add"), null, PDEPlugin.getResourceString("RequiresSection.up"), PDEPlugin.getResourceString("RequiresSection.down")});  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		getSection().setText(PDEPlugin.getResourceString("RequiresSection.title")); //$NON-NLS-1$
		boolean fragment = ((IPluginModelBase)getPage().getModel()).isFragmentModel();
		if (fragment)
			getSection().setDescription(PDEPlugin.getResourceString("RequiresSection.fDesc")); //$NON-NLS-1$
		else
			getSection().setDescription(PDEPlugin.getResourceString("RequiresSection.desc")); //$NON-NLS-1$
		getTablePart().setEditable(false);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		TablePart tablePart = getTablePart();
		importTable = tablePart.getTableViewer();

		importTable.setContentProvider(new ImportContentProvider());
		importTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		initialize();
	}

	protected void selectionChanged(IStructuredSelection sel) {
		getPage().getPDEEditor().setSelection(sel);
		if (getPage().getModel().isEditable())
			updateDirectionalButtons();
	}
	
	private void updateDirectionalButtons() {
		Table table = getTablePart().getTableViewer().getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		boolean canMove = table.getItemCount() > 1;
		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(
			2,
			canMove && hasSelection && table.getSelectionIndex() > 0);
		tablePart.setButtonEnabled(
			3,
			canMove
				&& hasSelection
				&& table.getSelectionIndex() < table.getItemCount() - 1);
	}

	
	protected void handleDoubleClick(IStructuredSelection sel) {
		handleOpen(sel);
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0:
				handleNew();
				break;
			case 2:
				handleUp();
				break;
			case 3:
				handleDown();
		} 
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		PDECore.getDefault().getWorkspaceModelManager().removeModelProviderListener(
			this);
		PDECore.getDefault().getExternalModelManager().removeModelProviderListener(
			this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
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
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste()
	 */
	protected void doPaste() {
	}

	public boolean setFormInput(Object object) {
		if (object instanceof IPluginImport) {
			ImportObject iobj = new ImportObject((IPluginImport) object);
			importTable.setSelection(new StructuredSelection(iobj), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = importTable.getSelection();
		manager.add(newAction);
		if (!selection.isEmpty()) {
			manager.add(openAction);
		}
		manager.add(new Separator());
		getPage().contextMenuAboutToShow(manager);
		
		if (!selection.isEmpty())
			manager.add(deleteAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(new ActionContext(selection));
		actionGroup.fillContextMenu(manager);
		if (((IModel)getPage().getModel()).getUnderlyingResource()!=null) {
			manager.add(new UnusedDependenciesAction((IPluginModelBase) getPage().getModel()));
		}
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
				 ManifestEditor.openPluginEditor(plugin);
		}
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
		
		refresh();
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
	
	private void handleUp() {
		int index = getTablePart().getTableViewer().getTable().getSelectionIndex();
		if (index < 1)
			return;
		swap(index, index - 1);
	}
	
	private void handleDown() {
		Table table = getTablePart().getTableViewer().getTable();
		int index = table.getSelectionIndex();
		if (index == table.getItemCount() - 1)
			return;
		swap(index, index + 1);		
	}
	
	public void swap(int index1, int index2) {
		Table table = getTablePart().getTableViewer().getTable();
		IPluginImport dep1 = ((ImportObject)table.getItem(index1).getData()).getImport();
		IPluginImport dep2 = ((ImportObject)table.getItem(index2).getData()).getImport();

		try {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IPluginBase pluginBase = model.getPluginBase();
			pluginBase.swap(dep1, dep2);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
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


	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		importTable.setInput(model.getPluginBase());
		getTablePart().setButtonEnabled(0, model.isEditable());
		getTablePart().setButtonEnabled(2, false);
		getTablePart().setButtonEnabled(3, false);
		model.addModelChangedListener(this);
		PDECore.getDefault().getWorkspaceModelManager().addModelProviderListener(
			this);
		PDECore.getDefault().getExternalModelManager().addModelProviderListener(this);
		newAction.setEnabled(model.isEditable());
		deleteAction.setEnabled(model.isEditable());
		buildpathAction.setEnabled(model.isEditable());
	}

	private void makeActions() {
		newAction = new Action(PDEPlugin.getResourceString("RequiresSection.add")) { //$NON-NLS-1$
			public void run() {
				handleNew();
			}
		};
		openAction = new Action(PDEPlugin.getResourceString("RequiresSection.open")) { //$NON-NLS-1$
			public void run() {
				handleOpen(importTable.getSelection());
			}
		};
		deleteAction = new Action(PDEPlugin.getResourceString("RequiresSection.delete")) { //$NON-NLS-1$
			public void run() {
				handleDelete();
			}
		};		
		buildpathAction = new Action(PDEPlugin.getResourceString("RequiresSection.compute")) { //$NON-NLS-1$
			public void run() {
				Object model = getPage().getModel();
				if (model instanceof IPluginModelBase)
					computeBuildPath((IPluginModelBase)model, true);
			}
		};
	}
	
	public void refresh() {
		imports = null;
		importTable.refresh();
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		if (event.getChangedProperty() == IPluginBase.P_IMPORT_ORDER) {
			refresh();
			updateDirectionalButtons();
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
		} else {
			importTable.update(((IStructuredSelection)importTable.getSelection()).toArray(), null);
		}
	}

	public void modelsChanged(IModelProviderEvent e) {
		imports = null;
		final Control control = importTable.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed())
						importTable.refresh();
				}
			});
		}
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

	private void computeBuildPath(
		final IPluginModelBase model,
		final boolean save) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				monitor.beginTask(PDEPlugin.getResourceString("RequiresSection.update"), 1); //$NON-NLS-1$
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
		
		try {
			PlatformUI.getWorkbench().getProgressService().runInUI(
					PDEPlugin.getActiveWorkbenchWindow(), op,
					PDEPlugin.getWorkspace().getRoot());
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
	
	Action getBuildpathAction() {
		return buildpathAction;
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
