package org.eclipse.pde.internal.editor.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.dialogs.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.model.jars.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import java.util.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.window.*;

public class FolderSection
	extends PDEFormSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE = "JarsEditor.FolderSection.title";
	public static final String POPUP_NEW_FOLDER = "JarsEditor.FolderSection.newFolder";
	public static final String NEW_SOURCE_FOLDER = "JarsEditor.FolderSection.newSourceFolder";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String SECTION_DESC = "JarsEditor.FolderSection.desc";
	public static final String SECTION_NEW = "JarsEditor.FolderSection.new";
	private FormWidgetFactory factory;
	private TableViewer entryTable;
	private Button newButton;
	private Image entryImage;
	private IJarEntry currentLibrary;

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IJarEntry) {
				IJarEntry entry = (IJarEntry) parent;
				return entry.getFolderNames();
			}
			return new Object[0];
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Viewer v, Object obj, int index) {
			return getColumnText(obj, index);
		}
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Viewer v, Object obj, int index) {
			return getColumnImage(obj, index);
		}
		public Image getColumnImage(Object obj, int index) {
			return entryImage;
		}
	}


public FolderSection(JarsPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	initializeImages();
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;

	container.setLayout(layout);
	final Table table = factory.createTable(container, SWT.FULL_SELECTION);
	TableLayout tlayout = new TableLayout();

	TableColumn tableColumn = new TableColumn(table, SWT.NULL);
	tableColumn.setText("Point Name");
	ColumnLayoutData cLayout = new ColumnWeightData(100, true);
	tlayout.addColumnData(cLayout);

	//table.setLinesVisible(true);
	//table.setHeaderVisible(true);
	table.setLayout(tlayout);

	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener() {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.addMenuListener(listener);
	popupMenuManager.setRemoveAllWhenShown(true);
	Menu menu = popupMenuManager.createContextMenu(table);
	table.setMenu(menu);

	entryTable = new TableViewer(table);
	entryTable.setContentProvider(new TableContentProvider());
	entryTable.setLabelProvider(new TableLabelProvider());
	factory.paintBordersFor(container);
/*
	if (getFormPage().getModel() instanceof IEditable) {

		CellEditor[] editors = new CellEditor[] { new ModifiedTextCellEditor(table)};
		String[] properties = { "name" };
		entryTable.setCellEditors(editors);
		entryTable.setCellModifier(new NameModifier());
		entryTable.setColumnProperties(properties);
	}
*/

	entryTable.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			Object item = ((IStructuredSelection) event.getSelection()).getFirstElement();
			fireSelectionNotification(item);
			getFormPage().setSelection(event.getSelection());
		}
	});

	GridData gd = new GridData(GridData.FILL_BOTH);
	table.setLayoutData(gd);

	Composite buttonContainer = factory.createComposite(container);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	layout = new GridLayout();
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	buttonContainer.setLayout(layout);

	newButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_NEW), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.verticalAlignment = GridData.BEGINNING;
	newButton.setLayoutData(gd);
	newButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleNew();
		}
	});
	return container;
}
public void dispose() {
	IJarsModel model = (IJarsModel)getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
	}
}
private void fillContextMenu(IMenuManager manager) {
	if (!(getFormPage().getModel() instanceof IEditable)) return;
	ISelection selection = entryTable.getSelection();

	manager.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_FOLDER)) {
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
	manager.add(new Separator());
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}
private void handleDelete() {
	IJarsModel jarsModel = (IJarsModel) getFormPage().getModel();
	if (jarsModel.isEditable() == false)
		return;
	Object object =
		((IStructuredSelection) entryTable.getSelection()).getFirstElement();
	if (object != null && object instanceof String) {
		IJarEntry entry = currentLibrary;
		if (entry != null) {
			try {
				entry.removeFolderName(object.toString());
				entryTable.remove(object);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
}
private void handleNew() {
	IJarsModel model = (IJarsModel) getFormPage().getModel();
	IFile file = (IFile) model.getUnderlyingResource();
	IProject project = file.getProject();
	ContainerSelectionDialog dialog =
		new ContainerSelectionDialog(
			PDEPlugin.getActiveWorkbenchShell(),
			project,
			true,
			PDEPlugin.getResourceString(NEW_SOURCE_FOLDER));
	if (dialog.open() == ContainerSelectionDialog.OK) {
		Object[] result = dialog.getResult();
		if (result.length == 1) {
			IPath path = (IPath) result[0];
			String folder = path.lastSegment();
			try {
				IJarEntry entry = currentLibrary;
				entry.addFolderName(folder);
				entryTable.add(folder);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
}
public void initialize(Object input) {
	IJarsModel model = (IJarsModel)input;
	setReadOnly(!model.isEditable());
	newButton.setEnabled(model.isEditable());
	model.addModelChangedListener(this);
}
private void initializeImages() {
	IWorkbench workbench = PlatformUI.getWorkbench();
	ISharedImages sharedImages = workbench.getSharedImages();
	entryImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		entryTable.refresh();
		return;
	}
/*
	Object changeObject = event.getChangedObjects()[0];
	if (changeObject instanceof IPluginLibrary) {
		if (event.getChangeType()==event.INSERT) {
			libraryTable.add(changeObject);
			libraryTable.editElement(changeObject, 0);
			//libraryTable.setSelection(new StructuredSelection(changeObject), true);
			//libraryTable.getTable().setFocus();
		}
		else if (event.getChangeType()==event.REMOVE) {
			libraryTable.remove(changeObject);
		}
		else {
			//libraryTable.update(changeObject, null);
		}
	}
*/
}
public void sectionChanged(
	FormSection source,
	int changeType,
	Object changeObject) {
	IJarEntry library = (IJarEntry) changeObject;
	update(library);
}
public void setFocus() {
	entryTable.getTable().setFocus();
}
private void update(IJarEntry library) {
	currentLibrary = library;
	entryTable.setInput(currentLibrary);
}
}
