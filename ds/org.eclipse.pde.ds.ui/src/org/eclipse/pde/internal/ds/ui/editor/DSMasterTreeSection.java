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
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.actions.DSAddItemAction;
import org.eclipse.pde.internal.ds.ui.editor.actions.DSRemoveItemAction;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSMasterTreeSection extends TreeSection implements IDSMaster {

	private static final int F_BUTTON_ADD_SERVICE = 0;
	private static final int F_BUTTON_ADD_PROPERTY = 1;
	private static final int F_BUTTON_ADD_REFERENCE = 2;
	private static final int F_BUTTON_ADD_PROPERTIES = 3;
	private static final int F_BUTTON_ADD_PROVIDE = 4;

	// 5 and 6 constants missing due to null, null parameters at Class
	// Constructor

	private static final int F_BUTTON_REMOVE = 7;
	private static final int F_BUTTON_UP = 8;
	private static final int F_BUTTON_DOWN = 9;

	private static final int F_UP_FLAG = -1;
	private static final int F_DOWN_FLAG = 1;

	private TreeViewer fTreeViewer;

	private IDSModel fModel;

	private CollapseAction fCollapseAction;

	private DSAddItemAction fAddStepAction;
	private DSRemoveItemAction fRemoveItemAction;

	private ControlDecoration fInfoDecoration;
	
	public DSMasterTreeSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {
				Messages.DSMasterTreeSection_addService,
				Messages.DSMasterTreeSection_addProperty,
				Messages.DSMasterTreeSection_addReference,
				Messages.DSMasterTreeSection_addProperties,
				Messages.DSMasterTreeSection_addProvide, null, null,
				Messages.DSMasterTreeSection_remove,
				Messages.DSMasterTreeSection_up,
				Messages.DSMasterTreeSection_down });

		// Create actions
		fAddStepAction = new DSAddItemAction();
		fRemoveItemAction = new DSRemoveItemAction();
		fCollapseAction = null;

	}

	protected void createClient(Section section, FormToolkit toolkit) {
		// Get the model
		fModel = (IDSModel) getPage().getModel();
		// TODO Externalize Strings (use PDEUIMessages?)
		section.setText(Messages.DSMasterTreeSection_client_text);
		// Set section description
		section
				.setDescription(Messages.DSMasterTreeSection_client_description);
		// Create section client
		Composite container = createClientContainer(section, 2, toolkit);

		createTree(container, toolkit);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		// Create section toolbar
		createSectionToolbar(section, toolkit);
	}

	/**
	 * @param section
	 * @param toolkit
	 */
	private void createSectionToolbar(Section section, FormToolkit toolkit) {

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(),
				SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) && (handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});
		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fTreeViewer,
				PDEUIMessages.ExtensionsPage_collapseAll, 1, fModel.getDSComponent());
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	public void fireSelection() {
		fTreeViewer.setSelection(fTreeViewer.getSelection());
	}

	/**
	 * 
	 */
	public void updateButtons() {
		if (!fModel.isEditable()) {
			return;
		}
		IDSObject dsObject = getCurrentSelection();

		boolean canRemove = false;
		boolean canMoveUp = false;
		boolean canMoveDown = false;

		boolean canAddService = false;
		boolean canAddProperty = true;
		boolean canAddProperties = true;
		boolean canAddReference = true;
		boolean canAddProvide = false;

		if (dsObject != null) {

			if (dsObject.getType() == IDSConstants.TYPE_ROOT) {
				// no op

			} else if (dsObject.getType() == IDSConstants.TYPE_IMPLEMENTATION) {
				canMoveUp = true;
				canMoveDown = true;
			} else if (dsObject.getType() == IDSConstants.TYPE_PROVIDE) {
				canRemove = true;

			} else if (dsObject.getType() == IDSConstants.TYPE_SERVICE) {
				canRemove = true;
				canMoveUp = true;
				canMoveDown = true;
				// if TYPE_Service has no child, can add one Provide component

				if (dsObject.getChildCount() == 0) {
					canAddProvide = true;
				}
			} else if ((dsObject.getType() == IDSConstants.TYPE_PROPERTIES)
					|| (dsObject.getType() == IDSConstants.TYPE_PROPERTY)
					|| (dsObject.getType() == IDSConstants.TYPE_REFERENCE)) {
				canRemove = true;
				canMoveUp = true;
				canMoveDown = true;
			}

			// DS Validity: if Root component has no service child, can add one
			// Service component
			int childNodeCount = dsObject.getModel().getDSComponent()
					.getChildNodeCount(IDSService.class);
			if (childNodeCount == 0) {
				canAddService = true;
			}
		}
		getTreePart().setButtonEnabled(F_BUTTON_ADD_SERVICE, canAddService);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_PROPERTY, canAddProperty);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_REFERENCE, canAddReference);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_PROPERTIES,
				canAddProperties);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_PROVIDE, canAddProvide);

		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, canRemove);
		getTreePart().setButtonEnabled(F_BUTTON_UP, canMoveUp);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, canMoveDown);

	}

	/**
	 * @param container
	 * @param toolkit
	 */
	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		fTreeViewer = treePart.getTreeViewer();
		fTreeViewer.setContentProvider(new DSContentProvider());
		fTreeViewer.setLabelProvider(new DSLabelProvider());
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		createTreeListeners();
	}

	/**
	 * 
	 */
	private void createTreeListeners() {
		// Create listener for the outline view 'link with editor' toggle
		// button
		fTreeViewer
				.addPostSelectionChangedListener(getPage().getPDEEditor().new PDEFormEditorChangeListener());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		super.modelChanged(event);

		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(event);
		} else if (event.getChangeType() == IModelChangedEvent.INSERT) {
			// FIXME unreached (handleAddAction() should raise this event)
			handleModelInsertType(event);
		} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
			handleModelRemoveType(event);
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			handleModelChangeType(event);
		}
	}

	private void handleModelInsertType(IModelChangedEvent event) {
		// Insert event
		Object[] objects = event.getChangedObjects();
		IDSObject object = (IDSObject) objects[0];
		if (object != null) {
			// Refresh the parent element in the tree viewer
			fTreeViewer.refresh(object.getParent());
			this.refresh();
			// Select the new item in the tree
			fTreeViewer.setSelection(new StructuredSelection(object), true);
		}
	}

	private void handleModelChangeType(IModelChangedEvent event) {
		// Change event
		Object[] objects = event.getChangedObjects();
		// Ensure right type
		if ((objects[0] instanceof IDSObject) == false) {
			return;
		}
		IDSObject object = (IDSObject) objects[0];
		if (object == null) {
			// Ignore
		} else if ((object.getType() == IDSConstants.TYPE_IMPLEMENTATION)
				|| (object.getType() == IDSConstants.TYPE_PROPERTIES)
				|| (object.getType() == IDSConstants.TYPE_PROPERTY)
				|| (object.getType() == IDSConstants.TYPE_PROVIDE)
				|| (object.getType() == IDSConstants.TYPE_REFERENCE)
				|| (object.getType() == IDSConstants.TYPE_ROOT)
				|| (object.getType() == IDSConstants.TYPE_SERVICE)) {
			// Refresh the element in the tree viewer
			fTreeViewer.update(object, null);

		}

	}

	private void handleModelRemoveType(IModelChangedEvent event) {
		// Remove event
		Object[] objects = event.getChangedObjects();
		IDSObject object = (IDSObject) objects[0];
		if (object != null) {
			fTreeViewer.remove(object);
			fTreeViewer.setSelection(
					new StructuredSelection(fModel.getDSComponent()), true);
		}
	}

	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Section will be updated on refresh
		markStale();
	}

	public ISelection getSelection() {
		return fTreeViewer.getSelection();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		// Get the form page
		DSPage page = (DSPage) getPage();
		// Replace the current dirty model with the model reloaded from
		// file
		fModel = (IDSModel) page.getModel();
		// Re-initialize the tree viewer. Makes a details page selection
		initializeTreeViewer();

		super.refresh();
	}

	private void initializeTreeViewer() {

		if (fModel == null) {
			return;
		}
		fTreeViewer.setInput(fModel);

		// Enable buttons when Root component is selected
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, false);
		getTreePart().setButtonEnabled(F_BUTTON_UP, false);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, false);

		getTreePart().setButtonEnabled(F_BUTTON_ADD_PROPERTIES, true);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_PROPERTY, true);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_PROVIDE, false);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_REFERENCE, true);
		boolean hasService = (fModel.getDSComponent().getChildNodeCount(
				IDSService.class) == 1);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_SERVICE, !hasService);

		IDSComponent dsComponent = fModel.getDSComponent();
		// Select the ds node in the tree
		fTreeViewer.setSelection(new StructuredSelection(dsComponent), true);
		fTreeViewer.expandToLevel(2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		// ISelection selection = fTreeViewer.getSelection();

		switch (index) {
		case F_BUTTON_ADD_PROPERTIES:
			handleAddAction(IDSConstants.TYPE_PROPERTIES);
			// FIXME add actions should raise IModelChangedEvent.INSERT event,
			// but it isn't raising (so I put this temporary 2-line code):
			this.refresh();
			fTreeViewer.setSelection(new StructuredSelection(fAddStepAction
					.getFNewObject()));
			break;
		case F_BUTTON_ADD_PROPERTY:
			handleAddAction(IDSConstants.TYPE_PROPERTY);
			this.refresh();
			fTreeViewer.setSelection(new StructuredSelection(fAddStepAction
					.getFNewObject()));
			break;
		case F_BUTTON_ADD_PROVIDE:
			handleAddAction(IDSConstants.TYPE_PROVIDE);
			this.refresh();
			fTreeViewer.setSelection(new StructuredSelection(fAddStepAction
					.getFNewObject()));
			break;
		case F_BUTTON_ADD_REFERENCE:
			handleAddAction(IDSConstants.TYPE_REFERENCE);
			this.refresh();
			fTreeViewer.setSelection(new StructuredSelection(fAddStepAction
					.getFNewObject()));
			break;
		case F_BUTTON_ADD_SERVICE:
			handleAddAction(IDSConstants.TYPE_SERVICE);
			this.refresh();
			fTreeViewer.setSelection(new StructuredSelection(fAddStepAction
					.getFNewObject()));
			break;
		case F_BUTTON_REMOVE:
			handleDeleteAction();
			break;
		case F_BUTTON_UP:
			handleMoveAction(F_UP_FLAG);
			break;
		case F_BUTTON_DOWN:
			handleMoveAction(F_DOWN_FLAG);
			break;
		}

	}

	private void handleAddAction(int type) {
		fAddStepAction.setType(type);

		IDSObject object = getCurrentSelection();
		if (object != null) {
			fAddStepAction.setSelection(object);

		} else {
			if (type != IDSConstants.TYPE_PROVIDE) {
				fAddStepAction.setSelection(fModel.getDSComponent());
			} else {
				return;
			}
		}
		// Execute the action
		fAddStepAction.run();
	}

	private void handleMoveAction(int upFlag) {
		IDSObject object = getCurrentSelection();
		if (object != null) {
			if (object instanceof IDSService || object instanceof IDSProperty
					|| object instanceof IDSImplementation
					|| object instanceof IDSProperties
					|| object instanceof IDSReference) {
				((IDSComponent) object.getParent()).moveItem(object, upFlag);
				this.refresh();
				fTreeViewer.setSelection(new StructuredSelection(object));
			}
		}
	}

	private void handleDeleteAction() {
		IDSObject object = getCurrentSelection();
		if (object != null) {
			if (object instanceof IDSComponent) {
				// Preserve ds validity
				// Semantic Rule: Cannot have a cheat sheet with no root
				// ds component node
				// Produce audible beep
				Display.getCurrent().beep();
			} else if (object instanceof IDSImplementation) {
				// Preserve ds validity
				// Semantic Rule: Cannot have a cheat sheet with no
				// implementation
				// Produce audible beep
				Display.getCurrent().beep();
			} else {
				// Preserve cheat sheet validity
				fRemoveItemAction.setItem(object);
				fRemoveItemAction.run();

			}
		}
	}

	/**
	 * @return
	 */
	private IDSObject getCurrentSelection() {
		ISelection selection = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) selection).getFirstElement();
		return (IDSObject) object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		updateButtons();
	}


	public boolean setFormInput(Object object) {
		// This method allows the outline view to select items in the tree
		// Invoked by
		// org.eclipse.ui.forms.editor.IFormPage.selectReveal(Object object)
		if (object instanceof IDSObject) {
			// Select the item in the tree
			fTreeViewer.setSelection(new StructuredSelection(object), true);
			// Verify that something was actually selected
			ISelection selection = fTreeViewer.getSelection();
			if ((selection != null) && (selection.isEmpty() == false)) {
				return true;
			}
		}
		return false;
	}

}
