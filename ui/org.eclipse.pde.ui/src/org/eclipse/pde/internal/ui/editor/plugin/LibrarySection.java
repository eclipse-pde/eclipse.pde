/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.build.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.*;
import org.eclipse.ui.views.navigator.*;

public class LibrarySection extends TableSection implements IModelChangedListener {

    private static final int NEW_INDEX = 0;
    private static final int ADD_INDEX = 1;
    private static final int REMOVE_INDEX = 2;
    private static final int UP_INDEX = 3;
    private static final int DOWN_INDEX = 4;
    
    private static final String SECTION_TITLE ="ManifestEditor.LibrarySection.title"; //$NON-NLS-1$
	private static final String SECTION_DESC = "ManifestEditor.LibrarySection.desc"; //$NON-NLS-1$
	private static final String SECTION_FDESC ="ManifestEditor.LibrarySection.fdesc"; //$NON-NLS-1$
	private static final String SECTION_ADD = "NewManifestEditor.LibrarySection.add"; //$NON-NLS-1$
	private static final String SECTION_NEW = "NewManifestEditor.LibrarySection.new"; //$NON-NLS-1$
	private static final String SECTION_UP = "ManifestEditor.LibrarySection.up"; //$NON-NLS-1$
	private static final String SECTION_DOWN = "ManifestEditor.LibrarySection.down"; //$NON-NLS-1$
	private static final String POPUP_NEW_LIBRARY = "ManifestEditor.LibrarySection.newLibrary"; //$NON-NLS-1$
	private static final String NEW_LIBRARY_ENTRY = "ManifestEditor.LibrarySection.newLibraryEntry"; //$NON-NLS-1$

	private Action fRenameAction;
    private Action fRemoveAction;
    private Action fNewAction;
	    
    private TableViewer fLibraryTable;
    
    class LibraryFilter extends JARFileFilter {
		public LibraryFilter(HashSet set) {
			super(set);
		}

		public boolean select(Viewer viewer, Object parent, Object element) {
			if (element instanceof IFolder)
				return isPathValid(((IFolder)element).getProjectRelativePath());
			if (element instanceof IFile)
				return isFileValid(((IFile)element).getProjectRelativePath());
			return false;
		}
	}
	
	class LibrarySelectionValidator extends JarSelectionValidator {
		
		public LibrarySelectionValidator(Class[] acceptedTypes, boolean allowMultipleSelection) {
			super(acceptedTypes, allowMultipleSelection);
		}

		public boolean isValid(Object element) {
			return (element instanceof IFolder) ? true : super.isValid(element);
		}
	}

	class TableContentProvider extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			IPluginModelBase model = (IPluginModelBase)getPage().getModel();
			return model.getPluginBase().getLibraries();
		}
	}

	public LibrarySection(PDEFormPage page, Composite parent) {
		super(
			page,
			parent,
			Section.DESCRIPTION,
			new String[] {
				PDEPlugin.getResourceString(SECTION_NEW),
				PDEPlugin.getResourceString(SECTION_ADD),
                PDEPlugin.getResourceString("Remove"),
				PDEPlugin.getResourceString(SECTION_UP),
				PDEPlugin.getResourceString(SECTION_DOWN)});
	}
    
    private String getSectionDescription() {
        IPluginModelBase model = (IPluginModelBase)getPage().getPDEEditor().getAggregateModel();
        return (model.isFragmentModel())
                    ? PDEPlugin.getResourceString(SECTION_FDESC)
                    : PDEPlugin.getResourceString(SECTION_DESC);   
    }
    
	public void createClient(Section section, FormToolkit toolkit) {
        section.setText(PDEPlugin.getResourceString(SECTION_TITLE));
        section.setDescription(getSectionDescription());
        
		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION, 2, toolkit);
		fLibraryTable = tablePart.getTableViewer();
		fLibraryTable.setContentProvider(new TableContentProvider());
		fLibraryTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
        
        makeActions();
        updateButtons();
        section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setClient(container);

        IPluginModelBase model = (IPluginModelBase) getPage().getModel();
        fLibraryTable.setInput(model.getPluginBase());
        model.addModelChangedListener(this);
	}

	private void updateButtons() {
        Table table = fLibraryTable.getTable();
        boolean hasSelection = table.getSelection().length > 0;
        int count = table.getItemCount();
        boolean canMoveUp = count > 1 && table.getSelectionIndex() > 0;
        boolean canMoveDown = count > 1 && hasSelection && table.getSelectionIndex() < count - 1;
        
        TablePart tablePart = getTablePart();
        tablePart.setButtonEnabled(ADD_INDEX, isEditable());
        tablePart.setButtonEnabled(NEW_INDEX, isEditable());
        tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && hasSelection);
        tablePart.setButtonEnabled(UP_INDEX, isEditable() && canMoveUp);
        tablePart.setButtonEnabled(DOWN_INDEX, isEditable() && canMoveDown);
    }

    private void makeActions() {
        fNewAction = new Action(PDEPlugin.getResourceString(POPUP_NEW_LIBRARY)) {
            public void run() {
                handleNew();
            }
        };
        fNewAction.setEnabled(isEditable());
        
        fRenameAction = new Action(PDEPlugin.getResourceString("EditableTablePart.renameAction")) {
            public void run() {
                getRenameAction().run();
            }
        };
        fRenameAction.setEnabled(isEditable());
        
        fRemoveAction = new Action(PDEPlugin.getResourceString("Remove")) {
            public void run() {
                handleRemove();
            }
        };
        fRemoveAction.setEnabled(isEditable());
    }

    protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		if (getPage().getModel().isEditable())
			updateButtons();
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case NEW_INDEX :
				handleNew();
				break;
			case ADD_INDEX:
				handleAdd();
				break;
            case REMOVE_INDEX:
                handleRemove();
                break;
			case UP_INDEX :
				handleUp();
				break;
			case DOWN_INDEX :
				handleDown();
				break;
		}
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
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
    
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginLibrary) {
			fLibraryTable.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fNewAction);
		if (!fLibraryTable.getSelection().isEmpty()) {
			manager.add(new Separator());
			manager.add(fRenameAction);			
			manager.add(fRemoveAction);
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}
    
	private void handleRemove() {
		Object object = ((IStructuredSelection) fLibraryTable.getSelection()).getFirstElement();
		if (object != null && object instanceof IPluginLibrary) {
			IPluginLibrary ep = (IPluginLibrary) object;
			IPluginBase plugin = ep.getPluginBase();
			try {
				plugin.remove(ep);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void handleDown() {
		Table table = getTablePart().getTableViewer().getTable();
		int index = table.getSelectionIndex();
		if (index != table.getItemCount() - 1)
            swap(index, index + 1);		
	}
	
	private void handleUp() {
		int index = getTablePart().getTableViewer().getTable().getSelectionIndex();
		if (index >= 1)
            swap(index, index - 1);
	}
	
	public void swap(int index1, int index2) {
		Table table = getTablePart().getTableViewer().getTable();
		IPluginLibrary l1 = (IPluginLibrary)table.getItem(index1).getData();
		IPluginLibrary l2 = (IPluginLibrary)table.getItem(index2).getData();

		try {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IPluginBase pluginBase = model.getPluginBase();
			pluginBase.swap(l1, l2);
			refresh();
			updateButtons();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}		
	}
	
	private void handleNew(){
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		NewRuntimeLibraryDialog dialog = new NewRuntimeLibraryDialog(getPage().getSite().getShell(), 
				model.getPluginBase().getLibraries());
		dialog.create();
		dialog.getShell().setText(PDEPlugin.getResourceString(NEW_LIBRARY_ENTRY));
		SWTUtil.setDialogSize(dialog, 250, 175);

		if (dialog.open() == Dialog.OK){
			String libName = dialog.getLibraryName();
			if (libName==null || libName.length()==0)
				return;
			try {
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				library.setName(libName);
				library.setExported(true);
				model.getPluginBase().add(library);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void handleAdd() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
					getPage().getSite().getShell(),
					new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		Class[] acceptedClasses = new Class[] { IFile.class };
		dialog.setValidator(new LibrarySelectionValidator(acceptedClasses, true));
		dialog.setTitle(PDEPlugin.getResourceString("BuildEditor.ClasspathSection.jarsTitle")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("LibrarySection.jarsMessage")); //$NON-NLS-1$
		IPluginLibrary[] libraries = ((IPluginModelBase)getPage().getModel()).getPluginBase().getLibraries();
		HashSet set = new HashSet();
		for (int i = 0; i < libraries.length; i++) {
			set.add(new Path(ClasspathUtilCore.expandLibraryName(libraries[i].getName())));
		}
		dialog.addFilter(new LibraryFilter(set));
		dialog.setInput(((IModel)getPage().getModel()).getUnderlyingResource().getProject());
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			Object[] elements = dialog.getResult();
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			for (int i = 0; i < elements.length; i++) {
				IResource elem = (IResource) elements[i];
				IPath path = elem.getProjectRelativePath();
				if (elem instanceof IFolder)
					path = path.addTrailingSeparator();
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				try {
					library.setName(path.toString());
					library.setExported(true);
					model.getPluginBase().add(library);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}							
			}
		}	
	}

	public void refresh() {
		if (fLibraryTable.getControl().isDisposed())
			return;
		fLibraryTable.setSelection(null);
		fLibraryTable.refresh();
		super.refresh();
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginLibrary) {
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				fLibraryTable.add(changeObject);
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				fLibraryTable.remove(changeObject);
			} else {
                fLibraryTable.update(changeObject, null);
			}
		} else if (changeObject.equals(fLibraryTable.getInput())) {
			markStale();
		} else if (changeObject instanceof IPluginElement && ((IPluginElement)changeObject).getParent() instanceof IPluginLibrary) {
			fLibraryTable.update(((IPluginElement)changeObject).getParent(), null);
		}
	}
    
	public void setFocus() {
		fLibraryTable.getTable().setFocus();
	}
    
	protected void doPaste(Object target, Object[] objects) {
		/*IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof IPluginLibrary) {
					PluginLibrary library = (PluginLibrary) obj;
					library.setModel(model);
					library.setParent(plugin);
					plugin.add(library);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}*/
	}
    
	protected boolean canPaste(Object target, Object[] objects) {
		return (objects[0] instanceof IPluginLibrary);
	}
    
    protected void entryModified(Object entry, String value) {
        try {
            IPluginLibrary library = (IPluginLibrary)entry;
            library.setName(value);
        } catch (CoreException e) {
            PDEPlugin.logException(e);
        }
    }
	

}
