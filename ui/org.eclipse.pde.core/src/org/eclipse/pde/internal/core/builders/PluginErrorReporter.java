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
package org.eclipse.pde.internal.core.builders;

import org.eclipse.core.resources.IFile;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class PluginErrorReporter extends PluginBaseErrorReporter {

	public PluginErrorReporter(IFile file) {
		super(file);
	}

	@Override
	protected void validateTopLevelAttributes(Element element) {
		super.validateTopLevelAttributes(element);
		Attr attr = element.getAttributeNode("class"); //$NON-NLS-1$
		if (attr != null) {
			validateJavaAttribute(element, attr);
		}
	}

	@Override
	protected String getRootElementName() {
		return "plugin"; //$NON-NLS-1$
	}

}
