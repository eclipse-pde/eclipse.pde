/*******************************************************************************
 * Copyright (c) 2026 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.pde.ui.tests.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.osgi.util.ManifestElement;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Exercises the executable extension that JDT instantiates for E4 content assist.
 */
public class E4TemplateCompletionTest {

	private static final String BUNDLE_ID = "org.eclipse.e4.tools.jdt.templates";

	@Test
	public void createsE4CompletionComputerThroughExtensionRegistry() throws Exception {
		for (IConfigurationElement element : Platform.getExtensionRegistry()
				.getConfigurationElementsFor("org.eclipse.jdt.ui.javaCompletionProposalComputer")) {
			if (BUNDLE_ID.equals(element.getContributor().getName())
					&& "javaCompletionProposalComputer".equals(element.getName())) {
				IJavaCompletionProposalComputer computer = assertInstanceOf(IJavaCompletionProposalComputer.class,
						element.createExecutableExtension("class"));
				computer.sessionStarted();
				try {
					// Construction initializes all three contributed E4 template contexts.
					// Without a compilation unit there must be no proposals or errors.
					assertTrue(computer.computeCompletionProposals(new JavaContentAssistInvocationContext((ICompilationUnit) null),
							new NullProgressMonitor()).isEmpty());
				} finally {
					computer.sessionEnded();
				}
				return;
			}
		}
		fail("E4 template completion extension is missing from the test runtime");
	}

	@Test
	public void requiresJdtWithCoreRegistryAccessors() throws Exception {
		Bundle bundle = Platform.getBundle(BUNDLE_ID);
		assertNotNull(bundle);
		ManifestElement[] required = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE,
				bundle.getHeaders().get(Constants.REQUIRE_BUNDLE));
		for (ManifestElement dependency : required) {
			if ("org.eclipse.jdt.ui".equals(dependency.getValue())) {
				VersionRange range = new VersionRange(dependency.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE));
				assertFalse(range.includes(new Version("3.40.0")), "Must reject JDT without the Core accessors");
				assertTrue(range.includes(new Version("3.40.100")), "Must accept the version introducing the Core accessors");
				return;
			}
		}
		fail("Missing dependency on org.eclipse.jdt.ui");
	}
}
