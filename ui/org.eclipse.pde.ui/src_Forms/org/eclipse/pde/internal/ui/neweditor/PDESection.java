/*
 * Created on Jan 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;

import org.eclipse.pde.core.*;
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
		super(parent, page.getManagedForm().getToolkit(), style);
		this.page = page;
		initialize(page.getManagedForm());
		//createClient(getSection(), page.getManagedForm().getToolkit());
	}
	
	protected abstract void createClient(Section section, FormToolkit toolkit);

	public PDEFormPage getPage() {
		return page;
	}

	public void modelChanged(IModelChangedEvent e) {
	}
	
	public String getContextId() {
		return null;
	}
}