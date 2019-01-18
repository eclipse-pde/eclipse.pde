/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.builders;

import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.w3c.dom.Element;

public class ElementOccurrenceResult {

	private final Element fElement;
	private final ISchemaElement fSchemaElement;
	private final int fActualOccurrences;
	private final int fAllowedOccurrences;

	public ElementOccurrenceResult(Element element, ISchemaElement schemaElement, int actualOccurrences, int allowedOccurrences) {
		fElement = element;
		fActualOccurrences = actualOccurrences;
		fAllowedOccurrences = allowedOccurrences;
		fSchemaElement = schemaElement;
	}

	public ISchemaElement getSchemaElement() {
		return fSchemaElement;
	}

	public Element getElement() {
		return fElement;
	}

	public int getActualOccurrences() {
		return fActualOccurrences;
	}

	public int getAllowedOccurrences() {
		return fAllowedOccurrences;
	}

}
