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
import org.eclipse.pde.model.plugin.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.parts.*;
import org.eclipse.pde.model.*;
import org.eclipse.swt.graphics.*;
import java.util.*;
import org.eclipse.pde.internal.PDEPlugin;

public class ExportSection extends TableSection {
	private Button noExportButton;
	private Button fullExportButton;
	private Button selectedExportButton;
	private IPluginLibrary currentLibrary;
	private Composite nameFilterContainer;
	private TableViewer nameTableViewer;
	public static final String SECTION_TITLE = "ManifestEditor.ExportSection.title";
	public static final String SECTION_DESC = "ManifestEditor.ExportSection.desc";
	public static final String KEY_NO_EXPORT =
		"ManifestEditor.ExportSection.noExport";
	public static final String KEY_NEW_FILTER =
		"ManifestEditor.ExportSection.newFilter";
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
		super(
			formPage,
			new String[] {
				PDEPlugin.getResourceString(KEY_ADD),
				PDEPlugin.getResourceString(KEY_REMOVE)});
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
			if (selectedExportButton.getSelection() == false) {
				if (currentLibrary.getContentFilters() != null)
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

		noExportButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(KEY_NO_EXPORT),
				SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		noExportButton.setLayoutData(gd);
		noExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (noExportButton.getSelection())
					buttonChanged(noExportButton);
			}
		});
		fullExportButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(KEY_FULL_EXPORT),
				SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fullExportButton.setLayoutData(gd);
		fullExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fullExportButton.getSelection())
					buttonChanged(fullExportButton);
			}
		});
		selectedExportButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(KEY_SELECTED_EXPORT),
				SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		selectedExportButton.setLayoutData(gd);
		selectedExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (selectedExportButton.getSelection())
					buttonChanged(selectedExportButton);
			}
		});

		nameFilterContainer = factory.createComposite(container);
		gd =
			new GridData(
				GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		nameFilterContainer.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		layout.numColumns = 2;
		nameFilterContainer.setLayout(layout);

		createNameTable(nameFilterContainer, factory);
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
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(((IModel) getFormPage().getModel()).isEditable());
		createViewerPartControl(parent, SWT.FULL_SELECTION, 2, factory);

		nameTableViewer = tablePart.getTableViewer();
		nameTableViewer.setContentProvider(new TableContentProvider());
		nameTableViewer.setLabelProvider(new TableLabelProvider());
		factory.paintBordersFor(parent);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		getFormPage().setSelection(selection);
		getTablePart().setButtonEnabled(1, item != null);
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleDelete();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		return false;
	}
	protected void entryModified(Object entry, String newValue) {
		Item item = (Item) entry;
		final NameFilter filter = (NameFilter) item.getData();
		filter.setName(newValue);
		setDirty(true);
		commitChanges(false);
		getTablePart().getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				nameTableViewer.update(filter, null);
			}
		});
	}
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	protected void fillContextMenu(IMenuManager manager) {
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
		getTablePart().setButtonEnabled(1, false);
		setDirty(true);
		commitChanges(false);
	}
	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		setReadOnly(!model.isEditable());
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (ignoreModelEvents)
			return;
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
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
		} else if (currentLibrary == null && !isReadOnly()) {
			fullExportButton.setEnabled(true);
			noExportButton.setEnabled(true);
			selectedExportButton.setEnabled(true);

		}
		this.currentLibrary = library;
		if (library.isFullyExported())
			selectButton(fullExportButton);
		else if (library.isExported() == false)
			selectButton(noExportButton);
		else {
			selectButton(selectedExportButton);
		}
		nameFilterContainer.setVisible(selectedExportButton.getSelection());
		filters = null;
		nameTableViewer.setInput(library);
		getTablePart().setButtonEnabled(1, false);
	}
}