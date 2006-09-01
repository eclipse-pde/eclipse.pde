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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.editor.text.PDETextHover;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class ExtensionAttributeRow {
	protected IContextPart part;
	protected Object att;
	protected IPluginElement input;
	protected boolean blockNotification;
	protected boolean dirty;
	protected IInformationControl fIC;
	
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
		addHoverListener(label);
	}
	
	protected void addHoverListener(final Control label) {
		String text = getDescription();
		if (text == null || text.trim().length() == 0)
			return;
		fIC = PDETextHover.getInformationControlCreator().createInformationControl(label.getShell());
		fIC.setSizeConstraints(300, 500);
		fIC.setInformation(text);
		Point p = fIC.computeSizeHint();
		fIC.setSize(p.x, p.y);
		label.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent e) {
			}
			public void mouseExit(MouseEvent e) {
				fIC.setVisible(false);
			}
			public void mouseHover(MouseEvent e) {
				fIC.setLocation(label.toDisplay(new Point(10, 25)));
				fIC.setVisible(true);
			}
		});
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
		if (fIC != null)
			fIC.dispose();
	}

	public void setInput(IPluginElement input) {
		this.input = input;
		update();
	}
	protected IProject getProject() {
		return part.getPage().getPDEEditor().getCommonProject();
	}
}
