/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class PDEDetails extends AbstractFormPart implements IDetailsPage, IContextPart {
	/**
	 * 
	 */
	public PDEDetails() {
	}
	
	public boolean canPaste(Clipboard clipboard) {
		return true;
	}
	
	public boolean doGlobalAction(String actionId) {
		return false;
	}
	
	protected void markDetailsPart(Control control) {
		control.setData("part", this);
	}
	
	protected void createSpacer(FormToolkit toolkit, Composite parent, int span) {
		Label spacer = toolkit.createLabel(parent, "");
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		spacer.setLayoutData(gd);
	}
	public void cancelEdit() {
		super.refresh();
	}
}