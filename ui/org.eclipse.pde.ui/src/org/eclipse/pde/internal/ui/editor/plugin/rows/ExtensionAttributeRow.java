/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin.rows;

import com.ibm.icu.text.BreakIterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class ExtensionAttributeRow {
	private static final int TOOLTIP_WIDTH_LIMIT = 300;
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
		label.setToolTipText(getToolTipText(label));
	}
	
	protected String getToolTipText(Control control) {
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
			return wrapText(control, buf.toString(), TOOLTIP_WIDTH_LIMIT);
		}
		return text;
	}
	
	private String wrapText(Control c, String src, int width) {
		BreakIterator wb = BreakIterator.getWordInstance();
		wb.setText(src);
		int saved = 0;
		int last = 0;
		StringBuffer buff = new StringBuffer();
		GC gc = new GC(c);
		
		for (int loc = wb.first(); loc != BreakIterator.DONE; loc = wb.next()) {
			String word = src.substring(saved, loc);
			Point extent = gc.textExtent(word);
			if (extent.x > width) {
				// overflow
				String prevLine = src.substring(saved, last);
				buff.append(prevLine);
				buff.append(SWT.LF);
				saved = last;
			}
			last = loc;
		}
		String lastLine = src.substring(saved, last);
		buff.append(lastLine);
		return buff.toString();
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
