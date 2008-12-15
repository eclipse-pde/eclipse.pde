/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.builders;

import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.w3c.dom.Element;

public class ElementOccurrenceResult {

	private Element fElement;
	private ISchemaElement fSchemaElement;
	private int fActualOccurrences;
	private int fAllowedOccurrences;

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
