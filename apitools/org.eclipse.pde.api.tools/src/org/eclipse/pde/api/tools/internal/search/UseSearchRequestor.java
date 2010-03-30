/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.builder.AbstractProblemDetector;
import org.eclipse.pde.api.tools.internal.builder.ProblemDetectorBuilder;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.builder.ReferenceAnalyzer;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
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
	 * The search scope for this requestor
	 */
	private IApiScope fScope = null;
	
	/**
	 * Patterns for jar API type roots to not scan
	 */
	private String[] jarPatterns = null;

	/**
	 * The default {@link ReferenceAnalyzer} for detecting illegal API use
	 * @see #includesIllegalUse()
	 */
	ReferenceAnalyzer fAnalyzer = null;
	
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
		fAnalyzer = new ReferenceAnalyzer();
		prepareScope(scope);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptComponent(org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent)
	 */
	public boolean acceptComponent(IApiComponent component) {
		try {
			if(!component.isSystemComponent() && getScope().encloses(component)) {
				if(includesIllegalUse()) {
					fAnalyzer.buildProblemDetectors(component, ProblemDetectorBuilder.K_USE, null);
				}
				return true;
			}
		}
		catch(CoreException ce) {
			//do nothing, return false
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptContainer(org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer)
	 */
	public boolean acceptContainer(IApiTypeContainer container) {
		return considerTypeContainer(container);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#acceptMember(org.eclipse.pde.api.tools.internal.provisional.model.IApiMember)
	 */
	public boolean acceptMember(IApiMember member) {
		// don't consider inner types, as they are considered with the root type
		switch(member.getType()) {
			case IApiElement.TYPE: {
				IApiType type = (IApiType) member;
				return !(type.isMemberType() || type.isLocal());
			}
		}
		return true;
	}
	
	/**
	 * Returns if the given {@link IApiTypeContainer} should be processed
	 * @param container
	 * @return true if the container should be processed false otherwise
	 */
	boolean considerTypeContainer(IApiTypeContainer container) {
		if(jarPatterns != null && container != null) {
			if(container.getContainerType() == IApiTypeContainer.ARCHIVE) {
				String[] pparts = null;
				for (int i = 0; i < jarPatterns.length; i++) {
					pparts = jarPatterns[i].split(":"); //$NON-NLS-1$
					if(pparts.length != 2) {
						continue;
					}
					if(container.getApiComponent().getSymbolicName().equals(pparts[0])) {
						if(container.getName().endsWith(pparts[1])) {
							return false;
						}
					}
				}
			}
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
				if(!fComponentIds.contains(component.getSymbolicName()) || component.equals(reference.getMember().getApiComponent())) {
					return false;
				}
				if(isIllegalUse(reference) || (includesAPI() && includesInternal())) {
					return true;
				}
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
			ApiPlugin.log(ce);
		}
		return false;
	}
	
	/**
	 * Returns true if the given reference is an illegal usage reference
	 * iff illegal use is part of the search mask.
	 * @param reference
	 * @return true if the reference is illegal use false otherwise
	 * @since 1.1
	 */
	boolean isIllegalUse(IReference reference) {
		IApiProblemDetector[] detectors = fAnalyzer.getProblemDetectors(reference.getReferenceKind());
		for (int i = 0; i < detectors.length; i++) {
			if(detectors[i].considerReference(reference)) {
				Reference ref = (Reference) reference;
				ref.setFlags(IReference.F_ILLEGAL);
				try {
					ref.addProblems(((AbstractProblemDetector)detectors[i]).createProblem(reference));
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return IReference.MASK_REF_ALL & ~IReference.REF_CONSTANTPOOL;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor#includesIllegalUse()
	 */
	public boolean includesIllegalUse() {
		return (fSearchMask & INCLUDE_ILLEGAL_USE) > 0;
	}
	
	/**
	 * The patterns for jar names to exclude from the search
	 * @param patterns
	 */
	public void setJarPatterns(String[] patterns) {
		jarPatterns = patterns;
	}
}
