/*******************************************************************************
 *  Copyright (c) 2007, 2015 IBM Corporation and others.
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

import java.util.Comparator;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;

public class XMLElementProposalComparator implements Comparator<Object> {

	public XMLElementProposalComparator() {
		// NO-OP
	}

	@Override
	public int compare(Object object1, Object object2) {
		String proposal1 = getSortKey((ISchemaElement) object1);
		String proposal2 = getSortKey((ISchemaElement) object2);

		return proposal1.compareToIgnoreCase(proposal2);
	}

	private String getSortKey(ISchemaElement proposal) {
		return proposal.getName();
	}

}
