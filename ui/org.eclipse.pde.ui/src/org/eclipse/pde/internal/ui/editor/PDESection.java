/*******************************************************************************
 *  Copyright (c) 2003, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.*;

public abstract class PDESection extends SectionPart implements IModelChangedListener, IContextPart, IAdaptable {

	private PDEFormPage fPage;

	public PDESection(PDEFormPage page, Composite parent, int style) {
		this(page, parent, style, true);
	}

	public PDESection(PDEFormPage page, Composite parent, int style, boolean titleBar) {
		super(parent, page.getManagedForm().getToolkit(), titleBar ? (ExpandableComposite.TITLE_BAR | style) : style);
		fPage = page;
		initialize(page.getManagedForm());
		getSection().clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		getSection().setData("part", this); //$NON-NLS-1$
	}

	protected abstract void createClient(Section section, FormToolkit toolkit);

	public PDEFormPage getPage() {
		return fPage;
	}

	protected IProject getProject() {
		return fPage.getPDEEditor().getCommonProject();
	}

	public boolean doGlobalAction(String actionId) {
		return false;
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED)
			markStale();
	}

	public String getContextId() {
		return null;
	}

	public void fireSaveNeeded() {
		markDirty();
		if (getContextId() != null)
			getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	public boolean isEditable() {
		// getAggregateModel() can (though never should) return null
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return model == null ? false : model.isEditable();
	}

	public boolean canCopy(ISelection selection) {
		// Sub-classes to override
		return false;
	}

	public boolean canCut(ISelection selection) {
		// Sub-classes to override
		return false;
	}

	public boolean canPaste(Clipboard clipboard) {
		return false;
	}

	public void cancelEdit() {
		super.refresh();
	}

	public Object getAdapter(Class adapter) {
		return null;
	}
}
