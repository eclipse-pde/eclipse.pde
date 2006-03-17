/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.FindReferencesAction;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
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
                Bundle bundle = (Bundle)getBundle();
                fHeader = (ExportPackageHeader)bundle.getManifestHeader(getExportedPackageHeader());
            }
            return fHeader == null ? new Object[0] : fHeader.getPackages();
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
                buffer.append(PDEUIMessages.ExportPackageSection_0);
                buffer.append(version);
                buffer.append(")"); //$NON-NLS-1$
            }
			return buffer.toString();
		}
	}

    private TableViewer fPackageViewer;

    private Action fAddAction;
    private Action fGoToAction;
    private Action fRemoveAction;
    private Action fPropertiesAction;
    private ExportPackageHeader fHeader;

	public ExportPackageSection(PDEFormPage page, Composite parent) {
		super(
				page,
				parent,
				Section.DESCRIPTION,
				new String[] {PDEUIMessages.ExportPackageSection_add, PDEUIMessages.ExportPackageSection_remove, PDEUIMessages.ExportPackageSection_properties}); 
	}

    private boolean isFragment() {
        IPluginModelBase model = (IPluginModelBase)getPage().getPDEEditor().getAggregateModel();
        return model.isFragmentModel();
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
        section.setText(PDEUIMessages.ExportPackageSection_title); 
        if (isFragment())
			section.setDescription(PDEUIMessages.ExportPackageSection_descFragment);
		else
			section.setDescription(PDEUIMessages.ExportPackageSection_desc);

        Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fPackageViewer = tablePart.getTableViewer();
		fPackageViewer.setContentProvider(new ExportPackageContentProvider());
		fPackageViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPackageViewer.setSorter(new ViewerSorter() {
            public int compare(Viewer viewer, Object e1, Object e2) {
                String s1 = e1.toString();
                String s2 = e2.toString();
                if (s1.indexOf(" ") != -1) //$NON-NLS-1$
                    s1 = s1.substring(0, s1.indexOf(" ")); //$NON-NLS-1$
                if (s2.indexOf(" ") != -1) //$NON-NLS-1$
                    s2 = s2.substring(0, s2.indexOf(" ")); //$NON-NLS-1$
                return super.compare(viewer, s1, s2);
            }
        });
		toolkit.paintBordersFor(container);
		section.setClient(container);
        GridData gd = new GridData(GridData.FILL_BOTH);
        if (((ManifestEditor)getPage().getEditor()).isEquinox()) {
	        gd.verticalSpan = 2;
	        gd.minimumWidth = 300;
        }
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
		Object[] selected = ((IStructuredSelection)fPackageViewer.getSelection()).toArray();

		TablePart tablePart = getTablePart();
        tablePart.setButtonEnabled(ADD_INDEX, isEditable());
        tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && selected.length > 0);
    	tablePart.setButtonEnabled(PROPERTIES_INDEX, shouldEnableProperties(selected));  
        
    }
	
	private boolean shouldEnableProperties(Object[] selected) {
		if (selected.length == 0)
			return false;
		if (selected.length == 1)
			return true;

		String version = ((ExportPackageObject) selected[0]).getVersion();
		for (int i = 1; i < selected.length; i++) {
			ExportPackageObject object = (ExportPackageObject) selected[i];
			if (version == null) {
				if (object.getVersion() != null) {
					return false;
				}
			} else if (!version.equals(object.getVersion())) {
				return false;
			}
		}
		return true;
	}
    
    protected void handleDoubleClick(IStructuredSelection selection) {
        handleGoToPackage(selection);
    }
    
    private IPackageFragment getPackageFragment(ISelection sel) {
    	if (sel instanceof IStructuredSelection) {
    		IStructuredSelection selection = (IStructuredSelection) sel;
    		if (selection.size() != 1) 
    			return null;
    		PackageObject importObject = (PackageObject)selection.getFirstElement();
    		String packageName = importObject.getName();

    		IJavaProject jp = JavaCore.create(getPage().getPDEEditor().getCommonProject());
    		if (jp != null)
    			try {
    				IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
    				for (int i = 0; i < roots.length; i++) {
    					IPackageFragment frag = roots[i].getPackageFragment(packageName);
    					if (frag.exists()) {
    						return frag;
    					}
    				}
    			} catch (JavaModelException e) {
    			}
    	}
    	return null;
    }
    
    private void handleGoToPackage(ISelection selection) {
    	IPackageFragment frag = getPackageFragment(selection);
    	if (frag != null)
    		try {
    			IViewPart part = PDEPlugin.getActivePage().showView(JavaUI.ID_PACKAGES);
    			ShowInPackageViewAction action = new ShowInPackageViewAction(part.getSite());
    			action.run(frag);
    		} catch (PartInitException e) {
    		}
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
		Object[] selected = ((IStructuredSelection) fPackageViewer.getSelection()).toArray();
		ExportPackageObject first = (ExportPackageObject)selected[0];
        DependencyPropertiesDialog dialog = new DependencyPropertiesDialog(isEditable(), first);
        dialog.create();
        SWTUtil.setDialogSize(dialog, 400, -1);
        if (selected.length == 1)
        	dialog.setTitle(((ExportPackageObject)selected[0]).getName());
        else 
        	dialog.setTitle(PDEUIMessages.ExportPackageSection_props);
        if (dialog.open() == Window.OK && isEditable()) {
        	String newVersion = dialog.getVersion();
        	for (int i = 0; i < selected.length; i++) {
        		ExportPackageObject object = (ExportPackageObject)selected[i];
        		if (!newVersion.equals(object.getVersion()))
        			object.setVersion(newVersion);
        	}
         }
    }

	private void handleRemove() {
		Object[] removed = ((IStructuredSelection) fPackageViewer.getSelection()).toArray();
        for (int i = 0; i < removed.length; i++) {
            fHeader.removePackage((PackageObject) removed[i]);
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
                        labelProvider, 
                        JavaCore.create(project), 
                        fHeader == null ? new Vector() : fHeader.getPackageNames(),
                        "true".equals(getBundle().getHeader(ICoreConstants.ECLIPSE_JREBUNDLE))); //$NON-NLS-1$
                if (dialog.open() == Window.OK) {
                    Object[] selected = dialog.getResult();
                    if (fHeader != null) {
                        for (int i = 0; i < selected.length; i++) {
                            IPackageFragment candidate = (IPackageFragment) selected[i];
                            fHeader.addPackage(new ExportPackageObject(fHeader, candidate, getVersionAttribute()));
                        }
                    } else {
                        getBundle().setHeader(getExportedPackageHeader(), getValue(selected));
                    }
                }
                labelProvider.dispose();
            }
        } catch (CoreException e) {
        }
	}
    
    private String getValue(Object[] objects) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < objects.length; i++) {
            IPackageFragment fragment = (IPackageFragment)objects[i];
            if (buffer.length() > 0)
                buffer.append("," + getLineDelimiter() + " "); //$NON-NLS-1$ //$NON-NLS-2$
            buffer.append(fragment.getElementName());
        }
        return buffer.toString();
    }
    
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
            fHeader = null;
			markStale();
			return;
		}   
        
        if (getExportedPackageHeader().equals(event.getChangedProperty())) {
            refresh();
            return;
        }
        
        Object[] objects = event.getChangedObjects();
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] instanceof ExportPackageObject) {
                ExportPackageObject object = (ExportPackageObject)objects[i];
                switch (event.getChangeType()) {
                    case IModelChangedEvent.INSERT:
                        fPackageViewer.add(object);
                        fPackageViewer.setSelection(new StructuredSelection(object));
                        fPackageViewer.getTable().setFocus();
                        break;
                    case IModelChangedEvent.REMOVE:
                        Table table = fPackageViewer.getTable();
                        int index = table.getSelectionIndex();
                        fPackageViewer.remove(object);
                        table.setSelection(index < table.getItemCount() ? index : table.getItemCount() -1);
                        break;
                    default:
                        fPackageViewer.refresh(object);
                }
            }
        }
	}

	public void refresh() {
		fPackageViewer.refresh();
		super.refresh();
	}
    
    private void makeActions() {
        fAddAction = new Action(PDEUIMessages.RequiresSection_add) { 
            public void run() {
                handleAdd();
            }
        };
        fAddAction.setEnabled(isEditable());
        fGoToAction = new Action(PDEUIMessages.ImportPackageSection_goToPackage) {
        	public void run() {
        		handleGoToPackage(fPackageViewer.getSelection());
        	}
        };
        fRemoveAction = new Action(PDEUIMessages.RequiresSection_delete) { 
            public void run() {
                handleRemove();
            }
        };
        fRemoveAction.setEnabled(isEditable());
        
        fPropertiesAction = new Action(PDEUIMessages.ExportPackageSection_propertyAction) { 
            public void run() {
                handleOpenProperties();
            }
        };
    }

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fPackageViewer.getSelection();
        manager.add(fAddAction);
        boolean singleSelection = selection instanceof IStructuredSelection && 
			((IStructuredSelection)selection).size() == 1;
        if (singleSelection)
        	manager.add(fGoToAction);
        manager.add(new Separator());
        if (!selection.isEmpty())
        	manager.add(fRemoveAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		if (singleSelection)
			manager.add(new Action(PDEUIMessages.ExportPackageSection_findReferences) {
				public void run() {
					doSearch(fPackageViewer.getSelection());
				}
			});
        if (shouldEnableProperties(((IStructuredSelection)fPackageViewer.getSelection()).toArray())) {
            manager.add(new Separator());
            manager.add(fPropertiesAction);
        }
	}
	
	private void doSearch(ISelection sel) {
		IPackageFragment frag = getPackageFragment(sel);
		if (frag != null) {
			FindReferencesAction action = new FindReferencesAction(getPage().getEditorSite());
			action.run(frag);
		} else if (sel instanceof IStructuredSelection)  {
    		IStructuredSelection selection = (IStructuredSelection) sel;
    		PackageObject exportObject = (PackageObject)selection.getFirstElement();
    		NewSearchUI.runQueryInBackground(new BlankQuery(exportObject));
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
    
     private String getLineDelimiter() {
		BundleInputContext inputContext = getBundleContext();
		if (inputContext != null) {
			return inputContext.getLineDelimiter();
		}
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

     private IBundle getBundle() {
        IBundleModel model = getBundleModel();
         return (model != null) ? model.getBundle() : null;
    }
    
    private String getVersionAttribute() {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
        return (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
    }
 
    public String getExportedPackageHeader() {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(getBundle());
        return (manifestVersion < 2) ? ICoreConstants.PROVIDE_PACKAGE : Constants.EXPORT_PACKAGE;
    }

}
