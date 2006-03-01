package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public abstract class AbstractFormValidator implements IEditorValidator {

	private static final int DEFAULT_SEVERITY = IMessageProvider.WARNING;
	private boolean fEnabled;
	private boolean fInputValidates = true;
	private String fMessage = PDEUIMessages.AbstractFormValidator_noMessageSet;
	private int fSeverity = -1;
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
	
	public String getMessage(boolean includePageName) {
		StringBuffer buff = new StringBuffer();
		if (includePageName) {
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
		fSeverity = severity;
	}
	
	public int getSeverity() {
		return fSeverity < 0 ? DEFAULT_SEVERITY : fSeverity;
	}
	
	public PDESection getSection() {
		return fSection;
	}
	public final boolean validate(boolean revalidate) {
		if (!isEnabled()) // if validator is disabled return true
			return true;
		if (revalidate) {
			fInputValidates = inputValidates();
			IEditorValidationStack stack = fSection.getPage().getPDEEditor().getValidationStack();
			if (fInputValidates)
				stack.top(this, fSection.getPage());
			else
				stack.push(this);
		}
		return fInputValidates;
	}
}
