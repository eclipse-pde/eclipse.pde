/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 244997, 248216
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.sections;

import java.util.Iterator;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.DSLabelProvider;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.editor.dialogs.DSEditPropertiesDialog;
import org.eclipse.pde.internal.ds.ui.editor.dialogs.DSEditPropertyDialog;
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

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class DSPropertiesSection extends TableSection {

	private TableViewer fPropertiesTable;
	private Action fRemoveAction;
	private Action fAddPropertiesAction;
	private Action fAddPropertyAction;
	private Action fEditAction;

	private static final int F_UP_FLAG = -1;
	private static final int F_DOWN_FLAG = 1;

	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IDSModel) {
				IDSModel model = (IDSModel) inputElement;
				IDSComponent component = model.getDSComponent();
				if (component != null) {
					// gets all children from DS component to get properties and
					// property elements in order of appearance
					IDocumentElementNode[] childNodes = component
							.getChildNodes();

					// count the number of property and properties elements
					int propertyLength = 0;
					int propertiesLength = 0;
					if (component.getPropertyElements() != null) {
						propertyLength = component.getPropertyElements().length;
					}

					if (component.getPropertiesElements() != null) {
						propertiesLength = component.getPropertiesElements().length;
					}

					// creates and returns an array with all property and
					// properties elements
					Object[] props = new Object[propertyLength
							+ propertiesLength];
					int index = 0;
					for (int i = 0; i < childNodes.length; i++) {
						IDocumentElementNode child = childNodes[i];
						if (child instanceof IDSProperties
								|| child instanceof IDSProperty) {
							props[index] = child;
							index++;
						}

					}
					return props;

				}

			}
			return new Object[0];
		}
	}

	public DSPropertiesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION,
				new String[] { Messages.DSPropertiesSection_addProperties,
						Messages.DSPropertiesSection_addProperty,
						Messages.DSPropertiesSection_edit,
						Messages.DSPropertiesSection_remove,
						Messages.DSPropertiesSection_up,
						Messages.DSPropertiesSection_down, });
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(Messages.DSPropertiesSection_title);
		section.setDescription(Messages.DSPropertiesSection_description);

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));

		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION | SWT.MULTI, 2,
				toolkit);
		fPropertiesTable = tablePart.getTableViewer();
		fPropertiesTable.setContentProvider(new ContentProvider());
		fPropertiesTable.setLabelProvider(new DSLabelProvider());

		makeActions();

		IDSModel model = getDSModel();
		if (model != null) {
			fPropertiesTable.setInput(model);
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
		fPropertiesTable.refresh();
		updateButtons();
		updateTitle();
	}

	private void updateTitle() {
		int itemCount = fPropertiesTable.getTable().getItemCount();
		getSection().setText(
				NLS.bind(Messages.DSPropertiesSection_title, new Integer(
						itemCount)));
	}

	protected void buttonSelected(int index) {
		switch (index) {
		case 0:
			handleAddProperties();
			break;
		case 1:
			handleAddProperty();
			break;
		case 2:
			handleEdit();
			break;
		case 3:
			handleRemove();
			break;
		case 4:
			handleUpDown(F_UP_FLAG);
			break;
		case 5:
			handleUpDown(F_DOWN_FLAG);
			break;
		}
	}

	private void handleUpDown(int newRelativeIndex) {
		ISelection sel = fPropertiesTable.getSelection();
		Object[] array = ((IStructuredSelection) sel).toArray();

		if (newRelativeIndex == F_UP_FLAG) {
			moveUp(newRelativeIndex, array);
		} else {
			moveDown(newRelativeIndex, array);

		}
		return;
	}

	private void moveDown(int newRelativeIndex, Object[] array) {
		for (int i = array.length - 1; i >= 0; i--) {
			Object object = array[i];
			if (object == null) {
				continue;
			} else if (object instanceof IDocumentElementNode) {
				// Move the task object up or down one position
				getDSModel().getDSComponent().moveChildNode(
						(IDocumentElementNode) object, newRelativeIndex, true);
			}
		}
	}

	private void moveUp(int newRelativeIndex, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			Object object = array[i];
			if (object == null) {
				continue;
			} else if (object instanceof IDocumentElementNode) {
				// Move the task object up or down one position
				getDSModel().getDSComponent().moveChildNode(
						(IDocumentElementNode) object, newRelativeIndex, true);
			}
		}
	}

	private void handleAddProperty() {
		DSEditPropertyDialog dialog = new DSEditPropertyDialog(Activator
				.getActiveWorkbenchShell(), createPropertyElement(), this, true);
		dialog.open();

	}

	private void handleEdit() {

		ISelection selection = fPropertiesTable.getSelection();
		if (selection != null) {

			int selectionIndex = fPropertiesTable.getTable()
					.getSelectionIndex();
			if (selectionIndex != -1) {
				Object selectionElement = fPropertiesTable
						.getElementAt(selectionIndex);

				if (selectionElement instanceof IDSProperties) {
					DSEditPropertiesDialog dialog = new DSEditPropertiesDialog(
							Activator.getActiveWorkbenchShell(),
							(IDSProperties) selectionElement, this);
					dialog.create();
					dialog.getShell().setSize(500, 200);
					dialog.open();

				} else if (selectionElement instanceof IDSProperty) {
					DSEditPropertyDialog dialog = new DSEditPropertyDialog(
							Activator.getActiveWorkbenchShell(),
							(IDSProperty) selectionElement, this, false);
					dialog.create();
					dialog.getShell().setSize(500, 300);
					dialog.open();
				}
			}

		}

	}

	private void makeActions() {
		fAddPropertiesAction = new Action(
				Messages.DSPropertiesSection_addProperties) {
			public void run() {
				handleAddProperties();
			}
		};
		fAddPropertiesAction.setEnabled(isEditable());

		fAddPropertyAction = new Action(
				Messages.DSPropertiesSection_addProperty) {
			public void run() {
				handleAddProperty();
			}
		};
		fAddPropertyAction.setEnabled(isEditable());

		fEditAction = new Action(Messages.DSPropertiesSection_edit) {
			public void run() {
				handleEdit();
			}
		};
		fEditAction.setEnabled(isEditable());

		fRemoveAction = new Action(Messages.DSPropertiesSection_remove) {
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());
	}

	private void updateButtons() {
		Table table = fPropertiesTable.getTable();

		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable());
		tablePart.setButtonEnabled(2, isEditable()
				&& table.getSelection().length > 0);
		tablePart.setButtonEnabled(3, isEditable());
		tablePart.setButtonEnabled(4, isEditable()
				&& table.getSelection().length > 0 && !table.isSelected(0));
		tablePart.setButtonEnabled(5, isEditable()
				&& table.getSelection().length > 0
				&& !table.isSelected(table.getItems().length - 1));
	}

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fPropertiesTable
				.getSelection();
		if (ssel.size() > 0) {
			Iterator iter = ssel.iterator();
			while (iter.hasNext()) {
				Object object = iter.next();
				if (object instanceof IDSProperties) {
					getDSModel().getDSComponent().removePropertiesElement(
							(IDSProperties) object);
				}
				if (object instanceof IDSProperty) {
					getDSModel().getDSComponent().removePropertyElement(
							(IDSProperty) object);
				}
			}
		}
	}

	private void handleAddProperties() {
		doOpenSelectionDialogProperties();
	}

	private void doOpenSelectionDialogProperties() {
		final IProject project = getProject();
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				Activator.getActiveWorkbenchShell(),
				new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setInput(project.getWorkspace());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (element instanceof IProject)
					return ((IProject) element).equals(project);
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(Messages.DSPropertiesDetails_dialogTitle);
		dialog.setMessage(Messages.DSPropertiesDetails_dialogMessage);
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null && selection.length > 0
						&& selection[0] instanceof IFile)
					return new Status(IStatus.OK, Activator.PLUGIN_ID,
							IStatus.OK, "", null); //$NON-NLS-1$

				return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						IStatus.ERROR, "", null); //$NON-NLS-1$
			}
		});
		if (dialog.open() == Window.OK) {
			IResource res = (IResource) dialog.getFirstResult();
			IPath path = res.getProjectRelativePath();
			if (res instanceof IContainer)
				path = path.addTrailingSeparator();
			String value = path.toString();
			addProperties(value);

		}
	}

	private void addProperties(String entry) {

		IDSDocumentFactory factory = getDSModel().getFactory();
		IDSComponent component = getDSModel().getDSComponent();

		IDSProperties properties = factory.createProperties();
		// set interface attribute
		properties.setEntry(entry);

		// add properties
		component.addPropertiesElement(properties);
	}

	private IDSProperty createPropertyElement() {

		IDSDocumentFactory factory = getDSModel().getFactory();

		IDSProperty property = factory.createProperty();

		return property;
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object[] objects = e.getChangedObjects();
			for (int i = 0; i < objects.length; i++) {
				Table table = fPropertiesTable.getTable();
				if (objects[i] instanceof IDSProperties) {
					int index = table.getSelectionIndex();
					fPropertiesTable.remove(objects[i]);
					if (canSelect()) {
						table.setSelection(index < table.getItemCount() ? index
								: table.getItemCount() - 1);
					}
				}
				if (objects[i] instanceof IDSProperty) {
					int index = table.getSelectionIndex();
					fPropertiesTable.remove(objects[i]);
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
				fPropertiesTable.refresh();
				fPropertiesTable.setSelection(new StructuredSelection(
						objects[objects.length - 1]));
			}
			updateButtons();
		} else {
			fPropertiesTable.refresh();
			updateButtons();
		}
		fPropertiesTable.getTable().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!fPropertiesTable.getTable().isDisposed())
					updateTitle();
			}
		});
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
