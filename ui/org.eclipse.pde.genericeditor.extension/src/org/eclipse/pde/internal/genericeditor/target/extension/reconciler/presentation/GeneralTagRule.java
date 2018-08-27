/*******************************************************************************
 * Copyright (c) 2016, 2018 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 531210] Target File Source Editor unreadable with dark theme
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.reconciler.presentation;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.ui.PlatformUI;

/**
 * Simple tag matcher
 */
public class GeneralTagRule implements IRule {
	private IToken tagToken = new Token(new TextAttribute(PlatformUI.getWorkbench().getThemeManager().getCurrentTheme()
			.getColorRegistry().get(IGETEColorConstants.P_TAG)));

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
