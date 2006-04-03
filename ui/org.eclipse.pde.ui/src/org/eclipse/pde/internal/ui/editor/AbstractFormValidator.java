/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public abstract class AbstractFormValidator implements IEditorValidator {

	private boolean fEnabled;
	private boolean fInputValidates = true;
	private String fMessage = PDEUIMessages.AbstractFormValidator_noMessageSet;
	private int fSeverity = IMessageProvider.WARNING;
	private PDESection fSection;
	
	public AbstractFormValidator(PDESection section) {
		fSection = section;
		fEnabled = fSection.isEditable();
	}
	
	public boolean isEnabled() {
		return fEnabled;
	}

	public void setEnabled(boolean enable) {
		if (fSection.isEditable()) {
			fEnabled = enable;
			validate(enable);
		}
	}
	
	public void setMessage(String message) {
		fMessage = message != null ? message : PDEUIMessages.AbstractFormValidator_noMessageSet;
	}
	
	public String getMessage() {
		StringBuffer buff = new StringBuffer();
		if (!fSection.getPage().isActive()) {
			buff.append('[');
			buff.append(fSection.getPage().getTitle());
			buff.append(']');
		}
		buff.append('[');
		buff.append(fSection.getSection().getText());
		buff.append(']');
		buff.append(' ');
		buff.append(fMessage);
		return buff.toString();
	}
	
	public void setSeverity(int severity) {
		if (severity >= 0)
			fSeverity = severity;
	}
	
	public int getSeverity() {
		return fSeverity;
	}
	
	public PDESection getSection() {
		return fSection;
	}
	
	public boolean markedInvalid() {
		return isEnabled() && !fInputValidates;
	}
	
	public final boolean validate(boolean revalidate) {
		IEditorValidationStack stack = fSection.getPage().getPDEEditor().getValidationStack();
		if (!isEnabled()) {
			stack.top();
			return true;
		}
		if (revalidate) {
			fInputValidates = inputValidates();
			if (fInputValidates)
				stack.top();
			else
				stack.push(this);
		}
		return fInputValidates;
	}
}
