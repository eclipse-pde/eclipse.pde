/*******************************************************************************
 *  Copyright (c) 2003, 2016 IBM Corporation and others.
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
 *     Brian de Alwis (MTI) - bug 429420
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin.rows;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEUIMessages;
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

	protected boolean isDeprecated() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute) att).isDeprecated();
		return false;
	}

	protected boolean isRequired() {
		if (att instanceof ISchemaAttribute)
			return ((ISchemaAttribute) att).getUse() == ISchemaAttribute.REQUIRED;
		return false;
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
		if (isDeprecated()) {
			if (isRequired()) {
				return NLS.bind(PDEUIMessages.ExtensionAttributeRow_AttrLabelReqDepr, label);
			}
			return NLS.bind(PDEUIMessages.ExtensionAttributeRow_AttrLabelDepr, label);
		}
		if (isRequired()) {
			return NLS.bind(PDEUIMessages.ExtensionAttributeRow_AttrLabelReq, label);
		}
		return NLS.bind(PDEUIMessages.ExtensionAttributeRow_AttrLabel, label);
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

	@Override
	public String getHoverContent(Control c) {
		if (c instanceof Label || c instanceof Hyperlink) {
			StringBuilder result = new StringBuilder();
			ISchemaAttribute attribute = getAttribute();
			if (attribute != null && attribute.getDescription().length() > 0) {
				// Append required/deprecated info
				if (isDeprecated()) {
					if (isRequired()) {
						result.append(NLS.bind(PDEUIMessages.ExtensionAttributeRow_AttrReqDepr, attribute.getDescription()));
					} else {
						result.append(NLS.bind(PDEUIMessages.ExtensionAttributeRow_AttrDepr, attribute.getDescription()));
					}
				} else if (isRequired()) {
					result.append(NLS.bind(PDEUIMessages.ExtensionAttributeRow_AttrReq, attribute.getDescription()));
				} else {
					result.append(attribute.getDescription());
				}

				// Append keybinding for filtering by attribute
				if (getValue().length() > 0) {
					String filterBinding = PlatformUI.getWorkbench().getAdapter(IBindingService.class)
							.getBestActiveBindingFormattedFor(ActionFactory.FIND.getCommandId());
					result.append("<br><br>"); //$NON-NLS-1$
					result.append(NLS.bind(PDEUIMessages.ExtensionAttributeRow_AttrFilter, filterBinding));
				}
			}
			return result.toString();
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
