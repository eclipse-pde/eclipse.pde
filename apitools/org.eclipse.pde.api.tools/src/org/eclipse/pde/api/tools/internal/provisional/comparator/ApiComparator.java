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
package org.eclipse.pde.api.tools.internal.provisional.comparator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.comparator.ClassFileComparator;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.comparator.TypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * This class defines a comparator to get a IDelta out of the comparison of two elements.
 *
 * @since 1.0
 */
public class ApiComparator {
	public static final IDelta NO_DELTA = new Delta();

	/**
	 * Constant used for controlling tracing in the API comparator
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the API comparator
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}

	/**
	 * Returns a delta that corresponds to the comparison of the given class file with the reference. 
	 * 
	 * @param classFile2 the given class file that comes from the <code>component2</code>
	 * @param component the given API component from the reference
	 * @param component2 the given API component to compare with
	 * @param referenceProfile the given API profile from which the given component <code>component</code> is coming from
	 * @param profile the given API profile from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>the given class file is null</li>
	 * <li>one of the given components is null</li>
	 * <li>one of the given profiles is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IClassFile classFile2,
			final IApiComponent component,
			final IApiComponent component2,
			final IApiProfile referenceProfile,
			final IApiProfile profile,
			final int visibilityModifiers) {
		
		if (classFile2 == null) {
			throw new IllegalArgumentException("The given class file is null"); //$NON-NLS-1$
		}
		if (component == null || component2 == null) {
			throw new IllegalArgumentException("One of the given components is null"); //$NON-NLS-1$
		}
		if (referenceProfile == null || profile == null) {
			throw new IllegalArgumentException("One of the given profiles is null"); //$NON-NLS-1$
		}

		try {
			TypeDescriptor typeDescriptor = new TypeDescriptor(classFile2);
			if (typeDescriptor.isNestedType()) {
				// we skip nested types (member, local and anonymous)
				return NO_DELTA;
			}
			String typeName = classFile2.getTypeName();
			IClassFile classFile = component.findClassFile(typeName);
			if (classFile == null) {
				return new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.ADDED, IDelta.TYPE, classFile2, typeName, null);
			}
			final IApiDescription apiDescription = component2.getApiDescription();
			IApiAnnotations elementDescription = apiDescription.resolveAnnotations(null, Factory.typeDescriptor(typeName));
			if (elementDescription != null) {
				int visibility = elementDescription.getVisibility();
				if ((visibility & visibilityModifiers) == 0) {
					// check visibility in the reference
					final IApiDescription referenceApiDescription = component.getApiDescription();
					elementDescription = referenceApiDescription.resolveAnnotations(null, Factory.typeDescriptor(typeName));
					if (elementDescription != null && (visibility & visibilityModifiers) == 0) {
						// no delta
						return NO_DELTA;
					}
					// visibility has been changed
					if (((visibility & VisibilityModifiers.API) != 0)
							&& ((visibilityModifiers & VisibilityModifiers.API) != 0)) {
						// was API and is no longer API
						return new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.TYPE, classFile2, typeName, typeName);
					}
					// no delta
					return new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_VISIBILITY, classFile2, typeName, null);
				}
			}
			ClassFileComparator comparator = new ClassFileComparator(classFile, classFile2, component, component2, referenceProfile, profile, visibilityModifiers);
			return comparator.getDelta();
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Returns a delta that corresponds to the comparison of the given class file. 
	 * 
	 * @param classFile the given class file
	 * @param classFile2 the given class file to compare with
	 * @param component the given API component from which the given class file is coming from
	 * @param component2 the given API component to compare with
	 * @param referenceProfile the given API profile from which the given component <code>component</code> is coming from
	 * @param profile the given API profile from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>one of the given components is null</li>
	 * <li>one of the given profiles is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IClassFile classFile,
			final IClassFile classFile2,
			final IApiComponent component,
			final IApiComponent component2,
			final IApiProfile referenceProfile,
			final IApiProfile profile,
			final int visibilityModifiers) {
		
		if (classFile == null || classFile2 == null) {
			throw new IllegalArgumentException("One of the given class files is null"); //$NON-NLS-1$
		}
		if (component == null || component2 == null) {
			throw new IllegalArgumentException("One of the given components is null"); //$NON-NLS-1$
		}
		if (referenceProfile == null || profile == null) {
			throw new IllegalArgumentException("One of the given profiles is null"); //$NON-NLS-1$
		}
		ClassFileComparator comparator =
			new ClassFileComparator(
					classFile,
					classFile2,
					component,
					component2,
					referenceProfile,
					profile,
					visibilityModifiers);
		return comparator.getDelta();
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API profiles. 
	 * 
	 * @param referenceProfile the given API profile which is the reference
	 * @param profile the given API profile to compare with
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two profiles is null
	 */
	public static IDelta compare(
			final IApiProfile referenceProfile,
			final IApiProfile profile,
			final boolean force) {
		return compare(referenceProfile, profile, VisibilityModifiers.ALL_VISIBILITIES, force);
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API profiles.
	 * Nested API components with the same versions are not compared.
	 * <p>Equivalent to: compare(profile, profile2, false);</p>
	 * 
	 * @param referenceProfile the given API profile which is the reference
	 * @param profile the given API profile to compare with
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two profiles is null
	 */
	public static IDelta compare(
			final IApiProfile referenceProfile,
			final IApiProfile profile) {
		return compare(referenceProfile, profile, VisibilityModifiers.ALL_VISIBILITIES, false);
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API profiles. 
	 * Nested API components with the same versions are not compared.
	 * <p>Equivalent to: compare(profile, profile2, visibilityModifiers, false);</p>
	 * 
	 * @param referenceProfile the given API profile which is the reference
	 * @param profile the given API profile to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two profiles is null
	 */
	public static IDelta compare(
			final IApiProfile referenceProfile,
			final IApiProfile profile,
			final int visibilityModifiers) {
		return compare(referenceProfile, profile, visibilityModifiers, false);
	}

	/**
	 * Returns a delta that corresponds to the difference between the given profile and the reference.
	 * 
	 * @param referenceProfile the given API profile which is used as the reference
	 * @param profile the given API profile to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two profiles is null
	 */
	public static IDelta compare(
			final IApiProfile referenceProfile,
			final IApiProfile profile,
			final int visibilityModifiers,
			final boolean force) {
		if (referenceProfile == null || profile == null) {
			throw new IllegalArgumentException("None of the profiles must be null"); //$NON-NLS-1$
		}
		IApiComponent[] apiComponents = referenceProfile.getApiComponents();
		IApiComponent[] apiComponents2 = profile.getApiComponents();
		Set apiComponentsIds = new HashSet();
		final Delta globalDelta = new Delta();
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			IApiComponent apiComponent = apiComponents[i];
			if (!apiComponent.isSystemComponent()) {
				String id = apiComponent.getId();
				IApiComponent apiComponent2 = profile.getApiComponent(id);
				IDelta delta = null;
				if (apiComponent2 == null) {
					// report removal of an API component
					delta = new Delta(IDelta.API_PROFILE_ELEMENT_TYPE, IDelta.REMOVED, IDelta.API_COMPONENT, null, id, id);
				} else {
					apiComponentsIds.add(id);
					if (!apiComponent.getVersion().equals(apiComponent2.getVersion())
							|| force) {
						long time = System.currentTimeMillis();
						try {
							delta = compare(apiComponent, apiComponent2, referenceProfile, profile, visibilityModifiers);
						} finally {
							if (DEBUG) {
								System.out.println("Time spent for " + id+ " " + apiComponent.getVersion() + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							}
						}
					}
				}
				if (delta != null && delta != NO_DELTA) {
					globalDelta.add(delta);
				}
			}
		}
		for (int i = 0, max = apiComponents2.length; i < max; i++) {
			IApiComponent apiComponent = apiComponents2[i];
			if (!apiComponent.isSystemComponent()) {
				String id = apiComponent.getId();
				if (!apiComponentsIds.contains(id)) {
					// addition of an API component
					globalDelta.add(new Delta(IDelta.API_PROFILE_ELEMENT_TYPE, IDelta.ADDED, IDelta.API_COMPONENT, null, id, id));
				}
			}
		}
		return globalDelta.isEmpty() ? NO_DELTA : globalDelta;
	}

	/**
	 * Returns a delta that corresponds to the difference between the given component and the reference profile.
	 * 
	 * @param component the given component to compare with the given reference profile
	 * @param referenceProfile the given API profile which is used as the reference
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>the given component is null</li>
	 * <li>the reference profile is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent component,
			final IApiProfile referenceProfile,
			final int visibilityModifiers,
			final boolean force) {
		
		if (component == null) {
			throw new IllegalArgumentException("The composent cannot be null"); //$NON-NLS-1$
		}
		if (referenceProfile == null) {
			throw new IllegalArgumentException("The reference profile cannot be null"); //$NON-NLS-1$
		}
		IDelta delta = null;
		if (!component.isSystemComponent()) {
			String id = component.getId();
			IApiComponent apiComponent2 = referenceProfile.getApiComponent(id);
			if (apiComponent2 == null) {
				// report addition of an API component
				delta = new Delta(IDelta.API_PROFILE_ELEMENT_TYPE, IDelta.ADDED, IDelta.API_COMPONENT, null, id, id);
			} else {
				if (!component.getVersion().equals(apiComponent2.getVersion())
						|| force) {
					long time = System.currentTimeMillis();
					try {
						delta = compare(apiComponent2, component, visibilityModifiers);
					} finally {
						if (DEBUG) {
							System.out.println("Time spent for " + id+ " " + component.getVersion() + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						}
					}
				}
			}
			if (delta != null && delta != NO_DELTA) {
				return delta;
			}
		}
		return NO_DELTA;
	}

	/**
	 * Returns a delta that corresponds to the difference between the given component and the given reference component.
	 * The given component cannot be null.
	 * 
	 * @param referenceComponent the given API component that is used as the reference
	 * @param component the given component to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>one of the given components is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent referenceComponent,
			final IApiComponent component,
			final int visibilityModifiers) {

		if (referenceComponent == null || component == null) {
			throw new IllegalArgumentException("One of the given components is null"); //$NON-NLS-1$
		}
		return compare(referenceComponent, component, referenceComponent.getProfile(), component.getProfile(), visibilityModifiers);
	}
	/**
	 * Returns a delta that corresponds to the comparison of the two given API components.
	 * The two components are compared even if their versions are identical.
	 * 
	 * @param referenceComponent the given API component from which the given class file is coming from
	 * @param component2 the given API component to compare with
	 * @param referenceProfile the given API profile from which the given component <code>component</code> is coming from
	 * @param profile the given API profile from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>both given components are null</li>
	 * <li>one of the profiles is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent referenceComponent,
			final IApiComponent component2,
			final IApiProfile referenceProfile,
			final IApiProfile profile) {
		return compare(referenceComponent, component2, referenceProfile, profile, VisibilityModifiers.ALL_VISIBILITIES);
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API components.
	 * The two components are compared even if their versions are identical.
	 * 
	 * @param referenceComponent the given API component
	 * @param component2 the given API component to compare with
	 * @param referenceProfile the given API profile from which the given component <code>component</code> is coming from
	 * @param profile the given API profile from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>both given components are null</li>
	 * <li>one of the profiles is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent referenceComponent,
			final IApiComponent component2,
			final IApiProfile referenceProfile,
			final IApiProfile profile,
			final int visibilityModifiers) {
	
		if (referenceProfile == null || profile == null) {
			throw new IllegalArgumentException("The profiles cannot be null"); //$NON-NLS-1$
		}
		if (referenceComponent == null) {
			if (component2 == null) {
				throw new IllegalArgumentException("Both components cannot be null"); //$NON-NLS-1$
			}
			return new Delta(IDelta.API_PROFILE_ELEMENT_TYPE, IDelta.ADDED, IDelta.API_COMPONENT, null, component2.getId(), component2.getId());
		} else if (component2 == null) {
			return new Delta(IDelta.API_PROFILE_ELEMENT_TYPE, IDelta.REMOVED, IDelta.API_COMPONENT, null, referenceComponent.getId(), referenceComponent.getId());
		}
		final Delta globalDelta = new Delta();

		// check the EE first
		Set referenceEEs = Util.convertAsSet(referenceComponent.getExecutionEnvironments());
		Set componentsEEs = Util.convertAsSet(component2.getExecutionEnvironments());
		
		for (Iterator iterator = referenceEEs.iterator(); iterator.hasNext(); ) {
			String currentEE = (String) iterator.next();
			if (!componentsEEs.remove(currentEE)) {
				globalDelta.add(new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.EXECUTION_ENVIRONMENT, null, referenceComponent.getId(), currentEE));
			}
		}
		for (Iterator iterator = componentsEEs.iterator(); iterator.hasNext(); ) {
			String currentEE = (String) iterator.next();
			globalDelta.add(new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.ADDED, IDelta.EXECUTION_ENVIRONMENT, null, referenceComponent.getId(), currentEE));
		}
		try {
			return internalCompare(referenceComponent, component2, referenceProfile, profile, visibilityModifiers, globalDelta);
		} catch(CoreException e) {
			// null means an error case
			return null;
		}
	}

	private static IDelta internalCompare(
			final IApiComponent component,
			final IApiComponent component2,
			final IApiProfile referenceProfile,
			final IApiProfile profile,
			final int visibilityModifiers,
			final Delta globalDelta) throws CoreException {

		final Set classFileBaseLineNames = new HashSet();
		final String id = component.getId();
		IClassFileContainer[] classFileContainers = component.getClassFileContainers(id);
		final IApiDescription apiDescription = component.getApiDescription();
		if (classFileContainers != null) {
			for (int i = 0, max = classFileContainers.length; i < max; i++) {
				IClassFileContainer container = classFileContainers[i];
				try {
					container.accept(new ClassFileContainerVisitor() {
						public void visit(String packageName, IClassFile classFile) {
							String typeName = classFile.getTypeName();
							IApiAnnotations elementDescription = apiDescription.resolveAnnotations(null, Factory.typeDescriptor(typeName));
							try {
								TypeDescriptor typeDescriptor = new TypeDescriptor(classFile);
								if (filterType(visibilityModifiers, elementDescription, typeDescriptor)) {
									return;
								}
								classFileBaseLineNames.add(typeName);
								IClassFile classFile2 = component2.findClassFile(typeName, id);
								if (classFile2 == null) {
									globalDelta.add(new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.TYPE, classFile, typeName, typeName));
								} else {
									ClassFileComparator comparator = new ClassFileComparator(typeDescriptor, classFile2, component, component2, referenceProfile, profile, visibilityModifiers);
									IDelta delta = comparator.getDelta();
									if (delta != null && delta != NO_DELTA) {
										globalDelta.add(delta);
									}
								}
							} catch (CoreException e) {
								ApiPlugin.log(e);
							}
						}
					});
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		IClassFileContainer[] classFileContainers2 = component2.getClassFileContainers(id);
		final IApiDescription apiDescription2 = component2.getApiDescription();
		if (classFileContainers2 != null) {
			for (int i = 0, max = classFileContainers2.length; i < max; i++) {
				IClassFileContainer container = classFileContainers2[i];
				try {
					container.accept(new ClassFileContainerVisitor() {
						public void visit(String packageName, IClassFile classFile) {
							String typeName = classFile.getTypeName();
							IApiAnnotations elementDescription = apiDescription2.resolveAnnotations(null, Factory.typeDescriptor(typeName));
							try {
								TypeDescriptor typeDescriptor = new TypeDescriptor(classFile);
								if (filterType(visibilityModifiers, elementDescription, typeDescriptor)) {
									return;
								}
								if (classFileBaseLineNames.contains(typeName)) {
									// already processed
									return;
								}
								globalDelta.add(new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.ADDED, IDelta.TYPE, classFile, typeName, null));
							} catch (CoreException e) {
								ApiPlugin.log(e);
							}
						}
					});
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return globalDelta.isEmpty() ? NO_DELTA : globalDelta;
	}
	
	/* (no javadoc)
	 * Returns true, if the given type descriptor should be skipped, false otherwise.
	 */
	static boolean filterType(final int visibilityModifiers,
			IApiAnnotations elementDescription,
			TypeDescriptor typeDescriptor) {
		if (elementDescription != null && (elementDescription.getVisibility() & visibilityModifiers) == 0) {
			// we skip the class file according to their visibility
			return true;
		}
		if (visibilityModifiers == VisibilityModifiers.API) {
			// if the visibility is API, we only consider public and protected types
			if (Util.isDefault(typeDescriptor.access)
						|| Util.isPrivate(typeDescriptor.access)) {
				return true;
			}
		}
		// we skip nested types (member, local and anonymous)
		return typeDescriptor.isNestedType();
	}
}
