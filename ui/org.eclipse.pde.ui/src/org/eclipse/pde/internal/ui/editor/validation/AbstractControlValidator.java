/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.validation;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;

/**
 * AbstractControlValidator
 *
 */
public abstract class AbstractControlValidator implements IControlValidator, IValidatorMessageHandler {

	public static final Object F_DEFAULT_MESSAGE_KEY = "k"; //$NON-NLS-1$

	private boolean fEnabled;

	private IManagedForm fManagedForm;

	private Control fControl;

	private String fMessagePrefix;

	private boolean fIsValid;

	private IProject fProject;

	/**
	 * @param managedForm
	 * @param control
	 * @param project
	 */
	public AbstractControlValidator(IManagedForm managedForm, Control control, IProject project) {
		fProject = project;
		fManagedForm = managedForm;
		fControl = control;
		fMessagePrefix = null;
		fEnabled = autoEnable();
		reset();
	}

	protected boolean autoEnable() {
		boolean isBinaryProject = WorkspaceModelManager.isBinaryProject(fProject);
		// Enable validator if this is a source projec, the control is enabled 
		// and the control is not disposed  
		if ((isBinaryProject == false) && fControl.getEnabled() && (fControl.isDisposed() == false)) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IControlValdiator#getEnabled()
	 */
	public boolean getEnabled() {
		return fEnabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IControlValdiator#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		// Nothing to do here if enablement is not being changed
		if (enabled == fEnabled) {
			return;
		}
		// Update enablement
		fEnabled = enabled;
		// Automatically perform actions depending on enablement
		if (fEnabled) {
			// Re-validate control if validator is enabled
			validate();
		} else {
			// Reset validation state if validator is disabled
			reset();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.IControlValdiator#validate()
	 */
	public boolean validate() {
		// Skip validation if the validator is disabled
		if (fEnabled == false) {
			return fIsValid;
		}
		// Validate the control
		fIsValid = validateControl();
		// If the control is valid, remove all the messages associated with 
		// the control (in case they were not individually removed by the
		// child class)
		if (fIsValid) {
			fManagedForm.getMessageManager().removeMessages(fControl);
		}
		return fIsValid;
	}

	protected abstract boolean validateControl();

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IValidatorMessageHandler#addMessage(java.lang.Object, java.lang.String, int)
	 */
	public void addMessage(Object key, String messageText, int messageType) {
		// Add a prefix, if one was specified
		if (fMessagePrefix != null) {
			messageText = fMessagePrefix + ' ' + messageText;
		}
		// Delegate to message manager
		fManagedForm.getMessageManager().addMessage(key, messageText, null, messageType, fControl);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IValidatorMessageHandler#addMessage(java.lang.String, int)
	 */
	public void addMessage(String messageText, int messageType) {
		// Add a prefix, if one was specified
		if (fMessagePrefix != null) {
			messageText = fMessagePrefix + ' ' + messageText;
		}
		// Delegate to message manager
		fManagedForm.getMessageManager().addMessage(F_DEFAULT_MESSAGE_KEY, messageText, null, messageType, fControl);
	}

	public static int getMessageType(IStatus status) {
		int severity = status.getSeverity();
		// Translate severity to the equivalent message provider type
		if (severity == IStatus.OK) {
			return IMessageProvider.NONE;
		} else if (severity == IStatus.ERROR) {
			return IMessageProvider.ERROR;
		} else if (severity == IStatus.WARNING) {
			return IMessageProvider.WARNING;
		} else if (severity == IStatus.INFO) {
			return IMessageProvider.INFORMATION;
		}
		// IStatus.CANCEL
		return IMessageProvider.NONE;
	}

	public static int getMessageType(IProject project, String compilerFlagId) {
		int severity = CompilerFlags.getFlag(project, compilerFlagId);
		// Translate severity to the equivalent message provider type
		if (severity == CompilerFlags.IGNORE) {
			return IMessageProvider.NONE;
		} else if (severity == CompilerFlags.ERROR) {
			return IMessageProvider.ERROR;
		} else {
			// CompilerFlags.WARNING
			return IMessageProvider.WARNING;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IValidatorMessageHandler#removeMessage(java.lang.Object)
	 */
	public void removeMessage(Object key) {
		fManagedForm.getMessageManager().removeMessage(key, fControl);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IValidatorMessageHandler#setMessagePrefix(java.lang.String)
	 */
	public void setMessagePrefix(String prefix) {
		fMessagePrefix = prefix;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IValidatorMessageHandler#getMessagePrefix()
	 */
	public String getMessagePrefix() {
		return fMessagePrefix;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IValidatorMessageHandler#getManagedForm()
	 */
	public IManagedForm getManagedForm() {
		return fManagedForm;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IValidatorMessageHandler#getMessageManager()
	 */
	public IMessageManager getMessageManager() {
		return fManagedForm.getMessageManager();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IControlValidator#setRefresh(boolean)
	 */
	public void setRefresh(boolean refresh) {
		getMessageManager().setAutoUpdate(refresh);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IControlValdiator#getControl()
	 */
	public Control getControl() {
		return fControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IControlValidator#isValid()
	 */
	public boolean isValid() {
		return fIsValid;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.validation.IControlValidator#reset()
	 */
	public void reset() {
		fIsValid = true;
		fManagedForm.getMessageManager().removeMessages(fControl);
	}
}
