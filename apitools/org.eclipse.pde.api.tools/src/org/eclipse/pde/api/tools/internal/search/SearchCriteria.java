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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
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
	protected Map fComponentIds = new HashMap();
		
	/**
	 * References kinds with corresponding visibility and usage
	 * restrictions on the referenced location
	 */
	protected int fReferenceKinds = ReferenceModifiers.MASK_REF_ALL;
	protected int fVisibilityKinds = VisibilityModifiers.ALL_VISIBILITIES;
	protected int fRestrictionKinds = RestrictionModifiers.NO_RESTRICTIONS;
	
	/**
	 * Java visibility to consider at the source or -1 if not considered
	 */
	protected int fSourceModifiers = -1;
	
	/**
	 * Corresponding visibility and usage restrictions on the source location
	 */
	protected int fSourceVisibility = VisibilityModifiers.ALL_VISIBILITIES;
	protected int fSourceRestriction = RestrictionModifiers.ALL_RESTRICTIONS;	
	
	/**
	 * Component filters for source reference locations or <code>null</code> if none.
	 */
	private String fSourceFilter[] = null;
	
	/**
	 * User data or <code>null</code>
	 */
	private Object fUserData;

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
		 * Return whether the given reference is a potential match for this element.
		 * 
		 * @param reference
		 * @return whether the given reference matches this pattern
		 */
		public boolean matches(IReference reference) {
			switch (fElement.getElementType()) {
				case IElementDescriptor.T_METHOD:
					if (reference.getReferenceType() == IReference.T_METHOD_REFERENCE) {
						// ensure names are equal
						IMethodDescriptor method = (IMethodDescriptor)fElement;
						return reference.getReferencedMemberName().equals(method.getName()) &&
							reference.getReferencedSignature().equals(method.getSignature());
					}
					return false;
				case IElementDescriptor.T_FIELD:
					if (reference.getReferenceType() == IReference.T_FIELD_REFERENCE) {
						IFieldDescriptor field = (IFieldDescriptor)fElement;
						return field.getName().equals(reference.getReferencedMemberName()) &&
							field.getEnclosingType().getQualifiedName().equals(reference.getReferencedTypeName());
					}
					return false;
				case IElementDescriptor.T_REFERENCE_TYPE:
					if (reference.getReferenceType() == IReference.T_TYPE_REFERENCE) {
						IReferenceTypeDescriptor type = (IReferenceTypeDescriptor)fElement;
						return type.getQualifiedName().equals(reference.getReferencedTypeName());
					}
					return false;
			}
			return false;
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
	 * @param member member
	 * @return parent elements
	 */
	private Set getParents(IApiMember member) {
		try {
			Set parents = new HashSet();
			IApiMember parent = member.getEnclosingType();
			while (parent != null) {
				parents.add(parent.getHandle());
				parent = parent.getEnclosingType();
			}
			return parents;
		} catch (CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
		return null;
	}
	
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
	private boolean encloses(String componentId, IApiMember member) {
		Set leaves = (Set) fComponentIds.get(componentId);
		if (leaves != null) {
			if (leaves.isEmpty()) {
				// contains every thing in the component
				// TODO: check if the element's type really exists in the component?
				return true;
			}
			Iterator iterator = leaves.iterator();
			Set parents = getParents(member);
			while (iterator.hasNext()) {
				IElementDescriptor leaf = (IElementDescriptor) iterator.next();
				if (leaf.equals(member.getHandle()) || parents.contains(leaf)) {
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
	 * @see org.eclipse.pde.api.tools.search.IApiSearchCriteria#getReferenceKinds()
	 */
	public int getReferenceKinds() {
		return fReferenceKinds;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.search.IApiSearchCriteria#isMatch(org.eclipse.pde.api.tools.search.IReference)
	 */
	public boolean isMatch(IReference reference) {
		IApiMember member = reference.getResolvedReference();
		if (member != null) {
			try {
				IApiAnnotations annotations = member.getApiComponent().getApiDescription().resolveAnnotations(member.getHandle());
				if (annotations != null) {
					return matchesElementRestrictions(member)
						&& matchesApiRestrictions(annotations);
				}
			} catch (CoreException e) {
				ApiPlugin.log(e.getStatus());
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
	 * Returns whether the given member is contained within the component and
	 * element restrictions of this search criteria.
	 * 
	 * @param member referenced member
	 * @return whether the given member is contained within the component and
	 * element restrictions of this search criteria
	 */
	private boolean matchesElementRestrictions(IApiMember member) {
		if (fComponentIds.isEmpty()) {
			return true;
		}
		return encloses(member.getApiComponent().getId(), member);
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
		return ((reference.getReferenceKind() & fReferenceKinds) > 0) &&
				!isFilteredSourceLocation(reference.getMember()) &&
				matchesSourceModifiers(reference.getMember()) &&
				matchesSourceApiRestrictions(reference.getMember()) &&
				isPotentialElementMatch(reference);
	}
	
	/**
	 * Returns whether the location is a filtered source location.
	 * 
	 * @param location source location
	 * @return whether the location (reference) should be filtered (ignored)
	 */
	private boolean isFilteredSourceLocation(IApiMember location) {
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
	protected boolean matchesSourceApiRestrictions(IApiMember location) {
		if (fSourceVisibility == VisibilityModifiers.ALL_VISIBILITIES && fSourceRestriction == RestrictionModifiers.ALL_RESTRICTIONS) {
			return true;
		}
		IApiComponent apiComponent = location.getApiComponent();
		try {
			IApiAnnotations annotations = apiComponent.getApiDescription().resolveAnnotations(location.getHandle());
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
	protected boolean matchesSourceModifiers(IApiMember member) {
		if (fSourceModifiers == -1) {
			return true;
		}
		while (member != null) {
			int modifiers = member.getModifiers();
			if ((fSourceModifiers & modifiers) > 0 || fSourceModifiers == modifiers) { // in case of Acc.Default (0)
				try {
					member = member.getEnclosingType();
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	protected boolean isPotentialElementMatch(IReference reference) {
		if (fPotentialElements == null) {
			return true;
		}
		Iterator iterator = fPotentialElements.iterator();
		while (iterator.hasNext()) {
			PotentialElementMatch match = (PotentialElementMatch) iterator.next();
			if (match.matches(reference)) {
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
		if(fUserData != null) {
			buffer.append("User Data: ").append(fUserData.toString()); //$NON-NLS-1$
		}
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
