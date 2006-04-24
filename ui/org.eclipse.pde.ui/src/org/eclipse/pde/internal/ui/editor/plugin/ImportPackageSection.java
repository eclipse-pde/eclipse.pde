/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.FindReferencesInWorkingSetAction;
import org.eclipse.jdt.ui.actions.ShowInPackageViewAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.ConditionalListSelectionDialog;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.dependencies.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;


public class ImportPackageSection extends TableSection implements IModelChangedListener {

    private static final int ADD_INDEX = 0;
    private static final int REMOVE_INDEX = 1;
    private static final int PROPERTIES_INDEX = 2;
    
    private ImportPackageHeader fHeader;
    
    class ImportItemWrapper {
    	Object fUnderlying;
    	
    	public ImportItemWrapper(Object underlying) {
    		fUnderlying = underlying;
    	}
    	
    	public String toString() {
    		return getName();
    	}
    	
    	public boolean equals(Object obj) {
    		if (obj instanceof ImportItemWrapper) {
	    		ImportItemWrapper item = (ImportItemWrapper)obj;
	    		return getName().equals(item.getName());
    		}
    		return false;
    	}
    	
    	public String getName() {
    		if (fUnderlying instanceof ExportPackageDescription)
    			return ((ExportPackageDescription)fUnderlying).getName();
    		if (fUnderlying instanceof IPackageFragment)
    			return ((IPackageFragment)fUnderlying).getElementName();
    		return null;
    	}
    	
    	public Version getVersion() {
    		if (fUnderlying instanceof ExportPackageDescription)
    			return ((ExportPackageDescription)fUnderlying).getVersion();
    		return null;
    	}
    	
    	boolean hasVersion() {
    		return hasEPD() && ((ExportPackageDescription)fUnderlying).getVersion() != null;
    	}
    	
    	boolean hasEPD() {
    		return fUnderlying instanceof ExportPackageDescription;
    	}
    }
    
	class ImportPackageContentProvider extends DefaultTableProvider {
        public Object[] getElements(Object parent) {
            if (fHeader == null) {
                Bundle bundle = (Bundle)getBundle();
                fHeader = (ImportPackageHeader)bundle.getManifestHeader(Constants.IMPORT_PACKAGE);
            }
            return fHeader == null ? new Object[0] : fHeader.getPackages();
        }
	}

	class ImportPackageDialogLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
		}

		public String getText(Object element) {
			StringBuffer buffer = new StringBuffer();
			ImportItemWrapper p = (ImportItemWrapper) element;
			buffer.append(p.getName());
			Version version = p.getVersion();
			if (version != null && !Version.emptyVersion.equals(version)) {
				buffer.append(" ("); //$NON-NLS-1$
				buffer.append(version.toString());
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

	public ImportPackageSection(PDEFormPage page, Composite parent) {
		super(
				page,
				parent,
				Section.DESCRIPTION,
				new String[] {PDEUIMessages.ImportPackageSection_add, PDEUIMessages.ImportPackageSection_remove, PDEUIMessages.ImportPackageSection_properties}); 
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
        section.setText(PDEUIMessages.ImportPackageSection_required);
        if (isFragment())
			section.setDescription(PDEUIMessages.ImportPackageSection_descFragment);
		else
			section.setDescription(PDEUIMessages.ImportPackageSection_desc);

        Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fPackageViewer = tablePart.getTableViewer();
		fPackageViewer.setContentProvider(new ImportPackageContentProvider());
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
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
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
    	handleGoToPackage(selection);
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
    
    private IPackageFragment getPackageFragment(ISelection sel) {
    	if (sel instanceof IStructuredSelection)  {
    		IStructuredSelection selection = (IStructuredSelection) sel;
    		if (selection.size() != 1) 
    			return null;
    		PackageObject importObject = (PackageObject)selection.getFirstElement();
    		String packageName = importObject.getName();

    		IBaseModel base = getPage().getModel();
    		if (!(base instanceof IPluginModelBase)) 
    			return null;
    		IPluginModelBase model = (IPluginModelBase)base;
    		if (model.getUnderlyingResource() == null) 
    			return getExternalPackageFragment(packageName, model);
    		IJavaProject jp = JavaCore.create(getPage().getPDEEditor().getCommonProject());
    		if (jp != null)
    			try {
    				IPackageFragmentRoot[] roots = jp.getAllPackageFragmentRoots();
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
    
    private IPackageFragment getExternalPackageFragment(String packageName, IPluginModelBase model) {
    	IPluginModelBase base = null;
    	try {
    		IPluginModelBase plugin = PDECore.getDefault().getModelManager().findModel(model.getPluginBase().getId());
    		ImportPackageSpecification[] packages = plugin.getBundleDescription().getImportPackages();
    		for (int i =0; i < packages.length; i++)
    			if (packages[i].getName().equals(packageName)) {
    				ExportPackageDescription desc = (ExportPackageDescription) packages[i].getSupplier();
    				base = PDECore.getDefault().getModelManager().findModel(desc.getExporter().getSymbolicName());
    				break;
    			}
    		if (base == null)
    			return null;
    		IResource res = base.getUnderlyingResource();
    		if (res != null) {
    			IJavaProject jp = JavaCore.create(res.getProject());
    			if (jp != null)
    				try {
    					IPackageFragmentRoot[] roots = jp.getAllPackageFragmentRoots();
    					for (int i = 0; i < roots.length; i++) {
    						IPackageFragment frag = roots[i].getPackageFragment(packageName);
    						if (frag.exists()) 
    							return frag;
    					}
    				} catch (JavaModelException e) {
    				}
    		}
			IProject proj = PDEPlugin.getWorkspace().getRoot().getProject(SearchablePluginsManager.PROXY_PROJECT_NAME);
			if (proj == null)
				return searchWorkspaceForPackage(packageName, base);
			IJavaProject jp = JavaCore.create(proj);
			IPath path = new Path(base.getInstallLocation());
			// if model is in jar form
			if (!path.toFile().isDirectory()) {
				IPackageFragmentRoot root = jp.findPackageFragmentRoot(path);
				if (root != null) {
					IPackageFragment frag = root.getPackageFragment(packageName);
					if (frag.exists())
						return frag;
				}
			// else model is in folder form, try to find model's libraries on filesystem
			} else {
				IPluginLibrary[] libs = base.getPluginBase().getLibraries();
				for (int i = 0; i < libs.length; i++) {
					if (IPluginLibrary.RESOURCE.equals(libs[i].getType()))
						continue;
					String libName = ClasspathUtilCore.expandLibraryName(libs[i].getName());
					IPackageFragmentRoot root = jp.findPackageFragmentRoot(path.append(libName));
					if (root != null) {
						IPackageFragment frag = root.getPackageFragment(packageName);
						if (frag.exists())
							return frag;
					}
				}
			}
		} catch (JavaModelException e){
		}
		return searchWorkspaceForPackage(packageName, base);
    }
    
    private IPackageFragment searchWorkspaceForPackage(String packageName, IPluginModelBase base) {
    	IPluginLibrary[] libs = base.getPluginBase().getLibraries();
    	ArrayList libPaths = new ArrayList();
    	IPath path = new Path(base.getInstallLocation());
    	if (libs.length == 0) {
    		libPaths.add(path);
    	}
		for (int i = 0; i < libs.length; i++) {
			if (IPluginLibrary.RESOURCE.equals(libs[i].getType()))
				continue;
			String libName = ClasspathUtilCore.expandLibraryName(libs[i].getName());
			libPaths.add(path.append(libName));
		}
		IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			try {
				if(!projects[i].hasNature(JavaCore.NATURE_ID) || !projects[i].isOpen())
					continue;
				IJavaProject jp = JavaCore.create(projects[i]);
				ListIterator li = libPaths.listIterator();
				while (li.hasNext()) {
					IPackageFragmentRoot root = jp.findPackageFragmentRoot((IPath)li.next());
					if (root != null) {
						IPackageFragment frag = root.getPackageFragment(packageName);
						if (frag.exists())
							return frag;
					}
				}
			} catch (CoreException e) {
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

	private void handleOpenProperties() {
        Object object = ((IStructuredSelection)fPackageViewer.getSelection()).getFirstElement();
        ImportPackageObject importObject = (ImportPackageObject)object;

        DependencyPropertiesDialog dialog = new DependencyPropertiesDialog(isEditable(),importObject);
        dialog.create();
        SWTUtil.setDialogSize(dialog, 400, -1);
        dialog.setTitle(importObject.getName());
        if (dialog.open() == Window.OK && isEditable()) {
            importObject.setOptional(dialog.isOptional());
            importObject.setVersion(dialog.getVersion());
         }
    }

	private void handleRemove() {
        Object[] removed = ((IStructuredSelection) fPackageViewer.getSelection()).toArray();
        for (int i = 0; i < removed.length; i++) {
            fHeader.removePackage((PackageObject) removed[i]);
        }
	}

	private void handleAdd() {
		final ConditionalListSelectionDialog dialog = new ConditionalListSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				new ImportPackageDialogLabelProvider(),
				PDEUIMessages.ImportPackageSection_dialogButtonLabel);
		Runnable runnable = new Runnable() {
			public void run() {
				setElements(dialog);
				dialog.setMultipleSelection(true);
				dialog.setMessage(PDEUIMessages.ImportPackageSection_exported);
				dialog.setTitle(PDEUIMessages.ImportPackageSection_selection);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 500);
			}
		};
		
		BusyIndicator.showWhile(Display.getCurrent(), runnable);
		if (dialog.open() == Window.OK) {
			Object[] selected = dialog.getResult();
			HashMap exportMap = new HashMap();
			if (fHeader != null) {
				for (int i = 0; i < selected.length; i++) {
					ImportPackageObject impObject = null;
					if (selected[i] instanceof ImportItemWrapper)
						selected[i] = ((ImportItemWrapper)selected[i]).fUnderlying;
					
					if (selected[i] instanceof ExportPackageDescription)
						impObject = new ImportPackageObject(fHeader, (ExportPackageDescription) selected[i], getVersionAttribute());
					else if (selected[i] instanceof IPackageFragment) {
						// non exported package
						IPackageFragment fragment = ((IPackageFragment) selected[i]);
						impObject = new ImportPackageObject(fHeader, fragment.getElementName(), null, getVersionAttribute());
						IProject project = fragment.getJavaProject().getProject();
						IFile file = project.getFile(PDEModelUtility.F_MANIFEST_FP);
						ArrayList list = (ArrayList)exportMap.get(file);
						if (list == null) {
							list = new ArrayList();
							exportMap.put(file, list);
						}
						list.add(fragment);
					}
					if (impObject != null)
						fHeader.addPackage(impObject);
				}
			} else {
				getBundle().setHeader(Constants.IMPORT_PACKAGE, getValue(selected));
			}
			
			//export all package fragments that are not exported already
			Set keys = exportMap.keySet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				final IFile underlying = (IFile)it.next();
				final ArrayList fragmentList = (ArrayList)exportMap.get(underlying);
				ModelModification mod = new ModelModification(underlying) {
					protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
						if (!(model instanceof IBundlePluginModelBase))
							return;
						IBundlePluginModelBase bundleBase = (IBundlePluginModelBase)model;
						IBundle bundle = bundleBase.getBundleModel().getBundle();
						IManifestHeader header = bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
						if (header instanceof ExportPackageHeader) {
							for (int i = 0; i < fragmentList.size(); i++) {
								((ExportPackageHeader)header).addPackage(
										new ExportPackageObject(
												(ExportPackageHeader)header,
												((IPackageFragment)fragmentList.get(i)),
												getVersionAttribute(bundle)));
							}
						} else {
							StringBuffer buffer = new StringBuffer();
							for (int i = 0; i < fragmentList.size(); i++) {
								if (i > 0)
									buffer.append(", "); //$NON-NLS-1$
								buffer.append(((IPackageFragment)fragmentList.get(i)).getElementName());
							}
							bundle.setHeader(Constants.EXPORT_PACKAGE, buffer.toString());
						}
					}
				};
				try {
					PDEModelUtility.modifyModel(mod, null);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}
	}

	private String getValue(Object[] objects) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < objects.length; i++) {
        	if (!(objects[i] instanceof ImportItemWrapper))
        		continue;
        	Version version = ((ImportItemWrapper)objects[i]).getVersion();
            if (buffer.length() > 0)
                buffer.append("," + getLineDelimiter() + " "); //$NON-NLS-1$ //$NON-NLS-2$
            buffer.append(((ImportItemWrapper)objects[i]).getName());
            if (version != null && !version.equals(Version.emptyVersion)) {
                buffer.append(";"); //$NON-NLS-1$
                buffer.append(getVersionAttribute());
                buffer.append("=\""); //$NON-NLS-1$
                buffer.append(version.toString());
                buffer.append("\""); //$NON-NLS-1$
            }
        }
        return buffer.toString();
    }
	
	private void setElements(ConditionalListSelectionDialog dialog) {
        Set forbidden = getForbiddenIds();      
        boolean allowJava = "true".equals(getBundle().getHeader(ICoreConstants.ECLIPSE_JREBUNDLE)); //$NON-NLS-1$

        ArrayList elements = new ArrayList();
        ArrayList conditional = new ArrayList();
        IPluginModelBase[] models = PDECore.getDefault().getModelManager().getPlugins();
        Set names = new HashSet();
        
        for (int i = 0; i < models.length; i++) {
        	BundleDescription desc = models[i].getBundleDescription();
        	String id = desc == null ? null : desc.getSymbolicName();
        	if (id == null || forbidden.contains(id))
        		continue;
        	
        	names.clear();
        	ExportPackageDescription[] exported = desc.getExportPackages();
        	for (int j = 0; j < exported.length; j++) {
        		String name = exported[j].getName();
        		names.add(name);
    			if (("java".equals(name) || name.startsWith("java.")) && !allowJava) //$NON-NLS-1$ //$NON-NLS-2$
    				continue;        		
    			if (fHeader == null || !fHeader.hasPackage(name))
    				elements.add(new ImportItemWrapper(exported[j]));			
        	}
        	
        	try {
    			// add un-exported packages in workspace non-binary plug-ins
    			IResource resource = models[i].getUnderlyingResource();
    			IProject project = resource != null ? resource.getProject() : null;
    			if (project == null || !project.hasNature(JavaCore.NATURE_ID) 
    				|| WorkspaceModelManager.isBinaryProject(project)
    				|| !WorkspaceModelManager.hasBundleManifest(project))
    				continue;
				IJavaProject jp = JavaCore.create(project);
				IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
				for (int j = 0; j < roots.length; j++) {
					if (roots[j].getKind() == IPackageFragmentRoot.K_SOURCE
						|| (roots[j].getKind() == IPackageFragmentRoot.K_BINARY && !roots[j].isExternal())) {
						IJavaElement[] children = roots[j].getChildren();
						for (int k = 0; k < children.length; k++) {
							IPackageFragment f = (IPackageFragment) children[k];
							String name = f.getElementName();
							if (name.equals("")) //$NON-NLS-1$
								name = "."; //$NON-NLS-1$
							if (names.contains(name))
								continue;
							if (f.hasChildren() || f.getNonJavaResources().length > 0)
								conditional.add(new ImportItemWrapper(f));
						}
					}
				}
    		} catch (CoreException e) {
    		}       	
        }       
        dialog.setElements(elements.toArray());
        dialog.setConditionalElements(conditional.toArray());
 	}
	
	public void modelChanged(IModelChangedEvent event) {
        if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
            fHeader = null;
            markStale();
            return;
        }   
        
        if (Constants.IMPORT_PACKAGE.equals(event.getChangedProperty())) {
            refresh();
            return;
        }
        
        Object[] objects = event.getChangedObjects();
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] instanceof ImportPackageObject) {
                ImportPackageObject object = (ImportPackageObject)objects[i];
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
        
        fPropertiesAction = new Action(PDEUIMessages.ImportPackageSection_propertyAction) { 
            public void run() {
                handleOpenProperties();
            }
        };
    }

	protected void fillContextMenu(IMenuManager manager) {
		final ISelection selection = fPackageViewer.getSelection();
        manager.add(fAddAction);
        boolean singleSelection = selection instanceof IStructuredSelection && 
			((IStructuredSelection)selection).size() == 1;
        if (singleSelection)
        	manager.add(fGoToAction);
        manager.add(new Separator());
        if (!selection.isEmpty())
        	manager.add(fRemoveAction);
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());
		if (singleSelection){ 
			manager.add(new Action(PDEUIMessages.DependencyExtentSearchResultPage_referencesInPlugin) {
				public void run() {
					doReferenceSearch(selection);
				}
			});
		}
		if (((IModel)getPage().getModel()).getUnderlyingResource()!=null) 
			manager.add(new UnusedDependenciesAction((IPluginModelBase) getPage().getModel(), false));
        if (!fPackageViewer.getSelection().isEmpty()) {
            manager.add(new Separator());
            manager.add(fPropertiesAction);
        }
	}
	
	private void doReferenceSearch(final ISelection sel) {
		IPackageFragmentRoot[] roots = null;
		try {
			roots = getSourceRoots();
		} catch (JavaModelException e) {
		}
		final IPackageFragment fragment = getPackageFragment(sel);
		if (fragment != null && roots != null) {
			IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
			IWorkingSet set = manager.createWorkingSet("temp", roots); //$NON-NLS-1$
			new FindReferencesInWorkingSetAction(getPage().getEditorSite(), new IWorkingSet[] {set}).run(fragment);
			manager.removeWorkingSet(set);
		} else if (sel instanceof IStructuredSelection)  {
			IStructuredSelection selection = (IStructuredSelection) sel;
			PackageObject importObject = (PackageObject)selection.getFirstElement();
			NewSearchUI.runQueryInBackground(new BlankQuery(importObject));
		}
	}
	
	private IPackageFragmentRoot[] getSourceRoots() throws JavaModelException {
		ArrayList result = new ArrayList();
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPackageFragmentRoot[] roots = JavaCore.create(project).getPackageFragmentRoots();
		for (int i = 0; i < roots.length; i++) {
			if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
					|| (roots[i].isArchive() && !roots[i].isExternal())) 
				result.add(roots[i]);
		}
		return (IPackageFragmentRoot[]) result.toArray(new IPackageFragmentRoot[result.size()]);
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
    	return getVersionAttribute(getBundle());
    }
    
    private String getVersionAttribute(IBundle bundle) {
        int manifestVersion = BundlePluginBase.getBundleManifestVersion(bundle);
        return (manifestVersion < 2) ? ICoreConstants.PACKAGE_SPECIFICATION_VERSION : Constants.VERSION_ATTRIBUTE;
    }
    
    private Set getForbiddenIds() {
        HashSet set = new HashSet();
        IPluginModelBase model = (IPluginModelBase)getPage().getPDEEditor().getAggregateModel();
        String id = model.getPluginBase().getId();
        if (id != null)
            set.add(id);
        IPluginImport[] imports = model.getPluginBase().getImports();
        State state = TargetPlatform.getState();
        for (int i = 0; i < imports.length; i++) {
            addDependency(state, imports[i].getId(), set);
        }
        return set;
    }
    
    private void addDependency(State state, String bundleID, Set set) {
        if (bundleID == null || !set.add(bundleID))
            return;
            
        BundleDescription desc = state.getBundle(bundleID, null);
        if (desc == null)
            return;
        
        BundleDescription[] fragments = desc.getFragments();
        for (int i = 0; i < fragments.length; i++) {
        	addDependency(state, fragments[i].getSymbolicName(), set);
        }
        
        BundleSpecification[] specs = desc.getRequiredBundles();
        for (int j = 0; j < specs.length; j++) {
            if (specs[j].isResolved() && specs[j].isExported()) {
                addDependency(state, specs[j].getName(), set);
            }
        }        
    }
}
