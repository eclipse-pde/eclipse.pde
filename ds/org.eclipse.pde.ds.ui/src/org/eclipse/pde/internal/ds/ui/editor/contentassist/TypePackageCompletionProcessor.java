/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.contentassist;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.swt.graphics.Image;

public abstract class TypePackageCompletionProcessor implements IContentAssistProcessor {

	private String fErrorMessage;
	private SearchEngine fSearchEngine;
	private Comparator fComparator;

	abstract class ProposalGenerator {
		abstract protected ICompletionProposal generateClassCompletion(String pName, String cName, boolean isClass);

		abstract protected ICompletionProposal generatePackageCompletion(String pName);
	}

	public TypePackageCompletionProcessor() {
		fSearchEngine = new SearchEngine();
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		return null;
	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	public String getErrorMessage() {
		return fErrorMessage;
	}

	protected void generateTypePackageProposals(String currentContent, IProject project, Collection c, int startOffset, int typeScope) {
		generateTypePackageProposals(currentContent, project, c, startOffset, typeScope, false);
	}

	protected void generateTypePackageProposals(String currentContent, IProject project, Collection c, int startOffset, int typeScope, boolean replaceEntireContents) {
		currentContent = removeLeadingSpaces(currentContent);
		if (c == null || currentContent.length() == 0)
			return;
		int length = (replaceEntireContents) ? -1 : currentContent.length();
		generateProposals(currentContent, project, c, startOffset, length, typeScope);
	}

	private void generateProposals(String currentContent, IProject project, final Collection c, final int startOffset, final int length, final int typeScope) {

		class TypePackageCompletionRequestor extends CompletionRequestor {

			public TypePackageCompletionRequestor() {
				super(true);
				setIgnored(CompletionProposal.PACKAGE_REF, false);
				setIgnored(CompletionProposal.TYPE_REF, false);
			}

			public void accept(CompletionProposal proposal) {
				ISharedImages images = JavaUI.getSharedImages();
				if (proposal.getKind() == CompletionProposal.PACKAGE_REF) {
					String pkgName = new String(proposal.getCompletion());
					addProposalToCollection(c, startOffset, length, pkgName,
							pkgName, images
									.getImage(ISharedImages.IMG_OBJS_PACKAGE));
				} else {
					boolean isInterface = Flags.isInterface(proposal.getFlags());
					String completion = new String(proposal.getCompletion());
					if (isInterface && typeScope == IJavaSearchConstants.CLASS || (!isInterface && typeScope == IJavaSearchConstants.INTERFACE) || completion.equals("Dummy2")) //$NON-NLS-1$
						// don't want Dummy class showing up as option.
						return;
					int period = completion.lastIndexOf('.');
					String cName = null, pName = null;
					if (period == -1) {
						cName = completion;
					} else {
						cName = completion.substring(period + 1);
						pName = completion.substring(0, period);
					}

					Image image = isInterface ? images
							.getImage(ISharedImages.IMG_OBJS_INTERFACE)
							: images.getImage(ISharedImages.IMG_OBJS_CLASS);
							addProposalToCollection(c, startOffset, length, cName
							+ " - " + pName, //$NON-NLS-1$
							completion, image);
				}
			}

		}

		try {
			ICompilationUnit unit = getWorkingCopy(project);
			if (unit == null) {
				generateTypeProposals(currentContent, project, c, startOffset, length, 1);
				return;
			}
			IBuffer buff = unit.getBuffer();
			buff.setContents("class Dummy2 { " + currentContent); //$NON-NLS-1$

			CompletionRequestor req = new TypePackageCompletionRequestor();
			unit.codeComplete(15 + currentContent.length(), req);
			unit.discardWorkingCopy();
		} catch (JavaModelException e) {
		}
	}

	private ICompilationUnit getWorkingCopy(IProject project) throws JavaModelException {
		IPackageFragmentRoot[] roots = JavaCore.create(project).getPackageFragmentRoots();
		if (roots.length > 0) {
			IPackageFragment frag = null;
			for (int i = 0; i < roots.length; i++)
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE || project.equals(roots[i].getCorrespondingResource()) || (roots[i].isArchive() && !roots[i].isExternal())) {
					IJavaElement[] elems = roots[i].getChildren();
					if ((elems.length > 0) && (i < elems.length) && (elems[i] instanceof IPackageFragment)) {
						frag = (IPackageFragment) elems[i];
						break;
					}
				}
			if (frag != null)
				return frag.getCompilationUnit("Dummy2.java").getWorkingCopy(new NullProgressMonitor()); //$NON-NLS-1$
		}
		return null;
	}

	protected void generateTypeProposals(String currentContent, IProject project, final Collection c, final int startOffset, final int length, int typeScope) {
		// Dynamically adjust the search scope depending on the current
		// state of the project
		IJavaSearchScope scope = PDEJavaHelper.getSearchScope(project);
		char[] packageName = null;
		char[] typeName = null;
		int index = currentContent.lastIndexOf('.');

		if (index == -1) {
			// There is no package qualification
			// Perform the search only on the type name
			typeName = currentContent.toCharArray();
		} else if ((index + 1) == currentContent.length()) {
			// There is a package qualification and the last character is a
			// dot
			// Perform the search for all types under the given package
			// Pattern for all types
			typeName = "".toCharArray(); //$NON-NLS-1$
			// Package name without the trailing dot
			packageName = currentContent.substring(0, index).toCharArray();
		} else {
			// There is a package qualification, followed by a dot, and 
			// a type fragment
			// Type name without the package qualification
			typeName = currentContent.substring(index + 1).toCharArray();
			// Package name without the trailing dot
			packageName = currentContent.substring(0, index).toCharArray();
		}

		try {
			TypeNameRequestor req = new TypeNameRequestor() {
				public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
					// Accept search results from the JDT SearchEngine
					String cName = new String(simpleTypeName);
					String pName = new String(packageName);
					String label = cName + " - " + pName; //$NON-NLS-1$
					String content = pName + "." + cName; //$NON-NLS-1$
					ISharedImages images = JavaUI.getSharedImages();
					Image image = (Flags.isInterface(modifiers)) ? images
							.getImage(ISharedImages.IMG_OBJS_INTERFACE)
							: images.getImage(ISharedImages.IMG_OBJS_CLASS);
					addProposalToCollection(c, startOffset, length, label, content, image);
				}
			};
			// Note:  Do not use the search() method, its performance is
			// bad compared to the searchAllTypeNames() method
			fSearchEngine.searchAllTypeNames(packageName, SearchPattern.R_EXACT_MATCH, typeName, SearchPattern.R_PREFIX_MATCH, typeScope, scope, req, IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
		} catch (CoreException e) {
			fErrorMessage = e.getMessage();
		}
	}

	public void sortCompletions(ICompletionProposal[] proposals) {
		Arrays.sort(proposals, getComparator());
	}

	private Comparator getComparator() {
		if (fComparator == null) {
			fComparator = new Comparator() {
				/* (non-Javadoc)
				 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
				 */
				public int compare(Object arg0, Object arg1) {
					ICompletionProposal p1 = (ICompletionProposal) arg0;
					ICompletionProposal p2 = (ICompletionProposal) arg1;

					return getSortKey(p1).compareToIgnoreCase(getSortKey(p2));
				}

				protected String getSortKey(ICompletionProposal p) {
					return p.getDisplayString();
				}
			};
		}
		return fComparator;
	}

	protected final String removeLeadingSpaces(String value) {
		char[] valueArray = value.toCharArray();
		int i = 0;
		for (; i < valueArray.length; i++)
			if (!Character.isWhitespace(valueArray[i]))
				break;
		return (i == valueArray.length) ? "" : new String(valueArray, i, valueArray.length - i); //$NON-NLS-1$
	}

	/**
	 * @param c
	 * @param startOffset
	 * @param length
	 * @param label
	 * @param content
	 * @param image
	 */
	protected void addProposalToCollection(final Collection c, final int startOffset, final int length, String label, String content, Image image) {
		TypeCompletionProposal proposal = new TypeCompletionProposal(content, image, label, startOffset, length);
		c.add(proposal);
	}

}
