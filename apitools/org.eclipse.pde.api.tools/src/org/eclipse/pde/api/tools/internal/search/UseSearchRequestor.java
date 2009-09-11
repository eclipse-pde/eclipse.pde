/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;

/**
 * Default implementation of an {@link IApiSearchRequestor} to use with the
 * {@link ApiSearchEngine}. This requestor returns a search scope
 * composed of the dependent (visible) {@link IApiComponent}s for the given 
 * {@link IApiElement}
 * 
 * @since 1.0.0
 */
public class UseSearchRequestor implements IApiSearchRequestor {

	/**
	 * The backing elements to search with
	 */
	private Set fComponentIds = null;

	/**
	 * The mask to use while searching
	 */
	private int fSearchMask = 0;
	
	/**
	 * The search scope for this requester
	 */
	private IApiScope fScope = null;
	
	/**
	 * Internal package patterns or <code>null</code> if none.
	 */
	private Pattern[] fInternalPackages;
		
	/**
	 * API package patterns of <code>null</code> if none.
	 */
	private Pattern[] fApiPackages;
		
	/**
	 * Constructor
	 * @param elements an array of {@link IApiElement}s for the search engine to use
	 * @param scope the raw list of {@link IApiElement}s to extract references from
	 * @param searchkinds the kinds of references to search for. 
	 * <br>Options include: 
	 * <ol>
	 * <li>{@link #INCLUDE_API}</li>
	 * <li>{@link #INCLUDE_INTERNAL}</li>
	 * </ol>
	 */
	public UseSearchRequestor(Set/*<String>*/ elementnames, IApiElement[] scope, int searchkinds) {
		fSearchMask = searchkinds;
		fComponentIds = elementnames;
		prepareScope(scope);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptComponent(org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent)
	 */
	public boolean acceptComponent(IApiComponent component) {
		return true; //fComponentIds.contains(component);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptMember(org.eclipse.pde.api.tools.internal.provisional.model.IApiMember)
	 */
	public boolean acceptMember(IApiMember member) {
		// don't consider inner types, as they are considered with the root type
		if (member.getType() == IApiElement.TYPE) {
			IApiType type = (IApiType) member;
			return !(type.isMemberType() || type.isLocal());
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptReference(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	public boolean acceptReference(IReference reference) {
		try {
			IApiMember member = reference.getResolvedReference();
			if(member != null) {
				IApiComponent component = member.getApiComponent();
				if(!fComponentIds.contains(component.getId())) {
					return false;
				}
				if(component.equals(reference.getMember().getApiComponent())) {
					return false;
				}
				if(includesAPI() && includesInternal()) {
					return true;
				}
				// check pattern overrides before API description
				if (includesAPI() && fApiPackages != null) {
					if (matchesPattern(member, fApiPackages)) {
						return true;
					}
				}
				if (includesInternal() && fInternalPackages != null) {
					if (matchesPattern(member, fInternalPackages)) {
						return true;
					}
				}
				// check API description
				IApiAnnotations annots = component.getApiDescription().resolveAnnotations(member.getHandle());
				if(annots != null) {
					int vis = annots.getVisibility();
					if(VisibilityModifiers.isAPI(vis) && includesAPI()) {
						return true;
					}
					else if(VisibilityModifiers.isPrivate(vis) && includesInternal()) {
						return true;
					}
				}
			}
		}
		catch(CoreException ce) {
			//ApiPlugin.log(ce);
		}
		return false;
	}
	
	/**
	 * Returns whether the member's package matches any of the given patterns.
	 * 
	 * @param member member to match
	 * @param patterns patterns to match against
	 * @return whether there's a match
	 */
	private boolean matchesPattern(IApiMember member, Pattern[] patterns) {
		String pkg = member.getPackageName();
		for (int i = 0; i < patterns.length; i++) {
			if (patterns[i].matcher(pkg).find()) {
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		int kinds = IReference.MASK_REF_ALL & ~IReference.REF_CONSTANTPOOL;
		return kinds;
	}
	
	/**
	 * Prepares the search scope based on the available entries in the constructor
	 * @param elements
	 */
	private void prepareScope(IApiElement[] elements) {
		if(elements != null) {
			fScope = new ApiScope();
			for(int i = 0; i < elements.length; i++) {
				fScope.addElement(elements[i].getApiComponent());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#getScope()
	 */
	public IApiScope getScope() {
		return fScope;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#includesAPI()
	 */
	public boolean includesAPI() {
		return (fSearchMask & INCLUDE_API) > 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#includesInternal()
	 */
	public boolean includesInternal() {
		return (fSearchMask & INCLUDE_INTERNAL) > 0;
	}
	
	/**
	 * Sets regular expressions to consider as internal packages. Used to override visibility settings
	 * in an API description.
	 * 
	 * @param patterns regular expressions, may be empty or <code>null</code>
	 */
	public void setInternalPatterns(String[] patterns) {
		if (patterns == null || patterns.length == 0) {
			fInternalPackages = null;
		} else {
			fInternalPackages = new Pattern[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				fInternalPackages[i] = Pattern.compile(patterns[i]);
			}
		}
	}
	
	/**
	 * Sets regular expressions to consider as API packages. Used to override visibility settings
	 * in an API description.
	 * 
	 * @param patterns regular expressions, may be empty or <code>null</code>
	 */
	public void setApiPatterns(String[] patterns) {
		if (patterns == null || patterns.length == 0) {
			fApiPackages = null;
		} else {
			fApiPackages = new Pattern[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				fApiPackages[i] = Pattern.compile(patterns[i]);
			}
		}
	}
}
