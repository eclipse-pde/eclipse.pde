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

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;
import org.osgi.framework.*;

public class ExportPackageSection extends TableSection implements IModelChangedListener {

    private static final int ADD_INDEX = 0;
    private static final int REMOVE_INDEX = 1;
    private static final int PROPERTIES_INDEX = 2;
    
	class ExportPackageContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (fPackages == null)
				createExportObjects();
			return fPackages.values().toArray();
		}

		private void createExportObjects() {
			fPackages = new TreeMap();
			try {
				String value = getBundle().getHeader(getExportedPackageHeader());
				if (value != null) {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, value);
					for (int i = 0; i < elements.length; i++) {
						ExportPackageObject p = new ExportPackageObject(elements[i], getVersionAttribute());
                        fPackages.put(p.getName(), p);
					}
				}
			} catch (BundleException e) {
			}
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

    private Map fPackages;
    private Action fAddAction;
    private Action fRemoveAction;
    private Action fPropertiesAction;

	public ExportPackageSection(PDEFormPage page, Composite parent) {
		super(
				page,
				parent,
				Section.DESCRIPTION,
				new String[] {"Add...", "Remove", "Properties..."}); 
		getSection().setText("Required Packages"); 
		getSection()
				.setDescription("You can specify packages this plug-in depends on without explicitly restricting what plug-ins they must come from."); 
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
             writeExportPackageHeader();
             fPackageViewer.refresh(exportObject);
         }
    }

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fPackageViewer.getSelection();
		Object[] items = ssel.toArray();
		fPackageViewer.remove(items);
		removeExportPackages(items);
	}

	private void removeExportPackages(Object[] removed) {
		for (int k = 0; k < removed.length; k++) {
			ExportPackageObject p = (ExportPackageObject) removed[k];
			fPackages.remove(p.getName());
		}
		writeExportPackageHeader();
	}

	private void handleAdd() {
        IPluginModelBase model = (IPluginModelBase) getPage().getModel();
        IProject project = model.getUnderlyingResource().getProject();
        try {
            if (project.hasNature(JavaCore.NATURE_ID)) {
                ILabelProvider labelProvider = new JavaElementLabelProvider();
                PackageSelectionDialog dialog = new PackageSelectionDialog(
                        PDEPlugin.getActiveWorkbenchShell(),
                        labelProvider, JavaCore.create(project), getNames());
                if (dialog.open() == PackageSelectionDialog.OK) {
                    Object[] selected = dialog.getResult();
                    for (int i = 0; i < selected.length; i++) {
                        IPackageFragment candidate = (IPackageFragment) selected[i];
                        ExportPackageObject p = new ExportPackageObject(candidate, getVersionAttribute());
                        fPackages.put(p.getName(), p);
                        fPackageViewer.add(p);
                    }
                    if (selected.length > 0) {
                        writeExportPackageHeader();
                    }
                }
                labelProvider.dispose();
            }
        } catch (CoreException e) {
        }
	}
    
    private Vector getNames() {
        Vector vector = new Vector(fPackages.size());
        Iterator iter = fPackages.keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            vector.add(iter.next().toString());
        }
        return vector;
    }

	private void writeExportPackageHeader() {
		StringBuffer buffer = new StringBuffer();
		if (fPackages != null) {
            Iterator iter = fPackages.values().iterator();
			while (iter.hasNext()) {
				buffer.append(((ExportPackageObject)iter.next()).write());
				if (iter.hasNext()) {
					buffer.append("," + System.getProperty("line.separator") + " "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		}
		getBundle().setHeader(getExportedPackageHeader(), buffer.toString());
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		refresh();
	}

	public void refresh() {
		fPackages = null;
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
 
    private String getExportedPackageHeader() {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
        return (manifestVersion < 2) ? ICoreConstants.PROVIDE_PACKAGE : Constants.EXPORT_PACKAGE;
    }

    
 
}
