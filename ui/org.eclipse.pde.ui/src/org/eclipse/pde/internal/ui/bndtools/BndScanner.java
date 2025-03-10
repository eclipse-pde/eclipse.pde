/*******************************************************************************
 * Copyright (c) 2011, 2023 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     David Savage <davemssavage@gmail.com> - initial API and implementation
 *     Neil Bartlett <njbartlett@gmail.com> - ongoing enhancements
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Amit Kumar Mondal <admin@amitinside.com> - ongoing enhancements
 *     Peter Kriens <Peter.Kriens@aQute.biz> - ongoing enhancements
 *     Christoph Läubrich - incline code from BndSourceViewerConfiguration regarding colors, adjust to eclipse coding conventions
 *******************************************************************************/
package org.eclipse.pde.internal.ui.bndtools;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.ui.text.IColorManager;
import org.eclipse.jdt.ui.text.IJavaColorConstants;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;

import aQute.bnd.help.Syntax;
import aQute.bnd.osgi.Constants;

public class BndScanner extends RuleBasedScanner {
	final Set<String>				instructions;
	final Set<String>				directives	= new HashSet<>();
	private final Token T_DEFAULT;
	private final Token T_KEY;
	private final Token T_ERROR;
	private final Token T_COMMENT;
	private final Token T_INSTRUCTION;
	private final Token T_OPTION;

	public BndScanner(IColorManager colorManager) {
		T_DEFAULT = new Token(new TextAttribute(colorManager.getColor(IJavaColorConstants.JAVA_DEFAULT)));
		T_KEY = new Token(new TextAttribute(colorManager.getColor(IJavaColorConstants.JAVADOC_LINK), null, SWT.NONE));
		T_ERROR = new Token(new TextAttribute(colorManager.getColor(IJavaColorConstants.JAVA_KEYWORD),
				colorManager.getColor(new RGB(255, 0, 0)), SWT.BOLD));
		T_COMMENT = new Token(new TextAttribute(colorManager.getColor(IJavaColorConstants.JAVA_SINGLE_LINE_COMMENT)));
		T_INSTRUCTION = new Token(
				new TextAttribute(colorManager.getColor(IJavaColorConstants.JAVADOC_KEYWORD), null, SWT.BOLD));
		T_OPTION = new Token(
				new TextAttribute(colorManager.getColor(IJavaColorConstants.JAVADOC_LINK), null, SWT.BOLD));
		instructions = Syntax.HELP.values()
			.stream()
			.map(Syntax::getHeader)
			.collect(Collectors.toSet());

		directives.addAll(Constants.directives);
		directives.addAll(Constants.COMPONENT_DIRECTIVES);
		directives.addAll(Constants.COMPONENT_DIRECTIVES_1_1);
		directives.addAll(Constants.COMPONENT_DIRECTIVES_1_2);

		IRule[] rules = new IRule[] {
			this::comment, //
			this::keyword, //
			this::error,
		};

		setRules(rules);
		setDefaultReturnToken(T_DEFAULT);
	}

	IToken comment(ICharacterScanner scanner) {
		if (scanner.getColumn() != 0) {
			return Token.UNDEFINED;
		}

		int c;
		int n = 0;
		while (true) {
			do {
				c = scanner.read();
				n++;
			} while ((c == ' ' || c == '\t'));

			if (c == '#' || c == '!') {
				while (true) {
					c = scanner.read();
					n++;

					if (c == '\n' || c == '\r' || c == ICharacterScanner.EOF) {
						return T_COMMENT;
					}
				}
			}
			while (n-- > 0) {
				scanner.unread();
			}
			return Token.UNDEFINED;
		}
	}

	IToken keyword(ICharacterScanner scanner) {
		if (scanner.getColumn() != 0) {
			return Token.UNDEFINED;
		}

		int c;
		int n = 0;
		c = scanner.read();
		n++;

		StringBuilder sb = new StringBuilder();
		while (!(c == ' ' || c == '\t' || c == ':' || c == '=' || c == ICharacterScanner.EOF)) {

			if (c == '\\') {
				c = scanner.read();
				n++;
				if (c == ICharacterScanner.EOF) {
					break;
				}
			}

			sb.append((char) c);
			c = scanner.read();
			n++;
		}

		if (sb.isEmpty()) {

			while (n-- > 0) {
				scanner.unread();
			}

			return Token.UNDEFINED;
		}

		scanner.unread();

		String key = sb.toString();

		if (Constants.options.contains(key)) {
			return T_OPTION;
		}

		if (instructions.contains(key)) {
			return T_INSTRUCTION;
		}

		return T_KEY;
	}

	IToken error(ICharacterScanner scanner) {
		int c = scanner.read();
		int n = 1;
		if (c == '\\') {
			c = scanner.read();
			n++;
			if (c == ' ' || c == '\t') {
				while (c == ' ' || c == '\t') {
					c = scanner.read();
				}
				scanner.unread();
				return T_ERROR;
			}
		}
		while (n-- > 0) {
			scanner.unread();
		}
		return Token.UNDEFINED;
	}

}
