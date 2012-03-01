/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.comparator.ClassFileComparator;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.osgi.framework.Version;

/**
 * This class defines a comparator to get a IDelta out of the comparison of two elements.
 *
 * @since 1.0
 */
public class ApiComparator {
	/**
	 * Default empty delta
	 */
	public static final IDelta NO_DELTA = new Delta();
	
	/**
	 * Returns a delta for a API component version change
	 * 
	 * @param apiComponent2
	 * @param id
	 * @param apiComponentVersion
	 * @param apiComponentVersion2
	 */
	static IDelta checkBundleVersionChanges(IApiComponent apiComponent2, String id, String apiComponentVersion, String apiComponentVersion2) {
		Version version = null;
		try {
			version = new Version(apiComponentVersion);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		Version version2 = null;
		try {
			version2 = new Version(apiComponentVersion2);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		if (version != null && version2 != null) {
			// add check for bundle versions
			if (version.getMajor() != version2.getMajor()) {
				return
					new Delta(
						Util.getDeltaComponentVersionsId(apiComponent2),
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.MAJOR_VERSION,
						RestrictionModifiers.NO_RESTRICTIONS,
						RestrictionModifiers.NO_RESTRICTIONS,
						0,
						0,
						null,
						id,
						new String[] {
							id,
							apiComponentVersion,
							apiComponentVersion2
						});
			} else if (version.getMinor() != version2.getMinor()) {
				return
					new Delta(
						Util.getDeltaComponentVersionsId(apiComponent2),
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.MINOR_VERSION,
						RestrictionModifiers.NO_RESTRICTIONS,
						RestrictionModifiers.NO_RESTRICTIONS,
						0,
						0,
						null,
						id,
						new String[] {
							id,
							apiComponentVersion,
							apiComponentVersion2
						});
			}
		}
		return null;
	}

	/**
	 * Returns a delta that corresponds to the difference between the given baseline and the reference.
	 * 
	 * @param referenceBaseline the given API baseline which is used as the reference
	 * @param baseline the given API baseline to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 * @param monitor
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two baselines is null
	 */
	public static IDelta compare(
			final IApiBaseline referenceBaseline,
			final IApiBaseline baseline,
			final int visibilityModifiers,
			final boolean force, 
			final IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, 2);
		try {
			if (referenceBaseline == null || baseline == null) {
				throw new IllegalArgumentException("None of the baselines must be null"); //$NON-NLS-1$
			}
			IApiComponent[] apiComponents = referenceBaseline.getApiComponents();
			IApiComponent[] apiComponents2 = baseline.getApiComponents();
			Set apiComponentsIds = new HashSet();
			final Delta globalDelta = new Delta();
			for (int i = 0, max = apiComponents.length; i < max; i++) {
				Util.updateMonitor(localmonitor);
				IApiComponent apiComponent = apiComponents[i];
				if (!apiComponent.isSystemComponent()) {
					String id = apiComponent.getSymbolicName();
					IApiComponent apiComponent2 = baseline.getApiComponent(id);
					IDelta delta = null;
					if (apiComponent2 == null) {
						// report removal of an API component
						delta =
							new Delta(
									null,
									IDelta.API_BASELINE_ELEMENT_TYPE,
									IDelta.REMOVED,
									IDelta.API_COMPONENT,
									null,
									id,
									id);
					} else {
						apiComponentsIds.add(id);
						String versionString = apiComponent.getVersion();
						String versionString2 = apiComponent2.getVersion();
						IDelta bundleVersionChangesDelta = checkBundleVersionChanges(apiComponent2, id, versionString, versionString2);
						if (bundleVersionChangesDelta != null) {
							globalDelta.add(bundleVersionChangesDelta);
						}
						if (!versionString.equals(versionString2)
								|| force) {
							long time = System.currentTimeMillis();
							try {
								delta = compare(apiComponent, apiComponent2, referenceBaseline, baseline, visibilityModifiers, localmonitor.newChild(1));
							} finally {
								if (ApiPlugin.DEBUG_API_COMPARATOR) {
									System.out.println("Time spent for " + id+ " " + versionString + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
								}
							}
						}
					}
					if (delta != null && delta != NO_DELTA) {
						globalDelta.add(delta);
					}
				}
			}
			Util.updateMonitor(localmonitor, 1);
			for (int i = 0, max = apiComponents2.length; i < max; i++) {
				Util.updateMonitor(localmonitor);
				IApiComponent apiComponent = apiComponents2[i];
				if (!apiComponent.isSystemComponent()) {
					String id = apiComponent.getSymbolicName();
					if (!apiComponentsIds.contains(id)) {
						// addition of an API component
						globalDelta.add(
								new Delta(
										null,
										IDelta.API_BASELINE_ELEMENT_TYPE,
										IDelta.ADDED,
										IDelta.API_COMPONENT,
										null,
										id,
										id));
					}
				}
			}
			return globalDelta.isEmpty() ? NO_DELTA : globalDelta;
		}
		finally {
			localmonitor.done();
		}
	}

	/**
	 * Returns a delta that corresponds to the difference between the given component and the reference baseline.
	 * 
	 * @param component the given component to compare with the given reference baseline
	 * @param referenceBaseline the given API baseline which is used as the reference
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 * @param monitor
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>the given component is null</li>
	 * <li>the reference baseline is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent component,
			final IApiBaseline referenceBaseline,
			final int visibilityModifiers,
			final boolean force, 
			final IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, 2);
		try {
			if (component == null) {
				throw new IllegalArgumentException("The composent cannot be null"); //$NON-NLS-1$
			}
			if (referenceBaseline == null) {
				throw new IllegalArgumentException("The reference baseline cannot be null"); //$NON-NLS-1$
			}
			Util.updateMonitor(localmonitor, 1);
			IDelta delta = null;
			if (!component.isSystemComponent()) {
				String id = component.getSymbolicName();
				IApiComponent apiComponent2 = referenceBaseline.getApiComponent(id);
				if (apiComponent2 == null) {
					// report addition of an API component
					delta =
						new Delta(
							null,
							IDelta.API_BASELINE_ELEMENT_TYPE,
							IDelta.ADDED,
							IDelta.API_COMPONENT,
							null,
							id,
							id);
				} else {
					if (!component.getVersion().equals(apiComponent2.getVersion())
							|| force) {
						long time = System.currentTimeMillis();
						try {
							delta = compare(apiComponent2, component, visibilityModifiers, localmonitor.newChild(1));
						} finally {
							if (ApiPlugin.DEBUG_API_COMPARATOR) {
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
		finally {
			localmonitor.done();
		}
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API components.
	 * The two components are compared even if their versions are identical.
	 * 
	 * @param referenceComponent the given API component
	 * @param component2 the given API component to compare with
	 * @param referenceBaseline the given API baseline from which the given component <code>component</code> is coming from
	 * @param baseline the given API baseline from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param monitor
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>both given components are null</li>
	 * <li>one of the baselines is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent referenceComponent,
			final IApiComponent component2,
			final IApiBaseline referenceBaseline,
			final IApiBaseline baseline,
			final int visibilityModifiers, 
			final IProgressMonitor monitor) {
		SubMonitor localmonitor = SubMonitor.convert(monitor, 3);
		try {
			if (referenceComponent == null) {
				if (component2 == null) {
					throw new IllegalArgumentException("Both components cannot be null"); //$NON-NLS-1$
				}
				return new Delta(
						null,
						IDelta.API_BASELINE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.API_COMPONENT,
						null,
						component2.getSymbolicName(),
						Util.getComponentVersionsId(component2));
			} else if (component2 == null) {
				String referenceComponentId = referenceComponent.getSymbolicName();
				return new Delta(
						null,
						IDelta.API_BASELINE_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.API_COMPONENT,
						null,
						referenceComponentId,
						Util.getComponentVersionsId(referenceComponent));
			}
			Util.updateMonitor(localmonitor, 1);
			if (referenceBaseline == null || baseline == null) {
				throw new IllegalArgumentException("The baselines cannot be null"); //$NON-NLS-1$
			}
			String referenceComponentId = referenceComponent.getSymbolicName();
			final Delta globalDelta = new Delta();
	
			// check the EE first
			Set referenceEEs = Util.convertAsSet(referenceComponent.getExecutionEnvironments());
			Set componentsEEs = Util.convertAsSet(component2.getExecutionEnvironments());
			Util.updateMonitor(localmonitor, 1);
			for (Iterator iterator = referenceEEs.iterator(); iterator.hasNext(); ) {
				Util.updateMonitor(localmonitor);
				String currentEE = (String) iterator.next();
				if (!componentsEEs.remove(currentEE)) {
					globalDelta.add(
							new Delta(
									Util.getDeltaComponentVersionsId(referenceComponent),
									IDelta.API_COMPONENT_ELEMENT_TYPE,
									IDelta.REMOVED,
									IDelta.EXECUTION_ENVIRONMENT,
									RestrictionModifiers.NO_RESTRICTIONS,
									RestrictionModifiers.NO_RESTRICTIONS,
									0,
									0,
									null,
									referenceComponentId,
									new String[] { currentEE, Util.getComponentVersionsId(referenceComponent)}));
				}
			}
			for (Iterator iterator = componentsEEs.iterator(); iterator.hasNext(); ) {
				Util.updateMonitor(localmonitor);
				String currentEE = (String) iterator.next();
				globalDelta.add(
						new Delta(
								Util.getDeltaComponentVersionsId(referenceComponent),
								IDelta.API_COMPONENT_ELEMENT_TYPE,
								IDelta.ADDED,
								IDelta.EXECUTION_ENVIRONMENT,
								RestrictionModifiers.NO_RESTRICTIONS,
								RestrictionModifiers.NO_RESTRICTIONS,
								0,
								0,
								null,
								referenceComponentId,
								new String[] { currentEE, Util.getComponentVersionsId(referenceComponent)}));
			}
			return internalCompare(referenceComponent, component2, referenceBaseline, baseline, visibilityModifiers, globalDelta, localmonitor.newChild(1));
		} catch(CoreException e) {
			// null means an error case
			return null;
		}
		finally {
			localmonitor.done();
		}
	}

	/**
	 * Returns a delta that corresponds to the difference between the given component and the given reference component.
	 * The given component cannot be null.
	 * 
	 * @param referenceComponent the given API component that is used as the reference
	 * @param component the given component to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions
	 * @param monitor 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 */
	public static IDelta compare(
			final IApiComponent referenceComponent,
			final IApiComponent component,
			final int visibilityModifiers, 
			final IProgressMonitor monitor) {
		try {
			return compare(referenceComponent, component, referenceComponent == null ? null : referenceComponent.getBaseline(), component.getBaseline(), visibilityModifiers, monitor);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return null;
	}

	/**
	 * Returns a delta that corresponds to the comparison of the given class file with the reference. 
	 * 
	 * @param typeRoot2 the given class file that comes from the <code>component2</code>
	 * @param component the given API component from the reference
	 * @param component2 the given API component to compare with
	 * @param reexporter the API component re-exporting component2, or <code>null</code> if none
	 * @param referenceBaseline the given API baseline from which the given component <code>component</code> is coming from
	 * @param baseline the given API baseline from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param monitor
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>the given class file is null</li>
	 * <li>one of the given components is null</li>
	 * <li>one of the given baselines is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiTypeRoot typeRoot2,
			final IApiComponent component,
			final IApiComponent component2,
			final IApiComponent reexporter,
			final IApiBaseline referenceBaseline,
			final IApiBaseline baseline, 
			final int visibilityModifiers, final IProgressMonitor monitor) {
		
		if (typeRoot2 == null) {
			throw new IllegalArgumentException("The given class file is null"); //$NON-NLS-1$
		}
		if (component == null || component2 == null) {
			throw new IllegalArgumentException("One of the given components is null"); //$NON-NLS-1$
		}
		if (referenceBaseline == null || baseline == null) {
			throw new IllegalArgumentException("One of the given baselines is null"); //$NON-NLS-1$
		}
		SubMonitor localmonitor = SubMonitor.convert(monitor, 6);
		try {
			IApiType typeDescriptor2 = typeRoot2.getStructure();
			if (typeDescriptor2.isMemberType() || typeDescriptor2.isAnonymous() || typeDescriptor2.isLocal()) {
				// we skip nested types (member, local and anonymous)
				return NO_DELTA;
			}
			String typeName = typeRoot2.getTypeName();
			IApiTypeRoot typeRoot = null;
			String id = component.getSymbolicName();
			if (Util.ORG_ECLIPSE_SWT.equals(id)) {
				typeRoot = component.findTypeRoot(typeName);
			} else {
				typeRoot = component.findTypeRoot(typeName, id);
			}
			final IApiDescription apiDescription2 = component2.getApiDescription();
			IApiAnnotations elementDescription2 = apiDescription2.resolveAnnotations(typeDescriptor2.getHandle());
			int visibility = 0;
			if (elementDescription2 != null) {
				visibility = elementDescription2.getVisibility();
			}
			Util.updateMonitor(localmonitor, 1);
			final IApiDescription referenceApiDescription = component.getApiDescription();
			IApiAnnotations refElementDescription = referenceApiDescription.resolveAnnotations(typeDescriptor2.getHandle());
			int refVisibility = 0;
			if (refElementDescription != null) {
				refVisibility = refElementDescription.getVisibility();
			}
			Util.updateMonitor(localmonitor, 1);
			String deltaComponentID = Util.getDeltaComponentVersionsId(component2);
			if (typeRoot == null) {
				if (Util.isAPI(visibility, typeDescriptor2)) {
					return new Delta(
							deltaComponentID,
							IDelta.API_COMPONENT_ELEMENT_TYPE,
							IDelta.ADDED,
							reexporter == null ? IDelta.TYPE : IDelta.REEXPORTED_TYPE,
							elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
							RestrictionModifiers.NO_RESTRICTIONS,
							0,
							typeDescriptor2.getModifiers(),
							typeName,
							typeName,
							new String[] { typeName, Util.getComponentVersionsId(component2)});
				}
				return NO_DELTA;
			}
			Util.updateMonitor(localmonitor, 1);
			IApiType typeDescriptor = typeRoot.getStructure();
			if ((visibility & visibilityModifiers) == 0) {
				if ((refVisibility & visibilityModifiers) == 0) {
					// no delta
					return NO_DELTA;
				}
				if (Util.isAPI(refVisibility, typeDescriptor)) {
					return new Delta(
							deltaComponentID,
							IDelta.API_COMPONENT_ELEMENT_TYPE,
							IDelta.REMOVED,
							IDelta.API_TYPE,
							elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
							RestrictionModifiers.NO_RESTRICTIONS,
							typeDescriptor.getModifiers(),
							typeDescriptor2.getModifiers(),
							typeName,
							typeName,
							new String[] { typeName, Util.getComponentVersionsId(component2)});
				}
			} else if (!Util.isAPI(refVisibility, typeDescriptor)
					&& Util.isAPI(visibility, typeDescriptor2)) {
				return new Delta(
						deltaComponentID,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.TYPE,
						elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
						RestrictionModifiers.NO_RESTRICTIONS,
						typeDescriptor.getModifiers(),
						typeDescriptor2.getModifiers(),
						typeName,
						typeName,
						new String[] { typeName, Util.getComponentVersionsId(component2)});
			}
			Util.updateMonitor(localmonitor, 1);
			if (visibilityModifiers == VisibilityModifiers.API) {
				// if the visibility is API, we only consider public and protected types
				if (Util.isDefault(typeDescriptor2.getModifiers())
							|| Flags.isPrivate(typeDescriptor2.getModifiers())) {
					// we need to check if the reference contains the type to report a reduced visibility
					if (Flags.isPublic(typeDescriptor.getModifiers())
							|| Flags.isProtected(typeDescriptor.getModifiers())) {
						return new Delta(
							deltaComponentID,
							IDelta.API_COMPONENT_ELEMENT_TYPE,
							IDelta.REMOVED,
							IDelta.API_TYPE,
							elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
							RestrictionModifiers.NO_RESTRICTIONS,
							typeDescriptor.getModifiers(),
							typeDescriptor2.getModifiers(),
							typeName,
							typeName,
							new String[] { typeName, Util.getComponentVersionsId(component2)});
					} else {
						return NO_DELTA;
					}
				}
			}
			Util.updateMonitor(localmonitor, 1);
			ClassFileComparator comparator = new ClassFileComparator(typeDescriptor, typeRoot2, component, component2, referenceBaseline, baseline, visibilityModifiers);
			IDelta delta = comparator.getDelta(localmonitor.newChild(1));
			if (ApiPlugin.DEBUG_API_COMPARATOR) {
				IStatus status = comparator.getStatus();
				if(status != null) {
					ApiPlugin.log(status);
				}
			}
			return delta;
		} catch (CoreException e) {
			return null;
		}
		finally {
			localmonitor.done();
		}
	}

	/**
	 * Returns a delta that corresponds to the comparison of the given class file. 
	 * 
	 * @param typeRoot the given class file
	 * @param typeRoot2 the given class file to compare with
	 * @param component the given API component from which the given class file is coming from
	 * @param component2 the given API component to compare with
	 * @param referenceBaseline the given API baseline from which the given component <code>component</code> is coming from
	 * @param baseline the given API baseline from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param monitor
	 *
	 * @return a delta, an empty delta if no difference is found or <code>null</code> if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>one of the given components is null</li>
	 * <li>one of the given baselines is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiTypeRoot typeRoot,
			final IApiTypeRoot typeRoot2,
			final IApiComponent component,
			final IApiComponent component2,
			final IApiBaseline referenceBaseline,
			final IApiBaseline baseline,
			final int visibilityModifiers, 
			final IProgressMonitor monitor) {
		if (typeRoot == null || typeRoot2 == null) {
			throw new IllegalArgumentException("One of the given class files is null"); //$NON-NLS-1$
		}
		if (component == null || component2 == null) {
			throw new IllegalArgumentException("One of the given components is null"); //$NON-NLS-1$
		}
		if (referenceBaseline == null || baseline == null) {
			throw new IllegalArgumentException("One of the given baselines is null"); //$NON-NLS-1$
		}
		IDelta delta = null;
		try {
			ClassFileComparator comparator =
				new ClassFileComparator(
						typeRoot,
						typeRoot2,
						component,
						component2,
						referenceBaseline,
						baseline,
						visibilityModifiers);
			delta = comparator.getDelta(SubMonitor.convert(monitor));
			if (ApiPlugin.DEBUG_API_COMPARATOR) {
				IStatus status = comparator.getStatus();
				if(status != null) {
					ApiPlugin.log(status);
				}
			}
		}
		catch(CoreException e) {
			ApiPlugin.log(e);
		}
		return delta;
	}
	
	/**
	 * Returns a delta that corresponds to the comparison of the two given API baselines. 
	 * Nested API components with the same versions are not compared.
	 * <p>Equivalent to: compare(baseline, baseline2, visibilityModifiers, false);</p>
	 * 
	 * @param scope the given scope for the comparison
	 * @param baseline the given API baseline to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 * @param monitor the given progress monitor to report progress
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two baselines is null
	 *         CoreException if one of the element in the scope cannot be visited
	 */
	public static IDelta compare(
			final IApiScope scope,
			final IApiBaseline baseline,
			final int visibilityModifiers,
			final boolean force,
			final IProgressMonitor monitor) throws CoreException {
		if (scope == null || baseline == null) {
			throw new IllegalArgumentException("None of the scope or the baseline must be null"); //$NON-NLS-1$
		}
		SubMonitor localmonitor = SubMonitor.convert(monitor, 2);
		try {
			final Set deltas = new HashSet();
			final CompareApiScopeVisitor visitor = new CompareApiScopeVisitor(deltas, baseline, force, visibilityModifiers, localmonitor.newChild(1));
			scope.accept(visitor);
			if (visitor.containsError()) {
				return null;
			}
			if (deltas.isEmpty()) {
				return NO_DELTA;
			}
			final Delta globalDelta = new Delta();
			for (Iterator iterator = deltas.iterator(); iterator.hasNext(); ) {
				IDelta delta = (IDelta) iterator.next();
				delta.accept(new DeltaVisitor() {
					public void endVisit(IDelta localDelta) {
						if (localDelta.getChildren().length == 0) {
							switch(localDelta.getElementType()) {
								case IDelta.ANNOTATION_ELEMENT_TYPE :
								case IDelta.ENUM_ELEMENT_TYPE :
								case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
								case IDelta.METHOD_ELEMENT_TYPE :
								case IDelta.INTERFACE_ELEMENT_TYPE :
								case IDelta.CLASS_ELEMENT_TYPE :
								case IDelta.FIELD_ELEMENT_TYPE :
								case IDelta.API_COMPONENT_ELEMENT_TYPE :
								case IDelta.API_BASELINE_ELEMENT_TYPE : 
									globalDelta.add(localDelta);
							}
						}
					}
				});
			}
			Util.updateMonitor(localmonitor, 1);
			return globalDelta.isEmpty() ? NO_DELTA : globalDelta;
		}
		finally {
			localmonitor.done();
		}
	}

	/**
	 * Returns true, if the given type descriptor should be skipped, false otherwise.
	 * @param visibilityModifiers
	 * @param elementDescription
	 * @param typeDescriptor
	 * @return
	 */
	static boolean filterType(final int visibilityModifiers,
			IApiAnnotations elementDescription,
			IApiType typeDescriptor) {
		if (elementDescription != null && (elementDescription.getVisibility() & visibilityModifiers) == 0) {
			// we skip the class file according to their visibility
			return true;
		}
		if (visibilityModifiers == VisibilityModifiers.API) {
			// if the visibility is API, we only consider public and protected types or types 
			// without element description
			if (elementDescription == null
					|| Util.isDefault(typeDescriptor.getModifiers())
					|| Flags.isPrivate(typeDescriptor.getModifiers())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Performs the internal compare of the given {@link IApiComponent}s using their type containers
	 * @param component
	 * @param component2
	 * @param referenceBaseline
	 * @param baseline
	 * @param visibilityModifiers
	 * @param globalDelta
	 * @param monitor
	 * 
	 * @return a delta of changed API elements
	 * @throws CoreException
	 */
	private static IDelta internalCompare(final IApiComponent component, 
			final IApiComponent component2, 
			final IApiBaseline referenceBaseline,
			final IApiBaseline baseline, 
			final int visibilityModifiers,	
			final Delta globalDelta, 
			final IProgressMonitor monitor) throws CoreException {
		final Set typeRootBaseLineNames = new HashSet();
		final String id = component.getSymbolicName();
		IApiTypeContainer[] typeRootContainers = null;
		IApiTypeContainer[] typeRootContainers2 = null;
		final SubMonitor localmonitor = SubMonitor.convert(monitor, 4);
		final boolean isSWT = Util.ORG_ECLIPSE_SWT.equals(id);
		if (isSWT) {
			typeRootContainers = component.getApiTypeContainers();
			typeRootContainers2 = component2.getApiTypeContainers();
		} else {
			typeRootContainers = component.getApiTypeContainers(id);
			typeRootContainers2 = component2.getApiTypeContainers(id);
		}
		final IApiDescription apiDescription = component.getApiDescription();
		final IApiDescription apiDescription2 = component2.getApiDescription();
		Util.updateMonitor(localmonitor, 1);
		if (typeRootContainers != null) {
			for (int i = 0, max = typeRootContainers.length; i < max; i++) {
				Util.updateMonitor(localmonitor);
				IApiTypeContainer container = typeRootContainers[i];
				try {
					container.accept(new ApiTypeContainerVisitor() {
						public void visit(String packageName, IApiTypeRoot typeRoot) {
							Util.updateMonitor(localmonitor);
							String typeName = typeRoot.getTypeName();
							try {
								IApiType typeDescriptor = typeRoot.getStructure();
								IApiAnnotations elementDescription = apiDescription.resolveAnnotations(typeDescriptor.getHandle());
								if (typeDescriptor.isMemberType() || typeDescriptor.isAnonymous() || typeDescriptor.isLocal()) {
									// we skip nested types (member, local and anonymous)
									return;
								}
								int visibility = 0;
								if (elementDescription != null) {
									visibility = elementDescription.getVisibility();
								}
								IApiTypeRoot typeRoot2 = null;
								if (isSWT) {
									typeRoot2 = component2.findTypeRoot(typeName);
								} else{
									typeRoot2 = component2.findTypeRoot(typeName, id);
								}
								String deltaComponentID = null;
								IApiComponent provider = null;
								IApiDescription providerApiDesc = null;
								boolean reexported = false;
								if (typeRoot2 == null) {
									// check if the type is provided by a required component (it could have been moved/re-exported)
									IApiComponent[] providers = component2.getBaseline().resolvePackage(component2, packageName);
									int index = 0;
									while (typeRoot2 == null && index < providers.length) {
										Util.updateMonitor(localmonitor);
										IApiComponent p = providers[index];
										if (!p.equals(component2)) {
											String id2 = p.getSymbolicName();
											if (Util.ORG_ECLIPSE_SWT.equals(id2)) {
												typeRoot2 = p.findTypeRoot(typeName);
											} else {
												typeRoot2 = p.findTypeRoot(typeName, id2);
											}
											if (typeRoot2 != null) {
												provider = p;
												providerApiDesc = p.getApiDescription();
												IRequiredComponentDescription[] required = component2.getRequiredComponents();
												for (int k = 0; k < required.length; k++) {
													IRequiredComponentDescription description = required[k];
													if (description.getId().equals(id2)) {
														reexported = description.isExported();
														break;
													}
												}
											}
										}
										index++;
									}
								} else {
									provider = component2;
									providerApiDesc = apiDescription2;
								}
								Util.updateMonitor(localmonitor);
								deltaComponentID = Util.getDeltaComponentVersionsId(component2);
								if(typeRoot2 == null) {
									if ((visibility & visibilityModifiers) == 0) {
										// we skip the class file according to their visibility
										return;
									}
									if (visibilityModifiers == VisibilityModifiers.API) {
										// if the visibility is API, we only consider public and protected types
										if (Util.isDefault(typeDescriptor.getModifiers())
													|| Flags.isPrivate(typeDescriptor.getModifiers())) {
											return;
										}
									}
									globalDelta.add(
											new Delta(
													deltaComponentID,
													IDelta.API_COMPONENT_ELEMENT_TYPE,
													IDelta.REMOVED,
													IDelta.TYPE,
													RestrictionModifiers.NO_RESTRICTIONS,
													RestrictionModifiers.NO_RESTRICTIONS,
													typeDescriptor.getModifiers(),
													0,
													typeName,
													typeName,
													new String[] { typeName, Util.getComponentVersionsId(component2) }));
								} else {
									if ((visibility & visibilityModifiers) == 0) {
										// we skip the class file according to their visibility
										return;
									}
									IApiType typeDescriptor2 = typeRoot2.getStructure();
									IApiAnnotations elementDescription2 = providerApiDesc.resolveAnnotations(typeDescriptor2.getHandle());
									int visibility2 = 0;
									if (elementDescription2 != null) {
										visibility2 = elementDescription2.getVisibility();
									}
									if (visibilityModifiers == VisibilityModifiers.API) {
										// if the visibility is API, we only consider public and protected types
										if (Util.isDefault(typeDescriptor.getModifiers())
												|| Flags.isPrivate(typeDescriptor.getModifiers())) {
											return;
										}
									}
									if (Util.isAPI(visibility, typeDescriptor)) {
										if (!Util.isAPI(visibility2, typeDescriptor2)) {
											globalDelta.add(
												new Delta(
													deltaComponentID,
													IDelta.API_COMPONENT_ELEMENT_TYPE,
													IDelta.REMOVED,
													reexported ?  IDelta.REEXPORTED_API_TYPE : IDelta.API_TYPE,
													elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
													RestrictionModifiers.NO_RESTRICTIONS,
													typeDescriptor.getModifiers(),
													typeDescriptor2.getModifiers(),
													typeName,
													typeName,
													new String[] { typeName, Util.getComponentVersionsId(component2) }));
											return;
										}
									}
									if ((visibility2 & visibilityModifiers) == 0) {
										// we simply report a changed visibility
										globalDelta.add(
												new Delta(
														deltaComponentID,
														IDelta.API_COMPONENT_ELEMENT_TYPE,
														IDelta.CHANGED,
														IDelta.TYPE_VISIBILITY,
														elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
														RestrictionModifiers.NO_RESTRICTIONS,
														typeDescriptor.getModifiers(),
														typeDescriptor2.getModifiers(),
														typeName,
														typeName,
														new String[] { typeName, Util.getComponentVersionsId(component2)}));
									}
									typeRootBaseLineNames.add(typeName);
									ClassFileComparator comparator = new ClassFileComparator(typeDescriptor, typeRoot2, component, provider, referenceBaseline, baseline, visibilityModifiers);
									IDelta delta = comparator.getDelta(localmonitor.newChild(1));
									if (ApiPlugin.DEBUG_API_COMPARATOR) {
										IStatus status = comparator.getStatus();
										if(status != null) {
											ApiPlugin.log(status);
										}
									}
									if (delta != null && delta != NO_DELTA) {
										globalDelta.add(delta);
									}
								}
								Util.updateMonitor(localmonitor);
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
		Util.updateMonitor(localmonitor, 1);
		IRequiredComponentDescription[] requiredComponents = component.getRequiredComponents();
		int length = requiredComponents.length;
		if (length != 0) {
			for (int j = 0; j < length; j++) {
				Util.updateMonitor(localmonitor);
				IRequiredComponentDescription description = requiredComponents[j];
				if (description.isExported()) {
					final String currentComponentID = Util.getDeltaComponentVersionsId(component);
					String descriptionID = description.getId();
					IApiComponent currentRequiredApiComponent = referenceBaseline.getApiComponent(descriptionID);
					if (currentRequiredApiComponent == null) {
						continue;
					}
					final IApiDescription reexportedApiDescription = currentRequiredApiComponent.getApiDescription();
					IApiTypeContainer[] apiTypeContainers = currentRequiredApiComponent.getApiTypeContainers();
					if (apiTypeContainers != null) {
						for (int i = 0, max = apiTypeContainers.length; i < max; i++) {
							Util.updateMonitor(localmonitor);
							IApiTypeContainer container = apiTypeContainers[i];
							try {
								container.accept(new ApiTypeContainerVisitor() {
									public void visit(String packageName, IApiTypeRoot typeRoot) {
										Util.updateMonitor(localmonitor);
										String typeName = typeRoot.getTypeName();
										try {
											IApiType typeDescriptor = typeRoot.getStructure();
											IApiAnnotations elementDescription = reexportedApiDescription.resolveAnnotations(typeDescriptor.getHandle());
											if (typeDescriptor.isMemberType() || typeDescriptor.isAnonymous() || typeDescriptor.isLocal()) {
												// we skip nested types (member, local and anonymous)
												return;
											}
											int visibility = 0;
											if (elementDescription != null) {
												visibility = elementDescription.getVisibility();
											}
											IApiTypeRoot typeRoot2 = null;
											if (isSWT) {
												typeRoot2 = component2.findTypeRoot(typeName);
											} else{
												typeRoot2 = component2.findTypeRoot(typeName, id);
											}
											IApiDescription providerApiDesc = null;
											if (typeRoot2 == null) {
												// check if the type is provided by a required component (it could have been moved/re-exported)
												IApiComponent[] providers = component2.getBaseline().resolvePackage(component2, packageName);
												int index = 0;
												while (typeRoot2 == null && index < providers.length) {
													IApiComponent p = providers[index];
													if (!p.equals(component2)) {
														String id2 = p.getSymbolicName();
														if (Util.ORG_ECLIPSE_SWT.equals(id2)) {
															typeRoot2 = p.findTypeRoot(typeName);
														} else {
															typeRoot2 = p.findTypeRoot(typeName, id2);
														}
														if (typeRoot2 != null) {
															providerApiDesc = p.getApiDescription();
														}
													}
													index++;
												}
											} else {
												providerApiDesc = apiDescription2;
											}
											if(typeRoot2 == null) {
												if ((visibility & visibilityModifiers) == 0) {
													// we skip the class file according to their visibility
													return;
												}
												if (visibilityModifiers == VisibilityModifiers.API) {
													// if the visibility is API, we only consider public and protected types
													if (Util.isDefault(typeDescriptor.getModifiers())
																|| Flags.isPrivate(typeDescriptor.getModifiers())) {
														return;
													}
												}
												globalDelta.add(
														new Delta(
																currentComponentID,
																IDelta.API_COMPONENT_ELEMENT_TYPE,
																IDelta.REMOVED,
																IDelta.REEXPORTED_TYPE,
																RestrictionModifiers.NO_RESTRICTIONS,
																RestrictionModifiers.NO_RESTRICTIONS,
																typeDescriptor.getModifiers(),
																0,
																typeName,
																typeName,
																new String[] { typeName, Util.getComponentVersionsId(component) }));
											} else {
												typeRootBaseLineNames.add(typeName);
												IApiType typeDescriptor2 = typeRoot2.getStructure();
												IApiAnnotations elementDescription2 = providerApiDesc.resolveAnnotations(typeDescriptor2.getHandle());
												int visibility2 = 0;
												if (elementDescription2 != null) {
													visibility2 = elementDescription2.getVisibility();
												}
												// if the visibility is API, we only consider public and protected types
												if (Util.isDefault(typeDescriptor.getModifiers())
														|| Flags.isPrivate(typeDescriptor.getModifiers())) {
													return;
												}
												if (Util.isAPI(visibility, typeDescriptor)) {
													if (!Util.isAPI(visibility2, typeDescriptor2)) {
														globalDelta.add(
															new Delta(
																currentComponentID,
																IDelta.API_COMPONENT_ELEMENT_TYPE,
																IDelta.REMOVED,
																IDelta.REEXPORTED_API_TYPE,
																elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
																RestrictionModifiers.NO_RESTRICTIONS,
																typeDescriptor.getModifiers(),
																typeDescriptor2.getModifiers(),
																typeName,
																typeName,
																new String[] { typeName, Util.getComponentVersionsId(component) }));
														return;
													}
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
				}
			}
		}
		Util.updateMonitor(localmonitor, 1);
		if (typeRootContainers2 != null) {
			for (int i = 0, max = typeRootContainers2.length; i < max; i++) {
				Util.updateMonitor(localmonitor);
				IApiTypeContainer container = typeRootContainers2[i];
				try {
					container.accept(new ApiTypeContainerVisitor() {
						public void visit(String packageName, IApiTypeRoot typeRoot) {
							Util.updateMonitor(localmonitor);
							String typeName = typeRoot.getTypeName();
							try {
								IApiType type = typeRoot.getStructure();
								IApiAnnotations elementDescription = apiDescription2.resolveAnnotations(type.getHandle());
								if (type.isMemberType() || type.isLocal() || type.isAnonymous()) {
									// we skip nested types (member, local and anonymous)
									return;
								}
								if (filterType(visibilityModifiers, elementDescription, type)) {
									return;
								}
								if (typeRootBaseLineNames.contains(typeName)) {
									// already processed
									return;
								}
								typeRootBaseLineNames.add(typeName);
								String deltaComponentID = Util.getDeltaComponentVersionsId(component2);
								globalDelta.add(
										new Delta(
												deltaComponentID,
												IDelta.API_COMPONENT_ELEMENT_TYPE,
												IDelta.ADDED,
												IDelta.TYPE,
												elementDescription != null ? elementDescription.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
												RestrictionModifiers.NO_RESTRICTIONS,
												0,
												type.getModifiers(),
												typeName,
												typeName,
												new String[] { typeName, Util.getComponentVersionsId(component2) }));
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
		Util.updateMonitor(localmonitor, 1);
		requiredComponents = component2.getRequiredComponents();
		length = requiredComponents.length;
		if (length != 0) {
			for (int j = 0; j < length; j++) {
				Util.updateMonitor(localmonitor);
				IRequiredComponentDescription description = requiredComponents[j];
				if (description.isExported()) {
					final String currentComponentID = Util.getDeltaComponentVersionsId(component);
					String descriptionID = description.getId();
					IApiComponent currentRequiredApiComponent = baseline.getApiComponent(descriptionID);
					if (currentRequiredApiComponent == null) {
						continue;
					}
					IApiTypeContainer[] apiTypeContainers = currentRequiredApiComponent.getApiTypeContainers();
					final IApiDescription reexportedApiDescription = currentRequiredApiComponent.getApiDescription();
					if (apiTypeContainers != null) {
						for (int i = 0, max = apiTypeContainers.length; i < max; i++) {
							Util.updateMonitor(localmonitor);
							IApiTypeContainer container = apiTypeContainers[i];
							try {
								container.accept(new ApiTypeContainerVisitor() {
									public void visit(String packageName, IApiTypeRoot typeRoot) {
										Util.updateMonitor(localmonitor);
										String typeName = typeRoot.getTypeName();
										try {
											IApiType typeDescriptor = typeRoot.getStructure();
											IApiAnnotations elementDescription = reexportedApiDescription.resolveAnnotations(typeDescriptor.getHandle());
											if (typeDescriptor.isMemberType() || typeDescriptor.isAnonymous() || typeDescriptor.isLocal()) {
												// we skip nested types (member, local and anonymous)
												return;
											}
											if (filterType(visibilityModifiers, elementDescription, typeDescriptor)) {
												return;
											}
											if (typeRootBaseLineNames.contains(typeName)) {
												// already processed
												return;
											}
											typeRootBaseLineNames.add(typeName);
											globalDelta.add(
													new Delta(
															currentComponentID,
															IDelta.API_COMPONENT_ELEMENT_TYPE,
															IDelta.ADDED,
															IDelta.REEXPORTED_TYPE,
															elementDescription != null ? elementDescription.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
															RestrictionModifiers.NO_RESTRICTIONS,
															0,
															typeDescriptor.getModifiers(),
															typeName,
															typeName,
															new String[] { typeName, Util.getComponentVersionsId(component) }));
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
				}
			}
		}
		return globalDelta.isEmpty() ? NO_DELTA : globalDelta;
	}
}
