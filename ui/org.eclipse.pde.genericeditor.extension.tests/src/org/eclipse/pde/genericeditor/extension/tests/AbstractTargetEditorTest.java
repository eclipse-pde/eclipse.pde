/*******************************************************************************
 * Copyright (c) 2017, 2024 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetDefinitionContentAssist;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.FrameworkUtil;

public abstract class AbstractTargetEditorTest {

	static TargetDefinitionContentAssist contentAssist = new TargetDefinitionContentAssist();
	private IProject project;
	protected File tempFile;

	protected void checkProposals(String[] expectedProposals, ICompletionProposal[] actualProposals, int offset) {
		assertEquals(Arrays.asList(expectedProposals), toProposalStrings(actualProposals));
	}

	@Before
	public void setUp() throws Exception {
		project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(getClass().getName() + "_" + System.currentTimeMillis());
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
	}

	protected ITextViewer getTextViewerForTarget(String name) throws Exception {
		IFile targetFile = project.getFile(name + ".target");
		try (InputStream testStream = FrameworkUtil.getBundle(this.getClass())
				.getEntry("testing-files/target-files/" + name + ".txt").openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(testStream))) {
			String normalizedLineFeeds = reader.lines().collect(Collectors.joining("\n"));
			InputStream normalizedStream = new ByteArrayInputStream(
					normalizedLineFeeds.getBytes(StandardCharsets.UTF_8));
			targetFile.create(normalizedStream, true, new NullProgressMonitor());
			IEditorPart editor = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
					targetFile, "org.eclipse.ui.genericeditor.GenericEditor");
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(editor);
			// Process UI events to ensure editor activation is complete
			Display display = PlatformUI.getWorkbench().getDisplay();
			for (int i = 0; i < 20; i++) {
				while (display.readAndDispatch()) {
					// Drain the event queue
				}
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// Ignore
				}
			}
			return (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
		}
	}

	protected String getLocationForSite(String name) {
		return FrameworkUtil.getBundle(this.getClass()).getEntry("testing-files/testing-sites/" + name + "/")
				.toString();
	}

	@After
	public void tearDown() throws Exception {
		if (tempFile != null) {
			tempFile.delete();
		}
		if (project != null) {
			project.delete(true, new NullProgressMonitor());
		}
	}

	protected String proposalListToString(ICompletionProposal[] proposals) {
		if (proposals == null) {
			return "null";
		}
		return "[" + String.join(",", toProposalStrings(proposals)) + "]";
	}

	private static List<String> toProposalStrings(ICompletionProposal[] proposals) {
		return Arrays.stream(proposals).map(ICompletionProposal::getDisplayString).toList();
	}

	public static ITextFileBuffer getTextFileBufferFromFile(File file) {
		try {
			IPath path = IPath.fromOSString(file.getAbsolutePath());
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(path, LocationKind.LOCATION, null);
			return manager.getTextFileBuffer(path, LocationKind.LOCATION);
		} catch (CoreException e) {
			fail("Unable to retrive target definition file");
		}
		return null;
	}
}
