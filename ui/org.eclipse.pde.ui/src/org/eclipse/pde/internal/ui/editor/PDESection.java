/*******************************************************************************
 *  Copyright (c) 2003, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.*;

public abstract class PDESection extends SectionPart implements IContextPart, IAdaptable {

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

	@Override
	public PDEFormPage getPage() {
		return fPage;
	}

	protected IProject getProject() {
		return fPage.getPDEEditor().getCommonProject();
	}

	public boolean doGlobalAction(String actionId) {
		return false;
	}

	protected void handleSelectAll() {
		// NO-OP
		// Sub-classes to override
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED)
			markStale();
	}

	@Override
	public String getContextId() {
		return null;
	}

	@Override
	public void fireSaveNeeded() {
		markDirty();
		if (getContextId() != null)
			getPage().getPDEEditor().fireSaveNeeded(getContextId(), false);
	}

	@Override
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

	@Override
	public void cancelEdit() {
		super.refresh();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}
