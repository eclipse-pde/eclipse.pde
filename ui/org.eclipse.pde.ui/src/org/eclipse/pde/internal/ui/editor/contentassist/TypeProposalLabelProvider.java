/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * TypeProposalLabelProvider
 *
 */
public class TypeProposalLabelProvider extends LabelProvider {

	/**
	 *
	 */
	public TypeProposalLabelProvider() {
		// NO-OP
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof TypeContentProposal) {
			return ((TypeContentProposal) element).getImage();
		}
		return null;
	}

}
