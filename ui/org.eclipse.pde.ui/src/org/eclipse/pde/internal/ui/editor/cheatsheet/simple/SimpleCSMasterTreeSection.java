/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSAddStepAction;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSAddSubStepAction;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSRemoveRunObjectAction;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSRemoveStepAction;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSRemoveSubStepAction;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSElementSection
 *
 */
public class SimpleCSMasterTreeSection extends TreeSection implements
		ICSMaster {

	private static final int F_BUTTON_ADD_STEP = 0;

	private static final int F_BUTTON_ADD_SUBSTEP = 1;
	
	private static final int F_BUTTON_REMOVE = 2;
	
	private static final int F_BUTTON_UP = 3;
	
	private static final int F_BUTTON_DOWN = 4;

	private static final int F_BUTTON_PREVIEW = 5;
	
	private static final int F_UP_FLAG = -1;

	private static final int F_DOWN_FLAG = -2;
	
	private TreeViewer fTreeViewer;
	
	private ISimpleCSModel fModel;
	
	private SimpleCSAddStepAction fAddStepAction;
	
	private SimpleCSRemoveStepAction fRemoveStepAction;

	private SimpleCSRemoveSubStepAction fRemoveSubStepAction;
	
	private SimpleCSAddSubStepAction fAddSubStepAction;
	
	private SimpleCSRemoveRunObjectAction fRemoveRunObjectAction;
	
	private CollapseAction fCollapseAction;
	
	/**
	 * @param formPage
	 * @param parent
	 * @param style
	 * @param buttonLabels
	 */
	public SimpleCSMasterTreeSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[] {
				PDEUIMessages.SimpleCSElementSection_0,
				PDEUIMessages.SimpleCSElementSection_6,
				PDEUIMessages.SimpleCSElementSection_7,
				PDEUIMessages.SimpleCSElementSection_1, 
				PDEUIMessages.SimpleCSElementSection_2, 
				PDEUIMessages.SimpleCSElementSection_3});
		getSection().setText(PDEUIMessages.SimpleCSElementSection_4);
		getSection().setDescription(PDEUIMessages.SimpleCSElementSection_5);

		// Create actions
		fAddStepAction = new SimpleCSAddStepAction();
		fRemoveStepAction = new SimpleCSRemoveStepAction();
		fRemoveSubStepAction = new SimpleCSRemoveSubStepAction();
		fAddSubStepAction = new SimpleCSAddSubStepAction();
		fRemoveRunObjectAction = new SimpleCSRemoveRunObjectAction();
		fCollapseAction = null;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createTree(container, toolkit);
		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
		createSectionToolbar(section, toolkit);
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
				if ((handCursor != null) &&
						(handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});		
		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fTreeViewer, 
				PDEUIMessages.ExtensionsPage_collapseAll, 
				1, 
				fModel.getSimpleCS());
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}	
	
	/**
	 * 
	 */
	private void initialize() {
		fModel = (ISimpleCSModel)getPage().getModel();
		if (fModel == null) {
			return;
		}
		fTreeViewer.setInput(fModel);

		getTreePart().setButtonEnabled(F_BUTTON_ADD_STEP, fModel.isEditable());
		// Set to false because initial node selected is the root cheatsheet node
		getTreePart().setButtonEnabled(F_BUTTON_ADD_SUBSTEP, false);
		// Set to false because initial node selected is the root cheatsheet node
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, false);
		// Set to false because initial node selected is the root cheatsheet node
		getTreePart().setButtonEnabled(F_BUTTON_UP, false);
		// Set to false because initial node selected is the root cheatsheet node
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, false);
		getTreePart().setButtonEnabled(F_BUTTON_PREVIEW, true);
		
		ISimpleCS cheatsheet = fModel.getSimpleCS();
		// Select the cheatsheet node in the tree
		fTreeViewer.setSelection(new StructuredSelection(cheatsheet), true);
		fTreeViewer.expandToLevel(2);
	}

	/**
	 * @param container
	 * @param toolkit
	 */
	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.SINGLE, 2, toolkit);
		fTreeViewer = treePart.getTreeViewer();
		fTreeViewer.setContentProvider(new SimpleCSContentProvider());
		fTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		createTreeListeners();		
		// TODO: MP: LOW: SimpleCS: Implement drag and drop move feature
	}	

	/**
	 * 
	 */
	private void createTreeListeners() {
		// Create listener for the outline view 'link with editor' toggle 
		// button
		fTreeViewer.addPostSelectionChangedListener(
				getPage().getPDEEditor().new PDEFormEditorChangeListener());
	}	

	/**
	 * @return
	 */
	public ISelection getSelection() {
		return fTreeViewer.getSelection();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
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
		case F_BUTTON_PREVIEW:
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

	/**
	 * 
	 */
	public void updateButtons() {
		if (!fModel.isEditable()) {
			return;
		}
		Object object = ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
		ISimpleCSObject csObject = (ISimpleCSObject)object;
		boolean canAddSubItem = false;
		boolean canRemove = false;
		boolean canMoveUp = false;
		boolean canMoveDown = false;

		if (csObject != null) {
			if (csObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				ISimpleCSItem item = (ISimpleCSItem)csObject;
				if (item.getSimpleCS().isFirstItem(item) == false) {
					canMoveUp = true;
				}
				if (item.getSimpleCS().isLastItem(item) == false) {
					canMoveDown = true;
				}
				
				// Preserve cheat sheet validity
				// Semantic Rule:  Cannot have a cheat sheet with no items
				if (item.getSimpleCS().getItemCount() > 1) {
					canRemove = true;
				}
				
				// Preserve cheat sheet validity
				// Semantic Rule:  Cannot have a subitem and any of the following
				// together:  perform-when, command, action
				if (item.getExecutable() == null) {
					canAddSubItem = true;
				}
			} else if (csObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem)csObject;
				ISimpleCSObject parent = subitem.getParent();
				if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
					ISimpleCSItem item = (ISimpleCSItem)parent;
					if (item.isFirstSubItem(subitem) == false) {
						canMoveUp = true;
					}
					if (item.isLastSubItem(subitem) == false) {
						canMoveDown = true;
					}
					// Preserve cheat sheet validity
					// Semantic Rule:  Cannot have a subitem and any of the following
					// together:  perform-when, command, action
					if (item.getExecutable() == null) {
						canAddSubItem = true;
					}					
				}
				canRemove = true;
				
			} else if ((csObject.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) ||
						(csObject.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) ||
						(csObject.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN) ||
						(csObject.getType() == ISimpleCSConstants.TYPE_ACTION) ||
						(csObject.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
				// Specifically for perform-when, repeated-subitem, 
				// conditional-subitem edge cases
				// Action and command supported; but, will never be applicable
				canRemove = true;
			}
		}

		getTreePart().setButtonEnabled(F_BUTTON_ADD_SUBSTEP, canAddSubItem);
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, canRemove);
		getTreePart().setButtonEnabled(F_BUTTON_UP, canMoveUp);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, canMoveDown);
	}	
	
	/**
	 * 
	 */
	private void handleAddStepAction() {
		fAddStepAction.setSimpleCS(fModel.getSimpleCS());
		fAddStepAction.run();
	}

	/**
	 * 
	 */
	private void handleAddSubStepAction() {
		
		ISelection sel = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object == null) {
			return;
		}
		if (object instanceof ISimpleCSItem) {
			fAddSubStepAction.setParentObject((ISimpleCSObject)object);
			fAddSubStepAction.run();
		} else if (object instanceof ISimpleCSSubItem) {
			fAddSubStepAction.setParentObject(((ISimpleCSObject)object).getParent());
			fAddSubStepAction.run();
		}
		
	}	
	
	/**
	 * 
	 */
	private void handleMoveStepAction(int index) {
		ISelection sel = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		// TODO: MP: LOW: SimpleCS:  There is a flicker when adding and removing items / subitems
		// probably do to focus going to the parent
		if (object != null) {
			if (object instanceof ISimpleCSItem) {
				ISimpleCSItem item = (ISimpleCSItem)object;
				// Get the current index of the item
				int currentIndex = item.getSimpleCS().indexOfItem(item);
				// Remove the item
				item.getSimpleCS().removeItem(item);
				// Calculate the new index
				int newIndex = index;
				if (index == F_UP_FLAG) {
					newIndex = currentIndex - 1;
				} else if (index == F_DOWN_FLAG) {
					newIndex = currentIndex + 1;
				}
				// Add the item back at the specified index
				item.getSimpleCS().addItem(newIndex, item);

			} else if (object instanceof ISimpleCSSubItem) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem)object;
				// Get the current index of the subitem
				ISimpleCSObject parent = subitem.getParent();
				if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
					ISimpleCSItem item = (ISimpleCSItem)parent;				
					int currentIndex = item.indexOfSubItem(subitem);
					// Remove the item
					item.removeSubItem(subitem);
					// Calculate the new index
					int newIndex = index;
					if (index == F_UP_FLAG) {
						newIndex = currentIndex - 1;
					} else if (index == F_DOWN_FLAG) {
						newIndex = currentIndex + 1;
					}
					// Add the item back at the specified index
					item.addSubItem(newIndex, subitem);
				}
			}
		}	
		
	}
	
	/**
	 * 
	 */
	private void handlePreviewAction() {
		
		if (!(fModel instanceof IEditable)) {
			return;
		}

		IFileEditorInput input = (IFileEditorInput)getPage().getEditorInput();
		IFile file  = input.getFile();
		
		try {
			// Write the current model into a String as raw XML
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			fModel.getSimpleCS().write("", writer); //$NON-NLS-1$
			writer.flush();
			swriter.close();
			// Launch in the cheat sheet view
			OpenCheatSheetAction openAction = new OpenCheatSheetAction(
					file.getName(),
					file.getName(), 
					swriter.toString(),
					file.getLocationURI().toURL());
			openAction.run();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		// No need to call super, world changed event handled here
		// TODO: MP: HIGH: SimpleCS:  STYLE CHANGE: If anything goes wrong revert change back

		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (event.getChangeType() == IModelChangedEvent.INSERT) {
			handleModelInsertType(event);
		} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
			handleModelRemoveType(event);
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			handleModelChangeType(event);
		}		
	}

	/**
	 * @param event
	 */
	private void handleModelInsertType(IModelChangedEvent event) {
		// Insert event
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			ISimpleCSObject object = (ISimpleCSObject)objects[i];		
			if (object == null) {
				// Ignore
			} else if ((object.getType() == ISimpleCSConstants.TYPE_ITEM) ||
						(object.getType() == ISimpleCSConstants.TYPE_SUBITEM)) {
				// Refresh the parent element in the tree viewer
				fTreeViewer.refresh(object.getParent());
				// Select the new item in the tree
				fTreeViewer.setSelection(new StructuredSelection(object), true);
			}		
		}
	}

	/**
	 * @param event
	 */
	private void handleModelRemoveType(IModelChangedEvent event) {
		// Remove event
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			ISimpleCSObject object = (ISimpleCSObject)objects[i];		
			if (object == null) {
				// Ignore
			} else if (object.getType() == ISimpleCSConstants.TYPE_ITEM) {
				// Remove the item
				fTreeViewer.remove(object);
				// Select the appropriate object
				ISimpleCSObject csObject = fRemoveStepAction.getObjectToSelect();
				if (csObject == null) {
					csObject = object.getParent();
				}
				fTreeViewer.setSelection(new StructuredSelection(csObject), true);
			} else if (object.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
				// Remove the subitem
				fTreeViewer.remove(object);
				// Select the appropriate object
				ISimpleCSObject csObject = fRemoveSubStepAction.getObjectToSelect();
				if (csObject == null) {
					csObject = object.getParent();
				}
				fTreeViewer.setSelection(new StructuredSelection(csObject), true);
			} else if ((object.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) ||
					(object.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) ||
					(object.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN)) {
				// Remove the object
				fTreeViewer.remove(object);
				// Select the parent in the tree
				fTreeViewer.setSelection(new StructuredSelection(object.getParent()), true);
			}
		}		
	}
	
	/**
	 * @param event
	 */
	private void handleModelChangeType(IModelChangedEvent event) {
		// Change event
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			ISimpleCSObject object = (ISimpleCSObject)objects[i];		
			if (object == null) {
				// Ignore
			} else if ((object.getType() == ISimpleCSConstants.TYPE_ITEM) ||
						(object.getType() == ISimpleCSConstants.TYPE_SUBITEM) ||
						(object.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET)) {
				// Refresh the element in the tree viewer
				fTreeViewer.refresh(object);
			}
		}		
	}	
	
	/* (non-Javadoc)
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
		// Do blind cast - has to be a simple CS object
		// Could be null
		ISimpleCSObject csObject = (ISimpleCSObject)object;
		// Create the "New" submenu
		MenuManager submenu = new MenuManager(PDEUIMessages.Menus_new_label);
		// Add the "New" submenu to the main context menu
		manager.add(submenu);
		if ((csObject == null) ||
				(csObject.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET)) {
			// Add to the "New" submenu
			// Add step action
			fAddStepAction.setSimpleCS(fModel.getSimpleCS());
			fAddStepAction.setEnabled(fModel.isEditable());
			submenu.add(fAddStepAction);
		} else if (csObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			ISimpleCSItem item = (ISimpleCSItem)csObject;
			// Add to the "New" submenu
			// Add sub-step action
			fAddSubStepAction.setParentObject(csObject);
			// Preserve cheat sheet validity
			// Semantic Rule:  Cannot have a subitem and any of the following
			// together:  perform-when, command, action			
			if (item.getExecutable() == null) {
				fAddSubStepAction.setEnabled(fModel.isEditable());
			} else {
				fAddSubStepAction.setEnabled(false);
			}
			submenu.add(fAddSubStepAction);
			// Add to the main context menu
			// Add a separator to the main context menu
			manager.add(new Separator());
			// Delete step action
			fRemoveStepAction.setItem((ISimpleCSItem)csObject);
			// Preserve cheat sheet validity
			// Semantic Rule:  Cannot have a cheat sheet with no items
			if (item.getSimpleCS().getItemCount() > 1) {
				fRemoveStepAction.setEnabled(fModel.isEditable());
			} else {
				fRemoveStepAction.setEnabled(false);
			}
			manager.add(fRemoveStepAction);
		} else if ((csObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) ||
					(csObject.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) ||
					(csObject.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM)) {
			// Add to the main context menu
			// Add a separator to the main context menu
			manager.add(new Separator());
			// Delete sub-step action
			fRemoveSubStepAction.setSubItem((ISimpleCSSubItemObject)csObject);
			fRemoveSubStepAction.setEnabled(fModel.isEditable());
			manager.add(fRemoveSubStepAction);			
		} else if ((csObject.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN) ||
					(csObject.getType() == ISimpleCSConstants.TYPE_ACTION) ||
					(csObject.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
			// Specifically for perform-when edge case
			// Action and command supported; but, will never be applicable
			// Add to the main context menu
			// Add a separator to the main context menu
			manager.add(new Separator());
			// Delete run object action
			fRemoveRunObjectAction.setRunObject((ISimpleCSRunContainerObject)csObject);
			fRemoveRunObjectAction.setEnabled(fModel.isEditable());
			manager.add(fRemoveRunObjectAction);				
		}
		// Add normal edit operations
		// TODO: MP: LOW: SimpleCS:  Enable context menu edit operations
		//getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		//manager.add(new Separator());
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
			if (object instanceof ISimpleCSItem) {
				ISimpleCSItem item = (ISimpleCSItem)object;
				// Preserve cheat sheet validity
				// Semantic Rule:  Cannot have a cheat sheet with no items
				if (item.getSimpleCS().getItemCount() > 1) {
					fRemoveStepAction.setItem(item);
					fRemoveStepAction.run();
				} else {
					// Produce audible beep
					Display.getCurrent().beep();
				}
			} else if (object instanceof ISimpleCSSubItemObject) {
				fRemoveSubStepAction.setSubItem((ISimpleCSSubItemObject)object);
				fRemoveSubStepAction.run();
			} else if (object instanceof ISimpleCSRunContainerObject) {
				// Specifically for perform-when edge case
				// Action and command supported; but, will never be applicable
				fRemoveRunObjectAction.setRunObject((ISimpleCSRunContainerObject)object);
				fRemoveRunObjectAction.run();
			} else if (object instanceof ISimpleCS) {
				// Preserve cheat sheet validity
				// Semantic Rule:  Cannot have a cheat sheet with no root
				// cheatsheet node
				// Produce audible beep
				Display.getCurrent().beep();				
			} else if (object instanceof ISimpleCSIntro) {
				// Preserve cheat sheet validity
				// Semantic Rule:  Cannot have a cheat sheet with no 
				// introduction
				// Produce audible beep
				Display.getCurrent().beep();				
			}
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFormInput(java.lang.Object)
	 */
	public boolean setFormInput(Object object) {
		// This method allows the outline view to select items in the tree
		// Invoked by
		// org.eclipse.ui.forms.editor.IFormPage.selectReveal(Object object)
		if (object instanceof ISimpleCSObject) {
			// Select the item in the tree
			fTreeViewer.setSelection(new StructuredSelection(object), true);
			// Verify that something was actually selected
			ISelection selection = fTreeViewer.getSelection();
			if ((selection != null) && 
					(selection.isEmpty() == false)) {
				return true;
			}
		}
		return false;
	}

}
