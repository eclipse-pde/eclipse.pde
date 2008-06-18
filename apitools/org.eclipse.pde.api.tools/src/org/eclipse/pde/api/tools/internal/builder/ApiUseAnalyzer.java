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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.pde.api.tools.internal.search.MethodSearchCriteria;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Analyzes a component or scope within a component for illegal API use in prerequisite
 * components.
 *  
 * @since 1.0
 */
public class ApiUseAnalyzer {
	
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
				if(RestrictionModifiers.isOverrideRestriction(mask)) {
					addCriteria(ReferenceModifiers.REF_OVERRIDE, RestrictionModifiers.NO_OVERRIDE, elements, IApiProblem.ILLEGAL_OVERRIDE, IElementDescriptor.T_METHOD);
				}
				if (RestrictionModifiers.isExtendRestriction(mask)) {
					addCriteria(ReferenceModifiers.REF_EXTENDS, RestrictionModifiers.NO_EXTEND, elements, IApiProblem.ILLEGAL_EXTEND, IElementDescriptor.T_REFERENCE_TYPE); 
				}
				if (RestrictionModifiers.isImplementRestriction(mask)) {
					addCriteria(ReferenceModifiers.REF_IMPLEMENTS, RestrictionModifiers.NO_IMPLEMENT, elements, IApiProblem.ILLEGAL_IMPLEMENT, IElementDescriptor.T_REFERENCE_TYPE);
					addCriteria(ReferenceModifiers.REF_EXTENDS, RestrictionModifiers.NO_IMPLEMENT, elements, IApiProblem.ILLEGAL_IMPLEMENT, IElementDescriptor.T_REFERENCE_TYPE);
				}
				if (RestrictionModifiers.isInstantiateRestriction(mask)) {
					addCriteria(ReferenceModifiers.REF_INSTANTIATE, RestrictionModifiers.NO_INSTANTIATE, elements, IApiProblem.ILLEGAL_INSTANTIATE, IElementDescriptor.T_REFERENCE_TYPE);
				}
				if (RestrictionModifiers.isReferenceRestriction(mask)) {
					if (element.getElementType() == IElementDescriptor.T_METHOD) {
						addCriteria(ReferenceModifiers.REF_INTERFACEMETHOD | ReferenceModifiers.REF_SPECIALMETHOD |
							ReferenceModifiers.REF_STATICMETHOD | ReferenceModifiers.REF_VIRTUALMETHOD | ReferenceModifiers.REF_CONSTRUCTORMETHOD,
							RestrictionModifiers.NO_REFERENCE, elements, IApiProblem.ILLEGAL_REFERENCE, IElementDescriptor.T_METHOD);
						
					} else if (element.getElementType() == IElementDescriptor.T_FIELD) {
						addCriteria(ReferenceModifiers.REF_GETFIELD | ReferenceModifiers.REF_GETSTATIC |
							ReferenceModifiers.REF_PUTFIELD | ReferenceModifiers.REF_PUTSTATIC,
							RestrictionModifiers.NO_REFERENCE, elements, IApiProblem.ILLEGAL_REFERENCE, IElementDescriptor.T_FIELD);
					}
				}
			}
			return true;
		}
		
		private void addCriteria(int refKind, int restriction, IElementDescriptor[] elements, int problemKind, int elemenType) {
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
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Problem Descriptor["); //$NON-NLS-1$
			buffer.append(Util.getProblemKind(IApiProblem.CATEGORY_USAGE, fKind)).append(", "); //$NON-NLS-1$
			buffer.append(Util.getProblemElementKind(IApiProblem.CATEGORY_USAGE, fElementType)).append(", "); //$NON-NLS-1$
			buffer.append(Util.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, fFlags));
			buffer.append("]\n"); //$NON-NLS-1$
			return buffer.toString();
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
	 * @param component component being analyzed
	 * @param scope scope within the component to analyze
	 * @param monitor progress monitor
	 * @exception CoreException if something goes wrong
	 */
	public IApiProblem[] findIllegalApiUse(IApiComponent component, IApiSearchScope scope, IProgressMonitor monitor)  throws CoreException {
		IApiSearchCriteria[] conditions = buildSearchConditions(component);
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
	 * Build and return search conditions for API usage in all prerequisite components for
	 * the given component and its profile.
	 * 
	 * @param component component to analyze for API use problems
	 * @return search conditions
	 */
	private IApiSearchCriteria[] buildSearchConditions(IApiComponent component) {
		long start = System.currentTimeMillis();
		IApiComponent[] components = component.getProfile().getPrerequisiteComponents(new IApiComponent[]{component});
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
		
		conditions.add(createLeakCondition(ReferenceModifiers.REF_EXTENDS, IApiProblem.API_LEAK, IElementDescriptor.T_REFERENCE_TYPE, 
				IApiProblem.LEAK_EXTENDS, RestrictionModifiers.ALL_RESTRICTIONS, Flags.AccPublic | Flags.AccProtected));
		conditions.add(createLeakCondition(ReferenceModifiers.REF_IMPLEMENTS, IApiProblem.API_LEAK, IElementDescriptor.T_REFERENCE_TYPE, 
				IApiProblem.LEAK_IMPLEMENTS, RestrictionModifiers.ALL_RESTRICTIONS, Flags.AccPublic | Flags.AccProtected));
		conditions.add(createLeakCondition(ReferenceModifiers.REF_FIELDDECL, IApiProblem.API_LEAK, IElementDescriptor.T_FIELD, 
				IApiProblem.LEAK_FIELD, RestrictionModifiers.ALL_RESTRICTIONS ^ RestrictionModifiers.NO_REFERENCE, Flags.AccPublic | Flags.AccProtected));
		
		//leaks return types
		MethodSearchCriteria criteria = new MethodSearchCriteria(ReferenceModifiers.REF_RETURNTYPE, new ProblemDescriptor(IApiProblem.API_LEAK, IElementDescriptor.T_METHOD, IApiProblem.LEAK_RETURN_TYPE));
		conditions.add(criteria);
		
		//leaks parameters
		criteria = new MethodSearchCriteria(ReferenceModifiers.REF_PARAMETER, new ProblemDescriptor(IApiProblem.API_LEAK, IElementDescriptor.T_METHOD, IApiProblem.LEAK_METHOD_PARAMETER));
		conditions.add(criteria);
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
	private IApiSearchCriteria createLeakCondition(int refKind, int problemKind, int elementType, int flags, int restrictions, int sourcevis) {
		IApiSearchCriteria criteria = Factory.newSearchCriteria();
		criteria.setReferenceKinds(refKind);
		criteria.setReferencedRestrictions(VisibilityModifiers.PRIVATE, RestrictionModifiers.ALL_RESTRICTIONS);
		criteria.setSourceRestrictions(VisibilityModifiers.API, restrictions);
		criteria.setSourceModifiers(sourcevis);	
		criteria.setUserData(new ProblemDescriptor(problemKind, elementType, flags));
		return criteria;
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
			String typename = resolvedLocation.getType().getName();
			IMemberDescriptor member = resolvedLocation.getMember();
			String[] messageargs = null;
			//modifiable flags that are sent to the problem creator 
			int resolvedflags = flags;
			try {
				switch(kind) {
					case IApiProblem.ILLEGAL_IMPLEMENT : {
						prefKey = IApiProblemTypes.ILLEGAL_IMPLEMENT;
						messageargs = new String[] {typename, type.getElementName()};
						// report error on the type
						ISourceRange range = type.getNameRange();
						charStart = range.getOffset();
						charEnd = charStart + range.getLength();
						lineNumber = document.getLineOfOffset(charStart);
						break;
					}
					case IApiProblem.ILLEGAL_EXTEND : {
						prefKey = IApiProblemTypes.ILLEGAL_EXTEND;
						messageargs = new String[] {typename, type.getElementName()};
						// report error on the type
						ISourceRange range = type.getNameRange();
						charStart = range.getOffset();
						charEnd = charStart + range.getLength();
						lineNumber = document.getLineOfOffset(charStart);
						break;
					}
					case IApiProblem.ILLEGAL_INSTANTIATE : {
						prefKey = IApiProblemTypes.ILLEGAL_INSTANTIATE;
						messageargs = new String[] {typename, type.getElementName()};
						int linenumber = (lineNumber == 0 ? 0 : lineNumber -1);
						IReferenceTypeDescriptor typeDesc = (IReferenceTypeDescriptor) member;
						int offset = document.getLineOffset(linenumber);
						String line = document.get(offset, document.getLineLength(linenumber));
						String qname = typeDesc.getQualifiedName();
						int first = findMethodNameStart(qname, line, 0);
						if(first < 0) {
							qname = typeDesc.getName();
							first = findMethodNameStart(qname, line, 0);
						}
						//TODO might be implicit check for source location type name
						if(first < 0) {
							qname = "super"; //$NON-NLS-1$
							first = line.indexOf(qname);
						}
						if(first > -1) {
							charStart = offset + first;
							charEnd = charStart + qname.length();
						}
						break;
					}
					case IApiProblem.ILLEGAL_OVERRIDE : {
						IMethodDescriptor method = (IMethodDescriptor) member;
						prefKey = IApiProblemTypes.ILLEGAL_OVERRIDE;
						String sig = Signature.toString(method.getSignature(), method.getName(), null, false, false);
						messageargs = new String[] {
								method.getEnclosingType().getName(),
								type.getElementName(),
								sig};
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
								messageargs = new String[] {
										method.getEnclosingType().getName(), 
										type.getElementName(), 
										Signature.toString(method.getSignature(), method.getName(), null, false, false)};
								resolvedflags = IApiProblem.METHOD;
								int linenumber = (lineNumber == 0 ? 0 : lineNumber -1);
								int offset = document.getLineOffset(linenumber);
								String line = document.get(offset, document.getLineLength(linenumber));
								String name = method.getName();
								if(method.isConstructor()) {
									name = method.getEnclosingType().getName();
									resolvedflags = IApiProblem.CONSTRUCTOR_METHOD;
									messageargs = new String[] {
											Signature.toString(method.getSignature(), method.getEnclosingType().getName(), null, false, false), 
											type.getElementName()};
								}
								int first = findMethodNameStart(name, line, 0);
								if(first < 0) {
									name = "super"; //$NON-NLS-1$
								}
								first = findMethodNameStart(name, line, 0);
								if(first > -1) {
									charStart = offset + first;
									charEnd = charStart + name.length();
								}
								break;
							}
							case IElementDescriptor.T_FIELD: {
								IFieldDescriptor field = (IFieldDescriptor) member;
								messageargs = new String[] {field.getEnclosingType().getName(), type.getElementName(), field.getName()};
								resolvedflags = IApiProblem.FIELD;
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
								if(first < 0) {
									//try a pattern [.*fieldname] 
									//the field might be ref'd via a constant, e.g. enum constant
									int idx = line.indexOf(name);
									while(idx > -1) {
										if(line.charAt(idx-1) == '.') {
											first = idx;
											qname = name;
											break;
										}
										idx = line.indexOf(name, idx+1);
									}
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
						switch (flags) {
							case IApiProblem.LEAK_EXTENDS:
							case IApiProblem.LEAK_IMPLEMENTS: {
								prefKey = (flags == IApiProblem.LEAK_EXTENDS ? IApiProblemTypes.LEAK_EXTEND : IApiProblemTypes.LEAK_IMPLEMENT);
								// report error on the type
								ISourceRange range = type.getNameRange();
								charStart = range.getOffset();
								charEnd = charStart + range.getLength();
								lineNumber = document.getLineOfOffset(charStart);
								messageargs = new String[] {typename, type.getElementName()};
								break;
							}
							case IApiProblem.LEAK_FIELD: {
								prefKey = IApiProblemTypes.LEAK_FIELD_DECL;
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
								messageargs = new String[] {typename, type.getElementName(), field.getName()};
								break;
							}
							case IApiProblem.LEAK_METHOD_PARAMETER:
							case IApiProblem.LEAK_RETURN_TYPE: {
								prefKey = (flags == IApiProblem.LEAK_RETURN_TYPE ? IApiProblemTypes.LEAK_METHOD_RETURN_TYPE : IApiProblemTypes.LEAK_METHOD_PARAM);
								IMethodDescriptor method = (IMethodDescriptor) reference.getSourceLocation().getMember();
								if ((Flags.AccProtected & method.getModifiers()) > 0) {
									// ignore protected members if contained in a @noextend type
									IApiDescription description = reference.getSourceLocation().getApiComponent().getApiDescription();
									IApiAnnotations annotations = description.resolveAnnotations(method.getEnclosingType());
									if (annotations == null || RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
										// ignore
										return null;
									}
								}
								// report the marker on the method
								String[] parameterTypes = Signature.getParameterTypes(method.getSignature());
								for (int i = 0; i < parameterTypes.length; i++) {
									parameterTypes[i] = parameterTypes[i].replace('/', '.');
								}
								String methodname = method.getName();
								if(method.isConstructor()) {
									methodname = method.getEnclosingType().getName();
									resolvedflags = IApiProblem.LEAK_CONSTRUCTOR_PARAMETER;
								}
								IMethod Qmethod = type.getMethod(methodname, parameterTypes);
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
								messageargs = new String[] {typename, type.getElementName(), Signature.toString(method.getSignature(), methodname, null, false, false)};
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
					type.getFullyQualifiedName(),
					messageargs, 
					new String[] {IApiMarkerConstants.MARKER_ATTR_HANDLE_ID, IApiMarkerConstants.API_MARKER_ATTR_ID}, 
					new Object[] {(element == null ? compilationUnit.getHandleIdentifier() : element.getHandleIdentifier()),
								   new Integer(IApiMarkerConstants.API_USAGE_MARKER_ID)}, 
					lineNumber, 
					charStart, 
					charEnd, 
					elementType, 
					kind,
					resolvedflags);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return null;
	}
	
	/**
	 * Finds the method name to select on the given line of code starting from the given index.
	 * This method will recurse to find a method name in the even there is a name clash with the type.
	 * For example:
	 * <pre>
	 * 		MyType type = new MyType();
	 * </pre>
	 * If we are trying to find the constructor method call we have a name collision (and the first occurrence of MyType would be selected). 
	 * <br>
	 * A name is determined to be a method name if it is followed by a '(' character (excluding spaces)
	 * @param namepart
	 * @param line
	 * @param index
	 * @return the index of the method name on the given line or -1 if not found
	 */
	private int findMethodNameStart(String namepart, String line, int index) {
		int start = line.indexOf(namepart, index);
		if(start < 0) {
			return -1;
		}
		int offset = start+namepart.length();
		while(line.charAt(offset) == ' ') {
			offset++;
		}
		if(line.charAt(offset) == '(') {
			return start;
		}
		return findMethodNameStart(namepart, line, offset);
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
		String typename = resolvedLocation.getType().getQualifiedName();
		String ltypename = location.getType().getQualifiedName();
		IMemberDescriptor member = resolvedLocation.getMember();
		String[] messageargs = null;
		int resolvedflags = flags;
		switch(kind) {
			case IApiProblem.ILLEGAL_IMPLEMENT : {
				messageargs = new String[] {typename, ltypename};
				break;
			}
			case IApiProblem.ILLEGAL_EXTEND : {
				messageargs = new String[] {typename, ltypename};
				break;
			}
			case IApiProblem.ILLEGAL_INSTANTIATE : {
				messageargs = new String[] {typename, ltypename};
				break;
			}
			case IApiProblem.ILLEGAL_OVERRIDE : {
				IMethodDescriptor method = (IMethodDescriptor) member;
				messageargs = new String[] {
						method.getEnclosingType().getQualifiedName(),
						ltypename,
						Signature.toString(method.getSignature(), method.getName(), null, false, false)};
				break;
			}
			case IApiProblem.ILLEGAL_REFERENCE: {
				switch (elementType) {
					case IElementDescriptor.T_METHOD: {
						IMethodDescriptor method = (IMethodDescriptor) member;
						String methodname = method.getName();
						resolvedflags = IApiProblem.METHOD;
						if(method.isConstructor()) {
							methodname = method.getEnclosingType().getName();
							resolvedflags = IApiProblem.CONSTRUCTOR_METHOD;
						}
						messageargs = new String[] {method.getEnclosingType().getQualifiedName(), ltypename, Signature.toString(method.getSignature(), methodname, null, false, false)};
						break;
					}
					case IElementDescriptor.T_FIELD: {
						IFieldDescriptor field = (IFieldDescriptor) member;
						messageargs = new String[] {field.getEnclosingType().getQualifiedName(), ltypename, field.getName()};
						resolvedflags = IApiProblem.FIELD;
						break;
					}
				}
				break;
			}
			case IApiProblem.API_LEAK: {
				try {
					switch (flags) {
						case IApiProblem.LEAK_EXTENDS:
						case IApiProblem.LEAK_IMPLEMENTS: {
							messageargs = new String[] {typename, ltypename};
							break;
						}
						case IApiProblem.LEAK_FIELD: {
							IFieldDescriptor field = (IFieldDescriptor) reference.getSourceLocation().getMember();
							if ((Flags.AccProtected & field.getModifiers()) > 0) {
								// ignore protected members if contained in a @noextend type
								IApiDescription description = reference.getSourceLocation().getApiComponent().getApiDescription();
								IApiAnnotations annotations = description.resolveAnnotations(field.getEnclosingType());
								if (annotations == null || RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
									// ignore
									return null;
								}
							}
							messageargs = new String[] {typename, ltypename, field.getName()};
							break;
						}
						case IApiProblem.LEAK_METHOD_PARAMETER:
						case IApiProblem.LEAK_RETURN_TYPE: {
							IMethodDescriptor method = (IMethodDescriptor) reference.getSourceLocation().getMember();
							if ((Flags.AccProtected & method.getModifiers()) > 0) {
								// ignore protected members if contained in a @noextend type
								IApiDescription description = reference.getSourceLocation().getApiComponent().getApiDescription();
								IApiAnnotations annotations = description.resolveAnnotations(method.getEnclosingType());
								if (annotations == null || RestrictionModifiers.isOverrideRestriction(annotations.getRestrictions())) {
									// ignore
									return null;
								}
							}
							String name = method.getName();
							if(method.isConstructor()) {
								name = method.getEnclosingType().getName();
								resolvedflags = IApiProblem.LEAK_CONSTRUCTOR_PARAMETER;
							}
							messageargs = new String[] {
									typename,
									ltypename,
									Signature.toString(method.getSignature(), name, null, false, false)};
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
		return ApiProblemFactory.newApiUsageProblem(
				null,
				refType.getQualifiedName(),
				messageargs, 
				new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID}, 
				new Object[] {new Integer(IApiMarkerConstants.API_USAGE_MARKER_ID)}, 
				lineNumber, 
				-1, 
				-1,
				elementType, 
				kind,
				resolvedflags);
	}	
}
