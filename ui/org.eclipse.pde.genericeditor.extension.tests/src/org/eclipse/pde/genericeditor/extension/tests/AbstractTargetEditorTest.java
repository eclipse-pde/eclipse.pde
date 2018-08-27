/*******************************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others
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
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetDefinitionContentAssist;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.After;
import org.junit.Before;
import org.osgi.framework.FrameworkUtil;

public class AbstractTargetEditorTest {

	static TargetDefinitionContentAssist contentAssist = new TargetDefinitionContentAssist();
	private IProject project;
	protected File tempFile;

	protected void checkProposals(String[] expectedProposals, ICompletionProposal[] actualProposals, int offset) {
		assertEquals("Proposal lengths are not equal at offset " + offset + ". Actual: "
				+ proposalListToString(actualProposals), expectedProposals.length, actualProposals.length);
		for (int i = 0; i < actualProposals.length; i++) {
			assertEquals("Proposal at index " + i + " did not match expected at offset " + offset,
					actualProposals[i].getDisplayString(), expectedProposals[i]);
		}
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
		InputStream testStream = FrameworkUtil.getBundle(this.getClass())
				.getEntry("testing-files/target-files/" + name + ".txt").openStream();
		String normalizedLineFeeds = new BufferedReader(new InputStreamReader(testStream)).lines()
				.collect(Collectors.joining("\n"));
		InputStream normalizedStream = new ByteArrayInputStream(normalizedLineFeeds.getBytes(StandardCharsets.UTF_8));
		targetFile.create(normalizedStream, true, new NullProgressMonitor());
		IEditorPart editor = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
				targetFile, "org.eclipse.ui.genericeditor.GenericEditor");
		return (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
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
		if (proposals.length == 0) {
			return "[]";
		}
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		for (ICompletionProposal proposal : proposals) {
			builder.append(proposal.getDisplayString());
			builder.append(", ");
		}
		builder.setLength(builder.length() - 2);
		builder.append(']');
		return builder.toString();
	}

	public static ITextFileBuffer getTextFileBufferFromFile(File file) {
		try {
			IPath path = Path.fromOSString(file.getAbsolutePath());
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(path, LocationKind.LOCATION, null);
			return manager.getTextFileBuffer(path, LocationKind.LOCATION);
		} catch (CoreException e) {
			fail("Unable to retrive target definition file");
		}
		return null;
	}
}
