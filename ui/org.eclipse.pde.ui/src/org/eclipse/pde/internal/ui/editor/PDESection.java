/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.pde.core.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public abstract class PDESection extends SectionPart implements IModelChangedListener, IContextPart {
	public static final int CLIENT_VSPACING = 4;
	private PDEFormPage page;
	public PDESection(PDEFormPage page, Composite parent, int style) {
		this(page, parent, style, true);
	}	
	/**
	 * @param section
	 *	 
	 */
	public PDESection(PDEFormPage page, Composite parent, int style, boolean titleBar) {
		super(parent, page.getManagedForm().getToolkit(), titleBar?(ExpandableComposite.TITLE_BAR | style): style);
		this.page = page;
		initialize(page.getManagedForm());
		getSection().clientVerticalSpacing = CLIENT_VSPACING;
		getSection().setData("part", this); //$NON-NLS-1$
		//createClient(getSection(), page.getManagedForm().getToolkit());
	}
	
	protected abstract void createClient(Section section, FormToolkit toolkit);

	public PDEFormPage getPage() {
		return page;
	}
	
	public boolean doGlobalAction(String actionId) {
		return false;
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType()==IModelChangedEvent.WORLD_CHANGED)
			markStale();
	}
	
	public String getContextId() {
		return null;
	}
	public void fireSaveNeeded() {
		markDirty();
		if (getContextId()!=null)
			getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}
	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}
	public boolean canPaste(Clipboard clipboard) {
		return false;
	}
	public void cancelEdit() {
		super.refresh();
	}
}