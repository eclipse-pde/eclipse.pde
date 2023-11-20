/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

public class NameGenerator {

	public static String createClassPropertyName(String name, String prefix) {
		// see
		// https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-component.property.mapping
		// > single-element annotation
		StringBuilder buf = new StringBuilder(name.length() * 2);
		if (prefix != null) {
			// rule 4: If the component property type declares a PREFIX_ field whose value
			// is a compile-time constant String, then the property name is prefixed with
			// the value of the PREFIX_ field.
			buf.append(prefix);
		}
		char[] chars = name.toCharArray();
		char last = 'X';
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if (Character.isLowerCase(last) && Character.isUpperCase(c)) {
				// Rule 1: When a lower case character is followed by an upper case character, a
				// full stop ('.' \u002E) is inserted between them.
				buf.append('.');
			}
			// Rule 2: Each upper case character is converted to lower case.
			buf.append(Character.toLowerCase(c));
			// Rule 3: All other characters are unchanged.
			// --> nothing to do
			last = c;
		}
		return buf.toString();
	}

	public static String createPropertyName(String name, String prefix, DSAnnotationVersion specVersion) {
		if (DSAnnotationVersion.V1_4.isEqualOrHigherThan(specVersion)) {
			// See
			// https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-component.property.mapping
			StringBuilder sb;
			if (prefix == null) {
				sb = new StringBuilder(name.length());
			} else {
				// Rule 4: If the component property type declares a PREFIX_ field whose value
				// is a compile-time constant String, then the property name is prefixed with
				// the value of the PREFIX_ field.
				sb = new StringBuilder(name.length() + prefix.length());
				sb.append(prefix);
			}
			char[] chars = name.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				char c = chars[i];
				if (c == '$') {
					// Rule 1: A single dollar sign ('$' \u0024) is removed unless it is followed by
					if (i < chars.length - 1) {
						char n = chars[i + 1];
						if (n == '_') {
							// Rule 1.1: A low line ('_' \u005F) and a dollar sign in which case the three
							// consecutive characters ("$_$") are converted to a single hyphen-minus ('-'
							// \u002D).
							if (i < chars.length - 2 && chars[i + 2] == '$') {
								i = i + 2;
								sb.append('-');
							}
						} else if (n == '$') {
							// Rule 1.2 : another dollar sign in which case the two consecutive dollar signs
							// ("$$") are converted to a single dollar sign.
							i = i + 1;
							sb.append('$');
						}
					}
					continue;
				}
				if (c == '_') {
					// Rule 2: A single low line ('_' \u005F) is converted into a full stop ('.'
					// \u002E) unless is it followed by an-
					// other low line in which case the two consecutive low lines ("__") are
					// converted to a single low
					// line.
					char n = chars[i + 1];
					if (i < chars.length - 1 && n == '_') {
						i = i + 1;
						sb.append('_');
					} else {
						sb.append('.');
					}
					continue;
				}
				// Rule 3: All other characters are unchanged.
				sb.append(chars[i]);
			}
			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder(name.length());
			char[] chars = name.toCharArray();
			for (int i = 0; i < chars.length; i++) {
				char c = chars[i];
				if (c == '$') {
					// Rule 1: A single dollar sign ('$' \u0024) is removed unless it is followed by
					// another dollar sign in which
					// case the two consecutive dollar signs ("$$") are converted to a single dollar
					// sign.
					if (i < chars.length - 1 && chars[i + 1] == '$') {
						i = i + 1;
						sb.append('$');
					}
					continue;
				}
				if (c == '_') {
					// Rule 2: A single low line ('_' \u005F) is converted into a full stop ('.'
					// \u002E) unless is it followed by an-
					// other low line in which case the two consecutive low lines ("__") are
					// converted to a single low
					// line.
					if (i < chars.length - 1 && chars[i + 1] == '_') {
						i = i + 1;
						sb.append('_');
					} else {
						sb.append('.');
					}
					continue;
				}
				// Rule 3: All other characters are unchanged.
				sb.append(chars[i]);
			}
			return sb.toString();
		}
	}

}
