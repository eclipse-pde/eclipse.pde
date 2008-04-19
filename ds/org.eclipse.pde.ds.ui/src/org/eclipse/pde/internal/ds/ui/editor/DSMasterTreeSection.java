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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSRoot;
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

	private TreeViewer fTreeViewer;

	private IDSModel fModel;

	private CollapseAction fCollapseAction;

	private ControlDecoration fSubStepInfoDecoration;

	public DSMasterTreeSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {
				"Add",
				"Add Sub-Step", null, null,
				"Remove",
				"Up", "Down" });
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
		// TODO Auto-generated method stub

	}

	public void updateButtons() {
		// TODO Auto-generated method stub

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
		// TODO createListeners and Decoration
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		// No need to call super, world changed event handled here

		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(event);
		} else if (event.getChangeType() == IModelChangedEvent.INSERT) {
			// handleModelInsertType(event);
		} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
			// handleModelRemoveType(event);
			// } else if ((event.getChangeType() == IModelChangedEvent.CHANGE)
			// && (event.getChangedProperty()
			// .equals(IDocumentElementNode.F_PROPERTY_CHANGE_TYPE_SWAP))) {
			// handleModelChangeTypeSwap(event);
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			// handleModelChangeType(event);
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

		// getTreePart().setButtonEnabled(F_BUTTON_ADD_STEP,
		// fModel.isEditable());
		//		getTreePart().setButtonEnabled(F_BUTTON_ADD_SUBSTEP, false);
		//		getTreePart().setButtonEnabled(F_BUTTON_REMOVE, false);
		//		getTreePart().setButtonEnabled(F_BUTTON_UP, false);
		// getTreePart().setButtonEnabled(F_BUTTON_DOWN, false);
		// getTreePart().setButtonEnabled(F_BUTTON_PREVIEW, true);

		IDSRoot dsRoot = fModel.getDSRoot();
		// Select the cheatsheet node in the tree
		fTreeViewer.setSelection(new StructuredSelection(dsRoot), true);
		fTreeViewer.expandToLevel(2);
	}
	


}
