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

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.Comparator;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;

public class XMLElementProposalComparator implements Comparator {

	public XMLElementProposalComparator() {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object object1, Object object2) {
		String proposal1 = getSortKey((ISchemaElement) object1);
		String proposal2 = getSortKey((ISchemaElement) object2);

		return proposal1.compareToIgnoreCase(proposal2);
	}

	private String getSortKey(ISchemaElement proposal) {
		return proposal.getName();
	}

}
