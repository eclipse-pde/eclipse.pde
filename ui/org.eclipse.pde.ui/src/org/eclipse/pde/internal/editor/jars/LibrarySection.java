package org.eclipse.pde.internal.editor.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
import org.eclipse.update.ui.forms.internal.*;
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


public class LibrarySection
	extends PDEFormSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE = "JarsEditor.LibrarySection.title";
	public static final String SECTION_DESC = "JarsEditor.LibrarySection.desc";
	public static final String SECTION_FDESC = "JarsEditor.LibrarySection.fdesc";
	public static final String SECTION_NEW = "JarsEditor.LibrarySection.new";
	public static final String POPUP_NEW_LIBRARY = "JarsEditor.LibrarySection.newLibrary";
	public static final String NEW_LIBRARY_ENTRY = "JarsEditor.LibrarySection.newLibraryEntry";
	public static final String POPUP_DELETE = "Actions.delete.label";
	private FormWidgetFactory factory;
	private TableViewer libraryTable;
	private Button newButton;
	private Image libraryImage;

	class NameModifier implements ICellModifier {
		public boolean canModify(Object object, String property) {
			return true;
		}
		public void modify(Object object, String property, Object value) {
			Item item = (Item) object;
			final IJarEntry entry = (IJarEntry) item.getData();
			try {
				String newValue = value.toString();
				if (newValue.equals(entry.getName()))
					return;
				entry.setName(value.toString());

				setDirty(true);
				commitChanges(false);
				libraryTable.getTable().getDisplay().asyncExec(new Runnable() {
					public void run() {
						libraryTable.update(entry, null);
					}
				});
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		public Object getValue(Object object, String property) {
			return object.toString();
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IJarsModel) {
				return ((IJarsModel) parent).getJars().getJarEntries();
			}
			return new Object[0];
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Viewer v, Object obj, int index) {
			return getColumnText(obj, index);
		}
		public String getColumnText(Object obj, int index) {
			if (obj instanceof IJarEntry && index == 0) {
				return ((IJarEntry) obj).getName();
			}
			return obj.toString();
		}
		public Image getColumnImage(Viewer v, Object obj, int index) {
			return getColumnImage(obj, index);
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0)
				return libraryImage;
			return null;
		}
	}


public LibrarySection(JarsPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	IJarsModel model = (IJarsModel) page.getModel();
	if (model.isFragment())
		setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
	else
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

	libraryTable = new TableViewer(table);
	libraryTable.setContentProvider(new TableContentProvider());
	libraryTable.setLabelProvider(new TableLabelProvider());
	factory.paintBordersFor(container);

	IJarsModel model = (IJarsModel) getFormPage().getModel();
	if (model.isEditable()) {
		CellEditor[] editors = new CellEditor[] { new ModifiedTextCellEditor(table)};
		String[] properties = { "name" };
		libraryTable.setCellEditors(editors);
		libraryTable.setCellModifier(new NameModifier());
		libraryTable.setColumnProperties(properties);
	}

	libraryTable.addSelectionChangedListener(new ISelectionChangedListener() {
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

	newButton =
		factory.createButton(
			buttonContainer,
			PDEPlugin.getResourceString(SECTION_NEW),
			SWT.PUSH);
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
	libraryImage.dispose();
	IJarsModel model = (IJarsModel)getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
	}
}
public void expandTo(Object object) {
	libraryTable.setSelection(new StructuredSelection(object), true);
}
private void fillContextMenu(IMenuManager manager) {
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
	if (object != null && object instanceof IJarEntry) {
		IJarEntry entry = (IJarEntry) object;
		IJars jars = entry.getModel().getJars();
		try {
			jars.remove(entry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
private void handleNew() {
	IJarsModel model = (IJarsModel) getFormPage().getModel();
	IJars jars = model.getJars();
	IJarEntry entry = model.getFactory().createEntry(PDEPlugin.getResourceString(NEW_LIBRARY_ENTRY));
	try {
		jars.add(entry);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}
public void initialize(Object input) {
	IJarsModel model = (IJarsModel)input;
	libraryTable.setInput(model);
	setReadOnly(!model.isEditable());
	newButton.setEnabled(model.isEditable());
	model.addModelChangedListener(this);
}
private void initializeImages() {
	libraryImage = PDEPluginImages.DESC_JAVA_LIB_OBJ.createImage();
}
public void modelChanged(IModelChangedEvent event) {
	if (event.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		libraryTable.refresh();
		return;
	}
	Object changeObject = event.getChangedObjects()[0];
	if (changeObject instanceof IJarEntry) {
		if (event.getChangeType()==event.INSERT) {
			libraryTable.add(changeObject);
			libraryTable.editElement(changeObject, 0);
		}
		else if (event.getChangeType()==event.REMOVE) {
			libraryTable.remove(changeObject);
		}
		else {
			if (event.getChangedProperty()==null) {
				libraryTable.update(changeObject, null);
			}
		}
	}
}
public void setFocus() {
	libraryTable.getTable().setFocus();
}
}
