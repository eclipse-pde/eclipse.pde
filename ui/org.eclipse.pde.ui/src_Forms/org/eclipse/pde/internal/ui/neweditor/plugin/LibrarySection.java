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
package org.eclipse.pde.internal.ui.neweditor.plugin;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.PluginLibrary;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.build.JarSelectionValidator;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.build.*;
import org.eclipse.pde.internal.ui.newparts.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.*;
import org.eclipse.ui.views.navigator.*;

public class LibrarySection
	extends TableSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE =
		"ManifestEditor.LibrarySection.title";
	public static final String SECTION_DESC = "ManifestEditor.LibrarySection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.LibrarySection.fdesc";
	public static final String SECTION_NEW = "ManifestEditor.LibrarySection.new";
	public static final String SECTION_UP = "ManifestEditor.LibrarySection.up";
	public static final String SECTION_DOWN = "ManifestEditor.LibrarySection.down";
	public static final String POPUP_NEW_LIBRARY =
		"ManifestEditor.LibrarySection.newLibrary";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String NEW_LIBRARY_ENTRY =
		"ManifestEditor.LibrarySection.newLibraryEntry";

	private TableViewer libraryTable;
	
	class LibraryFilter extends JARFileFilter {
		public LibraryFilter(HashSet set) {
			super(set);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.neweditor.build.JARFileFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public boolean select(Viewer viewer, Object parent, Object element) {
			if (element instanceof IFolder)
				return true;
			if (element instanceof IFile)
				return isValid(((IFile)element).getProjectRelativePath());
			return false;
		}
	}
	
	class LibrarySelectionValidator extends JarSelectionValidator {
		
		public LibrarySelectionValidator(Class[] acceptedTypes, boolean allowMultipleSelection) {
			super(acceptedTypes, allowMultipleSelection);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.pde.internal.ui.editor.build.JarSelectionValidator#isValid(java.lang.Object)
		 */
		public boolean isValid(Object element) {
			if (element instanceof IFolder)
				return true;
			return super.isValid(element);
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
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
				null,
				PDEPlugin.getResourceString(SECTION_UP),
				PDEPlugin.getResourceString(SECTION_DOWN)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		IPluginModelBase model = (IPluginModelBase)page.getPDEEditor().getAggregateModel();
		boolean fragment = model.isFragmentModel();
		if (fragment)
			getSection().setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		IModel model = (IModel) getPage().getModel();
		tablePart.setEditable(model.isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION, 2, toolkit);
		libraryTable = tablePart.getTableViewer();
		libraryTable.setContentProvider(new TableContentProvider());
		libraryTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);

		tablePart.setButtonEnabled(2, false);
		tablePart.setButtonEnabled(3, false);
		section.setClient(container);
		initialize();
	}

	protected void selectionChanged(IStructuredSelection selection) {
		//getFormPage().setSelection(selection);
		updateDirectionalButtons();
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
			case 2 :
				handleUp();
				break;
			case 3 :
				handleDown();
				break;
		}
	}

	protected void entryModified(Object entry, String newValue) {
		Item item = (Item) entry;
		final IPluginLibrary library = (IPluginLibrary) item.getData();
		try {
			if (newValue.equals(library.getName()))
				return;
			library.setName(newValue);
			markDirty();
			commit(false);
			libraryTable.getTable().getDisplay().asyncExec(new Runnable() {
				public void run() {
					libraryTable.update(library, null);
				}
			});
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.removeModelChangedListener(this);
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
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginLibrary) {
			libraryTable.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		IModel model = (IModel)getPage().getModel();
		ISelection selection = libraryTable.getSelection();

		Action newAction = new Action(PDEPlugin.getResourceString(POPUP_NEW_LIBRARY)) {
			public void run() {
				handleNew();
			}
		};
		newAction.setEnabled(model.isEditable());
		manager.add(newAction);

		if (!selection.isEmpty()) {
			manager.add(new Separator());
			IAction renameAction = getRenameAction();
			renameAction.setEnabled(model.isEditable());
			manager.add(renameAction);
			Action deleteAction = new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					handleDelete();
				}
			};
			deleteAction.setEnabled(model.isEditable());
			manager.add(deleteAction);
		}
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}
	private void handleDelete() {
		Object object =
			((IStructuredSelection) libraryTable.getSelection()).getFirstElement();
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
		/*int index = libraryTable.getTable().getSelectionIndex();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		IPluginLibrary[] libraries = plugin.getLibraries();
		IPluginLibrary l1 = libraries[index];
		IPluginLibrary l2 = libraries[index + 1];

		try {
			plugin.swap(l1, l2);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		updateDirectionalButtons();*/
	}
	
	private void handleNew() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getPage().getSite().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		Class[] acceptedClasses = new Class[] { IFile.class };
		dialog.setValidator(new LibrarySelectionValidator(acceptedClasses, true));
		dialog.setTitle(PDEPlugin.getResourceString("BuildPropertiesEditor.BuildClasspathSection.JarsSelection.title"));
		dialog.setMessage("Select JAR archives to be added to the plug-in's classpath:");
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
	private void handleUp() {
		/*int index = libraryTable.getTable().getSelectionIndex();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		IPluginLibrary[] libraries = plugin.getLibraries();
		IPluginLibrary l1 = libraries[index];
		IPluginLibrary l2 = libraries[index - 1];

		try {
			plugin.swap(l1, l2);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		updateDirectionalButtons();*/
	}
	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		libraryTable.setInput(model.getPluginBase());
		getTablePart().setButtonEnabled(0, model.isEditable());
		getTablePart().setButtonEnabled(2, false);
		getTablePart().setButtonEnabled(3, false);
		model.addModelChangedListener(this);
	}
	public void refresh() {
		libraryTable.refresh();
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
				libraryTable.add(changeObject);
				libraryTable.editElement(changeObject, 0);
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				libraryTable.remove(changeObject);
			} else {
				if (event.getChangedProperty() == null) {
					libraryTable.update(changeObject, null);
				}
			}
		} else if (changeObject.equals(libraryTable.getInput())) {
			markStale();
		}
	}
	public void setFocus() {
		libraryTable.getTable().setFocus();
	}
	private void updateDirectionalButtons() {
		Table table = libraryTable.getTable();
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
	protected void doPaste(Object target, Object[] objects) {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
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
		}
	}
	protected boolean canPaste(Object target, Object[] objects) {
		if (objects[0] instanceof IPluginLibrary) return true;
		return false;
	}
}
