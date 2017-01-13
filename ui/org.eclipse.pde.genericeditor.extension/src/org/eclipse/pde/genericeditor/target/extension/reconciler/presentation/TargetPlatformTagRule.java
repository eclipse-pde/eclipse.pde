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
package org.eclipse.pde.genericeditor.target.extension.reconciler.presentation;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.pde.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

/**
 * Word rule feeding the tags of a target definition to be highlighted
 */
public class TargetPlatformTagRule extends WordRule {

	private String[] tags = new String[] { ITargetConstants.LOCATIONS_TAG, ITargetConstants.LOCATION_TAG,
			ITargetConstants.TARGET_TAG, ITargetConstants.UNIT_TAG, ITargetConstants.REPOSITORY_TAG,
			ITargetConstants.TARGET_JRE_TAG };

	private IToken tagToken = new Token(new TextAttribute(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_CYAN)));

	public TargetPlatformTagRule() {
		super(new AlphanumericDetector());
		for (String tag : tags) {
			this.addWord(tag, tagToken);
		}
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		if (c != '<') {
			scanner.unread();
			return fDefaultToken;
		} else {
			c = scanner.read();
			if (c == '/') {
				while (Character.isWhitespace(scanner.read())) {
				}
				scanner.unread();
				return super.evaluate(scanner);
			}
			scanner.unread();
		}
		if (c == '>') {
			scanner.unread();
			return tagToken;
		}

		while (Character.isWhitespace(scanner.read())) {
		}
		scanner.unread();

		return super.evaluate(scanner);
	}

}
