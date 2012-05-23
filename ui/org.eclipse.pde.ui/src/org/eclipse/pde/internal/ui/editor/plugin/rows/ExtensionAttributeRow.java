/*******************************************************************************
 *  Copyright (c) 2003, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin.rows;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.editor.text.IControlHoverContentProvider;
import org.eclipse.pde.internal.ui.editor.text.PDETextHover;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.keys.IBindingService;

public abstract class ExtensionAttributeRow implements IControlHoverContentProvider {
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
		return (att instanceof ISchemaAttribute) ? (ISchemaAttribute) att : null;
	}

	public String getName() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute) att).getName();

		return ((IPluginAttribute) att).getName();
	}

	protected int getUse() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute) att).getUse();
		return ISchemaAttribute.OPTIONAL;
	}

	protected String getDescription() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute) att).getDescription();
		return ""; //$NON-NLS-1$
	}

	protected String getValue() {
		String value = ""; //$NON-NLS-1$
		if (input != null) {
			IPluginAttribute patt = input.getAttribute(getName());
			if (patt != null)
				value = patt.getValue();
		}
		return value;
	}

	protected String getPropertyLabel() {
		String label = getName();
		if (getUse() == ISchemaAttribute.REQUIRED)
			label += "*:"; //$NON-NLS-1$
		else
			label += ":"; //$NON-NLS-1$
		return label;
	}

	protected void createLabel(Composite parent, FormToolkit toolkit) {
		Label label = toolkit.createLabel(parent, getPropertyLabel(), SWT.NULL);
		label.setForeground(toolkit.getColors().getColor(IFormColors.TITLE));
		PDETextHover.addHoverListenerToControl(fIC, label, this);
	}

	/**
	 * @param control
	 */
	protected void createTextHover(Control control) {
		fIC = PDETextHover.getInformationControlCreator().createInformationControl(control.getShell());
		fIC.setSizeConstraints(300, 600);
	}

	public String getHoverContent(Control c) {
		if (c instanceof Label || c instanceof Hyperlink) {
			// reveal keybinding for shortcut to filtering
			String filterBinding = ((IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class)).getBestActiveBindingFormattedFor(ActionFactory.FIND.getCommandId());
			String findKeybinding = (getValue().length() > 0) ? "<br><br>Press " + filterBinding + " within text to filter for this attribute." : ""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			String description = getDescription().trim(); // prettify help text
			if (description.length() > 0) {
				String first = String.valueOf(description.charAt(0)); // always make first letter uppercase
				return getDescription().replaceFirst(first, first.toUpperCase()) + findKeybinding;
			}
			return description;
		}
		if (c instanceof Text) {
			String text = ((Text) c).getText();
			ISchemaAttribute sAtt = getAttribute();
			String translated = null;
			if (input != null && sAtt != null && sAtt.isTranslatable() && text.startsWith("%")) { //$NON-NLS-1$
				translated = input.getResourceString(text);
			}
			if (!text.equals(translated)) {
				return translated;
		}
		}
		return null;
	}

	/**
	 * @param parent
	 * @param toolkit
	 * @param span
	 */
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		createTextHover(parent);
	}

	protected abstract void update();

	public abstract void commit();

	public abstract void setFocus();

	public boolean isDirty() {
		return dirty;
	}

	protected void markDirty() {
		dirty = true;
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
