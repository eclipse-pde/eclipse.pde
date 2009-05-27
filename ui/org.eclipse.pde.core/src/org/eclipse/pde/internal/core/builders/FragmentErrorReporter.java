/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class FragmentErrorReporter extends PluginBaseErrorReporter {

	public FragmentErrorReporter(IFile file) {
		super(file);
	}

	/* (non-Javadoc)
	* @see org.eclipse.pde.internal.builders.PluginBaseErrorReporter#validateTopLevelAttributes(org.w3c.dom.Element)
	*/
	protected void validateTopLevelAttributes(Element element) {
		super.validateTopLevelAttributes(element);

		if (assertAttributeDefined(element, "plugin-id", CompilerFlags.ERROR)) //$NON-NLS-1$
			validatePluginIDRef(element, element.getAttributeNode("plugin-id")); //$NON-NLS-1$

		if (assertAttributeDefined(element, "plugin-version", CompilerFlags.ERROR)) //$NON-NLS-1$
			validateVersionAttribute(element, element.getAttributeNode("plugin-version")); //$NON-NLS-1$

		Attr attr = element.getAttributeNode("match"); //$NON-NLS-1$
		if (attr != null)
			validateMatch(element, attr);
	}

	protected String getRootElementName() {
		return "fragment"; //$NON-NLS-1$
	}

}
