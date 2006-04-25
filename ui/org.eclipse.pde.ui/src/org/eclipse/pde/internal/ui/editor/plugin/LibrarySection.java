/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.pde.internal.ui.editor.build.JARFileFilter;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

public class LibrarySection extends TableSection implements IModelChangedListener, IBuildPropertiesConstants {

    private static final int NEW_INDEX = 0;
    private static final int ADD_INDEX = 1;
    private static final int REMOVE_INDEX = 2;
    private static final int UP_INDEX = 3;
    private static final int DOWN_INDEX = 4;
    
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
			return getModel().getPluginBase().getLibraries();
		}
	}

	public LibrarySection(PDEFormPage page, Composite parent) {
		super(
			page,
			parent,
			Section.DESCRIPTION,
			new String[] {
				PDEUIMessages.NewManifestEditor_LibrarySection_new,
				PDEUIMessages.NewManifestEditor_LibrarySection_add,
                PDEUIMessages.NewManifestEditor_LibrarySection_remove,
				PDEUIMessages.ManifestEditor_LibrarySection_up,
				PDEUIMessages.ManifestEditor_LibrarySection_down});
	}
    
    private String getSectionDescription() {
        IPluginModelBase model = getModel();
        if (isBundle()) {
           return (model.isFragmentModel())
               ? PDEUIMessages.ClasspathSection_fragment
               : PDEUIMessages.ClasspathSection_plugin;
        }      
        return (model.isFragmentModel())
                    ? PDEUIMessages.ManifestEditor_LibrarySection_fdesc
                    : PDEUIMessages.ManifestEditor_LibrarySection_desc;       
    }
    
    protected boolean isBundle() {
        return getBundleContext() != null;
    }
    
    private BundleInputContext getBundleContext() {
        InputContextManager manager = getPage().getPDEEditor().getContextManager();
        return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
    }
    
	public void createClient(Section section, FormToolkit toolkit) {
        section.setText(PDEUIMessages.ManifestEditor_LibrarySection_title);
        section.setDescription(getSectionDescription());
        
		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fLibraryTable = tablePart.getTableViewer();
		fLibraryTable.setContentProvider(new TableContentProvider());
		fLibraryTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);
        
        makeActions();
        updateButtons();
        section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setClient(container);

        IPluginModelBase model = getModel();
        fLibraryTable.setInput(model.getPluginBase());
        model.addModelChangedListener(this);
	}

	private void updateButtons() {
        Table table = fLibraryTable.getTable();
        boolean hasSelection = table.getSelection().length > 0;
        boolean singleSelection = table.getSelection().length == 1;
        int count = table.getItemCount();
        int index = table.getSelectionIndex();
        boolean canMoveUp = singleSelection && index > 0;
        boolean canMoveDown = singleSelection && index < count - 1;
        
        TablePart tablePart = getTablePart();
        tablePart.setButtonEnabled(ADD_INDEX, isEditable());
        tablePart.setButtonEnabled(NEW_INDEX, isEditable());
        tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && hasSelection);
        tablePart.setButtonEnabled(UP_INDEX, isEditable() && canMoveUp);
        tablePart.setButtonEnabled(DOWN_INDEX, isEditable() && canMoveDown);
    }

    private void makeActions() {
        fNewAction = new Action(PDEUIMessages.ManifestEditor_LibrarySection_newLibrary) {
            public void run() {
                handleNew();
            }
        };
        fNewAction.setEnabled(isEditable());
        
        fRenameAction = new Action(PDEUIMessages.EditableTablePart_renameAction) {
            public void run() {
                getRenameAction().run();
            }
        };
        fRenameAction.setEnabled(isEditable());
        
        fRemoveAction = new Action(PDEUIMessages.NewManifestEditor_LibrarySection_remove) {
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
		IPluginModelBase model = getModel();
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
		Object[] selection = ((IStructuredSelection) fLibraryTable.getSelection()).toArray();
		int index = fLibraryTable.getTable().getSelectionIndex();
		int[] indices = fLibraryTable.getTable().getSelectionIndices();
		for (int i = 0; i < indices.length; i++)
			if (indices[i] < index)
				index = indices[i];
		
		String[] remove = new String[selection.length];
		for (int i = 0; i < selection.length; i++) {
			if (selection[i] != null && selection[i] instanceof IPluginLibrary) {
				IPluginLibrary ep = (IPluginLibrary) selection[i];
				IPluginBase plugin = ep.getPluginBase();
				try {
					plugin.remove(ep);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
				remove[i] = ep.getName();
			}
		}
		updateBuildProperties(remove, new String[remove.length], true);
		updateJavaClasspathLibs(remove, new String[remove.length]);
		
		int itemCount = fLibraryTable.getTable().getItemCount(); 
		if (itemCount > 0) {
			if (index >= itemCount)
				index = itemCount - 1;
			fLibraryTable.getTable().setSelection(index);
			fLibraryTable.getTable().setFocus();
		}
		updateButtons();
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
			IPluginModelBase model = getModel();
			IPluginBase pluginBase = model.getPluginBase();
			pluginBase.swap(l1, l2);
			refresh();
			table.setSelection(index2);
			table.setFocus();
			updateButtons();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}		
	}
	
	private void handleNew(){
		IPluginModelBase model = getModel();
		NewRuntimeLibraryDialog dialog = new NewRuntimeLibraryDialog(
				getPage().getSite().getShell(), 
				model.getPluginBase().getLibraries());
		dialog.create();
		dialog.getShell().setText(PDEUIMessages.ManifestEditor_LibrarySection_newLibraryEntry);
		SWTUtil.setDialogSize(dialog, 250, 175);

		if (dialog.open() == Window.OK){
			String libName = dialog.getLibraryName();
			if (libName==null || libName.length()==0)
				return;
			try {
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				library.setName(libName);
				library.setExported(true);
				model.getPluginBase().add(library);
				checkSourceRootEntry();
				updateBuildProperties(
						new String[] {null},
						new String[] { library.getName()},
						true);
				fLibraryTable.setSelection(new StructuredSelection(library));
		        fLibraryTable.getTable().setFocus();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	
	private void checkSourceRootEntry() {
		IPluginModelBase pluginModel = getModel();
		IPluginLibrary[] libraries = pluginModel.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++)
			if (libraries[i].getName().equals(".")) //$NON-NLS-1$
				return;
		IBuildModel model = getBuildModel();
		if (model == null)
			return;
		
		IBuildEntry[] entires = model.getBuild().getBuildEntries();
		for (int i = 0; i < entires.length; i++) {
			if (entires[i].getName().equals(PROPERTY_SOURCE_PREFIX + '.')) {
				IPluginLibrary library = pluginModel.getPluginFactory().createLibrary();
				try {
					library.setName("."); //$NON-NLS-1$
					pluginModel.getPluginBase().add(library);
				} catch (CoreException e) {
				}
			}
		}
	}

	private IBuildModel getBuildModel() {
		IFormPage page = getPage().getEditor().findPage(BuildInputContext.CONTEXT_ID);
		IBaseModel model = null;
		if (page instanceof BuildSourcePage)
			model = ((BuildSourcePage)page).getInputContext().getModel();
		
		if (model != null && model instanceof IBuildModel)
			return (IBuildModel)model;
		return null;
	}
	
	private void configureSourceBuildEntry(IBuildModel bmodel, String oldPath, String newPath) throws CoreException {
		IBuild build = bmodel.getBuild();
		IBuildEntry entry = build.getEntry(PROPERTY_SOURCE_PREFIX + (oldPath != null ? oldPath : newPath));
		try {
			if (newPath != null) {
				if (entry == null) {
					IProject project = ((IModel)getPage().getModel()).getUnderlyingResource().getProject();
					IJavaProject jproject = JavaCore.create(project);
					ArrayList tokens = new ArrayList();
					IClasspathEntry[] entries = jproject.getRawClasspath();
					for (int i = 0; i < entries.length; i++)
						if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE)
							tokens.add(entries[i].getPath().removeFirstSegments(1).addTrailingSeparator().toString());
					if (tokens.size() == 0)
						return;
					
					entry = bmodel.getFactory().createEntry(PROPERTY_SOURCE_PREFIX + newPath);
					for (int i = 0; i < tokens.size(); i++)
						entry.addToken((String)tokens.get(i));
					build.add(entry);
				} else
					entry.setName(PROPERTY_SOURCE_PREFIX + newPath);
			} else if (entry != null && newPath == null)
				build.remove(entry);
		} catch (JavaModelException e) {
		}
	}
	
	private void handleAdd() {
		final boolean[] updateClasspath = new boolean[] {true};
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getPage().getSite().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider()) {
			protected Control createDialogArea(Composite parent) {
				Composite comp = (Composite)super.createDialogArea(parent);
				final Button button = new Button(comp, SWT.CHECK);
				button.setText(PDEUIMessages.LibrarySection_addDialogButton);
				button.setSelection(updateClasspath[0]);
				button.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						updateClasspath[0] = button.getSelection();
					}
				});
				return comp;
			}
		};
				
		Class[] acceptedClasses = new Class[] { IFile.class };
		dialog.setValidator(new LibrarySelectionValidator(acceptedClasses, true));
		dialog.setTitle(PDEUIMessages.BuildEditor_ClasspathSection_jarsTitle); 
		dialog.setMessage(PDEUIMessages.ClasspathSection_jarsMessage); 
		IPluginLibrary[] libraries = getModel().getPluginBase().getLibraries();
		HashSet set = new HashSet();
		for (int i = 0; i < libraries.length; i++) 
			set.add(new Path(ClasspathUtilCore.expandLibraryName(libraries[i].getName())));
		
		dialog.addFilter(new LibraryFilter(set));
		IProject project = ((IModel)getPage().getModel()).getUnderlyingResource().getProject();
		dialog.setInput(project);
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			String[] filePaths = new String[elements.length];
			IPluginModelBase model = getModel();
			ArrayList list = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				IResource elem = (IResource) elements[i];
				IPath path = elem.getProjectRelativePath();
				if (elem instanceof IFolder)
					path = path.addTrailingSeparator();
				filePaths[i] = path.toString();
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				try {
					library.setName(filePaths[i]);
					library.setExported(true);
					model.getPluginBase().add(library);
					list.add(library);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			checkSourceRootEntry();
			updateBuildProperties(new String[filePaths.length], filePaths, false);
			if (updateClasspath[0])
				updateJavaClasspathLibs(new String[filePaths.length], filePaths);
			fLibraryTable.setSelection(new StructuredSelection(list.toArray()));
	        fLibraryTable.getTable().setFocus();
		}
	}

	private void updateBuildProperties(final String[] oldPaths, final String[] newPaths, boolean modifySourceEntry) {
		IBuildModel bmodel = getBuildModel();
		if (bmodel == null)
			return;
		
		IBuild build = bmodel.getBuild();
		
		IBuildEntry entry = build.getEntry(PROPERTY_BIN_INCLUDES);
		if (entry == null)
			entry = bmodel.getFactory().createEntry(PROPERTY_BIN_INCLUDES);
		
		try {
			// adding new entries
			if (oldPaths[0] == null) {
				for (int i = 0; i < newPaths.length; i++)
					if (newPaths[i] != null) {
						entry.addToken(newPaths[i]);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, null, newPaths[i]);
					}
			// removing entries
			} else if (newPaths[0] == null) {
				for (int i = 0; i < oldPaths.length; i++)
					if (oldPaths[i] != null) {
						entry.removeToken(oldPaths[i]);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, oldPaths[i], null);
					}
				if (entry.getTokens().length == 0)
					build.remove(entry);
			// rename entries
			} else {
				for (int i = 0; i < oldPaths.length; i++)
					if (newPaths[i] != null && oldPaths[i] != null) {
						entry.renameToken(oldPaths[i], newPaths[i]);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, oldPaths[i], newPaths[i]);
					}
			}
		} catch (CoreException e) {
		}
	}

	private void updateJavaClasspathLibs(String[] oldPaths, String[] newPaths) {
		IProject project = ((IModel)getPage().getModel()).getUnderlyingResource().getProject();
		IJavaProject jproject = JavaCore.create(project);
		try {
			IClasspathEntry[] entries = jproject.getRawClasspath();
			ArrayList toBeAdded = new ArrayList();
			int index = -1;
			entryLoop: for (int i = 0; i < entries.length; i++) {
				if (entries[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					if (index == -1)
						index = i;
					// do not add the old paths (handling deletion/renaming)
					IPath path = entries[i].getPath().removeFirstSegments(1).removeTrailingSeparator();
					for (int j = 0; j < oldPaths.length; j++)
						if (oldPaths[j] != null &&
								path.equals(new Path(oldPaths[j]).removeTrailingSeparator()))
							continue entryLoop;
				} else if (entries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER)
					if (index == -1)
						index = i;
				toBeAdded.add(entries[i]);
			}
			if (index == -1)
				index = entries.length;
			
			// add paths
			for (int i = 0; i < newPaths.length; i++) {
				if (newPaths[i] == null)
					continue;
				IClasspathEntry entry = JavaCore.newLibraryEntry(
						project.getFullPath().append(newPaths[i]), null, null, true);
				if (!toBeAdded.contains(entry))
					toBeAdded.add(index++, entry);
			}
			
			if (toBeAdded.size() == entries.length)
				return;
			
			IClasspathEntry[] updated = (IClasspathEntry[])toBeAdded.toArray(new IClasspathEntry[toBeAdded.size()]);
			jproject.setRawClasspath(updated, null);
		} catch (JavaModelException e) {
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
			IPluginModelBase model = getModel();
			IProject project = model.getUnderlyingResource().getProject();
			IPluginLibrary library = (IPluginLibrary) entry;
			model.getPluginBase().remove(library);
			String[] oldValue = { library.getName() };
			String[] newValue = { value };
			library.setName(value);
			boolean memberExists = project.findMember(value) != null;
			updateBuildProperties(oldValue, newValue, !memberExists);
			updateJavaClasspathLibs(oldValue, memberExists ? newValue : new String[] {null});
			model.getPluginBase().add(library);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
    private IPluginModelBase getModel() {
    	return (IPluginModelBase)getPage().getModel();
    }
}
