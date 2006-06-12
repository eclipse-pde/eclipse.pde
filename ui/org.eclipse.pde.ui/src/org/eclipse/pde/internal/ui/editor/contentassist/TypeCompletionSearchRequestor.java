/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ListIterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.PDEJavaHelper;
import org.eclipse.swt.graphics.Image;

/**
 * TypeCompletionSearchRequestor
 *
 */
public class TypeCompletionSearchRequestor extends TypeNameRequestor {

	protected ArrayList fResults;

	protected Comparator fComparator;	
	protected SearchEngine fSearchEngine;
	protected SearchParticipant[] fSearchParticipant;
	protected int fScope;
	protected IProject fProject;
	protected String fInitialContent;
	protected String fErrorMessage;
	
	/**
	 * 
	 */
	public TypeCompletionSearchRequestor(IProject project, int scope) {
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
		fSearchEngine = new SearchEngine();
		fSearchParticipant = new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		fProject = project;
		fScope = scope;
		reset();
	}
	
	public void acceptType(int modifiers, char[] packageName, char[] simpleTypeName, char[][] enclosingTypeNames, String path) {
		// Accept search results from the JDT SearchEngine
		String pName = new String(packageName);
		String cName = new String(simpleTypeName);
		String label = cName + " - " + pName; //$NON-NLS-1$
		String content = pName + "." + cName; //$NON-NLS-1$
		Image image = null;
		// Determine if the type is an interface or class
		if (Flags.isInterface(modifiers)) {
			image = PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_INTERFACE);
		} else {
			image = PDEPluginImages.get(PDEPluginImages.OBJ_DESC_GENERATE_CLASS);
		}
		// Generate the proposal
		TypeCompletionProposal proposal = new TypeCompletionProposal(content, image, label); 
		// Add it to the initial search results
		fResults.add(proposal);
	}

	protected ICompletionProposal[] getProposals(ArrayList list) {
		ICompletionProposal[] proposals = null;
		if ((list != null) && (list.size() != 0)) {
			// Convert the results array list into an array of completion
			// proposals
			proposals = (ICompletionProposal[]) list.toArray(new ICompletionProposal[list.size()]);
		}
		return proposals;
	}
	
	protected ICompletionProposal[] getSortedProposals(ArrayList list) {
		ICompletionProposal[] proposals = getProposals(list);
		if (proposals != null) {
			// Sort the proposals alphabetically
			Arrays.sort(proposals, fComparator);
		}
		return proposals;
	}
	
	protected ICompletionProposal[] filterCompletionProposals(String currentContents) {
		if (fResults == null) {
			return null;
		}
		ListIterator iterator = fResults.listIterator();
		// Maintain a list of filtered search results
		ArrayList filteredResults = new ArrayList();
		// Iterate over the initial search results
		while (iterator.hasNext()) {
			Object object = iterator.next();		
			TypeCompletionProposal proposal = (TypeCompletionProposal)object;
			String displayString = proposal.getDisplayString();
			// Filter out any proposal not matching the current contents
			if (compareStartStringIgnoreCase(displayString, currentContents)) {
				filteredResults.add(proposal);
			}
		}
		return getSortedProposals(filteredResults);
	}
	
	protected boolean compareStartStringIgnoreCase(String primary, String prefix) {
		String compareName = primary.toLowerCase();
		String compareContents = prefix.toLowerCase();
		boolean match = compareName.startsWith(compareContents, 0);
		return match;
	}
	
	public ICompletionProposal[] computeCompletionProposals(String currentContents) {
		ICompletionProposal[] proposals = null;
		// Determine method to obtain proposals based on current field contents
		if ((fResults == null) ||
				(currentContents.length() < fInitialContent.length())) {
			// Generate new proposals if the content assist session was just
			// started
			// Or generate new proposals if the current contents of the field
			// is less than the initial contents of the field used to 
			// generate the original proposals; thus, widening the search
			// scope.  This can occur when the user types backspace
			proposals = generateCompletionProposals(currentContents);
		} else {
			// Filter existing proposals from a prevous search; thus, narrowing
			// the search scope.  This can occur when the user types additional
			// characters in the field causing new characters to be appended to
			// the initial field contents
			proposals = filterCompletionProposals(currentContents);
		}
		return proposals;
	}
	
	protected ICompletionProposal[] generateCompletionProposals(String currentContents) {
		fResults = new ArrayList();
		// Store the initial field contents to determine if we need to
		// widen the scope later
		fInitialContent = currentContents;
		// Dynamically adjust the search scope depending on the current
		// state of the project
		IJavaSearchScope scope = PDEJavaHelper.getSearchScope(fProject);
	    try {
	    	// Note:  Do not use the search() method, its performance is
	    	// horrible compared to the searchAllTypeNames() method
	    	fSearchEngine.searchAllTypeNames(
	    			null,
                    currentContents.toCharArray(),
                    SearchPattern.R_PREFIX_MATCH,
                    fScope,
                    scope,
                    this,
                    IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
                    null);
	    } catch (CoreException e) {
			// DEBUG
	    	fErrorMessage = e.getMessage();
			// e.printStackTrace();
		}
	    return getSortedProposals(fResults);
	}

	public String getErrorMessage() {
		return fErrorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		fErrorMessage = errorMessage;
	}
	
	public void reset() {
		fResults = null;
	}
}
