/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.model.bundle.Bundle;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class ExportPackageSection extends TableSection implements IModelChangedListener {

    private static final int ADD_INDEX = 0;
    private static final int REMOVE_INDEX = 1;
    private static final int PROPERTIES_INDEX = 2;
    
	class ExportPackageContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (fHeader == null) {
                //TODO bad cast
                Bundle bundle = (Bundle)getBundle();
                fHeader = (ExportPackageHeader)bundle.getManifestHeader(getExportedPackageHeader());
            }
            return fHeader == null ? new Object[0] : fHeader.getPackages();
		}
    }
    
	class ExportPackageLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		public Image getColumnImage(Object obj, int index) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
		}
	}

	class ExportPackageDialogLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
		}

		public String getText(Object element) {
			ExportPackageDescription p = (ExportPackageDescription) element;
            StringBuffer buffer = new StringBuffer(p.getName());
            String version = p.getVersion().toString();
            if (!version.equals(Version.emptyVersion.toString())) {
                buffer.append(" (");
                buffer.append(version);
                buffer.append(")");
            }
			return buffer.toString();
		}
	}

    private TableViewer fPackageViewer;

    private Action fAddAction;
    private Action fRemoveAction;
    private Action fPropertiesAction;
    private ExportPackageHeader fHeader;

	public ExportPackageSection(PDEFormPage page, Composite parent) {
		super(
				page,
				parent,
				Section.DESCRIPTION,
				new String[] {"Add...", "Remove", "Properties..."}); 
		getSection().setText("Exported Packages"); 
		getSection()
				.setDescription("Enumerate all the packages that this plug-in exposes to clients.  All other packages will be hidden from clients at all times."); 
		getTablePart().setEditable(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		TablePart tablePart = getTablePart();
		fPackageViewer = tablePart.getTableViewer();
		fPackageViewer.setContentProvider(new ExportPackageContentProvider());
		fPackageViewer.setLabelProvider(new ExportPackageLabelProvider());
		fPackageViewer.setSorter(new ViewerSorter() {
            public int compare(Viewer viewer, Object e1, Object e2) {
                String s1 = e1.toString();
                String s2 = e2.toString();
                if (s1.indexOf(" ") != -1)
                    s1 = s1.substring(0, s1.indexOf(" "));
                if (s2.indexOf(" ") != -1)
                    s2 = s2.substring(0, s2.indexOf(" "));
                return super.compare(viewer, s1, s2);
            }
        });
		toolkit.paintBordersFor(container);
		section.setClient(container);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.verticalSpan = 2;
		section.setLayoutData(gd);
        makeActions();
        
        IBundleModel model = getBundleModel();
        fPackageViewer.setInput(model);
        model.addModelChangedListener(this);
        updateButtons();
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
    
    public void dispose() {
        IBundleModel model = getBundleModel();
        if (model != null)
            model.removeModelChangedListener(this);
        super.dispose();
    }
        
    protected void doPaste() {
    }

	protected void selectionChanged(IStructuredSelection sel) {
        getPage().getPDEEditor().setSelection(sel);
        updateButtons();
	}

	private void updateButtons() {
        int size = ((IStructuredSelection)fPackageViewer.getSelection()).size();
        TablePart tablePart = getTablePart();
        tablePart.setButtonEnabled(ADD_INDEX, isEditable());
        tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && size > 0);
        tablePart.setButtonEnabled(PROPERTIES_INDEX, size == 1);  
    }
    
    protected void handleDoubleClick(IStructuredSelection selection) {
        handleOpenProperties();
    }

    protected void buttonSelected(int index) {
        switch (index) {
        case ADD_INDEX:
            handleAdd();
            break;
        case REMOVE_INDEX:
            handleRemove();
            break;
        case PROPERTIES_INDEX:
            handleOpenProperties();
        }
	}

	private void handleOpenProperties() {
        Object object = ((IStructuredSelection)fPackageViewer.getSelection()).getFirstElement();
        ExportPackageObject exportObject = (ExportPackageObject)object;

        DependencyPropertiesDialog dialog = new DependencyPropertiesDialog(isEditable(),exportObject);
        dialog.create();
        SWTUtil.setDialogSize(dialog, 400, -1);
        dialog.setTitle(exportObject.getName());
        if (dialog.open() == DependencyPropertiesDialog.OK && isEditable()) {
             exportObject.setVersion(dialog.getVersion());
         }
    }

	private void handleRemove() {
		Object[] removed = ((IStructuredSelection) fPackageViewer.getSelection()).toArray();
        for (int i = 0; i < removed.length; i++) {
            fHeader.removePackage((ExportPackageObject) removed[i]);
        }
	}

	private void handleAdd() {
        IPluginModelBase model = (IPluginModelBase) getPage().getModel();
        IProject project = model.getUnderlyingResource().getProject();
        try {
            if (project.hasNature(JavaCore.NATURE_ID)) {
                ILabelProvider labelProvider = new JavaElementLabelProvider();
                PackageSelectionDialog dialog = new PackageSelectionDialog(
                        PDEPlugin.getActiveWorkbenchShell(),
                        labelProvider, JavaCore.create(project), fHeader.getPackageNames());
                if (dialog.open() == PackageSelectionDialog.OK) {
                    Object[] selected = dialog.getResult();
                    for (int i = 0; i < selected.length; i++) {
                        IPackageFragment candidate = (IPackageFragment) selected[i];
                        fHeader.addPackage(new ExportPackageObject(candidate, getVersionAttribute()));
                    }
                }
                labelProvider.dispose();
            }
        } catch (CoreException e) {
        }
	}
    
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
            fHeader = null;
			markStale();
			return;
		}
        if (event.getChangedProperty().equals(getExportedPackageHeader()))
            refresh();
	}

	public void refresh() {
		fPackageViewer.refresh();
		super.refresh();
	}
    
    private void makeActions() {
        fAddAction = new Action(PDEPlugin.getResourceString("RequiresSection.add")) { //$NON-NLS-1$
            public void run() {
                handleAdd();
            }
        };
        fAddAction.setEnabled(isEditable());
        fRemoveAction = new Action(PDEPlugin.getResourceString("RequiresSection.delete")) { //$NON-NLS-1$
            public void run() {
                handleRemove();
            }
        };
        fRemoveAction.setEnabled(isEditable());
        
        fPropertiesAction = new Action("Properties") { 
            public void run() {
                handleOpenProperties();
            }
        };
    }

	protected void fillContextMenu(IMenuManager manager) {
        manager.add(fAddAction);
        manager.add(new Separator());
        manager.add(fRemoveAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
        if (!fPackageViewer.getSelection().isEmpty()) {
            manager.add(new Separator());
            manager.add(fPropertiesAction);
        }
	}

    private BundleInputContext getBundleContext() {
        InputContextManager manager = getPage().getPDEEditor().getContextManager();
        return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
    }
    
    private IBundleModel getBundleModel() {
        BundleInputContext context = getBundleContext();
        return (context != null) ? (IBundleModel)context.getModel() : null;
        
    }
    
    private IBundle getBundle() {
        IBundleModel model = getBundleModel();
         return (model != null) ? model.getBundle() : null;
    }
    
    private String getVersionAttribute() {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
        return (manifestVersion < 2) ? Constants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
    }
 
    public String getExportedPackageHeader() {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
        return (manifestVersion < 2) ? ICoreConstants.PROVIDE_PACKAGE : Constants.EXPORT_PACKAGE;
    }

}
