/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BooleanAttributeRow extends ExtensionAttributeRow {
	private Button button;
	/**
	 * @param att
	 */
	public BooleanAttributeRow(ISchemaAttribute att) {
		super(att);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#createContents(org.eclipse.swt.widgets.Composite, org.eclipse.ui.forms.widgets.FormToolkit, int)
	 */
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		toolkit.createLabel(parent, "");		
		button = toolkit.createButton(parent, att.getName(), SWT.CHECK);
		GridData gd = new GridData();
		//gd.horizontalIndent = 10;
		gd.horizontalSpan = span-1;
		button.setLayoutData(gd);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#update(org.eclipse.pde.internal.ui.neweditor.plugin.DummyExtensionElement)
	 */
	protected void update() {
		String value = input!=null?input.getProperty(att.getName()):null;
		boolean state = value!=null && value.toLowerCase().equals("true");
		button.setSelection(state);
	}
	public void setFocus() {
		button.setFocus();
	}
}