package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.pde.model.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import java.util.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.parts.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.window.*;
import org.eclipse.pde.internal.model.plugin.PluginLibrary;


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

	private FormWidgetFactory factory;
	private TableViewer libraryTable;

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginBase) {
				return ((IPluginBase) parent).getLibraries();
			}
			return new Object[0];
		}
	}

	public LibrarySection(ManifestRuntimePage page) {
		super(
			page,
			new String[] {
				PDEPlugin.getResourceString(SECTION_NEW),
				null,
				PDEPlugin.getResourceString(SECTION_UP),
				PDEPlugin.getResourceString(SECTION_DOWN)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
		if (fragment)
			setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		Composite container = createClientContainer(parent, 2, factory);
		EditableTablePart tablePart = getTablePart();
		IModel model = (IModel) getFormPage().getModel();
		tablePart.setEditable(model.isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);
		libraryTable = tablePart.getTableViewer();
		libraryTable.setContentProvider(new TableContentProvider());
		libraryTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		factory.paintBordersFor(container);

		tablePart.setButtonEnabled(2, false);
		tablePart.setButtonEnabled(3, false);
		return container;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		fireSelectionNotification(item);
		getFormPage().setSelection(selection);
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
			setDirty(true);
			commitChanges(false);
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
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		if (actionId.equals(IWorkbenchActionConstants.CUT)) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(IWorkbenchActionConstants.PASTE)) {
			doPaste();
			return true;
		}
		return false;
	}
	public void expandTo(Object object) {
		libraryTable.setSelection(new StructuredSelection(object), true);
	}

	protected void fillContextMenu(IMenuManager manager) {
		if (!(getFormPage().getModel() instanceof IEditable))
			return;
		ISelection selection = libraryTable.getSelection();

		manager.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_LIBRARY)) {
			public void run() {
				handleNew();
			}
		});

		if (!selection.isEmpty()) {
			manager.add(new Separator());
			manager.add(new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					handleDelete();
				}
			});
		}
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
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
		int index = libraryTable.getTable().getSelectionIndex();
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		IPluginLibrary[] libraries = plugin.getLibraries();
		IPluginLibrary l1 = libraries[index];
		IPluginLibrary l2 = libraries[index + 1];

		try {
			plugin.swap(l1, l2);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		updateDirectionalButtons();
	}
	private void handleNew() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginLibrary library = model.getFactory().createLibrary();
		try {
			library.setName(PDEPlugin.getResourceString(NEW_LIBRARY_ENTRY));
			model.getPluginBase().add(library);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void handleUp() {
		int index = libraryTable.getTable().getSelectionIndex();
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		IPluginLibrary[] libraries = plugin.getLibraries();
		IPluginLibrary l1 = libraries[index];
		IPluginLibrary l2 = libraries[index - 1];

		try {
			plugin.swap(l1, l2);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		updateDirectionalButtons();
	}
	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		libraryTable.setInput(model.getPluginBase());
		setReadOnly(!model.isEditable());
		getTablePart().setButtonEnabled(0, model.isEditable());
		getTablePart().setButtonEnabled(2, model.isEditable());
		getTablePart().setButtonEnabled(3, model.isEditable());
		model.addModelChangedListener(this);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			libraryTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginLibrary) {
			if (event.getChangeType() == event.INSERT) {
				libraryTable.add(changeObject);
				libraryTable.editElement(changeObject, 0);
			} else if (event.getChangeType() == event.REMOVE) {
				libraryTable.remove(changeObject);
			} else {
				if (event.getChangedProperty() == null) {
					libraryTable.update(changeObject, null);
				}
			}
		} else if (changeObject.equals(libraryTable.getInput())) {
			libraryTable.refresh();
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
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
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