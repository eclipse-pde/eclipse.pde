/*******************************************************************************
 * Copyright (c) 2020 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fr Jeremy Krieg <fr.jkrieg@greekwelfaresa.org.au> - initial API and implementation
*******************************************************************************/
package bndtools.preferences;

public enum QuickFixVersioning {

	noversion,
	latest;

	public static final String				PREFERENCE_KEY	= "quickfixVersioning";
	public static final QuickFixVersioning	DEFAULT			= noversion;

	public static QuickFixVersioning parse(String string) {
		try {
			return valueOf(string);
		} catch (Exception e) {
			return DEFAULT;
		}
	}

}
