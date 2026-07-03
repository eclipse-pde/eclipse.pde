/*******************************************************************************
 * Copyright (c) 2026 Lars Vogel and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.css;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ILog;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.w3c.dom.Element;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.css.DocumentCSS;
import org.w3c.dom.stylesheets.StyleSheet;
import org.w3c.dom.stylesheets.StyleSheetList;

/**
 * Bridges the CSS spy over the e4 CSS engine's stylesheet access, which differs
 * between the current W3C {@code DocumentCSS} cascade and the reworked internal
 * cascade. All version-specific access goes through reflection so the bundle
 * compiles and runs against both.
 */
@SuppressWarnings("restriction")
final class CssEngineCompat {

	/** Receives one style rule enumerated from the engine's cascade. */
	@FunctionalInterface
	interface StyleRuleConsumer {
		/**
		 * @param selectorText the rule's selector text
		 * @param style        the rule's style declaration
		 * @param sourceName   the source-file name, or {@code null} when unknown
		 */
		void accept(String selectorText, CSSStyleDeclaration style, String sourceName);
	}

	/** True on the reworked engine, whose interface computes styles directly instead of exposing a W3C cascade. */
	private static final boolean CASCADE_ENGINE = hasMethod(CSSEngine.class, "computeStyle", Element.class, String.class); //$NON-NLS-1$

	private static boolean warned;

	private CssEngineCompat() {
	}

	/**
	 * Enumerates the engine's style rules in source order, passing each rule's
	 * selector, style declaration and source-file name to the consumer. Logs once
	 * and returns without invoking the consumer if the rules cannot be read.
	 */
	static void forEachStyleRule(CSSEngine engine, StyleRuleConsumer consumer) {
		try {
			if (CASCADE_ENGINE) {
				forEachCascadeRule(engine, consumer);
			} else {
				forEachDocumentCssRule(engine, consumer);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			logOnce(e);
		}
	}

	private static void forEachDocumentCssRule(CSSEngine engine, StyleRuleConsumer consumer)
			throws ReflectiveOperationException {
		DocumentCSS docCSS = (DocumentCSS) engine.getClass().getMethod("getDocumentCSS").invoke(engine); //$NON-NLS-1$
		if (docCSS == null) {
			return;
		}
		StyleSheetList sheets = docCSS.getStyleSheets();
		for (int i = 0; i < sheets.getLength(); i++) {
			if (!(sheets.item(i) instanceof CSSStyleSheet cssSheet)) {
				continue;
			}
			String href = cssSheet.getHref();
			String sourceName = href != null ? href.substring(href.lastIndexOf('/') + 1) : null;
			CSSRuleList rules = cssSheet.getCssRules();
			for (int j = 0; j < rules.getLength(); j++) {
				if (rules.item(j) instanceof CSSStyleRule styleRule) {
					consumer.accept(styleRule.getSelectorText(), styleRule.getStyle(), sourceName);
				}
			}
		}
	}

	private static void forEachCascadeRule(CSSEngine engine, StyleRuleConsumer consumer)
			throws ReflectiveOperationException {
		List<?> sheets = (List<?>) engine.getClass().getMethod("getStyleSheets").invoke(engine); //$NON-NLS-1$
		for (Object sheet : sheets) {
			List<?> rules = (List<?>) sheet.getClass().getMethod("getRules").invoke(sheet); //$NON-NLS-1$
			for (Object rule : rules) {
				Method selectorText;
				try {
					selectorText = rule.getClass().getMethod("getSelectorText"); //$NON-NLS-1$
				} catch (NoSuchMethodException notAStyleRule) {
					continue; // an @import rule, which has no selector
				}
				String selector = (String) selectorText.invoke(rule);
				CSSStyleDeclaration style = (CSSStyleDeclaration) rule.getClass().getMethod("getStyle").invoke(rule); //$NON-NLS-1$
				// The reworked cascade does not track a source file per sheet.
				consumer.accept(selector, style, null);
			}
		}
	}

	/**
	 * Parses the scratch-pad stylesheet and registers it with the engine. On the
	 * current engine the scratch sheet is inserted first; on the reworked cascade
	 * it is appended last, so it wins ties against the theme.
	 */
	static void applyScratchStyleSheet(CSSEngine engine, Reader reader) throws IOException {
		if (CASCADE_ENGINE) {
			// parseStyleSheet appends the parsed sheet to the cascade. Invoke it
			// reflectively: #4122 changes its return type, so a direct call would fail
			// with NoSuchMethodError on the reworked engine.
			try {
				engine.getClass().getMethod("parseStyleSheet", Reader.class).invoke(engine, reader); //$NON-NLS-1$
			} catch (InvocationTargetException e) {
				// Surface parse and I/O errors to the error label.
				Throwable cause = e.getCause();
				if (cause instanceof IOException ioException) {
					throw ioException;
				}
				if (cause instanceof RuntimeException runtimeException) {
					throw runtimeException;
				}
				throw new IllegalStateException(cause);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
			return;
		}
		try {
			Object docCSS = engine.getClass().getMethod("getDocumentCSS").invoke(engine); //$NON-NLS-1$
			StyleSheetList list = ((DocumentCSS) docCSS).getStyleSheets();
			List<StyleSheet> sheets = new ArrayList<>();
			for (int i = 0; i < list.getLength(); i++) {
				sheets.add(list.item(i));
			}
			Object scratch = engine.parseStyleSheet(reader);
			sheets.add(0, (StyleSheet) scratch);
			docCSS.getClass().getMethod("removeAllStyleSheets").invoke(docCSS); //$NON-NLS-1$
			Method addStyleSheet = docCSS.getClass().getMethod("addStyleSheet", StyleSheet.class); //$NON-NLS-1$
			for (StyleSheet sheet : sheets) {
				addStyleSheet.invoke(docCSS, sheet);
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private static boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
		try {
			type.getMethod(name, parameterTypes);
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static void logOnce(Throwable e) {
		if (!warned) {
			warned = true;
			ILog.of(CssEngineCompat.class).warn("Could not read the CSS engine stylesheets; " //$NON-NLS-1$
					+ "CSS rule sources will be unavailable in the spy.", e); //$NON-NLS-1$
		}
	}

}
