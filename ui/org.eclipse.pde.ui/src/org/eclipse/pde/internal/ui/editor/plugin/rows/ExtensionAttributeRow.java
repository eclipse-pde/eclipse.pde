/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.plugin.rows;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.editor.*;
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
	protected IContextPart part;
	protected Object att;
	protected IPluginElement input;
	protected boolean blockNotification;
	protected boolean dirty;
	
	public ExtensionAttributeRow(IContextPart part, ISchemaAttribute att) {
		this.part = part;
		this.att = att;
	}
	
	public ExtensionAttributeRow(IContextPart part, IPluginAttribute att) {
		this.part = part;
		this.att = att;
	}
	
	public ISchemaAttribute getAttribute() {
		return (att instanceof ISchemaAttribute) ? (ISchemaAttribute)att:null;
	}
	
	public String getName() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute)att).getName();
		else
			return ((IPluginAttribute)att).getName();
	}
	
	protected int getUse() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute)att).getUse();
		return ISchemaAttribute.OPTIONAL;
	}
	
	protected String getDescription() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute)att).getDescription();
		return ""; //$NON-NLS-1$
	}
	
	protected String getValue() {
		String value= ""; //$NON-NLS-1$
		if (input!=null) {
			IPluginAttribute patt = input.getAttribute(getName());
			if (patt!=null)
				value = patt.getValue();
		}
		return value;
	}
	protected String getPropertyLabel() {
		String label=getName();
		if (getUse()==ISchemaAttribute.REQUIRED)
			label+= "*:"; //$NON-NLS-1$
		else
			label+=":"; //$NON-NLS-1$
		return label;
	}
	protected void createLabel(Composite parent, FormToolkit toolkit) {
		Label label = toolkit.createLabel(parent, getPropertyLabel(), SWT.NULL);
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		label.setToolTipText(getToolTipText());
	}
	
	protected String getToolTipText() {
		String text = getDescription();
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
	public abstract void commit();

	public abstract void setFocus();
	
	public boolean isDirty() {
		return dirty;
	}

	protected void markDirty() {
		dirty=true;
		part.fireSaveNeeded();
	}

	public void dispose() {
	}

	public void setInput(IPluginElement input) {
		this.input = input;
		update();
	}
	protected IProject getProject() {
		return part.getPage().getPDEEditor().getCommonProject();
	}
}