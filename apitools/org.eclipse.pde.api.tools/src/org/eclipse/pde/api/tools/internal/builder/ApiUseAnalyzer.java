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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.search.Location;
import org.eclipse.pde.api.tools.internal.search.Reference;
import org.eclipse.pde.api.tools.internal.util.Util;

import com.ibm.icu.text.MessageFormat;

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
	 * Debugging flag
	 */
	private static final boolean DEBUG = Util.DEBUG;

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
		long start = System.currentTimeMillis();
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
		long end = System.currentTimeMillis();
		if (DEBUG) {
			System.out.println("Time to build search conditions: " + (end-start) + "ms");  //$NON-NLS-1$//$NON-NLS-2$
		}
		return (IApiSearchCriteria[]) condidtions.toArray(new IApiSearchCriteria[condidtions.size()]);
	}
	
	public class CompatibilityResult {
		
		/**
		 * Required component.
		 */
		private IApiComponent fComponent;
		
		/**
		 * Unresolved references, possibly empty.
		 */
		private IReference[] fUnresolved;
		
		
		CompatibilityResult(IApiComponent component, IReference[] unresolved) {
			fComponent = component;
			fUnresolved = unresolved;
		}
		
		/**
		 * Returns the component that was analyzed for compatibility.
		 * 
		 * @return required component
		 */
		public IApiComponent getRequiredComponent() {
			return fComponent;
		}
		
		/**
		 * Returns any references that could not be resolved by the required component.
		 * An empty collection indicates that it is compatible.
		 * 
		 * @return unresolved references, possibly empty
		 */
		public IReference[] getUnresolvedReferences() {
			return fUnresolved;
		}
	}
	
	/**
	 * Analyzes the given required API component for compatibility with the specified components
	 * in other profiles.
	 * 
	 * @param component the component being analyzed for compatibility with required components
	 * 	in other profiles.
	 * @param requiredComponents a collection of a collection of required components to analyze
	 * @param monitor
	 * @return results of analysis or null if canceled
	 * @throws CoreException
	 */
	public CompatibilityResult[] analyzeCompatibility(IApiComponent component, IApiComponent[] requiredComponents, IProgressMonitor monitor) throws CoreException {
		// group components by ID so we can re-use references between like components
		Map components = new HashMap();
		for (int i = 0; i < requiredComponents.length; i++) {
			IApiComponent reqComponent = requiredComponents[i];
			List list = (List) components.get(reqComponent.getId());
			if (list == null) {
				list = new ArrayList();
				components.put(reqComponent.getId(), list);
			}
			list.add(reqComponent);
		}
		CompatibilityResult[] results = new CompatibilityResult[requiredComponents.length];
		Iterator iterator = components.entrySet().iterator();
		SubMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.ApiUseAnalyzer_0, requiredComponents.length + components.size());
		while (iterator.hasNext()) {
			if (localMonitor.isCanceled()) {
				return null;
			}
			Entry entry = (Entry) iterator.next();
			String id = (String) entry.getKey();
			List reqComponents = (List) entry.getValue();
			// extract references between component and required component original profile
			IApiComponent baseComponent = component.getProfile().getApiComponent(id);
			localMonitor.subTask(MessageFormat.format(BuilderMessages.ApiUseAnalyzer_1, new String[]{baseComponent.getId()}));
			IReference[] references = findAllReferences(component, baseComponent, localMonitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
			//localMonitor.worked(1);
			Iterator cIterator = reqComponents.iterator();
			while (cIterator.hasNext()) {
				if (localMonitor.isCanceled()) {
					return null;
				}
				IApiComponent reqComponent = (IApiComponent) cIterator.next();
				IApiComponent sourceComponent = reqComponent.getProfile().getApiComponent(component.getId());
				// recreate unresolved references to re-resolve
				IReference[] unresolved = new IReference[references.length];
				for (int i = 0; i < references.length; i++) {
					IReference reference = references[i];
					ILocation source = new Location(sourceComponent, reference.getSourceLocation().getMember());
					source.setLineNumber(reference.getSourceLocation().getLineNumber());
					ILocation target = new Location(reqComponent, reference.getReferencedLocation().getMember());
					unresolved[i] = new Reference(source, target, reference.getReferenceKind());
				}
				localMonitor.subTask(MessageFormat.format(BuilderMessages.ApiUseAnalyzer_2, new String[]{	reqComponent.getId(), reqComponent.getVersion()}));
				Factory.newSearchEngine().resolveReferences(unresolved, localMonitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
				// collect unresolved references
				List missing = new ArrayList();
				for (int i = 0; i < unresolved.length; i++) {
					IReference reference = unresolved[i];
					if (reference.getResolvedLocation() == null) {
						missing.add(reference);
					}
				}
				int index = indexOf(reqComponent, requiredComponents);
				results[index] = new CompatibilityResult(reqComponent, (IReference[]) missing.toArray(new IReference[missing.size()]));
				//localMonitor.worked(1);
			}
		}
		localMonitor.done();
		monitor.done();
		return results;
	}
	
	/**
	 * Returns the index of the given object in the given array or -1 if not present.
	 * 
	 * @param object
	 * @param array
	 * @return index
	 */
	private int indexOf(Object object, Object[] array) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(object)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Extracts and returns all references that 'from' makes to 'to'.
	 * 
	 * @param from component references are extracted from
	 * @param to component references are to
	 * @return all references
	 * @throws CoreException 
	 */
	private IReference[] findAllReferences(IApiComponent from, IApiComponent to, IProgressMonitor monitor) throws CoreException {
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.addComponentRestriction(to.getId());
		criteria.setConsiderComponentLocalReferences(false);
		criteria.setReferenceKinds(ReferenceModifiers.MASK_REF_ALL, VisibilityModifiers.ALL_VISIBILITIES, RestrictionModifiers.ALL_RESTRICTIONS);
		return engine.search(Factory.newScope(new IApiComponent[]{from}), new IApiSearchCriteria[]{criteria}, monitor);
	}
}
