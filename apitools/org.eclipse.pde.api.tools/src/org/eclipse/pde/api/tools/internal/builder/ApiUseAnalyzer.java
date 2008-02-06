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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;

/**
 * Analyzes a component or scope within a component for illegal API use in prerequisite
 * components.
 *  
 * @since 1.0
 */
public class ApiUseAnalyzer {
	
	/**
	 * Empty reference collection
	 */
	private static final IReference[] EMPTY = new IReference[0];

	/**
	 * Collects search criteria from an API description for usage problems.
	 */
	static class UsageVisitor extends ApiDescriptionVisitor {
		
		private List fConditions;
		private String fOwningComponentId;
		
		/**
		 * @param conditions list to add conditions to
		 */
		UsageVisitor(List conditions) {
			fConditions = conditions;
		}
		
		/**
		 * Sets the owning component (i.e. component of description being visited).
		 * 
		 * @param id
		 */
		void setOwningComponentId(String id) {
			fOwningComponentId = id;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.ApiDescriptionVisitor#visitElement(org.eclipse.pde.api.tools.descriptors.IElementDescriptor, java.lang.String, org.eclipse.pde.api.tools.IApiAnnotations)
		 */
		public boolean visitElement(IElementDescriptor element, String componentContext, IApiAnnotations description) {
			int mask = description.getRestrictions();
			if (!RestrictionModifiers.isUnrestricted(mask)) {
				// if there are restrictions, added to the search list
				IElementDescriptor[] elements = new IElementDescriptor[]{element};
				if (RestrictionModifiers.isExtendRestriction(mask)) {
					if (element.getElementType() == IElementDescriptor.T_METHOD) {
						add(ReferenceModifiers.REF_OVERRIDE, RestrictionModifiers.NO_EXTEND, elements);
					} else if (element.getElementType() == IElementDescriptor.T_REFERENCE_TYPE) {
						add(ReferenceModifiers.REF_EXTENDS, RestrictionModifiers.NO_EXTEND, elements); 
					}
				}
				if (RestrictionModifiers.isImplementRestriction(mask)) {
					add(ReferenceModifiers.REF_IMPLEMENTS, RestrictionModifiers.NO_IMPLEMENT, elements);
				}
				if (RestrictionModifiers.isInstantiateRestriction(mask)) {
					add(ReferenceModifiers.REF_INSTANTIATE, RestrictionModifiers.NO_INSTANTIATE, elements);
				}
				if (RestrictionModifiers.isReferenceRestriction(mask)) {
					if (element.getElementType() == IElementDescriptor.T_METHOD) {
						add(
							ReferenceModifiers.REF_INTERFACEMETHOD | ReferenceModifiers.REF_SPECIALMETHOD |
							ReferenceModifiers.REF_STATICMETHOD | ReferenceModifiers.REF_VIRTUALMETHOD,
							RestrictionModifiers.NO_REFERENCE, elements);
					} else if (element.getElementType() == IElementDescriptor.T_FIELD) {
						add(
							ReferenceModifiers.REF_GETFIELD | ReferenceModifiers.REF_GETSTATIC |
							ReferenceModifiers.REF_PUTFIELD | ReferenceModifiers.REF_PUTSTATIC,
							RestrictionModifiers.NO_REFERENCE, elements);
					}
				}
			}
			return true;
		}
		
		private void add(int refKind, int restriction, IElementDescriptor[] elements) {
			IApiSearchCriteria condition = Factory.newSearchCriteria();
			condition.addElementRestriction(fOwningComponentId, elements);
			condition.setReferenceKinds(refKind, VisibilityModifiers.ALL_VISIBILITIES, restriction);
			fConditions.add(condition);
		}
		
	}	
	/**
	 * Searches the specified scope within the the specified component and returns
	 * reference objects identify illegal API use.
	 * 
	 * @param profile profile being analyzed
	 * @param component component being analyzed
	 * @param scope scope within the component to analyze
	 * @param monitor progress monitor
	 * @exception CoreException if something goes wrong
	 */
	public IReference[] findIllegalApiUse(IApiProfile profile, IApiComponent component, IApiSearchScope scope, IProgressMonitor monitor)  throws CoreException {
		IApiSearchCriteria[] conditions = buildSearchConditions(profile, component);
		if (conditions.length > 0) {
			IApiSearchEngine engine = Factory.newSearchEngine();
			return engine.search(scope, conditions, monitor);
		}
		return EMPTY;
	}
	
	/**
	 * Searches the entire profile and returns reference objects to identify illegal API use.
	 * 
	 * @param profile profile being analyzed
	 * @param monitor progress monitor
	 * @exception CoreException if something goes wrong
	 */
	public IReference[] findIllegalApiUse(IApiProfile profile, IProgressMonitor monitor)  throws CoreException {
		IApiComponent[] components = profile.getApiComponents();
		List references = new ArrayList();
		IProgressMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.ApiAnalyserTaskName, components.length);
		for (int i = 0; i < components.length; i++) {
			if (!monitor.isCanceled()) {
				IApiComponent component = components[i];
				if (!component.isSystemComponent()) {
					IReference[] illegal = findIllegalApiUse(profile, component, Factory.newScope(new IApiComponent[]{component}), localMonitor);
					for (int j = 0; j < illegal.length; j++) {
						references.add(illegal[j]);
					}
				}
				localMonitor.worked(1);
			}
		}
		localMonitor.done();
		return (IReference[]) references.toArray(new IReference[references.size()]);
	}	
	
	/**
	 * Build search conditions for API usage in all prerequisite components for
	 * the given component and its profile.
	 * 
	 * @param profile
	 * @param component component to analyze for API use problems
	 * @return search conditions
	 */
	private IApiSearchCriteria[] buildSearchConditions(IApiProfile profile, IApiComponent component) {
		IApiComponent[] components = profile.getPrerequisiteComponents(new IApiComponent[]{component});
		List condidtions = new ArrayList();
		UsageVisitor visitor = new UsageVisitor(condidtions);
		for (int i = 0; i < components.length; i++) {
			IApiComponent prereq = components[i];
			if (!prereq.equals(component)) {
				visitor.setOwningComponentId(prereq.getId());
				try {
					prereq.getApiDescription().accept(visitor);
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
				}
			}
		}
		return (IApiSearchCriteria[]) condidtions.toArray(new IApiSearchCriteria[condidtions.size()]);
	}	
}
