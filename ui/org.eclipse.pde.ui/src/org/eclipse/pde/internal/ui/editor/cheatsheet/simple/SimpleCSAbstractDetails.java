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

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSAbstractDetails
 *
 */
public abstract class SimpleCSAbstractDetails extends PDEDetails {

	private Section fSection;
	
	private SimpleCSElementSection fElementSection;
	
	// TODO: MP: Can refactor with copied method from AbstractSchemaDetails
	protected static final String[] BOOLS = 
		new String[] { Boolean.toString(true), Boolean.toString(false) };
	
	/**
	 * 
	 */
	public SimpleCSAbstractDetails(SimpleCSElementSection elementSection) {
		fElementSection = elementSection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public final void createContents(Composite parent) {
		
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		parent.setLayout(layout);
		FormToolkit toolkit = getManagedForm().getToolkit();
		fSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fSection.clientVerticalSpacing = PDESection.CLIENT_VSPACING;
		fSection.marginHeight = 5;
		fSection.marginWidth = 5; 
		GridData gd = new GridData(GridData.FILL_BOTH);
		fSection.setLayoutData(gd);
		Composite client = toolkit.createComposite(fSection);
		
		createDetails(client);
		updateFields();
		
		toolkit.paintBordersFor(client);
		fSection.setClient(client);
		markDetailsPart(fSection);
		
		hookListeners();
		
	}

	/**
	 * @param parent
	 */
	public abstract void createDetails(Composite parent);
	
	/**
	 * 
	 */
	public abstract void updateFields();
	
	/**
	 * 
	 */
	public abstract void hookListeners();
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#fireSaveNeeded()
	 */
	public void fireSaveNeeded() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#getContextId()
	 */
	public String getContextId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#getPage()
	 */
	public PDEFormPage getPage() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#isEditable()
	 */
	public boolean isEditable() {
		return fElementSection.isEditable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelChangedListener#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent event) {
		// TODO Auto-generated method stub

	}

	/**
	 * @param description
	 */
	protected void setDecription(String description) {
		fSection.setDescription(description); 
	}
	
	/**
	 * @param title
	 */
	protected void setText(String title) {
		fSection.setText(title);
	}	
	
	/**
	 * @return
	 */
	public boolean isEditableElement() {
		return fElementSection.isEditable();
	}	
	
	/**
	 * @param parent
	 * @param toolkit
	 * @param colSpan
	 * @return
	 */
	protected Button[] createTrueFalseButtons(Composite parent, FormToolkit toolkit, int colSpan) {
		// TODO: MP: Can refactor with copied method from AbstractSchemaDetails
		Composite comp = toolkit.createComposite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.marginWidth = 0;
		comp.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = colSpan;
		comp.setLayoutData(gd);
		Button tButton = toolkit.createButton(comp, BOOLS[0], SWT.RADIO);
		Button fButton = toolkit.createButton(comp, BOOLS[1], SWT.RADIO);
		gd = new GridData();
		gd.horizontalIndent = 20;
		fButton.setLayoutData(gd);
		return new Button[] {tButton, fButton};
	}
}
