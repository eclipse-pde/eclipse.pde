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
import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.model.bundle.PackageFriend;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;

public class ExportPackageVisibilitySection extends TableSection
		implements IPartSelectionListener {
    
    private static int ADD_INDEX = 0;
    private static int REMOVE_INDEX = 1;
    
	private static final String KEY_ADD = "ManifestEditor.ExportSection.add"; //$NON-NLS-1$
	private static final String KEY_REMOVE = "ManifestEditor.ExportSection.remove"; //$NON-NLS-1$

	private TableViewer fFriendViewer;
    private Action fAddAction;
    private Action fRemoveAction;
    private Button fInternalButton;
    private boolean fBlockChanges;
    private ExportPackageObject fCurrentObject;
    private Image fImage;
	
	class TableContentProvider extends DefaultContentProvider
			implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
            ExportPackageObject object = (ExportPackageObject)parent;
            return (object != null) ? object.getFriends() : new Object[0];
		}
	}
	class TableLabelProvider extends LabelProvider
			implements ITableLabelProvider {
        
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return fImage;
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
        fFriendViewer = tablePart.getTableViewer();
        fFriendViewer.setContentProvider(new TableContentProvider());
        fFriendViewer.setLabelProvider(new TableLabelProvider());
        toolkit.paintBordersFor(container);

        makeActions();
        fImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
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
        if (fImage != null)
            fImage.dispose();
		super.dispose();
	}
    
	protected void fillContextMenu(IMenuManager manager) {
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}
    
	private void handleAdd() {
        PluginSelectionDialog dialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), getModels(), true);
        dialog.create();
        if (dialog.open() == PluginSelectionDialog.OK) {
            Object[] selected = dialog.getResult();
            for (int i = 0; i < selected.length; i++) {
                IPluginModelBase model = (IPluginModelBase)selected[i];
                fCurrentObject.addFriend(new PackageFriend(fCurrentObject, model.getPluginBase().getId()));
            }
        }
	}
    
    private IPluginModelBase[] getModels() {
        ArrayList list = new ArrayList();
        IPluginModel[] models = PDECore.getDefault().getModelManager().getPluginsOnly();
        for (int i = 0; i < models.length; i++) {
            if (!fCurrentObject.hasFriend(models[i].getPlugin().getId()))
                list.add(models[i]);
        }
        return (IPluginModelBase[])list.toArray(new IPluginModelBase[list.size()]);
    }
    
	private void handleRemove() {
        Object[] removed = ((IStructuredSelection) fFriendViewer.getSelection()).toArray();
        for (int i = 0; i < removed.length; i++) {
            fCurrentObject.removeFriend((PackageFriend) removed[i]);
        }
	}
    
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
        
        if (Constants.EXPORT_PACKAGE.equals(event.getChangedProperty())) {
            refresh();
            return;
        }
        
        Object[] objects = event.getChangedObjects();
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] instanceof PackageFriend) {
                switch (event.getChangeType()) {
                    case IModelChangedEvent.INSERT:
                        fFriendViewer.add(objects[i]);
                        break;
                    case IModelChangedEvent.REMOVE:
                        fFriendViewer.remove(objects[i]);
                        break;
                    default:
                        fFriendViewer.refresh(objects[i]);
                }
            }
        }
	}

	public void refresh() {
        update(null);
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
        boolean hasSelection = !fFriendViewer.getSelection().isEmpty();
        fInternalButton.setEnabled(object != null && isEditable());
        fInternalButton.setSelection(object != null && object.isInternal());
        getTablePart().setButtonEnabled(0, object != null && isEditable());
        getTablePart().setButtonEnabled(1, object != null && isEditable() && hasSelection);
        fFriendViewer.setInput(object);
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
