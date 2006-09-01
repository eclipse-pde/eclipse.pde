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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TreeSection;
import org.eclipse.pde.internal.ui.parts.TreePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSElementSection
 *
 */
public class SimpleCSElementSection extends TreeSection {

	private TreeViewer fTreeViewer;
	
	/**
	 * @param formPage
	 * @param parent
	 * @param style
	 * @param buttonLabels
	 */
	public SimpleCSElementSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, new String[] {
				PDEUIMessages.SimpleCSElementSection_0, PDEUIMessages.SimpleCSElementSection_1, PDEUIMessages.SimpleCSElementSection_2, PDEUIMessages.SimpleCSElementSection_3});
		getSection().setText(PDEUIMessages.SimpleCSElementSection_4);
		// TODO: MP: Put for details section
		//getSection().setDescription("The following properties are available for this cheat sheet element:");
		getSection().setDescription(PDEUIMessages.SimpleCSElementSection_5);
			
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
		ISimpleCSModel model = (ISimpleCSModel)getPage().getModel();
		fTreeViewer.setInput(model);
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
}
