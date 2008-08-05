/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSReferenceSection extends TableSection {

	private TableViewer fReferencesTable;
	private Action fRemoveAction;
	private Action fAddAction;

	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IDSModel) {
				IDSModel model = (IDSModel) inputElement;
				IDSComponent component = model.getDSComponent();
				if (component != null)
					return component.getReferences();

			}
			return new Object[0];
		}
	}

	public DSReferenceSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {
				Messages.DSReferenceSection_add,
				Messages.DSReferenceSection_remove });
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(Messages.DSReferenceSection_title);
		section.setDescription(Messages.DSReferenceSection_title);

		section.setLayout(FormLayoutFactory
				.createClearTableWrapLayout(false, 1));

		// TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		GridData data = new GridData(GridData.BEGINNING);
		data.widthHint = 350;
		data.heightHint = 150;
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION | SWT.MULTI, 2,
				toolkit);
		fReferencesTable = tablePart.getTableViewer();
		fReferencesTable.setContentProvider(new ContentProvider());
		fReferencesTable.setLabelProvider(new DSLabelProvider());

		 makeActions();

		IDSModel model = getDSModel();
		if (model != null) {
			fReferencesTable.setInput(model);
			model.addModelChangedListener(this);
		}
		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	public void dispose() {
		IDSModel model = getDSModel();
		if (model != null)
			model.removeModelChangedListener(this);
	}

	public void refresh() {
		fReferencesTable.refresh();
		updateButtons();
	}

	protected void buttonSelected(int index) {
		switch (index) {
		case 0:
			handleAdd();
			break;
		case 1:
			handleRemove();
			break;
		}
	}

	private void makeActions() {
		fAddAction = new Action(Messages.DSReferenceSection_add) {
			public void run() {
				handleAdd();
			}
		};
		fAddAction.setEnabled(isEditable());

		fRemoveAction = new Action(Messages.DSReferenceSection_remove) {
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());

	}

	 private void updateButtons() {
		Table table = fReferencesTable.getTable();
		int count = table.getItemCount();

		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable()
				&& table.getSelection().length > 0);
		tablePart.setButtonEnabled(2, isEditable()
				&& table.getSelection().length > 0);
	}

	 private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fReferencesTable
				.getSelection();
		if (ssel.size() > 0) {
			Iterator iter = ssel.iterator();
			while (iter.hasNext()) {
				Object object = iter.next();
				if (object instanceof IDSReference) {
					getDSModel().getDSComponent().removeReference(
							(IDSReference) object);
				}
			}
		}
	}

	private void handleAdd() {
// ElementListSelectionDialog dialog = new ElementListSelectionDialog(
// PDEPlugin.getActiveWorkbenchShell(), new DSLabelProvider());
// // dialog.setElements(getEnvironments());
		// dialog.setAllowDuplicates(false);
		// dialog.setMultipleSelection(true);
		// dialog
		// .setTitle(Messages.DSReferenceSection_dialog_title);
		// dialog
		// .setMessage(Messages.DSReferenceSection_dialogMessage);
		// if (dialog.open() == Window.OK) {
		// addExecutionEnvironments(dialog.getResult());
		// }

	}


	//
	// private void addExecutionEnvironments(Object[] result) {
	// // IManifestHeader header = getHeader();
	// // if (header == null) {
	// // StringBuffer buffer = new StringBuffer();
	// // for (int i = 0; i < result.length; i++) {
	// // String id = null;
	// // if (result[i] instanceof IExecutionEnvironment)
	// // id = ((IExecutionEnvironment) result[i]).getId();
	// // else if (result[i] instanceof ExecutionEnvironment)
	// // id = ((ExecutionEnvironment) result[i]).getName();
	// // else
	// // continue;
	// // if (buffer.length() > 0) {
	// // buffer.append(","); //$NON-NLS-1$
	// // buffer.append(getLineDelimiter());
	// // buffer.append(" "); //$NON-NLS-1$
	// // }
	// // buffer.append(id);
	// // }
	// // getDS().setHeader(
	// // Constants.DS_REQUIREDEXECUTIONENVIRONMENT,
	// // buffer.toString());
	// // } else {
	// // RequiredExecutionEnvironmentHeader ee =
	// // (RequiredExecutionEnvironmentHeader)
	// // header;
	// // ee.addExecutionEnvironments(result);
	// // }
	// }

	private String getLineDelimiter() {
		DSInputContext inputContext = getDSContext();
		if (inputContext != null) {
			return inputContext.getLineDelimiter();
		}
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object[] objects = e.getChangedObjects();
			for (int i = 0; i < objects.length; i++) {
				Table table = fReferencesTable.getTable();
				if (objects[i] instanceof IDSReference) {
					int index = table.getSelectionIndex();
					fReferencesTable.remove(objects[i]);
					if (canSelect()) {
						table.setSelection(index < table.getItemCount() ? index
								: table.getItemCount() - 1);
					}
				}
			}
			 updateButtons();
		} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
			Object[] objects = e.getChangedObjects();
			if (objects.length > 0) {
				fReferencesTable.refresh();
				fReferencesTable.setSelection(new StructuredSelection(
						objects[objects.length - 1]));
			}
			 updateButtons();
		}
	}

	private DSInputContext getDSContext() {
		InputContextManager manager = getPage().getPDEEditor()
				.getContextManager();
		return (DSInputContext) manager.findContext(DSInputContext.CONTEXT_ID);
	}

	private IDSModel getDSModel() {
		DSInputContext context = getDSContext();
		return context == null ? null : (IDSModel) context.getModel();
	}

	public boolean doGlobalAction(String actionId) {
		if (!isEditable()) {
			return false;
		}

		if (actionId.equals(ActionFactory.DELETE.getId())) {
			 handleRemove();
			return true;
		}

		return false;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

}
