/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.tools.emf.ui.internal.common.properties.ProjectOSGiTranslationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProjectOSGiTranslationProviderTest {

	private IProject project;

	@BeforeEach
	public void setUp() throws CoreException {
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("translation"); //$NON-NLS-1$
		project.create(null);
		project.open(null);
		final IFolder osgiInf = project.getFolder("OSGI-INF"); //$NON-NLS-1$
		osgiInf.create(true, true, null);
		osgiInf.getFolder("l10n").create(true, true, null); //$NON-NLS-1$
	}

	@AfterEach
	public void tearDown() throws CoreException {
		project.delete(true, null);
	}

	@Test
	public void testDisposeStopsTrackingPropertiesFiles() throws CoreException {
		final AtomicInteger updates = new AtomicInteger();
		final ProjectOSGiTranslationProvider provider = new ProjectOSGiTranslationProvider(project, "en") { //$NON-NLS-1$
			@Override
			protected void updateResourceBundle() {
				updates.incrementAndGet();
				super.updateResourceBundle();
			}
		};
		final IFile properties = project.getFile("OSGI-INF/l10n/bundle.properties"); //$NON-NLS-1$

		properties.create(new ByteArrayInputStream("label=Label".getBytes()), true, null); //$NON-NLS-1$
		assertTrue(updates.get() > 0, "the provider reloads when its properties file changes"); //$NON-NLS-1$

		provider.dispose();
		final int before = updates.get();
		properties.setContents(new ByteArrayInputStream("label=Other".getBytes()), true, false, null); //$NON-NLS-1$
		assertEquals(before, updates.get());
	}
}
