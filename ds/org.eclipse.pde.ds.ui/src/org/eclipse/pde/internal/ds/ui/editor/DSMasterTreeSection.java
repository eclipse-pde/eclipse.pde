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

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.editor.actions.CollapseAction;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSMasterTreeSection extends TreeSection implements IDSMaster {

	private TreeViewer fTreeViewer;

	private IDSModel fModel;

	private CollapseAction fCollapseAction;

	private ControlDecoration fSubStepInfoDecoration;

	
	public DSMasterTreeSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.SimpleCSElementSection_0, PDEUIMessages.SimpleCSElementSection_6, null, null, PDEUIMessages.SimpleCSElementSection_7, PDEUIMessages.SimpleCSElementSection_1, PDEUIMessages.SimpleCSElementSection_2, null, null, PDEUIMessages.SimpleCSElementSection_3});
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
		// TODO Create section toolbar
		
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
// } else if (event.getChangeType() == IModelChangedEvent.INSERT) {
			// handleModelInsertType(event);
			// } else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
			// handleModelRemoveType(event);
			// } else if ((event.getChangeType() == IModelChangedEvent.CHANGE)
			// && (event.getChangedProperty()
			// .equals(IDocumentElementNode.F_PROPERTY_CHANGE_TYPE_SWAP))) {
			// handleModelChangeTypeSwap(event);
			// } else if (event.getChangeType() == IModelChangedEvent.CHANGE) {
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
	
}
