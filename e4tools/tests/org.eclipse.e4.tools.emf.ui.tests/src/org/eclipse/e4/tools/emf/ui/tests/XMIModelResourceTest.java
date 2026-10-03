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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.e4.tools.emf.ui.common.XMIModelResource;
import org.eclipse.e4.ui.model.application.MApplicationFactory;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.edit.command.SetCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class XMIModelResourceTest {

	@TempDir
	Path tempDir;

	private XMIModelResource createModel() throws IOException {
		final File file = tempDir.resolve("Application.e4xmi").toFile(); //$NON-NLS-1$
		final Resource resource = new XMIResourceImpl(URI.createFileURI(file.getAbsolutePath()));
		resource.getContents().add((EObject) MApplicationFactory.INSTANCE.createApplication());
		resource.save(null);
		return new XMIModelResource(URI.createFileURI(file.getAbsolutePath()));
	}

	private static void rename(XMIModelResource model) {
		final EObject application = model.getEditingDomain().getResourceSet().getResources().get(0).getContents()
				.get(0);
		model.getEditingDomain().getCommandStack().execute(SetCommand.create(model.getEditingDomain(), application,
				application.eClass().getEStructuralFeature("elementId"), "renamed")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSaveClearsDirtyState() throws IOException {
		final XMIModelResource model = createModel();
		rename(model);
		assertTrue(model.isDirty());

		assertEquals(IStatus.OK, model.save().getSeverity());
		assertFalse(model.isDirty());
	}

	@Test
	public void testFailedSaveReportsErrorAndStaysDirty() throws IOException {
		final XMIModelResource model = createModel();
		rename(model);
		final File file = tempDir.resolve("Application.e4xmi").toFile(); //$NON-NLS-1$
		assertTrue(file.setWritable(false));
		try {
			final IStatus status = model.save();
			assertEquals(IStatus.ERROR, status.getSeverity());
			assertTrue(model.isDirty());
		} finally {
			file.setWritable(true);
		}
	}
}
