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

import java.net.MalformedURLException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSAddStepAction;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSAddSubStepAction;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSRemoveStepAction;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.actions.SimpleCSRemoveSubStepAction;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSElementSection
 *
 */
public class SimpleCSElementSection extends TreeSection {

	// TODO: MP: Add button: Add subitem - context sensitive
	// TODO: MP: Add button: Remove - context sensitive
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
	
	
	/**
	 * @param formPage
	 * @param parent
	 * @param style
	 * @param buttonLabels
	 */
	public SimpleCSElementSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[] {
				PDEUIMessages.SimpleCSElementSection_0,
				PDEUIMessages.SimpleCSElementSection_6,
				PDEUIMessages.SimpleCSElementSection_7,
				PDEUIMessages.SimpleCSElementSection_1, 
				PDEUIMessages.SimpleCSElementSection_2, 
				PDEUIMessages.SimpleCSElementSection_3});
		getSection().setText(PDEUIMessages.SimpleCSElementSection_4);
		// TODO: MP: Put for details section
		//getSection().setDescription("The following properties are available for this cheat sheet element:");
		getSection().setDescription(PDEUIMessages.SimpleCSElementSection_5);
		
		// Create actions
		fAddStepAction = new SimpleCSAddStepAction();
		fRemoveStepAction = new SimpleCSRemoveStepAction();
		fRemoveSubStepAction = new SimpleCSRemoveSubStepAction();
		fAddSubStepAction = new SimpleCSAddSubStepAction();
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
	}
	
	/**
	 * 
	 */
	private void initialize() {
		// TODO: MP: Check if model is null
		fModel = (ISimpleCSModel)getPage().getModel();
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
		if (cheatsheet != null) {
			// Select the cheatsheet node in the tree
			fTreeViewer.setSelection(new StructuredSelection(cheatsheet), true);
			fTreeViewer.expandToLevel(2);
		}
		
	}

	/**
	 * @param container
	 * @param toolkit
	 */
	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fTreeViewer = treePart.getTreeViewer();
		// TODO: MP: Complete content provider
		fTreeViewer.setContentProvider(new SimpleCSContentProvider());
		fTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		// TODO: MP: Future drag and drop
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
		// TODO: MP: What to do here?
		//getPage().getManagedForm().fireSelectionChanged(this, selection);
		//getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}	

	/**
	 * 
	 */
	private void updateButtons() {
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
				// TODO: MP: Have to make sure it is not the last item
				canRemove = true;
				canAddSubItem = true;
			} else if (csObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem)csObject;
				ISimpleCSObject parent = subitem.getParent();
				// TODO: MP: Handle for conditional-subitems later
				// Actually probably beter to use some interface method if 
				// possible
				if (parent.getType() == ISimpleCSConstants.TYPE_ITEM) {
					ISimpleCSItem item = (ISimpleCSItem)parent;
					if (item.isFirstSubItem(subitem) == false) {
						canMoveUp = true;
					}
					if (item.isLastSubItem(subitem) == false) {
						canMoveDown = true;
					}
				}
			
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
		if (object != null) {
			if (object instanceof ISimpleCSItem) {
				fAddSubStepAction.setObject((ISimpleCSItem)object);
				fAddSubStepAction.run();
			}
		}	
		
	}	
	
	/**
	 * 
	 */
	private void handleMoveStepAction(int index) {
		ISelection sel = fTreeViewer.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		// TODO: MP: Refactor candidate
		// i.e. calculating the index for up and down or separate method
		// TODO: MP: There is a flicker when adding and removing items / subitems
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
				// TODO: MP: Handle for conditional-subitems later
				// Actually probably beter to use some interface method if 
				// possible
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
		// TODO: MP: If the file needs saving, save to a temporary file and give
		// that as the input. Need to figure out which temporary directory to 
		// use
		// Launch in the cheat sheet view
		IFileEditorInput input = (IFileEditorInput)getPage().getEditorInput();
		IFile file  = input.getFile();
		try {
			// TODO: MP: Determine unique ID and Name
			OpenCheatSheetAction openAction = new OpenCheatSheetAction("ID", //$NON-NLS-1$
					"NAME", file.getRawLocationURI().toURL()); //$NON-NLS-1$
			openAction.run();
		} catch (MalformedURLException e) {
			PDEPlugin.logException(e);
		}		
	}
	
	/**
	 * 
	 */
	public void handleModelChanged(IModelChangedEvent event) {
		// TODO: MP: Not sure when this happens - untested
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			ISimpleCSObject object = (ISimpleCSObject)objects[i];
			
			if (object.getType() == ISimpleCSConstants.TYPE_ITEM) {
				if (event.getChangeType() == IModelChangedEvent.INSERT) {
					// Refresh the parent element in the tree viewer
					fTreeViewer.refresh(object.getParent());
					// Select the new item in the tree
					fTreeViewer.setSelection(new StructuredSelection(object), true);
				} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
					// Remove the item
					fTreeViewer.remove(object);
					// Select the parent in the tree
					// TODO: MP: Think about making the sibling item selected instead
					fTreeViewer.setSelection(new StructuredSelection(object.getParent()), true);
				}
			} else if (object.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
				// TODO: MP: Can probably merge with Item above
				if (event.getChangeType() == IModelChangedEvent.INSERT) {
					// Refresh the parent element in the tree viewer
					fTreeViewer.refresh(object.getParent());
					// Select the new sub-item in the tree
					fTreeViewer.setSelection(new StructuredSelection(object), true);
				} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
					// Remove the subitem
					fTreeViewer.remove(object);
					// Select the parent in the tree
					// TODO: MP: Think about making the sibling subitem selected instead
					fTreeViewer.setSelection(new StructuredSelection(object.getParent()), true);
				}
			}

		}
	}
	
	/**
	 * Special case:  Need to set the selection after the full UI is created
	 * in order to properly fire an event to summon up the right details 
	 * section
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
		// TODO: MP: Verify can cast null ...
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
			// TODO: MP: Prevent last item from being deleted
		} else if (csObject.getType() == ISimpleCSConstants.TYPE_ITEM) {
			// Add to the "New" submenu
			// Add sub-step action
			fAddSubStepAction.setObject(csObject);
			fAddSubStepAction.setEnabled(fModel.isEditable());
			submenu.add(fAddSubStepAction);
			// Add to the main context menu
			// Add a separator to the main context menu
			manager.add(new Separator());
			// Delete step action
			fRemoveStepAction.setItem((ISimpleCSItem)csObject);
			fRemoveStepAction.setEnabled(fModel.isEditable());
			manager.add(fRemoveStepAction);
		} else if (csObject.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			// Add to the main context menu
			// Add a separator to the main context menu
			manager.add(new Separator());
			// Delete sub-step action
			fRemoveSubStepAction.setSubItem((ISimpleCSSubItem)csObject);
			fRemoveSubStepAction.setEnabled(fModel.isEditable());
			manager.add(fRemoveSubStepAction);			
		}
		// Add normal edit operations
		// TODO: MP: Enable
		//getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		//manager.add(new Separator());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		// TODO: MP: Do Cut
		// TODO: MP: Do Paste
		
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
				fRemoveStepAction.setItem((ISimpleCSItem)object);
				fRemoveStepAction.run();
			} else if (object instanceof ISimpleCSSubItem) {
				fRemoveSubStepAction.setSubItem((ISimpleCSSubItem)object);
				fRemoveSubStepAction.run();
			}
		}		
	}
	
}
