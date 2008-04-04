/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Implementation of an API search criteria.
 * 
 * @since 1.0.0
 */
public class SearchCriteria implements IApiSearchCriteria {
		
	/**
	 * Map of component ID's to collection of leaf element in that scope, or an empty
	 * collection for entire component.
	 */
	private Map fComponentIds = new HashMap();
		
	/**
	 * References kinds with corresponding visibility and usage
	 * restrictions on the referenced location
	 */
	private int fReferenceKinds = ReferenceModifiers.MASK_REF_ALL;
	private int fVisibilityKinds = VisibilityModifiers.ALL_VISIBILITIES;
	private int fRestrictionKinds = RestrictionModifiers.NO_RESTRICTIONS;
	
	/**
	 * Java visibility to consider at the source or -1 if not considered
	 */
	private int fSourceModifiers = -1;
	
	/**
	 * Corresponding visibility and usage restrictions on the source location
	 */
	private int fSourceVisibility = VisibilityModifiers.ALL_VISIBILITIES;
	private int fSourceRestriction = RestrictionModifiers.ALL_RESTRICTIONS;	
	
	/**
	 * Component filters for source reference locations or <code>null</code> if none.
	 */
	private String fSourceFilter[] = null;
	
	/**
	 * User data or <code>null</code>
	 */
	private Object fUserData;

	/**
	 * Used for pattern matching.
	 */
	class PatternMatch {
		private int fElementType;
		private Pattern fPattern = null;
		
		/**
		 * Constructs a new pattern matcher.
		 * 
		 * @param pattern regular expression
		 * @param elementType element type constant
		 */
		PatternMatch(String pattern, int elementType) {
			fElementType = elementType;
			fPattern = Pattern.compile(pattern); 
		}
		
		/**
		 * Return whether the given element matches this pattern.
		 * 
		 * @param element element
		 * @return whether the given element matches this pattern
		 */
		public boolean matches(IElementDescriptor element) {
			String name = null;
			IReferenceTypeDescriptor potentialInner = null;
			switch (fElementType) {
			case IElementDescriptor.T_PACKAGE:
				switch (element.getElementType()) {
					case IElementDescriptor.T_PACKAGE:
						name = ((IPackageDescriptor)element).getName();
						break;
					case IElementDescriptor.T_REFERENCE_TYPE:
					case IElementDescriptor.T_METHOD:
					case IElementDescriptor.T_FIELD:
						name = ((IMemberDescriptor)element).getPackage().getName();
						break;
					}
				break;
			case IElementDescriptor.T_REFERENCE_TYPE:
				switch (element.getElementType()) {
					case IElementDescriptor.T_REFERENCE_TYPE:
						potentialInner = (IReferenceTypeDescriptor)element;  
						name = potentialInner.getQualifiedName();
						break;
					case IElementDescriptor.T_METHOD:
					case IElementDescriptor.T_FIELD:
						potentialInner = ((IMemberDescriptor)element).getEnclosingType();
						name = potentialInner.getQualifiedName();
						break;
					}				
				break;
			case IElementDescriptor.T_METHOD:
				switch (element.getElementType()) {
					case IElementDescriptor.T_METHOD:
						name = ((IMethodDescriptor)element).getName();
						break;
				}				
				break;
			case IElementDescriptor.T_FIELD:
				switch (element.getElementType()) {
					case IElementDescriptor.T_FIELD:
						name = ((IFieldDescriptor)element).getName();
						break;
				}				
				break;
			}
			if (name != null) {
				Matcher matcher = fPattern.matcher(name);
				if (matcher.matches()) {
					return true;
				}
				if (potentialInner != null) {
					// check enclosing types for match
					IReferenceTypeDescriptor type = potentialInner.getEnclosingType();
					if (type != null) {
						return matches(type);
					}
				}
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Element: ").append(Util.getDescriptorKind(fElementType)); //$NON-NLS-1$
			if(fPattern != null) {
				buffer.append(" with pattern :").append(fPattern.toString()); //$NON-NLS-1$
			}
			return buffer.toString();
		}
	}
	
	/**
	 * Used for determining potential element matches.
	 */
	class PotentialElementMatch {
		/**
		 * Element to match against
		 */
		private IElementDescriptor fElement;
		
		/**
		 * Constructs a new element matcher.
		 * 
		 * @param element element to match
		 */
		PotentialElementMatch(IElementDescriptor element) {
			fElement = element; 
		}
		
		/**
		 * Return whether the given element is a potential match for this element.
		 * 
		 * @param element element
		 * @return whether the given element matches this pattern
		 */
		public boolean matches(IElementDescriptor element) {
			if (element.getElementType() == IElementDescriptor.T_METHOD) {
				if (fElement.getElementType() == IElementDescriptor.T_METHOD) {
					// ensure names are equal
					IMethodDescriptor potential = (IMethodDescriptor)element;
					IMethodDescriptor target = (IMethodDescriptor)fElement;
					return potential.getName().equals(target.getName()) &&
						potential.getSignature().equals(target.getSignature());
				} else {
					// all method sends must be resolved, so it is a potential match
					// TODO: could we optimize static methods?
					return true;
				}
			}
			if (fElement.equals(element)) {
				return true;
			}
			Set parents = getParents(element);
			return parents.contains(fElement);
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return fElement.toString();
		}
	}
	
	/**
	 * List of patterns or <code>null</code> if none.
	 */
	private List fPatterns = null;
	
	/**
	 * List of potential element matches or <code>null</code> if none
	 */
	private List fPotentialElements = null;
	
	/**
	 * Returns all parent elements of the given element in a set.
	 * 
	 * @param element
	 * @return parent elements
	 */
	private Set getParents(IElementDescriptor element) {
		Set parents = new HashSet();
		IElementDescriptor parent = element.getParent();
		while (parent != null) {
			parents.add(parent);
			parent = parent.getParent();
		}
		return parents;
	}


	/**
	 * Returns whether this criteria contains the specified element.
	 * 
	 * @param componentId component identifier
	 * @param element element descriptor
	 * @return whether this criteria contains the specified element
	 */
	private boolean encloses(String componentId, IElementDescriptor element) {
		Set leaves = (Set) fComponentIds.get(componentId);
		if (leaves != null) {
			if (leaves.isEmpty()) {
				// contains every thing in the component
				// TODO: check if the element's type really exists in the component?
				return true;
			}
			Iterator iterator = leaves.iterator();
			Set parents = getParents(element);
			while (iterator.hasNext()) {
				IElementDescriptor leaf = (IElementDescriptor) iterator.next();
				if (leaf.equals(element) || parents.contains(leaf)) {
					return true;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchCriteria#addComponentRestriction(java.lang.String)
	 */
	public void addReferencedComponentRestriction(String componentId) {
		Set components = (Set)fComponentIds.get(componentId);
		if (components == null) {
			components = new HashSet();
			fComponentIds.put(componentId, components);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchCriteria#addElementRestriction(java.lang.String, org.eclipse.pde.api.tools.descriptors.IElementDescriptor[])
	 */
	public void addReferencedElementRestriction(String componentId, IElementDescriptor[] elements) {
		if (fPotentialElements == null) {
			fPotentialElements = new ArrayList(elements.length);
		}
		for (int i = 0; i < elements.length; i++) {
			IElementDescriptor element = elements[i];
			fPotentialElements.add(new PotentialElementMatch(element));
			Set parents = getParents(element);
			Set leaves = (Set) fComponentIds.get(componentId);
			if (leaves == null) {
				addReferencedComponentRestriction(componentId);
				leaves = (Set) fComponentIds.get(componentId);
			}
			// first check if a parent is already in the scope (i.e already contained)
			Iterator iterator = parents.iterator();
			while (iterator.hasNext()) {
				IElementDescriptor el = (IElementDescriptor) iterator.next();
				if (leaves.contains(el)) {
					// already contains a parent element
					return;
				}
			}
			// remove existing leaves that are children of the element being added
			iterator = leaves.iterator();
			while (iterator.hasNext()) {
				IElementDescriptor leaf = (IElementDescriptor) iterator.next();
				parents = getParents(leaf);
				if (parents.contains(element)) {
					iterator.remove();
				}
			}
			leaves.add(element);			
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchCriteria#addPatternRestriction(java.lang.String, int)
	 */
	public void addReferencedPatternRestriction(String regEx, int elementType) {
		if (fPatterns == null) {
			fPatterns = new ArrayList();
		}
		fPatterns.add(new PatternMatch(regEx, elementType));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchCriteria#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return fReferenceKinds;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchCriteria#isMatch(org.eclipse.pde.api.tools.search.IReference)
	 */
	public boolean isMatch(IReference reference) {
		ILocation location = reference.getResolvedLocation();
		if (location != null) {
			IApiAnnotations annotations = reference.getResolvedAnnotations();
			if (annotations != null) {
				return matchesElementRestrictions(location)
					&& matchesApiRestrictions(annotations);
			}
		}
		return false;
	}
	
	/**
	 * Returns whether the given location meets pattern matching criteria.
	 * 
	 * @param location location
	 * @return whether the given location meets pattern matching criteria
	 */
	private boolean matchesPatternRestrictions(ILocation location) {
		if (fPatterns == null) {
			return true;
		}
		IElementDescriptor element = location.getMember();
		Iterator iterator = fPatterns.iterator();
		while (iterator.hasNext()) {
			PatternMatch pattern = (PatternMatch) iterator.next();
			if (pattern.matches(element)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns whether the given annotations matches the conditions of this
	 * search criteria.
	 * 
	 * @param annotations API annotations
	 * @return whether the given reference matches the conditions of this
	 * search criteria
	 */
	private boolean matchesApiRestrictions(IApiAnnotations annotations) {
		int vis = annotations.getVisibility();
		int res = annotations.getRestrictions();
		if ((vis & fVisibilityKinds) > 0) {
			if (fRestrictionKinds == RestrictionModifiers.ALL_RESTRICTIONS || (res & fRestrictionKinds) > 0) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns whether the given reference is one of the reference kinds this criteria
	 * is looking for.
	 * 
	 * @param reference reference
	 * @return whether the given reference is one of the reference kinds this criteria
	 * is looking for
	 */
	private boolean matchesReferenceKinds(IReference reference) {
		return (reference.getReferenceKind() & (fReferenceKinds)) > 0;
	}	
	
	/**
	 * Returns whether the given location is contained within the component and
	 * element restrictions of this search criteria.
	 * 
	 * @param location location
	 * @return whether the given location is contained within the component and
	 * element restrictions of this search criteria
	 */
	private boolean matchesElementRestrictions(ILocation location) {
		if (fComponentIds.isEmpty()) {
			return true;
		}
		return encloses(location.getApiComponent().getId(), location.getMember());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria#setReferencedRestrictions(int, int)
	 */
	public void setReferencedRestrictions(int visibilityMask, int restrictionMask) {
		fVisibilityKinds = visibilityMask;
		fRestrictionKinds = restrictionMask;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria#setReferenceKinds(int)
	 */
	public void setReferenceKinds(int referenceMask) {
		fReferenceKinds = referenceMask;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchCriteria#isPotentialMatch(org.eclipse.pde.api.tools.search.IReference)
	 */
	public boolean isPotentialMatch(IReference reference) {
		return
			matchesReferenceKinds(reference) &&
			!isFilteredSourceLocation(reference.getSourceLocation()) &&
			matchesSourceModifiers(reference.getSourceLocation()) &&
			matchesSourceApiRestrictions(reference.getSourceLocation()) &&
			matchesPatternRestrictions(reference.getReferencedLocation()) &&
			isPotentialElementMatch(reference.getReferencedLocation());
	}
	
	/**
	 * Returns whether the location is a filtered source location.
	 * 
	 * @param location source location
	 * @return whether the location (reference) should be filtered (ignored)
	 */
	private boolean isFilteredSourceLocation(ILocation location) {
		if (fSourceFilter == null) {
			return false;
		}
		for (int i = 0; i < fSourceFilter.length; i++) {
			if (location.getApiComponent().getId().equals(fSourceFilter[i])) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns whether the source location (i.e. location that the reference is made
	 * from) matches API restrictions for this condition.
	 *  
	 * @param location source location
	 * @return whether restrictions are satisfied
	 */
	private boolean matchesSourceApiRestrictions(ILocation location) {
		if (fSourceVisibility == VisibilityModifiers.ALL_VISIBILITIES && fSourceRestriction == RestrictionModifiers.ALL_RESTRICTIONS) {
			return true;
		}
		IApiComponent apiComponent = location.getApiComponent();
		try {
			IApiAnnotations annotations = apiComponent.getApiDescription().resolveAnnotations(location.getMember());
			if (annotations != null) {
				if ((annotations.getVisibility() & fSourceVisibility) > 0) {
					if(fSourceRestriction == RestrictionModifiers.ALL_RESTRICTIONS) {
						return true;
					}
					int ares = annotations.getRestrictions();
					if(ares != 0) {
						return (ares & fSourceRestriction) > 0; 
					}
					else {
						return fSourceRestriction != 0;
					}
				}
			} else {
				// TODO:
				return true;
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return false;
	}
	
	/**
	 * Returns whether the Java visibility of the given source location matches
	 * the restrictions of this criteria.
	 * 
	 * @param location source/referencing location
	 * @return whether it matches Java visibility modifiers
	 */
	private boolean matchesSourceModifiers(ILocation location) {
		if (fSourceModifiers == -1) {
			return true;
		}
		IMemberDescriptor member = location.getMember();
		while (member != null) {
			int modifiers = member.getModifiers();
			if ((fSourceModifiers & modifiers) > 0 || fSourceModifiers == modifiers) { // in case of Acc.Default (0)
				member = member.getEnclosingType();
			} else {
				return false;
			}
		}
		return true;
	}

	private boolean isPotentialElementMatch(ILocation location) {
		if (fPotentialElements == null) {
			return true;
		}
		IElementDescriptor element = location.getMember();
		Iterator iterator = fPotentialElements.iterator();
		while (iterator.hasNext()) {
			PotentialElementMatch match = (PotentialElementMatch) iterator.next();
			if (match.matches(element)) {
				return true;
			}
		}
		return false;		
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("*** Search Criteria ***\n"); //$NON-NLS-1$
		buffer.append("Reference Kinds: ").append(Util.getReferenceKind(fReferenceKinds)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("Reference Restriction Kinds: ").append(Util.getRestrictionKind(fRestrictionKinds)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("Reference Visibility Kinds: ").append(Util.getVisibilityKind(fVisibilityKinds)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("Source Restriction Kinds: ").append(Util.getRestrictionKind(fSourceRestriction)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("Source Visibility Kinds: ").append(Util.getVisibilityKind(fSourceVisibility)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if(fPatterns != null) {
			buffer.append("Patterns: ").append(fPatterns.toString()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if(fPotentialElements != null) {
			buffer.append("Potential Matches: ").append(fPotentialElements.toString()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return buffer.toString();
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria#setSourceModifiers(int)
	 */
	public void setSourceModifiers(int modifiers) {
		fSourceModifiers = modifiers;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria#setSourceRestrictions(int, int)
	 */
	public void setSourceRestrictions(int visibilityMask, int restrictionMask) {
		fSourceVisibility = visibilityMask;
		fSourceRestriction = restrictionMask;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria#addSourceFilter(java.lang.String)
	 */
	public void addSourceFilter(String componentId) {
		if (fSourceFilter == null) {
			fSourceFilter = new String[]{componentId};
		} else {
			String[] temp = new String[fSourceFilter.length + 1];
			System.arraycopy(fSourceFilter, 0, temp, 0, fSourceFilter.length);
			temp[fSourceFilter.length] = componentId;
			fSourceFilter = temp;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria#getUserData()
	 */
	public Object getUserData() {
		return fUserData;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria#setUserData(java.lang.Object)
	 */
	public void setUserData(Object object) {
		fUserData = object;
		
	}

}
