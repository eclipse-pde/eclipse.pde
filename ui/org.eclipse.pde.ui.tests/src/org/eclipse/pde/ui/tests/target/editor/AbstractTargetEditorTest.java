/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target.editor;

import junit.framework.TestCase;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.genericeditor.target.extension.autocomplete.TargetDefinitionContentAssist;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class AbstractTargetEditorTest extends TestCase {

	static TargetDefinitionContentAssist contentAssist = new TargetDefinitionContentAssist();
	private IProject project;

	protected void checkProposals(String[] expectedProposals, ICompletionProposal[] actualProposals, int offset) {
		assertEquals("Proposal lengths are not equal at offset " + offset + ". Actual: "
				+ proposalListToString(actualProposals), expectedProposals.length, actualProposals.length);
		for (int i = 0; i < actualProposals.length; i++) {
			assertTrue("Proposal at index " + i + " did not match expected at offset "+offset,
					actualProposals[i].getDisplayString().equals(expectedProposals[i]));
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(getClass().getName() + "_" + System.currentTimeMillis());
		project.create(new NullProgressMonitor());
		project.open(new NullProgressMonitor());
	}

	protected ITextViewer getTextViewerForTarget(String name) throws Exception {
		IFile targetFile = project.getFile(name + ".target");
		targetFile.create(getClass().getResourceAsStream("targetfiles/" + name + ".txt"), true,
				new NullProgressMonitor());
		IEditorPart editor = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),
				targetFile, "org.eclipse.ui.genericeditor.GenericEditor");
		return (ITextViewer) editor.getAdapter(ITextOperationTarget.class);
	}

	@Override
	protected void tearDown() throws Exception {
		if (project != null) {
			project.delete(true, new NullProgressMonitor());
		}
		super.tearDown();
	}

	protected String proposalListToString(ICompletionProposal[] proposals) {
		if (proposals == null)
			return "null";
		if (proposals.length == 0)
			return "[]";
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
}
