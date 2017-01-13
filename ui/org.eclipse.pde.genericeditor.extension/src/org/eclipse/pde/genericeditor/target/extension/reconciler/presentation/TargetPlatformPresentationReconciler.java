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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

/**
 * Presentation reconclier collecting different rules for syntax coloring.
 */

public class TargetPlatformPresentationReconciler extends PresentationReconciler {

	private final TextAttribute headerAttribute = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
	private final TextAttribute commentAttribute = new TextAttribute(
			Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
	private final TextAttribute quoteAttribute = new TextAttribute(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

	public TargetPlatformPresentationReconciler() {
		RuleBasedScanner scanner = new RuleBasedScanner();
		IRule[] rules = new IRule[6];
		rules[0] = new SingleLineRule("<?", "?>", new Token(headerAttribute));//$NON-NLS-1$ //$NON-NLS-2$
		rules[1] = new SingleLineRule("\"", "\"", new Token(quoteAttribute));//$NON-NLS-1$ //$NON-NLS-2$
		rules[2] = new MultiLineRule("<!--", "-->", new Token(commentAttribute));//$NON-NLS-1$ //$NON-NLS-2$
		rules[3] = new TargetPlatformTagRule();
		rules[4] = new TargetPlatformAttributeRule();
		rules[5] = new GeneralTagRule();
		scanner.setRules(rules);

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(scanner);
		this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	}

}
