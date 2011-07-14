/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.ctxhelp.ICtxHelpConstants;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.*;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details.CtxHelpAbstractAddAction;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details.CtxHelpRemoveAction;
import org.eclipse.pde.internal.ua.ui.editor.toc.HelpEditorUtil;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.editor.plugin.FormFilteredTree;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.keys.IBindingService;

/**
 * Tree section for the context help editor.  Displays the structure
 * of the xml file and adds actions allowing manipulation of the structure.
 * @since 3.4
 * @see CtxHelpEditor
 */
public class CtxHelpTreeSection extends TreeSection {
	private CtxHelpModel fModel;
	private TreeViewer fTree;
	private FormFilteredTree fFilteredTree;

	/* The indices for each button attached to the Tree Viewer.
	 * This type of UI form does not permit direct access to each particular
	 * button. However, using these indices, one can perform any typical SWT
	 * operation on any button.
	 */
	private static final int F_BUTTON_ADD_CONTEXT = 0;
	private static final int F_BUTTON_ADD_TOPIC = 2;
	private static final int F_BUTTON_ADD_COMMAND = 3;
	private static final int F_BUTTON_REMOVE = 5;
	private static final int F_BUTTON_UP = 6;
	private static final int F_BUTTON_DOWN = 7;

	// When one of the move buttons is pressed, a flag is used to determine the direction
	private static final int F_UP_FLAG = -1;
	private static final int F_DOWN_FLAG = 1;

	// The actions that will add each type of object
	private CtxHelpAbstractAddAction fAddContextAction;
	private CtxHelpAbstractAddAction fAddTopicAction;
	private CtxHelpAbstractAddAction fAddCommandAction;

	// The object removal action
	private CtxHelpRemoveAction fRemoveObjectAction;

	// The action for opening a link from the context menu
	private OpenLinkAction fOpenLinkAction;

	// Used to temporarily store the target of a drop operation 
	// so that it does not have be be recalculated
	private CtxHelpObject fDropTargetParent;
	private CtxHelpObject fDropTargetSibling;

	/** If items are dragged and dropped within this tree, then
	 * this flag inhibits reselection on the removal (drag) action,
	 * thus ensuring that the selected objects are the ones that were
	 * dropped.
	 */
	private boolean fDragFromHere;

	/**
	 * Action that allows a linked file to be opened in the editor
	 * @since 3.4
	 */
	class OpenLinkAction extends Action {
		private CtxHelpTopic fOpenTarget;

		public OpenLinkAction() {
			super(CtxHelpMessages.CtxHelpTreeSection_0);
		}

		public void setTarget(CtxHelpTopic target) {
			fOpenTarget = target;
		}

		public void run() {
			if (fOpenTarget != null) {
				open(fOpenTarget);
			}
		}
	}

	public CtxHelpTreeSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[] {CtxHelpMessages.CtxHelpTreeSection_1, null, CtxHelpMessages.CtxHelpTreeSection_2, CtxHelpMessages.CtxHelpTreeSection_3, null, CtxHelpMessages.CtxHelpTreeSection_4, CtxHelpMessages.CtxHelpTreeSection_5, CtxHelpMessages.CtxHelpTreeSection_6});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		fModel = (CtxHelpModel) getPage().getModel();

		Composite container = createClientContainer(section, 2, toolkit);
		createTree(container, toolkit);
		toolkit.paintBordersFor(container);

		section.setText(CtxHelpMessages.CtxHelpTreeSection_7);
		section.setDescription(CtxHelpMessages.CtxHelpTreeSection_8);
		section.setClient(container);

		createCommands();

		initializeTreeViewer();
		createSectionToolbar(section, toolkit);

		// Create the adapted listener for the filter entry field
		fFilteredTree.createUIListenerEntryFilter(this);
	}

	/**
	 * Creates the commands used in this section.
	 */
	private void createCommands() {
		fAddContextAction = new CtxHelpAbstractAddAction() {
			public void run() {
				if (fParentObject != null) {
					CtxHelpContext context = fParentObject.getModel().getFactory().createContext();
					String id = PDELabelUtility.generateName(getChildNames(), CtxHelpMessages.CtxHelpTreeSection_9);
					context.setID(id);
					addChild(context);
				}
			}
		};
		fAddContextAction.setText(CtxHelpMessages.CtxHelpTreeSection_10);
		fAddTopicAction = new CtxHelpAbstractAddAction() {
			public void run() {
				if (fParentObject != null) {
					CtxHelpTopic topic = fParentObject.getModel().getFactory().createTopic();
					String label = PDELabelUtility.generateName(getChildNames(), CtxHelpMessages.CtxHelpTreeSection_11);
					topic.setLabel(label);
					addChild(topic);
				}
			}
		};
		fAddTopicAction.setText(CtxHelpMessages.CtxHelpTreeSection_12);
		fAddCommandAction = new CtxHelpAbstractAddAction() {
			public void run() {
				if (fParentObject != null) {
					CtxHelpCommand command = fParentObject.getModel().getFactory().createCommand();
					String label = PDELabelUtility.generateName(getChildNames(), CtxHelpMessages.CtxHelpTreeSection_13);
					command.setLabel(label);
					addChild(command);
				}
			}
		};
		fAddCommandAction.setText(CtxHelpMessages.CtxHelpTreeSection_14);

		fRemoveObjectAction = new CtxHelpRemoveAction();
		fOpenLinkAction = new OpenLinkAction();
	}

	/**
	 * Adds a link (with hand cursor) for tree 'Collapse All' action,
	 * which collapses the tree down to the second level
	 * 
	 * @param section The section that the toolbar will belong to
	 * @param toolkit The toolkit that will be used to make the toolbar
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
		toolBarManager.add(new CollapseAction(fTree, CtxHelpMessages.CtxHelpTreeSection_15, 1, fModel.getCtxHelpRoot()));
		toolBarManager.update(true);
		section.setTextClient(toolbar);
	}

	/**
	 * Create the tree widget that will contain the structure
	 * 
	 * @param container The container of the tree widget
	 * @param toolkit The toolkit used to create the tree
	 */
	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);

		fTree = treePart.getTreeViewer();
		fTree.setContentProvider(new CtxHelpContentProvider());
		fTree.setLabelProvider(PDEUserAssistanceUIPlugin.getDefault().getLabelProvider());

		PDEUserAssistanceUIPlugin.getDefault().getLabelProvider().connect(this);

		// Create listener for the outline view 'link with editor' toggle button
		fTree.addPostSelectionChangedListener(getPage().getPDEEditor().new PDEFormEditorChangeListener());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		fFilteredTree = new FormFilteredTree(parent, style, new PatternFilter());
		parent.setData("filtered", Boolean.TRUE); //$NON-NLS-1$
		return fFilteredTree.getViewer();
	}

	/**
	 * Initialize the tree viewer widget and its buttons.
	 */
	private void initializeTreeViewer() {
		if (fModel == null) {
			return;
		}

		CtxHelpRoot root = fModel.getCtxHelpRoot();
		fTree.setInput(root);

		// Buttons must be disabled if file is not editable
		getTreePart().setButtonEnabled(F_BUTTON_ADD_CONTEXT, isEditable());
		getTreePart().setButtonEnabled(F_BUTTON_ADD_COMMAND, isEditable());
		getTreePart().setButtonEnabled(F_BUTTON_ADD_TOPIC, isEditable());

		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, false);
		getTreePart().setButtonEnabled(F_BUTTON_UP, false);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFormInput(java.lang.Object)
	 */
	public boolean setFormInput(Object object) {
		// This method allows the outline view to select items in the tree
		// (Invoked by org.eclipse.ui.forms.editor.IFormPage.selectReveal(Object object))
		if (object instanceof CtxHelpObject) {
			fTree.setSelection(new StructuredSelection(object), true);

			// Verify that something was actually selected
			ISelection selection = fTree.getSelection();
			if (selection != null && !selection.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the selection of the tree
	 */
	public ISelection getSelection() {
		return fTree.getSelection();
	}

	/**
	 * @param selection the new selection for the tree section
	 */
	public void setSelection(ISelection selection) {
		fTree.setSelection(selection);
	}

	/**
	 * Fire a selection change event and refresh the viewer's selection
	 */
	public void fireSelection() {
		fTree.setSelection(fTree.getSelection());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateButtons();
	}

	/**
	 * Update the buttons in the section based on the current selection
	 */
	public void updateButtons() {
		if (!fModel.isEditable()) {
			return;
		}

		IStructuredSelection selection = (IStructuredSelection) fTree.getSelection();
		CtxHelpObject firstSelectedObject = (CtxHelpObject) selection.getFirstElement();

		// Add Context
		getTreePart().setButtonEnabled(F_BUTTON_ADD_CONTEXT, true);

		// Add Topic
		boolean enableAdd = false;
		if (firstSelectedObject != null) {
			if (firstSelectedObject.canAddSibling(ICtxHelpConstants.TYPE_TOPIC)) {
				enableAdd = true;
			} else if (firstSelectedObject.canAddChild(ICtxHelpConstants.TYPE_TOPIC)) {
				enableAdd = true;
			}
		}
		getTreePart().setButtonEnabled(F_BUTTON_ADD_TOPIC, enableAdd);

		// Add Command
		enableAdd = false;
		if (firstSelectedObject != null) {
			if (firstSelectedObject.canAddSibling(ICtxHelpConstants.TYPE_COMMAND)) {
				enableAdd = true;
			} else if (firstSelectedObject.canAddChild(ICtxHelpConstants.TYPE_COMMAND)) {
				enableAdd = true;
			}
		}
		getTreePart().setButtonEnabled(F_BUTTON_ADD_COMMAND, enableAdd);

		// Remove button
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, getRemovableObjectFromSelection(selection).size() > 0);

		// Up and Down buttons
		boolean canMoveUp = true;
		boolean canMoveDown = true;
		if (firstSelectedObject == null || firstSelectedObject.getType() == ICtxHelpConstants.TYPE_ROOT || firstSelectedObject.getType() == ICtxHelpConstants.TYPE_DESCRIPTION || selection.size() > 1) {
			canMoveUp = false;
			canMoveDown = false;
		} else {
			CtxHelpObject parent = firstSelectedObject.getParent();
			if (parent != null) {
				int index = parent.indexOf(firstSelectedObject);
				if (index == 0) {
					canMoveUp = false;
				}
				if (index >= parent.getChildCount() - 1) {
					canMoveDown = false;
				}
			}
		}
		getTreePart().setButtonEnabled(F_BUTTON_UP, canMoveUp);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, canMoveDown);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) fTree.getSelection();
		Object object = selection.getFirstElement();
		// Has to be null or a CtxHelpObject object
		CtxHelpObject firstSelectedObject = (CtxHelpObject) object;

		// Populate the "New" sub-menu
		if (firstSelectedObject != null) {
			MenuManager submenu = new MenuManager(CtxHelpMessages.CtxHelpTreeSection_16);
			boolean addMenu = false;
			if (updateAddContextActionWithSelection(firstSelectedObject)) {
				submenu.add(fAddContextAction);
				addMenu = true;
			}
			if (updateAddTopicActionWithSelection(firstSelectedObject)) {
				submenu.add(fAddTopicAction);
				addMenu = true;
			}
			if (updateAddCommandActionWithSelection(firstSelectedObject)) {
				submenu.add(fAddCommandAction);
				addMenu = true;
			}
			if (addMenu) {
				manager.add(submenu);
				manager.add(new Separator());
			}
		}

		// Add the open link and show in actions
		if (firstSelectedObject instanceof CtxHelpTopic && ((CtxHelpTopic) firstSelectedObject).getLocation() != null) {
			fOpenLinkAction.setTarget((CtxHelpTopic) firstSelectedObject);
			manager.add(fOpenLinkAction);
			fillContextMenuShowInAction(manager);
			manager.add(new Separator());
		}

		// Add clipboard actions
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
		manager.add(new Separator());

		// Add remove action
		if (updateRemoveActionWithSelection(selection)) {
			manager.add(fRemoveObjectAction);
			manager.add(new Separator());
		}

	}

	/**
	 * Creates and a new submenu in the given menu manager and adds actions to
	 * allow a linked file to be opened in various views.
	 * @param manager menu manager to add the submenu to
	 */
	private void fillContextMenuShowInAction(IMenuManager manager) {
		String showInLabel = CtxHelpMessages.CtxHelpTreeSection_17;

		// Add a label for the keybinding for Show In action, if one exists
		IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService != null) {
			String keyBinding = bindingService.getBestActiveBindingFormattedFor("org.eclipse.ui.navigate.showInQuickMenu"); //$NON-NLS-1$
			if (keyBinding != null) {
				showInLabel += '\t' + keyBinding;
			}
		}

		IMenuManager showInMenu = new MenuManager(showInLabel);
		showInMenu.add(ContributionItemFactory.VIEWS_SHOW_IN.create(getPage().getSite().getWorkbenchWindow()));

		manager.add(showInMenu);
	}

	/**
	 * Updates the add context action if the action should be available for the selection.
	 * Updates enablement, parent object and target object.  Returns true if the action
	 * should be available to the selection. 
	 * @param selectedObject selected object
	 * @return true if the action should be available for the current selection, false otherwise
	 */
	private boolean updateAddContextActionWithSelection(CtxHelpObject selectedObject) {
		if (selectedObject != null && selectedObject.canAddSibling(ICtxHelpConstants.TYPE_CONTEXT)) {
			fAddContextAction.setParentObject(selectedObject.getParent());
			fAddContextAction.setTargetObject(selectedObject);
			fAddContextAction.setEnabled(fModel.isEditable());
			return true;
		} else if (selectedObject != null && selectedObject.canAddChild(ICtxHelpConstants.TYPE_CONTEXT)) {
			fAddContextAction.setParentObject(selectedObject);
			fAddContextAction.setTargetObject(null);
			fAddContextAction.setEnabled(fModel.isEditable());
			return true;
		} else if (fModel.getCtxHelpRoot().canAddChild(ICtxHelpConstants.TYPE_CONTEXT)) {
			fAddContextAction.setParentObject(fModel.getCtxHelpRoot());
			fAddContextAction.setTargetObject(null);
			fAddContextAction.setEnabled(fModel.isEditable());
			return true;
		}
		return false;
	}

	/**
	 * Updates the add topic action if the action should be available for the selection.
	 * Updates enablement, parent object and target object.  Returns true if the action
	 * should be available to the selection. 
	 * @param selectedObject selected object
	 * @return true if the action should be available for the current selection, false otherwise
	 */
	private boolean updateAddTopicActionWithSelection(CtxHelpObject selectedObject) {
		if (selectedObject != null) {
			if (selectedObject.canAddSibling(ICtxHelpConstants.TYPE_TOPIC)) {
				fAddTopicAction.setParentObject(selectedObject.getParent());
				fAddTopicAction.setTargetObject(selectedObject);
				fAddTopicAction.setEnabled(fModel.isEditable());
				return true;
			} else if (selectedObject.canAddChild(ICtxHelpConstants.TYPE_TOPIC)) {
				fAddTopicAction.setParentObject(selectedObject);
				fAddTopicAction.setTargetObject(null);
				fAddTopicAction.setEnabled(fModel.isEditable());
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the add topic action if the action should be available for the selection.
	 * Updates enablement, parent object and target object.  Returns true if the action
	 * should be available to the selection. 
	 * @param selectedObject selected object
	 * @return true if the action should be available for the current selection, false otherwise
	 */
	private boolean updateAddCommandActionWithSelection(CtxHelpObject selectedObject) {
		if (selectedObject != null) {
			if (selectedObject.canAddSibling(ICtxHelpConstants.TYPE_COMMAND)) {
				fAddCommandAction.setParentObject(selectedObject.getParent());
				fAddCommandAction.setTargetObject(selectedObject);
				fAddCommandAction.setEnabled(fModel.isEditable());
				return true;
			} else if (selectedObject.canAddChild(ICtxHelpConstants.TYPE_COMMAND)) {
				fAddCommandAction.setParentObject(selectedObject);
				fAddCommandAction.setTargetObject(null);
				fAddCommandAction.setEnabled(fModel.isEditable());
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates the remove action if the action should be available for the selection.
	 * Updates enablement, parent object and target object.  Returns true if the action
	 * should be available to the selection. 
	 * @param selectedObject selected object
	 * @return true if the action should be available for the current selection, false otherwise
	 */
	private boolean updateRemoveActionWithSelection(IStructuredSelection selection) {
		List objectsToRemove = getRemovableObjectFromSelection(selection);
		fRemoveObjectAction.setToRemove((CtxHelpObject[]) objectsToRemove.toArray(new CtxHelpObject[objectsToRemove.size()]));
		fRemoveObjectAction.setEnabled(fModel.isEditable());
		return objectsToRemove.size() > 0;
	}

	/**
	 * Returns a list of objects that is the subset of objects in the selection that
	 * can be removed.
	 * @param selection the selection
	 * @return list of {@link CtxHelpObject}s that can be removed, possibly empty.
	 */
	private List getRemovableObjectFromSelection(IStructuredSelection selection) {
		List objectsToRemove = new ArrayList();
		for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
			Object currentObject = iterator.next();
			if (currentObject instanceof CtxHelpObject && ((CtxHelpObject) currentObject).canBeRemoved()) {
				objectsToRemove.add(currentObject);
			}
		}
		return objectsToRemove;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		boolean cutAction = actionId.equals(ActionFactory.CUT.getId());

		if (cutAction || actionId.equals(ActionFactory.DELETE.getId())) {
			updateRemoveActionWithSelection((IStructuredSelection) fTree.getSelection());
			fRemoveObjectAction.run();
			return !cutAction;
		}

		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		return canDropCopy(targetObject, sourceObjects, ViewerDropAdapter.LOCATION_ON);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		doDropCopy(targetObject, sourceObjects, ViewerDropAdapter.LOCATION_ON);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#handleDoubleClick(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void handleDoubleClick(IStructuredSelection selection) {
		Object selected = selection.getFirstElement();
		if (selected instanceof CtxHelpTopic) {
			open((CtxHelpTopic) selected);
		} else if (selected instanceof CtxHelpObject) {
			fTree.setExpandedState(selected, !fTree.getExpandedState(selected));
		}
	}

	/**
	 * Opens the file that is linked in the given topic.
	 * @param topic the topic containing a link to a file
	 */
	public void open(CtxHelpTopic topic) {
		IPath resourcePath = topic.getLocation();
		if (!isEditable() || resourcePath == null || resourcePath.isEmpty()) {
			MessageDialog.openWarning(PDEUserAssistanceUIPlugin.getActiveWorkbenchShell(), CtxHelpMessages.CtxHelpTreeSection_18, CtxHelpMessages.CtxHelpTreeSection_19);
			return;
		}

		IResource resource = fModel.getUnderlyingResource().getProject().findMember(resourcePath);
		if (resource != null && resource instanceof IFile) {
			IPath path = resource.getFullPath();
			if (HelpEditorUtil.hasValidPageExtension(path)) {
				try {
					IDE.openEditor(PDEUserAssistanceUIPlugin.getActivePage(), (IFile) resource, true);
				} catch (PartInitException e) { //suppress exception
				}
			} else {
				MessageDialog.openWarning(PDEUserAssistanceUIPlugin.getActiveWorkbenchShell(), CtxHelpMessages.CtxHelpTreeSection_20, CtxHelpMessages.CtxHelpTreeSection_21);
			}
		} else {
			MessageDialog.openWarning(PDEUserAssistanceUIPlugin.getActiveWorkbenchShell(), CtxHelpMessages.CtxHelpTreeSection_22, CtxHelpMessages.CtxHelpTreeSection_23);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		IStructuredSelection selection = (IStructuredSelection) fTree.getSelection();
		Object object = selection.getFirstElement();
		CtxHelpObject firstSelectedObject = (CtxHelpObject) object;
		switch (index) {
			case F_BUTTON_ADD_CONTEXT :
				updateAddContextActionWithSelection(firstSelectedObject);
				fAddContextAction.run();
				break;
			case F_BUTTON_ADD_TOPIC :
				updateAddTopicActionWithSelection(firstSelectedObject);
				fAddTopicAction.run();
				break;
			case F_BUTTON_ADD_COMMAND :
				updateAddCommandActionWithSelection(firstSelectedObject);
				fAddCommandAction.run();
				break;
			case F_BUTTON_REMOVE :
				updateRemoveActionWithSelection(selection);
				fRemoveObjectAction.run();
				break;
			case F_BUTTON_UP :
				handleMoveAction(F_UP_FLAG);
				break;
			case F_BUTTON_DOWN :
				handleMoveAction(F_DOWN_FLAG);
				break;
		}
	}

	/**
	 * Move an object within the structure.
	 * 
	 * @param positionFlag The direction that the object will move, either F_UP_FLAG or F_DOWN_FLAG
	 */
	private void handleMoveAction(int positionFlag) {
		IStructuredSelection sel = (IStructuredSelection) fTree.getSelection();

		Object object = sel.getFirstElement();
		if (object == null) {
			return;
		} else if (object instanceof CtxHelpObject) {
			CtxHelpObject ctxHelpObject = (CtxHelpObject) object;
			CtxHelpObject parent = ctxHelpObject.getParent();
			if (parent != null) {
				parent.moveChild(ctxHelpObject, positionFlag);
				fTree.setSelection(new StructuredSelection(ctxHelpObject), true);
			}
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		PDEUserAssistanceUIPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#isDragAndDropEnabled()
	 */
	protected boolean isDragAndDropEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#getSupportedDNDOperations()
	 */
	public int getSupportedDNDOperations() {
		return DND.DROP_MOVE | DND.DROP_COPY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDropCopy(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropCopy(Object targetObject, Object[] sourceObjects, int targetLocation) {
		if (fDropTargetParent != null) {
			if (fDropTargetSibling != null) {
				if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
					for (int i = 0; i < sourceObjects.length; i++) {
						((CtxHelpObject) sourceObjects[i]).reconnect(fDropTargetParent, fModel);
						fDropTargetParent.addChild((CtxHelpObject) sourceObjects[i], fDropTargetSibling, true);
					}
				} else {
					for (int i = sourceObjects.length - 1; i >= 0; i--) {
						((CtxHelpObject) sourceObjects[i]).reconnect(fDropTargetParent, fModel);
						fDropTargetParent.addChild((CtxHelpObject) sourceObjects[i], fDropTargetSibling, false);
					}
				}
			} else {
				for (int i = 0; i < sourceObjects.length; i++) {
					((CtxHelpObject) sourceObjects[i]).reconnect(fDropTargetParent, fModel);
					fDropTargetParent.addChild((CtxHelpObject) sourceObjects[i]);
				}
			}
		}
		fDropTargetParent = null;
		fDropTargetSibling = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		doDropCopy(targetObject, sourceObjects, targetLocation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDropCopy(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropCopy(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Add as a child of the root
		if (targetObject == null || !(targetObject instanceof CtxHelpObject || ((CtxHelpObject) targetObject).getType() == ICtxHelpConstants.TYPE_ROOT)) {
			for (int i = 0; i < sourceObjects.length; i++) {
				if (!(sourceObjects[i] instanceof CtxHelpObject) || !fModel.getCtxHelpRoot().canAddChild(((CtxHelpObject) sourceObjects[i]).getType())) {
					return false;
				}
			}
			fDropTargetParent = fModel.getCtxHelpRoot();
			fDropTargetSibling = null;
			return true;
		}

		CtxHelpObject dropTarget = (CtxHelpObject) targetObject;

		// Add as a child of the target
		if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			boolean result = true;
			for (int i = 0; i < sourceObjects.length; i++) {
				if (!(sourceObjects[i] instanceof CtxHelpObject) || !dropTarget.canAddChild(((CtxHelpObject) sourceObjects[i]).getType())) {
					result = false;
					break;
				}
			}
			// If adding as a child works, do so, otherwise try as a sibling
			if (result) {
				fDropTargetParent = dropTarget;
				fDropTargetSibling = null;
				return true;
			}
		}

		// Add as a sibling of the target
		for (int i = 0; i < sourceObjects.length; i++) {
			if (!(sourceObjects[i] instanceof CtxHelpObject) || !dropTarget.canAddSibling(((CtxHelpObject) sourceObjects[i]).getType())) {
				return false;
			}
		}
		fDropTargetParent = dropTarget.getParent();
		fDropTargetSibling = dropTarget;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Same as drop copy operation
		return canDropCopy(targetObject, sourceObjects, targetLocation);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDragCopy(java.lang.Object[])
	 */
	public boolean canDragCopy(Object[] sourceObjects) {
		// Allow anything to be drag copied
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canDragMove(java.lang.Object[])
	 */
	public boolean canDragMove(Object[] sourceObjects) {
		for (int i = 0; i < sourceObjects.length; i++) {
			if (!(sourceObjects[i] instanceof CtxHelpObject) || !((CtxHelpObject) sourceObjects[i]).canBeRemoved()) {
				return false;
			}
		}
		fDragFromHere = true;
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doDragRemove(java.lang.Object[])
	 */
	public void doDragRemove(Object[] sourceObjects) {
		updateRemoveActionWithSelection(new StructuredSelection(sourceObjects));
		fRemoveObjectAction.run();
		fDragFromHere = false;
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

	/**
	 * Handles the swap event
	 * @param event the swap event
	 */
	private void handleModelChangeTypeSwap(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		CtxHelpObject object = (CtxHelpObject) objects[0];

		if (object != null) {
			fTree.refresh(object);
		}
	}

	/**
	 * The model is stale, refresh the UI
	 * @param event The world-change event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		markStale();
	}

	/**
	 * Handle insertions in the model
	 * @param event the insertion event
	 */
	private void handleModelInsertType(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof CtxHelpObject) {
				CtxHelpObject object = (CtxHelpObject) objects[i];
				if (object.getType() != ICtxHelpConstants.TYPE_ROOT) {
					fTree.refresh(object.getParent());
					// Select the new object in the tree, unless it is a description node
					if (!(object instanceof CtxHelpDescription)) {
						fTree.setSelection(new StructuredSelection(object), true);
					}
				}
			}
		}
	}

	/**
	 * Handle removals in the model
	 * @param event the removal event
	 */
	private void handleModelRemoveType(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof CtxHelpObject) {
				CtxHelpObject object = (CtxHelpObject) objects[i];
				fTree.remove(object);
				CtxHelpObject nextSelection = fRemoveObjectAction.getNextSelection();
				if (nextSelection != null) {
					fTree.refresh(object.getParent());
					if (!fDragFromHere) {
						fTree.setSelection(new StructuredSelection(nextSelection), true);
					}
					fRemoveObjectAction.clearNextSelection();
				}
			}
		}
	}

	/**
	 * Handle an update to an object's properties
	 * @param event the update event
	 */
	private void handleModelChangeType(IModelChangedEvent event) {
		Object[] objects = event.getChangedObjects();
		if (objects[0] != null) {
			fTree.update(objects[0], null);
		}
	}

}
