/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.*;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.actions.*;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSElementSection
 *
 */
public class SimpleCSMasterTreeSection extends TreeSection implements ICSMaster {

	private static final int F_BUTTON_ADD_STEP = 0;

	private static final int F_BUTTON_ADD_SUBSTEP = 1;

	private static final int F_BUTTON_REMOVE = 4;

	private static final int F_BUTTON_UP = 5;

	private static final int F_BUTTON_DOWN = 6;

	private static final int F_BUTTON_PREVIEW = 9;

	private static final int F_UP_FLAG = -1;

	private static final int F_DOWN_FLAG = 1;

	private TreeViewer fTreeViewer;

	private ISimpleCSModel fModel;

	private SimpleCSAddStepAction fAddStepAction;

	private SimpleCSRemoveStepAction fRemoveStepAction;

	private SimpleCSRemoveSubStepAction fRemoveSubStepAction;

	private SimpleCSAddSubStepAction fAddSubStepAction;

	private SimpleCSRemoveRunObjectAction fRemoveRunObjectAction;

	private CollapseAction fCollapseAction;

	private ControlDecoration fSubStepInfoDecoration;

	/**
	 * @param formPage
	 * @param parent
	 * @param style
	 * @param buttonLabels
	 */
	public SimpleCSMasterTreeSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[] {SimpleMessages.SimpleCSMasterTreeSection_addStep, SimpleMessages.SimpleCSMasterTreeSection_addSubStep, null, null, SimpleMessages.SimpleCSMasterTreeSection_remove, SimpleMessages.SimpleCSMasterTreeSection_up, SimpleMessages.SimpleCSMasterTreeSection_down, null, null, SimpleMessages.SimpleCSMasterTreeSection_preview});

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
		// Get the model
		fModel = (ISimpleCSModel) getPage().getModel();
		// Set section title 
		section.setText(SimpleMessages.SimpleCSMasterTreeSection_sectionTitle);
		// Set section description
		section.setDescription(SimpleMessages.SimpleCSMasterTreeSection_sectionDescription);
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
		fCollapseAction = new CollapseAction(fTreeViewer, SimpleMessages.SimpleCSMasterTreeSection_collapseAll, 1, fModel.getSimpleCS());
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);

		section.setTextClient(toolbar);
	}

	/**
	 * 
	 */
	private void initializeTreeViewer() {
		ISelection selection = fTreeViewer.getSelection();
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

		//ISimpleCS cheatsheet = fModel.getSimpleCS();
		// Select the cheatsheet node in the tree
		//fTreeViewer.setSelection(new StructuredSelection(cheatsheet), true);
		//fTreeViewer.expandToLevel(2);
		fTreeViewer.setSelection(selection, true);
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
		fTreeViewer.setLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());
		PDEUserAssistanceUIPlugin.getDefault().getLabelProvider().connect(this);
		createTreeListeners();
		createSubStepInfoDecoration();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#isDragAndDropEnabled()
	 */
	protected boolean isDragAndDropEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDragMove(java.lang.Object[])
	 */
	public boolean canDragMove(Object[] sourceObjects) {
		// Validate source objects
		if (validatePaste(sourceObjects) == false) {
			return false;
		}
		ISelection selection = new StructuredSelection(sourceObjects);
		return canCut(selection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Validate arguments
		if (validatePaste(targetObject, sourceObjects) == false) {
			return false;
		}
		// Multi-select not supported
		ISimpleCSObject sourceCSObject = (ISimpleCSObject) sourceObjects[0];
		ISimpleCSObject targetCSObject = (ISimpleCSObject) targetObject;
		// Objects have to be from the same model
		ISimpleCSModel sourceModel = sourceCSObject.getModel();
		ISimpleCSModel targetModel = targetCSObject.getModel();
		if (sourceModel.equals(targetModel) == false) {
			return false;
		}
		// Validate move
		if (sourceCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			ISimpleCSItem sourceItem = (ISimpleCSItem) sourceCSObject;
			if (targetCSObject.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
				// Source:  Item
				// Target:  Cheat Sheet
				ISimpleCS targetCheatSheet = (ISimpleCS) targetCSObject;
				return canDropMove(targetCheatSheet, sourceItem, targetLocation);
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				// Source:  Item
				// Target:  Item
				ISimpleCSItem targetItem = (ISimpleCSItem) targetCSObject;
				return canDropMove(targetItem, sourceItem, targetLocation);
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_INTRO) {
				// Source:  Item
				// Target:  Intro
				ISimpleCSIntro targetIntro = (ISimpleCSIntro) targetCSObject;
				return canDropMove(targetIntro, sourceItem, targetLocation);
			}
		} else if (sourceCSObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			ISimpleCSSubItem sourceSubItem = (ISimpleCSSubItem) sourceCSObject;
			if (targetCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				// Source:  SubItem
				// Target:  Item
				ISimpleCSItem targetItem = (ISimpleCSItem) targetCSObject;
				return canDropMove(targetItem, sourceSubItem, targetLocation);
			} else if ((targetCSObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) && (targetCSObject.getParent().getType() == ISimpleCSConstants.TYPE_ITEM)) {
				// Source:  SubItem
				// Target:  SubItem
				ISimpleCSSubItem targetSubItem = (ISimpleCSSubItem) targetCSObject;
				return canDropMove(targetSubItem, sourceSubItem, targetLocation);
			}
		}
		return false;
	}

	/**
	 * @param targetCheatSheet
	 * @param sourceItem
	 * @param targetLocation
	 * @return
	 */
	private boolean canDropMove(ISimpleCS targetCheatSheet, ISimpleCSItem sourceItem, int targetLocation) {
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			return false;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			return false;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			if (targetCheatSheet.isLastItem(sourceItem)) {
				return false;
			}
			// Paste item as last child of cheat sheet root 
			return true;
		}
		return false;
	}

	/**
	 * @param targetItem
	 * @param sourceItem
	 * @param targetLocation
	 * @return
	 */
	private boolean canDropMove(ISimpleCSItem targetItem, ISimpleCSItem sourceItem, int targetLocation) {
		ISimpleCS parent = targetItem.getSimpleCS();
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			IDocumentElementNode previousNode = parent.getPreviousSibling(targetItem, ISimpleCSItem.class);
			if (sourceItem.equals(previousNode)) {
				return false;
			}
			// Paste item as sibling of item (before)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetItem, ISimpleCSItem.class);
			if (sourceItem.equals(nextNode)) {
				return false;
			}
			// Paste item as sibling of item (after)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetItem, ISimpleCSItem.class);
			if (sourceItem.equals(nextNode)) {
				return false;
			}
			// Paste item as sibling of item (after)
			return true;
		}
		return false;
	}

	/**
	 * @param targetIntro
	 * @param sourceItem
	 * @param targetLocation
	 * @return
	 */
	private boolean canDropMove(ISimpleCSIntro targetIntro, ISimpleCSItem sourceItem, int targetLocation) {
		ISimpleCS parent = targetIntro.getSimpleCS();
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			return false;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetIntro, ISimpleCSItem.class);
			if (sourceItem.equals(nextNode)) {
				return false;
			}
			// Paste item as sibling of intro (first item child of cheat sheet after intro)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetIntro, ISimpleCSItem.class);
			if (sourceItem.equals(nextNode)) {
				return false;
			}
			// Paste item as sibling of intro (first item child of cheat sheet after intro)
			return true;
		}
		return false;
	}

	/**
	 * @param targetItem
	 * @param sourceSubItem
	 * @param targetLocation
	 * @return
	 */
	private boolean canDropMove(ISimpleCSItem targetItem, ISimpleCSSubItem sourceSubItem, int targetLocation) {
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			return false;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			if (targetItem.getExecutable() != null) {
				return false;
			} else if (targetItem.isFirstSubItem(sourceSubItem)) {
				return false;
			}
			// Paste subitem as the first child of item 
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			if (targetItem.getExecutable() != null) {
				return false;
			} else if (targetItem.isLastSubItem(sourceSubItem)) {
				return false;
			}
			// Paste subitem as the last child of item 
			return true;
		}
		return false;
	}

	/**
	 * @param targetSubItem
	 * @param sourceSubItem
	 * @param targetLocation
	 * @return
	 */
	private boolean canDropMove(ISimpleCSSubItem targetSubItem, ISimpleCSSubItem sourceSubItem, int targetLocation) {
		ISimpleCSItem parent = (ISimpleCSItem) targetSubItem.getParent();
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			IDocumentElementNode previousNode = parent.getPreviousSibling(targetSubItem, ISimpleCSSubItem.class);
			if (sourceSubItem.equals(previousNode)) {
				return false;
			}
			// Paste subitem as sibling of subitem (before)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetSubItem, ISimpleCSSubItem.class);
			if (sourceSubItem.equals(nextNode)) {
				return false;
			}
			// Paste subitem as sibling of subitem (after)
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetSubItem, ISimpleCSSubItem.class);
			if (sourceSubItem.equals(nextNode)) {
				return false;
			}
			// Paste subitem as sibling of subitem (after)
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Validate arguments
		if (validatePaste(targetObject, sourceObjects) == false) {
			Display.getDefault().beep();
			return;
		}
		// Multi-select not supported
		ISimpleCSObject sourceCSObject = (ISimpleCSObject) sourceObjects[0];
		ISimpleCSObject targetCSObject = (ISimpleCSObject) targetObject;
		// Validate move
		if (sourceCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			ISimpleCSItem sourceItem = (ISimpleCSItem) sourceCSObject;
			if (targetCSObject.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
				// Source:  Item
				// Target:  Cheat Sheet
				ISimpleCS targetCheatSheet = (ISimpleCS) targetCSObject;
				doDropMove(targetCheatSheet, sourceItem, targetLocation);
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				// Source:  Item
				// Target:  Item
				ISimpleCSItem targetItem = (ISimpleCSItem) targetCSObject;
				doDropMove(targetItem, sourceItem, targetLocation);
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_INTRO) {
				// Source:  Item
				// Target:  Intro
				ISimpleCSIntro targetIntro = (ISimpleCSIntro) targetCSObject;
				doDropMove(targetIntro, sourceItem, targetLocation);
			}
		} else if (sourceCSObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			ISimpleCSSubItem sourceSubItem = (ISimpleCSSubItem) sourceCSObject;
			if (targetCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				// Source:  SubItem
				// Target:  Item
				ISimpleCSItem targetItem = (ISimpleCSItem) targetCSObject;
				doDropMove(targetItem, sourceSubItem, targetLocation);
			} else if ((targetCSObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) && (targetCSObject.getParent().getType() == ISimpleCSConstants.TYPE_ITEM)) {
				// Source:  SubItem
				// Target:  SubItem
				ISimpleCSSubItem targetSubItem = (ISimpleCSSubItem) targetCSObject;
				doDropMove(targetSubItem, sourceSubItem, targetLocation);
			}
		}
	}

	/**
	 * @param targetCheatSheet
	 * @param sourceItem
	 * @param targetLocation
	 */
	private void doDropMove(ISimpleCS targetCheatSheet, ISimpleCSItem sourceItem, int targetLocation) {
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// NO-OP, not legal
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// NO-OP, not legal
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			if (targetCheatSheet.isLastItem(sourceItem)) {
				return;
			}
			// Adjust all the source object transient field values to
			// acceptable values
			sourceItem.reconnect(targetCheatSheet, fModel);
			// Paste item as the last child of cheat sheet root 
			targetCheatSheet.addItem(sourceItem);
		}
	}

	/**
	 * @param targetItem
	 * @param sourceItem
	 * @param targetLocation
	 */
	private void doDropMove(ISimpleCSItem targetItem, ISimpleCSItem sourceItem, int targetLocation) {
		ISimpleCS parent = targetItem.getSimpleCS();
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			IDocumentElementNode previousNode = parent.getPreviousSibling(targetItem, ISimpleCSItem.class);
			if (sourceItem.equals(previousNode)) {
				// NO-OP, not legal
				return;
			}
			// Adjust all the source object transient field values to
			// acceptable values
			sourceItem.reconnect(parent, fModel);
			// Get index of target item
			int index = parent.indexOfItem(targetItem);
			// Paste item as sibling of item (before) 
			if (index != -1) {
				parent.addItem(index, sourceItem);
			}
		} else if ((targetLocation == ViewerDropAdapter.LOCATION_AFTER) || (targetLocation == ViewerDropAdapter.LOCATION_ON)) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetItem, ISimpleCSItem.class);
			if (sourceItem.equals(nextNode)) {
				// NO-OP, not legal
				return;
			}
			// Adjust all the source object transient field values to
			// acceptable values
			sourceItem.reconnect(parent, fModel);

			if (nextNode == null) {
				parent.addItem(sourceItem);
			} else {
				// Get index of target item
				int index = parent.indexOfItem((ISimpleCSItem) nextNode);
				// Paste item as sibling of item (after)
				if (index != -1) {
					parent.addItem(index, sourceItem);
				}
			}
		}
	}

	/**
	 * @param targetIntro
	 * @param sourceItem
	 * @param targetLocation
	 */
	private void doDropMove(ISimpleCSIntro targetIntro, ISimpleCSItem sourceItem, int targetLocation) {
		ISimpleCS parent = targetIntro.getSimpleCS();
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// NO-OP, not legal
		} else if ((targetLocation == ViewerDropAdapter.LOCATION_AFTER) || (targetLocation == ViewerDropAdapter.LOCATION_ON)) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetIntro, ISimpleCSItem.class);
			if (sourceItem.equals(nextNode)) {
				// NO-OP, not legal
				return;
			}
			// Adjust all the source object transient field values to
			// acceptable values
			sourceItem.reconnect(parent, fModel);
			if (nextNode == null) {
				parent.addItem(sourceItem);
			} else {
				// Get index of target item
				int index = parent.indexOfItem((ISimpleCSItem) nextNode);
				// Paste item as sibling of intro (first item child of cheat sheet after intro)
				if (index != -1) {
					parent.addItem(index, sourceItem);
				}
			}
		}
	}

	/**
	 * @param targetItem
	 * @param sourceSubItem
	 * @param targetLocation
	 */
	private void doDropMove(ISimpleCSItem targetItem, ISimpleCSSubItem sourceSubItem, int targetLocation) {
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// NO-OP, not legal
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			if (targetItem.getExecutable() != null) {
				return;
			} else if (targetItem.isFirstSubItem(sourceSubItem)) {
				return;
			}
			// Adjust all the source object transient field values to
			// acceptable values
			sourceSubItem.reconnect(targetItem, fModel);
			// Get the item's first subitem child
			ISimpleCSSubItem firstSubItem = (ISimpleCSSubItem) targetItem.getChildNode(ISimpleCSSubItem.class);
			// Paste subitem as the first child of item
			if (firstSubItem == null) {
				targetItem.addSubItem(sourceSubItem);
			} else {
				int index = targetItem.indexOfSubItem(firstSubItem);
				// Paste subitem as the first child of item
				if (index != -1) {
					targetItem.addSubItem(index, sourceSubItem);
				}
			}
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			if (targetItem.getExecutable() != null) {
				return;
			} else if (targetItem.isLastSubItem(sourceSubItem)) {
				return;
			}
			// Adjust all the source object transient field values to
			// acceptable values
			sourceSubItem.reconnect(targetItem, fModel);
			// Paste subitem as the last child of item
			targetItem.addSubItem(sourceSubItem);
		}
	}

	/**
	 * @param targetSubItem
	 * @param sourceSubItem
	 * @param targetLocation
	 */
	private void doDropMove(ISimpleCSSubItem targetSubItem, ISimpleCSSubItem sourceSubItem, int targetLocation) {
		ISimpleCSItem parent = (ISimpleCSItem) targetSubItem.getParent();
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			IDocumentElementNode previousNode = parent.getPreviousSibling(targetSubItem, ISimpleCSSubItem.class);
			if (sourceSubItem.equals(previousNode)) {
				// NO-OP, not legal
				return;
			}
			// Adjust all the source object transient field values to
			// acceptable values
			sourceSubItem.reconnect(parent, fModel);
			// Get index of target item
			int index = parent.indexOfSubItem(targetSubItem);
			// Paste item as sibling of item (before) 
			if (index != -1) {
				parent.addSubItem(index, sourceSubItem);
			}
		} else if ((targetLocation == ViewerDropAdapter.LOCATION_AFTER) || (targetLocation == ViewerDropAdapter.LOCATION_ON)) {
			IDocumentElementNode nextNode = parent.getNextSibling(targetSubItem, ISimpleCSSubItem.class);
			if (sourceSubItem.equals(nextNode)) {
				// NO-OP, not legal
				return;
			}
			// Adjust all the source object transient field values to
			// acceptable values
			sourceSubItem.reconnect(parent, fModel);

			if (nextNode == null) {
				parent.addSubItem(sourceSubItem);
			} else {
				// Get index of target item
				int index = parent.indexOfSubItem((ISimpleCSSubItem) nextNode);
				// Paste item as sibling of item (after)
				if (index != -1) {
					parent.addSubItem(index, sourceSubItem);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDragRemove(java.lang.Object[])
	 */
	public void doDragRemove(Object[] sourceObjects) {
		// Validate source objects
		if (validatePaste(sourceObjects) == false) {
			return;
		}
		ISimpleCSObject object = (ISimpleCSObject) sourceObjects[0];
		// Remove the object from the model
		if (object.getType() == ISimpleCSConstants.TYPE_ITEM) {
			ISimpleCSItem item = (ISimpleCSItem) object;
			ISimpleCS cheatsheet = object.getSimpleCS();
			doSelect(false);
			cheatsheet.removeItem(item);
			doSelect(true);
		} else if (object.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			ISimpleCSObject parent = object.getParent();
			if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem) object;
				ISimpleCSItem item = ((ISimpleCSItem) parent);
				doSelect(false);
				item.removeSubItem(subitem);
				doSelect(true);
			}
		}
		// Applicable for move operations
		// Flush the text edit operations associated with the move operation
		// to the source page
		// Move involves add new cloned object x and remove of original object
		// x 
		// Without flushing, multiple move operations up and down cause the
		// text edit operations to get completely screwed up (e.g. mark-up
		// in wrong position or getting lost)
		// TODO: MP: Undo: What are the implications of this?
		((PDEFormEditor) getPage().getEditor()).getContextManager().getPrimaryContext().flushEditorInput();
	}

	/**
	 * 
	 */
	private void createSubStepInfoDecoration() {
		//
		Button button = getStructuredViewerPart().getButton(F_BUTTON_ADD_SUBSTEP);
		int bits = SWT.TOP | SWT.RIGHT;
		fSubStepInfoDecoration = new ControlDecoration(button, bits);
		fSubStepInfoDecoration.setMarginWidth(0);
		updateSubStepInfoDecoration(false, false, false);
		fSubStepInfoDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
	}

	/**
	 * @param show
	 * @param itemHasNoExecutable
	 * @param itemIsNotOptional
	 */
	private void updateSubStepInfoDecoration(boolean show, boolean itemHasNoExecutable, boolean itemIsNotOptional) {
		//
		if (show) {
			fSubStepInfoDecoration.show();
			if (itemHasNoExecutable == false) {
				fSubStepInfoDecoration.setDescriptionText(SimpleMessages.SimpleCSMasterTreeSection_descriptionText1);
			} else if (itemIsNotOptional == false) {
				fSubStepInfoDecoration.setDescriptionText(SimpleMessages.SimpleCSMasterTreeSection_descriptionText2);
			}
		} else {
			fSubStepInfoDecoration.hide();
		}
		fSubStepInfoDecoration.setShowHover(show);
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		switch (index) {
			case F_BUTTON_ADD_STEP :
				handleAddStepAction();
				break;
			case F_BUTTON_ADD_SUBSTEP :
				handleAddSubStepAction();
				break;
			case F_BUTTON_REMOVE :
				handleDeleteAction();
				break;
			case F_BUTTON_UP :
				handleMoveStepAction(F_UP_FLAG);
				break;
			case F_BUTTON_DOWN :
				handleMoveStepAction(F_DOWN_FLAG);
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
		// Update global selection used by source page to sychronize selections
		// made in the master tree viewer with elements in the source view
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

	/**
	 * 
	 */
	public void updateButtons() {
		if (!fModel.isEditable()) {
			return;
		}
		ISimpleCSObject csObject = getCurrentSelection();

		boolean canAddItem = false;
		boolean canAddSubItem = false;
		boolean canRemove = false;
		boolean canMoveUp = false;
		boolean canMoveDown = false;

		boolean itemHasNoExecutable = false;
		boolean itemIsNotOptional = false;
		boolean showDecoration = false;

		if (csObject != null) {

			if (csObject.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
				// Add item to end of cheat sheet child items
				canAddItem = true;
			} else if (csObject.getType() == ISimpleCSConstants.TYPE_INTRO) {
				// Add item as the first cheat sheet child item
				// which is right after the introduction node
				canAddItem = true;
			} else if (csObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				ISimpleCSItem item = (ISimpleCSItem) csObject;
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
				// Preserve cheat sheet validity
				// Semantic Rule:  Cannot add subitems to an item that is 
				// optional		
				itemHasNoExecutable = (item.getExecutable() == null);
				itemIsNotOptional = (item.getSkip() == false);
				if (itemHasNoExecutable && itemIsNotOptional) {
					canAddSubItem = true;
				}
				showDecoration = (canAddSubItem == false);
				// Add item right after this item
				canAddItem = true;

			} else if (csObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem) csObject;
				ISimpleCSObject parent = subitem.getParent();
				if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
					ISimpleCSItem item = (ISimpleCSItem) parent;
					if (item.isFirstSubItem(subitem) == false) {
						canMoveUp = true;
					}
					if (item.isLastSubItem(subitem) == false) {
						canMoveDown = true;
					}
					// Preserve cheat sheet validity
					// Semantic Rule:  Cannot have a subitem and any of the following
					// together:  perform-when, command, action
					// Preserve cheat sheet validity
					// Semantic Rule:  Cannot add subitems to an item that is 
					// optional				
					itemHasNoExecutable = (item.getExecutable() == null);
					itemIsNotOptional = (item.getSkip() == false);
					if (itemHasNoExecutable && itemIsNotOptional) {
						canAddSubItem = true;
					}
					showDecoration = (canAddSubItem == false);
				}
				canRemove = true;

			} else if ((csObject.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) || (csObject.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) || (csObject.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN) || (csObject.getType() == ISimpleCSConstants.TYPE_ACTION) || (csObject.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
				// Specifically for perform-when, repeated-subitem, 
				// conditional-subitem edge cases
				// Action and command supported; but, will never be applicable
				canRemove = true;
			}

			updateSubStepInfoDecoration(showDecoration, itemHasNoExecutable, itemIsNotOptional);
		}

		getTreePart().setButtonEnabled(F_BUTTON_ADD_STEP, canAddItem);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_SUBSTEP, canAddSubItem);
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, canRemove);
		getTreePart().setButtonEnabled(F_BUTTON_UP, canMoveUp);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, canMoveDown);
	}

	/**
	 * 
	 */
	private void handleAddStepAction() {
		// Get the current selection
		ISimpleCSObject csObject = getCurrentSelection();
		// If nothing is selected add to the root cheat sheet node
		if (csObject == null) {
			fAddStepAction.setDataObject(fModel.getSimpleCS());
		} else {
			fAddStepAction.setDataObject(csObject);
		}
		// Execute the action
		fAddStepAction.run();
	}

	/**
	 * @return
	 */
	private ISimpleCSObject getCurrentSelection() {
		ISelection selection = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) selection).getFirstElement();
		return (ISimpleCSObject) object;
	}

	/**
	 * 
	 */
	private void handleAddSubStepAction() {
		// Get the current selection
		ISimpleCSObject csObject = getCurrentSelection();
		// Ensure the selection is defined
		if (csObject == null) {
			return;
		}
		// Set the selection object to operate on
		fAddSubStepAction.setDataObject(csObject);
		// Execute the action
		fAddSubStepAction.run();
	}

	/**
	 * 
	 */
	private void handleMoveStepAction(int positionFlag) {
		ISimpleCSObject object = getCurrentSelection();
		if (object != null) {
			if (object instanceof ISimpleCSItem) {
				ISimpleCSItem item = (ISimpleCSItem) object;
				item.getSimpleCS().moveItem(item, positionFlag);
			} else if (object instanceof ISimpleCSSubItem) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem) object;
				// Get the current index of the subitem
				ISimpleCSObject parent = subitem.getParent();
				if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
					ISimpleCSItem item = (ISimpleCSItem) parent;
					item.moveSubItem(subitem, positionFlag);
				}
			}
		}
	}

	/**
	 * 
	 */
	private void handlePreviewAction() {
		// Get the editor
		PDEFormEditor editor = (PDEFormEditor) getPage().getEditor();
		// Get the form editor contributor
		SimpleCSEditorContributor contributor = (SimpleCSEditorContributor) editor.getContributor();
		// Get the preview action
		SimpleCSPreviewAction previewAction = contributor.getPreviewAction();
		// Set the cheat sheet object
		previewAction.setDataModelObject(fModel.getSimpleCS());
		// Set the editor input
		previewAction.setEditorInput(getPage().getEditorInput());
		// Run the preview action
		previewAction.run();
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
		} else if ((event.getChangeType() == IModelChangedEvent.CHANGE) && (event.getChangedProperty().equals(IDocumentElementNode.F_PROPERTY_CHANGE_TYPE_SWAP))) {
			handleModelChangeTypeSwap(event);
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			handleModelChangeType(event);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		// Get the form page
		SimpleCSDefinitionPage page = (SimpleCSDefinitionPage) getPage();
		// Replace the current dirty model with the model reloaded from
		// file
		fModel = (ISimpleCSModel) page.getModel();
		// Re-initialize the tree viewer.  Makes a details page selection
		initializeTreeViewer();

		super.refresh();
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Section will be updated on refresh
		markStale();
	}

	/**
	 * @param event
	 */
	private void handleModelChangeTypeSwap(IModelChangedEvent event) {
		// Swap event
		Object[] objects = event.getChangedObjects();
		// Ensure right type
		if ((objects[0] instanceof ISimpleCSObject) == false) {
			return;
		}
		ISimpleCSObject object = (ISimpleCSObject) objects[0];
		if (object == null) {
			// Ignore
		} else if ((object.getType() == ISimpleCSConstants.TYPE_ITEM) || (object.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET)) {
			// Refresh the element
			fTreeViewer.refresh(object);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelInsertType(IModelChangedEvent event) {
		// Insert event
		Object[] objects = event.getChangedObjects();
		ISimpleCSObject object = (ISimpleCSObject) objects[0];
		if (object == null) {
			// Ignore
		} else if ((object.getType() == ISimpleCSConstants.TYPE_ITEM) || (object.getType() == ISimpleCSConstants.TYPE_SUBITEM)) {
			// Refresh the parent element in the tree viewer
			fTreeViewer.refresh(object.getParent());
			// Select the new item in the tree
			fTreeViewer.setSelection(new StructuredSelection(object), true);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelRemoveType(IModelChangedEvent event) {
		// Remove event
		Object[] objects = event.getChangedObjects();
		ISimpleCSObject object = (ISimpleCSObject) objects[0];
		if (object == null) {
			// Ignore
		} else if (object.getType() == ISimpleCSConstants.TYPE_ITEM) {
			// Remove the item
			fTreeViewer.remove(object);
			// Determine if we should make a selection
			if (canSelect() == false) {
				return;
			}
			// Select the appropriate object
			ISimpleCSObject csObject = fRemoveStepAction.getObjectToSelect();
			if (csObject == null) {
				csObject = object.getParent();
			}
			fTreeViewer.setSelection(new StructuredSelection(csObject), true);
		} else if (object.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			// Remove the subitem
			fTreeViewer.remove(object);
			// Determine if we should make a selection
			if (canSelect() == false) {
				return;
			}
			// Select the appropriate object
			ISimpleCSObject csObject = fRemoveSubStepAction.getObjectToSelect();
			if (csObject == null) {
				csObject = object.getParent();
			}
			fTreeViewer.setSelection(new StructuredSelection(csObject), true);
		} else if ((object.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) || (object.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) || (object.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN)) {
			// Remove the object
			fTreeViewer.remove(object);
			// Select the parent in the tree
			fTreeViewer.setSelection(new StructuredSelection(object.getParent()), true);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelChangeType(IModelChangedEvent event) {
		// Change event
		Object[] objects = event.getChangedObjects();
		// Ensure right type
		if ((objects[0] instanceof ISimpleCSObject) == false) {
			return;
		}
		ISimpleCSObject object = (ISimpleCSObject) objects[0];
		if (object == null) {
			// Ignore
		} else if ((object.getType() == ISimpleCSConstants.TYPE_ITEM) || (object.getType() == ISimpleCSConstants.TYPE_SUBITEM) || (object.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET)) {
			// Refresh the element in the tree viewer
			fTreeViewer.update(object, null);
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
		ISimpleCSObject csObject = getCurrentSelection();
		// Create the "New" submenu
		MenuManager submenu = new MenuManager(SimpleMessages.SimpleCSMasterTreeSection_new);
		// Add the "New" submenu to the main context menu
		manager.add(submenu);
		if ((csObject == null) || (csObject.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET)) {
			// Add to the "New" submenu
			// Add step action
			fAddStepAction.setDataObject(fModel.getSimpleCS());
			fAddStepAction.setEnabled(fModel.isEditable());
			submenu.add(fAddStepAction);
		} else if (csObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			ISimpleCSItem item = (ISimpleCSItem) csObject;
			// Add to the "New" submenu
			// Add sub-step action
			fAddSubStepAction.setDataObject(csObject);
			// Preserve cheat sheet validity
			// Semantic Rule:  Cannot have a subitem and any of the following
			// together:  perform-when, command, action			
			// Preserve cheat sheet validity
			// Semantic Rule:  Cannot add subitems to an item that is 
			// optional				
			if ((item.getExecutable() == null) && (item.getSkip() == false)) {
				fAddSubStepAction.setEnabled(fModel.isEditable());
			} else {
				fAddSubStepAction.setEnabled(false);
			}
			submenu.add(fAddSubStepAction);
			// Add to the main context menu
			// Add a separator to the main context menu
			manager.add(new Separator());
			// Delete step action
			fRemoveStepAction.setItem((ISimpleCSItem) csObject);
			// Preserve cheat sheet validity
			// Semantic Rule:  Cannot have a cheat sheet with no items
			if (item.getSimpleCS().getItemCount() > 1) {
				fRemoveStepAction.setEnabled(fModel.isEditable());
			} else {
				fRemoveStepAction.setEnabled(false);
			}
			manager.add(fRemoveStepAction);
		} else if ((csObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) || (csObject.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) || (csObject.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM)) {
			// Add to the main context menu
			// Add a separator to the main context menu
			manager.add(new Separator());
			// Delete sub-step action
			fRemoveSubStepAction.setSubItem((ISimpleCSSubItemObject) csObject);
			fRemoveSubStepAction.setEnabled(fModel.isEditable());
			manager.add(fRemoveSubStepAction);
		} else if ((csObject.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN) || (csObject.getType() == ISimpleCSConstants.TYPE_ACTION) || (csObject.getType() == ISimpleCSConstants.TYPE_COMMAND)) {
			// Specifically for perform-when edge case
			// Action and command supported; but, will never be applicable
			// Add to the main context menu
			// Add a separator to the main context menu
			manager.add(new Separator());
			// Delete run object action
			fRemoveRunObjectAction.setRunObject((ISimpleCSRunContainerObject) csObject);
			fRemoveRunObjectAction.setEnabled(fModel.isEditable());
			manager.add(fRemoveRunObjectAction);
		}
		// Add clipboard operations
		manager.add(new Separator());
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		// Ensure model is editable
		if (isEditable() == false) {
			return false;
		} else if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDeleteAction();
			return true;
		} else if (actionId.equals(ActionFactory.CUT.getId())) {
			// Handle the delete here and let the editor transfer
			// the selection to the clipboard
			handleDeleteAction();
			return false;
		} else if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}

	/**
	 * @param object
	 */
	private void handleDeleteAction() {
		ISimpleCSObject object = getCurrentSelection();
		if (object != null) {
			if (object instanceof ISimpleCSItem) {
				ISimpleCSItem item = (ISimpleCSItem) object;
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
				fRemoveSubStepAction.setSubItem((ISimpleCSSubItemObject) object);
				fRemoveSubStepAction.run();
			} else if (object instanceof ISimpleCSRunContainerObject) {
				// Specifically for perform-when edge case
				// Action and command supported; but, will never be applicable
				fRemoveRunObjectAction.setRunObject((ISimpleCSRunContainerObject) object);
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
			if ((selection != null) && (selection.isEmpty() == false)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param targetObject
	 * @param sourceObjects
	 * @return
	 */
	private boolean validatePaste(Object targetObject, Object[] sourceObjects) {
		// Validate target object
		if ((targetObject instanceof ISimpleCSObject) == false) {
			return false;
		}
		// Validate source objects
		if (validatePaste(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourceObjects
	 * @return
	 */
	private boolean validatePaste(Object[] sourceObjects) {
		// Validate source objects
		if (sourceObjects == null) {
			return false;
		} else if (sourceObjects.length != 1) {
			return false;
		} else if ((sourceObjects[0] instanceof ISimpleCSObject) == false) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		// Validate arguments
		if (validatePaste(targetObject, sourceObjects) == false) {
			return false;
		}
		// Multi-select not supported
		ISimpleCSObject sourceCSObject = (ISimpleCSObject) sourceObjects[0];
		ISimpleCSObject targetCSObject = (ISimpleCSObject) targetObject;
		// Validate paste
		if (sourceCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			if (targetCSObject.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
				// Paste item as child of cheat sheet root 
				return true;
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				// Paste item as sibling of item
				return true;
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_INTRO) {
				// Paste item as sibling of intro (first item child of cheat sheet)
				return true;
			}
		} else if (sourceCSObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			if (targetCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				// Paste subitem as child of item
				return true;
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
				// Paste subitem as sibling of subitem
				return true;
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// Validate arguments
		if (validatePaste(targetObject, sourceObjects) == false) {
			Display.getDefault().beep();
			return;
		}
		// Multi-select not supported
		ISimpleCSObject sourceCSObject = (ISimpleCSObject) sourceObjects[0];
		ISimpleCSObject targetCSObject = (ISimpleCSObject) targetObject;
		// Validate paste
		if (sourceCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			ISimpleCSItem sourceItem = (ISimpleCSItem) sourceCSObject;
			if (targetCSObject.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
				ISimpleCS targetCheatSheet = (ISimpleCS) targetCSObject;
				// Adjust all the source object transient field values to
				// acceptable values
				sourceItem.reconnect(targetCheatSheet, fModel);
				// Paste item as the last child of cheat sheet root 
				targetCheatSheet.addItem(sourceItem);
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				ISimpleCSItem targetItem = (ISimpleCSItem) targetCSObject;
				ISimpleCS targetCheatSheet = targetItem.getSimpleCS();
				// Adjust all the source object transient field values to
				// acceptable values
				sourceItem.reconnect(targetCheatSheet, fModel);
				// Paste source item as sibling of the target item (right after it)
				int index = targetCheatSheet.indexOfItem(targetItem) + 1;
				targetCheatSheet.addItem(index, sourceItem);
			} else if (targetCSObject.getType() == ISimpleCSConstants.TYPE_INTRO) {
				ISimpleCSIntro targetIntro = (ISimpleCSIntro) targetCSObject;
				ISimpleCS targetCheatSheet = targetCSObject.getSimpleCS();
				// Adjust all the source object transient field values to
				// acceptable values
				sourceItem.reconnect(targetCheatSheet, fModel);
				// Paste source item as the first item (right after intro node)
				int index = targetCheatSheet.indexOf(targetIntro) + 1;
				targetCheatSheet.addItem(index, sourceItem);
			}
		} else if (sourceCSObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			ISimpleCSSubItem sourceSubitem = (ISimpleCSSubItem) sourceCSObject;
			if (targetCSObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
				ISimpleCSItem targetItem = (ISimpleCSItem) targetCSObject;
				// Adjust all the source object transient field values to
				// acceptable values
				sourceSubitem.reconnect(targetItem, fModel);
				// Paste subitem as the last child of the item 
				targetItem.addSubItem(sourceSubitem);
			} else if ((targetCSObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) && (targetCSObject.getParent().getType() == ISimpleCSConstants.TYPE_ITEM)) {
				ISimpleCSSubItem targetSubItem = (ISimpleCSSubItem) targetCSObject;
				ISimpleCSItem targetItem = (ISimpleCSItem) targetSubItem.getParent();
				// Adjust all the source object transient field values to
				// acceptable values
				sourceSubitem.reconnect(targetItem, fModel);
				// Paste source item as sibling of the target item (right after it)
				int index = targetItem.indexOfSubItem(targetSubItem) + 1;
				targetItem.addSubItem(index, sourceSubitem);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#canCut(org.eclipse.jface.viewers.ISelection)
	 */
	public boolean canCut(ISelection selection) {
		// Validate selection
		if (selection == null) {
			return false;
		} else if ((selection instanceof IStructuredSelection) == false) {
			return false;
		} else if (selection.isEmpty()) {
			return false;
		}
		// Get the first element
		Object object = ((IStructuredSelection) selection).getFirstElement();
		// Ensure we have a CS object
		if ((object instanceof ISimpleCSObject) == false) {
			return false;
		}
		ISimpleCSObject csObject = (ISimpleCSObject) object;
		// Can cut only items and subitems
		if ((csObject.getType() == ISimpleCSConstants.TYPE_ITEM) && (csObject.getSimpleCS().getItemCount() != 1)) {
			// Is an item and is not the last item
			return true;
		} else if (object instanceof ISimpleCSSubItem) {
			// Is a subitem
			return true;
		}
		// Cannot cut anything else
		return false;
	}

}
