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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Presentation reconclier collecting different rules for syntax coloring.
 */

public class TargetPlatformPresentationReconciler extends PresentationReconciler {

	private final class InvalidatingListener implements IPropertyChangeListener {
		ITextViewer viewer;

		public void setViewer(ITextViewer newViewer) {
			viewer = newViewer;
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if ("overriddenByCSS".equals(event.getProperty())) {
				return;
			}
			setDamageRepairerScanner();
			viewer.invalidateTextPresentation();
		}
	}

	private UpdatableDefaultDamagerRepairer dr;
	private InvalidatingListener listener;

	public TargetPlatformPresentationReconciler() {
		dr = new UpdatableDefaultDamagerRepairer(new RuleBasedScanner());
		this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		setDamageRepairerScanner();
		listener = new InvalidatingListener();
	}

	@Override
	public void install(ITextViewer viewer) {
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.workbench");
		store.addPropertyChangeListener(listener);
		listener.setViewer(viewer);
		super.install(viewer);
	}

	@Override
	public void uninstall() {
		super.uninstall();
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.workbench");
		store.removePropertyChangeListener(listener);
	}

	private void setDamageRepairerScanner() {
		if (dr == null) {
			return;
		}
		RuleBasedScanner scanner = new RuleBasedScanner();
		ColorRegistry manager = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
		IRule[] rules = new IRule[6];
		rules[0] = new SingleLineRule("<?", "?>", //$NON-NLS-1$ //$NON-NLS-2$
				new Token(new TextAttribute(manager.get(IGETEColorConstants.P_HEADER))));
		rules[1] = new SingleLineRule("\"", "\"", //$NON-NLS-1$ //$NON-NLS-2$
				new Token(new TextAttribute(manager.get(IGETEColorConstants.P_QUOTE))));
		rules[2] = new MultiLineRule("<!--", "-->", //$NON-NLS-1$ //$NON-NLS-2$
				new Token(new TextAttribute(manager.get(IGETEColorConstants.P_COMMENT))));
		rules[3] = new TargetPlatformTagRule();
		rules[4] = new TargetPlatformAttributeRule();
		rules[5] = new GeneralTagRule();
		scanner.setRules(rules);
		dr.updateTokenScanner(scanner);
	}

	/**
	 * Performs the repair on the full document to ensure MultiLineRules are
	 * enforced
	 */
	@Override
	protected TextPresentation createPresentation(IRegion damage, IDocument document) {
		TextPresentation presentation = new TextPresentation(damage, 1000);
		IPresentationRepairer repairer = this.getRepairer(IDocument.DEFAULT_CONTENT_TYPE);
		if (repairer != null)
			try {
				ITypedRegion[] regions = TextUtilities.computePartitioning(document, getDocumentPartitioning(), 0,
						document.getLength(), false);
				if (regions.length > 0) {
					repairer.createPresentation(presentation, regions[0]);
					return presentation;
				}
				return null;
			} catch (BadLocationException e) {
				return null;
			}

		return presentation;
	}

}
