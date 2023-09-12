/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ListIterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.swt.graphics.Image;

public class TypeContentProposalProvider extends TypePackageCompletionProcessor implements IContentProposalProvider {

	public static final char F_DOT = '.';

	private final IProject fProject;

	private final int fTypeScope;

	private ArrayList<Object> fInitialContentProposals;

	private String fInitialContent;

	private final Comparator<Object> fComparator;

	public TypeContentProposalProvider(IProject project, int scope) {
		fProject = project;
		fTypeScope = scope;
		fComparator = new TypeComparator();

		reset();
	}

	private static class TypeComparator implements Comparator<Object> {

		public TypeComparator() {
			// NO-OP
		}

		@Override
		public int compare(Object arg0, Object arg1) {
			String proposalSortKey1 = ((IContentProposal) arg0).getLabel();
			String proposalSortKey2 = ((IContentProposal) arg1).getLabel();
			return proposalSortKey1.compareToIgnoreCase(proposalSortKey2);
		}
	}

	@Override
	public IContentProposal[] getProposals(String contents, int position) {
		// Generate a list of proposals based on the current contents
		ArrayList<Object> currentContentProposals = null;
		// Determine method to obtain proposals based on current field contents
		if (position == 0) {
			// If the document offset is at the 0 position (i.e. no input entered),
			// do not perform content assist.  The operation is too expensive
			// because all classes and interfaces (depending on the specified scope)
			// will need to be resolved as proposals
//			currentContentProposals = null;
		} else if ((fInitialContentProposals == null) || (contents.length() < fInitialContent.length()) || (endsWithDot(contents))) {
			// Generate new proposals if the content assist session was just
			// started
			// Or generate new proposals if the current contents of the field
			// is less than the initial contents of the field used to
			// generate the original proposals; thus, widening the search
			// scope.  This can occur when the user types backspace
			// Or generate new proposals if the current contents ends with a
			// dot
			currentContentProposals = generateContentProposals(contents);
		} else {
			// Filter existing proposals from a prevous search; thus, narrowing
			// the search scope.  This can occur when the user types additional
			// characters in the field causing new characters to be appended to
			// the initial field contents
			currentContentProposals = filterContentProposals(contents);
		}

		return convertResultsToSortedProposals(currentContentProposals);
	}

	public void reset() {
		fInitialContentProposals = null;
	}

	@Override
	protected void addProposalToCollection(Collection<Object> collection, int startOffset, int length, String label, String content, Image image) {
		// Create content proposals for field assist
		// start offset and length not required
		Object proposal = new TypeContentProposal(label, content, null, image);
		// Add the proposal to the list of proposals
		collection.add(proposal);
	}

	private boolean endsWithDot(String string) {
		int index = string.lastIndexOf(F_DOT);
		if ((index + 1) == string.length()) {
			return true;
		}
		return false;
	}

	private ArrayList<Object> generateContentProposals(String currentContent) {
		fInitialContentProposals = new ArrayList<>();
		// Store the initial field contents to determine if we need to
		// widen the scope later
		fInitialContent = currentContent;
		generateTypePackageProposals(currentContent, fProject, fInitialContentProposals, 0, fTypeScope, true);
		return fInitialContentProposals;
	}

	private IContentProposal[] convertResultsToSortedProposals(ArrayList<Object> list) {
		IContentProposal[] proposals = null;
		if ((list != null) && (!list.isEmpty())) {
			// Convert the results array list into an array of completion
			// proposals
			proposals = list.toArray(new IContentProposal[list.size()]);
			// Sort the proposals alphabetically
			Arrays.sort(proposals, fComparator);
		} else {
			proposals = new IContentProposal[0];
		}
		return proposals;
	}

	private ArrayList<Object> filterContentProposals(String currentContent) {
		String lowerCaseCurrentContent = currentContent.toLowerCase();
		ListIterator<Object> iterator = fInitialContentProposals.listIterator();
		// Maintain a list of filtered search results
		ArrayList<Object> filteredContentProposals = new ArrayList<>();
		// Iterate over the initial search results
		while (iterator.hasNext()) {
			IContentProposal proposal = (IContentProposal) iterator.next();
			String compareString = null;
			if (lowerCaseCurrentContent.indexOf(F_DOT) == -1) {
				// Use only the type name
				compareString = proposal.getLabel().toLowerCase();
			} else {
				// Use the fully qualified type name
				compareString = proposal.getContent().toLowerCase();
			}
			// Filter out any proposal not matching the current contents
			// except for the edge case where the proposal is identical to the
			// current contents
			if (compareString.startsWith(lowerCaseCurrentContent, 0)) {
				filteredContentProposals.add(proposal);
			}
		}
		return filteredContentProposals;
	}

}
