/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.model.ApiType;
import org.eclipse.pde.api.tools.internal.model.MethodKey;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Detects leaked super types.
 *
 * @since 1.1
 */
public class LeakExtendsProblemDetector extends AbstractTypeLeakDetector {

	int problemFlags = IApiProblem.LEAK_EXTENDS;

	/**
	 * @param nonApiPackageNames
	 */
	public LeakExtendsProblemDetector(Set<String> nonApiPackageNames) {
		super(nonApiPackageNames);
	}

	@Override
	public int getReferenceKinds() {
		return IReference.REF_EXTENDS;
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.LEAK_EXTEND;
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		return problemFlags;
	}

	@Override
	public boolean isProblem(IReference reference) {
		boolean isProb = super.isProblem(reference);
		problemFlags = IApiProblem.LEAK_EXTENDS;
		//check if the no extend type is left to be extended
		// or if noimplement interface is extended but not marked noimplement
		if (isProb == false) {
			IApiMember member = reference.getResolvedReference();
			IApiMember sourceMember = reference.getMember();
			try {
				IApiAnnotations annotations = member.getApiComponent().getApiDescription().resolveAnnotations(member.getHandle());
				if (annotations != null) {
					if (RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
						IApiAnnotations annotationsSource = member.getApiComponent().getApiDescription().resolveAnnotations(sourceMember.getHandle());
						if (annotationsSource != null && !RestrictionModifiers.isExtendRestriction(annotationsSource.getRestrictions())) {
							if (!Flags.isFinal(sourceMember.getModifiers())) {
								if (sourceMember instanceof ApiType) {
									if (((ApiType) sourceMember).isClass()) {
										problemFlags = IApiProblem.LEAK_BY_EXTENDING_NO_EXTEND_CLASS_TYPE;
										return true;
									}
									if (((ApiType) sourceMember).isInterface()) {
										problemFlags = IApiProblem.LEAK_BY_EXTENDING_NO_EXTEND_INTERFACE_TYPE;
										return true;
									}
								}
							}
						}
					}
					if (RestrictionModifiers.isImplementRestriction(annotations.getRestrictions())) {
						IApiAnnotations annotationsSource = member.getApiComponent().getApiDescription().resolveAnnotations(sourceMember.getHandle());
						if (annotationsSource != null && !RestrictionModifiers.isImplementRestriction(annotationsSource.getRestrictions())) {
							// problemFlags =
							// IApiProblem.LEAK_BY_EXTENDING_NO_EXTEND_TYPE;
							return true;
						}
					}
				}
			}
			catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
		if (isProb) {
			// check the use restrictions on the API type (can be extended or
			// not)
			IApiType type = (IApiType) reference.getMember();
			IApiComponent component = type.getApiComponent();
			try {
				if (type.isClass()) {
					int modifiers = 0;
					IApiAnnotations annotations = component.getApiDescription().resolveAnnotations(type.getHandle());
					if (annotations != null) {
						// if annotations are null, the reference should not
						// have been retained
						// as it indicates a reference from a top level non
						// public type
						if (RestrictionModifiers.isExtendRestriction(annotations.getRestrictions())) {
							// The no extend restriction means only public
							// members can be seen
							modifiers = Flags.AccPublic;
						} else {
							if (Flags.isFinal(type.getModifiers())) {
								// if final then only public members can be seen
								modifiers = Flags.AccPublic;
							} else {
								// public and protected members can be seen
								modifiers = Flags.AccPublic | Flags.AccProtected;
							}
						}
						IApiType nonApiSuper = type.getSuperclass();
						// collect all visible methods in non-API types
						Set<MethodKey> methods = new HashSet<>();
						while (!isAPIType(nonApiSuper)) {
							if (hasVisibleField(nonApiSuper, modifiers)) {
								// a visible field in a non-API type is a
								// definite leak
								return true;
							}
							gatherVisibleMethods(nonApiSuper, methods, modifiers);
							nonApiSuper = nonApiSuper.getSuperclass();
						}
						if (methods.size() > 0) {
							// check if the visible members are part of an API
							// interface/class
							List<IApiType> apiTypes = new LinkedList<>();
							apiTypes.add(type);
							gatherAPISuperTypes(apiTypes, type);
							for (IApiType t2 : apiTypes) {
								Set<MethodKey> apiMembers = new HashSet<>();
								gatherVisibleMethods(t2, apiMembers, modifiers);
								methods.removeAll(apiMembers);
								if (methods.isEmpty()) {
									// there are no visible methods left that
									// are not part of an API type/interface
									return false;
								}
							}
							if (methods.size() > 0) {
								// there are visible members that are not part
								// of an API type/interface
								return true;
							}
						}
					}
				} else {
					// don't process interfaces, enums, annotations
					return true;
				}
			} catch (CoreException ce) {
				if (ApiPlugin.DEBUG_PROBLEM_DETECTOR) {
					ApiPlugin.log(ce);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds all visible methods to the given set in the specified type.
	 *
	 * @param type type to analyze
	 * @param members set to add methods to
	 * @param modifiers visibilities to consider
	 */
	private void gatherVisibleMethods(IApiType type, Set<MethodKey> members, int modifiers) {
		IApiMethod[] methods = type.getMethods();
		for (IApiMethod method : methods) {
			if ((method.getModifiers() & modifiers) > 0 && !method.isConstructor() && !method.isSynthetic()) {
				members.add(new MethodKey(type.getName(), method.getName(), method.getSignature(), false));
			}
		}
	}

	/**
	 * Returns whether the given type has any visible fields base on the given
	 * visibility flags to consider. A field is visible signals a definite leak.
	 *
	 * @param type type to analyze
	 * @param modifiers visibilities to consider
	 * @return whether there are any visible fields
	 */
	private boolean hasVisibleField(IApiType type, int modifiers) {
		IApiField[] fields = type.getFields();
		for (IApiField field : fields) {
			if ((field.getModifiers() & modifiers) > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds all API super types of the given type to the given list in top down
	 * order.
	 *
	 * @param superTypes list to add to
	 * @param type type being processed
	 */
	private void gatherAPISuperTypes(List<IApiType> superTypes, IApiType type) throws CoreException {
		if (type != null) {
			if (isAPIType(type)) {
				superTypes.add(0, type);
			}
			gatherAPISuperTypes(superTypes, type.getSuperclass());
			IApiType[] interfaces = type.getSuperInterfaces();
			if (interfaces != null) {
				for (IApiType i : interfaces) {
					if (isAPIType(i)) {
						superTypes.add(i);
						gatherAPISuperTypes(superTypes, i);
					}
				}
			}
		}
	}

	/**
	 * Returns whether the given type has API visibility.
	 *
	 * @param type type
	 * @return whether the given type has API visibility
	 */
	private boolean isAPIType(IApiType type) throws CoreException {
		IApiDescription description = type.getApiComponent().getApiDescription();
		IApiAnnotations annotations = description.resolveAnnotations(type.getHandle());
		if (annotations == null) {
			// top level non-public top can have no annotations - they are not
			// API
			return false;
		}
		return VisibilityModifiers.isAPI(annotations.getVisibility());
	}

}
