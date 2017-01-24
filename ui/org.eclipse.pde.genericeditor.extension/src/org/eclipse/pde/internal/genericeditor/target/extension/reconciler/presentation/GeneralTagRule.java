/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

/**
 * Simple tag matcher
 */
public class GeneralTagRule implements IRule {

	private IToken tagToken = new Token(new TextAttribute(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_CYAN)));

	@Override
	public IToken evaluate(ICharacterScanner scanner) {

		int c = scanner.read();
		if (c == '>') {
			return tagToken;
		}
		if (c == '/') {
			int d = scanner.read();
			if (d == '>')
				return tagToken;
			scanner.unread();
		}
		scanner.unread();
		return Token.UNDEFINED;
	}

}
