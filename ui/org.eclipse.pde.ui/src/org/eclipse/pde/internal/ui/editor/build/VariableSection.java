package org.eclipse.pde.internal.ui.editor.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class VariableSection
	extends TableSection
	implements IModelChangedListener {
	public static final String SECTION_TITLE = "BuildEditor.VariableSection.title";
	public static final String DIALOG_TITLE =
		"BuildEditor.VariableSection.dialogTitle";
	public static final String POPUP_NEW_VARIABLE =
		"BuildEditor.VariableSection.newVariable";
	public static final String POPUP_DELETE = "BuildEditor.VariableSection.delete";
	public static final String SECTION_NEW = "BuildEditor.VariableSection.new";
	public static final String SECTION_DESC = "BuildEditor.VariableSection.desc";
	private FormWidgetFactory factory;
	private TableViewer variableTable;

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				Object[] result = ((IBuildModel) parent).getBuild().getBuildEntries();
				return result;
			}
			return new Object[0];
		}
	}

	public VariableSection(BuildPage page) {
		super(page, new String[] { PDEPlugin.getResourceString(SECTION_NEW)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		Composite container = createClientContainer(parent, 2, factory);

		EditableTablePart tablePart = getTablePart();
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		tablePart.setEditable(model.isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);

		variableTable = tablePart.getTableViewer();
		variableTable.setContentProvider(new TableContentProvider());
		variableTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		factory.paintBordersFor(container);
		return container;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		fireSelectionNotification(item);
		getFormPage().setSelection(selection);
	}

	protected void entryModified(Object object, String newValue) {
		Item item = (Item) object;
		final IBuildEntry entry = (IBuildEntry) item.getData();
		try {
			if (newValue.equals(entry.getName()))
				return;
			entry.setName(newValue);
			setDirty(true);
			commitChanges(false);
			variableTable.getTable().getDisplay().asyncExec(new Runnable() {
				public void run() {
					variableTable.update(entry, null);
				}
			});
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
	}

	public void dispose() {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		return false;
	}
	public void expandTo(Object object) {
		variableTable.setSelection(new StructuredSelection(object), true);
	}
	protected void fillContextMenu(IMenuManager manager) {
		IModel model = (IModel) getFormPage().getModel();
		if (!model.isEditable())
			return;
		ISelection selection = variableTable.getSelection();

		manager.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_VARIABLE)) {
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
		Object object =
			((IStructuredSelection) variableTable.getSelection()).getFirstElement();
		if (object != null && object instanceof IBuildEntry) {
			IBuildEntry entry = (IBuildEntry) object;
			IBuild build = entry.getModel().getBuild();
			try {
				build.remove(entry);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	private void handleNew() {
		final IBuildModel model = (IBuildModel) getFormPage().getModel();
		final IBuild build = model.getBuild();

		BusyIndicator.showWhile(variableTable.getTable().getDisplay(), new Runnable() {
			public void run() {
				VariableSelectionDialog dialog =
					new VariableSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), model);
				dialog.create();
				dialog.getShell().setText(PDEPlugin.getResourceString(DIALOG_TITLE));
				dialog.getShell().setSize(300, 350);
				if (dialog.open() == VariableSelectionDialog.OK) {
					IBuildEntry entry =
						model.getFactory().createEntry(dialog.getSelectedVariable());
					try {
						build.add(entry);
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			}
		});
	}
	public void initialize(Object input) {
		IBuildModel model = (IBuildModel) input;
		variableTable.setInput(model);
		setReadOnly(!model.isEditable());
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			variableTable.refresh();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IBuildEntry) {
			if (event.getChangeType() == event.INSERT) {
				variableTable.add(changeObject);
			} else if (event.getChangeType() == event.REMOVE) {
				variableTable.remove(changeObject);
			} else {
				if (event.getChangedProperty() == null) {
					variableTable.update(changeObject, null);
				}
			}
		}
	}
	public void setFocus() {
		variableTable.getTable().setFocus();
	}
}