package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.layout.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.parts.*;
import org.eclipse.swt.graphics.*;
import java.util.*;
import org.eclipse.pde.internal.PDEPlugin;

public class ExportSection extends PDEFormSection {
	private Button noExportButton;
	private Button fullExportButton;
	private Button selectedExportButton;
	private IPluginLibrary currentLibrary;
	private Composite nameFilterContainer;
	private Table nameTable;
	private TableViewer nameTableViewer;
	private Button newNameButton;
	private Button deleteNameButton;
	public static final String SECTION_TITLE = "ManifestEditor.ExportSection.title";
	public static final String SECTION_DESC = "ManifestEditor.ExportSection.desc";
	public static final String KEY_NO_EXPORT =
		"ManifestEditor.ExportSection.noExport";
	public static final String KEY_NEW_FILTER = "ManifestEditor.ExportSection.newFilter";
	public static final String KEY_FULL_EXPORT =
		"ManifestEditor.ExportSection.fullExport";
	public static final String KEY_SELECTED_EXPORT =
		"ManifestEditor.ExportSection.selectedExport";
	public static final String KEY_ADD = "ManifestEditor.ExportSection.add";
	public static final String KEY_REMOVE = "ManifestEditor.ExportSection.remove";
	private Vector filters;
	private boolean ignoreModelEvents;

	class NameFilter {
		private String name;
		public NameFilter(String name) {
			this.name = name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public String toString() {
			return name;
		}
	}

	class NameModifier implements ICellModifier {
		public boolean canModify(Object object, String property) {
			return true;
		}
		public void modify(Object object, String property, Object value) {
			Item item = (Item) object;
			final NameFilter filter = (NameFilter) item.getData();
			filter.setName(value.toString());
			setDirty(true);
			commitChanges(false);
			nameTable.getDisplay().asyncExec(new Runnable() {
				public void run() {
					nameTableViewer.update(filter, null);
				}
			});
		}
		public Object getValue(Object object, String property) {
			return object.toString();
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginLibrary) {
				return createFilters(((IPluginLibrary) parent).getContentFilters());
			}
			return new Object[0];
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}

public ExportSection(ManifestRuntimePage formPage) {
	super(formPage);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
private void buttonChanged(Button selectedButton) {
	ignoreModelEvents = true;
	nameFilterContainer.setVisible(
		selectedButton == selectedExportButton && selectedButton.getSelection());
	try {
		currentLibrary.setExported(
			selectedButton == selectedExportButton || selectedButton == fullExportButton);
		if (selectedExportButton.getSelection()==false) {
			if (currentLibrary.getContentFilters()!=null)
			   currentLibrary.setContentFilters(null);
		}
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
	ignoreModelEvents = false;
}
public void commitChanges(boolean onSave) {
	if (isDirty() == false)
		return;
	ignoreModelEvents = true;
	if (filters != null && currentLibrary != null) {
		try {
			if (filters.size() == 0) {
				currentLibrary.setContentFilters(null);
			} else {
				String[] result = new String[filters.size()];
				for (int i = 0; i < filters.size(); i++) {
					result[i] = filters.elementAt(i).toString();
				}
				currentLibrary.setContentFilters(result);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	setDirty(false);
	ignoreModelEvents = false;
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	container.setLayout(layout);
	GridData gd;

	noExportButton = factory.createButton(container, PDEPlugin.getResourceString(KEY_NO_EXPORT), SWT.RADIO);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	noExportButton.setLayoutData(gd);
	noExportButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			if (noExportButton.getSelection())
				buttonChanged(noExportButton);
		}
	});
	fullExportButton = factory.createButton(container, PDEPlugin.getResourceString(KEY_FULL_EXPORT), SWT.RADIO);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	fullExportButton.setLayoutData(gd);
	fullExportButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			if (fullExportButton.getSelection())
				buttonChanged(fullExportButton);
		}
	});
	selectedExportButton =
		factory.createButton(container, PDEPlugin.getResourceString(KEY_SELECTED_EXPORT), SWT.RADIO);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	selectedExportButton.setLayoutData(gd);
	selectedExportButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			if (selectedExportButton.getSelection())
				buttonChanged(selectedExportButton);
		}
	});

	nameFilterContainer = factory.createComposite(container);
	gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	nameFilterContainer.setLayoutData(gd);
	layout = new GridLayout();
	layout.marginWidth = 2;
	layout.marginHeight = 2;
	layout.numColumns = 2;
	nameFilterContainer.setLayout(layout);

	createNameTable(nameFilterContainer, factory);

	Composite buttonContainer = factory.createComposite(nameFilterContainer);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	layout = new GridLayout();
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	buttonContainer.setLayout(layout);
	newNameButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(KEY_ADD), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	newNameButton.setLayoutData(gd);
	newNameButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleAdd();
		}
	});
	deleteNameButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(KEY_REMOVE), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	deleteNameButton.setLayoutData(gd);
	deleteNameButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleDelete();
		}
	});
	update(null);
	return container;
}
private Object[] createFilters(String[] names) {
	if (filters == null) {
		filters = new Vector();
		if (names != null) {
			for (int i = 0; i < names.length; i++) {
				filters.add(new NameFilter(names[i]));
			}
		}
	}
	Object[] result = new Object[filters.size()];
	filters.copyInto(result);
	return result;
}
private void createNameTable(Composite parent, FormWidgetFactory factory) {
	nameTable = new Table(parent, SWT.FULL_SELECTION);
	GridData gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL | GridData.GRAB_HORIZONTAL);
	nameTable.setLayoutData(gd);
	factory.hookDeleteListener(nameTable);

	TableLayout tlayout = new TableLayout();

	TableColumn tableColumn = new TableColumn(nameTable, SWT.NULL);
	tableColumn.setText("Filter");
	ColumnLayoutData cLayout = new ColumnWeightData(100, true);
	tlayout.addColumnData(cLayout);
	nameTable.setLayout(tlayout);

	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener () {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.addMenuListener(listener);
	popupMenuManager.setRemoveAllWhenShown(true);
	Menu menu=popupMenuManager.createContextMenu(nameTable);
	nameTable.setMenu(menu);

	nameTableViewer = new TableViewer(nameTable);
	nameTableViewer.setContentProvider(new TableContentProvider());
	nameTableViewer.setLabelProvider(new TableLabelProvider());
	factory.paintBordersFor(parent);

	nameTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			Object item = ((IStructuredSelection)event.getSelection()).getFirstElement();
			//fireSelectionNotification(item);
			getFormPage().setSelection(event.getSelection());
			if (item!=null) deleteNameButton.setEnabled(true);
		}
	});
	if (((IPluginModelBase)getFormPage().getModel()).isEditable()) {
	   CellEditor [] editors = new CellEditor [] { new ModifiedTextCellEditor(nameTable) };
	   String [] properties = new String [] { "name" };
	   nameTableViewer.setCellEditors(editors);
	   nameTableViewer.setColumnProperties(properties);
	   nameTableViewer.setCellModifier(new NameModifier());
	}
}
public void dispose() {
	IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
}
private void fillContextMenu(IMenuManager manager) {
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}
private void handleAdd() {
	NameFilter filter = new NameFilter(PDEPlugin.getResourceString(KEY_NEW_FILTER));
	filters.add(filter);
	nameTableViewer.add(filter);
	nameTableViewer.editElement(filter, 0);
	setDirty(true);
	commitChanges(false);
}
private void handleDelete() {
	ISelection selection = nameTableViewer.getSelection();
	Object item = ((IStructuredSelection) selection).getFirstElement();
	if (item != null) {
		filters.remove(item);
		nameTableViewer.remove(item);
	}
	deleteNameButton.setEnabled(false);
	setDirty(true);
	commitChanges(false);
}
public void initialize(Object input) {
	IPluginModelBase model = (IPluginModelBase) input;
	setReadOnly(!model.isEditable());
	model.addModelChangedListener(this);
}

public void modelChanged(IModelChangedEvent e) {
	if (ignoreModelEvents) return;
	if (e.getChangeType()==IModelChangedEvent.CHANGE) {
		Object object = e.getChangedObjects()[0];
		if (object.equals(currentLibrary)) {
			update(currentLibrary);
		}
	}
}

public void sectionChanged(
	FormSection source,
	int changeType,
	Object changeObject) {
	update((IPluginLibrary) changeObject);
}
private void selectButton(Button button) {
	noExportButton.setSelection(button == noExportButton);
	selectedExportButton.setSelection(button == selectedExportButton);
	fullExportButton.setSelection(button == fullExportButton);
}
private void update(IPluginLibrary library) {
	if (library == null) {
		nameFilterContainer.setVisible(false);
		fullExportButton.setEnabled(false);
		fullExportButton.setSelection(false);
		noExportButton.setEnabled(false);
		noExportButton.setSelection(false);
		selectedExportButton.setEnabled(false);
		selectedExportButton.setSelection(false);
		currentLibrary = null;
		return;
	} else
		if (currentLibrary == null && !isReadOnly()) {
			fullExportButton.setEnabled(true);
			noExportButton.setEnabled(true);
			selectedExportButton.setEnabled(true);

		}
	this.currentLibrary = library;
	if (library.isFullyExported())
		selectButton(fullExportButton);
	else
		if (library.isExported() == false)
			selectButton(noExportButton);
		else {
			selectButton(selectedExportButton);
		}
	nameFilterContainer.setVisible(selectedExportButton.getSelection());
	filters = null;
	nameTableViewer.setInput(library);
	deleteNameButton.setEnabled(false);
}
}
