/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028, 248226
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.sections;

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.DSLabelProvider;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.editor.dialogs.DSEditReferenceDialog;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSReferenceSection extends TableSection {

	private TableViewer fReferencesTable;
	private Action fRemoveAction;
	private Action fAddAction;
	private Action fEditAction;
	private Action fUpAction;
	private Action fDownAction;

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
				Messages.DSReferenceSection_remove,
				Messages.DSReferenceSection_edit,
				Messages.DSReferenceSection_up,
				Messages.DSReferenceSection_down });
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(Messages.DSReferenceSection_title);
		section.setDescription(Messages.DSReferenceSection_description);

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));

		GridData data = new GridData(GridData.FILL_BOTH);
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
		case 2:
			handleEdit();
			break;
		case 3:
			handleMove(true);
			break;
		case 4:
			handleMove(false);
			break;
		}
	}

	private void handleMove(boolean moveUp) {
		ISelection selection = fReferencesTable.getSelection();
		if (selection != null) {

			int selectionIndex = fReferencesTable.getTable()
					.getSelectionIndex();
			if (selectionIndex != -1) {

				IDSReference ref = (IDSReference) fReferencesTable
						.getElementAt(selectionIndex);
				if (ref != null) {
					int positionFlag = moveUp ? -1 : 1;
					ref.getComponent().moveChildNode(ref, positionFlag, true);
				}
			}
		}

	}

	private void handleEdit() {

		ISelection selection = fReferencesTable.getSelection();
		if (selection != null) {

			int selectionIndex = fReferencesTable.getTable()
					.getSelectionIndex();
			if (selectionIndex != -1) {
				DSEditReferenceDialog dialog = new DSEditReferenceDialog(
						Activator.getActiveWorkbenchShell(),
						(IDSReference) fReferencesTable
								.getElementAt(selectionIndex), this);
				dialog.create();
				dialog.getShell().setSize(500, 400);
				dialog.open();
			}

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

		fEditAction = new Action(Messages.DSReferenceSection_edit) {
			public void run() {
				handleEdit();
			}
		};
		fEditAction.setEnabled(isEditable());

	}

	private void updateButtons() {
		Table table = fReferencesTable.getTable();
		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(0, isEditable());
		int length = table.getSelection().length;
		tablePart.setButtonEnabled(1, isEditable() && length > 0);
		tablePart.setButtonEnabled(2, isEditable() && length > 0);
		
		tablePart.setButtonEnabled(3, isEditable()
				&& table.getSelection().length > 0 && !table.isSelected(0));
		tablePart.setButtonEnabled(4, isEditable()
				&& table.getSelection().length > 0
				&& !table.isSelected(table.getItems().length - 1));
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
		doOpenSelectionDialog(IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES);
	}

	private void doOpenSelectionDialog(int scopeType) {
		try {
			String filter = ""; //$NON-NLS-1$
			filter = filter.substring(filter.lastIndexOf(".") + 1); //$NON-NLS-1$
			SelectionDialog dialog = JavaUI.createTypeDialog(Activator
					.getActiveWorkbenchShell(), PlatformUI.getWorkbench()
					.getProgressService(), SearchEngine.createWorkspaceScope(),
					scopeType, true, filter);
			dialog.setTitle(Messages.DSReferenceDetails_selectType);
			if (dialog.open() == Window.OK) {
				Object[] result = dialog.getResult();
				for (int i = 0; i < result.length; i++) {
					IType type = (IType) result[i];
					String fullyQualifiedName = type.getFullyQualifiedName('$');
					addReference(fullyQualifiedName);
				}
			}
		} catch (CoreException e) {
		}
	}

	private void addReference(String fullyQualifiedName) {

		IDSReference reference = getDSModel().getFactory().createReference();
		// set interface attribute
		reference.setReferenceInterface(fullyQualifiedName);

		// set name attribute
		int index = fullyQualifiedName.lastIndexOf("."); //$NON-NLS-1$
		if (index != -1) {
			fullyQualifiedName = fullyQualifiedName.substring(index + 1);
		}
		reference.setReferenceName(fullyQualifiedName);

		// add reference
		getDSModel().getDSComponent().addReference(reference);
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
		} else {
			fReferencesTable.refresh();
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
