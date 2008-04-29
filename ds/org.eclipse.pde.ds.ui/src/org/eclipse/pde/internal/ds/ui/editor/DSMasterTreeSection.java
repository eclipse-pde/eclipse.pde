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
import org.eclipse.pde.internal.ds.core.IDSRoot;
import org.eclipse.pde.internal.ds.core.IDSService;
import org.eclipse.pde.internal.ds.ui.editor.actions.DSAddStepAction;
import org.eclipse.pde.internal.ds.ui.editor.actions.DSAddSubStepAction;
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

	private static final int F_BUTTON_ADD_STEP = 0;
	private static final int F_BUTTON_ADD_SUBSTEP = 1;
	private static final int F_BUTTON_REMOVE = 4;
	private static final int F_BUTTON_UP = 5;
	private static final int F_BUTTON_DOWN = 6;
	private static final int F_UP_FLAG = -1;
	private static final int F_DOWN_FLAG = 1;

	private TreeViewer fTreeViewer;

	private IDSModel fModel;

	private CollapseAction fCollapseAction;

	private ControlDecoration fSubStepInfoDecoration;

	private DSAddStepAction fAddStepAction;
	private DSRemoveItemAction fRemoveItemAction;
	private DSAddSubStepAction fAddSubStepAction;

	public DSMasterTreeSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] { "Add",
				"Add Sub-Step", null, null, "Remove", "Up", "Down" });

		// Create actions
		fAddStepAction = new DSAddStepAction();
		fRemoveItemAction = new DSRemoveItemAction();
		fAddSubStepAction = new DSAddSubStepAction();
		fCollapseAction = null;

	}

	protected void createClient(Section section, FormToolkit toolkit) {
		// Get the model
		fModel = (IDSModel) getPage().getModel();
		// TODO Externalize Strings (use PDEUIMessages?)
		section.setText("Content");
		// Set section description
		section
				.setDescription("Edit the structure of this DS XML file in the following section.");
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
				PDEUIMessages.ExtensionsPage_collapseAll, 1, fModel.getDSRoot());
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

		boolean canAdd = false;
		boolean canAddSub = false;
		boolean canRemove = false;
		boolean canMoveUp = false;
		boolean canMoveDown = false;

		if (dsObject != null) {

			if (dsObject.getType() == IDSConstants.TYPE_ROOT) {
				// Add item to end of cheat sheet child items
				canAdd = true;
			} else if (dsObject.getType() == IDSConstants.TYPE_IMPLEMENTATION) {
				canMoveUp = true;
				canMoveDown = true;
			} else if (dsObject.getType() == IDSConstants.TYPE_PROVIDE) {
				canRemove = true;
				canAddSub = true;

			} else if (dsObject.getType() == IDSConstants.TYPE_SERVICE) {
				canAdd = true;
				canAddSub = true;
				canRemove = true;
				canMoveUp = true;
				canMoveDown = true;
			} else if ((dsObject.getType() == IDSConstants.TYPE_PROPERTIES)
					|| (dsObject.getType() == IDSConstants.TYPE_PROPERTY)
					|| (dsObject.getType() == IDSConstants.TYPE_REFERENCE)) {
				canRemove = true;
				canMoveUp = true;
				canMoveDown = true;
			}
			
		}
		getTreePart().setButtonEnabled(F_BUTTON_ADD_STEP, canAdd);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_SUBSTEP, canAddSub);
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
		fTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
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
			// handleModelInsertType(event);
		} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
			handleModelRemoveType(event);
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			handleModelChangeType(event);
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
		if (object == null) {
			// Ignore
		} else if (object.getType() == IDSConstants.TYPE_ROOT) {
			// Remove the item
			fTreeViewer.remove(object);
			// Determine if we should make a selection
			if (canSelect() == false) {
				return;
			}
			// Select the appropriate object
			IDSObject dsObject = fRemoveItemAction.getObjectToSelect();
			if (dsObject == null) {
				dsObject = object.getParent();
			}
			fTreeViewer.setSelection(new StructuredSelection(dsObject), true);
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

		getTreePart().setButtonEnabled(F_BUTTON_ADD_STEP, fModel.isEditable());
		getTreePart().setButtonEnabled(F_BUTTON_ADD_SUBSTEP, false);
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, false);
		getTreePart().setButtonEnabled(F_BUTTON_UP, false);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, false);

		IDSRoot dsRoot = fModel.getDSRoot();
		// Select the ds node in the tree
		fTreeViewer.setSelection(new StructuredSelection(dsRoot), true);
		fTreeViewer.expandToLevel(2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		ISelection selection = fTreeViewer.getSelection();
		switch (index) {
		case F_BUTTON_ADD_STEP:
			handleAddStepAction();
			break;
		case F_BUTTON_ADD_SUBSTEP:
			handleAddSubStepAction();
			break;
		case F_BUTTON_REMOVE:
			handleDeleteAction();
			break;
		case F_BUTTON_UP:
			handleMoveStepAction(F_UP_FLAG);
			break;
		case F_BUTTON_DOWN:
			handleMoveStepAction(F_DOWN_FLAG);
			break;
		}
		
		// TODO verify if it is correct to refresh here.
		this.refresh();
		if (selection != null)
			fTreeViewer.setSelection(selection);
	}

	private void handleMoveStepAction(int upFlag) {
		IDSObject object = getCurrentSelection();
		if (object != null) {
			if (object instanceof IDSService || object instanceof IDSProperty
					|| object instanceof IDSImplementation
					|| object instanceof IDSProperties
					|| object instanceof IDSReference) {
				((IDSRoot) object.getParent()).moveItem(object, upFlag);
			}
		}
	}

	private void handleDeleteAction() {
		IDSObject object = getCurrentSelection();
		if (object != null) {
			if (object instanceof IDSRoot) {
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

	private void handleAddSubStepAction() {
		// TODO Auto-generated method stub
		System.out.println("handleAddSubStepAction");

	}

	private void handleAddStepAction() {
		// TODO Auto-generated method stub
		System.out.println("handleAddStepAction");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		updateButtons();
	}

}
