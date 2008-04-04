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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.api.tools.internal.PluginProjectApiComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchResult;
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
	 * The result of a compatibility check
	 */
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
	 * Collects search criteria from an API description for usage problems.
	 */
	class UsageVisitor extends ApiDescriptionVisitor {
		
		/**
		 * Maps search criteria to associated problem descriptors.
		 */
		private List fConditions;
		
		/**
		 * Identifier of component elements are being searched for in
		 */
		private String fOwningComponentId;
		
		/**
		 * @param conditions list to add conditions to
		 */
		UsageVisitor() {
			fConditions = new ArrayList();
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
		public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
			int mask = description.getRestrictions();
			if (!RestrictionModifiers.isUnrestricted(mask)) {
				// if there are restrictions, added to the search list
				IElementDescriptor[] elements = new IElementDescriptor[]{element};
				if (RestrictionModifiers.isExtendRestriction(mask)) {
					if (element.getElementType() == IElementDescriptor.T_METHOD) {
						add(ReferenceModifiers.REF_OVERRIDE, RestrictionModifiers.NO_EXTEND, elements, IApiProblem.ILLEGAL_OVERRIDE, IElementDescriptor.T_METHOD);
					} else if (element.getElementType() == IElementDescriptor.T_REFERENCE_TYPE) {
						add(ReferenceModifiers.REF_EXTENDS, RestrictionModifiers.NO_EXTEND, elements, IApiProblem.ILLEGAL_EXTEND, IElementDescriptor.T_REFERENCE_TYPE); 
					}
				}
				if (RestrictionModifiers.isImplementRestriction(mask)) {
					add(ReferenceModifiers.REF_IMPLEMENTS, RestrictionModifiers.NO_IMPLEMENT, elements, IApiProblem.ILLEGAL_IMPLEMENT, IElementDescriptor.T_REFERENCE_TYPE);
				}
				if (RestrictionModifiers.isInstantiateRestriction(mask)) {
					add(ReferenceModifiers.REF_INSTANTIATE, RestrictionModifiers.NO_INSTANTIATE, elements, IApiProblem.ILLEGAL_INSTANTIATE, IElementDescriptor.T_REFERENCE_TYPE);
				}
				if (RestrictionModifiers.isReferenceRestriction(mask)) {
					if (element.getElementType() == IElementDescriptor.T_METHOD) {
						add(
							ReferenceModifiers.REF_INTERFACEMETHOD | ReferenceModifiers.REF_SPECIALMETHOD |
							ReferenceModifiers.REF_STATICMETHOD | ReferenceModifiers.REF_VIRTUALMETHOD,
							RestrictionModifiers.NO_REFERENCE, elements, IApiProblem.ILLEGAL_REFERENCE, IElementDescriptor.T_METHOD);
						
					} else if (element.getElementType() == IElementDescriptor.T_FIELD) {
						add(
							ReferenceModifiers.REF_GETFIELD | ReferenceModifiers.REF_GETSTATIC |
							ReferenceModifiers.REF_PUTFIELD | ReferenceModifiers.REF_PUTSTATIC,
							RestrictionModifiers.NO_REFERENCE, elements, IApiProblem.ILLEGAL_REFERENCE, IElementDescriptor.T_FIELD);
					}
				}
			}
			return true;
		}
		
		private void add(int refKind, int restriction, IElementDescriptor[] elements, int problemKind, int elemenType) {
			IApiSearchCriteria condition = Factory.newSearchCriteria();
			condition.addReferencedElementRestriction(fOwningComponentId, elements);
			condition.setReferenceKinds(refKind);
			condition.setReferencedRestrictions(VisibilityModifiers.ALL_VISIBILITIES, restriction);
			condition.setUserData(new ProblemDescriptor(problemKind, elemenType));
			fConditions.add(condition);
		}
		
		/**
		 * Returns search criteria with associated problem descriptions as user data
		 * 
		 * @return search criteria
		 */
		List getConditions() {
			return fConditions;
		}
		
	}	
	
	/**
	 * Describes a kind of problem associated with a search criteria.
	 */
	class ProblemDescriptor {
		private int fKind;
		private int fElementType;
		private int fFlags;
		
		ProblemDescriptor(int kind, int elementType) {
			this(kind, elementType, IApiProblem.NO_FLAGS);
		}
		
		ProblemDescriptor(int kind, int elementType, int flags) {
			fKind = kind;
			fElementType = elementType;
			fFlags = flags;
		}
		
		public int getKind() {
			return fKind;
		}
		
		public int getElementType() {
			return fElementType;
		}
		
		public int getFlags() {
			return fFlags;
		}
	}
	
	/**
	 * Empty reference collection
	 */
	private static final IApiProblem[] EMPTY = new IApiProblem[0];
	
	/**
	 * Debugging flag
	 */
	private static final boolean DEBUG = Util.DEBUG;
	
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
	public IApiProblem[] findIllegalApiUse(IApiProfile profile, IApiComponent component, IApiSearchScope scope, IProgressMonitor monitor)  throws CoreException {
		IApiSearchCriteria[] conditions = buildSearchConditions(profile, component);
		List problems = new ArrayList();
		if (conditions.length > 0) {
			IApiSearchEngine engine = Factory.newSearchEngine();
			IApiSearchResult[] results = engine.search(scope, conditions, monitor);
			IApiProblem problem = null;
			IReference[] references = null;
			ProblemDescriptor desc = null;
			for (int i = 0; i < results.length; i++) {
				references = results[i].getReferences();
				desc = (ProblemDescriptor) results[i].getSearchCriteria().getUserData();
				for (int j = 0; j < references.length; j++) {
					problem = createProblem(desc.getKind(), desc.getElementType(), desc.getFlags(), references[j]);
					if (problem != null) {
						problems.add(problem);
					}
				}
			}
			return (IApiProblem[]) problems.toArray(new IApiProblem[problems.size()]);
		}
		return EMPTY;
	}
	
	/**
	 * Creates an API problem of the given type for the specified element type and
	 * reference that exhibits the problem.
	 * 
	 * @param kind problem kind
	 * @param elementType element type where the problem occurs
	 * @param reference the reference that is a problem
	 * @return API problem
	 */
	private IApiProblem createProblem(int kind, int elementType, int flags, IReference reference) {
		IApiComponent component = reference.getSourceLocation().getApiComponent();
		if (component instanceof PluginProjectApiComponent) {
			PluginProjectApiComponent ppac = (PluginProjectApiComponent) component;
			IJavaProject project = ppac.getJavaProject();
			return createUsageProblem(kind, elementType, flags, reference, project);
		} else {
			return createUsageProblem(kind, elementType, flags, reference);
		}
	}
	
	/**
	 * Returns all references in the given search results.
	 * 
	 * TODO: this method will be deleted
	 * 
	 * @param results
	 * @return
	 * @deprecated to be deleted, and the analyzer will return IApiProblems instead
	 */
	private IReference[] getReferences(IApiSearchResult[] results) {
		if (results.length == 1) {
			return results[0].getReferences();
		}
		if (results.length == 0) {
			return new IReference[0];
		}
		int size = 0;
		for (int i = 0; i < results.length; i++) {
			IApiSearchResult result = results[i];
			size += result.getReferences().length;
		}
		IReference[] refs = new IReference[size];
		int index = 0;
		for (int i = 0; i < results.length; i++) {
			IApiSearchResult result = results[i];
			IReference[] references = result.getReferences();
			System.arraycopy(references, 0, refs, index, references.length);
			index = index + references.length;
		}
		return refs;
	}
	
	/**
	 * Build and return search conditions for API usage in all prerequisite components for
	 * the given component and its profile.
	 * 
	 * @param profile
	 * @param component component to analyze for API use problems
	 * @return search conditions
	 */
	private IApiSearchCriteria[] buildSearchConditions(IApiProfile profile, IApiComponent component) {
		long start = System.currentTimeMillis();
		IApiComponent[] components = profile.getPrerequisiteComponents(new IApiComponent[]{component});
		UsageVisitor visitor = new UsageVisitor();
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
		// Add API leak conditions
		List conditions = visitor.getConditions();
		addLeakCondition(conditions, ReferenceModifiers.REF_EXTENDS, IApiProblem.API_LEAK, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.LEAK_EXTENDS, RestrictionModifiers.ALL_RESTRICTIONS);
		addLeakCondition(conditions, ReferenceModifiers.REF_IMPLEMENTS, IApiProblem.API_LEAK, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.LEAK_IMPLEMENTS, RestrictionModifiers.ALL_RESTRICTIONS);
		addLeakCondition(conditions, ReferenceModifiers.REF_FIELDDECL, IApiProblem.API_LEAK, IElementDescriptor.T_FIELD, IApiProblem.LEAK_FIELD, RestrictionModifiers.ALL_RESTRICTIONS ^ RestrictionModifiers.NO_REFERENCE);
		addLeakCondition(conditions, ReferenceModifiers.REF_PARAMETER, IApiProblem.API_LEAK, IElementDescriptor.T_METHOD, IApiProblem.LEAK_METHOD_PARAMETER, RestrictionModifiers.ALL_RESTRICTIONS ^ RestrictionModifiers.NO_REFERENCE);
		addLeakCondition(conditions, ReferenceModifiers.REF_RETURNTYPE, IApiProblem.API_LEAK, IElementDescriptor.T_METHOD, IApiProblem.LEAK_RETURN_TYPE, RestrictionModifiers.ALL_RESTRICTIONS ^ RestrictionModifiers.NO_REFERENCE);
		return (IApiSearchCriteria[]) conditions.toArray(new IApiSearchCriteria[conditions.size()]);
	}
	
	/**
	 * Creates a new {@link IApiSearchCriteria} for a leak kind and adds it to the 
	 * collector specified
	 * @param conditions
	 * @param refKind
	 * @param problemKind
	 * @param elementType
	 * @param flags
	 * @param restrictions
	 */
	private void addLeakCondition(List conditions, int refKind, int problemKind, int elementType, int flags, int restrictions) {
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setReferenceKinds(refKind);
		criteria.setReferencedRestrictions(VisibilityModifiers.PRIVATE, RestrictionModifiers.ALL_RESTRICTIONS);
		criteria.setSourceRestrictions(VisibilityModifiers.API, restrictions);
		criteria.setSourceModifiers(Flags.AccPublic | Flags.AccProtected);	
		criteria.setUserData(new ProblemDescriptor(problemKind, elementType, flags));
		conditions.add(criteria);
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
		Set reqComponentIds = new HashSet();
		for (int i = 0; i < requiredComponents.length; i++) {
			reqComponentIds.add(requiredComponents[i].getId());
		}
		CompatibilityResult[] results = new CompatibilityResult[requiredComponents.length];
		//extracting references take half the time
		int weight = requiredComponents.length / 2;
		if (weight == 0) {
			weight = 1;
		}
		SubMonitor localMonitor = SubMonitor.convert(monitor, BuilderMessages.Compatibility_Analysis, requiredComponents.length + weight);
		// extract all references by component
		Map referencesById = findAllReferences(component, (String[])reqComponentIds.toArray(new String[reqComponentIds.size()]), localMonitor.newChild(weight, SubMonitor.SUPPRESS_ALL_LABELS));
		if (localMonitor.isCanceled()) {
			return null;
		}
		for (int i = 0; i < requiredComponents.length; i++) {
			if (localMonitor.isCanceled()) {
				return null;
			}
			IApiComponent reqComponent = requiredComponents[i];
			String id = reqComponent.getId();
			localMonitor.subTask(MessageFormat.format(BuilderMessages.Analyzing_0_1, new String[]{id, reqComponent.getVersion()}));
			List references = (List) referencesById.get(id);
			if (references != null) { 
				IApiComponent sourceComponent = reqComponent.getProfile().getApiComponent(component.getId());
				// recreate unresolved references to re-resolve
				IReference[] unresolved = new IReference[references.size()];
				Iterator iterator = references.iterator();
				int index = 0;
				// TODO: there is an issue when a new required plug-in is introduced, as references won't be resolved
				// in the "old profile" that does not have the required plug-in
				// TODO: there is an issue with new packages introduced in the base plug-in, as they cannot be resolved
				// in the "old profile"
				while (iterator.hasNext()) {
					IReference reference = (IReference) iterator.next();
					ILocation source = new Location(sourceComponent, reference.getSourceLocation().getMember());
					source.setLineNumber(reference.getSourceLocation().getLineNumber());
					ILocation target = new Location(reqComponent, reference.getReferencedLocation().getMember());
					unresolved[index++] = new Reference(source, target, reference.getReferenceKind());
				}				
				Factory.newSearchEngine().resolveReferences(unresolved, localMonitor.newChild(1, SubMonitor.SUPPRESS_ALL_LABELS));
				// collect unresolved references
				List missing = new ArrayList();
				for (int j = 0; j < unresolved.length; j++) {
					IReference reference = unresolved[j];
					if (reference.getResolvedLocation() == null) {
						missing.add(reference);
					}
				}
				results[i] = new CompatibilityResult(reqComponent, (IReference[]) missing.toArray(new IReference[missing.size()]));
			} else {
				// no references... compatible
				results[i] = new CompatibilityResult(reqComponent, new IReference[0]);
				localMonitor.worked(1);
			}
		}
		localMonitor.done();
		monitor.done();
		return results;
	}
	
	/**
	 * Extracts and returns all references that 'from' makes to 'IDs', in a map keyed by ID.
	 * 
	 * @param from component references are extracted from
	 * @param ids component IDs references are to
	 * @return map of <String> -> <IReference[]>
	 * @throws CoreException 
	 */
	private Map findAllReferences(IApiComponent from, String[] ids, IProgressMonitor monitor) throws CoreException {
		IApiSearchEngine engine = Factory.newSearchEngine();
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		for (int i = 0; i < ids.length; i++) {
			criteria.addReferencedComponentRestriction(ids[i]);
		}
		criteria.setReferenceKinds(ReferenceModifiers.MASK_REF_ALL);
		criteria.setReferencedRestrictions(VisibilityModifiers.ALL_VISIBILITIES, RestrictionModifiers.ALL_RESTRICTIONS);
		IReference[] all = getReferences(engine.search(Factory.newScope(new IApiComponent[]{from}), new IApiSearchCriteria[]{criteria}, monitor));
		Map map = new HashMap(ids.length);
		for (int i = 0; i < all.length; i++) {
			IReference reference = all[i];
			ILocation location = reference.getResolvedLocation();
			if (location != null) {
				IApiComponent component = location.getApiComponent();
				List list = (List) map.get(component.getId());
				if (list == null) {
					list = new LinkedList();
					map.put(component.getId(), list);
				}
				list.add(reference);
			}
		}
		return map;
	}
	
	/**
	 * Creates an {@link IApiProblem} for the given illegal reference.
	 * 
	 * @param reference illegal reference
	 * @param project project the compilation unit is in
	 * @return a new {@link IApiProblem} or <code>null</code>
	 */
	private IApiProblem createUsageProblem(int kind, int elementType, int flags, IReference reference, IJavaProject project) {
		try {
			ILocation location = reference.getSourceLocation();
			IReferenceTypeDescriptor refType = location.getType();
			String lookupName = null;
			if (refType.getEnclosingType() == null) {
				lookupName = refType.getQualifiedName();
			} else {
				lookupName = refType.getQualifiedName().replace('$', '.');
			}
			IType type = project.findType(lookupName);
			if (type == null) {
				return null;
			}
			ICompilationUnit compilationUnit = type.getCompilationUnit();
			if (compilationUnit == null) {
				return null;
			}
			IResource resource = compilationUnit.getCorrespondingResource();
			if (resource == null) {
				return null;
			}
			IDocument document = Util.getDocument(compilationUnit);
			// retrieve line number, char start and char end
			int lineNumber = location.getLineNumber();
			int charStart = -1;
			int charEnd = -1;
			
			String prefKey = null;
			ILocation resolvedLocation = reference.getResolvedLocation();
			String qualifiedTypeName = resolvedLocation.getType().getQualifiedName();
			IMemberDescriptor member = resolvedLocation.getMember();
			String[] messageargs = null;
			try {
				switch(kind) {
					case IApiProblem.ILLEGAL_IMPLEMENT : {
						prefKey = IApiProblemTypes.ILLEGAL_IMPLEMENT;
						messageargs = new String[] {qualifiedTypeName};
						// report error on the type
						ISourceRange range = type.getNameRange();
						charStart = range.getOffset();
						charEnd = charStart + range.getLength();
						lineNumber = document.getLineOfOffset(charStart);
						break;
					}
					case IApiProblem.ILLEGAL_EXTEND : {
						prefKey = IApiProblemTypes.ILLEGAL_EXTEND;
						messageargs = new String[] {qualifiedTypeName};
						// report error on the type
						ISourceRange range = type.getNameRange();
						charStart = range.getOffset();
						charEnd = charStart + range.getLength();
						lineNumber = document.getLineOfOffset(charStart);
						break;
					}
					case IApiProblem.ILLEGAL_INSTANTIATE : {
						prefKey = IApiProblemTypes.ILLEGAL_INSTANTIATE;
						messageargs = new String[] {qualifiedTypeName};
						int linenumber = (lineNumber == 0 ? 0 : lineNumber -1);
						IReferenceTypeDescriptor typeDesc = (IReferenceTypeDescriptor) member;
						int offset = document.getLineOffset(linenumber);
						String line = document.get(offset, document.getLineLength(linenumber));
						String qname = typeDesc.getQualifiedName();
						int first = line.indexOf(qname);
						if(first < 0) {
							qname = typeDesc.getName();
							first = line.indexOf(qname);
						}
						if(first > -1) {
							charStart = offset + first;
							charEnd = charStart + qname.length();
						}
						//TODO support the call to 'super'
						break;
					}
					case IApiProblem.ILLEGAL_OVERRIDE : {
						IMethodDescriptor method = (IMethodDescriptor) member;
						prefKey = IApiProblemTypes.ILLEGAL_OVERRIDE;
						messageargs = new String[] {method.getEnclosingType().getQualifiedName(), Signature.toString(method.getSignature(), method.getName(), null, false, false)};
						// report the marker on the method
						String[] parameterTypes = Signature.getParameterTypes(method.getSignature());
						for (int i = 0; i < parameterTypes.length; i++) {
							parameterTypes[i] = parameterTypes[i].replace('/', '.');
						}
						IMethod Qmethod = type.getMethod(method.getName(), parameterTypes);
						IMethod[] methods = type.getMethods();
						IMethod match = null;
						for (int i = 0; i < methods.length; i++) {
							IMethod m = methods[i];
							if (m.isSimilar(Qmethod)) {
								match = m;
								break;
							}
						}
						if (match != null) {
							ISourceRange range = match.getNameRange();
							charStart = range.getOffset();
							charEnd = charStart + range.getLength();
							lineNumber = document.getLineOfOffset(charStart);
						}					
						break;
					}
					case IApiProblem.ILLEGAL_REFERENCE: {
						prefKey = IApiProblemTypes.ILLEGAL_REFERENCE;
						switch (elementType) {
							case IElementDescriptor.T_METHOD: {
								IMethodDescriptor method = (IMethodDescriptor) member;							
								messageargs = new String[] {method.getEnclosingType().getQualifiedName(), Signature.toString(method.getSignature(), method.getName(), null, false, false)};
								int linenumber = (lineNumber == 0 ? 0 : lineNumber -1);
								int offset = document.getLineOffset(linenumber);
								String line = document.get(offset, document.getLineLength(linenumber));
								String name = method.getName();
								int first = line.indexOf(name);
								if(first > -1) {
									charStart = offset + first;
									charEnd = charStart + name.length();
								}							
								break;
							}
							case IElementDescriptor.T_FIELD: {
								IFieldDescriptor field = (IFieldDescriptor) member;
								messageargs = new String[] {field.getEnclosingType().getQualifiedName(), field.getName()};
								String name = field.getName();
								int linenumber = (lineNumber == 0 ? 0 : lineNumber -1);
								int offset = document.getLineOffset(linenumber);
								String line = document.get(offset, document.getLineLength(linenumber));
								IReferenceTypeDescriptor parent = field.getEnclosingType();
								String qname = parent.getQualifiedName()+"."+name; //$NON-NLS-1$
								int first = line.indexOf(qname);
								if(first < 0) {
									qname = parent.getName()+"."+name; //$NON-NLS-1$
									first = line.indexOf(qname);
								}
								if(first < 0) {
									qname = "super."+name; //$NON-NLS-1$
									first = line.indexOf(qname);
								}
								if(first < 0) {
									qname = "this."+name; //$NON-NLS-1$
									first = line.indexOf(qname);
								}
								if(first > -1) {
									charStart = offset + first;
									charEnd = charStart + qname.length();
								}
								else {
									//optimistically select the whole line since we can't find the correct variable name and we can't just select
									//the first occurrence 
									charStart = offset;
									charEnd = offset + line.length();
								}							
								break;
							}
						}
						
						break;
					}
					case IApiProblem.API_LEAK: {
						prefKey = IApiProblemTypes.API_LEAK;
						messageargs = new String[] {qualifiedTypeName};
						switch (flags) {
							case IApiProblem.LEAK_EXTENDS:
							case IApiProblem.LEAK_IMPLEMENTS: {
								// report error on the type
								ISourceRange range = type.getNameRange();
								charStart = range.getOffset();
								charEnd = charStart + range.getLength();
								lineNumber = document.getLineOfOffset(charStart);
								break;
							}
							case IApiProblem.LEAK_FIELD: {
								IFieldDescriptor field = (IFieldDescriptor) reference.getSourceLocation().getMember();
								if ((Flags.AccProtected & field.getModifiers()) > 0) {
									// ignore protected members if contained in a @noextend type
									IApiDescription description = reference.getSourceLocation().getApiComponent().getApiDescription();
									IApiAnnotations annotations = description.resolveAnnotations(field.getEnclosingType());
									if(annotations == null) {
										return null;
									}
									if (RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
										// ignore
										return null;
									}
								}
								IField javaField = type.getField(field.getName());
								if (javaField.exists()) {
									ISourceRange range = javaField.getNameRange();
									charStart = range.getOffset();
									charEnd = charStart + range.getLength();
									lineNumber = document.getLineOfOffset(charStart);
								}
								break;
							}
							case IApiProblem.LEAK_METHOD_PARAMETER:
							case IApiProblem.LEAK_RETURN_TYPE: {
								IMethodDescriptor method = (IMethodDescriptor) reference.getSourceLocation().getMember();
								if ((Flags.AccProtected & method.getModifiers()) > 0) {
									// ignore protected members if contained in a @noextend type
									IApiDescription description = reference.getSourceLocation().getApiComponent().getApiDescription();
									IApiAnnotations annotations = description.resolveAnnotations(method.getEnclosingType());
									if (RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
										// ignore
										return null;
									}
								}								
								// report the marker on the method
								// TODO: can we just lookup the method with resolved signature?
								String[] parameterTypes = Signature.getParameterTypes(method.getSignature());
								for (int i = 0; i < parameterTypes.length; i++) {
									parameterTypes[i] = parameterTypes[i].replace('/', '.');
								}
								IMethod Qmethod = type.getMethod(method.getName(), parameterTypes);
								IMethod[] methods = type.getMethods();
								IMethod match = null;
								for (int i = 0; i < methods.length; i++) {
									IMethod m = methods[i];
									if (m.isSimilar(Qmethod)) {
										match = m;
										break;
									}
								}
								if (match != null) {
									ISourceRange range = match.getNameRange();
									charStart = range.getOffset();
									charEnd = charStart + range.getLength();
									lineNumber = document.getLineOfOffset(charStart);
								}										
								break;
							}
						}
						break;
					}
				} 
			} catch (BadLocationException e) {
				ApiPlugin.log(e);
			}
			if (ApiPlugin.getDefault().getSeverityLevel(prefKey, project.getProject()) == ApiPlugin.SEVERITY_IGNORE) {
				return null;
			}
			IJavaElement element = compilationUnit;
			if(charStart > -1) {
				element = compilationUnit.getElementAt(charStart);
			}
			return ApiProblemFactory.newApiUsageProblem(resource.getProjectRelativePath().toPortableString(), 
					messageargs, 
					new String[] {IApiMarkerConstants.MARKER_ATTR_HANDLE_ID, IApiMarkerConstants.API_MARKER_ATTR_ID}, 
					new Object[] {(element == null ? compilationUnit.getHandleIdentifier() : element.getHandleIdentifier()),
								   new Integer(IApiMarkerConstants.API_USAGE_MARKER_ID)}, 
					lineNumber, 
					charStart, 
					charEnd, 
					elementType, 
					kind,
					flags);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return null;
	}
	
	/**
	 * Creates an {@link IApiProblem} for the given illegal reference.
	 * 
	 * @param reference illegal reference
	 * @return a new {@link IApiProblem} or <code>null</code>
	 */
	private IApiProblem createUsageProblem(int kind, int elementType, int flags, IReference reference) {
		ILocation location = reference.getSourceLocation();
		IReferenceTypeDescriptor refType = location.getType();
		int lineNumber = location.getLineNumber();			
		ILocation resolvedLocation = reference.getResolvedLocation();
		String qualifiedTypeName = resolvedLocation.getType().getQualifiedName();
		IMemberDescriptor member = resolvedLocation.getMember();
		String[] messageargs = null;
		switch(kind) {
			case IApiProblem.ILLEGAL_IMPLEMENT : {
				messageargs = new String[] {qualifiedTypeName};
				break;
			}
			case IApiProblem.ILLEGAL_EXTEND : {
				messageargs = new String[] {qualifiedTypeName};
				break;
			}
			case IApiProblem.ILLEGAL_INSTANTIATE : {
				messageargs = new String[] {qualifiedTypeName};
				break;
			}
			case IApiProblem.ILLEGAL_OVERRIDE : {
				IMethodDescriptor method = (IMethodDescriptor) member;
				messageargs = new String[] {method.getEnclosingType().getQualifiedName(), Signature.toString(method.getSignature(), method.getName(), null, false, false)};
				break;
			}
			case IApiProblem.ILLEGAL_REFERENCE: {
				switch (elementType) {
					case IElementDescriptor.T_METHOD: {
						IMethodDescriptor method = (IMethodDescriptor) member;							
						messageargs = new String[] {method.getEnclosingType().getQualifiedName(), Signature.toString(method.getSignature(), method.getName(), null, false, false)};
						break;
					}
					case IElementDescriptor.T_FIELD: {
						IFieldDescriptor field = (IFieldDescriptor) member;
						messageargs = new String[] {field.getEnclosingType().getQualifiedName(), field.getName()};
						break;
					}
				}						
				break;
			}
			case IApiProblem.API_LEAK: {
				messageargs = new String[] {qualifiedTypeName};
				try {
					switch (flags) {
						case IApiProblem.LEAK_FIELD: {
							IFieldDescriptor field = (IFieldDescriptor) reference.getSourceLocation().getMember();
							if ((Flags.AccProtected & field.getModifiers()) > 0) {
								// ignore protected members if contained in a @noextend type
								IApiDescription description = reference.getSourceLocation().getApiComponent().getApiDescription();
								IApiAnnotations annotations = description.resolveAnnotations(field.getEnclosingType());
								if(annotations == null) {
									return null;
								}
								if (RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
									// ignore
									return null;
								}
							}
							break;
						}
						case IApiProblem.LEAK_METHOD_PARAMETER:
						case IApiProblem.LEAK_RETURN_TYPE: {
							IMethodDescriptor method = (IMethodDescriptor) reference.getSourceLocation().getMember();
							if ((Flags.AccProtected & method.getModifiers()) > 0) {
								// ignore protected members if contained in a @noextend type
								IApiDescription description = reference.getSourceLocation().getApiComponent().getApiDescription();
								IApiAnnotations annotations = description.resolveAnnotations(method.getEnclosingType());
								if(annotations == null) {
									return null;
								}
								if (RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
									// ignore
									return null;
								}
							}								
							break;
						}
					}				
				} catch (CoreException e) {
					// unable to retrieve API description
					ApiPlugin.log(e.getStatus());
				}
				break;
			}
		} 
		return ApiProblemFactory.newApiUsageProblem(refType.getQualifiedName(), 
				messageargs, 
				new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID}, 
				new Object[] {new Integer(IApiMarkerConstants.API_USAGE_MARKER_ID)}, 
				lineNumber, 
				-1, 
				-1,
				elementType, 
				kind,
				flags);
	}	
}
