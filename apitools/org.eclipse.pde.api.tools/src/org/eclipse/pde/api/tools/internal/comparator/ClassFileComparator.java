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
package org.eclipse.pde.api.tools.internal.comparator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.model.TypeStructureCache;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.util.ClassFileResult;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.signature.SignatureReader;

import com.ibm.icu.text.MessageFormat;

/**
 * Compares class files from the workspace to those in the default {@link IApiProfile}
 * 
 * @since 1.0.0
 */
public class ClassFileComparator {
	
	/**
	 * Constant used for controlling tracing in the class file comparator
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the class file comparator
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}

	private boolean isCheckedException(IApiProfile profile, IApiComponent apiComponent, String exceptionName) {
		if (profile == null) {
			return true;
		}
		try {
			String packageName = Util.getPackageName(exceptionName);
			ClassFileResult result = Util.getComponent(
					profile.resolvePackage(apiComponent, packageName),
					exceptionName);
			if (result != null) {
				// TODO should this be reported as a checked exception
				IApiType exception = TypeStructureCache.getTypeStructure(result.getClassFile(), result.getComponent());
				while (!Util.isJavaLangObject(exception.getName())) {
					String superName = exception.getSuperclassName();
					packageName = Util.getPackageName(superName);
					result = Util.getComponent(
							profile.resolvePackage(apiComponent, packageName),
							superName);
					if (result == null) {
						// TODO should we report this failure ?
						if (DEBUG) {
							System.err.println("CHECKED EXCEPTION LOOKUP: Could not find " + superName + " in profile " + profile.getName() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						break;
					}
					exception = TypeStructureCache.getTypeStructure(result.getClassFile(), result.getComponent());
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

	private IApiProfile apiProfile = null;
	private IApiProfile apiProfile2 = null;
	
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
	 * @param classFile the class file from the workspace to compare
	 * @param classFile2 the class file from the baseline to compare to
	 * @param component the API component from the workspace
	 * @param component2 the API component from the baseline
	 * @param apiState the workspace API profile
	 * @param apiState2 the baseline API profile
	 * @param visibilityModifiers any modifiers from the class file
	 * @throws CoreException if the contents of the specified class files cannot be acquired
	 */
	public ClassFileComparator(IClassFile classFile, IClassFile classFile2, IApiComponent component, IApiComponent component2, IApiProfile apiState, IApiProfile apiState2, int visibilityModifiers) throws CoreException {
		this.component = component;
		this.component2 = component2;
		this.type1 = TypeStructureCache.getTypeStructure(classFile, component);
		this.type2 = TypeStructureCache.getTypeStructure(classFile2, component2);
		this.apiProfile = apiState;
		this.apiProfile2 = apiState2;
		this.visibilityModifiers = visibilityModifiers;
	}

	/**
	 * Constructor
	 * @param type the {@link IApiType} from the workspace to compare
	 * @param classFile2 the class file from the baseline to compare to
	 * @param component the API component from the workspace
	 * @param component2 the API component from the baseline
	 * @param apiState the workspace API profile
	 * @param apiState2 the baseline API profile
	 * @param visibilityModifiers any modifiers from the class file
	 * @throws CoreException if the contents of the specified class file cannot be acquired
	 */
	public ClassFileComparator(IApiType type, IClassFile classFile2, IApiComponent component, IApiComponent component2, IApiProfile apiState, IApiProfile apiState2, int visibilityModifiers) throws CoreException {
		this.component = component;
		this.component2 = component2;
		this.type1 = type;
		this.type2 = TypeStructureCache.getTypeStructure(classFile2, component2);
		this.apiProfile = apiState;
		this.apiProfile2 = apiState2;
		this.visibilityModifiers = visibilityModifiers;
	}
	private void addDelta(IDelta delta) {
		this.delta.add(delta);
	}
	private void addDelta(int elementType, int kind, int flags, int restrictions, int modifiers, IApiType type, String key, String data) {
		this.addDelta(new Delta(Util.getDeltaComponentID(this.component2), elementType, kind, flags, restrictions, modifiers, type.getName(), key, data));
	}
	private void addDelta(int elementType, int kind, int flags, int restrictions, int modifiers, IApiType type, String key, String[] datas) {
		this.addDelta(new Delta(Util.getDeltaComponentID(this.component2), elementType, kind, flags, restrictions, modifiers, type.getName(), key, datas));
	}
	/**
	 * Checks if the super-class set has been change in any way compared to the baseline (grown or reduced or types changed)
	 */
	private void checkSuperclass() throws CoreException {
		// check superclass set
		List superclassList1 = getSuperclassList(this.type1);
		if(!isStatusOk()) {
			return;
		}
		Set superclassNames2 = null;
		List superclassList2 = getSuperclassList(this.type2);
		if(!isStatusOk()) {
			return;
		}
		if (superclassList2 != null) {
			superclassNames2 = new HashSet();
			Iterator iterator = superclassList2.iterator();
			while (iterator.hasNext()) {
				superclassNames2.add(((IApiType) iterator.next()).getName());
			}
		}
		if (superclassList1 == null) {
			if (superclassList2 != null) {
				// this means the direct super class of descriptor1 is java.lang.Object
				this.addDelta(
						getElementType(this.type1),
						IDelta.ADDED,
						IDelta.SUPERCLASS,
						this.currentDescriptorRestrictions,
						this.type1.getModifiers(),
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
			}
		} else if (superclassList2 == null) {
			// this means the direct super class of descriptor2 is java.lang.Object
			this.addDelta(
					getElementType(this.type1),
					IDelta.REMOVED,
					IDelta.SUPERCLASS,
					this.currentDescriptorRestrictions,
					this.type1.getModifiers(),
					this.type1,
					this.type1.getName(),
					Util.getDescriptorName(type1));
		}
		// get superclass of descriptor2
		if (superclassList1 != null && superclassList2 != null) {
			IApiType superclassType2 = (IApiType) superclassList2.get(0);
			IApiType superclassType = (IApiType) superclassList1.get(0);
			if (!superclassType.getName().equals(superclassType2.getName())) {
				if (!superclassNames2.contains(superclassType.getName())) {
					this.addDelta(
							getElementType(this.type1),
							IDelta.REMOVED,
							IDelta.SUPERCLASS,
							this.currentDescriptorRestrictions,
							this.type1.getModifiers(),
							this.type1,
							this.type1.getName(),
							Util.getDescriptorName(type1));
				} else {
					this.addDelta(
							getElementType(this.type1),
							IDelta.ADDED,
							IDelta.SUPERCLASS,
							this.currentDescriptorRestrictions,
							this.type1.getModifiers(),
							this.type1,
							this.type1.getName(),
							Util.getDescriptorName(type1));
				}
			}
		}
		// TODO check super class if they are not checked anyway
		// case where an API type inherits from a non-API type that contains public methods/fields
		if (visibilityModifiers == VisibilityModifiers.API) {
			int currentTypeVisibility = 0;
			String superTypeName = this.type1.getSuperclassName();
			String superTypeName2 = this.type2.getSuperclassName();
			if (!superTypeName.equals(superTypeName2)) {
				// we don't need to check the superclass if it has changed
				return;
			}
			ClassFileResult pair = getType(superTypeName, this.component, this.apiProfile);
			if (pair == null) return;
			IClassFile superclassType1 = pair.getClassFile();
			IApiDescription apiDescription = pair.getComponent().getApiDescription();
			IApiType superType = TypeStructureCache.getTypeStructure(pair.getClassFile(), pair.getComponent());
			IApiAnnotations superclassAnnotations = apiDescription.resolveAnnotations(superType.getHandle());
			if (superclassAnnotations != null) {
				currentTypeVisibility = superclassAnnotations.getVisibility();
				if (!VisibilityModifiers.isAPI(currentTypeVisibility)) {
					// superclass is not an API type so we need to check it for visible members
					// if this is an API, it will be checked when the supertype is checked
					final boolean ignoreProtected = RestrictionModifiers.isExtendRestriction(this.currentDescriptorRestrictions) || Util.isFinal(this.type1.getModifiers());
					IClassFile superclassType2 = this.component2.findClassFile(superTypeName);
					if (superclassType2 != null) {
						ClassFileComparator comparator = new ClassFileComparator(superclassType1, superclassType2, this.component, this.component2, this.apiProfile, this.apiProfile2, this.visibilityModifiers);
						IDelta delta2 = comparator.getDelta();
						if (delta2 != null && delta2 != ApiComparator.NO_DELTA) {
							// filter all protected members changes
							delta2.accept(new DeltaVisitor() {
								public boolean visit(IDelta delta) {
									IDelta[] children = delta.getChildren();
									if (children.length == 0) {
										// leaf node
										int modifiers = delta.getModifiers();
										if (Util.isProtected(modifiers)) {
											if (!ignoreProtected) {
												switch(delta.getElementType()) {
													case IDelta.METHOD_ELEMENT_TYPE : 
													case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
													case IDelta.FIELD_ELEMENT_TYPE :
													case IDelta.CLASS_ELEMENT_TYPE :
													case IDelta.ENUM_ELEMENT_TYPE :
														break;
													default :
														addDelta(delta);
												}
											}
										} else if (Util.isPublic(modifiers)) {
											switch(delta.getElementType()) {
												case IDelta.METHOD_ELEMENT_TYPE : 
												case IDelta.FIELD_ELEMENT_TYPE :
												case IDelta.CLASS_ELEMENT_TYPE :
												case IDelta.ENUM_ELEMENT_TYPE :
													switch(delta.getFlags()) {
														case IDelta.CONSTRUCTOR :
															break;
														default:
															addDelta(delta);
													}
											}
										}
										return false;
									}
									return true;
								}
							});
						}
					}
				}
			}
		}
	}
	/**
	 * reports problem status to the comparators' complete status
	 * @param newstatus
	 */
	protected void reportStatus(IStatus newstatus) {
		if(this.status == null) {
			String msg = MessageFormat.format(ComparatorMessages.ClassFileComparator_0, new String[] {this.type1.getName()});
			this.status = new MultiStatus(ApiPlugin.PLUGIN_ID, Status.ERROR, msg, null);
		}
		this.status.add(newstatus);
	}
	
	/**
	 * Report problem to the comparators' status
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
	 * Checks if there are any changes to the super-interface set for the current type descriptor context.
	 * A change is one of:
	 * <ul>
	 * <li>An interface has been added to the current super-interface set compared to the current baseline</li>
	 * <li>An interface has been removed from the current super-interface set compared to the current baseline</li>
	 * <li>An interface has changed (same number of interfaces, but different types) compared to the current baseline</li>
	 * </ul>
	 */
	private void checkSuperInterfaces() {
		Set superinterfacesSet1 = getInterfacesSet(this.type1);
		if(!isStatusOk()) {
			return;
		}
		Set superinterfacesSet2 = getInterfacesSet(this.type2);
		if(!isStatusOk()) {
			return;
		}
		if (superinterfacesSet1 == null) {
			if (superinterfacesSet2 != null) {
				this.addDelta(
						getElementType(this.type1),
						IDelta.CHANGED,
						IDelta.EXPANDED_SUPERINTERFACES_SET,
						this.currentDescriptorRestrictions,
						this.type1.getModifiers(),
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
			}
		} else if (superinterfacesSet2 == null) {
			this.addDelta(
					getElementType(this.type1),
					IDelta.CHANGED,
					IDelta.CONTRACTED_SUPERINTERFACES_SET,
					this.currentDescriptorRestrictions,
					this.type1.getModifiers(),
					this.type1,
					this.type1.getName(),
					Util.getDescriptorName(type1));
		} else {
			Set names2 = new HashSet();
			for (Iterator iterator = superinterfacesSet2.iterator(); iterator.hasNext();) {
				names2.add(((IApiType)iterator.next()).getName());
			}
			for (Iterator iterator = superinterfacesSet1.iterator(); iterator.hasNext();) {
				IApiType superInterfaceType = (IApiType) iterator.next();
				if (!names2.contains(superInterfaceType.getName())) {
					this.addDelta(
							getElementType(this.type1),
							IDelta.CHANGED,
							IDelta.CONTRACTED_SUPERINTERFACES_SET,
							this.currentDescriptorRestrictions,
							this.type1.getModifiers(),
							this.type1,
							this.type1.getName(),
							Util.getDescriptorName(type1));
					return;
				}
			}
			if (superinterfacesSet1.size() < superinterfacesSet2.size()) {
				this.addDelta(
						getElementType(this.type1),
						IDelta.CHANGED,
						IDelta.EXPANDED_SUPERINTERFACES_SET,
						this.currentDescriptorRestrictions,
						this.type1.getModifiers(),
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
			}
		}
	}

	private void checkTypeMembers() throws CoreException {
		IApiType[] typeMembers = this.type1.getMemberTypes();
		IApiType[] typeMembers2 = this.type2.getMemberTypes();
		List added = new ArrayList(typeMembers2.length);
		for (int i = 0; i < typeMembers2.length; i++) {
			added.add(typeMembers2[i].getName());
		}
		if (typeMembers.length > 0) {
			if (typeMembers2.length == 0) {
				loop: for (int i = 0; i < typeMembers.length; i++) {
					try {
						IApiType typeMember = typeMembers[i];
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
						if (visibilityModifiers == VisibilityModifiers.API) {
							// if the visibility is API, we only consider public and protected types
							if (Util.isDefault(typeMember.getModifiers())
										|| Util.isPrivate(typeMember.getModifiers())) {
								continue loop;
							}
						}
						this.addDelta(
								getElementType(this.type1),
								IDelta.REMOVED,
								IDelta.TYPE_MEMBER,
								this.currentDescriptorRestrictions,
								typeMember.getModifiers(),
								this.type1,
								typeMember.getName(),
								new String[] { typeMember.getName().replace('$', '.'), Util.getDeltaComponentID(component2)});
					} catch (CoreException e) {
						reportStatus(e);
					}
				}
				return;
			}
			// check removed or added type members
			List removedTypeMembers = new ArrayList();
			loop: for (int i =  0; i < typeMembers.length; i++) {
				IApiType typeMember = typeMembers[i];
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
							// we skip the class file according to their visibility
							continue loop;
						}
						IApiDescription apiDescription2 = this.component2.getApiDescription();
						IApiAnnotations memberTypeElementDescription2 = apiDescription2.resolveAnnotations(typeMember2.getHandle());
						int memberTypeVisibility2 = 0;
						if (memberTypeElementDescription2 != null) {
							memberTypeVisibility2 = memberTypeElementDescription2.getVisibility();
						}
						String deltaComponentID = Util.getDeltaComponentID(component2);
						int restrictions = memberTypeElementDescription2 != null ? memberTypeElementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS;
						if (Util.isFinal(this.type2.getModifiers())) {
							restrictions |= RestrictionModifiers.NO_EXTEND;
						}
						if (isAPI(memberTypeVisibility, typeMember) && !isAPI(memberTypeVisibility2, typeMember2)) {
							this.addDelta(
									new Delta(
											deltaComponentID,
											getElementType(typeMember),
											IDelta.CHANGED,
											IDelta.DECREASE_ACCESS,
											restrictions | this.currentDescriptorRestrictions,
											typeMember.getModifiers(),
											typeMember.getName(),
											typeMember.getName(),
											new String[] { typeMember.getName().replace('$', '.') }));
							continue;
						}
						if ((memberTypeVisibility2 & visibilityModifiers) == 0) {
							// we simply report a changed visibility
							this.addDelta(
									new Delta(
											deltaComponentID,
											getElementType(typeMember),
											IDelta.CHANGED,
											IDelta.TYPE_VISIBILITY,
											restrictions | this.currentDescriptorRestrictions,
											typeMember2.getModifiers(),
											typeMember.getName(),
											typeMember.getName(),
											new String[] { typeMember.getName().replace('$', '.') }));
						}
						if (visibilityModifiers == VisibilityModifiers.API) {
							// if the visibility is API, we only consider public and protected types
							if (Util.isDefault(typeMember2.getModifiers())
										|| Util.isPrivate(typeMember2.getModifiers())) {
								continue loop;
							}
						}
						IClassFile memberType2 = this.component2.findClassFile(typeMember.getName());
						ClassFileComparator comparator = new ClassFileComparator(typeMember, memberType2, this.component, this.component2, this.apiProfile, this.apiProfile2, this.visibilityModifiers);
						IDelta delta2 = comparator.getDelta();
						if (delta2 != null && delta2 != ApiComparator.NO_DELTA) {
							this.addDelta(delta2);
						}
					} catch (CoreException e) {
						reportStatus(e);
					}
				}
			}
			loop: for (Iterator iterator = removedTypeMembers.iterator(); iterator.hasNext();) {
				try {
					IApiType typeMember = (IApiType) iterator.next();
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
					if (visibilityModifiers == VisibilityModifiers.API) {
						// if the visibility is API, we only consider public and protected types
						if (Util.isDefault(typeMember.getModifiers())
									|| Util.isPrivate(typeMember.getModifiers())) {
							continue loop;
						}
					}
					this.addDelta(
							getElementType(this.type1),
							IDelta.REMOVED,
							IDelta.TYPE_MEMBER,
							memberTypeElementDescription != null ? memberTypeElementDescription.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
							typeMember.getModifiers(),
							this.type1,
							typeMember.getName(),
							new String[] { typeMember.getName().replace('$', '.'), Util.getDeltaComponentID(component2)});
				} catch (CoreException e) {
					reportStatus(e);
				}
			}
		}
		// report remaining types as addition
		// Report delta as a breakage
		loop: for (Iterator iterator = added.iterator(); iterator.hasNext();) {
			try {
				String name = (String) iterator.next();
				int index = name.lastIndexOf('$');
				IApiType typeMember = this.type2.getMemberType(name.substring(index+1));
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
				if (visibilityModifiers == VisibilityModifiers.API) {
					// if the visibility is API, we only consider public and protected types
					if (Util.isDefault(typeMember.getModifiers())
								|| Util.isPrivate(typeMember.getModifiers())) {
						continue loop;
					}
				}
				this.addDelta(
						getElementType(this.type1),
						IDelta.ADDED,
						IDelta.TYPE_MEMBER,
						this.currentDescriptorRestrictions,
						typeMember.getModifiers(),
						this.type1,
						typeMember.getSimpleName(),
						typeMember.getSimpleName());
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
					this.addDelta(
							getElementType(element1),
							IDelta.ADDED,
							IDelta.TYPE_PARAMETERS,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							element1.getName(),
							new String[] {getDataFor(element1, this.type1)});
				} else if (signatureDescriptor2.getTypeArguments().length != 0) {
					this.addDelta(
							getElementType(element1),
							IDelta.ADDED,
							IDelta.TYPE_ARGUMENTS,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							element1.getName(),
							new String[] {getDataFor(element1, this.type1)});
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
					this.addDelta(
							getElementType(element1),
							IDelta.REMOVED,
							IDelta.TYPE_PARAMETER,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							element1.getName(),
							new String[] {getDataFor(element1, type1), typeParameterDescriptor.name});
				}
			} else  {
				String[] typeArguments = signatureDescriptor.getTypeArguments();
				length = typeArguments.length;
				if (length != 0) {
					for (int i = 0; i < length; i++) {
						String typeArgument = typeArguments[i];
							this.addDelta(
								getElementType(element1),
								IDelta.REMOVED,
								IDelta.TYPE_ARGUMENT,
								this.currentDescriptorRestrictions,
								element1.getModifiers(),
								this.type1,
								element1.getName(),
								new String[] {getDataFor(element1, type1), typeArgument});
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
			int length2 =  typeParameterDescriptors2.length;
			int min = length;
			int max = length2;
			if (length > length2) {
				min = length2;
				max = length;
			}
			int i = 0;
			for (;i < min; i++) {
				TypeParameterDescriptor parameterDescriptor1 = typeParameterDescriptors1[i];
				TypeParameterDescriptor parameterDescriptor2 = typeParameterDescriptors2[i];
				String name = parameterDescriptor1.name;
				if (!name.equals(parameterDescriptor2.name)) {
					this.addDelta(
							IDelta.TYPE_PARAMETER_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TYPE_PARAMETER_NAME,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							name,
							new String[] {getDataFor(element1, type1), name });
				}
				if (parameterDescriptor1.classBound == null) {
					if (parameterDescriptor2.classBound != null) {
						// report delta added class bound of a type parameter
						this.addDelta(
								IDelta.TYPE_PARAMETER_ELEMENT_TYPE,
								IDelta.ADDED,
								IDelta.CLASS_BOUND,
								this.currentDescriptorRestrictions,
								element1.getModifiers(),
								this.type1,
								name,
								new String[] {getDataFor(element1, type1), name });
					}
				} else if (parameterDescriptor2.classBound == null) {
					// report delta removed class bound of a type parameter
					this.addDelta(
							IDelta.TYPE_PARAMETER_ELEMENT_TYPE,
							IDelta.REMOVED,
							IDelta.CLASS_BOUND,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							name,
							new String[] {getDataFor(element1, type1), name});
				} else if (!parameterDescriptor1.classBound.equals(parameterDescriptor2.classBound)) {
					// report delta changed class bound of a type parameter
					this.addDelta(
							IDelta.TYPE_PARAMETER_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.CLASS_BOUND,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							name,
							new String[] {getDataFor(element1, type1), name});
				}
				List interfaceBounds1 = parameterDescriptor1.interfaceBounds;
				List interfaceBounds2 = parameterDescriptor2.interfaceBounds;
				if (interfaceBounds1 == null) {
					if (interfaceBounds2 != null) {
						for (Iterator iterator = interfaceBounds2.iterator(); iterator.hasNext(); ) {
							// report delta added interface bounds
							this.addDelta(
									IDelta.TYPE_PARAMETER_ELEMENT_TYPE,
									IDelta.ADDED,
									IDelta.INTERFACE_BOUND,
									this.currentDescriptorRestrictions,
									element1.getModifiers(),
									this.type1,
									name,
									new String[] {getDataFor(element1, type1), name, (String) iterator.next()});
						}
					}
				} else if (interfaceBounds2 == null) {
					// report delta removed interface bounds
					for (Iterator iterator = interfaceBounds1.iterator(); iterator.hasNext(); ) {
						// report delta added interface bounds
						this.addDelta(
								IDelta.TYPE_PARAMETER_ELEMENT_TYPE,
								IDelta.REMOVED,
								IDelta.INTERFACE_BOUND,
								this.currentDescriptorRestrictions,
								element1.getModifiers(),
								this.type1,
								name,
								new String[] {getDataFor(element1, type1), name, (String) iterator.next()});
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
					for (;index < boundsMin; index++) {
						String currentInterfaceBound = (String) interfaceBounds1.get(index);
						if (!currentInterfaceBound.equals(interfaceBounds2.get(index))) {
							// report delta: different interface bounds (or reordered interface bound)
							this.addDelta(
									IDelta.TYPE_PARAMETER_ELEMENT_TYPE,
									IDelta.CHANGED,
									IDelta.INTERFACE_BOUND,
									this.currentDescriptorRestrictions,
									element1.getModifiers(),
									this.type1,
									name,
									new String[] {getDataFor(element1, type1), name, currentInterfaceBound});
						}
					}
					if (boundsMin != boundsMax) {
						// if max = length2 => addition of type parameter descriptor
						// if max = length => removal of type parameter descriptor
						boolean added = boundsMax == size2;
						for (; index < boundsMax; index++) {
							String currentInterfaceBound = added ? (String) interfaceBounds2.get(index) : (String) interfaceBounds1.get(index);
							this.addDelta(
									IDelta.TYPE_PARAMETER_ELEMENT_TYPE,
									added ? IDelta.ADDED : IDelta.REMOVED,
									IDelta.INTERFACE_BOUND,
									this.currentDescriptorRestrictions,
									element1.getModifiers(),
									this.type1,
									element1.getName(),
									new String[] {getDataFor(element1, type1), name, currentInterfaceBound});
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
					this.addDelta(
							getElementType(element1),
							added ? IDelta.ADDED : IDelta.REMOVED,
							IDelta.TYPE_PARAMETER,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							element1.getName(),
							new String[] {getDataFor(element1, type1), currentTypeParameter.name});
				}
			}
			if (length2 > 0 || length > 0) return;
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
			for (;i < min; i++) {
				String currentTypeArgument = typeArguments[i];
				String newTypeArgument = typeArguments2[i];
				if (!currentTypeArgument.equals(newTypeArgument)) {
					this.addDelta(
							getElementType(element1),
							IDelta.CHANGED,
							IDelta.TYPE_ARGUMENT,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							element1.getName(),
							new String[] {getDataFor(element1, type1), currentTypeArgument, newTypeArgument});
				}
			}
			if (min != max) {
				// if max = length2 => addition of type arguments
				// if max = length => removal of type arguments
				boolean added = max == length2;
				for (; i < max; i++) {
					String currentTypeArgument = added ? typeArguments2[i] : typeArguments[i];
					this.addDelta(
							getElementType(element1),
							added ? IDelta.ADDED : IDelta.REMOVED,
							IDelta.TYPE_ARGUMENT,
							this.currentDescriptorRestrictions,
							element1.getModifiers(),
							this.type1,
							element1.getName(),
							new String[] {getDataFor(element1, type1), currentTypeArgument});
				}
			}
		}
	}

	/**
	 * Recursively collects all of the super-interfaces of the given type descriptor within the scope of 
	 * the given API component
	 * @param type
	 * @param apiComponent
	 * @param profile
	 * @param set
	 */
	private void collectAllInterfaces(IApiType type, Set set) {
		try {
			IApiType[] interfaces = type.getSuperInterfaces();
			if (interfaces != null) {
				for (int i = 0; i < interfaces.length; i++) {
					IApiType anInterface = interfaces[i];
					int visibility = VisibilityModifiers.PRIVATE;
					IApiComponent ifaceComponent = anInterface.getApiComponent();
					if (ifaceComponent.hasApiDescription()) {
						IApiDescription apiDescription = ifaceComponent.getApiDescription();
						IApiAnnotations elementDescription = apiDescription.resolveAnnotations(anInterface.getHandle());
						if (elementDescription != null) {
							visibility = elementDescription.getVisibility();
						}
					} else if (Util.isPublic(anInterface.getModifiers()) || Util.isProtected(anInterface.getModifiers())) {
						visibility = VisibilityModifiers.API;
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
		}
		catch(CoreException e) {
			reportStatus(e);
		}
	}
	
	private String getDataFor(IApiMember member, IApiType type) {
		switch(member.getElementType()) {
			case IApiMember.T_TYPE:
				if (((IApiType)member).isMemberType()) {
					return member.getName().replace('$', '.');
				}
				return member.getName();
			case IApiMember.T_METHOD:
				StringBuffer buffer = new StringBuffer();
				buffer.append(type.getName()).append('.').append(getMethodDisplayName((IApiMethod) member, type));
				return String.valueOf(buffer);
			case IApiMember.T_FIELD:
				buffer = new StringBuffer();
				buffer.append(type.getName()).append('.').append(member.getName());
				return String.valueOf(buffer);
		}
		return null;
	}

	/**
	 * Returns a new {@link Delta} to use, and resets the status of creating a delta
	 * @return
	 */
	private Delta createDelta() {
		return new Delta();
	}
	
	/**
	 * Returns the change(s) between the type descriptor and its equivalent in the current baseline.
	 * @return the changes in the type descriptor or <code>null</code>
	 */
	public IDelta getDelta() {
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
						// adding/removing no instantiate on an abstract class is ok
						if (this.type1.isInterface()) {
							if (RestrictionModifiers.isImplementRestriction(restrictions2)
									&& !RestrictionModifiers.isImplementRestriction(restrictions)) {
							this.addDelta(
									getElementType(this.type1),
									IDelta.ADDED,
									IDelta.RESTRICTIONS,
									restrictions2,
									typeAccess,
									this.type1,
									this.type1.getName(),
									Util.getDescriptorName(type1));
							}
						} else {
							boolean reportChangedRestrictions = false;
							if (!Util.isFinal(typeAccess2) && !Util.isFinal(typeAccess)) {
								if (RestrictionModifiers.isExtendRestriction(restrictions2)
										&& !RestrictionModifiers.isExtendRestriction(restrictions)) {
									reportChangedRestrictions = true;
									this.addDelta(
											getElementType(this.type1),
											IDelta.ADDED,
											IDelta.RESTRICTIONS,
											restrictions2,
											typeAccess,
											this.type1,
											this.type1.getName(),
											Util.getDescriptorName(type1));
								}
							}
							if (!reportChangedRestrictions
									&& !Util.isAbstract(typeAccess2)
									&& !Util.isAbstract(typeAccess)) {
								if (RestrictionModifiers.isInstantiateRestriction(restrictions2)
											&& !RestrictionModifiers.isInstantiateRestriction(restrictions)) {
									this.addDelta(
											getElementType(this.type1),
											IDelta.ADDED,
											IDelta.RESTRICTIONS,
											restrictions2,
											typeAccess,
											this.type1,
											this.type1.getName(),
											Util.getDescriptorName(type1));
								}
							}
						}
					}
				}
				this.currentDescriptorRestrictions = restrictions2;
			}
			// first make sure that we compare interface with interface, class with class,
			// annotation with annotation and enum with enums
			if (Util.isFinal(typeAccess2)) {
				this.currentDescriptorRestrictions |= RestrictionModifiers.NO_EXTEND;
			}
			if (Util.isFinal(typeAccess)) {
				this.initialDescriptorRestrictions |= RestrictionModifiers.NO_EXTEND;
			}

			if (Util.isProtected(typeAccess)) {
				if (Util.isPrivate(typeAccess2) || Util.isDefault(typeAccess2)) {
					// report delta - decrease access: protected to default or private
					this.addDelta(
							getElementType(this.type1),
							IDelta.CHANGED,
							IDelta.DECREASE_ACCESS,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.type1,
							this.type1.getName(),
							Util.getDescriptorName(type1));
					return this.delta;
				} else if (Util.isPublic(typeAccess2)) {
					// report delta - increase access: protected to public
					this.addDelta(
							getElementType(this.type1),
							IDelta.CHANGED,
							IDelta.INCREASE_ACCESS,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.type1,
							this.type1.getName(),
							Util.getDescriptorName(type1));
					return this.delta;
				}
			} else if (Util.isPublic(typeAccess)
					&& (Util.isProtected(typeAccess2)
							|| Util.isPrivate(typeAccess2)
							|| Util.isDefault(typeAccess2))) {
				// report delta - decrease access: public to protected, default or private
				this.addDelta(
						getElementType(this.type1),
						IDelta.CHANGED,
						IDelta.DECREASE_ACCESS,
						this.currentDescriptorRestrictions,
						typeAccess2,
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
				return this.delta;
			} else if (Util.isDefault(typeAccess)
					&& (Util.isPublic(typeAccess2)
							|| Util.isProtected(typeAccess2))) {
				this.addDelta(
						getElementType(this.type1),
						IDelta.CHANGED,
						IDelta.INCREASE_ACCESS,
						this.currentDescriptorRestrictions,
						typeAccess2,
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
				return this.delta;
			} else if (Util.isPrivate(typeAccess)
					&& (Util.isDefault(typeAccess2)
							|| Util.isPublic(typeAccess2)
							|| Util.isProtected(typeAccess2))) {
				this.addDelta(
						getElementType(this.type1),
						IDelta.CHANGED,
						IDelta.INCREASE_ACCESS,
						this.currentDescriptorRestrictions,
						typeAccess2,
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
				return this.delta;
			}
			
			if (Util.isAnnotation(typeAccess)) {
				if (!Util.isAnnotation(typeAccess2)) {
					if (Util.isInterface(typeAccess2)) {
						// report conversion from annotation to interface
						this.addDelta(
								IDelta.ANNOTATION_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TYPE_CONVERSION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.type1,
								this.type1.getName(),
								new String[] {
										Util.getDescriptorName(type1),
										Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
										Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE)
									});
					} else if (Util.isEnum(typeAccess2)) {
						// report conversion from annotation to enum
						this.addDelta(
								IDelta.ANNOTATION_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TYPE_CONVERSION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.type1,
								this.type1.getName(),
								new String[] {
										Util.getDescriptorName(type1),
										Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
										Integer.toString(IDelta.ENUM_ELEMENT_TYPE)
								});
					} else {
						// report conversion from annotation to class
						this.addDelta(
								IDelta.ANNOTATION_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TYPE_CONVERSION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.type1,
								this.type1.getName(),
								new String[] {
										Util.getDescriptorName(type1),
										Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE),
										Integer.toString(IDelta.CLASS_ELEMENT_TYPE)
								});
					}
					return this.delta;
				}
			} else if (Util.isInterface(typeAccess)) {
				if (Util.isAnnotation(typeAccess2)) {
					// conversion from interface to annotation
					this.addDelta(
							IDelta.INTERFACE_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TYPE_CONVERSION,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.type1,
							this.type1.getName(),
							new String[] {
								Util.getDescriptorName(type1),
								Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE),
								Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE)
							});
					return this.delta;
				} else if (!Util.isInterface(typeAccess2)) {
					if (Util.isEnum(typeAccess2)) {
						// conversion from interface to enum
						this.addDelta(
								IDelta.INTERFACE_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TYPE_CONVERSION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.type1,
								this.type1.getName(),
								new String[] {
										Util.getDescriptorName(type1),
										Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE),
										Integer.toString(IDelta.ENUM_ELEMENT_TYPE)
								});
					} else {
						// conversion from interface to class
						this.addDelta(
								IDelta.INTERFACE_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TYPE_CONVERSION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.type1,
								this.type1.getName(),
								new String[] {
										Util.getDescriptorName(type1),
										Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE),
										Integer.toString(IDelta.CLASS_ELEMENT_TYPE)
								});
					}
					return this.delta;
				}
			} else if (Util.isEnum(typeAccess)) {
				if (!Util.isEnum(typeAccess2)) {
					if (Util.isAnnotation(typeAccess2)) {
						// report conversion from enum to annotation
						this.addDelta(
								IDelta.ENUM_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TYPE_CONVERSION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.type1,
								this.type1.getName(),
								new String[] {
										Util.getDescriptorName(type1),
										Integer.toString(IDelta.ENUM_ELEMENT_TYPE),
										Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE)
								});
					} else if (Util.isInterface(typeAccess2)) {
						// report conversion from enum to interface
						this.addDelta(
								IDelta.ENUM_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TYPE_CONVERSION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.type1,
								this.type1.getName(),
								new String[] {
										Util.getDescriptorName(type1),
										Integer.toString(IDelta.ENUM_ELEMENT_TYPE),
										Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE)
								});
					} else {
						// report conversion from enum to class
						this.addDelta(
								IDelta.ENUM_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TYPE_CONVERSION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.type1,
								this.type1.getName(),
								new String[] {
										Util.getDescriptorName(type1),
										Integer.toString(IDelta.ENUM_ELEMENT_TYPE),
										Integer.toString(IDelta.CLASS_ELEMENT_TYPE)
								});
					}
					return this.delta;
				}
			} else if (!Util.isClass(typeAccess2)) {
				if (Util.isAnnotation(typeAccess2)) {
					// report conversion from class to annotation
					this.addDelta(
							IDelta.CLASS_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TYPE_CONVERSION,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.type1,
							this.type1.getName(),
							new String[] {
									Util.getDescriptorName(type1),
									Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
									Integer.toString(IDelta.ANNOTATION_ELEMENT_TYPE)
							});
				} else if (Util.isInterface(typeAccess2)) {
					// report conversion from class to interface
					this.addDelta(
							IDelta.CLASS_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TYPE_CONVERSION,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.type1,
							this.type1.getName(),
							new String[] {
									Util.getDescriptorName(type1),
									Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
									Integer.toString(IDelta.INTERFACE_ELEMENT_TYPE)
							});
				} else {
					// report conversion from class to enum
					this.addDelta(
							IDelta.CLASS_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TYPE_CONVERSION,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.type1,
							this.type1.getName(),
							new String[] {
									Util.getDescriptorName(type1),
									Integer.toString(IDelta.CLASS_ELEMENT_TYPE),
									Integer.toString(IDelta.ENUM_ELEMENT_TYPE)
							});
				}
				return this.delta;
			}

			if (Util.isStatic(typeAccess)) {
				if (!Util.isStatic(typeAccess2)) {
					this.addDelta(
							getElementType(this.type1),
							IDelta.CHANGED,
							IDelta.STATIC_TO_NON_STATIC,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.type1,
							this.type1.getName(),
							Util.getDescriptorName(type1));
				}
			} else if (Util.isStatic(typeAccess2)) {
				this.addDelta(
						getElementType(this.type1),
						IDelta.CHANGED,
						IDelta.NON_STATIC_TO_STATIC,
						this.currentDescriptorRestrictions,
						typeAccess2,
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
			}
			// check super class set
			checkSuperclass();
			// check super interfaces set
			checkSuperInterfaces();
			
			// checks fields
			IApiField[] fields1 = this.type1.getFields();
			IApiField[] fields2 = this.type2.getFields();
			Set addedFields = new HashSet(fields2.length);
			for (int i = 0; i < fields2.length; i++) {
				addedFields.add(fields2[i].getName());
			}
			for (int i = 0; i < fields1.length; i++) {
				addedFields.remove(fields1[i].getName());
				getDeltaForField(fields1[i]);
			}
			// checks remaining fields (added fields)
			for (Iterator iterator = addedFields.iterator(); iterator.hasNext();) {
				IApiField field = this.type2.getField((String) iterator.next());
				reportFieldAddition(field, this.type2);
			}
			
			// checks methods
			IApiMethod[] methods1 = this.type1.getMethods();
			IApiMethod[] methods2 = this.type2.getMethods();
			Set addedMethods = new HashSet(methods2.length);
			for (int i = 0; i < methods2.length; i++) {
				if(!methods2[i].isSynthetic()) {
					addedMethods.add(methods2[i].getHandle());
				}
			}
			for (int i = 0; i < methods1.length; i++) {
				addedMethods.remove(methods1[i].getHandle());
				getDeltaForMethod(methods1[i]);
			}
			// checks remaining methods (added methods)
			for (Iterator iterator = addedMethods.iterator(); iterator.hasNext();) {
				IMethodDescriptor md = (IMethodDescriptor) iterator.next();
				IApiMethod method = this.type2.getMethod(md.getName(), md.getSignature());
				reportMethodAddition(method, this.type2);
			}
			if (Util.isAbstract(typeAccess)) {
				if (!Util.isAbstract(typeAccess2)) {
					// report delta - changed from abstract to non-abstract
					this.addDelta(
							getElementType(this.type1),
							IDelta.CHANGED,
							IDelta.ABSTRACT_TO_NON_ABSTRACT,
							this.currentDescriptorRestrictions,
							typeAccess,
							this.type1,
							this.type1.getName(),
							Util.getDescriptorName(type1));
				}
			} else if (Util.isAbstract(typeAccess2)){
				// report delta - changed from non-abstract to abstract
				this.addDelta(
						getElementType(this.type1),
						IDelta.CHANGED,
						IDelta.NON_ABSTRACT_TO_ABSTRACT,
						this.currentDescriptorRestrictions,
						typeAccess,
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
			}

			if (Util.isFinal(typeAccess)) {
				if (!Util.isFinal(typeAccess2)) {
					// report delta - changed from final to non-final
					this.addDelta(
							getElementType(this.type1),
							IDelta.CHANGED,
							IDelta.FINAL_TO_NON_FINAL,
							this.currentDescriptorRestrictions,
							typeAccess,
							this.type1,
							this.type1.getName(),
							Util.getDescriptorName(type1));
				}
			} else if (Util.isFinal(typeAccess2)){
				// report delta - changed from non-final to final
				this.addDelta(
						getElementType(this.type1),
						IDelta.CHANGED,
						IDelta.NON_FINAL_TO_FINAL,
						this.initialDescriptorRestrictions,
						typeAccess,
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
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
		if (Util.isSynthetic(access)) {
			// we ignore synthetic fields
			return;
		}
		String name = field.getName();
		IApiField field2 = this.type2.getField(name);
		if (field2 == null) {
			if (Util.isPrivate(access)
					|| Util.isDefault(access)) {
				this.addDelta(
						getElementType(this.type1),
						IDelta.REMOVED,
						IDelta.FIELD,
						this.currentDescriptorRestrictions,
						access,
						this.type1,
						name,
						new String[] {Util.getDescriptorName(this.type1), name});
			} else {
				boolean found = false;
				if (this.component2 != null) {
					if (this.type1.isInterface()) {
						Set interfacesSet = getInterfacesSet(this.type2);
						if (interfacesSet != null) {
							for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
								IApiType superTypeDescriptor = (IApiType) iterator.next();
								IApiField field3 = superTypeDescriptor.getField(name);
								if (field3 == null) {
									continue;
								} else {
									// interface method can only be public
									// method has been move up in the hierarchy - report the delta and abort loop
									this.addDelta(
											getElementType(this.type1),
											IDelta.REMOVED,
											IDelta.FIELD_MOVED_UP,
											this.currentDescriptorRestrictions,
											field.getModifiers(),
											this.type1,
											name,
											new String[] {Util.getDescriptorName(this.type1), name});
									found = true;
									break;
								}
							}
						}
					} else {
						List superclassList = getSuperclassList(this.type2);
						if (superclassList != null && isStatusOk()) {
							loop: for (Iterator iterator = superclassList.iterator(); iterator.hasNext();) {
								IApiType superTypeDescriptor = (IApiType) iterator.next();
								IApiField field3 = superTypeDescriptor.getField(name);
								if (field3 == null) {
									continue;
								} else {
									int access3 = field3.getModifiers();
									if (Util.isPublic(access3)
											|| Util.isProtected(access3)) {
										// method has been move up in the hierarchy - report the delta and abort loop
										this.addDelta(
												getElementType(this.type1),
												IDelta.REMOVED,
												IDelta.FIELD_MOVED_UP,
												this.currentDescriptorRestrictions,
												field3.getModifiers(),
												this.type1,
												name,
												new String[] {Util.getDescriptorName(this.type1), name});
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
						// check if this method should be removed because it is tagged as @noreference
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
									// if not found, but tagged as @noreference in reference we don't need to report 
									// a removed field
									return;
								}
							}
						}
					}
					if (field.isEnumConstant()) {
						// report delta (removal of an enum constant - not compatible)
						this.addDelta(
								getElementType(this.type1),
								IDelta.REMOVED,
								IDelta.ENUM_CONSTANT,
								this.currentDescriptorRestrictions,
								this.type2.getModifiers(),
								this.type1,
								name,
								new String[] {Util.getDescriptorName(this.type1), name});
						return;
					}
					// removing a public field is a breakage
					this.addDelta(
							getElementType(this.type1),
							IDelta.REMOVED,
							IDelta.FIELD,
							this.currentDescriptorRestrictions,
							field.getModifiers(),
							this.type1,
							name,
							new String[] {Util.getDescriptorName(this.type1), name});
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
		if (this.visibilityModifiers == VisibilityModifiers.API && component.hasApiDescription()) {
			// check if this method should be removed because it is tagged as @noreference
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
						// report delta (addition of an enum constant - compatible
						this.addDelta(
								getElementType(this.type2),
								IDelta.ADDED,
								IDelta.ENUM_CONSTANT,
								this.currentDescriptorRestrictions,
								access2,
								this.type1,
								name,
								new String[] {Util.getDescriptorName(this.type2), name});
					} else {
						this.addDelta(
								getElementType(this.type2),
								IDelta.ADDED,
								IDelta.FIELD,
								this.currentDescriptorRestrictions,
								access2,
								this.type1,
								name,
								new String[] {Util.getDescriptorName(this.type2), name});
					}
					return;
				}
			} else if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
				if (((Util.isPublic(access2) || Util.isProtected(access2)) && (Util.isPublic(access) || Util.isProtected(access)))
						&& visibilityModifiers == VisibilityModifiers.API) {
					// report that it is no longer an API field
					this.addDelta(
							getElementType(this.type2),
							IDelta.REMOVED,
							field2.isEnumConstant() ? IDelta.API_ENUM_CONSTANT : IDelta.API_FIELD,
							restrictions,
							access2,
							this.type1,
							name,
							new String[] {Util.getDescriptorName(this.type2), name});
				}
				return;
			}
		}

		restrictions |= this.currentDescriptorRestrictions;

		if (!field.getSignature().equals(field2.getSignature())) {
			// report delta
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.TYPE,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name});
		} else {
			// check type parameters
			String signature1 = field.getGenericSignature();
			String signature2 = field2.getGenericSignature();
			checkGenericSignature(signature1, signature2, field, field2);
		}
		boolean changeFinalToNonFinal = false;
		if (Util.isProtected(access)) {
			if (Util.isPrivate(access2) || Util.isDefault(access2)) {
				// report delta - decrease access: protected to default or private
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.DECREASE_ACCESS,
						restrictions,
						access2,
						this.type1,
						name,
						new String[] {Util.getDescriptorName(this.type1), name});
			} else if (Util.isPublic(access2)) {
				// report delta - increase access: protected to public
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.INCREASE_ACCESS,
						restrictions,
						access2,
						this.type1,
						name,
						new String[] {Util.getDescriptorName(this.type1), name});
			}
		} else if (Util.isPublic(access)
				&& (Util.isProtected(access2)
						|| Util.isPrivate(access2)
						|| Util.isDefault(access2))) {
			// report delta - decrease access: public to protected, default or private
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.DECREASE_ACCESS,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name});
		} else if (Util.isPrivate(access)
				&& (Util.isProtected(access2)
						|| Util.isDefault(access2)
						|| Util.isPublic(access2))) {
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.INCREASE_ACCESS,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name});
		} else if (Util.isDefault(access)
				&& (Util.isProtected(access2)
						|| Util.isPublic(access2))) {
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.INCREASE_ACCESS,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name});
		}
		if (Util.isFinal(access)) {
			if (!Util.isFinal(access2)) {
				if (!Util.isStatic(access2)) {
					// report delta - final to non-final for a non static field
					this.addDelta(
							IDelta.FIELD_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.FINAL_TO_NON_FINAL_NON_STATIC,
							restrictions,
							access2,
							this.type1,
							name,
							new String[] {Util.getDescriptorName(this.type1), name});
				} else if (field.getConstantValue() != null) {
					// report delta - final to non-final for a static field with a compile time constant
					changeFinalToNonFinal = true;
					this.addDelta(
							IDelta.FIELD_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT,
							restrictions,
							access2,
							this.type1,
							name,
							new String[] {Util.getDescriptorName(this.type1), name});
				} else {
					// report delta - final to non-final for a static field with no compile time constant
					this.addDelta(
							IDelta.FIELD_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT,
							restrictions,
							access2,
							this.type1,
							name,
							new String[] {Util.getDescriptorName(this.type1), name});
				}
			}
		} else if (Util.isFinal(access2)) {
			// report delta - non-final to final
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.NON_FINAL_TO_FINAL,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name});
		}
		if (Util.isStatic(access)) {
			if (!Util.isStatic(access2)) {
				// report delta - static to non-static
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.STATIC_TO_NON_STATIC,
						restrictions,
						access2,
						this.type1,
						name,
						new String[] {Util.getDescriptorName(this.type1), name});
			}
		} else if (Util.isStatic(access2)) {
			// report delta - non-static to static
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.NON_STATIC_TO_STATIC,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name});
		}
		if (Util.isTransient(access)) {
			if (!Util.isTransient(access2)) {
				// report delta - transient to non-transient
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.TRANSIENT_TO_NON_TRANSIENT,
						restrictions,
						access2,
						this.type1,
						name,
						new String[] {Util.getDescriptorName(this.type1), name});
			}
		} else if (Util.isTransient(access2)) {
			// report delta - non-transient to transient
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.NON_TRANSIENT_TO_TRANSIENT,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name});
		}
		if (Util.isVolatile(access)) {
			if (!Util.isVolatile(access2)) {
				// report delta - volatile to non-volatile
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.VOLATILE_TO_NON_VOLATILE,
						restrictions,
						access2,
						this.type1,
						name,
						new String[] {Util.getDescriptorName(this.type1), name});
			}
		} else if (Util.isVolatile(access2)) {
			// report delta - non-volatile to volatile
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.NON_VOLATILE_TO_VOLATILE,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name});
		}
		if (field.getConstantValue() != null) {
			if (field2.getConstantValue() == null) {
				if (!changeFinalToNonFinal) {
					// report delta - removal of constant value
					this.addDelta(
							IDelta.FIELD_ELEMENT_TYPE,
							IDelta.REMOVED,
							IDelta.VALUE,
							restrictions,
							access2,
							this.type1,
							name,
							new String[] {Util.getDescriptorName(this.type1), name, String.valueOf(field.getConstantValue())});
				}
			} else if (!field.getConstantValue().equals(field2.getConstantValue())) {
				// report delta - modified constant value
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.VALUE,
						restrictions,
						access2,
						this.type1,
						name,
						new String[] {Util.getDescriptorName(this.type1), name, String.valueOf(field.getConstantValue())});
			}
		} else if (field2.getConstantValue() != null) {
			// report delta
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.ADDED,
					IDelta.VALUE,
					restrictions,
					access2,
					this.type1,
					name,
					new String[] {Util.getDescriptorName(this.type1), name, String.valueOf(field2.getConstantValue())});
		}
	}
	private void getDeltaForMethod(IApiMethod method) {
		int access = method.getModifiers();
		if (Util.isSynthetic(access)) {
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
				// report delta: removal of a clinit method
				this.addDelta(
						getElementType(this.type1),
						IDelta.REMOVED,
						IDelta.CLINIT,
						this.currentDescriptorRestrictions,
						access,
						this.type1,
						this.type1.getName(),
						Util.getDescriptorName(type1));
				return;
			} else if (Util.isPrivate(access)
					|| Util.isDefault(access)) {
				this.addDelta(
						getElementType(this.type1),
						IDelta.REMOVED,
						getTargetType(method),
						Util.isAbstract(this.type2.getModifiers()) ? this.currentDescriptorRestrictions | RestrictionModifiers.NO_INSTANTIATE : this.currentDescriptorRestrictions,
						access,
						this.type1,
						this.type1.getName(),
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
				return;
			}
			// if null we need to walk the hierarchy of descriptor2
			boolean found = false;
			if (this.component2 != null && !method.isConstructor()) {
				if (this.type1.isInterface()) {
					Set interfacesSet = getInterfacesSet(this.type2);
					if (interfacesSet != null && isStatusOk()) {
						for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
							IApiType superTypeDescriptor = (IApiType) iterator.next();
							IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
							if (method3 == null) {
								continue;
							} else {
								// interface method can only be public
								// method has been move up in the hierarchy - report the delta and abort loop
								this.addDelta(
										getElementType(this.type1),
										IDelta.REMOVED,
										IDelta.METHOD_MOVED_UP,
										this.currentDescriptorRestrictions,
										access,
										this.type1,
										this.type1.getName(),
										new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
								found = true;
								break;
							}
						}
					}
				} else {
					List superclassList = getSuperclassList(this.type2, true);
					if (superclassList != null && isStatusOk()) {
						loop: for (Iterator iterator = superclassList.iterator(); iterator.hasNext();) {
							IApiType superTypeDescriptor = (IApiType) iterator.next();
							IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
							if (method3 == null) {
								continue;
							} else {
								int access3 = method3.getModifiers();
								if (Util.isPublic(access3)
										|| Util.isProtected(access3)) {
									// method has been move up in the hierarchy - report the delta and abort loop
									// TODO need to make the distinction between methods that need to be re-implemented and methods that don't
									this.addDelta(
											getElementType(this.type1),
											IDelta.REMOVED,
											IDelta.METHOD_MOVED_UP,
											this.currentDescriptorRestrictions,
											access,
											this.type1,
											this.type1.getName(),
											new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
									found = true;
									break loop;
								}
							}
						}
					}
				}
			}
			if (!found) {
				if (this.visibilityModifiers == VisibilityModifiers.API && component.hasApiDescription()) {
					// check if this method should be removed because it is tagged as @noreference
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
								// if not found, but tagged as @noreference in reference we don't need to report 
								// a removed method
								return;
							}
						}
					}
				}
				if (this.type1.isAnnotation()) {
					this.addDelta(
							getElementType(this.type1),
							IDelta.REMOVED,
							method.getDefaultValue() != null ? IDelta.METHOD_WITH_DEFAULT_VALUE : IDelta.METHOD_WITHOUT_DEFAULT_VALUE,
							this.currentDescriptorRestrictions,
							method.getModifiers(),
							this.type1,
							this.type1.getName(),
							new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
				} else {
					this.addDelta(
							getElementType(this.type1),
							IDelta.REMOVED,
							getTargetType(method),
							Util.isAbstract(this.type2.getModifiers()) ? this.currentDescriptorRestrictions | RestrictionModifiers.NO_INSTANTIATE : this.currentDescriptorRestrictions,
							method.getModifiers(),
							this.type1,
							this.type1.getName(),
							new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
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
		if (component.hasApiDescription()) {
			// check if this method should be removed because it is tagged as @noreference
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
		if (this.visibilityModifiers == VisibilityModifiers.API) {
			if (RestrictionModifiers.isReferenceRestriction(referenceRestrictions)) {
				// tagged as @noreference in the reference component
				if (!RestrictionModifiers.isReferenceRestriction(restrictions)) {
					// no longer tagged as @noreference
					// report a method addition
					if (method.isConstructor()) {
						this.addDelta(
								getElementType(this.type2),
								IDelta.ADDED,
								IDelta.CONSTRUCTOR,
								this.currentDescriptorRestrictions,
								access2,
								this.type1,
								getKeyForMethod(method, this.type2),
								new String[] {Util.getDescriptorName(this.type2), methodDisplayName});
					} else if (this.type2.isAnnotation()) {
						if (method.getDefaultValue() != null) {
							this.addDelta(
									getElementType(this.type2),
									IDelta.ADDED,
									IDelta.METHOD_WITH_DEFAULT_VALUE,
									this.currentDescriptorRestrictions,
									access2,
									this.type1,
									getKeyForMethod(method, this.type2),
									new String[] {Util.getDescriptorName(this.type2), methodDisplayName });
						} else {
							this.addDelta(
									getElementType(this.type2),
									IDelta.ADDED,
									IDelta.METHOD_WITHOUT_DEFAULT_VALUE,
									this.currentDescriptorRestrictions,
									access2,
									this.type1,
									getKeyForMethod(method, this.type2),
									new String[] {Util.getDescriptorName(this.type2), methodDisplayName });
						}
					} else {
						// check superclass
						// if null we need to walk the hierarchy of descriptor2
						boolean found = false;
						if (this.component2 != null) {
							if (this.type1.isInterface()) {
								Set interfacesSet = getInterfacesSet(this.type2);
								if (interfacesSet != null && isStatusOk()) {
									for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
										IApiType superTypeDescriptor = (IApiType) iterator.next();
										IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
										if (method3 == null) {
											continue;
										} else {
											// interface method can only be public
											// method has been move up in the hierarchy - report the delta and abort loop
											found = true;
											break;
										}
									}
								}
							} else {
								List superclassList = getSuperclassList(this.type2, true);
								if (superclassList != null) {
									loop: for (Iterator iterator = superclassList.iterator(); iterator.hasNext();) {
										IApiType superTypeDescriptor = (IApiType) iterator.next();
										IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
										if (method3 == null) {
											continue;
										} else {
											int access3 = method3.getModifiers();
											if (Util.isPublic(access3)
													|| Util.isProtected(access3)) {
												// method has been move up in the hierarchy - report the delta and abort loop
												// TODO need to make the distinction between methods that need to be re-implemented and methods that don't
												found = true;
												break loop;
											}
										}
									}
								}
							}
						}
						this.addDelta(
								getElementType(this.type2),
								IDelta.ADDED,
								found ? IDelta.OVERRIDEN_METHOD : IDelta.METHOD,
								this.currentDescriptorRestrictions,
								access2,
								this.type1,
								getKeyForMethod(method, this.type2),
								new String[] {Util.getDescriptorName(this.type2), methodDisplayName });
					}
					return;
				}
			} else if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
				if (Util.isPublic(access2) || Util.isProtected(access2)) {
					// report that it is no longer an API method
					if (this.type2.isAnnotation()) {
						this.addDelta(
								getElementType(this.type2),
								IDelta.REMOVED,
								method.getDefaultValue() != null ? IDelta.API_METHOD_WITH_DEFAULT_VALUE : IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE,
								this.currentDescriptorRestrictions,
								access2,
								this.type1,
								getKeyForMethod(method2, this.type2),
								new String[] {Util.getDescriptorName(this.type2), methodDisplayName});
					} else if (Util.isPublic(access) || Util.isProtected(access)) {
						this.addDelta(
								getElementType(this.type2),
								IDelta.REMOVED,
								method.isConstructor() ? IDelta.API_CONSTRUCTOR : IDelta.API_METHOD,
								Util.isAbstract(this.type2.getModifiers()) ? this.currentDescriptorRestrictions | RestrictionModifiers.NO_INSTANTIATE : this.currentDescriptorRestrictions,
								access2,
								this.type1,
								getKeyForMethod(method2, this.type2),
								new String[] {Util.getDescriptorName(this.type2), methodDisplayName});
					}
					return;
				}
			}
		}
		if (this.component.hasApiDescription() && !method.isConstructor() && !method.isClassInitializer()
				&& !(type1.isInterface() || type1.isAnnotation())) {
			if (restrictions != referenceRestrictions) {
				if (!Util.isFinal(access2)) {
					if (RestrictionModifiers.isOverrideRestriction(restrictions)
							&& !RestrictionModifiers.isOverrideRestriction(referenceRestrictions)) {
							this.addDelta(
									getElementType(method),
									IDelta.ADDED,
									IDelta.RESTRICTIONS,
									restrictions,
									access2,
									this.type1,
									getKeyForMethod(method2, this.type2),
									new String[] {Util.getDescriptorName(this.type2), methodDisplayName});
					}
				}
			}
		}
		String[] names1 = method.getExceptionNames();
		List list1 = null;
		if (names1 != null) {
			list1 = new ArrayList(names1.length);
			for (int i = 0; i < names1.length; i++) {
				list1.add(names1[i]);
			}
		}
		String[] names2 = method2.getExceptionNames();
		List list2 = null;
		if (names2 != null) {
			list2 = new ArrayList(names2.length);
			for (int i = 0; i < names2.length; i++) {
				list2.add(names2[i]);
			}
		}
		if (names1 != null) {
			if (names2 == null) {
				// check all exception in method descriptor to see if they are checked or unchecked exceptions
				loop: for (Iterator iterator = list1.iterator(); iterator.hasNext(); ) {
					String exceptionName = ((String) iterator.next()).replace('/', '.');
					if (isCheckedException(this.apiProfile, this.component, exceptionName)) {
						// report delta - removal of checked exception
						// TODO should we continue the loop for all remaining exceptions
						this.addDelta(
								getElementType(method),
								IDelta.REMOVED,
								IDelta.CHECKED_EXCEPTION,
								restrictions,
								method2.getModifiers(),
								this.type1,
								key,
								new String[] {Util.getDescriptorName(this.type1), methodDisplayName, exceptionName});
						break loop;
					} else {
						// report delta - removal of unchecked exception
						this.addDelta(
								getElementType(method),
								IDelta.REMOVED,
								IDelta.UNCHECKED_EXCEPTION,
								restrictions,
								method2.getModifiers(),
								this.type1,
								key,
								new String[] {Util.getDescriptorName(this.type1), methodDisplayName, exceptionName});
					}
				}
			} else {
				// check if the exceptions are consistent for both descriptors
				List removedExceptions = new ArrayList();
				for (Iterator iterator = list1.iterator(); iterator.hasNext(); ) {
					String exceptionName = ((String) iterator.next()).replace('/', '.');
					if (!list2.remove(exceptionName)) {
						// this means that the exceptionName was not found inside the new set of exceptions
						// so it has been removed
						removedExceptions.add(exceptionName);
					}
				}
				if (removedExceptions.size() != 0) {
					loop: for (Iterator iterator = removedExceptions.iterator(); iterator.hasNext(); ) {
						String exceptionName = ((String) iterator.next()).replace('/', '.');
						if (isCheckedException(this.apiProfile, this.component, exceptionName)) {
							// report delta - removal of checked exception
							// TODO should we continue the loop for all remaining exceptions
							this.addDelta(
									getElementType(method),
									IDelta.REMOVED,
									IDelta.CHECKED_EXCEPTION,
									restrictions,
									method2.getModifiers(),
									this.type1,
									key,
									new String[] {Util.getDescriptorName(this.type1), methodDisplayName, exceptionName});
							break loop;
						} else {
							// report delta - removal of unchecked exception
							this.addDelta(
									getElementType(method),
									IDelta.REMOVED,
									IDelta.UNCHECKED_EXCEPTION,
									restrictions,
									method2.getModifiers(),
									this.type1,
									key,
									new String[] {Util.getDescriptorName(this.type1), methodDisplayName, exceptionName});
						}
					}
				}
				loop: for (Iterator iterator = list2.iterator(); iterator.hasNext(); ) {
					String exceptionName = ((String) iterator.next()).replace('/', '.');
					if (isCheckedException(this.apiProfile2, this.component2, exceptionName)) {
						// report delta - addition of checked exception
						// TODO should we continue the loop for all remaining exceptions
						this.addDelta(
								getElementType(method),
								IDelta.ADDED,
								IDelta.CHECKED_EXCEPTION,
								restrictions,
								method2.getModifiers(),
								this.type1,
								key,
								new String[] {Util.getDescriptorName(this.type1), methodDisplayName, exceptionName});
						break loop;
					} else {
						// report delta - addition of unchecked exception
						this.addDelta(
								getElementType(method),
								IDelta.ADDED,
								IDelta.UNCHECKED_EXCEPTION,
								restrictions,
								method2.getModifiers(),
								this.type1,
								key,
								new String[] {Util.getDescriptorName(this.type1), methodDisplayName, exceptionName});
					}
				}
			}
		} else if (names2 != null) {
			// check all exception in method descriptor to see if they are checked or unchecked exceptions
			loop: for (Iterator iterator = list2.iterator(); iterator.hasNext(); ) {
				String exceptionName = ((String) iterator.next()).replace('/', '.');
				if (isCheckedException(this.apiProfile2, this.component2, exceptionName)) {
					// report delta - addition of checked exception
					this.addDelta(
							getElementType(method),
							IDelta.ADDED,
							IDelta.CHECKED_EXCEPTION,
							restrictions,
							method2.getModifiers(),
							this.type1,
							key,
							new String[] {Util.getDescriptorName(this.type1), methodDisplayName, exceptionName});
					// TODO should we continue the loop for all remaining exceptions
					break loop;
				} else {
					// report delta - addition of unchecked exception
					this.addDelta(
							getElementType(method),
							IDelta.ADDED,
							IDelta.UNCHECKED_EXCEPTION,
							restrictions,
							method2.getModifiers(),
							this.type1,
							key,
							new String[] {Util.getDescriptorName(this.type1), methodDisplayName, exceptionName});
				}
			}
		}
		if (Util.isVarargs(access)) {
			if (!Util.isVarargs(access2)) {
				// report delta: conversion from T... to T[] - break compatibility 
				this.addDelta(
						getElementType(method),
						IDelta.CHANGED,
						IDelta.VARARGS_TO_ARRAY,
						restrictions,
						method2.getModifiers(),
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			}
		} else if (Util.isVarargs(access2)) {
			// report delta: conversion from T[] to T... compatible
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.ARRAY_TO_VARARGS,
					restrictions,
					method2.getModifiers(),
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		}
		if (Util.isProtected(access)) {
			if (Util.isPrivate(access2) || Util.isDefault(access2)) {
				// report delta - decrease access: protected to default or private
				this.addDelta(
						getElementType(method),
						IDelta.CHANGED,
						IDelta.DECREASE_ACCESS,
						restrictions,
						access2,
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			} else if (Util.isPublic(access2)) {
				// report delta - increase access: protected to public
				this.addDelta(
						getElementType(method),
						IDelta.CHANGED,
						IDelta.INCREASE_ACCESS,
						restrictions,
						access2,
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			}
		} else if (Util.isPublic(access)
				&& (Util.isProtected(access2)
						|| Util.isPrivate(access2)
						|| Util.isDefault(access2))) {
			// report delta - decrease access: public to protected, default or private
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.DECREASE_ACCESS,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		} else if (Util.isDefault(access)
				&& (Util.isPublic(access2)
						|| Util.isProtected(access2))) {
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.INCREASE_ACCESS,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		} else if (Util.isPrivate(access)
				&& (Util.isDefault(access2)
						|| Util.isPublic(access2)
						|| Util.isProtected(access2))) {
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.INCREASE_ACCESS,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		}
		if (Util.isAbstract(access)) {
			if (!Util.isAbstract(access2)) {
				// report delta - changed from abstract to non-abstract
				this.addDelta(
						getElementType(method),
						IDelta.CHANGED,
						IDelta.ABSTRACT_TO_NON_ABSTRACT,
						restrictions,
						access2,
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			}
		} else if (Util.isAbstract(access2)){
			// report delta - changed from non-abstract to abstract
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.NON_ABSTRACT_TO_ABSTRACT,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		}
		if (Util.isFinal(access)) {
			if (!Util.isFinal(access2)) {
				// report delta - changed from final to non-final
				this.addDelta(
						getElementType(method),
						IDelta.CHANGED,
						IDelta.FINAL_TO_NON_FINAL,
						restrictions,
						access2,
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			}
		} else if (Util.isFinal(access2)) {
			int res = restrictions;
			if (!RestrictionModifiers.isOverrideRestriction(res)) {
				if (RestrictionModifiers.isExtendRestriction(this.currentDescriptorRestrictions)) {
					res = this.currentDescriptorRestrictions;
				} else if (RestrictionModifiers.isExtendRestriction(this.initialDescriptorRestrictions)) {
					res = this.initialDescriptorRestrictions;
				}
				if (RestrictionModifiers.isOverrideRestriction(referenceRestrictions)) {
					// it is ok to remove @nooverride and add final at the same time
					res = referenceRestrictions;
				}
			}
			this.addDelta(
					getElementType(method2),
					IDelta.CHANGED,
					IDelta.NON_FINAL_TO_FINAL,
					res,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type2), getMethodDisplayName(method2, this.type2)});
		}
		if (Util.isStatic(access)) {
			if (!Util.isStatic(access2)) {
				// report delta: change from static to non-static
				this.addDelta(
						getElementType(method),
						IDelta.CHANGED,
						IDelta.STATIC_TO_NON_STATIC,
						restrictions,
						access2,
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			}
		} else if (Util.isStatic(access2)){
			// report delta: change from non-static to static
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.NON_STATIC_TO_STATIC,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		}
		if (Util.isNative(access)) {
			if (!Util.isNative(access2)) {
				// report delta: change from native to non-native
				this.addDelta(
						getElementType(method),
						IDelta.CHANGED,
						IDelta.NATIVE_TO_NON_NATIVE,
						restrictions,
						access2,
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			}
		} else if (Util.isNative(access2)){
			// report delta: change from non-native to native
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.NON_NATIVE_TO_NATIVE,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		}
		if (Util.isSynchronized(access)) {
			if (!Util.isSynchronized(access2)) {
				// report delta: change from synchronized to non-synchronized
				this.addDelta(
						getElementType(method),
						IDelta.CHANGED,
						IDelta.SYNCHRONIZED_TO_NON_SYNCHRONIZED,
						restrictions,
						access2,
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			}
		} else if (Util.isSynchronized(access2)){
			// report delta: change from non-synchronized to synchronized
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.NON_SYNCHRONIZED_TO_SYNCHRONIZED,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		}
		// check type parameters
		String signature1 = method.getGenericSignature();
		String signature2 = method2.getGenericSignature();
		checkGenericSignature(signature1, signature2, method, method2);
		
		if (method.getDefaultValue() == null) {
			if (method2.getDefaultValue() != null) {
				// report delta : default value has been added - compatible
				this.addDelta(
						getElementType(method),
						IDelta.ADDED,
						IDelta.ANNOTATION_DEFAULT_VALUE,
						restrictions,
						access2,
						this.type1,
						key,
						new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
			}
		} else if (method2.getDefaultValue() == null) {
			// report delta : default value has been removed - incompatible
			this.addDelta(
					getElementType(method),
					IDelta.REMOVED,
					IDelta.ANNOTATION_DEFAULT_VALUE,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		} else if (!method.getDefaultValue().equals(method2.getDefaultValue())) {
			// report delta: default value has changed
			this.addDelta(
					getElementType(method),
					IDelta.CHANGED,
					IDelta.ANNOTATION_DEFAULT_VALUE,
					restrictions,
					access2,
					this.type1,
					key,
					new String[] {Util.getDescriptorName(this.type1), methodDisplayName});
		}
	}

	/**
	 * Returns the complete super-interface set for the given type descriptor or null, if it could not be
	 * computed
	 * @param type
	 * @param apiComponent
	 * @param profile
	 * @return the complete super-interface set for the given descriptor, or <code>null</code>
	 */
	private Set getInterfacesSet(IApiType type) {
		HashSet set = new HashSet();
		this.status = null;
		collectAllInterfaces(type, set);
		if (set.isEmpty()) {
			return null;
		}
		return set;
	}

	private String getMethodDisplayName(IApiMethod method, IApiType type) {
		String methodName = method.getName();
		if (method.isConstructor()) {
			String typeName = type.getName();
			int index = typeName.lastIndexOf('.');
			if (index == -1) {
				methodName = typeName;
			} else {
				int index2 = typeName.lastIndexOf('$');
				if (index2 > index) {
					methodName = typeName.substring(index2 + 1);
				} else {
					methodName = typeName.substring(index + 1);
				}
			}
		}
		return Signature.toString(method.getSignature(), methodName, null, false, false);
	}

	private SignatureDescriptor getSignatureDescriptor(String signature) {
		SignatureDescriptor signatureDescriptor = new SignatureDescriptor();
		SignatureReader signatureReader = new SignatureReader(signature);
		signatureReader.accept(new SignatureDecoder(signatureDescriptor));
		return signatureDescriptor;
	}
	private List getSuperclassList(IApiType type) {
		return getSuperclassList(type, false);
	}
	private List getSuperclassList(IApiType type, boolean includeObject) {
		return getSuperclassList(type, includeObject, false);
	}
	private List getSuperclassList(IApiType type, boolean includeObject, boolean includePrivate) {
		IApiType superClass = type;
		this.status = null;
		String superName = superClass.getSuperclassName();
		if (Util.isJavaLangObject(superName) && !includeObject) {
			return null;
		}
		List list = new ArrayList();
		try {
			while (superName != null && (!Util.isJavaLangObject(superName) || includeObject)) {
				superClass = superClass.getSuperclass();
				int visibility = VisibilityModifiers.PRIVATE;
				IApiComponent superComponent = superClass.getApiComponent();
				if (superComponent.hasApiDescription()) {
					IApiDescription apiDescription = superComponent.getApiDescription();
					IApiAnnotations elementDescription = apiDescription.resolveAnnotations(superClass.getHandle());
					if (elementDescription != null) {
						visibility = elementDescription.getVisibility();
					}
				} else if (Util.isPublic(superClass.getModifiers()) || Util.isProtected(superClass.getModifiers())) {
					visibility = VisibilityModifiers.API;
				}
				if (includePrivate || ((visibility & visibilityModifiers) != 0)) {
					list.add(superClass);
				}
				superName = superClass.getSuperclassName();
			}
		} catch (CoreException e) {
			reportStatus(e);
		}
		if (list.isEmpty()) return null;
		return list;
	}
	
	private void reportFieldAddition(IApiField field, IApiType type) {
		int access = field.getModifiers();
		String name = field.getName();
	
		if (Util.isSynthetic(access)) {
			// we ignore synthetic fields 
			return;
		}
		if (this.visibilityModifiers == VisibilityModifiers.API && component2.hasApiDescription()) {
			// check if this method should be removed because it is tagged as @noreference
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
			this.addDelta(
					getElementType(type),
					IDelta.ADDED,
					IDelta.ENUM_CONSTANT,
					this.currentDescriptorRestrictions,
					type.getModifiers(),
					this.type1,
					name,
					new String[] {Util.getDescriptorName(type), name});
		} else {
			this.addDelta(
					getElementType(type),
					IDelta.ADDED,
					IDelta.FIELD,
					this.currentDescriptorRestrictions,
					field.getModifiers(),
					this.type1,
					name,
					new String[] {Util.getDescriptorName(type), name});
		}
	}
	private void reportMethodAddition(IApiMethod method, IApiType type) {
		if (method.isClassInitializer()) {
			// report delta: addition of clinit method
			this.addDelta(
					getElementType(type),
					IDelta.ADDED,
					IDelta.CLINIT,
					this.currentDescriptorRestrictions,
					method.getModifiers(),
					this.type1,
					type.getName(),
					Util.getDescriptorName(type1));
			return;
		}
		int access = method.getModifiers();
		if (Util.isSynthetic(access)) {
			// we ignore synthetic method
			return;
		}
		if (this.visibilityModifiers == VisibilityModifiers.API && component2.hasApiDescription()) {
			// check if this method should be removed because it is tagged as @noreference
			IApiDescription apiDescription = null;
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
			// check if this method should be removed because it is tagged as @noreference
			if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
				// such a method is not seen as an API method
				return;
			}
		}
		String methodDisplayName = getMethodDisplayName(method, type);
		int restrictionsForMethodAddition = this.currentDescriptorRestrictions;
		if (Util.isFinal(this.type2.getModifiers())) {
			restrictionsForMethodAddition |= RestrictionModifiers.NO_EXTEND;
		}
		if (Util.isPublic(access) || Util.isProtected(access)) {
			if (method.isConstructor()) {
				this.addDelta(
						getElementType(type),
						IDelta.ADDED,
						IDelta.CONSTRUCTOR,
						restrictionsForMethodAddition,
						access,
						this.type1,
						getKeyForMethod(method, type),
						new String[] {Util.getDescriptorName(type), methodDisplayName});
			} else if (type.isAnnotation()) {
				if (method.getDefaultValue() != null) {
					this.addDelta(
							getElementType(type),
							IDelta.ADDED,
							IDelta.METHOD_WITH_DEFAULT_VALUE,
							restrictionsForMethodAddition,
							access,
							this.type1,
							getKeyForMethod(method, type),
							new String[] {Util.getDescriptorName(type), methodDisplayName });
				} else {
					this.addDelta(
							getElementType(type),
							IDelta.ADDED,
							IDelta.METHOD_WITHOUT_DEFAULT_VALUE,
							restrictionsForMethodAddition,
							type.getModifiers(),
							this.type1,
							getKeyForMethod(method, type),
							new String[] {Util.getDescriptorName(type), methodDisplayName });
				}
			} else {
				// check superclass
				// if null we need to walk the hierarchy of descriptor2
				boolean found = false;
				if (this.component2 != null) {
					String name = method.getName();
					String descriptor = method.getSignature();
					if (this.type1.isInterface()) {
						Set interfacesSet = getInterfacesSet(this.type2);
						if (interfacesSet != null && isStatusOk()) {
							for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
								IApiType superTypeDescriptor = (IApiType) iterator.next();
								IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
								if (method3 == null) {
									continue;
								} else {
									// interface method can only be public
									// method has been move up in the hierarchy - report the delta and abort loop
									found = true;
									break;
								}
							}
						}
					} else {
						List superclassList = getSuperclassList(this.type2, true);
						if (superclassList != null && isStatusOk()) {
							loop: for (Iterator iterator = superclassList.iterator(); iterator.hasNext();) {
								IApiType superTypeDescriptor = (IApiType) iterator.next();
								IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
								if (method3 == null) {
									continue;
								} else {
									int access3 = method3.getModifiers();
									if (Util.isPublic(access3)
											|| Util.isProtected(access3)) {
										// method has been move up in the hierarchy - report the delta and abort loop
										// TODO need to make the distinction between methods that need to be re-implemented and methods that don't
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
							Set interfacesSet = getInterfacesSet(this.type1);
							if (interfacesSet != null && isStatusOk()) {
								for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
									IApiType superTypeDescriptor = (IApiType) iterator.next();
									IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
									if (method3 == null) {
										continue;
									} else {
										// interface method can only be public
										// method has been move up in the hierarchy - report the delta and abort loop
										found = true;
										break;
									}
								}
							}
						} else {
							List superclassList = getSuperclassList(this.type1, true);
							if (superclassList != null && isStatusOk()) {
								loop: for (Iterator iterator = superclassList.iterator(); iterator.hasNext();) {
									IApiType superTypeDescriptor = (IApiType) iterator.next();
									IApiMethod method3 = superTypeDescriptor.getMethod(name, descriptor);
									if (method3 == null) {
										continue;
									} else {
										int access3 = method3.getModifiers();
										if (Util.isPublic(access3)
												|| Util.isProtected(access3)) {
											// method has been pushed down in the hierarchy - report the delta and abort loop
											// TODO need to make the distinction between methods that need to be re-implemented and methods that don't
											found = true;
											break loop;
										}
									}
								}
							}
						}
					}
					this.addDelta(
							getElementType(type),
							IDelta.ADDED,
							found ? IDelta.METHOD_MOVED_DOWN : IDelta.METHOD,
							restrictionsForMethodAddition,
							method.getModifiers(),
							this.type1,
							getKeyForMethod(method, type),
							new String[] {Util.getDescriptorName(type), methodDisplayName });
			} else {
					this.addDelta(
							getElementType(type),
							IDelta.ADDED,
							found ? IDelta.OVERRIDEN_METHOD : IDelta.METHOD,
							restrictionsForMethodAddition,
							method.getModifiers(),
							this.type1,
							getKeyForMethod(method, type),
							new String[] {Util.getDescriptorName(type), methodDisplayName });
				}
			}
		} else {
			this.addDelta(
					getElementType(type),
					IDelta.ADDED,
					method.isConstructor() ? IDelta.CONSTRUCTOR : IDelta.METHOD,
					restrictionsForMethodAddition,
					method.getModifiers(),
					this.type1,
					getKeyForMethod(method, type),
					new String[] {Util.getDescriptorName(type), methodDisplayName });
		}
	}
	
	private String getKeyForMethod(IApiMethod method, IApiType type) {
		StringBuffer buffer = new StringBuffer();
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
		buffer.append(method.getSignature());
		return String.valueOf(buffer);
	}

	private static boolean isAPI(int visibility,
			IApiType memberTypeDescriptor) {
		int access = memberTypeDescriptor.getModifiers();
		return (visibility & VisibilityModifiers.API) != 0
			&& (Util.isPublic(access) || Util.isProtected(access));
	}
	
	private ClassFileResult getType(String typeName, IApiComponent component, IApiProfile profile) throws CoreException {
		String packageName = Util.getPackageName(typeName);
		IApiComponent[] components = profile.resolvePackage(component, packageName);
		if (components == null) {
			String msg = MessageFormat.format(ComparatorMessages.ClassFileComparator_1, new String[] {packageName, profile.getName(), component.getId()});
			if (DEBUG) {
				System.err.println("TYPE LOOKUP: "+msg); //$NON-NLS-1$
			}
			reportStatus(new Status(Status.ERROR, component.getId(), msg));
			return null;
		}
		ClassFileResult result = Util.getComponent(components, typeName); 
		if (result == null) {
			String msg = MessageFormat.format(ComparatorMessages.ClassFileComparator_1, new String[] {packageName, profile.getName(), component.getId()});
			if (DEBUG) {
				System.err.println("TYPE LOOKUP: "+msg); //$NON-NLS-1$
			}
			reportStatus(new Status(Status.ERROR, component.getId(), msg));
			return null;
		}
		IClassFile classfile = result.getClassFile();
		if (classfile == null) {
			String msg = MessageFormat.format(ComparatorMessages.ClassFileComparator_2, new String[] {typeName, profile.getName(), component.getId()});
			if (DEBUG) {
				System.err.println("TYPE LOOKUP: "+msg); //$NON-NLS-1$
			}
			reportStatus(new Status(Status.ERROR, component.getId(), msg));
			return null;
		}
		return result;
	}
	
	/**
	 * Returns the delta element type code for the given type.
	 * Translates a type to interface, class, emum or annotation.
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
	 * Returns the delta element type code for the given method.
	 * Translates a method to constructor or method.
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
	 * Returns the delta type code for the given method when it is the target of a remove/add.
	 * Translates a method to constructor or method.
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
		switch (member.getElementType()) {
		case IApiMember.T_TYPE:
			return getElementType((IApiType)member);
		case IApiMember.T_METHOD:
			return getElementType((IApiMethod)member);
		}
		return IDelta.FIELD_ELEMENT_TYPE;
	}
}
