/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.search.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.provisional.search.IMetadata;

/**
 * Test reporter for the search engine tests
 *
 * @since 1.0.1
 */
public class TestReporter implements IApiSearchReporter {

	private SearchTest test = null;
	private HashSet<String> notsearched = null;
	private HashMap<String, ArrayList<Integer>> references = null;

	public TestReporter(SearchTest test) {
		this.test = test;
	}

	@Override
	public void reportNotSearched(IApiElement[] elements) {
		if (this.notsearched != null) {
			if (this.notsearched.size() != elements.length) {
				this.test.reportFailure("Expecting [" + this.notsearched.size() + "] but reported [" + elements.length + "] references"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			for (int i = 0; i < elements.length; i++) {
				if (!this.notsearched.remove(elements[i].getName())) {
					this.test.reportFailure("Not searched element [" + elements[i] + "] was not expected"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			if (this.notsearched.size() != 0) {
				this.test.reportFailure("[" + this.notsearched.size() + "] expected not-searched elements were not reported."); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for (Iterator<String> iter = this.notsearched.iterator(); iter.hasNext();) {
				System.out.println("Expected not-searched element was not reported: [" + iter.next() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			if (elements.length > 0) {
				this.test.reportFailure("Expecting no not-searched projects but [" + elements.length + "] were found"); //$NON-NLS-1$ //$NON-NLS-2$
				System.out.println("Unexpected excluded elements:"); //$NON-NLS-1$
				for (int i = 0; i < elements.length; i++) {
					System.out.println("  - " + elements[i].getName()); //$NON-NLS-1$
				}

			}
		}
	}

	@Override
	public void reportResults(IApiElement element, IReference[] references) {
		String name = (element.getType() == IApiElement.COMPONENT ? ((IApiComponent) element).getSymbolicName() : element.getName());
		if (this.references == null) {
			// expecting no references
			if (references.length > 0) {
				System.out.println("Unexpected References:"); //$NON-NLS-1$
				for (int i = 0; i < references.length; i++) {
					System.out.println("  - " + references[i]); //$NON-NLS-1$
				}
				this.test.reportFailure("No references were expected for IApiElement [" + name + "] but [" + references.length + "] were found"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return;
		}
		ArrayList<Integer> refs = this.references.get(name);
		if (refs == null) {
			if (references.length == 0) {
				return;
			} else {
				this.test.reportFailure("Unexpected references found for IApiElement [" + name + "], was expecting none"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else {
			if (refs.size() != references.length) {
				this.test.reportFailure("Expecting [" + refs.size() + "] but reported [" + references.length + "] references"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			for (int i = 0; i < references.length; i++) {
				if (!refs.remove(Integer.valueOf(references[i].getReferenceKind()))) {
					this.test.reportFailure("Reference [" + Reference.getReferenceText(references[i].getReferenceKind()) + "] was not expected"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			if (refs.size() != 0) {
				System.out.println("Missing references not reported:"); //$NON-NLS-1$
				for (Iterator<Integer> iterator = refs.iterator(); iterator.hasNext();) {
					System.out.println("  - " + Reference.getReferenceText((iterator.next()))); //$NON-NLS-1$
				}
				this.test.reportFailure("[" + refs.size() + "] references were not reported"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			for (Iterator<Integer> iter = refs.iterator(); iter.hasNext();) {
				System.out.println("Reference [" + Reference.getReferenceText(iter.next()) + "] was not reported"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Sets the expected reference kinds to the names of the element they came
	 * from. passing in <code>null</code>(s) will reset to not expecting any
	 * references
	 *
	 * @param references
	 */
	void setExpectedReferences(String[] names, int[][] referencekinds) {
		if (names == null || referencekinds == null) {
			if (this.references != null) {
				this.references.clear();
				this.references = null;
			}
		} else {
			this.references = new HashMap<>(names.length);
			ArrayList<Integer> ints = null;
			for (int i = 0; i < names.length; i++) {
				ints = new ArrayList<>(referencekinds[i].length);
				this.references.put(names[i], ints);
				for (int j = 0; j < referencekinds[i].length; j++) {
					ints.add(referencekinds[i][j]);
				}
			}
		}
	}

	/**
	 * Sets the {@link IApiElement}s we expect to see as not searched
	 *
	 * @param elements
	 */
	void setExpectedNotSearched(String[] elements) {
		if (elements != null) {
			this.notsearched = new HashSet<>(elements.length);
			for (int i = 0; i < elements.length; i++) {
				this.notsearched.add(elements[i]);
			}
		} else {
			if (this.notsearched != null) {
				this.notsearched.clear();
				this.notsearched = null;
			}
		}
	}

	@Override
	public void reportMetadata(IMetadata data) {
	}

	@Override
	public void reportCounts() {
	}
}
