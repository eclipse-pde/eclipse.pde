/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.comparator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.signature.SignatureReader;

/**
 * Compares class files from the workspace to those in the default
 * {@link IApiBaseline}
 *
 * @since 1.0.0
 */
public class ClassFileComparator {

	private boolean isCheckedException(IApiBaseline baseline, IApiComponent apiComponent, String exceptionName) {
		if (baseline == null) {
			return true;
		}
		try {
			if (Util.isJavaLangRuntimeException(exceptionName)) {
				return false;
			}
			String packageName = Signatures.getPackageName(exceptionName);
			IApiTypeRoot result = Util.getClassFile(baseline.resolvePackage(apiComponent, packageName), exceptionName);
			if (result != null) {
				// TODO should this be reported as a checked exception
				IApiType exception = result.getStructure();
				if (exception == null) {
					return false;
				}
				while (!Util.isJavaLangObject(exception.getName())) {
					String superName = exception.getSuperclassName();
					packageName = Signatures.getPackageName(superName);
					result = Util.getClassFile(baseline.resolvePackage(apiComponent, packageName), superName);
					if (result == null) {
						// TODO should we report this failure ?
						if (ApiPlugin.DEBUG_CLASSFILE_COMPARATOR) {
							System.err.println("CHECKED EXCEPTION LOOKUP: Could not find " + superName + " in baseline " + baseline.getName() + " from component " + apiComponent.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						break;
					}
					exception = result.getStructure();
					if (Util.isJavaLangRuntimeException(exception.getName())) {
						return false;
					}
				}
			}
		} catch (CoreException e) {
			// by default exception are considered as checked exception
			reportStatus(e);
		}
		return true;
	}

	private IApiBaseline apiBaseline1 = null;
	private IApiBaseline apiBaseline2 = null;

	private IApiComponent component = null;
	private IApiComponent component2 = null;

	private Delta delta = null;
	private IApiType type1 = null;

	private IApiType type2 = null;

	private int visibilityModifiers;
	private int currentDescriptorRestrictions;
	private int initialDescriptorRestrictions;
	private MultiStatus status = null;

	/**
	 * Constructor
	 *
	 * @param classFile the class file from the workspace to compare
	 * @param classFile2 the class file from the baseline to compare to
	 * @param component the API component from the workspace
	 * @param component2 the API component from the baseline
	 * @param apiState the workspace API baseline
	 * @param apiState2 the baseline API baseline
	 * @param visibilityModifiers any modifiers from the class file
	 * @throws CoreException if the contents of the specified class files cannot
	 *             be acquired
	 */
	public ClassFileComparator(IApiTypeRoot classFile, IApiTypeRoot classFile2, IApiComponent component, IApiComponent component2, IApiBaseline apiState, IApiBaseline apiState2, int visibilityModifiers) throws CoreException {
		this.component = component;
		this.component2 = component2;
		this.type1 = classFile.getStructure();
		this.type2 = classFile2.getStructure();
		this.apiBaseline1 = apiState;
		this.apiBaseline2 = apiState2;
		this.visibilityModifiers = visibilityModifiers;
	}

	/**
	 * Constructor
	 *
	 * @param type the {@link IApiType} from the workspace to compare
	 * @param classFile2 the class file from the baseline to compare to
	 * @param component the API component from the workspace
	 * @param component2 the API component from the baseline
	 * @param apiState the workspace API baseline
	 * @param apiState2 the baseline API baseline
	 * @param visibilityModifiers any modifiers from the class file
	 * @throws CoreException if the contents of the specified class file cannot
	 *             be acquired
	 */
	public ClassFileComparator(IApiType type, IApiTypeRoot classFile2, IApiComponent component, IApiComponent component2, IApiBaseline apiState, IApiBaseline apiState2, int visibilityModifiers) throws CoreException {
		this.component = component;
		this.component2 = component2;
		this.type1 = type;
		this.type2 = classFile2.getStructure();
		this.apiBaseline1 = apiState;
		this.apiBaseline2 = apiState2;
		this.visibilityModifiers = visibilityModifiers;
	}

	private void addDelta(IDelta delta) {
		this.delta.add(delta);
	}

	private void addDelta(int elementType, int kind, int flags, int restrictions, int oldModifiers, int newModifiers, IApiType type, String key, String data) {
		this.addDelta(new Delta(Util.getDeltaComponentVersionsId(this.component2), elementType, kind, flags, restrictions, oldModifiers, newModifiers, type.getName(), key, data));
	}

	private void addDelta(int elementType, int kind, int flags, int restrictions, int oldModifiers, int newModifiers, IApiType type, String key, String[] datas) {
		this.addDelta(new Delta(Util.getDeltaComponentVersionsId(this.component2), elementType, kind, flags, restrictions, 0, oldModifiers, newModifiers, type.getName(), key, datas));
	}

	private void addDelta(int elementType, int kind, int flags, int currentRestrictions, int previousRestrictions, int oldModifiers, int newModifiers, IApiType type, String key, String[] datas) {
		this.addDelta(new Delta(Util.getDeltaComponentVersionsId(this.component2), elementType, kind, flags, currentRestrictions, previousRestrictions, oldModifiers, newModifiers, type.getName(), key, datas));
	}

	/**
	 * Checks if the super-class set has been change in any way compared to the
	 * baseline (grown or reduced or types changed)
	 */
	private void checkSuperclass() {
		// check superclass set
		List<IApiType> superclassList1 = getSuperclassList(this.type1);
		if (!isStatusOk()) {
			return;
		}
		Set<String> superclassNames2 = null;
		List<IApiType> superclassList2 = getSuperclassList(this.type2);
		if (!isStatusOk()) {
			return;
		}
		if (superclassList2 != null) {
			superclassNames2 = new HashSet<>();
			for (IApiType type : superclassList2) {
				superclassNames2.add(type.getName());
			}
		}
		if (superclassList1 == null) {
			if (superclassList2 != null) {
				// If superclassList2 has 1 abstract method and current class don't have method
				// implemented, then breaking change
				for (IApiType iApiType : superclassList2) {
					IApiMethod[] methods = iApiType.getMethods();
					for (IApiMethod iMethod : methods) {
						boolean isAbstractMethod = Flags.isAbstract(iMethod.getModifiers());
						if (isAbstractMethod) {
							boolean isBreakingChange = false;
							IApiMethod meth = this.type2.getMethod(iMethod.getName(), iMethod.getSignature());
							if(meth == null) {
								isBreakingChange = true;
							}
							if(meth !=null) {
								isBreakingChange= Flags.isSynthetic(meth.getModifiers());
							}
							if (isBreakingChange) {
								this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.SUPERCLASS_BREAKING,
										this.currentDescriptorRestrictions, this.type1.getModifiers(),
										this.type2.getModifiers(), this.type1, this.type1.getName(),
										Util.getDescriptorName(type1));
								return;
							}
						}
					}
				}
				// this means the direct super class of descriptor1 is
				// java.lang.Object
				this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.SUPERCLASS, this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(), this.type1, this.type1.getName(), Util.getDescriptorName(type1));
			}
		} else if (superclassList2 == null) {
			// this means the direct super class of descriptor2 is
			// java.lang.Object
			this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.SUPERCLASS, this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(), this.type1, this.type1.getName(), Util.getDescriptorName(type1));
		}
		// get superclass of descriptor2
		if (superclassList1 != null && superclassList2 != null) {
			IApiType superclassType2 = superclassList2.get(0);
			IApiType superclassType = superclassList1.get(0);
			if (!superclassType.getName().equals(superclassType2.getName())) {
				if (!superclassNames2.contains(superclassType.getName())) {
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.SUPERCLASS, this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(), this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				} else {
					this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.SUPERCLASS, this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(), this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				}
			}
		}
	}

	/**
	 * reports problem status to the comparators' complete status
	 *
	 * @param newstatus
	 */
	protected void reportStatus(IStatus newstatus) {
		if (this.status == null) {
			String msg = MessageFormat.format(ComparatorMessages.ClassFileComparator_0, this.type1.getName());
			this.status = new MultiStatus(ApiPlugin.PLUGIN_ID, IStatus.ERROR, msg, null);
		}
		this.status.add(newstatus);
	}

	/**
	 * Report problem to the comparators' status
	 *
	 * @param e
	 */
	private void reportStatus(CoreException e) {
		reportStatus(e.getStatus());
	}

	/**
	 * @return if the status of the compare is ok
	 */
	private boolean isStatusOk() {
		return this.status == null;
	}

	/**
	 * @return the status of the compare and delta creation
	 */
	public IStatus getStatus() {
		return this.status;
	}

	/**
	 * Checks if there are any changes to the super-interface set for the
	 * current type descriptor context. A change is one of:
	 * <ul>
	 * <li>An interface has been added to the current super-interface set
	 * compared to the current baseline</li>
	 * <li>An interface has been removed from the current super-interface set
	 * compared to the current baseline</li>
	 * <li>An interface has changed (same number of interfaces, but different
	 * types) compared to the current baseline</li>
	 * </ul>
	 */
	private void checkSuperInterfaces() {
		Set<IApiType> superinterfacesSet1 = getInterfacesSet(this.type1);
		if (!isStatusOk()) {
			return;
		}
		Set<IApiType> superinterfacesSet2 = getInterfacesSet(this.type2);
		if (!isStatusOk()) {
			return;
		}
		if (superinterfacesSet1 == null) {
			if (superinterfacesSet2 != null) {
				if (this.type1.isClass()) {
					for (IApiType iApiType : superinterfacesSet2) {
						IApiMethod[] methods = iApiType.getMethods();
						for (IApiMethod iMethod : methods) {

							boolean defMethod = iMethod.isDefaultMethod();
							if (defMethod == false) {
								boolean isBreakingChange = false;
								IApiMethod meth = this.type2.getMethod(iMethod.getName(), iMethod.getSignature());
								if (meth == null) {
									// check in superclasses
									List<IApiType> superclassList = getSuperclassList(this.type2);
									if (superclassList != null) {
										for (IApiType apiType : superclassList) {
											meth = apiType.getMethod(iMethod.getName(),
													iMethod.getSignature());
											if (meth != null) {
												break;
											}
										}
									}
								}
								if (meth == null) {
									isBreakingChange = true;
								}
								if(meth !=null) {
									isBreakingChange = Flags.isSynthetic(meth.getModifiers());
								}
								if (isBreakingChange) {
									this.addDelta(getElementType(this.type1), IDelta.ADDED,
											IDelta.EXPANDED_SUPERINTERFACES_SET_BREAKING,
											this.currentDescriptorRestrictions, this.type1.getModifiers(),
											this.type2.getModifiers(), this.type1, this.type1.getName(),
											Util.getDescriptorName(type1));
									return;
								}
							}
						}
					}
				}

				this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.EXPANDED_SUPERINTERFACES_SET,
						this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(),
						this.type1, this.type1.getName(), new String[] { Util.getDescriptorName(type1),
								computeDiff(superinterfacesSet1, superinterfacesSet2, true) });
				if (this.type1.isInterface()) {
					for (IApiType type : superinterfacesSet2) {
						IApiMethod[] methods = type.getMethods();
						int length = methods.length;
						if (length != 0) {
							// we should check if every method defined in the
							// new interface exists in the old hierarchy
							// could be methods moved up in the hierarchy
							boolean isSuperInterfaceWithMethodDeltaAdded = false;
							for (int j = 0; j < length; j++) {
								IApiMethod method = methods[j];
								IApiMethod method3 = this.type1.getMethod(method.getName(), method.getSignature());
								if (method3 == null) {
									String key = this.type1.getName();
									boolean isDefaultMethod = false;
									if (this.type2.getMethod(method.getName(), method.getSignature()) != null) {
										isDefaultMethod = this.type2.getMethod(method.getName(), method.getSignature()).isDefaultMethod();
										if(isDefaultMethod) {
											key = getKeyForMethod(this.type2.getMethod(method.getName(), method.getSignature()), this.type2);
										}
									}
									if (!isDefaultMethod && isSuperInterfaceWithMethodDeltaAdded == false) {
										this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.SUPER_INTERFACE_WITH_METHODS, this.currentDescriptorRestrictions, this.initialDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(), this.type1, key, new String[] {
												Util.getDescriptorName(type1),
												type.getName(),
												getMethodDisplayName(method, type) });
										isSuperInterfaceWithMethodDeltaAdded = true;
									}
									if (isDefaultMethod) {
										this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.SUPER_INTERFACE_DEFAULT_METHOD, this.currentDescriptorRestrictions, this.initialDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(), this.type1, key, new String[] {
												Util.getDescriptorName(type1),
												type.getName(),
												getMethodDisplayName(method, type) });
									}
								}
							}
						}
					}
				}
			}
		} else if (superinterfacesSet2 == null) {
			this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.CONTRACTED_SUPERINTERFACES_SET,
					this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(),
					this.type1, this.type1.getName(), new String[] { Util.getDescriptorName(type1),
							computeDiff(superinterfacesSet1, superinterfacesSet2, false) });
		} else {
			Set<String> names2 = new HashSet<>();
			for (IApiType iApiType : superinterfacesSet2) {
				names2.add(iApiType.getName());
			}
			Set<String> names1 = new HashSet<>();
			for (IApiType iApiType : superinterfacesSet1) {
				names1.add(iApiType.getName());
			}
			boolean contracted = false;
			for (String name : names1) {
				if (!names2.remove(name)) {
					contracted = true;
				}
			}
			if (contracted) {
				this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.CONTRACTED_SUPERINTERFACES_SET,
						this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(),
						this.type1, this.type1.getName(), new String[] { Util.getDescriptorName(type1),
								computeDiff(superinterfacesSet1, superinterfacesSet2, false) });
				return;
			}
			if (names2.size() > 0) {
				if (this.type1.isClass()) {
					for (IApiType apiType : superinterfacesSet2) {
						String name = apiType.getName();
						if (names2.contains(name)) {
							IApiMethod[] methods = apiType.getMethods();
							for (IApiMethod iMethod : methods) {
								boolean defMethod = iMethod.isDefaultMethod();
								if (defMethod == false) {
									boolean isBreakingChange = false;
									IApiMethod meth = this.type2.getMethod(iMethod.getName(), iMethod.getSignature());
									if (meth == null) {
										// check in superclasses
										List<IApiType> superclassList = getSuperclassList(this.type2);
										if (superclassList != null) {
											for (IApiType type : superclassList) {
												meth = type.getMethod(iMethod.getName(), iMethod.getSignature());
												if (meth != null) {
													break;
												}
											}
										}
									}
									if (meth == null) {
										isBreakingChange = true;
									}
									if(meth !=null) {
										isBreakingChange = Flags.isSynthetic(meth.getModifiers());
									}
									if (isBreakingChange) {
										this.addDelta(getElementType(this.type1), IDelta.CHANGED,
												IDelta.EXPANDED_SUPERINTERFACES_SET_BREAKING,
												this.currentDescriptorRestrictions, this.type1.getModifiers(),
												this.type2.getModifiers(), this.type1, this.type1.getName(),
												Util.getDescriptorName(type1));
										return;
									}
								}
							}

						}
					}
				}

				this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.EXPANDED_SUPERINTERFACES_SET,
						this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(),
						this.type1, this.type1.getName(), new String[] { Util.getDescriptorName(type1),
								computeDiff(superinterfacesSet1, superinterfacesSet2, true) });
				if (this.type1.isInterface()) {
					for (String interfaceName : names2) {
						try {
							IApiTypeRoot interfaceClassFile = getType(interfaceName, this.component2, this.apiBaseline2);
							if (interfaceClassFile == null) {
								continue;
							}
							IApiType type = interfaceClassFile.getStructure();
							if (type == null) {
								continue;
							}
							IApiMethod[] methods = type.getMethods();
							int length = methods.length;
							if (length > 0) {
								// we should check if every method defined in
								// the new interface exists in the old hierarchy
								// could be methods moved up in the hierarchy
								methodLoop: for (int j = 0; j < length; j++) {
									IApiMethod method = methods[j];
									boolean found = false;
									interfaceLoop: for (IApiType superTypeDescriptor : superinterfacesSet1) {
										IApiMethod method3 = superTypeDescriptor.getMethod(method.getName(), method.getSignature());
										if (method3 == null) {
											continue interfaceLoop;
										} else {
											found = true;
											break interfaceLoop;
										}
									}
									if (!found) {
										String key = this.type1.getName();
										boolean isDefaultMethod = false;
										if(this.type2.getMethod(method.getName(), method.getSignature())!=null) {
											isDefaultMethod = this.type2.getMethod(method.getName(), method.getSignature()).isDefaultMethod();
											if (isDefaultMethod) {
												key = getKeyForMethod(this.type2.getMethod(method.getName(), method.getSignature()), this.type2);
											}
										}
										this.addDelta(getElementType(this.type1), IDelta.ADDED, isDefaultMethod ? IDelta.DEFAULT_METHOD : IDelta.SUPER_INTERFACE_WITH_METHODS, this.currentDescriptorRestrictions, this.initialDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(), this.type1, key, new String[] {
												Util.getDescriptorName(type1),
												type.getName(),
												getMethodDisplayName(method, type) });
										break methodLoop;
									}
								}
							}
						} catch (CoreException e) {
							ApiPlugin.log(e);
						}
					}
				}
			}
		}
	}

	private String computeDiff(Set<IApiType> superinterfacesSet1, Set<IApiType> superinterfacesSet2, boolean expand) {
		Set<String> namesToReturn = new HashSet<>();
		if (superinterfacesSet1 == null) {
			for (IApiType iApiType : superinterfacesSet2) {
				namesToReturn.add(iApiType.getName());
			}
			return processNames(namesToReturn);

		}
		if (superinterfacesSet2 == null) {
			for (IApiType iApiType : superinterfacesSet1) {
				namesToReturn.add(iApiType.getName());
			}
			return processNames(namesToReturn);

		}
		for (Iterator<IApiType> iterator = expand ? superinterfacesSet2.iterator()
				: superinterfacesSet1.iterator(); iterator.hasNext();) {
			namesToReturn.add(iterator.next().getName());
		}
		Set<String> names1 = new HashSet<>();
		for (Iterator<IApiType> iterator = expand ? superinterfacesSet1.iterator()
				: superinterfacesSet2.iterator(); iterator.hasNext();) {
			names1.add(iterator.next().getName());
		}

		for (String name : names1) {
			namesToReturn.remove(name);

		}
		return processNames(namesToReturn);

	}

	private String processNames(Set<String> namesToReturn) {
		StringBuilder str = new StringBuilder();
		for (String string : namesToReturn) {
			str.append(string);
			str.append(',');
		}
		return str.substring(0, str.length() - 1);
	}

	private void checkTypeMembers() throws CoreException {
		IApiType[] typeMembers = this.type1.getMemberTypes();
		IApiType[] typeMembers2 = this.type2.getMemberTypes();
		List<String> added = new ArrayList<>(typeMembers2.length);
		for (IApiType type : typeMembers2) {
			added.add(type.getName());
		}
		if (typeMembers.length > 0) {
			if (typeMembers2.length == 0) {
				loop: for (IApiType typeMember : typeMembers) {
					try {
						// check visibility
						IApiDescription apiDescription = this.component.getApiDescription();
						IApiAnnotations memberTypeElementDescription = apiDescription.resolveAnnotations(typeMember.getHandle());
						int memberTypeVisibility = 0;
						if (memberTypeElementDescription != null) {
							memberTypeVisibility = memberTypeElementDescription.getVisibility();
						}
						if ((memberTypeVisibility & visibilityModifiers) == 0) {
							// we skip the class file according to their
							// visibility
							continue loop;
						}
						if (visibilityModifiers == VisibilityModifiers.API) {
							// if the visibility is API, we only consider public
							// and protected types
							if (Util.isDefault(typeMember.getModifiers()) || Flags.isPrivate(typeMember.getModifiers())) {
								continue loop;
							}
						}
						this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.TYPE_MEMBER, this.currentDescriptorRestrictions, typeMember.getModifiers(), 0, this.type1, typeMember.getName(), new String[] {
								typeMember.getName().replace('$', '.'),
								Util.getComponentVersionsId(component2) });
					} catch (CoreException e) {
						reportStatus(e);
					}
				}
				return;
			}
			// check removed or added type members
			List<IApiType> removedTypeMembers = new ArrayList<>();
			loop: for (IApiType typeMember : typeMembers) {
				IApiType typeMember2 = this.type2.getMemberType(typeMember.getSimpleName());
				if (typeMember2 == null) {
					removedTypeMembers.add(typeMember);
				} else {
					added.remove(typeMember2.getName());
					// check deltas inside the type member
					try {
						// check visibility of member types
						IApiDescription apiDescription = this.component.getApiDescription();
						IApiAnnotations memberTypeElementDescription = apiDescription.resolveAnnotations(typeMember.getHandle());
						int memberTypeVisibility = 0;
						if (memberTypeElementDescription != null) {
							memberTypeVisibility = memberTypeElementDescription.getVisibility();
						}
						if ((memberTypeVisibility & visibilityModifiers) == 0) {
							// we skip the class file according to their
							// visibility
							continue loop;
						}
						IApiDescription apiDescription2 = this.component2.getApiDescription();
						IApiAnnotations memberTypeElementDescription2 = apiDescription2.resolveAnnotations(typeMember2.getHandle());
						int memberTypeVisibility2 = 0;
						if (memberTypeElementDescription2 != null) {
							memberTypeVisibility2 = memberTypeElementDescription2.getVisibility();
						}
						String deltaComponentID = Util.getDeltaComponentVersionsId(component2);
						int restrictions = memberTypeElementDescription2 != null ? memberTypeElementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
						if (Flags.isFinal(this.type2.getModifiers())) {
							restrictions |= RestrictionModifiers.NO_EXTEND;
						}
						if (isAPI(memberTypeVisibility, typeMember) && !isAPI(memberTypeVisibility2, typeMember2)) {
							this.addDelta(new Delta(deltaComponentID, getElementType(typeMember), IDelta.CHANGED, IDelta.DECREASE_ACCESS, restrictions | this.currentDescriptorRestrictions, 0, typeMember.getModifiers(), typeMember2.getModifiers(), typeMember.getName(), typeMember.getName(), new String[] { typeMember.getName().replace('$', '.') }));
							continue;
						}
						if ((memberTypeVisibility2 & visibilityModifiers) == 0) {
							// we simply report a changed visibility
							this.addDelta(new Delta(deltaComponentID, getElementType(typeMember), IDelta.CHANGED, IDelta.TYPE_VISIBILITY, restrictions | this.currentDescriptorRestrictions, 0, typeMember.getModifiers(), typeMember2.getModifiers(), typeMember.getName(), typeMember.getName(), new String[] { typeMember.getName().replace('$', '.') }));
						}
						if (this.visibilityModifiers == VisibilityModifiers.API) {
							// if the visibility is API, we only consider public
							// and protected types
							if (Util.isDefault(typeMember2.getModifiers()) || Flags.isPrivate(typeMember2.getModifiers())) {
								continue loop;
							}
						}
						IApiTypeRoot memberType2 = this.component2.findTypeRoot(typeMember.getName());
						ClassFileComparator comparator = new ClassFileComparator(typeMember, memberType2, this.component, this.component2, this.apiBaseline1, this.apiBaseline2, this.visibilityModifiers);
						IDelta delta2 = comparator.getDelta(null);
						if (delta2 != null && delta2 != ApiComparator.NO_DELTA) {
							this.addDelta(delta2);
						}
					} catch (CoreException e) {
						reportStatus(e);
					}
				}
			}
			loop: for (IApiType typeMember : removedTypeMembers) {
				try {
					// check visibility
					IApiDescription apiDescription = this.component.getApiDescription();
					IApiAnnotations memberTypeElementDescription = apiDescription.resolveAnnotations(typeMember.getHandle());
					int memberTypeVisibility = 0;
					if (memberTypeElementDescription != null) {
						memberTypeVisibility = memberTypeElementDescription.getVisibility();
					}
					if ((memberTypeVisibility & visibilityModifiers) == 0) {
						// we skip the class file according to their visibility
						continue loop;
					}
					if (this.visibilityModifiers == VisibilityModifiers.API) {
						// if the visibility is API, we only consider public and
						// protected types
						if (Util.isDefault(typeMember.getModifiers()) || Flags.isPrivate(typeMember.getModifiers())) {
							continue loop;
						}
					}
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.TYPE_MEMBER, memberTypeElementDescription != null ? memberTypeElementDescription.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS, typeMember.getModifiers(), 0, this.type1, typeMember.getName(), new String[] {
							typeMember.getName().replace('$', '.'),
							Util.getComponentVersionsId(component2) });
				} catch (CoreException e) {
					reportStatus(e);
				}
			}
		}
		// report remaining types as addition
		// Report delta as a breakage
		loop: for (String name : added) {
			try {
				int index = name.lastIndexOf('$');
				IApiType typeMember = this.type2.getMemberType(name.substring(index + 1));
				// check visibility
				IApiDescription apiDescription2 = this.component2.getApiDescription();
				IApiAnnotations memberTypeElementDescription2 = apiDescription2.resolveAnnotations(typeMember.getHandle());
				int memberTypeVisibility2 = 0;
				if (memberTypeElementDescription2 != null) {
					memberTypeVisibility2 = memberTypeElementDescription2.getVisibility();
				}
				if ((memberTypeVisibility2 & visibilityModifiers) == 0) {
					// we skip the class file according to their visibility
					continue loop;
				}
				if (this.visibilityModifiers == VisibilityModifiers.API) {
					// if the visibility is API, we only consider public and
					// protected types
					if (Util.isDefault(typeMember.getModifiers()) || Flags.isPrivate(typeMember.getModifiers())) {
						continue loop;
					}
				}
				this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.TYPE_MEMBER, this.currentDescriptorRestrictions, 0, typeMember.getModifiers(), this.type1, typeMember.getSimpleName(), typeMember.getSimpleName());
			} catch (CoreException e) {
				reportStatus(e);
			}
		}
	}

	private void checkGenericSignature(String signature1, String signature2, IApiMember element1, IApiMember element2) {
		if (signature1 == null) {
			if (signature2 != null) {
				// added type parameter from scratch (none before)
				// report delta as compatible
				SignatureDescriptor signatureDescriptor2 = getSignatureDescriptor(signature2);
				TypeParameterDescriptor[] typeParameterDescriptors = signatureDescriptor2.getTypeParameterDescriptors();
				if (typeParameterDescriptors.length != 0) {
					this.addDelta(getElementType(element1), IDelta.ADDED, IDelta.TYPE_PARAMETERS, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, getKeyFor(element2, this.type1), new String[] {
							getDataFor(element1, this.type1) });
				} else if (signatureDescriptor2.getTypeArguments().length != 0) {
					this.addDelta(getElementType(element1), IDelta.ADDED, IDelta.TYPE_ARGUMENTS, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, getKeyFor(element2, this.type1), new String[] {
							getDataFor(element1, this.type1) });
				}
			}
		} else if (signature2 == null) {
			// removed type parameters
			SignatureDescriptor signatureDescriptor = getSignatureDescriptor(signature1);
			TypeParameterDescriptor[] typeParameterDescriptors = signatureDescriptor.getTypeParameterDescriptors();
			int length = typeParameterDescriptors.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					TypeParameterDescriptor typeParameterDescriptor = typeParameterDescriptors[i];
					this.addDelta(getElementType(element1), IDelta.REMOVED, IDelta.TYPE_PARAMETER, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, getKeyFor(element2, this.type1), new String[] {
							getDataFor(element1, type1),
							typeParameterDescriptor.name });
				}
			} else {
				String[] typeArguments = signatureDescriptor.getTypeArguments();
				length = typeArguments.length;
				if (length != 0) {
					for (int i = 0; i < length; i++) {
						String typeArgument = typeArguments[i];
						this.addDelta(getElementType(element1), IDelta.REMOVED, IDelta.TYPE_ARGUMENT, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, getKeyFor(element2, this.type1), new String[] {
								getDataFor(element1, type1), typeArgument });
					}
				}
			}
		} else {
			// both types have generic signature
			// need to check delta for type parameter one by one
			SignatureDescriptor signatureDescriptor = getSignatureDescriptor(signature1);
			SignatureDescriptor signatureDescriptor2 = getSignatureDescriptor(signature2);

			TypeParameterDescriptor[] typeParameterDescriptors1 = signatureDescriptor.getTypeParameterDescriptors();
			TypeParameterDescriptor[] typeParameterDescriptors2 = signatureDescriptor2.getTypeParameterDescriptors();
			int length = typeParameterDescriptors1.length;
			int length2 = typeParameterDescriptors2.length;
			int min = length;
			int max = length2;
			if (length > length2) {
				min = length2;
				max = length;
			}
			int i = 0;
			for (; i < min; i++) {
				TypeParameterDescriptor parameterDescriptor1 = typeParameterDescriptors1[i];
				TypeParameterDescriptor parameterDescriptor2 = typeParameterDescriptors2[i];
				String name = parameterDescriptor1.name;
				if (!name.equals(parameterDescriptor2.name)) {
					this.addDelta(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_PARAMETER_NAME, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, name, new String[] {
							getDataFor(element1, type1), name });
				}
				if (parameterDescriptor1.classBound == null) {
					if (parameterDescriptor2.classBound != null) {
						// report delta added class bound of a type parameter
						this.addDelta(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, IDelta.ADDED, IDelta.CLASS_BOUND, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, name, new String[] {
								getDataFor(element1, type1), name });
					}
				} else if (parameterDescriptor2.classBound == null) {
					// report delta removed class bound of a type parameter
					this.addDelta(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, IDelta.REMOVED, IDelta.CLASS_BOUND, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, name, new String[] {
							getDataFor(element1, type1), name });
				} else if (!parameterDescriptor1.classBound.equals(parameterDescriptor2.classBound)) {
					// report delta changed class bound of a type parameter
					this.addDelta(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, IDelta.CHANGED, IDelta.CLASS_BOUND, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, name, new String[] {
							getDataFor(element1, type1), name });
				}
				List<String> interfaceBounds1 = parameterDescriptor1.interfaceBounds;
				List<String> interfaceBounds2 = parameterDescriptor2.interfaceBounds;
				if (interfaceBounds1 == null) {
					if (interfaceBounds2 != null) {
						for (String string : interfaceBounds2) {
							// report delta added interface bounds
							this.addDelta(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, IDelta.ADDED, IDelta.INTERFACE_BOUND, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, name, new String[] {
									getDataFor(element1, type1), name,
									string });
						}
					}
				} else if (interfaceBounds2 == null) {
					// report delta removed interface bounds
					for (String string : interfaceBounds1) {
						// report delta added interface bounds
						this.addDelta(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, IDelta.REMOVED, IDelta.INTERFACE_BOUND, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, name, new String[] {
								getDataFor(element1, type1), name,
								string });
					}
				} else {
					int size1 = interfaceBounds1.size();
					int size2 = interfaceBounds2.size();
					int boundsMin = size1;
					int boundsMax = size2;
					if (size1 > size2) {
						boundsMin = size2;
						boundsMax = size1;
					}
					int index = 0;
					for (; index < boundsMin; index++) {
						String currentInterfaceBound = interfaceBounds1.get(index);
						if (!currentInterfaceBound.equals(interfaceBounds2.get(index))) {
							// report delta: different interface bounds (or
							// reordered interface bound)
							this.addDelta(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, IDelta.CHANGED, IDelta.INTERFACE_BOUND, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, name, new String[] {
									getDataFor(element1, type1), name,
									currentInterfaceBound });
						}
					}
					if (boundsMin != boundsMax) {
						// if max = length2 => addition of type parameter
						// descriptor
						// if max = length => removal of type parameter
						// descriptor
						boolean added = boundsMax == size2;
						for (; index < boundsMax; index++) {
							String currentInterfaceBound = added ? (String) interfaceBounds2.get(index) : (String) interfaceBounds1.get(index);
							this.addDelta(IDelta.TYPE_PARAMETER_ELEMENT_TYPE, added ? IDelta.ADDED : IDelta.REMOVED, IDelta.INTERFACE_BOUND, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, getKeyFor(element2, this.type1), new String[] {
									getDataFor(element1, type1), name,
									currentInterfaceBound });
						}
					}
				}
			}
			if (min != max) {
				// if max = length2 => addition of type parameter descriptor
				// if max = length => removal of type parameter descriptor
				boolean added = max == length2;
				for (; i < max; i++) {
					TypeParameterDescriptor currentTypeParameter = added ? typeParameterDescriptors2[i] : typeParameterDescriptors1[i];
					int kind = added ? IDelta.ADDED : IDelta.REMOVED;
					int flags = added && length == 0 ? IDelta.TYPE_PARAMETERS : IDelta.TYPE_PARAMETER;
					this.addDelta(getElementType(element1), kind, flags, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, getKeyFor(element2, this.type1), new String[] {
							getDataFor(element1, type1),
							currentTypeParameter.name });
				}
			}
			if (length2 > 0 || length > 0) {
				return;
			}
			String[] typeArguments = signatureDescriptor.getTypeArguments();
			String[] typeArguments2 = signatureDescriptor2.getTypeArguments();
			length = typeArguments.length;
			length2 = typeArguments2.length;
			min = length;
			max = length2;
			if (length > length2) {
				min = length2;
				max = length;
			}
			i = 0;
			for (; i < min; i++) {
				String currentTypeArgument = typeArguments[i];
				String newTypeArgument = typeArguments2[i];
				if (!currentTypeArgument.equals(newTypeArgument)) {
					this.addDelta(getElementType(element1), IDelta.CHANGED, IDelta.TYPE_ARGUMENT, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, getKeyFor(element2, this.type1), new String[] {
							getDataFor(element1, type1), currentTypeArgument,
							newTypeArgument });
				}
			}
			if (min != max) {
				// if max = length2 => addition of type arguments
				// if max = length => removal of type arguments
				boolean added = max == length2;
				for (; i < max; i++) {
					String currentTypeArgument = added ? typeArguments2[i] : typeArguments[i];
					this.addDelta(getElementType(element1), added ? IDelta.ADDED : IDelta.REMOVED, IDelta.TYPE_ARGUMENT, this.currentDescriptorRestrictions, element1.getModifiers(), element2.getModifiers(), this.type1, getKeyFor(element2, this.type1), new String[] {
							getDataFor(element1, type1), currentTypeArgument });
				}
			}
		}
	}

	/**
	 * Recursively collects all of the super-interfaces of the given type
	 * descriptor within the scope of the given API component
	 *
	 * @param type
	 * @param set
	 */
	private void collectAllInterfaces(IApiType type, Set<IApiType> set) {
		try {
			IApiType[] interfaces = type.getSuperInterfaces();
			if (interfaces != null) {
				for (IApiType anInterface : interfaces) {
					int visibility = VisibilityModifiers.PRIVATE;
					IApiComponent ifaceComponent = anInterface.getApiComponent();
					IApiDescription apiDescription = ifaceComponent.getApiDescription();
					IApiAnnotations elementDescription = apiDescription.resolveAnnotations(anInterface.getHandle());
					if (elementDescription != null) {
						visibility = elementDescription.getVisibility();
					}
					if ((visibility & visibilityModifiers) != 0) {
						set.add(anInterface);
					}
					collectAllInterfaces(anInterface, set);
				}
			}
			String superclassName = type.getSuperclassName();
			if (superclassName != null && !Util.isJavaLangObject(superclassName)) {
				IApiType superclass = type.getSuperclass();
				collectAllInterfaces(superclass, set);
			}
		} catch (CoreException e) {
			if (ApiPlugin.DEBUG_API_COMPARATOR) {
				ApiPlugin.log(e);
			}
			reportStatus(e);
		}
	}

	private String getDataFor(IApiMember member, IApiType type) {
		switch (member.getType()) {
			case IApiElement.TYPE:
				if (((IApiType) member).isMemberType()) {
					return member.getName().replace('$', '.');
				}
				return member.getName();
			case IApiElement.METHOD:
				StringBuilder buffer = new StringBuilder();
				buffer.append(type.getName()).append('.').append(getMethodDisplayName((IApiMethod) member, type));
				return String.valueOf(buffer);
			case IApiElement.FIELD:
				buffer = new StringBuilder();
				buffer.append(type.getName()).append('.').append(member.getName());
				return String.valueOf(buffer);
			default:
				break;
		}
		return null;
	}

	private String getKeyFor(IApiMember member, IApiType type) {
		switch (member.getType()) {
			case IApiElement.TYPE:
				return member.getName();
			case IApiElement.METHOD:
				return getKeyForMethod((IApiMethod) member, type);
			case IApiElement.FIELD:
				return member.getName();
			default:
				break;
		}
		return null;
	}

	/**
	 * Returns a new {@link Delta} to use, and resets the status of creating a
	 * delta
	 *
	 * @return
	 */
	private Delta createDelta() {
		return new Delta();
	}

	/**
	 * Returns the change(s) between the type descriptor and its equivalent in
	 * the current baseline.
	 *
	 * @return the changes in the type descriptor or <code>null</code>
	 */
	public IDelta getDelta(IProgressMonitor monitor) {
		try {
			this.delta = createDelta();
			// check visibility
			int typeAccess = this.type1.getModifiers();
			int typeAccess2 = this.type2.getModifiers();
			final IApiDescription component2ApiDescription = component2.getApiDescription();
			IApiAnnotations elementDescription2 = component2ApiDescription.resolveAnnotations(this.type2.getHandle());
			this.initialDescriptorRestrictions = RestrictionModifiers.NO_RESTRICTIONS;
			this.currentDescriptorRestrictions = RestrictionModifiers.NO_RESTRICTIONS;
			if (elementDescription2 != null) {
				int restrictions2 = elementDescription2.getRestrictions();
				IApiDescription apiDescription = this.component.getApiDescription();
				if (this.component.hasApiDescription()) {
					int restrictions = RestrictionModifiers.NO_RESTRICTIONS;
					IApiAnnotations componentApiDescription = apiDescription.resolveAnnotations(this.type1.getHandle());
					if (componentApiDescription != null) {
						restrictions = componentApiDescription.getRestrictions();
						this.initialDescriptorRestrictions = restrictions;
					}
					if (restrictions2 != restrictions) {
						// report different restrictions
						// adding/removing no extend on a final class is ok
						// adding/removing no instantiate on an abstract class
						// is ok
						String NO_EXTEND = "@noextend"; //$NON-NLS-1$
						String NO_IMPLEMENT = "@noimplement"; //$NON-NLS-1$
						String NO_INSTANSTIATE = "@noinstantiate"; //$NON-NLS-1$
						if (this.type1.isInterface()) {
							boolean noImplementAdded = (RestrictionModifiers.isImplementRestriction(restrictions2) && !RestrictionModifiers.isImplementRestriction(restrictions));
							if (noImplementAdded || (RestrictionModifiers.isExtendRestriction(restrictions2) && !RestrictionModifiers.isExtendRestriction(restrictions))) {
								this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.RESTRICTIONS, restrictions2, typeAccess, typeAccess2, this.type2, this.type2.getName(), new String[] {noImplementAdded? NO_IMPLEMENT:NO_EXTEND, Util.getDescriptorName(type1) });
							}
							boolean noImplementRemoved = (!RestrictionModifiers.isImplementRestriction(restrictions2) && RestrictionModifiers.isImplementRestriction(restrictions)) ;
							if (noImplementRemoved || (!RestrictionModifiers.isExtendRestriction(restrictions2) && RestrictionModifiers.isExtendRestriction(restrictions))) {
								this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.RESTRICTIONS, restrictions2, typeAccess, typeAccess2, this.type2, this.type2.getName(), new String[] {noImplementRemoved?  NO_IMPLEMENT:NO_EXTEND, Util.getDescriptorName(type1) });
							}
						} else {
							boolean reportChangedRestrictions = false;
							if (!Flags.isFinal(typeAccess2) && !Flags.isFinal(typeAccess)) {
								if (RestrictionModifiers.isExtendRestriction(restrictions2) && !RestrictionModifiers.isExtendRestriction(restrictions)) {
									reportChangedRestrictions = true;
									this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.RESTRICTIONS,restrictions2, typeAccess, typeAccess2, this.type2, this.type2.getName(), new String[] { NO_EXTEND, Util.getDescriptorName(type1) });
								}
								if (!RestrictionModifiers.isExtendRestriction(restrictions2) && RestrictionModifiers.isExtendRestriction(restrictions)) {
									reportChangedRestrictions = true;
									this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.RESTRICTIONS,restrictions2, typeAccess, typeAccess2, this.type2, this.type2.getName(),new String[] { NO_EXTEND, Util.getDescriptorName(type1) });
								}
							}
							if (!reportChangedRestrictions && !Flags.isAbstract(typeAccess2) && !Flags.isAbstract(typeAccess)) {
								if (RestrictionModifiers.isInstantiateRestriction(restrictions2) && !RestrictionModifiers.isInstantiateRestriction(restrictions)) {
									this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.RESTRICTIONS, restrictions2, typeAccess, typeAccess2, this.type2, this.type2.getName(), new String[] { NO_INSTANSTIATE, Util.getDescriptorName(type1) });
								}
							}
							if (!reportChangedRestrictions && !Flags.isAbstract(typeAccess2)
									&& !Flags.isAbstract(typeAccess)) {
								if (!RestrictionModifiers.isInstantiateRestriction(restrictions2) && RestrictionModifiers.isInstantiateRestriction(restrictions)) {
									this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.RESTRICTIONS,restrictions2, typeAccess, typeAccess2, this.type2, this.type2.getName(),new String[] {  NO_INSTANSTIATE, Util.getDescriptorName(type1) });
								}
							}
						}
					}
				}
				this.currentDescriptorRestrictions = restrictions2;
			}
			// first make sure that we compare interface with interface, class
			// with class,
			// annotation with annotation and enum with enums
			if (Flags.isFinal(typeAccess2)) {
				this.currentDescriptorRestrictions |= RestrictionModifiers.NO_EXTEND;
			}
			if (Flags.isFinal(typeAccess)) {
				this.initialDescriptorRestrictions |= RestrictionModifiers.NO_EXTEND;
			}

			if (Flags.isDeprecated(typeAccess)) {
				if (!Flags.isDeprecated(typeAccess2)) {
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.DEPRECATION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				}
			} else if (Flags.isDeprecated(typeAccess2)) {
				this.addDelta(getElementType(this.type1), IDelta.ADDED, IDelta.DEPRECATION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
			}
			if (Flags.isProtected(typeAccess)) {
				if (Flags.isPrivate(typeAccess2) || Util.isDefault(typeAccess2)) {
					// report delta - decrease access: protected to default or
					// private
					this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.DECREASE_ACCESS, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
					return this.delta;
				} else if (Flags.isPublic(typeAccess2)) {
					// report delta - increase access: protected to public
					this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
					return this.delta;
				}
			} else if (Flags.isPublic(typeAccess) && (Flags.isProtected(typeAccess2) || Flags.isPrivate(typeAccess2) || Util.isDefault(typeAccess2))) {
				// report delta - decrease access: public to protected, default
				// or private
				this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.DECREASE_ACCESS, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				return this.delta;
			} else if (Util.isDefault(typeAccess) && (Flags.isPublic(typeAccess2) || Flags.isProtected(typeAccess2))) {
				this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				return this.delta;
			} else if (Flags.isPrivate(typeAccess) && (Util.isDefault(typeAccess2) || Flags.isPublic(typeAccess2) || Flags.isProtected(typeAccess2))) {
				this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				return this.delta;
			}

			if (Flags.isAnnotation(typeAccess)) {
				if (!Flags.isAnnotation(typeAccess2)) {
					if (Flags.isInterface(typeAccess2)) {
						// report conversion from annotation to interface
						this.addDelta(IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
								Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE) });
					} else if (Flags.isEnum(typeAccess2)) {
						// report conversion from annotation to enum
						this.addDelta(IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
								Integer.toString(IDelta.ENUM_ELEMENT_TYPE) });
					} else {
						// report conversion from annotation to class
						this.addDelta(IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
								Integer.toString(IDelta.CLASS_ELEMENT_TYPE) });
					}
					return this.delta;
				}
			} else if (Flags.isInterface(typeAccess)) {
				if (Flags.isAnnotation(typeAccess2)) {
					// conversion from interface to annotation
					this.addDelta(IDelta.INTERFACE_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
							Util.getDescriptorName(type1),
							Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE),
							Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE) });
					return this.delta;
				} else if (!Flags.isInterface(typeAccess2)) {
					if (Flags.isEnum(typeAccess2)) {
						// conversion from interface to enum
						this.addDelta(IDelta.INTERFACE_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE),
								Integer.toString(IDelta.ENUM_ELEMENT_TYPE) });
					} else {
						// conversion from interface to class
						this.addDelta(IDelta.INTERFACE_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE),
								Integer.toString(IDelta.CLASS_ELEMENT_TYPE) });
					}
					return this.delta;
				}
			} else if (Flags.isEnum(typeAccess)) {
				if (!Flags.isEnum(typeAccess2)) {
					if (Flags.isAnnotation(typeAccess2)) {
						// report conversion from enum to annotation
						this.addDelta(IDelta.ENUM_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.ENUM_ELEMENT_TYPE),
								Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE) });
					} else if (Flags.isInterface(typeAccess2)) {
						// report conversion from enum to interface
						this.addDelta(IDelta.ENUM_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.ENUM_ELEMENT_TYPE),
								Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE) });
					} else {
						// report conversion from enum to class
						this.addDelta(IDelta.ENUM_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.ENUM_ELEMENT_TYPE),
								Integer.toString(IDelta.CLASS_ELEMENT_TYPE) });
					}
					return this.delta;
				}
			} else if (!Util.isClass(typeAccess2)) {
				if (Flags.isAnnotation(typeAccess2)) {
					// report conversion from class to annotation
					this.addDelta(IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
							Util.getDescriptorName(type1),
							Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
							Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE) });
				} else if (Flags.isInterface(typeAccess2)) {
					// report conversion from class to interface
					this.addDelta(IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
							Util.getDescriptorName(type1),
							Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
							Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE) });
				} else {
					// report conversion from class to enum
					this.addDelta(IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE_CONVERSION, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), new String[] {
							Util.getDescriptorName(type1),
							Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
							Integer.toString(IDelta.ENUM_ELEMENT_TYPE) });
				}
				return this.delta;
			}

			if (Flags.isStatic(typeAccess)) {
				if (!Flags.isStatic(typeAccess2)) {
					this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.STATIC_TO_NON_STATIC, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				}
			} else if (Flags.isStatic(typeAccess2)) {
				this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.NON_STATIC_TO_STATIC, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
			}
			// check super class set
			checkSuperclass();
			// check super interfaces set
			checkSuperInterfaces();

			// checks fields
			IApiField[] fields1 = this.type1.getFields();
			IApiField[] fields2 = this.type2.getFields();
			Set<String> addedFields = new HashSet<>(fields2.length);
			for (IApiField field : fields2) {
				addedFields.add(field.getName());
			}
			for (IApiField field : fields1) {
				addedFields.remove(field.getName());
				getDeltaForField(field);
			}
			// checks remaining fields (added fields)
			for (String addedField : addedFields) {
				IApiField field = this.type2.getField(addedField);
				reportFieldAddition(field, this.type2);
			}

			// checks methods
			IApiMethod[] methods1 = this.type1.getMethods();
			IApiMethod[] methods2 = this.type2.getMethods();
			Set<IMemberDescriptor> addedMethods = new HashSet<>(methods2.length);
			for (IApiMethod method : methods2) {
				if (!method.isSynthetic()) {
					addedMethods.add(method.getHandle());
				}
			}
			for (IApiMethod method : methods1) {
				addedMethods.remove(method.getHandle());
				getDeltaForMethod(method);
			}
			// checks remaining methods (added methods)
			for (IMemberDescriptor addedMethod : addedMethods) {
				IMethodDescriptor md = (IMethodDescriptor) addedMethod;
				IApiMethod method = this.type2.getMethod(md.getName(), md.getSignature());
				reportMethodAddition(method, this.type2);
			}
			if (Flags.isAbstract(typeAccess)) {
				if (!Flags.isAbstract(typeAccess2)) {
					// report delta - changed from abstract to non-abstract
					this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.ABSTRACT_TO_NON_ABSTRACT, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				}
			} else if (Flags.isAbstract(typeAccess2)) {
				// report delta - changed from non-abstract to abstract
				if (!RestrictionModifiers.isInstantiateRestriction(initialDescriptorRestrictions)) {
					this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.NON_ABSTRACT_TO_ABSTRACT, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				}
			}

			if (Flags.isFinal(typeAccess)) {
				if (!Flags.isFinal(typeAccess2)) {
					// report delta - changed from final to non-final
					this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.FINAL_TO_NON_FINAL, this.currentDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				}
			} else if (Flags.isFinal(typeAccess2)) {
				// report delta - changed from non-final to final
				this.addDelta(getElementType(this.type1), IDelta.CHANGED, IDelta.NON_FINAL_TO_FINAL, this.initialDescriptorRestrictions, typeAccess, typeAccess2, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
			}
			// check type parameters
			String signature1 = this.type1.getGenericSignature();
			String signature2 = this.type2.getGenericSignature();
			checkGenericSignature(signature1, signature2, this.type1, this.type2);

			// check type members
			checkTypeMembers();
			return this.delta.isEmpty() ? ApiComparator.NO_DELTA : this.delta;
		} catch (CoreException e) {
			reportStatus(e);
			return null;
		}
	}

	private void getDeltaForField(IApiField field) {
		int access = field.getModifiers();
		if (Flags.isSynthetic(access)) {
			// we ignore synthetic fields
			return;
		}
		String name = field.getName();
		IApiField field2 = this.type2.getField(name);
		if (field2 == null) {
			if (Flags.isPrivate(access) || Util.isDefault(access)) {
				if (!(this.visibilityModifiers == VisibilityModifiers.API && component.hasApiDescription())) {
					// report non-API delta:
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.FIELD, this.currentDescriptorRestrictions, access, 0, this.type1, name, new String[] {
							Util.getDescriptorName(this.type1), name });
				}
			} else {
				boolean found = false;
				if (this.component2 != null) {
					if (this.type1.isInterface()) {
						Set<IApiType> interfacesSet = getInterfacesSet(this.type2);
						if (interfacesSet != null) {
							for (IApiType superTypeDescriptor : interfacesSet) {
								IApiField field3 = superTypeDescriptor.getField(name);
								if (field3 == null) {
									continue;
								} else {
									// interface method can only be public
									// method has been move up in the hierarchy
									// - report the delta and abort loop
									this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.FIELD_MOVED_UP, this.currentDescriptorRestrictions, access, field3.getModifiers(), this.type1, name, new String[] {
											Util.getDescriptorName(this.type1),
											name });
									found = true;
									break;
								}
							}
						}
					} else {
						List<IApiType> superclassList = getSuperclassList(this.type2);
						if (superclassList != null && isStatusOk()) {
							loop: for (IApiType superTypeDescriptor : superclassList) {
								IApiField field3 = superTypeDescriptor.getField(name);
								if (field3 == null) {
									continue;
								} else {
									int access3 = field3.getModifiers();
									if (Flags.isPublic(access3) || Flags.isProtected(access3)) {
										// method has been move up in the
										// hierarchy - report the delta and
										// abort loop
										this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.FIELD_MOVED_UP, this.currentDescriptorRestrictions, access, field3.getModifiers(), this.type1, name, new String[] {
												Util.getDescriptorName(this.type1),
												name });
										found = true;
										break loop;
									}
								}
							}
						}
					}
				}
				if (!found) {
					if ((this.visibilityModifiers == VisibilityModifiers.API) && component.hasApiDescription()) {
						// check if this field should be removed because it is
						// tagged as @noreference
						IApiDescription apiDescription = null;
						try {
							apiDescription = this.component.getApiDescription();
						} catch (CoreException e) {
							reportStatus(e);
						}
						if (apiDescription != null) {
							IApiAnnotations apiAnnotations = apiDescription.resolveAnnotations(field.getHandle());
							if (apiAnnotations != null) {
								int restrictions = apiAnnotations.getRestrictions();
								if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
									// if not found, but tagged as @noreference
									// in reference we don't need to report
									// a removed field
									return;
								}
							}
						}
					}
					if (field.isEnumConstant()) {
						// report delta (removal of an enum constant - not
						// compatible)
						this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.ENUM_CONSTANT, this.currentDescriptorRestrictions, this.type1.getModifiers(), this.type2.getModifiers(), this.type1, name, new String[] {
								Util.getDescriptorName(this.type1), name });
						return;
					}
					// removing a public field is a breakage
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.FIELD, this.currentDescriptorRestrictions, access, 0, this.type1, name, new String[] {
							Util.getDescriptorName(this.type1), name });
				}
			}
			return;
		}
		int restrictions = RestrictionModifiers.NO_RESTRICTIONS;
		int referenceRestrictions = RestrictionModifiers.NO_RESTRICTIONS;
		int access2 = field2.getModifiers();
		if (this.component2.hasApiDescription()) {
			try {
				IApiDescription apiDescription = this.component2.getApiDescription();
				IApiAnnotations resolvedAPIDescription = apiDescription.resolveAnnotations(field2.getHandle());
				if (resolvedAPIDescription != null) {
					restrictions = resolvedAPIDescription.getRestrictions();
				}
			} catch (CoreException e) {
				// ignore
			}
		}
		if ((this.visibilityModifiers == VisibilityModifiers.API) && component.hasApiDescription()) {
			// check if this field should be removed because it is tagged as
			// @noreference
			IApiDescription apiDescription = null;
			try {
				apiDescription = this.component.getApiDescription();
			} catch (CoreException e) {
				reportStatus(e);
			}
			if (apiDescription != null) {
				IApiAnnotations apiAnnotations = apiDescription.resolveAnnotations(field.getHandle());
				if (apiAnnotations != null) {
					referenceRestrictions = apiAnnotations.getRestrictions();
				}
			}
			if (RestrictionModifiers.isReferenceRestriction(referenceRestrictions)) {
				// tagged as @noreference in the reference component
				if (!RestrictionModifiers.isReferenceRestriction(restrictions)) {
					// no longer tagged as @noreference
					// report a field addition
					if (field2.isEnumConstant()) {
						// report delta (addition of an enum constant -
						// compatible
						this.addDelta(getElementType(this.type2), IDelta.ADDED, IDelta.ENUM_CONSTANT, this.currentDescriptorRestrictions, access, access2, this.type1, name, new String[] {
								Util.getDescriptorName(this.type2), name });
					} else {
						this.addDelta(getElementType(this.type2), IDelta.ADDED, IDelta.FIELD, this.currentDescriptorRestrictions, access, access2, this.type1, name, new String[] {
								Util.getDescriptorName(this.type2), name });
					}
					return;
				}
			} else if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
				if (((Flags.isPublic(access2) || Flags.isProtected(access2)) && (Flags.isPublic(access) || Flags.isProtected(access))) && (this.visibilityModifiers == VisibilityModifiers.API)) {
					// report that it is no longer an API field
					this.addDelta(getElementType(this.type2), IDelta.REMOVED, field2.isEnumConstant() ? IDelta.API_ENUM_CONSTANT : IDelta.API_FIELD, restrictions, access, access2, this.type1, name, new String[] {
							Util.getDescriptorName(this.type2), name });
				}
				return;
			}
			if ((Flags.isPrivate(access) || Util.isDefault(access) || (Flags.isProtected(access) && RestrictionModifiers.isExtendRestriction(this.initialDescriptorRestrictions)))
					&& (Flags.isPrivate(access2) || Util.isDefault(access2) || (Flags.isProtected(access2) && RestrictionModifiers.isExtendRestriction(this.currentDescriptorRestrictions)))) {
				// don't report non-API deltas
				return;
			}
		}

		restrictions |= this.currentDescriptorRestrictions;

		if (!field.getSignature().equals(field2.getSignature())) {
			// report delta
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TYPE, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		} else {
			// check type parameters
			String signature1 = field.getGenericSignature();
			String signature2 = field2.getGenericSignature();
			checkGenericSignature(signature1, signature2, field, field2);
		}
		boolean changeFinalToNonFinal = false;
		if (Flags.isProtected(access)) {
			if (Flags.isPrivate(access2) || Util.isDefault(access2)) {
				// report delta - decrease access: protected to default or
				// private
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.DECREASE_ACCESS, restrictions, access, access2, this.type1, name, new String[] {
						Util.getDescriptorName(this.type1), name });
			} else if (Flags.isPublic(access2)) {
				// report delta - increase access: protected to public
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.INCREASE_ACCESS, restrictions, access, access2, this.type1, name, new String[] {
						Util.getDescriptorName(this.type1), name });
			}
		} else if (Flags.isPublic(access) && (Flags.isProtected(access2) || Flags.isPrivate(access2) || Util.isDefault(access2))) {
			// report delta - decrease access: public to protected, default or
			// private
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.DECREASE_ACCESS, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		} else if (Flags.isPrivate(access) && (Flags.isProtected(access2) || Util.isDefault(access2) || Flags.isPublic(access2))) {
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.INCREASE_ACCESS, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		} else if (Util.isDefault(access) && (Flags.isProtected(access2) || Flags.isPublic(access2))) {
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.INCREASE_ACCESS, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		}
		if (Flags.isFinal(access)) {
			if (!Flags.isFinal(access2)) {
				if (!Flags.isStatic(access2)) {
					// report delta - final to non-final for a non static field
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.FINAL_TO_NON_FINAL_NON_STATIC, restrictions, access, access2, this.type1, name, new String[] {
							Util.getDescriptorName(this.type1), name });
				} else if (field.getConstantValue() != null) {
					// report delta - final to non-final for a static field with
					// a compile time constant
					changeFinalToNonFinal = true;
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, restrictions, access, access2, this.type1, name, new String[] {
							Util.getDescriptorName(this.type1), name });
				} else {
					// report delta - final to non-final for a static field with
					// no compile time constant
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT, restrictions, access, access2, this.type1, name, new String[] {
							Util.getDescriptorName(this.type1), name });
				}
			}
		} else if (Flags.isFinal(access2)) {
			// report delta - non-final to final
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.NON_FINAL_TO_FINAL, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		}
		if (Flags.isStatic(access)) {
			if (!Flags.isStatic(access2)) {
				// report delta - static to non-static
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.STATIC_TO_NON_STATIC, restrictions, access, access2, this.type1, name, new String[] {
						Util.getDescriptorName(this.type1), name });
			}
		} else if (Flags.isStatic(access2)) {
			// report delta - non-static to static
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.NON_STATIC_TO_STATIC, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		}
		if (Flags.isTransient(access)) {
			if (!Flags.isTransient(access2)) {
				// report delta - transient to non-transient
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TRANSIENT_TO_NON_TRANSIENT, restrictions, access, access2, this.type1, name, new String[] {
						Util.getDescriptorName(this.type1), name });
			}
		} else if (Flags.isTransient(access2)) {
			// report delta - non-transient to transient
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.NON_TRANSIENT_TO_TRANSIENT, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		}
		if (Flags.isVolatile(access)) {
			if (!Flags.isVolatile(access2)) {
				// report delta - volatile to non-volatile
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.VOLATILE_TO_NON_VOLATILE, restrictions, access, access2, this.type1, name, new String[] {
						Util.getDescriptorName(this.type1), name });
			}
		} else if (Flags.isVolatile(access2)) {
			// report delta - non-volatile to volatile
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.NON_VOLATILE_TO_VOLATILE, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		}
		if (Flags.isDeprecated(access)) {
			if (!Flags.isDeprecated(access2)) {
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.REMOVED, IDelta.DEPRECATION, restrictions, access, access2, this.type1, name, new String[] {
						Util.getDescriptorName(this.type1), name });
			}
		} else if (Flags.isDeprecated(access2)) {
			// report delta - non-volatile to volatile
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.ADDED, IDelta.DEPRECATION, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name });
		}
		if (field.getConstantValue() != null) {
			if (field2.getConstantValue() == null) {
				if (!changeFinalToNonFinal) {
					// report delta - removal of constant value
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.REMOVED, IDelta.VALUE, restrictions, access, access2, this.type1, name, new String[] {
							Util.getDescriptorName(this.type1), name,
							String.valueOf(field.getConstantValue()) });
				}
			} else if (!field.getConstantValue().equals(field2.getConstantValue())) {
				// report delta - modified constant value
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.VALUE, restrictions, access, access2, this.type1, name, new String[] {
						Util.getDescriptorName(this.type1), name,
						String.valueOf(field.getConstantValue()) });
			}
		} else if (field2.getConstantValue() != null) {
			// report delta
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.ADDED, IDelta.VALUE, restrictions, access, access2, this.type1, name, new String[] {
					Util.getDescriptorName(this.type1), name,
					String.valueOf(field2.getConstantValue()) });
		}
	}

	private void getDeltaForMethod(IApiMethod method) {
		int access = method.getModifiers();
		if (Flags.isSynthetic(access)) {
			// we ignore synthetic methods
			return;
		}
		String name = method.getName();
		String descriptor = method.getSignature();
		String key = getKeyForMethod(method, this.type1);
		IApiMethod method2 = this.type2.getMethod(name, descriptor);
		String methodDisplayName = getMethodDisplayName(method, this.type1);
		if (method2 == null) {
			if (method.isClassInitializer()) {
				if (!(this.visibilityModifiers == VisibilityModifiers.API && component.hasApiDescription())) {
					// report non-API delta: removal of a clinit method
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.CLINIT, this.currentDescriptorRestrictions, access, 0, this.type1, this.type1.getName(), Util.getDescriptorName(type1));
				}
				return;
			} else if (Flags.isPrivate(access) || Util.isDefault(access)) {
				if (!(this.visibilityModifiers == VisibilityModifiers.API && component.hasApiDescription())) {
					// report non-API delta:
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, getTargetType(method), Flags.isAbstract(this.type2.getModifiers()) ? this.currentDescriptorRestrictions | RestrictionModifiers.NO_INSTANTIATE : this.currentDescriptorRestrictions, access, 0, this.type1, getKeyForMethod(method, this.type1), new String[] {
							Util.getDescriptorName(this.type1), methodDisplayName });
				}
				return;
			}
			// if null we need to walk the hierarchy of descriptor2
			boolean found = false;
			if (this.component2 != null && !method.isConstructor()) {
				if (this.type1.isInterface()) {
					Set<IApiType> interfacesSet = getInterfacesSet(this.type2);
					if (interfacesSet != null && isStatusOk()) {
						for (IApiType superTypeDescriptor : interfacesSet) {
							IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
							if (method3 == null) {
								continue;
							} else {
								// interface method can only be public
								// method has been move up in the hierarchy -
								// report the delta and abort loop
								this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.METHOD_MOVED_UP, this.currentDescriptorRestrictions, access, method3.getModifiers(), this.type1, getKeyForMethod(method3, this.type1), new String[] {
										Util.getDescriptorName(this.type1),
										methodDisplayName });
								found = true;
								break;
							}
						}
					}
				} else {
					List<IApiType> superclassList = getSuperclassList(this.type2, true);
					if (superclassList != null && isStatusOk()) {
						loop: for (IApiType superTypeDescriptor : superclassList) {
							IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
							if (method3 == null) {
								continue;
							} else {
								int access3 = method3.getModifiers();
								if (Flags.isPublic(access3) || Flags.isProtected(access3)) {
									// method has been move up in the hierarchy
									// - report the delta and abort loop
									// TODO need to make the distinction between
									// methods that need to be re-implemented
									// and methods that don't
									this.addDelta(getElementType(this.type1), IDelta.REMOVED, IDelta.METHOD_MOVED_UP, this.currentDescriptorRestrictions, access, access3, this.type1, getKeyForMethod(method3, this.type1), new String[] {
											Util.getDescriptorName(this.type1),
											methodDisplayName });
									found = true;
									break loop;
								}
							}
						}
					}
				}
			}
			if (!found) {
				if ((this.visibilityModifiers == VisibilityModifiers.API) && component.hasApiDescription()) {
					// check if this method should be removed because it is
					// tagged as @noreference
					IApiDescription apiDescription = null;
					try {
						apiDescription = this.component.getApiDescription();
					} catch (CoreException e) {
						reportStatus(e);
					}
					if (apiDescription != null) {
						IApiAnnotations apiAnnotations = apiDescription.resolveAnnotations(method.getHandle());
						if (apiAnnotations != null) {
							int restrictions = apiAnnotations.getRestrictions();
							if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
								// if not found, but tagged as @noreference in
								// reference we don't need to report
								// a removed method
								return;
							}
						}
					}
				}
				if (this.type1.isAnnotation()) {
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, method.getDefaultValue() != null ? IDelta.METHOD_WITH_DEFAULT_VALUE : IDelta.METHOD_WITHOUT_DEFAULT_VALUE, this.currentDescriptorRestrictions, access, 0, this.type1, getKeyForMethod(method, this.type1), new String[] {
							Util.getDescriptorName(this.type1),
							methodDisplayName });
				} else {
					int restrictions = this.currentDescriptorRestrictions;
					if (RestrictionModifiers.isExtendRestriction(this.initialDescriptorRestrictions) && !RestrictionModifiers.isExtendRestriction(this.currentDescriptorRestrictions)) {
						restrictions = this.initialDescriptorRestrictions;
					}
					this.addDelta(getElementType(this.type1), IDelta.REMOVED, getTargetType(method), Flags.isAbstract(this.type2.getModifiers()) ? restrictions | RestrictionModifiers.NO_INSTANTIATE : restrictions, access, 0, this.type1, getKeyForMethod(method, this.type1), new String[] {
							Util.getDescriptorName(this.type1),
							methodDisplayName });
				}
			}
			return;
		}
		int restrictions = this.currentDescriptorRestrictions;
		if (component2.hasApiDescription()) {
			try {
				IApiDescription apiDescription = this.component2.getApiDescription();
				IApiAnnotations resolvedAPIDescription = apiDescription.resolveAnnotations(method2.getHandle());
				if (resolvedAPIDescription != null) {
					restrictions |= resolvedAPIDescription.getRestrictions();
				}
			} catch (CoreException e) {
				// ignore
			}
		}
		int referenceRestrictions = this.initialDescriptorRestrictions;
		int access2 = method2.getModifiers();
		if (this.component.hasApiDescription()) {
			// check if this method should be removed because it is tagged as
			// @noreference
			IApiDescription apiDescription = null;
			try {
				apiDescription = this.component.getApiDescription();
			} catch (CoreException e) {
				reportStatus(e);
			}
			if (apiDescription != null) {
				IApiAnnotations apiAnnotations = apiDescription.resolveAnnotations(method.getHandle());
				if (apiAnnotations != null) {
					referenceRestrictions |= apiAnnotations.getRestrictions();
				}
			}
		}
		if ((this.visibilityModifiers == VisibilityModifiers.API) && this.component.hasApiDescription()) {
			if (RestrictionModifiers.isReferenceRestriction(referenceRestrictions)) {
				// tagged as @noreference in the reference component
				if (!RestrictionModifiers.isReferenceRestriction(restrictions)) {
					// no longer tagged as @noreference
					// report a method addition
					if (method.isConstructor()) {
						this.addDelta(getElementType(this.type2), IDelta.ADDED, IDelta.CONSTRUCTOR, this.currentDescriptorRestrictions, access, access2, this.type1, getKeyForMethod(method, this.type2), new String[] {
								Util.getDescriptorName(this.type2),
								methodDisplayName });
					} else if (this.type2.isAnnotation()) {
						if (method.getDefaultValue() != null) {
							this.addDelta(getElementType(this.type2), IDelta.ADDED, IDelta.METHOD_WITH_DEFAULT_VALUE, this.currentDescriptorRestrictions, access, access2, this.type1, getKeyForMethod(method, this.type2), new String[] {
									Util.getDescriptorName(this.type2),
									methodDisplayName });
						} else {
							this.addDelta(getElementType(this.type2), IDelta.ADDED, IDelta.METHOD_WITHOUT_DEFAULT_VALUE, this.currentDescriptorRestrictions, access, access2, this.type1, getKeyForMethod(method, this.type2), new String[] {
									Util.getDescriptorName(this.type2),
									methodDisplayName });
						}
					} else {
						// check superclass
						// if null we need to walk the hierarchy of descriptor2
						boolean found = false;
						if (this.component2 != null) {
							if (this.type1.isInterface()) {
								Set<IApiType> interfacesSet = getInterfacesSet(this.type2);
								if (interfacesSet != null && isStatusOk()) {
									for (IApiType superTypeDescriptor : interfacesSet) {
										IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
										if (method3 == null) {
											continue;
										} else {
											// interface method can only be
											// public
											// method has been move up in the
											// hierarchy - report the delta and
											// abort loop
											found = true;
											break;
										}
									}
								}
							} else {
								List<IApiType> superclassList = getSuperclassList(this.type2, true);
								if (superclassList != null) {
									loop: for (IApiType superTypeDescriptor : superclassList) {
										IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
										if (method3 == null) {
											continue;
										} else {
											int access3 = method3.getModifiers();
											if (Flags.isPublic(access3) || Flags.isProtected(access3)) {
												// method has been move up in
												// the hierarchy - report the
												// delta and abort loop
												// TODO need to make the
												// distinction between methods
												// that need to be
												// re-implemented and methods
												// that don't
												found = true;
												break loop;
											}
										}
									}
								}
							}
						}
						this.addDelta(getElementType(this.type2), IDelta.ADDED, found ? IDelta.OVERRIDEN_METHOD : IDelta.METHOD, this.currentDescriptorRestrictions, access, access2, this.type1, getKeyForMethod(method, this.type2), new String[] {
								Util.getDescriptorName(this.type2),
								methodDisplayName });
					}
					return;
				}
			} else if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
				if (Flags.isPublic(access2) || Flags.isProtected(access2)) {
					// report that it is no longer an API method
					if (this.type2.isAnnotation()) {
						this.addDelta(getElementType(this.type2), IDelta.REMOVED, method.getDefaultValue() != null ? IDelta.API_METHOD_WITH_DEFAULT_VALUE : IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE, this.currentDescriptorRestrictions, access, access2, this.type1, getKeyForMethod(method2, this.type2), new String[] {
								Util.getDescriptorName(this.type2),
								methodDisplayName });
					} else if (Flags.isPublic(access) || Flags.isProtected(access)) {
						this.addDelta(getElementType(this.type2), IDelta.REMOVED, method.isConstructor() ? IDelta.API_CONSTRUCTOR : IDelta.API_METHOD, Flags.isAbstract(this.type2.getModifiers()) ? this.currentDescriptorRestrictions | RestrictionModifiers.NO_INSTANTIATE : this.currentDescriptorRestrictions, access, access2, this.type1, getKeyForMethod(method2, this.type2), new String[] {
								Util.getDescriptorName(this.type2),
								methodDisplayName });
					}
					return;
				}
			}
			if ((Flags.isPrivate(access) || Util.isDefault(access) || (Flags.isProtected(access) && RestrictionModifiers.isExtendRestriction(this.initialDescriptorRestrictions)))
					&& (Flags.isPrivate(access2) || Util.isDefault(access2) || (Flags.isProtected(access2) && RestrictionModifiers.isExtendRestriction(this.currentDescriptorRestrictions)))) {
				// don't report non-API deltas
				return;
			}
		}
		if (this.component.hasApiDescription() && !method.isConstructor() && !method.isClassInitializer() && !(type1.isInterface() || type1.isAnnotation())) {
			if (restrictions != referenceRestrictions) {
				if (!Flags.isFinal(access2)) {
					if (RestrictionModifiers.isOverrideRestriction(restrictions) && !RestrictionModifiers.isOverrideRestriction(referenceRestrictions)) {
						this.addDelta(getElementType(method), IDelta.ADDED, IDelta.RESTRICTIONS, restrictions, referenceRestrictions, access, access2, this.type1, getKeyForMethod(method2, this.type2), new String[] {
								Util.getDescriptorName(this.type2),
								methodDisplayName });
					}
				}
			}
		}
		String[] names1 = method.getExceptionNames();
		List<String> list1 = null;
		if (names1 != null) {
			list1 = new ArrayList<>(names1.length);
			Collections.addAll(list1, names1);
		}
		String[] names2 = method2.getExceptionNames();
		List<String> list2 = null;
		if (names2 != null) {
			list2 = new ArrayList<>(names2.length);
			Collections.addAll(list2, names2);
		}
		if (names1 != null) {
			if (names2 == null) {
				// check all exception in method descriptor to see if they are
				// checked or unchecked exceptions
				loop: for (String string : list1) {
					String exceptionName = string.replace('/', '.');
					if (isCheckedException(this.apiBaseline1, this.component, exceptionName)) {
						// report delta - removal of checked exception
						// TODO should we continue the loop for all remaining
						// exceptions
						this.addDelta(getElementType(method), IDelta.REMOVED, IDelta.CHECKED_EXCEPTION, restrictions, access, access2, this.type1, key, new String[] {
								Util.getDescriptorName(this.type1),
								methodDisplayName, exceptionName });
						break loop;
					} else {
						// report delta - removal of unchecked exception
						this.addDelta(getElementType(method), IDelta.REMOVED, IDelta.UNCHECKED_EXCEPTION, restrictions, access, access2, this.type1, key, new String[] {
								Util.getDescriptorName(this.type1),
								methodDisplayName, exceptionName });
					}
				}
			} else {
				// check if the exceptions are consistent for both descriptors
				List<String> removedExceptions = new ArrayList<>();
				for (String string : list1) {
					String exceptionName = string.replace('/', '.');
					if (!list2.remove(exceptionName)) {
						// this means that the exceptionName was not found
						// inside the new set of exceptions
						// so it has been removed
						removedExceptions.add(exceptionName);
					}
				}
				if (removedExceptions.size() != 0) {
					loop: for (String removedException : removedExceptions) {
						String exceptionName = removedException.replace('/', '.');
						if (isCheckedException(this.apiBaseline1, this.component, exceptionName)) {
							// report delta - removal of checked exception
							// TODO should we continue the loop for all
							// remaining exceptions
							this.addDelta(getElementType(method), IDelta.REMOVED, IDelta.CHECKED_EXCEPTION, restrictions, access, access2, this.type1, key, new String[] {
									Util.getDescriptorName(this.type1),
									methodDisplayName, exceptionName });
							break loop;
						} else {
							// report delta - removal of unchecked exception
							this.addDelta(getElementType(method), IDelta.REMOVED, IDelta.UNCHECKED_EXCEPTION, restrictions, access, access2, this.type1, key, new String[] {
									Util.getDescriptorName(this.type1),
									methodDisplayName, exceptionName });
						}
					}
				}
				loop: for (String string : list2) {
					String exceptionName = string.replace('/', '.');
					if (isCheckedException(this.apiBaseline2, this.component2, exceptionName)) {
						// report delta - addition of checked exception
						// TODO should we continue the loop for all remaining
						// exceptions
						this.addDelta(getElementType(method), IDelta.ADDED, IDelta.CHECKED_EXCEPTION, restrictions, access, access2, this.type1, key, new String[] {
								Util.getDescriptorName(this.type1),
								methodDisplayName, exceptionName });
						break loop;
					} else {
						// report delta - addition of unchecked exception
						this.addDelta(getElementType(method), IDelta.ADDED, IDelta.UNCHECKED_EXCEPTION, restrictions, access, access2, this.type1, key, new String[] {
								Util.getDescriptorName(this.type1),
								methodDisplayName, exceptionName });
					}
				}
			}
		} else if (names2 != null) {
			// check all exception in method descriptor to see if they are
			// checked or unchecked exceptions
			loop: for (String string : list2) {
				String exceptionName = string.replace('/', '.');
				if (isCheckedException(this.apiBaseline2, this.component2, exceptionName)) {
					// report delta - addition of checked exception
					this.addDelta(getElementType(method), IDelta.ADDED, IDelta.CHECKED_EXCEPTION, restrictions, access, access2, this.type1, key, new String[] {
							Util.getDescriptorName(this.type1),
							methodDisplayName, exceptionName });
					// TODO should we continue the loop for all remaining
					// exceptions
					break loop;
				} else {
					// report delta - addition of unchecked exception
					this.addDelta(getElementType(method), IDelta.ADDED, IDelta.UNCHECKED_EXCEPTION, restrictions, access, access2, this.type1, key, new String[] {
							Util.getDescriptorName(this.type1),
							methodDisplayName, exceptionName });
				}
			}
		}
		if (Flags.isVarargs(access)) {
			if (!Flags.isVarargs(access2)) {
				// report delta: conversion from T... to T[] - break
				// compatibility
				this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.VARARGS_TO_ARRAY, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (Flags.isVarargs(access2)) {
			// report delta: conversion from T[] to T... compatible
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.ARRAY_TO_VARARGS, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		}
		if (Flags.isProtected(access)) {
			if (Flags.isPrivate(access2) || Util.isDefault(access2)) {
				// report delta - decrease access: protected to default or
				// private
				this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.DECREASE_ACCESS, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			} else if (Flags.isPublic(access2)) {
				// report delta - increase access: protected to public
				this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.INCREASE_ACCESS, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (Flags.isPublic(access) && (Flags.isProtected(access2) || Flags.isPrivate(access2) || Util.isDefault(access2))) {
			// report delta - decrease access: public to protected, default or
			// private
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.DECREASE_ACCESS, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		} else if (Util.isDefault(access) && (Flags.isPublic(access2) || Flags.isProtected(access2))) {
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.INCREASE_ACCESS, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		} else if (Flags.isPrivate(access) && (Util.isDefault(access2) || Flags.isPublic(access2) || Flags.isProtected(access2))) {
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.INCREASE_ACCESS, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		}
		if (Flags.isAbstract(access)) {
			if (!Flags.isAbstract(access2)) {
				// report delta - changed from abstract to non-abstract
				this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.ABSTRACT_TO_NON_ABSTRACT, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (Flags.isAbstract(access2)) {
			// report delta - changed from non-abstract to abstract
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.NON_ABSTRACT_TO_ABSTRACT, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		}
		if (Flags.isFinal(access)) {
			if (!Flags.isFinal(access2)) {
				// report delta - changed from final to non-final
				this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.FINAL_TO_NON_FINAL, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (Flags.isFinal(access2)) {
			int res = restrictions;
			if (!RestrictionModifiers.isOverrideRestriction(res)) {
				if (RestrictionModifiers.isExtendRestriction(this.currentDescriptorRestrictions)) {
					res = this.currentDescriptorRestrictions;
				} else if (RestrictionModifiers.isExtendRestriction(this.initialDescriptorRestrictions)) {
					res = this.initialDescriptorRestrictions;
				}
				if (RestrictionModifiers.isOverrideRestriction(referenceRestrictions)) {
					// it is ok to remove @nooverride and add final at the same
					// time
					res = referenceRestrictions;
				}
			}
			// only report this delta is the method was visible
			this.addDelta(getElementType(method2), IDelta.CHANGED, IDelta.NON_FINAL_TO_FINAL, res, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type2),
					getMethodDisplayName(method2, this.type2) });
		}
		if (Flags.isStatic(access)) {
			if (!Flags.isStatic(access2)) {
				// report delta: change from static to non-static
				this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.STATIC_TO_NON_STATIC, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (Flags.isStatic(access2)) {
			// report delta: change from non-static to static
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.NON_STATIC_TO_STATIC, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		}
		if (Flags.isNative(access)) {
			if (!Flags.isNative(access2)) {
				// report delta: change from native to non-native
				this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.NATIVE_TO_NON_NATIVE, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (Flags.isNative(access2)) {
			// report delta: change from non-native to native
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.NON_NATIVE_TO_NATIVE, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		}
		if (Flags.isSynchronized(access)) {
			if (!Flags.isSynchronized(access2)) {
				// report delta: change from synchronized to non-synchronized
				this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.SYNCHRONIZED_TO_NON_SYNCHRONIZED, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (Flags.isSynchronized(access2)) {
			// report delta: change from non-synchronized to synchronized
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.NON_SYNCHRONIZED_TO_SYNCHRONIZED, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		}
		if (Flags.isDeprecated(access)) {
			if (!Flags.isDeprecated(access2)) {
				this.addDelta(getElementType(method), IDelta.REMOVED, IDelta.DEPRECATION, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (Flags.isDeprecated(access2)) {
			// report delta - non-volatile to volatile
			this.addDelta(getElementType(method), IDelta.ADDED, IDelta.DEPRECATION, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		}
		// check type parameters
		String signature1 = method.getGenericSignature();
		String signature2 = method2.getGenericSignature();
		checkGenericSignature(signature1, signature2, method, method2);

		if (method.getDefaultValue() == null) {
			if (method2.getDefaultValue() != null) {
				// report delta : default value has been added - compatible
				this.addDelta(getElementType(method), IDelta.ADDED, IDelta.ANNOTATION_DEFAULT_VALUE, restrictions, access, access2, this.type1, key, new String[] {
						Util.getDescriptorName(this.type1), methodDisplayName });
			}
		} else if (method2.getDefaultValue() == null) {
			// report delta : default value has been removed - incompatible
			this.addDelta(getElementType(method), IDelta.REMOVED, IDelta.ANNOTATION_DEFAULT_VALUE, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		} else if (!method.getDefaultValue().equals(method2.getDefaultValue())) {
			// report delta: default value has changed
			this.addDelta(getElementType(method), IDelta.CHANGED, IDelta.ANNOTATION_DEFAULT_VALUE, restrictions, access, access2, this.type1, key, new String[] {
					Util.getDescriptorName(this.type1), methodDisplayName });
		}
	}

	/**
	 * Returns the complete super-interface set for the given type descriptor or
	 * null, if it could not be computed
	 *
	 * @param type
	 * @return the complete super-interface set for the given descriptor, or
	 *         <code>null</code>
	 */
	private Set<IApiType> getInterfacesSet(IApiType type) {
		HashSet<IApiType> set = new HashSet<>();
		this.status = null;
		collectAllInterfaces(type, set);
		if (set.isEmpty()) {
			return null;
		}
		return set;
	}

	private String getMethodDisplayName(IApiMethod method, IApiType type) {
		String methodName = null;
		if (method.isConstructor()) {
			methodName = type.getSimpleName();
		} else {
			methodName = method.getName();
		}
		String signature = null;
		String genericSignature = method.getGenericSignature();
		if (genericSignature != null) {
			signature = genericSignature;
		} else {
			signature = method.getSignature();
		}
		return Signature.toString(signature, methodName, null, false, false);
	}

	private SignatureDescriptor getSignatureDescriptor(String signature) {
		SignatureDescriptor signatureDescriptor = new SignatureDescriptor();
		SignatureReader signatureReader = new SignatureReader(signature);
		signatureReader.accept(new SignatureDecoder(signatureDescriptor));
		return signatureDescriptor;
	}

	private List<IApiType> getSuperclassList(IApiType type) {
		return getSuperclassList(type, false);
	}

	private List<IApiType> getSuperclassList(IApiType type, boolean includeObject) {
		return getSuperclassList(type, includeObject, false);
	}

	private List<IApiType> getSuperclassList(IApiType type, boolean includeObject, boolean includePrivate) {
		IApiType superClass = type;
		this.status = null;
		String superName = superClass.getSuperclassName();
		if (Util.isJavaLangObject(superName) && !includeObject) {
			return null;
		}
		List<IApiType> list = new ArrayList<>();
		try {
			while (superName != null && (!Util.isJavaLangObject(superName) || includeObject)) {
				superClass = superClass.getSuperclass();
				int visibility = VisibilityModifiers.PRIVATE;
				IApiComponent superComponent = superClass.getApiComponent();
				IApiDescription apiDescription = superComponent.getApiDescription();
				IApiAnnotations elementDescription = apiDescription.resolveAnnotations(superClass.getHandle());
				if (elementDescription != null) {
					visibility = elementDescription.getVisibility();
				}
				if (Util.isJavaLangObject(superName) && includeObject) {
					list.add(superClass);
					break;
				}
				if (includePrivate || ((visibility & visibilityModifiers) != 0)) {
					list.add(superClass);
				}
				superName = superClass.getSuperclassName();
			}
		} catch (CoreException e) {
			reportStatus(e);
		}
		if (list.isEmpty()) {
			return null;
		}
		return list;
	}

	private void reportFieldAddition(IApiField field, IApiType type) {
		int access = field.getModifiers();
		String name = field.getName();

		if (Flags.isSynthetic(access)) {
			// we ignore synthetic fields
			return;
		}
		if ((this.visibilityModifiers == VisibilityModifiers.API) && component2.hasApiDescription()) {
			// check if this method should be removed because it is tagged as
			// @noreference
			IApiDescription apiDescription = null;
			try {
				apiDescription = this.component2.getApiDescription();
			} catch (CoreException e) {
				reportStatus(e);
			}
			if (apiDescription != null) {
				IApiAnnotations apiAnnotations = apiDescription.resolveAnnotations(field.getHandle());
				if (apiAnnotations != null) {
					int restrictions = apiAnnotations.getRestrictions();
					if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
						// such a method is not seen as an API method
						return;
					}
				}
			}
		}
		if (field.isEnumConstant()) {
			// report delta (addition of an enum constant - compatible
			this.addDelta(getElementType(type), IDelta.ADDED, IDelta.ENUM_CONSTANT, this.currentDescriptorRestrictions, this.initialDescriptorRestrictions, 0, access, this.type1, name, new String[] {
					Util.getDescriptorName(type), name });
		} else {
			if (!(this.visibilityModifiers == VisibilityModifiers.API && component.hasApiDescription()) || Flags.isPublic(access) || Flags.isProtected(access)) {
				// report non-API delta:
				this.addDelta(getElementType(type), IDelta.ADDED, IDelta.FIELD, this.currentDescriptorRestrictions, this.initialDescriptorRestrictions, 0, access, this.type1, name, new String[] {
						Util.getDescriptorName(type), name });
			}
		}
	}

	private void reportMethodAddition(IApiMethod method, IApiType type) {
		int access = method.getModifiers();
		if (method.isClassInitializer()) {
			if (!(this.visibilityModifiers == VisibilityModifiers.API && component.hasApiDescription())) {
				// report non-API delta: addition of clinit method
				this.addDelta(getElementType(type), IDelta.ADDED, IDelta.CLINIT, this.currentDescriptorRestrictions, 0, access, this.type1, type.getName(), Util.getDescriptorName(type1));
			}
			return;
		}
		if (Flags.isSynthetic(access)) {
			// we ignore synthetic method
			return;
		}
		IApiDescription apiDescription = null;
		if (((this.visibilityModifiers & VisibilityModifiers.API) != 0) && component2.hasApiDescription()) {
			// check if this method should be removed because it is tagged as
			// @noreference
			int restrictions = RestrictionModifiers.NO_RESTRICTIONS;
			try {
				apiDescription = this.component2.getApiDescription();
			} catch (CoreException e) {
				reportStatus(e);
			}
			if (apiDescription != null) {
				IApiAnnotations apiAnnotations = apiDescription.resolveAnnotations(method.getHandle());
				if (apiAnnotations != null) {
					restrictions = apiAnnotations.getRestrictions();
				}
			}
			// check if this method should be removed because it is tagged as
			// @noreference
			if (this.visibilityModifiers == VisibilityModifiers.API && RestrictionModifiers.isReferenceRestriction(restrictions)) {
				// such a method is not seen as an API method
				return;
			}
		}
		String methodDisplayName = getMethodDisplayName(method, type);
		int restrictionsForMethodAddition = this.currentDescriptorRestrictions;
		if (Flags.isFinal(this.type2.getModifiers())) {
			restrictionsForMethodAddition |= RestrictionModifiers.NO_EXTEND;
		}
		if (apiDescription != null) {
			if (this.type2.isMemberType() && Flags.isProtected(this.type2.getModifiers())) {
				// protected member - check restriction on the enclosing type
				IApiType enclosingType = this.type2;
				try {
					do {
						if (enclosingType != null) {
							final IApiAnnotations memberTypeAnnotations = apiDescription.resolveAnnotations(enclosingType.getHandle());
							if (memberTypeAnnotations != null) {
								int restrictions = memberTypeAnnotations.getRestrictions();
								if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
									restrictionsForMethodAddition |= RestrictionModifiers.NO_REFERENCE;
								}
								if (RestrictionModifiers.isExtendRestriction(restrictions)) {
									// @noextend on a class that contains a
									// protected member means that it cannot be
									// referenced
									restrictionsForMethodAddition |= RestrictionModifiers.NO_EXTEND;
									if (this.visibilityModifiers == VisibilityModifiers.API) {
										return;
									}
								}
								if (RestrictionModifiers.isImplementRestriction(restrictions)) {
									restrictionsForMethodAddition |= RestrictionModifiers.NO_IMPLEMENT;
								}
							}
						}
						enclosingType = enclosingType.getEnclosingType();
					} while (enclosingType != null);
				} catch (CoreException e) {
					reportStatus(e);
				}
			}
		}
		if (Flags.isPublic(access) || Flags.isProtected(access)) {
			if (method.isConstructor()) {
				this.addDelta(getElementType(type), IDelta.ADDED, IDelta.CONSTRUCTOR, restrictionsForMethodAddition, this.initialDescriptorRestrictions, 0, access, this.type1, getKeyForMethod(method, type), new String[] {
						Util.getDescriptorName(type), methodDisplayName });
			} else if (type.isAnnotation()) {
				if (method.getDefaultValue() != null) {
					this.addDelta(getElementType(type), IDelta.ADDED, IDelta.METHOD_WITH_DEFAULT_VALUE, restrictionsForMethodAddition, this.initialDescriptorRestrictions, 0, access, this.type1, getKeyForMethod(method, type), new String[] {
							Util.getDescriptorName(type), methodDisplayName });
				} else {
					this.addDelta(getElementType(type), IDelta.ADDED, IDelta.METHOD_WITHOUT_DEFAULT_VALUE, restrictionsForMethodAddition, this.initialDescriptorRestrictions, 0, access, this.type1, getKeyForMethod(method, type), new String[] {
							Util.getDescriptorName(type), methodDisplayName });
				}
			} else {
				// check superclass
				// if null we need to walk the hierarchy of descriptor2
				boolean found = false;
				if (this.component2 != null) {
					String name = method.getName();
					String descriptor = method.getSignature();
					if (this.type1.isInterface()) {
						Set<IApiType> interfacesSet = getInterfacesSet(this.type2);
						if (interfacesSet != null && isStatusOk()) {
							for (IApiType superTypeDescriptor : interfacesSet) {
								IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
								if (method3 == null) {
									continue;
								} else {
									// interface method can only be public
									// method has been move up in the hierarchy
									// - report the delta and abort loop
									found = true;
									break;
								}
							}
						}
					} else {
						List<IApiType> superclassList = getSuperclassList(this.type2, true);
						if (superclassList != null && isStatusOk()) {
							loop: for (IApiType superTypeDescriptor : superclassList) {
								IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
								if (method3 == null) {
									continue;
								} else {
									int access3 = method3.getModifiers();
									if (Flags.isPublic(access3) || Flags.isProtected(access3)) {
										IApiAnnotations apiAnnotations = null;
										if (apiDescription != null) {
											apiAnnotations = apiDescription.resolveAnnotations(method3.getHandle());
										}
										if (apiAnnotations != null) {
											int restrictions = apiAnnotations.getRestrictions();
											// if overriding no reference method, break the loop and report method addition
											if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
												found = false;
												break loop;
											}
										}
										// method has been move up in the
										// hierarchy - report the delta and
										// abort loop
										// TODO need to make the distinction
										// between methods that need to be
										// re-implemented and methods that don't
										found = true;
										break loop;
									}
								}
							}
						}
					}
				}
				if (!found) {
					// check if the method has been pushed down
					// if null we need to walk the hierarchy of descriptor
					if (this.component != null) {
						String name = method.getName();
						String descriptor = method.getSignature();
						if (this.type1.isInterface()) {
							Set<IApiType> interfacesSet = getInterfacesSet(this.type1);
							if (interfacesSet != null && isStatusOk()) {
								for (IApiType superTypeDescriptor : interfacesSet) {
									IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
									if (method3 == null) {
										continue;
									} else {
										// interface method can only be public
										// method has been move up in the
										// hierarchy - report the delta and
										// abort loop
										found = true;
										break;
									}
								}
							}
						} else {
							List<IApiType> superclassList = getSuperclassList(this.type1, true);
							if (superclassList != null && isStatusOk()) {
								loop: for (IApiType superTypeDescriptor : superclassList) {
									IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
									if (method3 == null) {
										continue;
									} else {
										int access3 = method3.getModifiers();
										if (Flags.isPublic(access3) || Flags.isProtected(access3)) {
											IApiAnnotations apiAnnotations = null;
											if (apiDescription != null) {
												apiAnnotations = apiDescription.resolveAnnotations(method3.getHandle());
											}
											if (apiAnnotations != null) {
												int restrictions = apiAnnotations.getRestrictions();
												// if overriding no reference
												// method, break the loop and
												// report method addition
												if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
													found = false;
													break loop;
												}

											}
											// method has been pushed down in
											// the hierarchy - report the delta
											// and abort loop
											// TODO need to make the distinction
											// between methods that need to be
											// re-implemented and methods that
											// don't
											found = true;
											break loop;
										}
									}
								}
							}
						}
					}
					boolean isOverride = false;
					if (!found) {
						if (this.component != null) {
							String name = method.getName();
							String descriptor = method.getSignature();
							// check method of interfaces.
							HashSet<IApiType> interfaces = new HashSet<>();
							collectAllInterfaces(this.type2, interfaces);
							if (!interfaces.isEmpty()) {
								for (IApiType inter : interfaces) {
									IApiMethod methodInterface = inter.getMethod(name, descriptor);
									if (methodInterface == null) {
										continue;
									} else {
										int access3 = methodInterface.getModifiers();
										if (Flags.isPublic(access3) || Flags.isProtected(access3)) {
											isOverride = true;
											break;
										}
									}
								}
							}
						}
					}
					if (isOverride) {
						this.addDelta(getElementType(type), IDelta.ADDED, isOverride ? IDelta.OVERRIDEN_METHOD : method.isDefaultMethod() ? IDelta.DEFAULT_METHOD : IDelta.METHOD, restrictionsForMethodAddition, this.initialDescriptorRestrictions, 0, method.isDefaultMethod() ? (method.getModifiers() | Flags.AccDefaultMethod) : method.getModifiers(), this.type1, getKeyForMethod(method, type), new String[] {
								Util.getDescriptorName(type),
								methodDisplayName });
					} else {
						this.addDelta(getElementType(type), IDelta.ADDED, found ? IDelta.METHOD_MOVED_DOWN : method.isDefaultMethod() ? IDelta.DEFAULT_METHOD : IDelta.METHOD, restrictionsForMethodAddition, this.initialDescriptorRestrictions, 0, method.isDefaultMethod() ? (method.getModifiers() | Flags.AccDefaultMethod) : method.getModifiers(), this.type1, getKeyForMethod(method, type), new String[] {
							Util.getDescriptorName(type), methodDisplayName });
					}

				} else {
					this.addDelta(getElementType(type), IDelta.ADDED, found ? IDelta.OVERRIDEN_METHOD : IDelta.METHOD, restrictionsForMethodAddition, this.initialDescriptorRestrictions, 0, method.getModifiers(), this.type1, getKeyForMethod(method, type), new String[] {
							Util.getDescriptorName(type), methodDisplayName });
				}
			}
		} else if (!(this.visibilityModifiers == VisibilityModifiers.API && component.hasApiDescription())) {
			// report non-API deltas for private and package-accessible methods
			// as well:
			this.addDelta(getElementType(type), IDelta.ADDED, method.isConstructor() ? IDelta.CONSTRUCTOR : IDelta.METHOD, restrictionsForMethodAddition, this.initialDescriptorRestrictions, 0, method.getModifiers(), this.type1, getKeyForMethod(method, type), new String[] {
					Util.getDescriptorName(type), methodDisplayName });
		}
	}

	private String getKeyForMethod(IApiMethod method, IApiType type) {
		StringBuilder buffer = new StringBuilder();
		if (method.isConstructor()) {
			String name = type.getName();
			int index = name.lastIndexOf('.');
			int dollarIndex = name.lastIndexOf('$');
			if (dollarIndex != -1 && type.isMemberType()) {
				buffer.append(type.getName().substring(dollarIndex + 1));
			} else {
				buffer.append(type.getName().substring(index + 1));
			}
		} else {
			buffer.append(method.getName());
		}
		String genericSignature = method.getGenericSignature();
		if (genericSignature != null) {
			buffer.append(genericSignature);
		} else {
			buffer.append(method.getSignature());
		}
		return String.valueOf(buffer);
	}

	private static boolean isAPI(int visibility, IApiType memberTypeDescriptor) {
		int access = memberTypeDescriptor.getModifiers();
		return VisibilityModifiers.isAPI(visibility) && (Flags.isPublic(access) || Flags.isProtected(access));
	}

	private IApiTypeRoot getType(String typeName, IApiComponent component, IApiBaseline baseline) throws CoreException {
		String packageName = Signatures.getPackageName(typeName);
		IApiComponent[] components = baseline.resolvePackage(component, packageName);
		if (components == null) {
			String msg = MessageFormat.format(ComparatorMessages.ClassFileComparator_1, packageName, baseline.getName(), component.getSymbolicName());
			if (ApiPlugin.DEBUG_CLASSFILE_COMPARATOR) {
				System.err.println("TYPE LOOKUP: " + msg); //$NON-NLS-1$
			}
			reportStatus(Status.error(msg));
			return null;
		}
		IApiTypeRoot result = Util.getClassFile(components, typeName);
		if (result == null) {
			String msg = MessageFormat.format(ComparatorMessages.ClassFileComparator_2, typeName, baseline.getName(), component.getSymbolicName());
			if (ApiPlugin.DEBUG_CLASSFILE_COMPARATOR) {
				System.err.println("TYPE LOOKUP: " + msg); //$NON-NLS-1$
			}
			reportStatus(Status.error(msg));
			return null;
		}
		return result;
	}

	/**
	 * Returns the delta element type code for the given type. Translates a type
	 * to interface, class, emum or annotation.
	 *
	 * @param type
	 * @return delta element type
	 */
	private int getElementType(IApiType type) {
		if (type.isAnnotation()) {
			return IDelta.ANNOTATION_ELEMENT_TYPE;
		}
		if (type.isEnum()) {
			return IDelta.ENUM_ELEMENT_TYPE;
		}
		if (type.isInterface()) {
			return IDelta.INTERFACE_ELEMENT_TYPE;
		}
		return IDelta.CLASS_ELEMENT_TYPE;
	}

	/**
	 * Returns the delta element type code for the given method. Translates a
	 * method to constructor or method.
	 *
	 * @param method
	 * @return delta element type
	 */
	private int getElementType(IApiMethod method) {
		if (method.isConstructor()) {
			return IDelta.CONSTRUCTOR_ELEMENT_TYPE;
		}
		return IDelta.METHOD_ELEMENT_TYPE;
	}

	/**
	 * Returns the delta type code for the given method when it is the target of
	 * a remove/add. Translates a method to constructor or method.
	 *
	 * @param method
	 * @return delta type
	 */
	private int getTargetType(IApiMethod method) {
		if (method.isConstructor()) {
			return IDelta.CONSTRUCTOR;
		}
		return IDelta.METHOD;
	}

	/**
	 * Translates a member to its delta element type code.
	 *
	 * @param member
	 * @return delta element type code
	 */
	private int getElementType(IApiMember member) {
		switch (member.getType()) {
			case IApiElement.TYPE:
				return getElementType((IApiType) member);
			case IApiElement.METHOD:
				return getElementType((IApiMethod) member);
			default:
				break;
		}
		return IDelta.FIELD_ELEMENT_TYPE;
	}
}
