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


import java.util.ArrayList;
import java.util.ListIterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

public class TypeCompletionProcessor extends TypePackageCompletionProcessor implements ISubjectControlContentAssistProcessor {
	
	public static final char F_DOT = '.';
	
	protected ArrayList fResults;
	protected String fInitialContent;
	private IProject fProject;
	private int fTypeScope;
	
	public TypeCompletionProcessor(IProject project, int scope) {
		fProject = project;
		fTypeScope = scope;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] {F_DOT};
	}
	
	public ICompletionProposal[] computeCompletionProposals(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		// If the document offset is at the 0 position (i.e. no input entered), 
		// do not perform content assist.  The operation is too expensive 
		// because all classes and interfaces (depending on the specified scope)
		// will need to be resolved as proposals
		if (documentOffset == 0) {
			return null;
		}
		// Get the current contents of the field
		String currentContents = contentAssistSubjectControl.getDocument().get();
		// Generate a list of proposals based on the current contents
		ICompletionProposal[] proposals = computeCompletionProposals(currentContents);

		return proposals;
	}

	public IContextInformation[] computeContextInformation(IContentAssistSubjectControl contentAssistSubjectControl, int documentOffset) {
		// No context
		return null;
	}

	public void assistSessionEnded() {
		// After content assist session has ended, clear
		// the previous search results
		fResults = null;
		fErrorMessage = null;
	}
	
	public ICompletionProposal[] computeCompletionProposals(String currentContent) {
		ICompletionProposal[] proposals = null;
		// Determine method to obtain proposals based on current field contents
		if ((fResults == null) ||
			(currentContent.length() < fInitialContent.length()) ||
			(endsWithDot(currentContent))) {
			// Generate new proposals if the content assist session was just
			// started
			// Or generate new proposals if the current contents of the field
			// is less than the initial contents of the field used to 
			// generate the original proposals; thus, widening the search
			// scope.  This can occur when the user types backspace
			// Or generate new proposals if the current contents ends with a
			// dot
			proposals = generateCompletionProposals(currentContent);
		} else {
			// Filter existing proposals from a prevous search; thus, narrowing
			// the search scope.  This can occur when the user types additional
			// characters in the field causing new characters to be appended to
			// the initial field contents
			proposals = filterCompletionProposals(currentContent);
		}
		return proposals;
	}
	
	protected boolean endsWithDot(String string) {
    	int index = string.lastIndexOf(F_DOT);
		return ((index + 1) == string.length());
	}
	
	protected ICompletionProposal[] filterCompletionProposals(String currentContent) {
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
			String compareString = null;
			if (currentContent.indexOf(F_DOT) == -1) {
				// Use only the type name
				compareString = proposal.getDisplayString().toLowerCase();
			} else {
				// Use the fully qualified type name
				compareString = proposal.getReplacementString().toLowerCase();
			}
			// Filter out any proposal not matching the current contents
			// except for the edge case where the proposal is identical to the
			// current contents
			if (compareString.startsWith(currentContent, 0)) {
				filteredResults.add(proposal);
			}
		}
		return getSortedProposals(filteredResults);
	}
	
	protected ICompletionProposal[] getSortedProposals(ArrayList list) {
		ICompletionProposal[] proposals = getProposals(list);
		if (proposals != null) {
			// Sort the proposals alphabetically
			sortCompletions(proposals);
		}
		return proposals;
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
	
	protected ICompletionProposal[] generateCompletionProposals(String currentContent) {
		fResults = new ArrayList();
		// Store the initial field contents to determine if we need to
		// widen the scope later
		fInitialContent = currentContent;
		generateTypePackageProposals(currentContent, fProject, fResults, 0, fTypeScope, true);
	    return getSortedProposals(fResults);
	}

}
