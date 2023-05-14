/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.iproduct.IIntroInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class IntroInfo extends ProductObject implements IIntroInfo {

	public static final String P_INTRO_ID = "introId"; //$NON-NLS-1$
	private static final long serialVersionUID = 1L;
	private String fIntroId;

	public IntroInfo(IProductModel model) {
		super(model);
	}

	@Override
	public void setId(String id) {
		String old = fIntroId;
		fIntroId = id;
		if (isEditable()) {
			firePropertyChanged(P_INTRO_ID, old, fIntroId);
		}
	}

	@Override
	public String getId() {
		return fIntroId;
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fIntroId = element.getAttribute(P_INTRO_ID);
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		if (fIntroId != null && fIntroId.length() > 0) {
			writer.println(indent + "<intro " + P_INTRO_ID + "=\"" + getWritableString(fIntroId) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

}
