/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@itemis.de> - bug 215314
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.contentassist;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.ProposalInfo;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class TypeCompletionProposal extends AbstractJavaCompletionProposal implements ICompletionProposal, ICompletionProposalExtension3, ICompletionProposalExtension4, ICompletionProposalExtension5 {

	private static final class PDETypeProposalInfo extends ProposalInfo {
		private IJavaProject fJavaProject;
		private String fTypeName;

		/**
		 * Creates a new proposal info.
		 *
		 * @param project the java project to reference when resolving types
		 * @param typeName the fully qualified type name
		 */
		public PDETypeProposalInfo(IJavaProject project, String typeName) {
			fJavaProject = project;
			fTypeName = typeName;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.java.ProposalInfo#getJavaElement()
		 */
		@Override
		public IJavaElement getJavaElement() throws JavaModelException {
			return fJavaProject.findType(fTypeName);
		}
	}

	protected String fAdditionalInfo;
	private boolean fProposalInfoComputed;
	private IProject fProject;
	private String fTypeName;

	public TypeCompletionProposal(String replacementString, Image image, String displayString, int startOffset, int length, IProject project, String typeName) {
		this(replacementString, image, displayString, startOffset, length);
		fTypeName = typeName;
		fProject = project;
	}

	public TypeCompletionProposal(String replacementString, Image image, String displayString, int startOffset, int length) {
		Assert.isNotNull(replacementString);
		setReplacementString(replacementString);
		setReplacementOffset(startOffset);
		setReplacementLength(length);
		setDisplayString(displayString);
		setImage(image);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
	 */
	public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
		IDocument document = viewer.getDocument();
		if (getReplacementLength() == -1) {
			setReplacementLength(document.getLength());
		}
		try {
			document.replace(getReplacementOffset(), getReplacementLength(), getReplacementString());
		} catch (BadLocationException e) {
			// DEBUG
			// e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
	 */
	public Point getSelection(IDocument document) {
		if (getReplacementString().equals("\"\"")) //$NON-NLS-1$
			return new Point(getReplacementOffset() + 1, 0);
		return new Point(getReplacementOffset() + getReplacementString().length(), 0);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension5#getAdditionalProposalInfo(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		if (fAdditionalInfo != null)
			return fAdditionalInfo;
		return super.getAdditionalProposalInfo(monitor);
	}

	public void setAdditionalProposalInfo(String info) {
		fAdditionalInfo = info;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	public boolean isAutoInsertable() {
		return true;
	}

	/**
	 * Returns the additional proposal info, or <code>null</code> if none
	 * exists.
	 *
	 * @return the additional proposal info, or <code>null</code> if none
	 *         exists
	 */
	protected final ProposalInfo getProposalInfo() {
		if (!fProposalInfoComputed) {
			setProposalInfo(computeProposalInfo());
			fProposalInfoComputed = true;
		}
		return super.getProposalInfo();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeProposalInfo()
	 */
	protected ProposalInfo computeProposalInfo() {
		if (fProject != null && fTypeName != null)
			return new PDETypeProposalInfo(JavaCore.create(fProject), fTypeName);
		return null;
	}

}