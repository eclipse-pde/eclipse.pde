/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.tests.internal.classpath;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Helpers to inspect the entries computed by PDE's required-plugins classpath
 * container.
 */
final class ClasspathTestUtils {

	/**
	 * Names of all entries starting with the given prefix, sorted and including
	 * duplicates, so that a bundle added more than once is visible.
	 */
	static List<String> entryNames(IClasspathEntry[] entries, String prefix) {
		return Arrays.stream(entries).map(entry -> entry.getPath().lastSegment())
				.filter(name -> name.startsWith(prefix)).sorted().toList();
	}

	/** Access rule patterns of the entry with the given name, with duplicates. */
	static List<String> accessRulePatterns(IClasspathEntry[] entries, String name) {
		return Arrays.stream(entries).filter(entry -> name.equals(entry.getPath().lastSegment()))
				.map(IClasspathEntry::getAccessRules).flatMap(Arrays::stream).map(IAccessRule::getPattern)
				.map(Object::toString).toList();
	}

	private ClasspathTestUtils() {
	}
}
