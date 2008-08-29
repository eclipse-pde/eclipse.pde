/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 244997
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
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
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class DSPropertiesSection extends TableSection {

	private TableViewer fPropertiessTable;
	private Action fRemoveAction;
	private Action fAddPropertiesAction;
	private Action fAddPropertyAction;
	private Action fEditAction;

	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			//			Arraylist list = new Arraylist();
			if (inputElement instanceof IDSModel) {
				IDSModel model = (IDSModel) inputElement;
				IDSComponent component = model.getDSComponent();
				if (component != null) {
					return component.getPropertiesElements();
				}

			}
			return new Object[0];
		}
	}

	public DSPropertiesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.TWISTIE,
				new String[] {
				Messages.DSPropertiesSection_addProperties,
						Messages.DSPropertiesSection_addProperty,
						Messages.DSPropertiesSection_edit,
				Messages.DSPropertiesSection_remove });
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(Messages.DSPropertiesSection_title);
		section.setDescription(Messages.DSPropertiesSection_description);

		section.setLayout(FormLayoutFactory
				.createClearGridLayout(false, 1));
		
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		section.setLayoutData(data);

		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.FULL_SELECTION | SWT.MULTI, 2,
				toolkit);
		fPropertiessTable = tablePart.getTableViewer();
		fPropertiessTable.setContentProvider(new ContentProvider());
		fPropertiessTable.setLabelProvider(new DSLabelProvider());

		makeActions();

		IDSModel model = getDSModel();
		if (model != null) {
			fPropertiessTable.setInput(model);
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
		fPropertiessTable.refresh();
		updateButtons();
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
		}
	}
	
	private void handleAddProperty() {
		// TODO Auto-generated method stub

	}

	private void handleEdit() {

		ISelection selection = fPropertiessTable.getSelection();
		if (selection != null) {

			int selectionIndex = fPropertiessTable.getTable()
					.getSelectionIndex();
			if (selectionIndex != -1) {
				DSEditPropertiesDialog dialog = new DSEditPropertiesDialog(
						Activator.getActiveWorkbenchShell(),
						(IDSProperties) fPropertiessTable
								.getElementAt(selectionIndex), this);
				dialog.open();
			}

		}

	}

	private void makeActions() {
		fAddPropertiesAction = new Action(Messages.DSPropertiesSection_addProperties) {
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
		Table table = fPropertiessTable.getTable();
		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable());
		tablePart.setButtonEnabled(2, isEditable()
				&& table.getSelection().length > 0);
		tablePart.setButtonEnabled(3, isEditable());
	}

	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection) fPropertiessTable
				.getSelection();
		if (ssel.size() > 0) {
			Iterator iter = ssel.iterator();
			while (iter.hasNext()) {
				Object object = iter.next();
				if (object instanceof IDSProperties) {
					getDSModel().getDSComponent().removePropertiesElement(
							(IDSProperties) object);
				}
			}
		}
	}

	private void handleAddProperties() {
		doOpenSelectionDialog();
	}

	private void doOpenSelectionDialog() {
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
				if (selection != null
						&& selection.length > 0
						&& (selection[0] instanceof IFile || selection[0] instanceof IContainer))
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

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			Object[] objects = e.getChangedObjects();
			for (int i = 0; i < objects.length; i++) {
				Table table = fPropertiessTable.getTable();
				if (objects[i] instanceof IDSProperties) {
					int index = table.getSelectionIndex();
					fPropertiessTable.remove(objects[i]);
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
				fPropertiessTable.refresh();
				fPropertiessTable.setSelection(new StructuredSelection(
						objects[objects.length - 1]));
			}
			updateButtons();
		} else {
			fPropertiessTable.refresh();
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
