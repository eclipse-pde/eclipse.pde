/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.validation.AbstractControlValidator;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;

/**
 * ImageEntryValidator
 * This class serves as a bridge between the old editor validator code 
 * (AbstractFormValidator) and the new validator (AbstractControlValidator).
 * This class really should not exist; but, to reuse functionality provided
 * by EditorUtilities it was necessary.
 */
public abstract class ImageEntryValidator extends AbstractControlValidator {

	private static final Object F_MESSAGE_KEY = "k"; //$NON-NLS-1$
	
	/**
	 * @param managedForm
	 * @param control
	 * @param prefix
	 */
	public ImageEntryValidator(IManagedForm managedForm, Control control,
			String prefix) {
		super(managedForm, control, prefix);
	}

	/**
	 * @param managedForm
	 * @param control
	 */
	public ImageEntryValidator(IManagedForm managedForm, Control control) {
		super(managedForm, control);
	}

	/**
	 * @param message
	 * @param severity
	 * @param writeField
	 * @param provider
	 */
	public void handleMessage(String message, int severity, boolean writeField,
			FormEntry provider) {
		if (message != null) {
			StringBuffer sb = new StringBuffer();
			if (writeField) {
				sb.append(provider.getText().getText());
				sb.append(" - "); //$NON-NLS-1$
			}
			sb.append(message);

			addMessage(F_MESSAGE_KEY, sb.toString(), severity);
		}
	}	
	
}
