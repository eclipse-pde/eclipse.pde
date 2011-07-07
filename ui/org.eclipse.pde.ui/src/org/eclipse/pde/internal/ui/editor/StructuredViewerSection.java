/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class StructuredViewerSection extends PDESection implements IPDEDragParticipant, IPDEDropParticipant {

	protected StructuredViewerPart fViewerPart;

	private boolean fDoSelection;

	private PDEDragAdapter fDragAdapter;

	private PDEDropAdapter fDropAdapter;

	/**
	 * Constructor for StructuredViewerSection.
	 * @param formPage
	 */
	public StructuredViewerSection(PDEFormPage formPage, Composite parent, int style, String[] buttonLabels) {
		this(formPage, parent, style, true, buttonLabels);
	}

	/**
	 * Constructor for StructuredViewerSection.
	 * @param formPage
	 */
	public StructuredViewerSection(PDEFormPage formPage, Composite parent, int style, boolean titleBar, String[] buttonLabels) {
		super(formPage, parent, style, titleBar);
		fViewerPart = createViewerPart(buttonLabels);
		fViewerPart.setMinimumSize(50, 50);
		FormToolkit toolkit = formPage.getManagedForm().getToolkit();
		createClient(getSection(), toolkit);
		fDoSelection = true;
	}

	protected void createViewerPartControl(Composite parent, int style, int span, FormToolkit toolkit) {
		fViewerPart.createControl(parent, style, span, toolkit);
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		Control control = fViewerPart.getControl();
		Menu menu = popupMenuManager.createContextMenu(control);
		control.setMenu(menu);
		registerPopupMenu(popupMenuManager);
		// Initialize drag and drop
		if (isDragAndDropEnabled()) {
			initializeDragAndDrop();
		} else {
			fDragAdapter = null;
			fDropAdapter = null;
		}
	}

	/**
	 * If the context menu for this section should be registered, do it here
	 * with the appropriate id etc.  By default do nothing.
	 * @param popupMenuManager the menu manager to be registered
	 */
	protected void registerPopupMenu(MenuManager popupMenuManager) {
		// do nothing by default
	}

	protected Composite createClientContainer(Composite parent, int span, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, span));
		return container;
	}

	protected abstract StructuredViewerPart createViewerPart(String[] buttonLabels);

	protected void fillContextMenu(IMenuManager manager) {
	}

	protected void buttonSelected(int index) {
	}

	protected void doPaste() {
		ISelection selection = getViewerSelection();
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() > 1)
			return;

		Object target = ssel.getFirstElement();

		Clipboard clipboard = getPage().getPDEEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object[] objects = (Object[]) clipboard.getContents(modelTransfer);
		if (objects != null) {
			doPaste(target, objects);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#canPaste(org.eclipse.swt.dnd.Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		// TODO: MP: CCP: Checking clipboard data done incorrectly.  See Bug 37223
		ISelection selection = getViewerSelection();
		IStructuredSelection ssel = (IStructuredSelection) selection;
		if (ssel.size() > 1)
			return false;

		Object target = ssel.getFirstElement();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object[] objects = (Object[]) clipboard.getContents(modelTransfer);
		if (objects != null && objects.length > 0) {
			return canPaste(target, objects);
		}
		return false;
	}

	protected ISelection getViewerSelection() {
		return fViewerPart.getViewer().getSelection();
	}

	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// NO-OP
		// Children will override to provide fuctionality
	}

	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		return false;
	}

	public void setFocus() {
		fViewerPart.getControl().setFocus();
	}

	public StructuredViewerPart getStructuredViewerPart() {
		return this.fViewerPart;
	}

	/**
	 * <p>Given the index of TreeViewer item and the size of the array of its immediate
	 * siblings, gets the index of the desired new selection as follows:
	 * <ul><li>if this is the only item, return -1 (meaning select the parent)</li>
	 * <li>if this is the last item, return the index of the predecessor</li>
	 * <li>otherwise, return the index of the successor</li></p>
	 * 
	 * @param thisIndex
	 * 			the item's index
	 * @param length
	 * 			the array length
	 * @return
	 * 			new selection index or -1 for parent
	 */
	protected int getNewSelectionIndex(int thisIndex, int length) {
		if (thisIndex == length - 1)
			return thisIndex - 1;
		return thisIndex + 1;
	}

	protected int getArrayIndex(Object[] array, Object object) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(object))
				return i;
		}
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDragParticipant#canDragCopy(java.lang.Object[])
	 */
	public boolean canDragCopy(Object[] sourceObjects) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDragParticipant#canDragLink(java.lang.Object[])
	 */
	public boolean canDragLink(Object[] sourceObjects) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDragParticipant#canDragMove(java.lang.Object[])
	 */
	public boolean canDragMove(Object[] sourceObjects) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDropParticipant#canDropCopy(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropCopy(Object targetObject, Object[] sourceObjects, int targetLocation) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDropParticipant#canDropLink(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropLink(Object targetObject, Object[] sourceObjects, int targetLocation) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDropParticipant#canDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDropParticipant#doDropCopy(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropCopy(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// NO-OP
		// Sub-classes to override
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDropParticipant#doDropLink(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropLink(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// NO-OP
		// Sub-classes to override
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDropParticipant#doDropMove(java.lang.Object, java.lang.Object[], int)
	 */
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// NO-OP
		// Sub-classes to override
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDragParticipant#doDragRemove(java.lang.Object[])
	 */
	public void doDragRemove(Object[] sourceObjects) {
		// NO-OP
		// Sub-classes to override
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IPDEDragParticipant#getSupportedDNDOperations()
	 */
	public int getSupportedDNDOperations() {
		return DND.DROP_MOVE;
	}

	protected void initializeDragAndDrop() {
		// Ensure the model is editable and we have a viewer part
		if (isEditable() == false) {
			return;
		} else if (fViewerPart == null) {
			return;
		}
		StructuredViewer viewer = fViewerPart.getViewer();
		// Ensure we have a viewer
		if (viewer == null) {
			return;
		}
		// Create drag adapter
		fDragAdapter = new PDEDragAdapter(this);
		// Create drop adapter
		fDropAdapter = new PDEDropAdapter(viewer, this, fDragAdapter);
		// Add drag support to viewer
		int dragOperations = getSupportedDNDOperations();
		viewer.addDragSupport(dragOperations, getDragTransfers(), fDragAdapter);
		// Add drop support to viewer
		int dropOperations = dragOperations | DND.DROP_DEFAULT;
		viewer.addDropSupport(dropOperations, getDropTransfers(), fDropAdapter);
	}

	/**
	 * @return The original source objects (dragged)
	 */
	protected Object[] getDragSourceObjects() {
		// Verify DND is enabled
		if (isDragAndDropEnabled() == false) {
			return null;
		} else if (fDragAdapter == null) {
			return null;
		}
		return fDragAdapter.getSourceObjects();
	}

	protected Transfer[] getDragTransfers() {
		return new Transfer[] {ModelDataTransfer.getInstance(), TextTransfer.getInstance()};
	}

	protected Transfer[] getDropTransfers() {
		return getDragTransfers();
	}

	protected boolean isDragAndDropEnabled() {
		return false;
	}

	protected void doSelect(boolean select) {
		fDoSelection = select;
	}

	protected boolean canSelect() {
		return fDoSelection;
	}

}
