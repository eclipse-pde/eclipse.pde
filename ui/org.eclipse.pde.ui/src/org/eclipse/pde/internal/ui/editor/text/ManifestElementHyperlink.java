/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IRegion;

public abstract class ManifestElementHyperlink extends AbstractHyperlink {

	public ManifestElementHyperlink(IRegion region, String element) {
		super(region, element);
	}

	protected abstract void open2();

	@Override
	public void open() {
		// remove whitespace inbetween chars
		int len = fElement.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = fElement.charAt(i);
			if (!Character.isWhitespace(c))
				sb.append(c);
		}
		fElement = sb.toString();
		open2();
	}
}
