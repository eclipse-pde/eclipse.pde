/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;

/**
 * TypeFieldAssistDisposer
 */
public class TypeFieldAssistDisposer {

	private final ContentAssistCommandAdapter fAdapter;

	private final TypeContentProposalListener fListener;

	public TypeFieldAssistDisposer(ContentAssistCommandAdapter adapter, TypeContentProposalListener listener) {
		fAdapter = adapter;
		fListener = listener;
	}

	public void dispose() {
		if (fAdapter == null) {
			return;
		}
		// Dispose of the label provider
		ILabelProvider labelProvider = fAdapter.getLabelProvider();
		if ((labelProvider != null)) {
			fAdapter.setLabelProvider(null);
			labelProvider.dispose();
		}
		// Remove the listeners
		if (fListener != null) {
			fAdapter.removeContentProposalListener((IContentProposalListener) fListener);
			fAdapter.removeContentProposalListener((IContentProposalListener2) fListener);
		}
	}

}
