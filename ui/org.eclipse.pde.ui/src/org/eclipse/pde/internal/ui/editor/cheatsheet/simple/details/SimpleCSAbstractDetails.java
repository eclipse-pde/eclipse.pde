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

package org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ui.editor.PDEDetails;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSElementSection;
import org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * SimpleCSAbstractDetails
 *
 */
public abstract class SimpleCSAbstractDetails extends PDEDetails implements
		ISimpleCSDetails {

	private SimpleCSElementSection fElementSection;
	
	protected FormToolkit fToolkit;
	
	/**
	 * 
	 */
	public SimpleCSAbstractDetails(SimpleCSElementSection elementSection) {
		fElementSection = elementSection;
		fToolkit = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	public final void createContents(Composite parent) {
		createDetails(parent);
		updateFields();
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
		// TODO: MP: Anything needed here?

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#fireSaveNeeded()
	 */
	public void fireSaveNeeded() {
		markDirty();
		getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#getContextId()
	 */
	public String getContextId() {
		return SimpleCSInputContext.CONTEXT_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IContextPart#getPage()
	 */
	public PDEFormPage getPage() {
		return (PDEFormPage)getManagedForm().getContainer();
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
		// TODO: MP: Do we need to do anything here?

	}
	
	/**
	 * @return
	 */
	public boolean isEditableElement() {
		return fElementSection.isEditable();
	}	
	
	/**
	 * @return
	 */
	public FormToolkit getToolkit() {
		return fToolkit;
	}
}
