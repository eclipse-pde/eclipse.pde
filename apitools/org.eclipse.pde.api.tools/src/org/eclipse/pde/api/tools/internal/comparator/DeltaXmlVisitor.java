/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
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
	/**
	 * XML doc being generated
	 */
	private final Document fDoc;

	/**
	 * Top deltas element
	 */
	private final Element fDeltas;

	/**
	 * Constructs a new visitor for the given component.
	 *
	 * @throws CoreException if unable to construct the visitor
	 */
	public DeltaXmlVisitor() throws CoreException {
		fDoc = Util.newDocument();
		fDeltas = fDoc.createElement(IApiXmlConstants.DELTAS_ELEMENT_NAME);
		fDoc.appendChild(fDeltas);
	}

	@Override
	public boolean visit(IDelta delta) {
		if (delta == ApiComparator.NO_DELTA) {
			return false;
		}
		if (delta.getChildren().length == 0) {
			processLeafDelta(delta);
		}
		return true;
	}

	protected void processLeafDelta(IDelta delta) {
		Element deltaElement = fDoc.createElement(IApiXmlConstants.DELTA_ELEMENT_NAME);
		deltaElement.setAttribute(IApiXmlConstants.ATTR_FLAGS, Integer.toString(delta.getFlags()));
		deltaElement.setAttribute(IApiXmlConstants.ATTR_KIND, Util.getDeltaKindName(delta));
		deltaElement.setAttribute(IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE, Util.getDeltaElementType(delta));
		deltaElement.setAttribute(IApiXmlConstants.ATTR_KEY, delta.getKey());
		String typeName = delta.getTypeName();
		if (typeName != null) {
			deltaElement.setAttribute(IApiXmlConstants.ATTR_NAME_TYPE_NAME, typeName);
		}
		deltaElement.setAttribute(IApiXmlConstants.ATTR_NAME_COMPATIBLE, Boolean.toString(DeltaProcessor.isCompatible(delta)));
		deltaElement.setAttribute(IApiXmlConstants.ATTR_NAME_OLD_MODIFIERS, Integer.toString(delta.getOldModifiers()));
		deltaElement.setAttribute(IApiXmlConstants.ATTR_NAME_NEW_MODIFIERS, Integer.toString(delta.getNewModifiers()));
		deltaElement.setAttribute(IApiXmlConstants.ATTR_RESTRICTIONS, Integer.toString(delta.getCurrentRestrictions()));
		String apiComponentID = delta.getComponentVersionId();
		if (apiComponentID != null) {
			deltaElement.setAttribute(IApiXmlConstants.ATTR_NAME_COMPONENT_ID, apiComponentID);
		}
		deltaElement.setAttribute(IApiXmlConstants.ATTR_MESSAGE, delta.getMessage());
		String[] messageArguments = delta.getArguments();
		int length = messageArguments.length;
		if (length > 0) {
			Element messageArgumentsElement = fDoc.createElement(IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS);
			for (int j = 0; j < length; j++) {
				Element messageArgumentElement = fDoc.createElement(IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENT);
				messageArgumentElement.setAttribute(IApiXmlConstants.ATTR_VALUE, String.valueOf(messageArguments[j]));
				messageArgumentsElement.appendChild(messageArgumentElement);
			}
			deltaElement.appendChild(messageArgumentsElement);
		}
		fDeltas.appendChild(deltaElement);
	}

	@Override
	public void endVisit(IDelta delta) {
		// nothing to do
	}

	/**
	 * Return the xml dom document this visitor generates. Use
	 * {@link Util#serializeDocument(Document)} to get the serialized xml string.
	 *
	 * @return xml dom document
	 */
	public Document getDocument() {
		return fDoc;
	}
}
