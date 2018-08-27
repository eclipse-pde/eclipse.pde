/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
