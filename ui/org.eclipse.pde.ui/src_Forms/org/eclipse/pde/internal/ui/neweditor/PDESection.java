/*
 * Created on Jan 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.pde.core.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class PDESection extends SectionPart implements IModelChangedListener, IContextPart {
	private PDEFormPage page;
	/**
	 * @param section
	 */
	public PDESection(PDEFormPage page, Composite parent, int style) {
		super(parent, page.getManagedForm().getToolkit(), Section.TITLE_BAR | style);
		this.page = page;
		initialize(page.getManagedForm());
		getSection().clientVerticalSpacing = 8;
		getSection().setData("part", this);
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
}