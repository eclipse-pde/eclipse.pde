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
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IRegion;

public abstract class ManifestElementHyperlink extends AbstractHyperlink {

	public ManifestElementHyperlink(IRegion region, String element) {
		super(region, element);
	}

	protected abstract void open2();

	public void open() {
		// remove whitespace inbetween chars
		int len = fElement.length();
		StringBuffer sb = new StringBuffer(len);
		for (int i = 0; i < len; i++) {
			char c = fElement.charAt(i);
			if (!Character.isWhitespace(c))
				sb.append(c);
		}
		fElement = sb.toString();
		open2();
	}
}
