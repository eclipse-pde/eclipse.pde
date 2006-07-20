/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.PDEJavaHelper;
import org.eclipse.swt.graphics.Image;

public abstract class TypePackageCompletionProcessor implements IContentAssistProcessor {
	
	protected String fErrorMessage;
	protected SearchEngine fSearchEngine;
	protected Comparator fComparator;
	
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
	
	protected void generateTypePackageProposals(String currentContent, IProject project, Collection c, 
			int startOffset, int typeScope) {
		generateTypePackageProposals(currentContent, project, c, startOffset, typeScope, false);
	}
	
	protected void generateTypePackageProposals(String currentContent, IProject project, Collection c, 
			int startOffset, int typeScope, boolean replaceEntireContents) {
		if (c == null)
			return;
		int length = (replaceEntireContents) ? -1 : currentContent.length();
		generateTypeProposals(currentContent, project, c, startOffset, length, typeScope);
//		generatePackageProposals(currentContent, project, c, startOffset, length);
	}
	
	private void generateTypeProposals(String currentContent, IProject project, final Collection c, 
			final int startOffset, final int length, int typeScope) {
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
	    		public void acceptType(int modifiers, char[] packageName,
	    				char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
	    			// Accept search results from the JDT SearchEngine
	    			String cName = new String(simpleTypeName);
	    			String pName = new String(packageName);
	    			String label = cName + " - " + pName; //$NON-NLS-1$
	    			String content = pName + "." + cName; //$NON-NLS-1$
	    			Image image = (Flags.isInterface(modifiers)) ? PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_CLASS) :
	    				PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_CLASS);
	    			TypeCompletionProposal proposal =  new TypeCompletionProposal(content, image, label, 
	    					startOffset, length);
	    			c.add(proposal);
	    		}
	    	};
	    	// Note:  Do not use the search() method, its performance is
	    	// bad compared to the searchAllTypeNames() method
	    	fSearchEngine.searchAllTypeNames(
	    			packageName,
	    			typeName,
                    SearchPattern.R_PREFIX_MATCH,
                    typeScope,
                    scope,
                    req,
                    IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
                    null);
	    } catch (CoreException e) {
	    	fErrorMessage = e.getMessage();
		}
	}

//  Commented out function until we can improve package search performance
	
//	private void generatePackageProposals(String currentContent, IProject project, Collection c,
//			int startOffset, int length) {
//		// Get the package fragment roots
//		IPackageFragmentRoot[] packageFragments = 
//			PDEJavaHelper.getNonJRERoots(JavaCore.create(project));
//		// Use set to avoid duplicate proposals
//		HashSet set = new HashSet();
//		// Do not allow an empty package proposals
//		set.add("");  //$NON-NLS-1$
//		// Check all package fragments
//		for (int x = 0; x < packageFragments.length; x++) {
//			IJavaElement[] javaElements = null;
//			// Get packages
//			try {
//				javaElements = packageFragments[x].getChildren();
//			} catch (JavaModelException e) {
//				fErrorMessage = e.getMessage();
//				break;
//			}
//			// Search for matching packages
//			for (int j = 0; j < javaElements.length; j++) {
//				String pName = javaElements[j].getElementName();
//				if (pName.startsWith(currentContent, 0) && 
//						set.add(pName)) {
//					// Generate the proposal
//					TypeCompletionProposal proposal = 
//						new TypeCompletionProposal(pName, PDEPluginImages.get(PDEPluginImages.OBJ_DESC_PACKAGE),
//								pName, startOffset, length);
//
//					// Add it to the search results
//					c.add(proposal);
//				}
//			}
//		}
//	}
	
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

}
