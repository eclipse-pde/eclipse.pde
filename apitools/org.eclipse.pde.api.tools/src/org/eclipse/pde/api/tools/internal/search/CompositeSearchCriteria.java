/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;

/**
 * Specialized search criteria which allows groups of criteria to be evaluated in a boolean way
 */
public class CompositeSearchCriteria extends SearchCriteria {

	public static final int EVALUATE_AND = 1;
	public static final int EVALUATE_OR = 2;
	public static final int EVALUATE_XOR = 3;
	
	private HashSet fCriteria = null;
	private int fKind = 0;
	
	/**
	 * Constructor
	 * Defaults to an and-wise evaluation of criteria
	 */
	public CompositeSearchCriteria(Object userdata) {
		this(EVALUATE_OR, userdata);
	}
	
	/**
	 * Constructor
	 * @param kind
	 * Uses the specified boolean evaluation type
	 */
	public CompositeSearchCriteria(int kind, Object userdata) {
		fKind = kind;
		setUserData(userdata);
	}
	
	public void addCriteria(IApiSearchCriteria criteria) {
		if(fCriteria == null) {
			fCriteria = new HashSet(3);
		}
		fCriteria.add(criteria);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.SearchCriteria#isMatch(org.eclipse.pde.api.tools.internal.provisional.search.IReference)
	 */
	public boolean isMatch(IReference reference) {
		if(fCriteria != null) {
			boolean match = fKind != EVALUATE_OR;
			IApiSearchCriteria criteria = null;
			for(Iterator iter = fCriteria.iterator(); iter.hasNext();) {
				criteria = (IApiSearchCriteria) iter.next();
				match = evaluate(match, criteria.isMatch(reference));
			}
			return match;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.SearchCriteria#isPotentialMatch(org.eclipse.pde.api.tools.internal.provisional.search.IReference)
	 */
	public boolean isPotentialMatch(IReference reference) {
		if(fCriteria != null) {
			boolean match = fKind != EVALUATE_OR;
			IApiSearchCriteria criteria = null;
			for(Iterator iter = fCriteria.iterator(); iter.hasNext();) {
				criteria = (IApiSearchCriteria) iter.next();
				match = evaluate(match, criteria.isPotentialMatch(reference));
			}
			return match;
		}
		return false;
	}
	
	/**
	 * Evaluates the provided boolean values using the kind of evaluation specified
	 * @param context
	 * @param value
	 * @return the evaluated value of the context against the value using the specified kind of evaluation
	 */
	private boolean evaluate(boolean context, boolean value) {
		switch(fKind) {
			case EVALUATE_AND: {
				return context & value;
			}
			case EVALUATE_OR: {
				return context | value;
			}
			case EVALUATE_XOR: {
				return context != value;
			}
		}
		return false;
	}
}
