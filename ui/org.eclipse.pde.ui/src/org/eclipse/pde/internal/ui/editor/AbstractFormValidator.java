package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public abstract class AbstractFormValidator implements IEditorValidator {

	private boolean fEnabled = true;
	private boolean fInputValidates;
	private String fMessage = PDEUIMessages.AbstractFormValidator_noMessageSet;
	private String fMessagePrefix;
	private int fSeverity = -1;
	private PDESection fSection;
	
	public AbstractFormValidator(PDESection section) {
		fSection = section;
	}
	
	public boolean isEnabled() {
		return fEnabled;
	}

	public void setEnabled(boolean enable) {
		fEnabled = enable;
	}
	
	public void setMessage(String message) {
		fMessage = message;
	}
	
	public String getMessage() {
		if (fMessagePrefix == null)
			fMessagePrefix = "[" + fSection.getSection().getText() + "] "; //$NON-NLS-1$ //$NON-NLS-2$
		return fMessagePrefix + fMessage;
	}
	
	public void setSeverity(int severity) {
		fSeverity = severity;
	}
	
	public int getSeverity() {
		return fSeverity < 0 ? IMessageProvider.ERROR : fSeverity;
	}
	
	public PDESection getSection() {
		return fSection;
	}
	public final boolean validate(boolean revalidate) {
		// TODO
		// move stack to section
		if (revalidate && isEnabled()) {
			fInputValidates = inputValidates();
			if (fInputValidates)
				fSection.getPage().getPDEEditor().getValidationStack().top(this);
			else
				fSection.getPage().getPDEEditor().getValidationStack().push(this);
		}
		return fInputValidates;
	}
}
