/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
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
import org.eclipse.pde.internal.ui.search.dependencies.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;

public class RequiresSection
	extends TableSection
	implements IModelChangedListener, IModelProviderListener {
    
    private static final int ADD_INDEX = 0;
    private static final int REMOVE_INDEX = 1;
    private static final int UP_INDEX = 2;
    private static final int DOWN_INDEX = 3;
    private static final int PROPERTIES_INDEX = 4;
    
	private TableViewer fImportViewer;
	private Vector fImports;
	private Action fOpenAction;
	private Action fAddAction;
	private Action fRemoveAction;
    private Action fPropertiesAction;

	class ImportContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (fImports == null) {
				createImportObjects();
			}
			return fImports.toArray();
		}
		private void createImportObjects() {
			fImports = new Vector();
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IPluginImport[] iimports = model.getPluginBase().getImports();
			for (int i = 0; i < iimports.length; i++) {
				IPluginImport iimport = iimports[i];
				fImports.add(new ImportObject(iimport));
			}
		}
	}

	public RequiresSection(DependenciesPage page, Composite parent, String[] labels) {
		super(page, parent, Section.DESCRIPTION, labels);
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
		fImportViewer = tablePart.getTableViewer();

		fImportViewer.setContentProvider(new ImportContentProvider());
		fImportViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		GridData gd = new GridData(GridData.FILL_BOTH);
		if (!isBundle())
			gd.verticalSpan = 2;
		section.setLayoutData(gd);
		initialize();
	}

	protected void selectionChanged(IStructuredSelection sel) {
		getPage().getPDEEditor().setSelection(sel);
		updateButtons();
	}
	
	private void updateButtons() {
		Table table = getTablePart().getTableViewer().getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		boolean canMove = table.getItemCount() > 1;
		TablePart tablePart = getTablePart();
        tablePart.setButtonEnabled(ADD_INDEX, isEditable());
		tablePart.setButtonEnabled(
			UP_INDEX,
			canMove && isEditable() && hasSelection && table.getSelectionIndex() > 0);
		tablePart.setButtonEnabled(
			DOWN_INDEX,
			canMove
				&& hasSelection && isEditable()
				&& table.getSelectionIndex() < table.getItemCount() - 1);
        if (isBundle())
            tablePart.setButtonEnabled(PROPERTIES_INDEX, hasSelection);
        tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && hasSelection);
	}

	protected void handleDoubleClick(IStructuredSelection sel) {
		handleOpen(sel);
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case ADD_INDEX:
				handleAdd();
				break;
            case REMOVE_INDEX:
                handleRemove();
                break;
 			case UP_INDEX:
				handleUp();
				break;
			case DOWN_INDEX:
				handleDown();
                break;
            case PROPERTIES_INDEX:
                handleOpenProperties();
                break;
		} 
	}
    
    private void handleOpenProperties() {
        Object changeObject = ((IStructuredSelection)fImportViewer.getSelection()).getFirstElement();
        IPluginImport importObject = ((ImportObject) changeObject).getImport();

        DependencyPropertiesDialog dialog = new DependencyPropertiesDialog(
                                            isEditable(),
                                            importObject);
        dialog.create();
        SWTUtil.setDialogSize(dialog, 400, -1);
        dialog.setTitle(importObject.getId());
        if (dialog.open() == DependencyPropertiesDialog.OK && isEditable()) {
            try {
                importObject.setOptional(dialog.isOptional());
                importObject.setReexported(dialog.isReexported());
                importObject.setVersion(dialog.getVersion());
            } catch (CoreException e) {
               PDEPlugin.logException(e);
            }
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
			handleRemove();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleRemove();
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
			fImportViewer.setSelection(new StructuredSelection(iobj), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fImportViewer.getSelection();
		manager.add(fAddAction);
		if (!selection.isEmpty()) {
			manager.add(fOpenAction);
		}
		manager.add(new Separator());
		getPage().contextMenuAboutToShow(manager);
		
		if (!selection.isEmpty())
			manager.add(fRemoveAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(new ActionContext(selection));
		actionGroup.fillContextMenu(manager);
		if (((IModel)getPage().getModel()).getUnderlyingResource()!=null) {
			manager.add(new UnusedDependenciesAction((IPluginModelBase) getPage().getModel(), false));
		}
        if (fPropertiesAction != null && !fImportViewer.getSelection().isEmpty()) {
            manager.add(new Separator());
            manager.add(fPropertiesAction);
        }
	}

	private void handleOpen(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) sel;
			if (ssel.size() == 1) {
                Object obj = ssel.getFirstElement();
                if (obj instanceof ImportObject) {
                    IPlugin plugin = ((ImportObject) obj).getPlugin();
                    if (plugin != null)
                         ManifestEditor.openPluginEditor(plugin);
                }
			}
		}
	}
	
	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fImportViewer.getSelection();
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
        updateButtons();
	}
    
	private void handleAdd() {
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
		fImportViewer.setInput(model.getPluginBase());
        updateButtons();
		model.addModelChangedListener(this);
		PDECore.getDefault().getWorkspaceModelManager().addModelProviderListener(
			this);
		PDECore.getDefault().getExternalModelManager().addModelProviderListener(this);
		fAddAction.setEnabled(model.isEditable());
		fRemoveAction.setEnabled(model.isEditable());
	}

	private void makeActions() {
		fAddAction = new Action(PDEPlugin.getResourceString("RequiresSection.add")) { //$NON-NLS-1$
			public void run() {
				handleAdd();
			}
		};
		fOpenAction = new Action(PDEPlugin.getResourceString("RequiresSection.open")) { //$NON-NLS-1$
			public void run() {
				handleOpen(fImportViewer.getSelection());
			}
		};
		fRemoveAction = new Action(PDEPlugin.getResourceString("RequiresSection.delete")) { //$NON-NLS-1$
			public void run() {
				handleRemove();
			}
		};
        if (isBundle()) {
            fPropertiesAction = new Action("Properties") { 
                public void run() {
                    handleOpenProperties();
                }
            };
        }
	}
	
	public void refresh() {
		fImports = null;
		fImportViewer.refresh();
		super.refresh();
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		if (event.getChangedProperty() == IPluginBase.P_IMPORT_ORDER) {
			refresh();
			updateButtons();
			return;
		}

		Object changeObject = event.getChangedObjects()[0];		
		if (changeObject instanceof IPluginImport) {
			IPluginImport iimport = (IPluginImport) changeObject;
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				ImportObject iobj = new ImportObject(iimport);
				fImports.add(iobj);
				fImportViewer.add(iobj);
				fImportViewer.setSelection(new StructuredSelection(iobj), true);
				fImportViewer.getTable().setFocus();
			} else {
				ImportObject iobj = findImportObject(iimport);
				if (iobj != null) {
					if (event.getChangeType() == IModelChangedEvent.REMOVE) {
						fImports.remove(iobj);
                        Table table = fImportViewer.getTable();
                        int index = table.getSelectionIndex();
						fImportViewer.remove(iobj);
                        table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
					} else {
						fImportViewer.update(iobj, null);
					}
				}
			}
		} else {
			fImportViewer.update(((IStructuredSelection)fImportViewer.getSelection()).toArray(), null);
		}
	}

	public void modelsChanged(IModelProviderEvent e) {
		fImports = null;
		final Control control = fImportViewer.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed())
						fImportViewer.refresh();
				}
			});
		}
	}

	private ImportObject findImportObject(IPluginImport iimport) {
		if (fImports == null)
			return null;
		for (int i = 0; i < fImports.size(); i++) {
			ImportObject iobj = (ImportObject) fImports.get(i);
			if (iobj.getImport().equals(iimport))
				return iobj;
		}
		return null;
	}

	public void setFocus() {
		if (fImportViewer != null)
			fImportViewer.getTable().setFocus();
	}
	
	private boolean isBundle() {
		return getPage().getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
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
