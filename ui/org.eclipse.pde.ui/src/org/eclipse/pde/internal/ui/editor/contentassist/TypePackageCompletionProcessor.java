/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 257143
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;

public abstract class TypePackageCompletionProcessor implements IContentAssistProcessor {

	private String fErrorMessage;
	private SearchEngine fSearchEngine;
	private Comparator<Object> fComparator;

	abstract class ProposalGenerator {
		abstract protected ICompletionProposal generateClassCompletion(String pName, String cName, boolean isClass);

		abstract protected ICompletionProposal generatePackageCompletion(String pName);
	}

	public TypePackageCompletionProcessor() {
		fSearchEngine = new SearchEngine();
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return fErrorMessage;
	}

	protected void generateTypePackageProposals(String currentContent, IProject project, Collection<Object> c, int startOffset, int typeScope) {
		generateTypePackageProposals(currentContent, project, c, startOffset, typeScope, false);
	}

	protected void generateTypePackageProposals(String currentContent, IProject project, Collection<Object> c, int startOffset, int typeScope, boolean replaceEntireContents) {
		currentContent = removeLeadingSpaces(currentContent);
		if (c == null || currentContent.length() == 0)
			return;
		int length = (replaceEntireContents) ? -1 : currentContent.length();
		generateProposals(currentContent, project, c, startOffset, length, typeScope);
	}

	private void generateProposals(String currentContent, final IProject project, final Collection<Object> c, final int startOffset, final int length, final int typeScope) {

		class TypePackageCompletionRequestor extends CompletionRequestor {

			public TypePackageCompletionRequestor() {
				super(true);
				setIgnored(CompletionProposal.PACKAGE_REF, false);
				setIgnored(CompletionProposal.TYPE_REF, false);
			}

			@Override
			public void accept(CompletionProposal proposal) {
				if (proposal.getKind() == CompletionProposal.PACKAGE_REF) {
					String pkgName = new String(proposal.getCompletion());
					addProposalToCollection(c, startOffset, length, pkgName, pkgName, PDEPluginImages.get(PDEPluginImages.OBJ_DESC_PACKAGE));
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
					Image image = isInterface ? PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_INTERFACE) : PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_CLASS);
					String label = cName;
					if (pName != null)
						label = label + " - " + pName; //$NON-NLS-1$
					String typeName = String.valueOf(Signature.toCharArray(Signature.getTypeErasure(proposal.getSignature())));
					addProposalToCollection(c, startOffset, length, label, completion, image, project, typeName);
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

	protected void generateTypeProposals(String currentContent, final IProject project, final Collection<Object> c, final int startOffset, final int length, int typeScope) {
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
				@Override
				public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
					// Accept search results from the JDT SearchEngine
					String cName = new String(simpleTypeName);
					String pName = new String(packageName);
					String label = cName;
					String replaceString = cName;
					if (pName.length() > 0) {
						label = label + " - " + pName; //$NON-NLS-1$
						replaceString = pName + "." + replaceString; //$NON-NLS-1$
					}
					Image image = (Flags.isInterface(modifiers)) ? PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_INTERFACE) : PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_CLASS);
					addProposalToCollection(c, startOffset, length, label, replaceString, image, project, replaceString);
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

	private Comparator<Object> getComparator() {
		if (fComparator == null) {
			fComparator = new Comparator<Object>() {
				@Override
				public int compare(Object arg0, Object arg1) {
					if (arg0 instanceof ICompletionProposal && arg1 instanceof ICompletionProposal) {
						ICompletionProposal p1 = (ICompletionProposal) arg0;
						ICompletionProposal p2 = (ICompletionProposal) arg1;
						return getSortKey(p1).compareToIgnoreCase(getSortKey(p2));
					}
					return 0;
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

	protected void addProposalToCollection(final Collection<Object> c, final int startOffset, final int length, String label, String content, Image image) {
		c.add(new TypeCompletionProposal(content, image, label, startOffset, length));
	}

	protected void addProposalToCollection(final Collection<Object> c, final int startOffset, final int length, String label, String content, Image image, IProject project, String typeName) {
		c.add(new TypeCompletionProposal(content, image, label, startOffset, length, project, typeName));
	}

}
