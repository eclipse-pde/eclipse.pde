/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.processors;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * Abstract class of all specific completion (tag, attribute etc.) processors.
 */
public abstract class DelegateProcessor {

	/**
	 * Method to be implemented by specific completion processors.
	 *
	 * @return Completion proposals
	 */
	public abstract ICompletionProposal[] getCompletionProposals();
}
