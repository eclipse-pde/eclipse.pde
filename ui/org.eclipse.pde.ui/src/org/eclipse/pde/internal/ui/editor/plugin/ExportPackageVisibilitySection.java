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
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;

public class ExportPackageVisibilitySection extends TableSection
		implements IPartSelectionListener {
    
    private static int ADD_INDEX = 0;
    private static int REMOVE_INDEX = 1;
    
	private static final String KEY_ADD = "ManifestEditor.ExportSection.add"; //$NON-NLS-1$
	private static final String KEY_REMOVE = "ManifestEditor.ExportSection.remove"; //$NON-NLS-1$

	private TableViewer fPackageExportViewer;
    private Action fAddAction;
    private Action fRemoveAction;
    private Button fInternalButton;
    private boolean fBlockChanges;
    private ExportPackageObject fCurrentObject;
	
	class TableContentProvider extends DefaultContentProvider
			implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
            return (fCurrentObject != null) ? fCurrentObject.getFriends() : new Object[0];
		}
	}
	class TableLabelProvider extends LabelProvider
			implements ITableLabelProvider {
        
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(
					ISharedImages.IMG_OBJS_PACKAGE);
		}
	}
    
	public ExportPackageVisibilitySection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[]{
				PDEPlugin.getResourceString(KEY_ADD),
				PDEPlugin.getResourceString(KEY_REMOVE)});
		handleDefaultButton = false;
	}
    
	public void createClient(Section section, FormToolkit toolkit) {
        section.setText("Package Visibility (Eclipse 3.1 Only)");
        section.setDescription("By default, an exported package is visible to all clients.  Eclipse 3.1 allows access restrictions on exported packages.");
		Composite comp = toolkit.createComposite(section);
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 9;
		comp.setLayout(layout);
        
        fInternalButton = toolkit.createButton(comp, "Hide this package from all plug-ins when running in strict mode", SWT.CHECK);
        fInternalButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (!fBlockChanges)
                    fCurrentObject.setInternal(fInternalButton.getSelection());                   
            }
        });
        
        toolkit.createLabel(comp, "Expose this package to the following plug-ins only:");
        
        Composite container = toolkit.createComposite(comp);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.numColumns = 2;
        container.setLayout(layout);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        EditableTablePart tablePart = getTablePart();
        tablePart.setEditable(getPage().getModel().isEditable());
        createViewerPartControl(container, SWT.FULL_SELECTION, 2, toolkit);
        fPackageExportViewer = tablePart.getTableViewer();
        fPackageExportViewer.setContentProvider(new TableContentProvider());
        fPackageExportViewer.setLabelProvider(new TableLabelProvider());
        fPackageExportViewer.setSorter(new ViewerSorter());
        toolkit.paintBordersFor(container);

        makeActions();
        
        update(null);
        getBundleModel().addModelChangedListener(this);
        
        section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setClient(comp);
	}
    
	private void makeActions() {
        fAddAction = new Action(PDEPlugin.getResourceString(KEY_ADD)) {
            public void run() {
                handleAdd();
            }
        };
        fAddAction.setEnabled(isEditable());
        
        fRemoveAction = new Action(PDEPlugin.getResourceString(KEY_REMOVE)) {
            public void run() {
                handleRemove();
            }
        }; 
        fRemoveAction.setEnabled(isEditable());
    }

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		getTablePart().setButtonEnabled(1, item != null);
	}
    
	protected void buttonSelected(int index) {
		if (index == ADD_INDEX)
			handleAdd();
		else if (index == REMOVE_INDEX)
			handleRemove();
	}
    
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		return false;
	}
    
	public void dispose() {
		IBundleModel model = getBundleModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
    
	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}
    
	private void handleAdd() {
	}
    
	private void handleRemove() {
	}
    
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
        //if (e.getChangedProperty().equals(fMaster.getExportedPackageHeader()))
            //refresh();
	}

	public void refresh() {
		super.refresh();
	}

	public void selectionChanged(IFormPart source, ISelection selection) {
        IStructuredSelection ssel = (IStructuredSelection)selection;
        if (ssel.size() == 1) {
            Object object = ((IStructuredSelection) selection).getFirstElement();
             if (object instanceof ExportPackageObject)
                 update((ExportPackageObject)object);
        } else {
            update(null);
        }
	}
    
    private void update(ExportPackageObject object) {
        fBlockChanges = true;
        fCurrentObject = object;
        boolean hasSelection = !fPackageExportViewer.getSelection().isEmpty();
        fInternalButton.setEnabled(object != null && isEditable());
        fInternalButton.setSelection(object != null && object.isInternal());
        getTablePart().setButtonEnabled(0, object != null && isEditable());
        getTablePart().setButtonEnabled(1, object != null && isEditable() && hasSelection);
        fPackageExportViewer.setInput(object);
        fBlockChanges = false;
    }
    
    private BundleInputContext getBundleContext() {
        InputContextManager manager = getPage().getPDEEditor().getContextManager();
        return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
    }
    
    private IBundleModel getBundleModel() {
        BundleInputContext context = getBundleContext();
        return (context != null) ? (IBundleModel)context.getModel() : null;       
    }
}
