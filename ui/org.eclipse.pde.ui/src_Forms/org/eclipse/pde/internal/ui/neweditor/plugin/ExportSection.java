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
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.TableSection;
import org.eclipse.pde.internal.ui.newparts.EditableTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class ExportSection extends TableSection implements IPartSelectionListener {
	private Button noExportButton;
	private Button fullExportButton;
	private Button selectedExportButton;
	private IPluginLibrary currentLibrary;
	private Composite nameFilterContainer;
	private TableViewer nameTableViewer;
	public static final String SECTION_TITLE = "ManifestEditor.ExportSection.title";
	public static final String SECTION_DESC = "ManifestEditor.ExportSection.desc";
	public static final String KEY_NO_EXPORT = "ManifestEditor.ExportSection.noExport";
	public static final String KEY_NEW_FILTER = "ManifestEditor.ExportSection.newFilter";
	public static final String KEY_FULL_EXPORT = "ManifestEditor.ExportSection.fullExport";
	public static final String KEY_SELECTED_EXPORT = "ManifestEditor.ExportSection.selectedExport";
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
	class TableContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginLibrary) {
				return createFilters(((IPluginLibrary) parent)
						.getContentFilters());
			}
			return new Object[0];
		}
	}
	class TableLabelProvider extends LabelProvider
			implements
				ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
	}
	public ExportSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[]{
				PDEPlugin.getResourceString(KEY_ADD),
				PDEPlugin.getResourceString(KEY_REMOVE)});
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		handleDefaultButton = false;
	}
	private void buttonChanged(Button selectedButton) {
		ignoreModelEvents = true;
		nameFilterContainer.setVisible(selectedButton == selectedExportButton
				&& selectedButton.getSelection());
		try {
			currentLibrary.setExported(selectedButton == selectedExportButton
					|| selectedButton == fullExportButton);
			if (selectedExportButton.getSelection() == false) {
				if (currentLibrary.getContentFilters() != null)
					currentLibrary.setContentFilters(null);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		ignoreModelEvents = false;
	}
	public void commit(boolean onSave) {
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
		ignoreModelEvents = false;
		super.commit(onSave);
	}
	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		GridData gd;
		noExportButton = toolkit.createButton(container, PDEPlugin
				.getResourceString(KEY_NO_EXPORT), SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		noExportButton.setLayoutData(gd);
		noExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (noExportButton.getSelection())
					buttonChanged(noExportButton);
			}
		});
		fullExportButton = toolkit.createButton(container, PDEPlugin
				.getResourceString(KEY_FULL_EXPORT), SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fullExportButton.setLayoutData(gd);
		fullExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fullExportButton.getSelection())
					buttonChanged(fullExportButton);
			}
		});
		selectedExportButton = toolkit.createButton(container, PDEPlugin
				.getResourceString(KEY_SELECTED_EXPORT), SWT.RADIO);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		selectedExportButton.setLayoutData(gd);
		selectedExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (selectedExportButton.getSelection())
					buttonChanged(selectedExportButton);
			}
		});
		nameFilterContainer = toolkit.createComposite(container);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL);
		nameFilterContainer.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		layout.numColumns = 2;
		nameFilterContainer.setLayout(layout);
		createNameTable(nameFilterContainer, toolkit);
		update(null);
		initialize();
		section.setClient(container);
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
	private void createNameTable(Composite parent, FormToolkit toolkit) {
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(getPage().getModel().isEditable());
		createViewerPartControl(parent, SWT.FULL_SELECTION, 2, toolkit);
		nameTableViewer = tablePart.getTableViewer();
		nameTableViewer.setContentProvider(new TableContentProvider());
		nameTableViewer.setLabelProvider(new TableLabelProvider());
		toolkit.paintBordersFor(parent);
	}
	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		//getFormPage().setSelection(selection);
		getTablePart().setButtonEnabled(1, item != null);
	}
	protected void buttonSelected(int index) {
		if (index == 0)
			handleAdd();
		else if (index == 1)
			handleDelete();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		return false;
	}
	protected void entryModified(Object entry, String newValue) {
		Item item = (Item) entry;
		final NameFilter filter = (NameFilter) item.getData();
		filter.setName(newValue);
		markDirty();
		commit(false);
		getTablePart().getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				nameTableViewer.update(filter, null);
			}
		});
	}
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	protected void fillContextMenu(IMenuManager manager) {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IAction renameAction = getRenameAction();
		renameAction.setEnabled(model.isEditable());
		manager.add(renameAction);
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager);
	}
	private void handleAdd() {
		NameFilter filter = new NameFilter(PDEPlugin
				.getResourceString(KEY_NEW_FILTER));
		filters.add(filter);
		nameTableViewer.add(filter);
		nameTableViewer.editElement(filter, 0);
		markDirty();
		commit(false);
	}
	private void handleDelete() {
		ISelection selection = nameTableViewer.getSelection();
		Object item = ((IStructuredSelection) selection).getFirstElement();
		if (item != null) {
			filters.remove(item);
			nameTableViewer.remove(item);
		}
		getTablePart().setButtonEnabled(1, false);
		markDirty();
		commit(false);
	}
	public void initialize() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
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
	public void selectionChanged(IFormPart source, ISelection selection) {
		IStructuredSelection ssel = (IStructuredSelection)selection;
		update((IPluginLibrary) ssel.getFirstElement());
	}
	private void selectButton(Button button) {
		noExportButton.setSelection(button == noExportButton);
		selectedExportButton.setSelection(button == selectedExportButton);
		fullExportButton.setSelection(button == fullExportButton);
	}
	private boolean isReadOnly() {
		IBaseModel model = getPage().getModel();
		if (model instanceof IEditable)
			return !((IEditable)model).isEditable();
		return true;
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
		} 
		if (currentLibrary == null && !isReadOnly()) {
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
