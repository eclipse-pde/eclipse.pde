/*******************************************************************************
 * Copyright (c) 2008, 2017 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028, 248226
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.sections;

import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
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
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSReferenceSection extends TableSection implements
		IDoubleClickListener {

	private TableViewer fReferencesTable;
	private Action fRemoveAction;
	private Action fAddAction;
	private Action fEditAction;

	class ReferenceLabelProvider extends StyledCellLabelProvider {

		private DSLabelProvider labelProvider = new DSLabelProvider();

		@Override
		public void update(ViewerCell cell) {
			final Object element = cell.getElement();
			IDSReference reference = (IDSReference) element;
			String name = reference.getReferenceName();
			if (name == null || name.length() == 0)
				name = reference.getReferenceInterface();
			if (name == null)
				name = ""; //$NON-NLS-1$ // Better than an NPE
			StyledString styledString = new StyledString(name);
			String bind = reference.getReferenceBind();
			String unbind = reference.getReferenceUnbind();
			bind = (bind == null || bind.length() == 0 ? "<none>" : bind); //$NON-NLS-1$
			unbind = (unbind == null || unbind.length() == 0 ? "<none>" : unbind); //$NON-NLS-1$
			styledString
					.append(
							" [" + bind + "," + unbind + "]", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			String target = reference.getReferenceTarget();
			if (target != null)
				styledString.append(" " + target, //$NON-NLS-1$
						StyledString.QUALIFIER_STYLER);

			cell.setText(styledString.toString());
			cell.setStyleRanges(styledString.getStyleRanges());
			cell.setImage(labelProvider.getImage(reference));
			super.update(cell);
		}

		@Override
		public void dispose() {
			super.dispose();
			labelProvider.dispose();
		}

	}

	class ContentProvider implements IStructuredContentProvider {
		@Override
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
		super(page, parent, Section.DESCRIPTION | Section.TWISTIE,
				new String[] {
				Messages.DSReferenceSection_add,
				Messages.DSReferenceSection_remove,
				Messages.DSReferenceSection_edit,
				Messages.DSReferenceSection_up,
				Messages.DSReferenceSection_down });
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setDescription(Messages.DSReferenceSection_description);
		section.setExpanded(true);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));

		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION | SWT.MULTI, 2,
				toolkit);
		fReferencesTable = tablePart.getTableViewer();

		fReferencesTable.setContentProvider(new ContentProvider());
		fReferencesTable.setLabelProvider(new ReferenceLabelProvider());
		fReferencesTable.addDoubleClickListener(this);

		makeActions();

		IDSModel model = getDSModel();
		if (model != null) {
			fReferencesTable.setInput(model);
			model.addModelChangedListener(this);
		}
		toolkit.paintBordersFor(container);
		section.setClient(container);
		updateTitle();
	}

	@Override
	public void dispose() {
		IDSModel model = getDSModel();
		if (model != null)
			model.removeModelChangedListener(this);
	}

	@Override
	public void refresh() {
		fReferencesTable.refresh();
		updateButtons();
		updateTitle();
	}

	@Override
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
		IStructuredSelection selection = fReferencesTable.getStructuredSelection();
		Object[] array = selection.toArray();
		if (moveUp) {
			moveUp(array);
		} else {
			moveDown(array);
		}
	}

	private void moveDown(Object[] array) {
		for (int i = array.length - 1; i >= 0; i--) {
			Object object = array[i];
			if (object == null) {
				continue;
			} else if (object instanceof IDocumentElementNode) {
				// Move the task object up or down one position
				getDSModel().getDSComponent().moveChildNode(
						(IDocumentElementNode) object, 1, true);
			}
		}
	}

	private void moveUp(Object[] array) {
		for (Object object : array) {
			if (object == null) {
				continue;
			} else if (object instanceof IDocumentElementNode) {
				// Move the task object up or down one position
				getDSModel().getDSComponent().moveChildNode(
						(IDocumentElementNode) object, -1, true);
			}
		}
	}

	private void handleEdit() {
		int selectionIndex = fReferencesTable.getTable().getSelectionIndex();
		if (selectionIndex != -1) {
			DSEditReferenceDialog dialog = new DSEditReferenceDialog(Activator.getActiveWorkbenchShell(),
					(IDSReference) fReferencesTable.getElementAt(selectionIndex), this);
			dialog.create();
			dialog.getShell().setSize(500, 400);
			dialog.open();
		}

	}

	private void makeActions() {
		fAddAction = new Action(Messages.DSReferenceSection_add) {
			@Override
			public void run() {
				handleAdd();
			}
		};
		fAddAction.setEnabled(isEditable());

		fRemoveAction = new Action(Messages.DSReferenceSection_remove) {
			@Override
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());

		fEditAction = new Action(Messages.DSReferenceSection_edit) {
			@Override
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
		IStructuredSelection ssel = fReferencesTable.getStructuredSelection();
		if (!ssel.isEmpty()) {
			Iterator<?> iter = ssel.iterator();
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
				for (Object element : result) {
					IType type = (IType) element;
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

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object[] objects = e.getChangedObjects();
			for (Object object : objects) {
				Table table = fReferencesTable.getTable();
				if (object instanceof IDSReference) {
					int index = table.getSelectionIndex();
					fReferencesTable.remove(object);
					if (canSelect()) {
						table.setSelection(index < table.getItemCount() ? index
								: table.getItemCount() - 1);
					}
				}
			}
			updateButtons();
			updateTitle();
		} else if (e.getChangeType() == IModelChangedEvent.INSERT) {
			Object[] objects = e.getChangedObjects();
			if (objects.length > 0) {
				fReferencesTable.refresh();
				fReferencesTable.setSelection(new StructuredSelection(
						objects[objects.length - 1]));
			}
			updateButtons();
			updateTitle();
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

	@Override
	public boolean doGlobalAction(String actionId) {
		if (!isEditable()) {
			return false;
		}

		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}

		return super.doGlobalAction(actionId);
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

	private void updateTitle() {
		int itemCount = fReferencesTable.getTable().getItemCount();
		getSection().setText(
				NLS.bind(Messages.DSReferenceSection_title, Integer.valueOf(
						itemCount)));
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		IDSReference reference = (IDSReference) fReferencesTable.getStructuredSelection().getFirstElement();
		String value = reference.getReferenceInterface();
		IProject project = getProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$',
						'.'));
				if (element != null)
					JavaUI.openInEditor(element);
			}
		} catch (PartInitException e) {
			Activator.logException(e);
		} catch (CoreException e) {
			Activator.logException(e);
		}
	}

}
