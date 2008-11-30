/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.*;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.actions.CompCSAddGroupAction;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.actions.CompCSAddTaskAction;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.actions.CompCSRemoveTaskObjectAction;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSMasterTreeSection
 *
 */
public class CompCSMasterTreeSection extends TreeSection implements ICSMaster {

	private static final int F_BUTTON_ADD_TASK = 0;

	private static final int F_BUTTON_ADD_GROUP = 1;

	private static final int F_BUTTON_REMOVE = 2;

	private static final int F_BUTTON_UP = 3;

	private static final int F_BUTTON_DOWN = 4;

	private static final int F_BUTTON_PREVIEW = 5;

	private static final int F_UP_FLAG = -1;

	private static final int F_DOWN_FLAG = 1;

	private TreeViewer fTreeViewer;

	private ICompCSModel fModel;

	private CollapseAction fCollapseAction;

	private CompCSRemoveTaskObjectAction fRemoveTaskObjectAction;

	private CompCSAddGroupAction fAddGroupAction;

	private CompCSAddTaskAction fAddTaskAction;

	private CompCSGroupValidator fGroupValidator;

	/**
	 * @param formPage
	 * @param parent
	 * @param style
	 * @param buttonLabels
	 */
	public CompCSMasterTreeSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[] {Messages.CompCSMasterTreeSection_addTask, Messages.CompCSMasterTreeSection_addGroup, Messages.CompCSMasterTreeSection_Remove, Messages.CompCSMasterTreeSection_Up, Messages.CompCSMasterTreeSection_Down, Messages.CompCSMasterTreeSection_Preview});

		// Create actions
		fAddGroupAction = new CompCSAddGroupAction();
		fAddTaskAction = new CompCSAddTaskAction();
		fRemoveTaskObjectAction = new CompCSRemoveTaskObjectAction();
		fCollapseAction = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		// Get the model
		fModel = (ICompCSModel) getPage().getModel();

		Composite container = createClientContainer(section, 2, toolkit);
		createTree(container, toolkit);
		toolkit.paintBordersFor(container);
		section.setText(Messages.CompCSMasterTreeSection_Content);
		section.setDescription(Messages.CompCSMasterTreeSection_sectionDesc);
		section.setClient(container);
		initializeTreeViewer();
		createSectionToolbar(section, toolkit);
	}

	/**
	 * @param container
	 * @param toolkit
	 */
	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		fTreeViewer = treePart.getTreeViewer();
		fTreeViewer.setContentProvider(new CompCSContentProvider());
		fTreeViewer.setLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
		PDEUserAssistanceUIPlugin.getDefault().getLabelProvider().connect(this);
		createTreeListeners();
		// TODO: MP: LOW: CompCS: Implement drag and drop move feature
	}

	/**
	 * 
	 */
	private void createTreeListeners() {
		// Create listener for the outline view 'link with editor' toggle 
		// button
		fTreeViewer.addPostSelectionChangedListener(getPage().getPDEEditor().new PDEFormEditorChangeListener());
	}

	/**
	 * @return
	 */
	public ISelection getSelection() {
		return fTreeViewer.getSelection();
	}

	/**
	 * 
	 */
	private void initializeTreeViewer() {

		if (fModel == null) {
			return;
		}
		fTreeViewer.setInput(fModel);
		ICompCS cheatsheet = fModel.getCompCS();

		// Create the group validator and register all existing groups to be
		// validated within the workspace model
		fGroupValidator = new CompCSGroupValidator(cheatsheet, getManagedForm().getForm().getForm(), Messages.CompCSMasterTreeSection_content);

		// If the cheat sheet already has a task object, then the object has
		// to be deleted first before a new task or group can be added to
		// the root cheatsheet node
		boolean addFlag = false;
		if (cheatsheet.getFieldTaskObject() == null) {
			addFlag = fModel.isEditable();
		}
		getTreePart().setButtonEnabled(F_BUTTON_ADD_TASK, addFlag);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_GROUP, addFlag);

		// Set to false because initial node selected is the root cheatsheet node
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, false);
		// Set to false because initial node selected is the root cheatsheet node
		getTreePart().setButtonEnabled(F_BUTTON_UP, false);
		// Set to false because initial node selected is the root cheatsheet node
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, false);

		// Validate initial file content
		// TODO: MP: LOW: CompCS: The error message does not show up in the form on load for some reason
		// TODO: MP: LOW: CompCS: Implement error image overlay on icon ILightWeightLabelDecorator
		// TODO: MP: LOW: CompCS: The error message dissapears on up / down movement
		updatePreviewButton(fGroupValidator.validate());

		// Select the cheatsheet node in the tree
		fTreeViewer.setSelection(new StructuredSelection(cheatsheet), true);
		fTreeViewer.expandToLevel(2);
	}

	/**
	 * @param section
	 * @param toolkit
	 */
	private void createSectionToolbar(Section section, FormToolkit toolkit) {

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
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
		fCollapseAction = new CollapseAction(fTreeViewer, Messages.CompCSMasterTreeSection_collapseAll, 1, fModel.getCompCS());
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		switch (index) {
			case F_BUTTON_ADD_TASK :
				handleAddTaskAction();
				break;
			case F_BUTTON_ADD_GROUP :
				handleAddGroupAction();
				break;
			case F_BUTTON_REMOVE :
				handleDeleteAction();
				break;
			case F_BUTTON_UP :
				handleMoveTaskObjectAction(F_UP_FLAG);
				break;
			case F_BUTTON_DOWN :
				handleMoveTaskObjectAction(F_DOWN_FLAG);
				break;
			case F_BUTTON_PREVIEW :
				handlePreviewAction();
				break;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		updateButtons();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSMaster#updateButtons()
	 */
	public void updateButtons() {
		if (!fModel.isEditable()) {
			return;
		}
		Object object = ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
		ICompCSObject csObject = (ICompCSObject) object;
		boolean canAddTask = false;
		boolean canAddGroup = false;
		boolean canRemove = false;
		boolean canMoveUp = false;
		boolean canMoveDown = false;

		if (csObject != null) {
			ICompCSObject parent = csObject.getParent();
			if ((csObject.getType() == ICompCSConstants.TYPE_TASK) || (csObject.getType() == ICompCSConstants.TYPE_TASKGROUP)) {

				if ((parent.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) && (csObject.getType() == ICompCSConstants.TYPE_TASKGROUP)) {
					canAddTask = true;
					canAddGroup = true;
				} else if (parent.getType() == ICompCSConstants.TYPE_TASKGROUP) {
					ICompCSTaskGroup taskGroup = (ICompCSTaskGroup) parent;
					ICompCSTaskObject taskObject = (ICompCSTaskObject) csObject;
					if (taskGroup.isFirstFieldTaskObject(taskObject) == false) {
						canMoveUp = true;
					}
					if (taskGroup.isLastFieldTaskObject(taskObject) == false) {
						canMoveDown = true;
					}
					canRemove = canRemoveTaskObject(taskGroup);
					canAddTask = true;
					canAddGroup = true;
				}
			}
		}

		getTreePart().setButtonEnabled(F_BUTTON_ADD_TASK, canAddTask);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_GROUP, canAddGroup);
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, canRemove);
		getTreePart().setButtonEnabled(F_BUTTON_UP, canMoveUp);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, canMoveDown);
	}

	/**
	 * 
	 */
	private void handleAddTaskAction() {

		ISelection sel = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object == null) {
			return;
		}
		if (object instanceof ICompCSTaskGroup) {
			fAddTaskAction.setParentObject((ICompCSObject) object);
			fAddTaskAction.run();
		} else if (object instanceof ICompCSTask) {
			fAddTaskAction.setParentObject(((ICompCSObject) object).getParent());
			fAddTaskAction.run();
		}
	}

	/**
	 * @param flag
	 */
	private void updatePreviewButton(boolean flag) {
		getTreePart().setButtonEnabled(F_BUTTON_PREVIEW, flag);
	}

	/**
	 * 
	 */
	private void handleAddGroupAction() {

		ISelection sel = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object == null) {
			return;
		}
		if (object instanceof ICompCSTaskGroup) {
			fAddGroupAction.setParentObject((ICompCSObject) object);
			fAddGroupAction.run();
		} else if (object instanceof ICompCSTask) {
			fAddGroupAction.setParentObject(((ICompCSObject) object).getParent());
			fAddGroupAction.run();
		}
	}

	/**
	 * 
	 */
	private void handleMoveTaskObjectAction(int positionFlag) {

		ISelection sel = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object == null) {
			return;
		} else if (object instanceof ICompCSTaskObject) {
			ICompCSTaskObject taskObject = (ICompCSTaskObject) object;
			ICompCSTaskGroup parent = null;
			// Determine the parents type
			if (taskObject.getParent().getType() == ICompCSConstants.TYPE_TASKGROUP) {
				parent = (ICompCSTaskGroup) taskObject.getParent();
			} else {
				return;
			}
			// Move the task object up or down one position
			parent.moveFieldTaskObject(taskObject, positionFlag);
		}
	}

	/**
	 * 
	 */
	private void handlePreviewAction() {
		// Get the editor input
		// Could be IFileEditorInput (File in workpspace - e.g. Package Explorer View)
		// Could be IStorageEditorInput (File not in workpsace - e.g. CVS Repositories View)
		IEditorInput input = getPage().getEditorInput();
		URL url = null;
		try {
			if (input instanceof IFileEditorInput) {
				IFileEditorInput fileInput = (IFileEditorInput) input;
				url = fileInput.getFile().getLocationURI().toURL();
			} else if (input instanceof IStorageEditorInput) {
				// Note:  This URL does not exist on the local file system
				// As a result any tasks this composite cheat sheet has that 
				// specify a pathes to simple cheat sheets will not resolve
				// Cheat sheet view will log an error loading simple cheat
				// sheets
				IStorageEditorInput storageInput = (IStorageEditorInput) input;
				url = storageInput.getStorage().getFullPath().toFile().toURI().toURL();
			} else {
				// No base URL.  Pathes will definitely not resolve here 
				url = null;
			}

			// Write the current model into a String as raw XML
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			fModel.getCompCS().write("", writer); //$NON-NLS-1$
			writer.flush();
			swriter.close();
			// Launch in the cheat sheet view
			OpenCheatSheetAction openAction = new OpenCheatSheetAction(input.getName(), input.getName(), swriter.toString(), url);
			openAction.run();
		} catch (IOException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		} catch (CoreException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		// No need to call super, world changed event handled here
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(event);
		} else if (event.getChangeType() == IModelChangedEvent.INSERT) {
			handleModelInsertType(event);
		} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
			handleModelRemoveType(event);
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			handleModelChangeType(event);
		}

		// Validate registered groups regardless of change type
		// Validation is not required for task and composite cheat sheet 
		// change types (performance savings available); but, is required for
		// everything else
		updatePreviewButton(fGroupValidator.validate());
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {

		Object[] objects = event.getChangedObjects();
		ICompCSObject object = (ICompCSObject) objects[0];
		if (object == null) {
			// Ignore
			return;
		} else if (object.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) {
			// Get the form page
			CompCSPage page = (CompCSPage) getPage();
			// Remember the currently selected page
			IDetailsPage previousDetailsPage = page.getBlock().getDetailsPart().getCurrentPage();
			// Replace the current dirty model with the model reloaded from
			// file
			fModel = ((ICompCS) object).getModel();
			// Reset the treeviewer using the new model as input
			// TODO: MP: CompCS:  This is redundant and should be deleted
			fTreeViewer.setInput(fModel);
			// Re-initialize the tree viewer.  Makes a details page selection
			initializeTreeViewer();
			// Get the current details page selection
			IDetailsPage currentDetailsPage = page.getBlock().getDetailsPart().getCurrentPage();
			// If the selected page before the revert is the same as the 
			// selected page after the revert, then its fields will need to
			// be updated
			// TODO: MP: REVERT: LOW: Revisit to see if updating details page is necessary - especially after making static
			if (currentDetailsPage.equals(previousDetailsPage) && currentDetailsPage instanceof ICSDetails) {
				((ICSDetails) currentDetailsPage).updateFields();
			}
		}

	}

	/**
	 * @param event
	 */
	private void handleModelInsertType(IModelChangedEvent event) {
		// Insert event
		Object[] objects = event.getChangedObjects();
		ICompCSObject object = (ICompCSObject) objects[0];
		if (object == null) {
			// Ignore
		} else if (object.getType() == ICompCSConstants.TYPE_TASK) {
			handleTaskObjectInsert(object);
		} else if (object.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			handleTaskObjectInsert(object);
			// Register the group for validation
			fGroupValidator.addGroup((ICompCSTaskGroup) object);
		}
	}

	/**
	 * @param object
	 */
	private void handleTaskObjectInsert(ICompCSObject object) {
		// Refresh the parent element in the tree viewer
		// TODO: MP: CompCS: LOW: Can we get away with an update instead of a refresh here?
		fTreeViewer.refresh(object.getParent());
		// Select the new task / group in the tree
		fTreeViewer.setSelection(new StructuredSelection(object), true);
	}

	/**
	 * @param event
	 */
	private void handleModelRemoveType(IModelChangedEvent event) {
		// Remove event
		Object[] objects = event.getChangedObjects();
		ICompCSObject object = (ICompCSObject) objects[0];
		if (object == null) {
			// Ignore
		} else if (object.getType() == ICompCSConstants.TYPE_TASK) {
			handleTaskObjectRemove(object);
		} else if (object.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			handleTaskObjectRemove(object);
			// Unregister the group from validation
			fGroupValidator.removeGroup((ICompCSTaskGroup) object);
		}
	}

	/**
	 * @param object
	 */
	private void handleTaskObjectRemove(ICompCSObject object) {
		// Remove the item
		fTreeViewer.remove(object);
		// Select the appropriate object
		ICompCSObject csObject = fRemoveTaskObjectAction.getObjectToSelect();
		if (csObject == null) {
			csObject = object.getParent();
		}
		fTreeViewer.setSelection(new StructuredSelection(csObject), true);
	}

	/**
	 * @param event
	 */
	private void handleModelChangeType(IModelChangedEvent event) {
		// Change event
		Object[] objects = event.getChangedObjects();
		ICompCSObject object = (ICompCSObject) objects[0];
		if (object == null) {
			// Ignore
		} else if (object.getType() == ICompCSConstants.TYPE_TASK) {
			// Update the element in the tree viewer
			fTreeViewer.update(object, null);
		} else if (object.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			// Refresh the element in the tree viewer
			fTreeViewer.update(object, null);
		} else if (object.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) {
			// Refresh the element in the tree viewer
			fTreeViewer.update(object, null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster#fireSelection()
	 */
	public void fireSelection() {
		fTreeViewer.setSelection(fTreeViewer.getSelection());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		// Get the current selection
		ISelection selection = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) selection).getFirstElement();
		// Do blind cast - has to be a composite CS object
		// Could be null
		ICompCSObject csObject = (ICompCSObject) object;
		// Create the "New" sub-menu
		MenuManager submenu = new MenuManager(Messages.CompCSMasterTreeSection_new);
		// Add the "New" sub-menu to the main context menu
		manager.add(submenu);
		if ((csObject == null) || (csObject.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET)) {
			// NO-OP
		} else if (csObject.getType() == ICompCSConstants.TYPE_TASK) {
			// Remove task action
			fillContextMenuRemoveAction(manager, (ICompCSTaskObject) csObject);
		} else if (csObject.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			ICompCSTaskGroup group = (ICompCSTaskGroup) csObject;
			// Add to the "New" sub-menu
			// Add task action
			fAddTaskAction.setParentObject(group);
			fAddTaskAction.setEnabled(fModel.isEditable());
			submenu.add(fAddTaskAction);
			// Add to the "New" sub-menu
			// Add group action
			fAddGroupAction.setParentObject(group);
			fAddGroupAction.setEnabled(fModel.isEditable());
			submenu.add(fAddGroupAction);
			// Remove task group action
			fillContextMenuRemoveAction(manager, (ICompCSTaskObject) csObject);
		}
		// Add normal edit operations
		// TODO: MP: LOW: SimpleCS:  Enable context menu edit operations
		//getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		//manager.add(new Separator());
	}

	/**
	 * @param manager
	 * @param csObject
	 */
	private void fillContextMenuRemoveAction(IMenuManager manager, ICompCSTaskObject taskObject) {
		// Add to the main context menu
		// Add a separator to the main context menu
		manager.add(new Separator());
		// Delete task object action
		fRemoveTaskObjectAction.setTaskObject(taskObject);
		manager.add(fRemoveTaskObjectAction);
		ICompCSObject parent = taskObject.getParent();
		if (canRemoveTaskObject(parent) == false) {
			fRemoveTaskObjectAction.setEnabled(false);
		} else {
			fRemoveTaskObjectAction.setEnabled(fModel.isEditable());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDeleteAction();
			return true;
		}
		return false;
	}

	/**
	 * @param object
	 */
	private void handleDeleteAction() {

		ISelection sel = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object != null) {
			if (object instanceof ICompCSTaskObject) {
				ICompCSTaskObject taskObject = (ICompCSTaskObject) object;
				ICompCSObject parent = taskObject.getParent();
				if (canRemoveTaskObject(parent) == false) {
					// Preserve cheat sheet validity
					// Semantic Rule:  Cannot have a task group with no tasks					
					Display.getCurrent().beep();
				} else {
					fRemoveTaskObjectAction.setTaskObject(taskObject);
					fRemoveTaskObjectAction.run();
				}
			} else if (object instanceof ICompCS) {
				// Preserve cheat sheet validity
				// Semantic Rule:  Cannot have a cheat sheet with no root
				// cheatsheet node
				// Produce audible beep
				Display.getCurrent().beep();
			}
		}
	}

	/**
	 * @param parent
	 * @return
	 */
	private boolean canRemoveTaskObject(ICompCSObject parent) {
		if (parent.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) {
			// Preserve cheat sheet validity
			// Semantic Rule: Cannot delete the task object directly under
			// the root cheat sheet node
			// Wizard by default creates a task group as the only allowed
			// child of the root cheat sheet node. No good reason to
			// substitute with a task instead. Specification supports its,
			// but cheat sheet editor will not support it
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFormInput(java.lang.Object)
	 */
	public boolean setFormInput(Object object) {
		// This method allows the outline view to select items in the tree
		// Invoked by
		// org.eclipse.ui.forms.editor.IFormPage.selectReveal(Object object)
		if (object instanceof ICompCSObject) {
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
