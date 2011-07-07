/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.validation;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessageManager;

public interface IValidatorMessageHandler {

	/**
	 * Report a message with a given severity and associate it with a key
	 * @param key
	 * @param messageText
	 * @param messageType
	 */
	public void addMessage(Object key, String messageText, int messageType);

	/**
	 * Report a message with a given severity.  The message will be associated
	 * with a default message key
	 * @param messageText
	 * @param messageType
	 */
	public void addMessage(String messageText, int messageType);

	/**
	 * Remove a previously reported message associated with the given key
	 * @param key
	 */
	public void removeMessage(Object key);

	/**
	 * Set an optional message prefix to be prepended to all messages on report
	 * @param prefix
	 */
	public void setMessagePrefix(String prefix);

	/**
	 * Get the managed form.
	 */
	public IManagedForm getManagedForm();

	/**
	 * Get the message manager.
	 */
	public IMessageManager getMessageManager();

	/**
	 * Get the optional message prefix.
	 */
	public String getMessagePrefix();

}
