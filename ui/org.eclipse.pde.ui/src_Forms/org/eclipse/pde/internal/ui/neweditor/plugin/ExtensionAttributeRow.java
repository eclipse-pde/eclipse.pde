/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.neweditor.plugin.dummy.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class ExtensionAttributeRow {
	protected ISchemaAttribute att;
	protected DummyExtensionElement input;
	
	public ExtensionAttributeRow(ISchemaAttribute att) {
		this.att = att;
	}
	
	public ISchemaAttribute getAttribute() {
		return att;
	}
	protected String getPropertyLabel() {
		String label=att.getName();
		if (att.getUse()==ISchemaAttribute.REQUIRED)
			label+= "*:";
		else
			label+=":";
		return label;
	}
	protected void createLabel(Composite parent, FormToolkit toolkit) {
		Label label = toolkit.createLabel(parent, getPropertyLabel(), SWT.NULL);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		label.setToolTipText(getToolTipText());
	}
	
	protected String getToolTipText() {
		String text = att.getDescription();
		if (text==null) return null;
		int dot = text.indexOf('.');
		if (dot != -1) {
			StringBuffer buf = new StringBuffer();
			boolean inTag=false;
			for (int i=0; i<text.length(); i++) {
				char c = text.charAt(i);
				if (inTag) {
					if (c=='>') {
						inTag = false;
						continue;
					}
				}
				else {
					if (c=='<') {
						inTag = true;
						continue;
					}
					else if (c=='.') {
						if (i<text.length()-1) {
							char c2 = text.charAt(i+1);
							if (c2==' ' || c2=='\t' || c2=='\n') break;
						}
					}
					buf.append(c);
				}
			}
			return buf.toString();
		}
		else
			return text;
	}
	
	public abstract void createContents(Composite parent, FormToolkit toolkit, int span);
	protected abstract void update();	
	public abstract void setFocus();
	
	public void dispose() {
	}
	public void setInput(DummyExtensionElement input) {
		this.input = input;
		update();
	}
}