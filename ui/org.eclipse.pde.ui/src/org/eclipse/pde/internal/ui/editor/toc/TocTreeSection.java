/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.toc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.toc.Toc;
import org.eclipse.pde.internal.core.text.toc.TocLink;
import org.eclipse.pde.internal.core.text.toc.TocModel;
import org.eclipse.pde.internal.core.text.toc.TocObject;
import org.eclipse.pde.internal.core.text.toc.TocTopic;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.ModelDataTransfer;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.editor.plugin.FormFilteredTree;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocAddAnchorAction;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocAddLinkAction;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocAddObjectAction;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocAddTopicAction;
import org.eclipse.pde.internal.ui.editor.toc.actions.TocRemoveObjectAction;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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
 * TocTreeSection - The section that displays the TOC
 * tree structure and any buttons used to manipulate it.
 * This is the main section that the user will interact
 * with the TOC through.
 */
public class TocTreeSection extends TreeSection {
	private TocModel fModel;
	private TreeViewer fTocTree;
	private FormFilteredTree fFilteredTree;

	/* The indices for each button attached to the Tree Viewer.
	 * This type of UI form does not permit direct access to each particular
	 * button. However, using these indices, one can perform any typical SWT
	 * operation on any button.
	 */
	private static final int F_BUTTON_ADD_TOPIC = 0;
	private static final int F_BUTTON_ADD_LINK = 3;
	private static final int F_BUTTON_ADD_ANCHOR = 4;
	private static final int F_BUTTON_REMOVE = 5;
	private static final int F_BUTTON_UP = 6;
	private static final int F_BUTTON_DOWN = 7;
	private static final int F_UP_FLAG = -1;
	private static final int F_DOWN_FLAG = 1;

	// The action that collapses down the TOC tree
	private CollapseAction fCollapseAction;

	// The actions that will add each type of TOC object
	private TocAddTopicAction fAddTopicAction;
	private TocAddLinkAction fAddLinkAction;
	private TocAddAnchorAction fAddAnchorAction;
	
	// The object removal action
	private TocRemoveObjectAction fRemoveObjectAction;

	// The adapter that will listen for drag events in the tree
	private TocDragAdapter fDragAdapter;

	/* If items are dragged and dropped within this tree, then
	 * this flag inhibits reselection on the removal (drag) action,
	 * thus ensuring that the selected objects are the ones that were
	 * dropped.
	 */
	private boolean fDragFromHere;

	/**
	 * Constructs a new TOC tree section.
	 * 
	 * @param formPage The page that will hold this new tree section
	 * @param parent The parent composite in the page that will contain the section widgets
	 */
	public TocTreeSection(PDEFormPage formPage, Composite parent) {

		/* Create a new section with a description area, and some buttons.
		 * The null entries in the String array will become blank space 
		 * separators between the buttons.
		 */
		super(formPage, parent, Section.DESCRIPTION, 
			new String[] { PDEUIMessages.TocPage_addTopic,
				null,
				null,
				PDEUIMessages.TocPage_addLink,
				PDEUIMessages.TocPage_addAnchor,
				PDEUIMessages.TocPage_remove,
				PDEUIMessages.TocPage_up,
				PDEUIMessages.TocPage_down } );

		// Initialize all the actions
		fAddTopicAction = new TocAddTopicAction();
		fAddLinkAction = new TocAddLinkAction();
		fAddAnchorAction = new TocAddAnchorAction();
		fRemoveObjectAction = new TocRemoveObjectAction();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		// Get the model
		fModel = (TocModel)getPage().getModel();		

		// Create a container in the section
		Composite container = createClientContainer(section, 2, toolkit);
		// Create a TOC tree in the new container
		createTree(container, toolkit);
		toolkit.paintBordersFor(container);
		section.setText(PDEUIMessages.TocTreeSection_title);
		section.setDescription(PDEUIMessages.TocTreeSection_sectionDescription);
		section.setClient(container);
		
		initializeTreeViewer();
		createSectionToolbar(section, toolkit);

		// Create the adapted listener for the filter entry field
		fFilteredTree.createUIListenerEntryFilter(this);
	}

	/**
	 * Adds a link (with hand cursor) for tree 'Collapse All' action,
	 * which collapses the TOC tree down to the second level
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
				if ((handCursor != null) &&
						(handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});

		// Add collapse action to the tool bar
		fCollapseAction = new CollapseAction(fTocTree, 
				PDEUIMessages.ExtensionsPage_collapseAll, 
				1, 
				fModel.getToc());
		toolBarManager.add(fCollapseAction);

		toolBarManager.update(true);
		section.setTextClient(toolbar);
	}

	/**
	 * Create the tree widget that will contain the TOC
	 * 
	 * @param container The container of the tree widget
	 * @param toolkit The toolkit used to create the tree
	 */
	private void createTree(Composite container, FormToolkit toolkit) {
		TreePart treePart = getTreePart();
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);

		fTocTree = treePart.getTreeViewer();
		fTocTree.setContentProvider(new TocContentProvider());
		fTocTree.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		PDEPlugin.getDefault().getLabelProvider().connect(this);
		
		createTreeListeners();
		initDragAndDrop();
	}

	/**
	 * Initialize the section's drag and drop capabilities
	 */
	private void initDragAndDrop()
	{	int ops = DND.DROP_COPY;
		if(isEditable())
		{	ops |= DND.DROP_MOVE;
		}

		//Content dragged from the tree viewer can be treated as model objects (TocObjects)
		//or as text (XML representation of the TocObjects)
		Transfer[] dragTransfers = new Transfer[] { ModelDataTransfer.getInstance(), TextTransfer.getInstance() };
		fDragAdapter = new TocDragAdapter(this);
		fTocTree.addDragSupport(ops, dragTransfers, fDragAdapter);

		if(isEditable())
		{	//Model objects and files can be dropped onto the viewer
			//TODO: Consider allowing drops/pastes of pure XML text
			Transfer[] dropTransfers = new Transfer[] { ModelDataTransfer.getInstance(), FileTransfer.getInstance() };
			fTocTree.addDropSupport(ops | DND.DROP_DEFAULT, dropTransfers, new TocDropAdapter(fTocTree, this));
		}
	}

	/**
	 * Create the action listeners for the tree.
	 */
	private void createTreeListeners() {
		// Create listener for the outline view 'link with editor' toggle button
		fTocTree.addPostSelectionChangedListener(
				getPage().getPDEEditor().new PDEFormEditorChangeListener());
	}

	
	/**
	 * Initialize the tree viewer widget and its buttons.
	 */
	private void initializeTreeViewer()
	{	if (fModel == null)
		{	return;
		}

		// Connect the tree viewer to the TOC model
		fTocTree.setInput(fModel);
		Toc toc = fModel.getToc();

		// Nodes can always be added to the root TOC node
		getTreePart().setButtonEnabled(F_BUTTON_ADD_TOPIC, isEditable());
		getTreePart().setButtonEnabled(F_BUTTON_ADD_ANCHOR, isEditable());
		getTreePart().setButtonEnabled(F_BUTTON_ADD_LINK, isEditable());

		// Set to false because initial node selected is the root TOC node
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, false);
		// Set to false because initial node selected is the root TOC node
		getTreePart().setButtonEnabled(F_BUTTON_UP, false);
		// Set to false because initial node selected is the root TOC node
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, false);

		//Initially, the root TOC element is selected
		fTocTree.setSelection(new StructuredSelection(toc), true);
		fTocTree.expandToLevel(2);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFormInput(java.lang.Object)
	 */
	public boolean setFormInput(Object object) {
		// This method allows the outline view to select items in the tree
		// (Invoked by org.eclipse.ui.forms.editor.IFormPage.selectReveal(Object object))

		if (object instanceof TocObject)
		{	// Select the item in the tree
			fTocTree.setSelection(new StructuredSelection(object), true);

			// Verify that something was actually selected
			ISelection selection = fTocTree.getSelection();
			if (selection != null && !selection.isEmpty()) 
			{	return true;
			}
		}

		return false;
	}		

	/**
	 * @return the selection of the tree section
	 */
	public ISelection getSelection() {
		return fTocTree.getSelection();
	}

	/**
	 * @param selection the new selection for the tree section
	 */
	public void setSelection(ISelection selection) {
		fTocTree.setSelection(selection);
	}

	/**
	 * Fire a selection change event and refresh the viewer's selection
	 */
	public void fireSelection() {
		fTocTree.setSelection(fTocTree.getSelection());
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
	public void updateButtons()
	{	if (!fModel.isEditable())
		{	return;
		}

		// 'Add' actions are enabled if any object in the selection can
		// be added to
		boolean canAddObject = false;
		// 'Remove' is enabled if any object in the selection is removable
		boolean canRemove = false;

		IStructuredSelection sel = (IStructuredSelection)fTocTree.getSelection();
		//TODO: Implement multi-select move actions from the root TOC element

		// 'Up' is disabled if any object in the selection can't be moved up.
		boolean canMoveUp = sel.size() == 1;

		// 'Down' is disabled if any object in the selection can't be moved down.
		boolean canMoveDown = sel.size() == 1;

		for(Iterator iter = sel.iterator(); iter.hasNext();)
		{	TocObject tocObject = (TocObject)iter.next();

			if (tocObject != null)
			{	if(tocObject.canBeRemoved())
				{	canRemove = true;
				}

				TocObject parent = tocObject.getParent();
				if (sel.size() == 1 &&
						(tocObject.getType() == ITocConstants.TYPE_TOC ||
						 parent.getType() == ITocConstants.TYPE_TOPIC ||
						 parent.getType() == ITocConstants.TYPE_TOC))
				{	/* Semantic rule: 
					 * As long as the selection is a child of a 
					 * TOC root or a topic, or the selection itself
					 * is a TOC root, then a new object can be added
					 * either to the selection or to the parent
					 */
					canAddObject = true;
				}
				
				//Semantic rule:
				//You cannot rearrange the TOC root itself
				if(tocObject.getType() == ITocConstants.TYPE_TOC)
				{	canMoveUp = false;
					canMoveDown = false;
				}
				else
				{	if(parent != null)
					{	TocTopic topic = (TocTopic)parent;
						if(topic.isFirstChildObject(tocObject))
						{	canMoveUp = false;
						}

						if(topic.isLastChildObject(tocObject))
						{	canMoveDown = false;
						}
					}
				}
			}
			else
			{	// How anyone can select a null object, I don't know.
				// However, if it happens, disable all buttons.
				canAddObject = false;
				canRemove = false;
				canMoveUp = false;
				canMoveDown = false;

				break;
			}
		}

		getTreePart().setButtonEnabled(F_BUTTON_ADD_TOPIC, canAddObject);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_LINK, canAddObject);
		getTreePart().setButtonEnabled(F_BUTTON_ADD_ANCHOR, canAddObject);
		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, canRemove);
		getTreePart().setButtonEnabled(F_BUTTON_UP, canMoveUp);
		getTreePart().setButtonEnabled(F_BUTTON_DOWN, canMoveDown);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		// Get the current selection
		ISelection selection = fTocTree.getSelection();
		Object object = ((IStructuredSelection) selection).getFirstElement();
		// Has to be null or a TOC object
		TocObject tocObject = (TocObject)object;

		if(tocObject.canBeParent())
		{	// Create the "New" sub-menu
			MenuManager submenu = new MenuManager(PDEUIMessages.Menus_new_label);
			// Populate the "New" sub-menu
			fillContextMenuAddActions(submenu, tocObject);
			// Add the "New" sub-menu to the main context menu
			manager.add(submenu);

			// Add a separator to the main context menu
			manager.add(new Separator());
		}

		// Add the Show In action and Remove action if an object is selected
		if (tocObject != null)
		{	// Add clipboard actions
			getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
			manager.add(new Separator());

			// Add the TOC object removal action
			fillContextMenuRemoveAction(manager, tocObject);
			manager.add(new Separator());

			fillContextMenuShowInAction(manager);
			manager.add(new Separator());
		}
	}

	private void fillContextMenuShowInAction(IMenuManager manager) {
		String showInLabel = PDEUIMessages.PluginsView_showIn;

		// Add a label for the keybinding for Show In action, if one exists
		IBindingService bindingService = (IBindingService) PlatformUI
			.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService != null)
		{	String keyBinding = bindingService
				.getBestActiveBindingFormattedFor(
					"org.eclipse.ui.navigate.showInQuickMenu"); //$NON-NLS-1$
			if (keyBinding != null)
			{	showInLabel += '\t' + keyBinding;
			}
		}

		// Add the "Show In" action and its contributions
		IMenuManager showInMenu = new MenuManager(showInLabel);
		showInMenu.add(ContributionItemFactory.VIEWS_SHOW_IN
			.create(getPage().getSite().getWorkbenchWindow()));

		manager.add(showInMenu);
	}

	/**
	 * Add the addition actions (Topic, Link, Anchor) to the specified submenu
	 * 
	 * @param submenu The submenu to add the addition actions to
	 * @param tocObject The object that the additions would occur relative to
	 */
	private void fillContextMenuAddActions(MenuManager submenu, 
			TocObject tocObject) {

		if(tocObject != null && tocObject.canBeParent())
		{	// Add the 'Add Topic' action to the sub-menu
			fAddTopicAction.setParentObject(tocObject);
			fAddTopicAction.setEnabled(fModel.isEditable());
			submenu.add(fAddTopicAction);
	
			// Add the 'Add Link' action to the sub-menu
			fAddLinkAction.setParentObject(tocObject);
			fAddLinkAction.setEnabled(fModel.isEditable());
			submenu.add(fAddLinkAction);
	
			// Add the 'Add Anchor' action to the sub-menu
			fAddAnchorAction.setParentObject(tocObject);
			fAddAnchorAction.setEnabled(fModel.isEditable());
			submenu.add(fAddAnchorAction);
		}
	}

	/**
	 * Add the remove action to the context menu.
	 * 
	 * @param manager The context menu to add the remove action to
	 * @param tocObject The object that would be targetted for removal
	 */
	private void fillContextMenuRemoveAction(IMenuManager manager,
			TocObject tocObject) {
		// Add to the main context menu

		// Delete task object action
		fRemoveObjectAction.setToRemove(tocObject);
		manager.add(fRemoveObjectAction);

		fRemoveObjectAction.setEnabled(tocObject.canBeRemoved() && fModel.isEditable());
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#canPaste(java.lang.Object, java.lang.Object[])
	 */
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		boolean cutAction = actionId.equals(ActionFactory.CUT.getId());

		if (cutAction || actionId.equals(ActionFactory.DELETE.getId())) {
			handleDeleteAction();
			return !cutAction;
		}

		if(actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}

		return false;
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#doPaste(java.lang.Object, java.lang.Object[])
	 */
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		performDrop(targetObject, sourceObjects, ViewerDropAdapter.LOCATION_ON);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#handleDoubleClick(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void handleDoubleClick(IStructuredSelection selection) {
		Object selected = selection.getFirstElement();
		if(selected instanceof TocObject)
		{	String path = ((TocObject)selected).getPath();

			if(path != null)
			{	openDocument(path);
			}
		}
	}

	/**
	 * Opens a document with the specified path
	 * 
	 * @param path a path to a resource, relative to this TOC's root project
	 */
	public void openDocument(String path)
	{	Path resourcePath = new Path(path);
		if(isEditable() && !resourcePath.isEmpty())
		{	IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
			IPath pluginPath = fModel.getUnderlyingResource().getProject().getFullPath();
			IResource resource = root.findMember(pluginPath.append(resourcePath));
			try
			{	if (resource != null && resource instanceof IFile)
				{	IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
				}
				else
				{	MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_warning);
				}
			}
			catch (PartInitException e)
			{	//suppress exception
			}
		}
		else
		{	MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath);
		}
	}

	/**
	 * Perform a drop of the specified objects on the target in the widget
	 * 
	 * @param currentTarget The object that the drop will occur near/on
	 * @param dropped The dropped objects
	 * @param location The location of the drop relative to the target
	 * 
	 * @return true iff the drop was successful
	 */
	public boolean performDrop(Object currentTarget, Object dropped, int location)
	{	if(dropped instanceof Object[])
		{	TocObject tocTarget = (TocObject)currentTarget;
			// Determine the object that the dropped objects will be the
			// children of
			TocTopic targetParent = determineParent(tocTarget, location);

			if(location == TocDropAdapter.LOCATION_JUST_AFTER
					&& targetParent == tocTarget
					&& !tocTarget.getChildren().isEmpty()
					&& fTocTree.getExpandedState(tocTarget))
			{	// If the drop occurs just after a parentable object
				// and it is expanded, then insert the dropped items
				// as the first children of the parent
				location = ViewerDropAdapter.LOCATION_BEFORE;
				tocTarget = (TocObject)tocTarget.getChildren().get(0);
			}

			if(targetParent != null)
			{	// Get the TocObject versions of the dropped objects
				ArrayList objectsToAdd = getObjectsToAdd((Object[])dropped, targetParent);
				
				if(objectsToAdd != null && !objectsToAdd.isEmpty())
				{	if(fDragAdapter.getDraggedElements() != null
						&& fDragAdapter.getDraggedElements().size() == 1
						&& currentTarget == fDragAdapter.getDraggedElements().get(0))
					{	// Last-minute check: ignore drops of an object onto/near itself
						// to avoid unnecessarily dirtying the page
						return false;
					}

					boolean insertBefore = (location == ViewerDropAdapter.LOCATION_BEFORE);

					// Add the objects
					handleMultiAddAction(objectsToAdd, tocTarget, insertBefore, targetParent);
					return true;
				}
			}
		}
	
		return false;
	}

	/**
	 * Determine the parent object that a drop will occur under,
	 * based on the relative location of the drop and the ability
	 * of the target to be a parent
	 * 
	 * @param dropTarget The target that the drop occurs near/on
	 * @param dropLocation The location of the drop relative to the target
	 * @return
	 */
	private TocTopic determineParent(TocObject dropTarget, int dropLocation) {
		//We must determine what object will be the parent of the
		//dropped objects. This is done by looking at the drop location
		//and drop target type

		if(dropTarget == null || dropTarget.getType() == ITocConstants.TYPE_TOC)
		{	//Since the TOC root has no parent, it must be the target parent
			return fModel.getToc();
		}
		else if(!dropTarget.canBeParent())
		{	//If the object is a leaf, it cannot be the parent
			//of the new objects,
			//so the target parent must be its parent
			return (TocTopic)dropTarget.getParent();
		}
		else
		{	//In all other cases, it depends on the location of the drop
			//relative to the drop target
			switch(dropLocation)
			{	case TocDropAdapter.LOCATION_JUST_AFTER:
				{	//if the drop occured after an expanded node
					//and all of its children,
					//make the drop target's parent the target parent object
					if(!fTocTree.getExpandedState(dropTarget))
					{	return (TocTopic)dropTarget.getParent();
					}
					//otherwise, the target parent is the drop target,
					//since the drop occured between it and its first child
				}
				case ViewerDropAdapter.LOCATION_ON:
				{	//the drop location is directly on the drop target
					//set the parent object to be the drop target
					return (TocTopic)dropTarget;
				}
				case ViewerDropAdapter.LOCATION_BEFORE:
				case ViewerDropAdapter.LOCATION_AFTER:
				{	//if the drop is before or after the drop target,
					//make the drop target's parent the target parent object
					return (TocTopic)dropTarget.getParent();
				}
			}
		}

		return null;
	}

	/**
	 * Get the TocObject representations of a group of dropped objects.
	 * 
	 * @param droppings The objects that are dropped; can be file path Strings or
	 * deserialized TocObjects
	 * 
	 * @param targetParent The designated parent of the dropped objects
	 * 
	 * @return a list of the (reconnected) TocObject representations of the dropped objects
	 */
	private ArrayList getObjectsToAdd(Object[] droppings, TocTopic targetParent) {
		ArrayList tocObjects = new ArrayList(droppings.length);
		
		if(fDragAdapter.getDraggedElements() != null)
		{	// If there are items in the drag adapter, then the current drag must be from
			// this section
			fDragFromHere = fDragAdapter.getDraggedElements().size() == droppings.length;
		}

		for(int i = 0; i < droppings.length; ++i)
		{	if(droppings[i] instanceof String)
			{	// If the array contains Strings, we treat them as file paths
				Path path = new Path((String)droppings[i]);
				
				// If the path is to a file with a TOC's extension, make a link
				if(TocExtensionUtil.hasValidTocExtension(path))
				{	tocObjects.add(makeNewTocLink(targetParent, path));
				}
				// If the path is to a file with an HTML page extension, make a topic
				else if(TocExtensionUtil.hasValidPageExtension(path))
				{	TocTopic topic = makeNewTocTopic(targetParent, path); 
					String title = generateTitle(targetParent, path);

					topic.setFieldLabel(title);
					tocObjects.add(topic);
				}
			}
			else if(droppings[i] instanceof TocObject)
			{	ArrayList dragged = fDragAdapter.getDraggedElements();
				if(fDragFromHere)
				{	TocObject draggedObj = (TocObject)dragged.get(i);

					//Nesting an object inside itself or its children
					//is so stupid and ridiculous that I get a headache
					//just thinking about it. Thus, this drag is not going to complete.
					if(targetParent.descendsFrom(draggedObj))
					{	return null;
					}
				}

				//Reconnect this TocObject, since it was deserialized
				((TocObject)droppings[i]).reconnect(targetParent, fModel);
				tocObjects.add(droppings[i]);
			}
		}

		return tocObjects;
	}

	/**
	 * Generate the title of a Topic created via dragging in an HTML page.
	 * Use the title of the HTML page, or generate a name based on the target
	 * parent if no title exists.
	 * 
	 * @param targetParent The designated parent of this topic
	 * @param path The path to the HTML file
	 * 
	 * @return The generated name of the Topic.
	 */
	private String generateTitle(TocTopic targetParent, Path path) {
		String title = TocHTMLTitleUtil.findTitle(path.toFile());	
		if(title == null)
		{	int numChildren = targetParent.getChildren().size();
			TocObject[] children = 
				(TocObject[])targetParent.getChildren().toArray(new TocObject[numChildren]);

			String[] tocObjectNames = new String[children.length];

			for(int j = 0; j < numChildren; ++j)
			{	tocObjectNames[j] = children[j].getName();
			}

			title = PDELabelUtility.generateName(tocObjectNames, PDEUIMessages.TocPage_TocTopic);
		}
		return title;
	}

	/**
	 * Create a new Topic node using the model's factory.
	 *  
	 * @param parent The designated parent for the new topic
	 * @param path The file that this Topic will link to
	 * 
	 * @return the newly created topic
	 */
	private TocTopic makeNewTocTopic(TocObject parent, Path path) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return fModel.getFactory().createTocTopic(root.getFileForLocation(path));
	}
	
	/**
	 * Create a new Link node using the model's factory.
	 *  
	 * @param parent The designated parent for the new link
	 * @param path The file that this link will be associated with
	 * 
	 * @return the newly created link
	 */
	private TocLink makeNewTocLink(TocObject parent, Path path) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return fModel.getFactory().createTocLink(root.getFileForLocation(path));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		switch (index) {
		case F_BUTTON_ADD_TOPIC:
			handleAddAction(fAddTopicAction);
			break;
		case F_BUTTON_ADD_LINK:
			handleAddAction(fAddLinkAction);
			break;
		case F_BUTTON_ADD_ANCHOR:
			handleAddAction(fAddAnchorAction);
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
	
	/**
	 * Handle the addition of an object by preparing and running the
	 * specified action.
	 * 
	 * @param action The action to run for the addition
	 */
	private void handleAddAction(TocAddObjectAction action) {
		//Currently, all additions in the TOC editor are semantically similar
		//Thus, all addition operations can follow the same procedure
		
		ISelection sel = fTocTree.getSelection();
		Object object = ((IStructuredSelection) sel).getFirstElement();
		if (object == null) {
			return;
		}

		TocObject tocObject = (TocObject)object;
		
		if (tocObject.canBeParent()) {
			// If the selected object can be a parent, then add
			// the new object as a child of this object
			action.setParentObject(tocObject);
			action.run();
		}
		else
		{	// If the selected object cannot be a parent, then add
			// the new object as a direct sibling of this object
			action.setParentObject(tocObject.getParent());
			action.setTargetObject(tocObject);
			action.run();
		}
	}

	/**
	 * Handle the addition of multiple initialized objects to the TOC.
	 * 
	 * @param objectsToAdd The objects to be added
	 * @param tocTarget The target to add these objects relative to
	 * @param insertBefore Whether or not the insertion occurs before the target
	 * @param targetParent The parent object of the newly added objects
	 */
	private void handleMultiAddAction(List objectsToAdd, TocObject tocTarget, boolean insertBefore, TocObject targetParent)
	{	TocObject[] tocObjects = (TocObject[])objectsToAdd.toArray(new TocObject[objectsToAdd.size()]);
		if (tocObjects == null) return;

		for(int i = 0; i < tocObjects.length; ++i)
		{	if (tocObjects[i] != null) {
				if (targetParent != null && targetParent.canBeParent()) {
					if(tocTarget != null && tocTarget != targetParent)
					{	// Add the object as a direct sibling of the target
						((TocTopic)targetParent).addChild(tocObjects[i], tocTarget, insertBefore);
					}
					else
					{	// Add the object as the last child of the target parent
						((TocTopic)targetParent).addChild(tocObjects[i]);
					}
				}
			}
		}
	}

	/**
	 * Remove the selected objects from the TOC tree
	 */
	private void handleDeleteAction()
	{	ArrayList objects = new ArrayList(((IStructuredSelection)fTocTree.getSelection()).toList());
		boolean beep = false;
		
		// Iterate through the list of selected objects, removing ones
		// that cannot be removed
		for(Iterator i = objects.iterator(); i.hasNext();)
		{	Object object = i.next();
			if (object instanceof TocObject)
			{	TocObject tocObject = (TocObject)object;
	
				if (!tocObject.canBeRemoved()) {
					i.remove();
					beep = true;
				}
			}
		}
	
		if(beep)
		{	// If any object cannot be removed, beep to notify the user			
			Display.getCurrent().beep();
		}

		// Remove the remaining objects
		handleRemove(objects);
	}

	/**
	 * Remove the items listed from the TOC.
	 * 
	 * @param itemsToRemove The list of items to remove from the TOC 
	 */
	public void handleRemove(List itemsToRemove)
	{	if(!itemsToRemove.isEmpty())
		{	// Target the objects for removal
			fRemoveObjectAction.setToRemove((TocObject[])itemsToRemove.toArray(new TocObject[itemsToRemove.size()]));

			// Run the removal action
			fRemoveObjectAction.run();
		}
	}

	/**
	 * Handle the dragging of objects out of this TOC.
	 * 
	 * @param itemsDragged The items dragged out of the TOC
	 */
	public void handleDrag(List itemsDragged)
	{	handleRemove(itemsDragged);

		// The drag is finished, so there is no intra-editor DND operation occuring now
		fDragFromHere = false;
	}

	/**
	 * Move an object within the TOC.
	 * 
	 * @param positionFlag The direction that the object will move
	 */
	private void handleMoveAction(int positionFlag) {
		IStructuredSelection sel = (IStructuredSelection)fTocTree.getSelection();

		for(Iterator iter = sel.iterator(); iter.hasNext();)
		{	Object object = iter.next();
			if (object == null) {
				return;
			}
			else if (object instanceof TocObject) {
				TocObject tocObject = (TocObject)object;
				TocTopic parent = (TocTopic)tocObject.getParent();
	
				// Determine the parent type
				if (parent != null)
				{	// Move the object up or down one position
					parent.moveChild(tocObject, positionFlag);
				}
			}
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
		} else if ((event.getChangeType() == IModelChangedEvent.CHANGE) &&
				(event.getChangedProperty().equals(IDocumentNode.F_PROPERTY_CHANGE_TYPE_SWAP)) ) {
			handleModelChangeTypeSwap(event);
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			handleModelChangeType(event);
		}
	}
	
	/**
	 * @param event
	 */
	private void handleModelChangeTypeSwap(IModelChangedEvent event) {
		// Swap event
		// Get the changed object
		Object[] objects = event.getChangedObjects();
		TocObject object = (TocObject) objects[0];

		if (object != null)
		{	// Update the element in the tree viewer
			fTocTree.refresh(object);
		}		
	}		
	
	/**
	 * The model is stale, refresh the UI
	 * 
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
		// Insert event
		Object[] objects = event.getChangedObjects();
		TocObject object = (TocObject) objects[0];
		if (object != null)
		{	if (object.getType() != ITocConstants.TYPE_TOC) {
				// Refresh the parent element in the tree viewer
				// TODO: Can we get away with an update instead of a refresh here?
				fTocTree.refresh(object.getParent());
				// Select the new object in the tree
				fTocTree.setSelection(new StructuredSelection(object), true);
			}
		}
	}	

	/**
	 * Handle removals in the model
	 * 
	 * @param event the removal event
	 */
	private void handleModelRemoveType(IModelChangedEvent event) {
		// Remove event
		Object[] objects = event.getChangedObjects();
		TocObject object = (TocObject) objects[0];
		if (object != null)
		{	if (object.getType() != ITocConstants.TYPE_TOC) {
				handleTaskObjectRemove(object);
			}
		}
	}

	/**
	 * An object was removed, update the UI to respond to the removal
	 * 
	 * @param object The object that was removed
	 */
	private void handleTaskObjectRemove(TocObject object) {
		// Remove the item
		fTocTree.remove(object);

		// Select the appropriate object
		TocObject tocObject = fRemoveObjectAction.getNextSelection();
		if (tocObject == null)
		{	tocObject = object.getParent();
		}

		if(tocObject.equals(object.getParent()))
		{	fTocTree.refresh(object.getParent());
		}

		if(!fDragFromHere)
		{	fTocTree.setSelection(new StructuredSelection(tocObject), true);
		}
	}	

	/**
	 * Handle an update to a TocObject's properties
	 * @param event the update event
	 */
	private void handleModelChangeType(IModelChangedEvent event) {
		// Get the changed object
		Object[] objects = event.getChangedObjects();
		TocObject object = (TocObject) objects[0];

		if (object != null)
		{	// Update the element in the tree viewer
			fTocTree.update(object, null);
		}
	}	

	public void refresh() {
		TocModel model = (TocModel)getPage().getModel();
		fTocTree.setInput(model);
		fTocTree.expandToLevel(2);
		fTocTree.setSelection(new StructuredSelection(model.getToc()), true);
		getManagedForm().fireSelectionChanged(this,
				fTocTree.getSelection());
		super.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TreeSection#createTreeViewer(org.eclipse.swt.widgets.Composite, int)
	 */
	protected TreeViewer createTreeViewer(Composite parent, int style) {
		fFilteredTree = new FormFilteredTree(parent, style, new PatternFilter());
		parent.setData("filtered", Boolean.TRUE); //$NON-NLS-1$
		return fFilteredTree.getViewer();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
}
