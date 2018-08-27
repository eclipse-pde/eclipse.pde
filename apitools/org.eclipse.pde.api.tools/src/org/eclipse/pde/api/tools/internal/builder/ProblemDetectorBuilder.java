/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Builds problem detectors for reference analysis.
 *
 * @since 1.1
 */
public class ProblemDetectorBuilder extends ApiDescriptionVisitor {

	/**
	 * Kind mask bits to allow only certain detectors to be built regardless of
	 * the IDE being available
	 */
	/**
	 * All detectors will be added
	 */
	public static int K_ALL = 0xFFFFFFFF;
	/**
	 * Illegal use detectors will be added
	 */
	public static int K_USE = 1;
	/**
	 * Leak detectors will be added
	 */
	public static int K_LEAK = 1 << 1;
	/**
	 * The system detector will be added
	 */
	public static int K_SYSTEM = 1 << 2;

	/**
	 * Problem detectors
	 */
	private IllegalExtendsProblemDetector fIllegalExtends = null;
	private IllegalImplementsProblemDetector fIllegalImplements = null;
	private IllegalInstantiateProblemDetector fIllegalInstantiate = null;
	private IllegalOverrideProblemDetector fIllegalOverride = null;
	private IllegalMethodReferenceDetector fIllegalMethodRef = null;
	private IllegalFieldReferenceDetector fIllegalFieldRef = null;
	private IllegalAnnotationReferenceDetector fIllegalAnnotationRef = null;
	private SystemApiDetector fSystemApiDetector = null;

	/**
	 * Cache of non-API package names visited
	 */
	private Set<String> fNonApiPackageNames = new HashSet<>();

	/**
	 * The owning {@link IApiComponent} of this builder
	 */
	private IApiComponent fComponent = null;

	/**
	 * Problem detectors used for a component's analysis
	 */
	private List<IApiProblemDetector> fDetectors;

	/**
	 * The mask of kinds of detectors to build
	 */
	private int fKindMask = 0;

	/**
	 * Stack containing all of the types tagged &#64;noreference
	 *
	 * @since 1.0.400
	 */
	Set<IElementDescriptor> fRestrictedTypes = new HashSet<>();

	/**
	 * Build problem detectors for a component.
	 *
	 * @param component
	 * @param kinds the integer mask of the kinds of detectors to create
	 */
	public ProblemDetectorBuilder(IApiComponent component, int kinds) {
		fKindMask = kinds;
		initializeDetectors(component);
	}

	@Override
	public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
		int mask = description.getRestrictions();
		switch (element.getElementType()) {
			case IElementDescriptor.PACKAGE: {
				if (VisibilityModifiers.isPrivate(description.getVisibility())) {
					fNonApiPackageNames.add(((IPackageDescriptor) element).getName());
					return false; // no need to visit types in non-API package
				}
				break;
			}
			case IElementDescriptor.TYPE: {
				if ((fKindMask & K_USE) > 0) {
					String symbolicname = fComponent.getSymbolicName();
					IReferenceTypeDescriptor type = (IReferenceTypeDescriptor) element;
					if (RestrictionModifiers.isExtendRestriction(mask) && fIllegalExtends != null) {
						fIllegalExtends.addIllegalType(type, symbolicname);
					}
					if (RestrictionModifiers.isImplementRestriction(mask) && fIllegalImplements != null) {
						fIllegalImplements.addIllegalType(type, symbolicname);
					}
					if (RestrictionModifiers.isInstantiateRestriction(mask) && fIllegalInstantiate != null) {
						fIllegalInstantiate.addIllegalType(type, symbolicname);
					}
					if (RestrictionModifiers.isReferenceRestriction(mask)) {
						fRestrictedTypes.add(element);
						if (fIllegalFieldRef != null) {
							fIllegalFieldRef.addIllegalType(type, symbolicname);
						}
						if (fIllegalMethodRef != null) {
							fIllegalMethodRef.addIllegalType(type, symbolicname);
						}
						if (fIllegalAnnotationRef != null) {
							fIllegalAnnotationRef.addIllegalType(type, symbolicname);
						}
					}
				}
				break;
			}
			case IElementDescriptor.METHOD: {
				if ((fKindMask & K_USE) > 0) {
					String symbolicname = fComponent.getSymbolicName();
					IMethodDescriptor method = (IMethodDescriptor) element;
					if (RestrictionModifiers.isOverrideRestriction(mask) && fIllegalOverride != null) {
						fIllegalOverride.addIllegalMethod(method, symbolicname);
					}
					if (fIllegalMethodRef != null) {
						if (RestrictionModifiers.isReferenceRestriction(mask)) {
							fIllegalMethodRef.addIllegalMethod(method, symbolicname);
						}
					}
				}
				break;
			}
			case IElementDescriptor.FIELD: {
				if ((fKindMask & K_USE) > 0 && fIllegalFieldRef != null) {
					if (RestrictionModifiers.isReferenceRestriction(mask)) {
						fIllegalFieldRef.addIllegalField((IFieldDescriptor) element, fComponent.getSymbolicName());
					}
				}
				break;
			}
			default: {
				break;
			}
		}
		return true;
	}

	/**
	 * Sets the owning component of this builder
	 *
	 * @param component
	 */
	public void setOwningComponent(IApiComponent component) {
		fComponent = component;
	}

	/**
	 * @return the {@link IProject} associated with the set
	 *         {@link IApiComponent} or <code>null</code> if the component is
	 *         not a {@link PluginProjectApiComponent}
	 */
	private IProject getProject(IApiComponent component) {
		if (component instanceof ProjectComponent) {
			ProjectComponent comp = (ProjectComponent) component;
			return comp.getJavaProject().getProject();
		}
		return null;
	}

	/**
	 * Initializes the detectors for this builder. This method is only called
	 * when an owning component is set
	 */
	private void initializeDetectors(IApiComponent component) {
		fDetectors = new ArrayList<>();
		IProject project = getProject(component);
		if ((fKindMask & K_USE) > 0) {
			addUseDetectors(fDetectors, project);
		}
		if ((fKindMask & K_SYSTEM) > 0) {
			addSystemDetector(fDetectors, project);
		}
		if ((fKindMask & K_LEAK) > 0) {
			addLeakDetectors(fDetectors, project);
		}
	}

	/**
	 * Returns whether the given problem kind should be ignored.
	 *
	 * @param problemKey
	 * @return whether the given problem kind should be ignored
	 */
	private boolean isIgnore(String problemKey, IProject project) {
		int severity = ApiPlugin.getDefault().getSeverityLevel(problemKey, project);
		return severity == ApiPlugin.SEVERITY_IGNORE;
	}

	/**
	 * Returns a set of all non-API package names that are in prerequisite
	 * components.
	 *
	 * @return
	 */
	Set<String> getNonApiPackageNames() {
		return fNonApiPackageNames;
	}

	/**
	 * Adds additional non-API package descriptors to the detector builder.
	 *
	 * @param packagee
	 * @return true if the descriptor did not exist in the current collection
	 *         and was added, false otherwise
	 */
	public boolean addNonApiPackageName(String packagee) {
		if (packagee != null) {
			return fNonApiPackageNames.add(packagee);
		}
		return false;
	}

	/**
	 * Returns a list of problem detectors to be used.
	 *
	 * @return problem detectors
	 */
	public List<IApiProblemDetector> getProblemDetectors() {
		return fDetectors;
	}

	/**
	 * Adds the system detector to the given listing
	 *
	 * @param detectors
	 * @param project
	 */
	private void addSystemDetector(List<IApiProblemDetector> detectors, IProject project) {
		if (project != null) {
			// do not add the detector even if the setting is not ignore if
			// there are no EE descriptions installed
			if (!isIgnore(IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, project) && fSystemApiDetector == null && StubApiComponent.getInstalledMetadata().length > 0) {
				fSystemApiDetector = new SystemApiDetector();
				fDetectors.add(fSystemApiDetector);
			}
		} else {
			// add detector by default only outside of the IDE if we have no
			// preference context
			fSystemApiDetector = new SystemApiDetector();
			fDetectors.add(fSystemApiDetector);
		}
	}

	/**
	 * Adds the use detectors to the listing
	 *
	 * @param detectors
	 * @param project
	 */
	private void addUseDetectors(List<IApiProblemDetector> detectors, IProject project) {
		if (project != null) {
			if (!isIgnore(IApiProblemTypes.ILLEGAL_EXTEND, project) && fIllegalExtends == null) {
				fIllegalExtends = new IllegalExtendsProblemDetector();
				detectors.add(fIllegalExtends);
			}
			if (!isIgnore(IApiProblemTypes.ILLEGAL_IMPLEMENT, project) && fIllegalImplements == null) {
				fIllegalImplements = new IllegalImplementsProblemDetector();
				detectors.add(fIllegalImplements);
			}
			if (!isIgnore(IApiProblemTypes.ILLEGAL_INSTANTIATE, project) && fIllegalInstantiate == null) {
				fIllegalInstantiate = new IllegalInstantiateProblemDetector();
				detectors.add(fIllegalInstantiate);
			}
			if (!isIgnore(IApiProblemTypes.ILLEGAL_OVERRIDE, project) && fIllegalOverride == null) {
				fIllegalOverride = new IllegalOverrideProblemDetector();
				detectors.add(fIllegalOverride);
			}
			if (!isIgnore(IApiProblemTypes.ILLEGAL_REFERENCE, project) && fIllegalMethodRef == null) {
				fIllegalMethodRef = new IllegalMethodReferenceDetector();
				detectors.add(fIllegalMethodRef);
				fIllegalFieldRef = new IllegalFieldReferenceDetector();
				detectors.add(fIllegalFieldRef);
				fIllegalAnnotationRef = new IllegalAnnotationReferenceDetector();
				detectors.add(fIllegalAnnotationRef);
			}
		} else {
			// add all detectors by default if we have no preference context
			fIllegalExtends = new IllegalExtendsProblemDetector();
			detectors.add(fIllegalExtends);
			fIllegalImplements = new IllegalImplementsProblemDetector();
			detectors.add(fIllegalImplements);
			fIllegalInstantiate = new IllegalInstantiateProblemDetector();
			detectors.add(fIllegalInstantiate);
			fIllegalOverride = new IllegalOverrideProblemDetector();
			detectors.add(fIllegalOverride);
			fIllegalMethodRef = new IllegalMethodReferenceDetector();
			detectors.add(fIllegalMethodRef);
			fIllegalFieldRef = new IllegalFieldReferenceDetector();
			detectors.add(fIllegalFieldRef);
			fIllegalAnnotationRef = new IllegalAnnotationReferenceDetector();
			detectors.add(fIllegalAnnotationRef);
		}
	}

	/**
	 * Adds any leak detectors to the listing. If a project context is available
	 * we filter out disabled detectors based on project / workspace preference
	 * settings
	 *
	 * @param detectors
	 * @param project
	 */
	private void addLeakDetectors(List<IApiProblemDetector> detectors, IProject project) {
		if (project != null) {
			if (!isIgnore(IApiProblemTypes.LEAK_EXTEND, project)) {
				detectors.add(new LeakExtendsProblemDetector(fNonApiPackageNames));
			}
			if (!isIgnore(IApiProblemTypes.LEAK_IMPLEMENT, project)) {
				detectors.add(new LeakImplementsProblemDetector(fNonApiPackageNames));
			}
			if (!isIgnore(IApiProblemTypes.LEAK_FIELD_DECL, project)) {
				detectors.add(new LeakFieldProblemDetector(fNonApiPackageNames));
			}
			if (!isIgnore(IApiProblemTypes.LEAK_METHOD_PARAM, project)) {
				detectors.add(new LeakParameterTypeDetector(fNonApiPackageNames));
			}
			if (!isIgnore(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE, project)) {
				detectors.add(new LeakReturnTypeDetector(fNonApiPackageNames));
			}
		} else {
			// add all leak detectors by default if we have no preference
			// context
			detectors.add(new LeakExtendsProblemDetector(fNonApiPackageNames));
			detectors.add(new LeakImplementsProblemDetector(fNonApiPackageNames));
			detectors.add(new LeakFieldProblemDetector(fNonApiPackageNames));
			detectors.add(new LeakReturnTypeDetector(fNonApiPackageNames));
			detectors.add(new LeakParameterTypeDetector(fNonApiPackageNames));
		}
	}
}
