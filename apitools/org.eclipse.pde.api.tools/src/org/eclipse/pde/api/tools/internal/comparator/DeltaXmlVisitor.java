/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Delta visitor that generates XML for the delta.
 *  
 * @since 1.0.0
 */
public class DeltaXmlVisitor extends DeltaVisitor {
	public static final String DELTAS_ELEMENT_NAME = "deltas"; //$NON-NLS-1$
	public static final String DELTA_ELEMENT_NAME = "delta"; //$NON-NLS-1$
	public static final String ATTR_NAME_FLAGS = "flags"; //$NON-NLS-1$
	public static final String ATTR_NAME_KIND = "kind"; //$NON-NLS-1$
	public static final String ATTR_NAME_ELEMENT_TYPE = "element_type"; //$NON-NLS-1$
	public static final String ATTR_NAME_KEY = "key"; //$NON-NLS-1$
	public static final String ATTR_NAME_TYPE_NAME = "type_name"; //$NON-NLS-1$
	public static final String ATTR_NAME_BINARY_COMPATIBLE = "binary_compatible"; //$NON-NLS-1$

	/**
	 * XML doc being generated
	 */
	private Document fDoc;

	/**
	 * Top deltas element
	 */
	private Element fDeltas;
	

	/**
	 * Constructs a new visitor for the given component.
	 * 
	 * @param component API component
	 * @throws CoreException if unable to construct the visitor
	 */
	public DeltaXmlVisitor() throws CoreException {
		fDoc = Util.newDocument();
		fDeltas = fDoc.createElement(DELTAS_ELEMENT_NAME);
		fDoc.appendChild(fDeltas);
	}
	
	public boolean visit(IDelta delta) {
		if (delta.getChildren().length == 0) {
			Element deltaElement = fDoc.createElement(DELTA_ELEMENT_NAME);
			deltaElement.setAttribute(ATTR_NAME_FLAGS, Integer.toString(delta.getFlags()));
			deltaElement.setAttribute(ATTR_NAME_KIND, Util.getDeltaKindName(delta));
			deltaElement.setAttribute(ATTR_NAME_ELEMENT_TYPE, Util.getDeltaElementType(delta));
			deltaElement.setAttribute(ATTR_NAME_KEY, delta.getKey());
			deltaElement.setAttribute(ATTR_NAME_TYPE_NAME, delta.getTypeName());
			deltaElement.setAttribute(ATTR_NAME_BINARY_COMPATIBLE, Boolean.toString(DeltaProcessor.isBinaryCompatible(delta)));
			fDeltas.appendChild(deltaElement);
		}
		return true;
	}

	public void endVisit(IDelta delta) {
		// nothing to do
	}
	
	/**
	 * Returns the settings as a UTF-8 string containing XML.
	 * 
	 * @return XML
	 * @throws CoreException if something goes wrong 
	 */
	public String getXML() throws CoreException {
		return Util.serializeDocument(fDoc);
	}
}
