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
import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.signature.SignatureReader;

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

	private static boolean isCheckedException(IApiProfile profile, IApiComponent apiComponent, String exceptionName) {
		if (profile == null) {
			return true;
		}
		try {
			String packageName = Util.getPackageName(exceptionName);
			IClassFile classFile = Util.getClassFile(
					profile.resolvePackage(apiComponent, packageName),
					exceptionName);
			if (classFile != null) {
				// TODO should this be reported as a checked exception
				byte[] contents = classFile.getContents();
				TypeDescriptor typeDescriptor = new TypeDescriptor(contents);
				while (!Util.isJavaLangObject(typeDescriptor.name)) {
					String superName = typeDescriptor.superName;
					packageName = Util.getPackageName(superName);
					classFile = Util.getClassFile(
							profile.resolvePackage(apiComponent, packageName),
							superName);
					if (classFile == null) {
						// TODO should we report this failure ?
						if (DEBUG) {
							System.err.println("CHECKED EXCEPTION LOOKUP: Could not find " + superName + " in profile " + profile.getName() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						break;
					}
					typeDescriptor = new TypeDescriptor(classFile.getContents());
					if (Util.isJavaLangRuntimeException(typeDescriptor.name)) {
						return false;
					}
				}
			}
		} catch (CoreException e) {
			// by default exception are considered as checked exception 
			ApiPlugin.log(e);
		}
		return true;
	}

	IApiProfile apiProfile;
	IApiProfile apiProfile2;

	IClassFile classFile;
	IClassFile classFile2;
	
	IApiComponent component;
	IApiComponent component2;

	Delta delta;
	TypeDescriptor descriptor1;

	TypeDescriptor descriptor2;
	
	int visibilityModifiers;
	int currentDescriptorRestrictions;
	int initialDescriptorRestrictions;

	public ClassFileComparator(IClassFile classFile, IClassFile classFile2, IApiComponent component, IApiComponent component2, IApiProfile apiState, IApiProfile apiState2, int visibilityModifiers) {
		try {
			this.component = component;
			this.component2 = component2;
			this.descriptor1 = new TypeDescriptor(classFile.getContents());
			this.descriptor2 = new TypeDescriptor(classFile2.getContents());
			this.apiProfile = apiState;
			this.apiProfile2 = apiState2;
			this.classFile = classFile;
			this.classFile2 = classFile2;
			this.visibilityModifiers = visibilityModifiers;
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}

	public ClassFileComparator(TypeDescriptor typeDescriptor, IClassFile classFile2, IApiComponent component, IApiComponent component2, IApiProfile apiState, IApiProfile apiState2, int visibilityModifiers) {
		try {
			this.component = component;
			this.component2 = component2;
			this.descriptor1 = typeDescriptor;
			this.descriptor2 = new TypeDescriptor(classFile2.getContents());
			this.apiProfile = apiState;
			this.apiProfile2 = apiState2;
			this.classFile = typeDescriptor.classFile;
			this.classFile2 = classFile2;
			this.visibilityModifiers = visibilityModifiers;
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}
	private void addDelta(IDelta delta) {
		this.delta.add(delta);
	}
	private void addDelta(int elementType, int kind, int flags, int restrictions, int modifiers, IClassFile classFile, String key, String data) {
		this.delta.add(new Delta(Util.getDeltaComponentID(this.component2), elementType, kind, flags, restrictions, modifiers, classFile.getTypeName(), key, data));
	}
	private void addDelta(int elementType, int kind, int flags, int restrictions, int modifiers, IClassFile classFile, String key, String[] datas) {
		this.delta.add(new Delta(Util.getDeltaComponentID(this.component2), elementType, kind, flags, restrictions, modifiers, classFile.getTypeName(), key, datas));
	}
	private void checkSuperclass() {
		// check superclass set
		Set superclassSet1 = getSuperclassSet(this.descriptor1, this.component, this.apiProfile);
		Set superclassSet2 = getSuperclassSet(this.descriptor2, this.component2, this.apiProfile2);
		if (superclassSet1 == null) {
			if (superclassSet2 != null) {
				// this means the direct super class of descriptor1 is java.lang.Object
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.ADDED,
						IDelta.SUPERCLASS,
						this.currentDescriptorRestrictions,
						this.descriptor1.access,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
				return;
			}
			// both types extends java.lang.Object
			return;
		}
		if (superclassSet2 == null) {
			// this means the direct super class of descriptor2 is java.lang.Object
			this.addDelta(
					this.descriptor1.getElementType(),
					IDelta.CHANGED,
					IDelta.SUPERCLASS,
					this.currentDescriptorRestrictions,
					this.descriptor1.access,
					this.classFile,
					this.descriptor1.name,
					Util.getDescriptorName(descriptor1));
			return;
		}
		for (Iterator iterator = superclassSet1.iterator(); iterator.hasNext();) {
			TypeDescriptor superclassTypeDescriptor = (TypeDescriptor) iterator.next();
			if (!superclassSet2.contains(superclassTypeDescriptor)) {
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.CONTRACTED_SUPERCLASS_SET,
						this.currentDescriptorRestrictions,
						this.descriptor1.access,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
				return;
			}
		}
		if (superclassSet1.size() < superclassSet2.size()) {
			this.addDelta(
					this.descriptor1.getElementType(),
					IDelta.CHANGED,
					IDelta.EXPANDED_SUPERCLASS_SET,
					this.currentDescriptorRestrictions,
					this.descriptor1.access,
					this.classFile,
					this.descriptor1.name,
					Util.getDescriptorName(descriptor1));
		}
		
		// TODO check super class if they are not checked anyway
		// case where an API type inherits from a non-API type that contains public methods/fields
	}

	private void checkSuperInterfaces() {
		Set superinterfacesSet1 = getInterfacesSet(this.descriptor1, this.component, this.apiProfile);
		Set superinterfacesSet2 = getInterfacesSet(this.descriptor2, this.component2, this.apiProfile2);

		if (superinterfacesSet1 == null) {
			if (superinterfacesSet2 != null) {
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.EXPANDED_SUPERINTERFACES_SET,
						this.currentDescriptorRestrictions,
						this.descriptor1.access,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
			}
		} else if (superinterfacesSet2 == null) {
			this.addDelta(
					this.descriptor1.getElementType(),
					IDelta.CHANGED,
					IDelta.CONTRACTED_SUPERINTERFACES_SET,
					this.currentDescriptorRestrictions,
					this.descriptor1.access,
					this.classFile,
					this.descriptor1.name,
					Util.getDescriptorName(descriptor1));
		} else {
			for (Iterator iterator = superinterfacesSet1.iterator(); iterator.hasNext();) {
				TypeDescriptor superInterfaceTypeDescriptor = (TypeDescriptor) iterator.next();
				if (!superinterfacesSet2.contains(superInterfaceTypeDescriptor)) {
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.CONTRACTED_SUPERINTERFACES_SET,
							this.currentDescriptorRestrictions,
							this.descriptor1.access,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
					return;
				}
			}
			if (superinterfacesSet1.size() < superinterfacesSet2.size()) {
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.EXPANDED_SUPERINTERFACES_SET,
						this.currentDescriptorRestrictions,
						this.descriptor1.access,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
			}
		}
	}

	private void checkTypeMembers() {
		Set typeMembers = this.descriptor1.typeMembers;
		Set typeMembers2 = this.descriptor2.typeMembers;
		if (typeMembers != null) {
			if (typeMembers2 == null) {
				loop: for (Iterator iterator = typeMembers.iterator(); iterator.hasNext();) {
					try {
						MemberTypeDescriptor typeMember = (MemberTypeDescriptor ) iterator.next();
						String typeMemberName = ((IReferenceTypeDescriptor) typeMember.handle).getQualifiedName();
						// check visibility
						IApiDescription apiDescription = this.component.getApiDescription();
						IApiAnnotations memberTypeElementDescription = apiDescription.resolveAnnotations(Factory.typeDescriptor(typeMemberName));
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
							if (Util.isDefault(typeMember.access)
										|| Util.isPrivate(typeMember.access)) {
								continue loop;
							}
						}
						this.addDelta(
								this.descriptor1.getElementType(),
								IDelta.REMOVED,
								IDelta.TYPE_MEMBER,
								this.currentDescriptorRestrictions,
								typeMember.access,
								this.classFile,
								typeMember.name,
								typeMember.name.replace('$', '.'));
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
				}
				return;
			}
			// check removed or added type members
			List removedTypeMembers = new ArrayList();
			loop: for (Iterator iterator = typeMembers.iterator(); iterator.hasNext();) {
				MemberTypeDescriptor typeMember = (MemberTypeDescriptor ) iterator.next();
				String typeMemberName = ((IReferenceTypeDescriptor) typeMember.handle).getQualifiedName();
				if (!typeMembers2.remove(typeMember)) {
					removedTypeMembers.add(typeMember);
				} else {
					// check deltas inside the type member
					try {
						IClassFile memberType1 = this.component.findClassFile(typeMemberName);
						// check visibility of member types
						IApiDescription apiDescription = this.component.getApiDescription();
						IApiAnnotations memberTypeElementDescription = apiDescription.resolveAnnotations(Factory.typeDescriptor(typeMemberName));
						int memberTypeVisibility = 0;
						if (memberTypeElementDescription != null) {
							memberTypeVisibility = memberTypeElementDescription.getVisibility();
						}
						if ((memberTypeVisibility & visibilityModifiers) == 0) {
							// we skip the class file according to their visibility
							continue loop;
						}
						IApiDescription apiDescription2 = this.component2.getApiDescription();
						IApiAnnotations memberTypeElementDescription2 = apiDescription2.resolveAnnotations(Factory.typeDescriptor(typeMemberName));
						int memberTypeVisibility2 = 0;
						if (memberTypeElementDescription2 != null) {
							memberTypeVisibility2 = memberTypeElementDescription2.getVisibility();
						}
						if (visibilityModifiers == VisibilityModifiers.API) {
							// if the visibility is API, we only consider public and protected types
							if (Util.isDefault(typeMember.access)
										|| Util.isPrivate(typeMember.access)) {
								continue loop;
							}
						}
						String deltaComponentID = Util.getDeltaComponentID(component2);
						if (((memberTypeVisibility & VisibilityModifiers.API) != 0) && ((memberTypeVisibility2 & VisibilityModifiers.API) == 0)) {
							this.addDelta(
									new Delta(
											deltaComponentID,
											IDelta.API_COMPONENT_ELEMENT_TYPE,
											IDelta.REMOVED,
											IDelta.API_TYPE,
											memberTypeElementDescription2 != null ? memberTypeElementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
											typeMember.access,
											typeMemberName,
											typeMemberName,
											new String[] { typeMemberName.replace('$', '.'), deltaComponentID}));
							continue;
						}
						if ((memberTypeVisibility2 & visibilityModifiers) == 0) {
							// we simply report a changed visibility
							this.addDelta(
									new Delta(
											deltaComponentID,
											IDelta.API_COMPONENT_ELEMENT_TYPE,
											IDelta.CHANGED,
											IDelta.TYPE_VISIBILITY,
											memberTypeElementDescription2 != null ? memberTypeElementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
											typeMember.access,
											typeMemberName,
											typeMemberName,
											new String[] { typeMemberName.replace('$', '.'), deltaComponentID}));
						}
						IClassFile memberType2 = this.component2.findClassFile(typeMemberName);
						ClassFileComparator comparator = new ClassFileComparator(memberType1, memberType2, this.component, this.component2, this.apiProfile, this.apiProfile2, this.visibilityModifiers);
						IDelta delta2 = comparator.getDelta();
						if (delta2 != null && delta2 != ApiComparator.NO_DELTA) {
							this.addDelta(delta2);
						}
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
				}
			}
			loop: for (Iterator iterator = removedTypeMembers.iterator(); iterator.hasNext();) {
				try {
					MemberTypeDescriptor typeMember = (MemberTypeDescriptor) iterator.next();
					String typeMemberName = ((IReferenceTypeDescriptor) typeMember.handle).getQualifiedName();
					// check visibility
					IApiDescription apiDescription = this.component.getApiDescription();
					IApiAnnotations memberTypeElementDescription = apiDescription.resolveAnnotations(Factory.typeDescriptor(typeMemberName));
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
						if (Util.isDefault(typeMember.access)
									|| Util.isPrivate(typeMember.access)) {
							continue loop;
						}
					}
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.REMOVED,
							IDelta.TYPE_MEMBER,
							memberTypeElementDescription != null ? memberTypeElementDescription.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
							typeMember.access,
							this.classFile,
							typeMember.name,
							new String[] { typeMemberName.replace('$', '.'), Util.getDeltaComponentID(component2)});
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		if (typeMembers2 == null) return;
		// report remaining types in type members2 as addition
		int currentTypeApiRestrictions = this.currentDescriptorRestrictions;
		// Report delta as a breakage
		loop: for (Iterator iterator = typeMembers2.iterator(); iterator.hasNext();) {
			try {
				MemberTypeDescriptor typeMember = (MemberTypeDescriptor) iterator.next();
				// check visibility
				String typeMemberName = ((IReferenceTypeDescriptor) typeMember.handle).getQualifiedName();
				IApiDescription apiDescription2 = this.component2.getApiDescription();
				IApiAnnotations memberTypeElementDescription2 = apiDescription2.resolveAnnotations(Factory.typeDescriptor(typeMemberName));
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
					if (Util.isDefault(typeMember.access)
								|| Util.isPrivate(typeMember.access)) {
						continue loop;
					}
				}
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.ADDED,
						IDelta.TYPE_MEMBER,
						currentTypeApiRestrictions,
						typeMember.access,
						this.classFile,
						typeMember.name,
						typeMember.name.replace('$', '.'));
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
	}

	private void checkGenericSignature(String signature1, String signature2, ElementDescriptor elementDescriptor1, ElementDescriptor elementDescriptor2) {
		if (signature1 == null) {
			if (signature2 != null) {
				// added type parameter from scratch (none before)
				// report delta as compatible
				SignatureDescriptor signatureDescriptor2 = getSignatureDescritor(signature2);
				if (signatureDescriptor2.getTypeParameterDescriptors().length != 0) {
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.ADDED,
							IDelta.TYPE_PARAMETERS,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							elementDescriptor1.name,
							new String[] {getDataFor(elementDescriptor1, this.descriptor1)});
				} else if (signatureDescriptor2.getTypeArguments().length != 0) {
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.ADDED,
							IDelta.TYPE_ARGUMENTS,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							elementDescriptor1.name,
							new String[] {getDataFor(elementDescriptor1, this.descriptor1)});
				}
			}
		} else if (signature2 == null) {
			// removed type parameters
			SignatureDescriptor signatureDescriptor = getSignatureDescritor(signature1);
			if (signatureDescriptor.getTypeParameterDescriptors().length != 0) {
				this.addDelta(
						elementDescriptor1.getElementType(),
						IDelta.REMOVED,
						IDelta.TYPE_PARAMETERS,
						this.currentDescriptorRestrictions,
						elementDescriptor1.access,
						this.classFile,
						elementDescriptor1.name,
						new String[] {getDataFor(elementDescriptor1, this.descriptor1)});
			} else if (signatureDescriptor.getTypeArguments().length != 0) {
				this.addDelta(
						elementDescriptor1.getElementType(),
						IDelta.REMOVED,
						IDelta.TYPE_ARGUMENTS,
						this.currentDescriptorRestrictions,
						elementDescriptor1.access,
						this.classFile,
						elementDescriptor1.name,
						new String[] {getDataFor(elementDescriptor1, descriptor1)});
			}
		} else {
			// both types have generic signature
			// need to check delta for type parameter one by one
			SignatureDescriptor signatureDescriptor = getSignatureDescritor(signature1);
			SignatureDescriptor signatureDescriptor2 = getSignatureDescritor(signature2);
			
			TypeParameterDescriptor[] typeParameterDescriptors1 = signatureDescriptor.getTypeParameterDescriptors();
			int typeParameterDescriptorsLength1 = typeParameterDescriptors1.length;
			TypeParameterDescriptor[] typeParameterDescriptors2 = signatureDescriptor2.getTypeParameterDescriptors();
			int typeParameterDescriptorsLength2 = typeParameterDescriptors2.length;
			if (typeParameterDescriptorsLength1 < typeParameterDescriptorsLength2) {
				// report delta: incompatible
				this.addDelta(
						elementDescriptor1.getElementType(),
						IDelta.ADDED,
						IDelta.TYPE_PARAMETER,
						this.currentDescriptorRestrictions,
						elementDescriptor1.access,
						this.classFile,
						elementDescriptor1.name,
						new String[] {getDataFor(elementDescriptor1, descriptor1)});
				return;
			} else if (typeParameterDescriptorsLength1 > typeParameterDescriptorsLength2) {
				this.addDelta(
						elementDescriptor1.getElementType(),
						IDelta.REMOVED,
						IDelta.TYPE_PARAMETER,
						this.currentDescriptorRestrictions,
						elementDescriptor1.access,
						this.classFile,
						elementDescriptor1.name,
						new String[] {getDataFor(elementDescriptor1, descriptor1)});
				return;
			}
			// same number of type parameter descriptors
			for (int i = 0; i < typeParameterDescriptorsLength1; i++) {
				TypeParameterDescriptor parameterDescriptor1 = typeParameterDescriptors1[i];
				TypeParameterDescriptor parameterDescriptor2 = typeParameterDescriptors2[i];
				String name = parameterDescriptor1.name;
				if (!name.equals(parameterDescriptor2.name)) {
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.TYPE_PARAMETER_NAME,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							name,
							new String[] {getDataFor(elementDescriptor1, descriptor1), name });
				}
				if (parameterDescriptor1.classBound == null) {
					if (parameterDescriptor2.classBound != null) {
						// report delta added class bound of a type parameter
						this.addDelta(elementDescriptor1.getElementType(),
								IDelta.ADDED,
								IDelta.CLASS_BOUND,
								this.currentDescriptorRestrictions,
								elementDescriptor1.access,
								this.classFile,
								name,
								new String[] {getDataFor(elementDescriptor1, descriptor1), name });
					}
				} else if (parameterDescriptor2.classBound == null) {
					// report delta removed class bound of a type parameter
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.REMOVED,
							IDelta.CLASS_BOUND,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							name,
							new String[] {getDataFor(elementDescriptor1, descriptor1), name});
				} else if (!parameterDescriptor1.classBound.equals(parameterDescriptor2.classBound)) {
					// report delta changed class bound of a type parameter
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.CLASS_BOUND,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							name,
							new String[] {getDataFor(elementDescriptor1, descriptor1), name});
				}
				List interfaceBounds1 = parameterDescriptor1.interfaceBounds;
				List interfaceBounds2 = parameterDescriptor2.interfaceBounds;
				if (interfaceBounds1 == null) {
					if (interfaceBounds2 != null) {
						// report delta added interface bounds
						this.addDelta(
								elementDescriptor1.getElementType(),
								IDelta.ADDED,
								IDelta.INTERFACE_BOUNDS,
								this.currentDescriptorRestrictions,
								elementDescriptor1.access,
								this.classFile,
								name,
								new String[] {getDataFor(elementDescriptor1, descriptor1), name});
					}
				} else if (interfaceBounds2 == null) {
					// report delta removed interface bounds
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.REMOVED,
							IDelta.INTERFACE_BOUNDS,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							name,
							new String[] {getDataFor(elementDescriptor1, descriptor1), name});
				} else if (interfaceBounds1.size() < interfaceBounds2.size()) {
					// report delta added some interface bounds
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.ADDED,
							IDelta.INTERFACE_BOUND,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							name,
							new String[] {getDataFor(elementDescriptor1, descriptor1), name});
				} else if (interfaceBounds1.size() > interfaceBounds2.size()) {
					// report delta removed some interface bounds
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.REMOVED,
							IDelta.INTERFACE_BOUND,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							name,
							new String[] {getDataFor(elementDescriptor1, descriptor1), name});
				} else {
					loop: for (int j = 0, max = interfaceBounds1.size(); j < max; j++) {
						if (!interfaceBounds1.get(j).equals(interfaceBounds2.get(j))) {
							// report delta: different interface bounds (or reordered interface bound)
							this.addDelta(
									elementDescriptor1.getElementType(),
									IDelta.CHANGED,
									IDelta.INTERFACE_BOUND,
									this.currentDescriptorRestrictions,
									elementDescriptor1.access,
									this.classFile,
									name,
									new String[] {getDataFor(elementDescriptor1, descriptor1), name});
							break loop;
						}
					}
				}
			}

			if (typeParameterDescriptorsLength2 > 0 || typeParameterDescriptorsLength1 > 0) return;
			String[] typeArguments = signatureDescriptor.getTypeArguments();
			String[] typeArguments2 = signatureDescriptor2.getTypeArguments();
			int length = typeArguments.length;
			// typeArguments length and typeArguments2 length are identical
			for (int i = 0; i < length; i++) {
				if (!typeArguments[i].equals(typeArguments2[i])) {
					this.addDelta(
							elementDescriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.TYPE_ARGUMENTS,
							this.currentDescriptorRestrictions,
							elementDescriptor1.access,
							this.classFile,
							elementDescriptor1.name,
							new String[] {getDataFor(elementDescriptor1, descriptor1)});
					return;
				}
			}
		}
	}

	private void collectAllInterfaces(TypeDescriptor typeDescriptor, IApiComponent apiComponent, IApiProfile profile, Set set) {
		try {
			Set interfaces = typeDescriptor.interfaces;
			if (interfaces != null) {
				for (Iterator iterator = interfaces.iterator(); iterator.hasNext();) {
					String interfaceName = (String) iterator.next();
					String packageName = Util.getPackageName(interfaceName);
					IApiComponent[] components = profile.resolvePackage(apiComponent, packageName);
					if (components == null) {
						// TODO should we report this failure ?
						if (DEBUG) {
							System.err.println("SUPERINTERFACES LOOKUP: Could not find package " + packageName + " in profile " + profile.getName() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						continue;
					}
					IClassFile superinterface = Util.getClassFile(components, interfaceName);
					if (superinterface == null) {
						// TODO should we report this failure ?
						if (DEBUG) {
							System.err.println("SUPERINTERFACES LOOKUP: Could not find interface " + interfaceName + " in profile " + profile.getName() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
						continue;
					}
					TypeDescriptor typeDescriptor2 = new TypeDescriptor(superinterface.getContents());
					set.add(typeDescriptor2);
					collectAllInterfaces(typeDescriptor2, apiComponent, profile, set);
				}
			}
			String superclassName = typeDescriptor.superName;
			if (superclassName != null && !Util.isJavaLangObject(superclassName)) {
				String packageName = Util.getPackageName(superclassName);
				IApiComponent[] components = profile.resolvePackage(apiComponent, packageName);
				if (components == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERINTERFACES LOOKUP: Could not find package " + packageName + " in profile " + profile.getName() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					return;
				}
				IClassFile superclass = Util.getClassFile(components, superclassName);
				if (superclass == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERINTERFACES LOOKUP: Could not find class " + superclassName + " in profile " + profile.getName() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					return;
				}
				TypeDescriptor typeDescriptor2 = new TypeDescriptor(superclass.getContents());
				collectAllInterfaces(typeDescriptor2, apiComponent, profile, set);
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}
	
	private String getDataFor(ElementDescriptor descriptor, TypeDescriptor typeDescriptor) {
		switch(descriptor.getElementType()) {
			case IDelta.CLASS_ELEMENT_TYPE :
			case IDelta.INTERFACE_ELEMENT_TYPE :
			case IDelta.ANNOTATION_ELEMENT_TYPE :
			case IDelta.ENUM_ELEMENT_TYPE :
				return descriptor.name;
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
			case IDelta.METHOD_ELEMENT_TYPE :
				StringBuffer buffer = new StringBuffer();
				buffer.append(typeDescriptor.name).append('.').append(getMethodDisplayName((MethodDescriptor) descriptor, typeDescriptor));
				return String.valueOf(buffer);
			case IDelta.MEMBER_ELEMENT_TYPE :
				return descriptor.name.replace('$', '.');
			case IDelta.FIELD_ELEMENT_TYPE :
				buffer = new StringBuffer();
				buffer.append(typeDescriptor.name).append('.').append(descriptor.name);
				return String.valueOf(buffer);
		}
		return null;
	}

	public IDelta getDelta() {
		try {
			this.delta = new Delta();
			// check visibility
			int typeAccess = this.descriptor1.access;
			final IApiDescription component2ApiDescription = component2.getApiDescription();
			IApiAnnotations elementDescription2 = component2ApiDescription.resolveAnnotations(this.descriptor2.handle);
			this.initialDescriptorRestrictions = RestrictionModifiers.NO_RESTRICTIONS;
			this.currentDescriptorRestrictions = RestrictionModifiers.NO_RESTRICTIONS;
			if (elementDescription2 != null) {
				int restrictions2 = elementDescription2.getRestrictions();
				IApiDescription apiDescription = this.component.getApiDescription();
				IApiAnnotations componentApiDescription = apiDescription.resolveAnnotations(this.descriptor1.handle);
				if (componentApiDescription != null) {
					int restrictions = componentApiDescription.getRestrictions();
					this.initialDescriptorRestrictions = restrictions;
					if (restrictions2 != restrictions) {
						if (RestrictionModifiers.isUnrestricted(restrictions)) {
							// report different restrictions - from not restricted to restricted
							this.addDelta(
									this.descriptor1.getElementType(),
									IDelta.CHANGED,
									IDelta.RESTRICTIONS,
									restrictions,
									typeAccess,
									this.classFile,
									this.descriptor1.name,
									Util.getDescriptorName(descriptor1));
						} else if (!RestrictionModifiers.isUnrestricted(restrictions2)) {
							// report different restrictions - different restrictions
							this.addDelta(
									this.descriptor1.getElementType(),
									IDelta.CHANGED,
									IDelta.RESTRICTIONS,
									restrictions2,
									typeAccess,
									this.classFile,
									this.descriptor1.name,
									Util.getDescriptorName(descriptor1));
						}
					}
				}
				this.currentDescriptorRestrictions = restrictions2;
			}
			// first make sure that we compare interface with interface, class with class,
			// annotation with annotation and enum with enums
			int typeAccess2 = this.descriptor2.access;

			if (Util.isProtected(typeAccess)) {
				if (Util.isPrivate(typeAccess2) || Util.isDefault(typeAccess2)) {
					// report delta - decrease access: protected to default or private
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.DECREASE_ACCESS,
							this.currentDescriptorRestrictions,
							typeAccess,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
					return this.delta;
				} else if (Util.isPublic(typeAccess2)) {
					// report delta - increase access: protected to public
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.INCREASE_ACCESS,
							this.currentDescriptorRestrictions,
							typeAccess,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
					return this.delta;
				}
			} else if (Util.isPublic(typeAccess)
					&& (Util.isProtected(typeAccess2)
							|| Util.isPrivate(typeAccess2)
							|| Util.isDefault(typeAccess2))) {
				// report delta - decrease access: public to protected, default or private
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.DECREASE_ACCESS,
						this.currentDescriptorRestrictions,
						typeAccess,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
				return this.delta;
			} else if (Util.isDefault(typeAccess)
					&& (Util.isPublic(typeAccess2)
							|| Util.isProtected(typeAccess2))) {
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.INCREASE_ACCESS,
						this.currentDescriptorRestrictions,
						typeAccess,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
				return this.delta;
			} else if (Util.isPrivate(typeAccess)
					&& (Util.isDefault(typeAccess2)
							|| Util.isPublic(typeAccess2)
							|| Util.isProtected(typeAccess2))) {
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.INCREASE_ACCESS,
						this.currentDescriptorRestrictions,
						typeAccess,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
				return this.delta;
			}
			
			if (Util.isAnnotation(typeAccess)) {
				if (!Util.isAnnotation(typeAccess2)) {
					if (Util.isInterface(typeAccess2)) {
						// report conversion from annotation to interface
						this.addDelta(
								IDelta.ANNOTATION_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TO_INTERFACE,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.classFile,
								this.descriptor1.name,
								Util.getDescriptorName(descriptor1));
					} else if (Util.isEnum(typeAccess2)) {
						// report conversion from annotation to enum
						this.addDelta(
								IDelta.ANNOTATION_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TO_ENUM,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.classFile,
								this.descriptor1.name,
								Util.getDescriptorName(descriptor1));
					} else {
						// report conversion from annotation to class
						this.addDelta(
								IDelta.ANNOTATION_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TO_CLASS,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.classFile,
								this.descriptor1.name,
								Util.getDescriptorName(descriptor1));
					}
					return this.delta;
				}
			} else if (Util.isInterface(typeAccess)) {
				if (Util.isAnnotation(typeAccess2)) {
					// conversion from interface to annotation
					this.addDelta(
							IDelta.INTERFACE_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TO_ANNOTATION,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
					return this.delta;
				} else if (!Util.isInterface(typeAccess2)) {
					if (Util.isEnum(typeAccess2)) {
						// conversion from interface to enum
						this.addDelta(
								IDelta.INTERFACE_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TO_ENUM,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.classFile,
								this.descriptor1.name,
								Util.getDescriptorName(descriptor1));
					} else {
						// conversion from interface to class
						this.addDelta(
								IDelta.INTERFACE_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TO_CLASS,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.classFile,
								this.descriptor1.name,
								Util.getDescriptorName(descriptor1));
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
								IDelta.TO_ANNOTATION,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.classFile,
								this.descriptor1.name,
								Util.getDescriptorName(descriptor1));
					} else if (Util.isInterface(typeAccess2)) {
						// report conversion from enum to interface
						this.addDelta(
								IDelta.ENUM_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TO_INTERFACE,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.classFile,
								this.descriptor1.name,
								Util.getDescriptorName(descriptor1));
					} else {
						// report conversion from enum to class
						this.addDelta(
								IDelta.ENUM_ELEMENT_TYPE,
								IDelta.CHANGED,
								IDelta.TO_CLASS,
								this.currentDescriptorRestrictions,
								typeAccess2,
								this.classFile,
								this.descriptor1.name,
								Util.getDescriptorName(descriptor1));
					}
					return this.delta;
				}
			} else if (!Util.isClass(typeAccess2)) {
				if (Util.isAnnotation(typeAccess2)) {
					// report conversion from class to annotation
					this.addDelta(
							IDelta.CLASS_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TO_ANNOTATION,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
				} else if (Util.isInterface(typeAccess2)) {
					// report conversion from class to interface
					this.addDelta(
							IDelta.CLASS_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TO_INTERFACE,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
				} else {
					// report conversion from class to enum
					this.addDelta(
							IDelta.CLASS_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.TO_ENUM,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
				}
				return this.delta;
			}

			if (Util.isStatic(typeAccess)) {
				if (!Util.isStatic(typeAccess2)) {
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.STATIC_TO_NON_STATIC,
							this.currentDescriptorRestrictions,
							typeAccess2,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
				}
			} else if (Util.isStatic(typeAccess2)) {
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.NON_STATIC_TO_STATIC,
						this.currentDescriptorRestrictions,
						typeAccess2,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
			}
			// check super class set
			checkSuperclass();
			// check super interfaces set
			checkSuperInterfaces();
			
			// checks fields
			for (Iterator iterator = this.descriptor1.fields.values().iterator(); iterator.hasNext();) {
				FieldDescriptor fieldDescriptor = (FieldDescriptor) iterator.next();
				getDeltaForFieldDescriptor(fieldDescriptor);
			}
			// checks remaining fields (added fields)
			for (Iterator iterator = this.descriptor2.fields.values().iterator(); iterator.hasNext();) {
				FieldDescriptor fieldDescriptor = (FieldDescriptor) iterator.next();
				reportFieldAddition(fieldDescriptor, this.descriptor1);
			}
			
			// checks methods
			for (Iterator iterator = this.descriptor1.methods.values().iterator(); iterator.hasNext();) {
				Object object = iterator.next();
				if (object instanceof List) {
					List list = (List) object;
					for (Iterator iterator2 = list.iterator(); iterator2.hasNext();) {
						MethodDescriptor methodDescriptor = (MethodDescriptor) iterator2.next();
						getDeltaForMethodDescriptor(methodDescriptor);
					}
				} else {
					MethodDescriptor methodDescriptor = (MethodDescriptor) object;
					getDeltaForMethodDescriptor(methodDescriptor);
				}
			}
			// checks remaining methods (added methods)
			for (Iterator iterator = this.descriptor2.methods.values().iterator(); iterator.hasNext();) {
				Object object = iterator.next();
				if (object instanceof List) {
					List list = (List) object;
					for (Iterator iterator2 = list.iterator(); iterator2.hasNext();) {
						MethodDescriptor methodDescriptor = (MethodDescriptor) iterator2.next();
						reportMethodAddition(methodDescriptor, this.descriptor1);
					}
				} else {
					MethodDescriptor methodDescriptor = (MethodDescriptor) object;
					reportMethodAddition(methodDescriptor, this.descriptor1);
				}
			}
			if (Util.isAbstract(typeAccess)) {
				if (!Util.isAbstract(typeAccess2)) {
					// report delta - changed from abstract to non-abstract
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.ABSTRACT_TO_NON_ABSTRACT,
							this.currentDescriptorRestrictions,
							typeAccess,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
				}
			} else if (Util.isAbstract(typeAccess2)){
				// report delta - changed from non-abstract to abstract
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.NON_ABSTRACT_TO_ABSTRACT,
						this.currentDescriptorRestrictions,
						typeAccess,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
			}

			if (Util.isFinal(typeAccess)) {
				if (!Util.isFinal(typeAccess2)) {
					// report delta - changed from final to non-final
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.CHANGED,
							IDelta.FINAL_TO_NON_FINAL,
							this.currentDescriptorRestrictions,
							typeAccess,
							this.classFile,
							this.descriptor1.name,
							Util.getDescriptorName(descriptor1));
				}
			} else if (Util.isFinal(typeAccess2)){
				// report delta - changed from non-final to final
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.CHANGED,
						IDelta.NON_FINAL_TO_FINAL,
						this.currentDescriptorRestrictions,
						typeAccess,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
			}
			// check type parameters
			String signature1 = this.descriptor1.signature;
			String signature2 = this.descriptor2.signature;
			checkGenericSignature(signature1, signature2, this.descriptor1, this.descriptor2);
			
			// check type members
			checkTypeMembers();
			return this.delta.isEmpty() ? ApiComparator.NO_DELTA : this.delta;
		} catch (CoreException e) {
			ApiPlugin.log(e);
			return null;
		}
	}

	private void getDeltaForFieldDescriptor(FieldDescriptor fieldDescriptor) {
		int access = fieldDescriptor.access;
		if (Util.isSynthetic(access)) {
			// we ignore synthetic fields
			return;
		}
		String name = fieldDescriptor.name;
		FieldDescriptor fieldDescriptor2 = getFieldDescriptor(this.descriptor2, name);
		if (fieldDescriptor2 == null) {
			if (Util.isPrivate(access)
					|| Util.isDefault(access)) {
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.REMOVED,
						IDelta.FIELD,
						this.currentDescriptorRestrictions,
						access,
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(this.descriptor1), name});
			} else {
				boolean found = false;
				if (this.component2 != null) {
					if (this.descriptor1.isInterface()) {
						Set interfacesSet = getInterfacesSet(this.descriptor2, this.component2, this.apiProfile2);
						if (interfacesSet != null) {
							for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
								TypeDescriptor superTypeDescriptor = (TypeDescriptor) iterator.next();
								FieldDescriptor fieldDescriptor3 = getFieldDescriptor(superTypeDescriptor, name);
								if (fieldDescriptor3 == null) {
									continue;
								} else {
									// interface method can only be public
									// method has been move up in the hierarchy - report the delta and abort loop
									this.addDelta(
											this.descriptor1.getElementType(),
											IDelta.REMOVED,
											IDelta.FIELD_MOVED_UP,
											this.currentDescriptorRestrictions,
											fieldDescriptor.access,
											this.classFile,
											name,
											new String[] {Util.getDescriptorName(this.descriptor1), name});
									found = true;
									break;
								}
							}
						}
					} else {
						Set superclassSet = getSuperclassSet(this.descriptor2, this.component2, this.apiProfile2);
						if (superclassSet != null) {
							loop: for (Iterator iterator = superclassSet.iterator(); iterator.hasNext();) {
								TypeDescriptor superTypeDescriptor = (TypeDescriptor) iterator.next();
								FieldDescriptor fieldDescriptor3 = getFieldDescriptor(superTypeDescriptor, name);
								if (fieldDescriptor3 == null) {
									continue;
								} else {
									int access3 = fieldDescriptor3.access;
									if (Util.isPublic(access3)
											|| Util.isProtected(access3)) {
										// method has been move up in the hierarchy - report the delta and abort loop
										this.addDelta(
												this.descriptor1.getElementType(),
												IDelta.REMOVED,
												IDelta.FIELD_MOVED_UP,
												this.currentDescriptorRestrictions,
												fieldDescriptor3.access,
												this.classFile,
												name,
												new String[] {Util.getDescriptorName(this.descriptor1), name});
										found = true;
										break loop;
									}
								}
							}
						}
					}
				}
				if (!found) {
					if (fieldDescriptor.isEnum()) {
						// report delta (removal of an enum constant - not compatible)
						this.addDelta(
								this.descriptor1.getElementType(),
								IDelta.REMOVED,
								IDelta.ENUM_CONSTANT,
								this.currentDescriptorRestrictions,
								this.descriptor2.access,
								this.classFile,
								name,
								new String[] {Util.getDescriptorName(this.descriptor1), name});
						return;
					}
					// removing a public field is a breakage
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.REMOVED,
							IDelta.FIELD,
							this.currentDescriptorRestrictions,
							fieldDescriptor.access,
							this.classFile,
							name,
							new String[] {Util.getDescriptorName(this.descriptor1), name});
				}
			}
			return;
		}
		int restrictions = RestrictionModifiers.NO_RESTRICTIONS;
		try {
			IApiDescription apiDescription = this.component2.getApiDescription();
			IApiAnnotations resolvedAPIDescription = apiDescription.resolveAnnotations(fieldDescriptor2.handle);
			if (resolvedAPIDescription != null) {
				restrictions = resolvedAPIDescription.getRestrictions();
			}
		} catch (CoreException e) {
			// ignore
		}
		if (RestrictionModifiers.isUnrestricted(restrictions)) {
			restrictions = this.currentDescriptorRestrictions; 
		}
		int access2 = fieldDescriptor2.access;
		if (!fieldDescriptor.descriptor.equals(fieldDescriptor2.descriptor)) {
			// report delta
			this.addDelta(
					fieldDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.TYPE,
					restrictions,
					access2,
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name});
		} else {
			// check type parameters
			String signature1 = fieldDescriptor.signature;
			String signature2 = fieldDescriptor2.signature;
			checkGenericSignature(signature1, signature2, fieldDescriptor, fieldDescriptor2);
		}
		if (fieldDescriptor.value != null) {
			if (fieldDescriptor2.value == null) {
				// report delta - removal of constant value
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.VALUE,
						restrictions,
						access2,
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(this.descriptor1), name, String.valueOf(fieldDescriptor.value)});
			} else if (!fieldDescriptor.value.equals(fieldDescriptor2.value)) {
				// report delta - modified constant value
				if (Util.isFinal(this.descriptor2.access)) {
					// we make the restriction, non extend restrictions
					this.addDelta(
							IDelta.FIELD_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.VALUE,
							restrictions | RestrictionModifiers.NO_EXTEND,
							access2,
							this.classFile,
							name,
							new String[] {Util.getDescriptorName(this.descriptor1), name, String.valueOf(fieldDescriptor.value)});
				} else {
					this.addDelta(
							IDelta.FIELD_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.VALUE,
							restrictions,
							access2,
							this.classFile,
							name,
							new String[] {Util.getDescriptorName(this.descriptor1), name, String.valueOf(fieldDescriptor.value)});
				}
			}
		} else if (fieldDescriptor2.value != null) {
			// report delta
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.ADDED,
					IDelta.VALUE,
					restrictions,
					access2,
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name, String.valueOf(fieldDescriptor2.value)});
		}
		if (Util.isProtected(access)) {
			if (Util.isPrivate(access2) || Util.isDefault(access2)) {
				// report delta - decrease access: protected to default or private
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.DECREASE_ACCESS,
						restrictions,
						access2,
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(this.descriptor1), name});
			} else if (Util.isPublic(access2)) {
				// report delta - increase access: protected to public
				this.addDelta(
						IDelta.FIELD_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.INCREASE_ACCESS,
						restrictions,
						access2,
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(this.descriptor1), name});
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
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name});
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
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name});
		} else if (Util.isDefault(access)
				&& (Util.isProtected(access2)
						|| Util.isPublic(access2))) {
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.INCREASE_ACCESS,
					restrictions,
					access2,
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name});
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
							this.classFile,
							name,
							new String[] {Util.getDescriptorName(this.descriptor1), name});
				} else if (fieldDescriptor.value != null) {
					// report delta - final to non-final for a static field with a compile time constant
					this.addDelta(
							IDelta.FIELD_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT,
							restrictions,
							access2,
							this.classFile,
							name,
							new String[] {Util.getDescriptorName(this.descriptor1), name});
				} else {
					// report delta - final to non-final for a static field with no compile time constant
					this.addDelta(
							IDelta.FIELD_ELEMENT_TYPE,
							IDelta.CHANGED,
							IDelta.FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT,
							restrictions,
							access2,
							this.classFile,
							name,
							new String[] {Util.getDescriptorName(this.descriptor1), name});
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
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name});
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
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(this.descriptor1), name});
			}
		} else if (Util.isStatic(access2)) {
			// report delta - non-static to static
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.NON_STATIC_TO_STATIC,
					restrictions,
					access2,
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name});
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
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(this.descriptor1), name});
			}
		} else if (Util.isTransient(access2)) {
			// report delta - non-tansient to transient
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.NON_TRANSIENT_TO_TRANSIENT,
					restrictions,
					access2,
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name});
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
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(this.descriptor1), name});
			}
		} else if (Util.isVolatile(access2)) {
			// report delta - non-volatile to volatile
			this.addDelta(
					IDelta.FIELD_ELEMENT_TYPE,
					IDelta.CHANGED,
					IDelta.NON_VOLATILE_TO_VOLATILE,
					restrictions,
					access2,
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(this.descriptor1), name});
		}
	}
	private void getDeltaForMethodDescriptor(MethodDescriptor methodDescriptor) {
		int access = methodDescriptor.access;
		if (Util.isSynthetic(access)) {
			// we ignore synthetic methods
			return;
		}
		String name = methodDescriptor.name;
		String descriptor = methodDescriptor.descriptor;
		String key = getKeyForMethod(methodDescriptor, this.descriptor1);
		MethodDescriptor methodDescriptor2 = getMethodDescriptor(this.descriptor2, name, descriptor);
		String methodDisplayName = getMethodDisplayName(methodDescriptor, this.descriptor1);
		if (methodDescriptor2 == null) {
			if (methodDescriptor.isClinit()) {
				// report delta: removal of a clinit method
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.REMOVED,
						IDelta.CLINIT,
						this.currentDescriptorRestrictions,
						access,
						this.classFile,
						this.descriptor1.name,
						Util.getDescriptorName(descriptor1));
				return;
			} else if (Util.isPrivate(access)
					|| Util.isDefault(access)) {
				this.addDelta(
						this.descriptor1.getElementType(),
						IDelta.REMOVED,
						methodDescriptor.getElementType() == IDelta.CONSTRUCTOR_ELEMENT_TYPE ? IDelta.CONSTRUCTOR : IDelta.METHOD,
						this.currentDescriptorRestrictions,
						access,
						this.classFile,
						this.descriptor1.name,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
				return;
			}
			// if null we need to walk the hierarchy of descriptor2
			TypeDescriptor typeDescriptor = this.descriptor2;
			boolean found = false;
			if (this.component2 != null) {
				if (this.descriptor1.isInterface()) {
					Set interfacesSet = getInterfacesSet(typeDescriptor, this.component2, this.apiProfile2);
					if (interfacesSet != null) {
						for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
							TypeDescriptor superTypeDescriptor = (TypeDescriptor) iterator.next();
							MethodDescriptor methodDescriptor3 = getMethodDescriptor(superTypeDescriptor, name, descriptor);
							if (methodDescriptor3 == null) {
								continue;
							} else {
								// interface method can only be public
								// method has been move up in the hierarchy - report the delta and abort loop
								this.addDelta(
										this.descriptor1.getElementType(),
										IDelta.REMOVED,
										IDelta.METHOD_MOVED_UP,
										this.currentDescriptorRestrictions,
										access,
										this.classFile,
										this.descriptor1.name,
										new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
								found = true;
								break;
							}
						}
					}
				} else {
					Set superclassSet = getSuperclassSet(typeDescriptor, this.component2, this.apiProfile2);
					if (superclassSet != null) {
						loop: for (Iterator iterator = superclassSet.iterator(); iterator.hasNext();) {
							TypeDescriptor superTypeDescriptor = (TypeDescriptor) iterator.next();
							MethodDescriptor methodDescriptor3 = getMethodDescriptor(superTypeDescriptor, name, descriptor);
							if (methodDescriptor3 == null) {
								continue;
							} else {
								int access3 = methodDescriptor3.access;
								if (Util.isPublic(access3)
										|| Util.isProtected(access3)) {
									// method has been move up in the hierarchy - report the delta and abort loop
									// TODO need to make the distinction between methods that need to be reimplemented and methods that don't
									this.addDelta(
											this.descriptor1.getElementType(),
											IDelta.REMOVED,
											IDelta.METHOD_MOVED_UP,
											this.currentDescriptorRestrictions,
											access,
											this.classFile,
											this.descriptor1.name,
											new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
									found = true;
									break loop;
								}
							}
						}
					}
				}
			}
			if (!found) {
				if (this.descriptor1.isAnnotation()) {
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.REMOVED,
							methodDescriptor.defaultValue != null ? IDelta.METHOD_WITH_DEFAULT_VALUE : IDelta.METHOD_WITHOUT_DEFAULT_VALUE,
							this.currentDescriptorRestrictions,
							methodDescriptor.access,
							this.classFile,
							this.descriptor1.name,
							new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
				} else {
					this.addDelta(
							this.descriptor1.getElementType(),
							IDelta.REMOVED,
							methodDescriptor.isConstructor() ? IDelta.CONSTRUCTOR : IDelta.METHOD,
							Util.isAbstract(this.descriptor1.access) ? this.currentDescriptorRestrictions | RestrictionModifiers.NO_INSTANTIATE : this.currentDescriptorRestrictions,
							methodDescriptor.access,
							this.classFile,
							this.descriptor1.name,
							new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
				}
			}
			return;
		}
		int restrictions = RestrictionModifiers.NO_RESTRICTIONS;
		try {
			IApiDescription apiDescription = this.component2.getApiDescription();
			IApiAnnotations resolvedAPIDescription = apiDescription.resolveAnnotations(methodDescriptor2.handle);
			if (resolvedAPIDescription != null) {
				restrictions = resolvedAPIDescription.getRestrictions();
			}
		} catch (CoreException e) {
			// ignore
		}
		if (methodDescriptor.exceptions != null) {
			if (methodDescriptor2.exceptions == null) {
				// check all exception in method descriptor to see if they are checked or unchecked exceptions
				loop: for (Iterator iterator = methodDescriptor.exceptions.iterator(); iterator.hasNext(); ) {
					String exceptionName = ((String) iterator.next()).replace('/', '.');
					if (isCheckedException(this.apiProfile, this.component, exceptionName)) {
						// report delta - removal of checked exception
						// TODO should we continue the loop for all remaining exceptions
						this.addDelta(
								methodDescriptor.getElementType(),
								IDelta.REMOVED,
								IDelta.CHECKED_EXCEPTION,
								restrictions,
								methodDescriptor2.access,
								this.classFile,
								key,
								new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName, exceptionName});
						break loop;
					} else {
						// report delta - removal of unchecked exception
						this.addDelta(
								methodDescriptor.getElementType(),
								IDelta.REMOVED,
								IDelta.UNCHECKED_EXCEPTION,
								restrictions,
								methodDescriptor2.access,
								this.classFile,
								key,
								new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName, exceptionName});
					}
				}
			} else {
				// check if the exceptions are consistent for both descriptors
				List removedExceptions = new ArrayList();
				for (Iterator iterator = methodDescriptor.exceptions.iterator(); iterator.hasNext(); ) {
					String exceptionName = ((String) iterator.next()).replace('/', '.');
					if (!methodDescriptor2.exceptions.remove(exceptionName)) {
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
									methodDescriptor.getElementType(),
									IDelta.REMOVED,
									IDelta.CHECKED_EXCEPTION,
									restrictions,
									methodDescriptor2.access,
									this.classFile,
									key,
									new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName, exceptionName});
							break loop;
						} else {
							// report delta - removal of unchecked exception
							this.addDelta(
									methodDescriptor.getElementType(),
									IDelta.REMOVED,
									IDelta.UNCHECKED_EXCEPTION,
									restrictions,
									methodDescriptor2.access,
									this.classFile,
									key,
									new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName, exceptionName});
						}
					}
				}
				loop: for (Iterator iterator = methodDescriptor2.exceptions.iterator(); iterator.hasNext(); ) {
					String exceptionName = ((String) iterator.next()).replace('/', '.');
					if (isCheckedException(this.apiProfile2, this.component2, exceptionName)) {
						// report delta - addition of checked exception
						// TODO should we continue the loop for all remaining exceptions
						this.addDelta(
								methodDescriptor.getElementType(),
								IDelta.ADDED,
								IDelta.CHECKED_EXCEPTION,
								restrictions,
								methodDescriptor2.access,
								this.classFile,
								key,
								new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName, exceptionName});
						break loop;
					} else {
						// report delta - addition of unchecked exception
						this.addDelta(
								methodDescriptor.getElementType(),
								IDelta.ADDED,
								IDelta.UNCHECKED_EXCEPTION,
								restrictions,
								methodDescriptor2.access,
								this.classFile,
								key,
								new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName, exceptionName});
					}
				}
			}
		} else if (methodDescriptor2.exceptions != null) {
			// check all exception in method descriptor to see if they are checked or unchecked exceptions
			loop: for (Iterator iterator = methodDescriptor2.exceptions.iterator(); iterator.hasNext(); ) {
				String exceptionName = ((String) iterator.next()).replace('/', '.');
				if (isCheckedException(this.apiProfile2, this.component2, exceptionName)) {
					// report delta - addition of checked exception
					this.addDelta(
							methodDescriptor.getElementType(),
							IDelta.ADDED,
							IDelta.CHECKED_EXCEPTION,
							restrictions,
							methodDescriptor2.access,
							this.classFile,
							key,
							new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName, exceptionName});
					// TODO should we continue the loop for all remaining exceptions
					break loop;
				} else {
					// report delta - addition of unchecked exception
					this.addDelta(
							methodDescriptor.getElementType(),
							IDelta.ADDED,
							IDelta.UNCHECKED_EXCEPTION,
							restrictions,
							methodDescriptor2.access,
							this.classFile,
							key,
							new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName, exceptionName});
				}
			}
		}
		int access2 = methodDescriptor2.access;
		if (Util.isVarargs(access)) {
			if (!Util.isVarargs(access2)) {
				// report delta: conversion from T... to T[] - break compatibility 
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.CHANGED,
						IDelta.VARARGS_TO_ARRAY,
						restrictions,
						methodDescriptor2.access,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			}
		} else if (Util.isVarargs(access2)) {
			// report delta: conversion from T[] to T... compatible
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.ARRAY_TO_VARARGS,
					restrictions,
					methodDescriptor2.access,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		}
		if (Util.isProtected(access)) {
			if (Util.isPrivate(access2) || Util.isDefault(access2)) {
				// report delta - decrease access: protected to default or private
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.CHANGED,
						IDelta.DECREASE_ACCESS,
						restrictions,
						methodDescriptor.access,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			} else if (Util.isPublic(access2)) {
				// report delta - increase access: protected to public
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.CHANGED,
						IDelta.INCREASE_ACCESS,
						restrictions,
						methodDescriptor.access,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			}
		} else if (Util.isPublic(access)
				&& (Util.isProtected(access2)
						|| Util.isPrivate(access2)
						|| Util.isDefault(access2))) {
			// report delta - decrease access: public to protected, default or private
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.DECREASE_ACCESS,
					restrictions,
					methodDescriptor.access,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		} else if (Util.isDefault(access)
				&& (Util.isPublic(access2)
						|| Util.isProtected(access2))) {
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.INCREASE_ACCESS,
					restrictions,
					methodDescriptor.access,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		} else if (Util.isPrivate(access)
				&& (Util.isDefault(access2)
						|| Util.isPublic(access2)
						|| Util.isProtected(access2))) {
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.INCREASE_ACCESS,
					restrictions,
					methodDescriptor.access,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		}
		if (Util.isAbstract(access)) {
			if (!Util.isAbstract(access2)) {
				// report delta - changed from abstract to non-abstract
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.CHANGED,
						IDelta.ABSTRACT_TO_NON_ABSTRACT,
						restrictions,
						access,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			}
		} else if (Util.isAbstract(access2)){
			// report delta - changed from non-abstract to abstract
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.NON_ABSTRACT_TO_ABSTRACT,
					restrictions,
					methodDescriptor2.access,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		}
		if (Util.isFinal(access)) {
			if (!Util.isFinal(access2)) {
				// report delta - changed from final to non-final
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.CHANGED,
						IDelta.FINAL_TO_NON_FINAL,
						restrictions,
						methodDescriptor2.access,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			}
		} else if (Util.isFinal(access2)) {
			int res = restrictions;
			if (!RestrictionModifiers.isOverrideRestriction(res)) {
				if (RestrictionModifiers.isExtendRestriction(this.currentDescriptorRestrictions)) {
					res = this.currentDescriptorRestrictions;
				} else if (RestrictionModifiers.isExtendRestriction(this.initialDescriptorRestrictions)) {
					res = this.initialDescriptorRestrictions;
				}
			}
			this.addDelta(
					methodDescriptor2.getElementType(),
					IDelta.CHANGED,
					IDelta.NON_FINAL_TO_FINAL,
					res,
					methodDescriptor2.access,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor2), getMethodDisplayName(methodDescriptor2, this.descriptor2)});
		}
		if (Util.isStatic(access)) {
			if (!Util.isStatic(access2)) {
				// report delta: change from static to non-static
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.CHANGED,
						IDelta.STATIC_TO_NON_STATIC,
						restrictions,
						access2,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			}
		} else if (Util.isStatic(access2)){
			// report delta: change from non-static to static
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.NON_STATIC_TO_STATIC,
					restrictions,
					access2,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		}
		if (Util.isNative(access)) {
			if (!Util.isNative(access2)) {
				// report delta: change from native to non-native
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.CHANGED,
						IDelta.NATIVE_TO_NON_NATIVE,
						restrictions,
						access2,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			}
		} else if (Util.isNative(access2)){
			// report delta: change from non-native to native
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.NON_NATIVE_TO_NATIVE,
					restrictions,
					access2,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		}
		if (Util.isSynchronized(access)) {
			if (!Util.isSynchronized(access2)) {
				// report delta: change from synchronized to non-synchronized
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.CHANGED,
						IDelta.SYNCHRONIZED_TO_NON_SYNCHRONIZED,
						restrictions,
						access2,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			}
		} else if (Util.isSynchronized(access2)){
			// report delta: change from non-synchronized to synchronized
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.NON_SYNCHRONIZED_TO_SYNCHRONIZED,
					restrictions,
					access2,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		}
		// check type parameters
		String signature1 = methodDescriptor.signature;
		String signature2 = methodDescriptor2.signature;
		checkGenericSignature(signature1, signature2, methodDescriptor, methodDescriptor2);
		
		if (methodDescriptor.defaultValue == null) {
			if (methodDescriptor2.defaultValue != null) {
				// report delta : default value has been added - compatible
				this.addDelta(
						methodDescriptor.getElementType(),
						IDelta.ADDED,
						IDelta.ANNOTATION_DEFAULT_VALUE,
						restrictions,
						access2,
						this.classFile,
						key,
						new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
			}
		} else if (methodDescriptor2.defaultValue == null) {
			// report delta : default value has been removed - incompatible
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.REMOVED,
					IDelta.ANNOTATION_DEFAULT_VALUE,
					restrictions,
					access,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		} else if (!methodDescriptor.defaultValue.equals(methodDescriptor2.defaultValue)) {
			// report delta: default value has changed
			this.addDelta(
					methodDescriptor.getElementType(),
					IDelta.CHANGED,
					IDelta.ANNOTATION_DEFAULT_VALUE,
					restrictions,
					access,
					this.classFile,
					key,
					new String[] {Util.getDescriptorName(this.descriptor1), methodDisplayName});
		}
	}

	FieldDescriptor getFieldDescriptor(TypeDescriptor typeDescriptor, String name) {
		Object object = typeDescriptor.fields.get(name);
		if (object == null) return null;
		FieldDescriptor fieldDescriptor  = (FieldDescriptor) object;
		typeDescriptor.fields.remove(name);
		return fieldDescriptor;
	}

	private Set getInterfacesSet(TypeDescriptor typeDescriptor, IApiComponent apiComponent, IApiProfile profile) {
		HashSet set = new HashSet();
		collectAllInterfaces(typeDescriptor, apiComponent, profile, set);
		if (set.isEmpty()) return null;
		return set;
	}

	MethodDescriptor getMethodDescriptor(TypeDescriptor typeDescriptor, String name, String descriptor) {
		Object object = typeDescriptor.methods.get(name);
		if (object == null) return null;
		if (object instanceof List) {
			List list = (List) object;
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				MethodDescriptor methodDescriptor = (MethodDescriptor) iterator.next();
				if (methodDescriptor.name.equals(name) && methodDescriptor.descriptor.equals(descriptor)) {
					list.remove(methodDescriptor);
					if (list.size() == 0) {
						typeDescriptor.methods.remove(name);
					}
					return methodDescriptor;
				}
			}
			return null;
		}
		MethodDescriptor methodDescriptor = (MethodDescriptor) object;
		if (methodDescriptor.name.equals(name) && methodDescriptor.descriptor.equals(descriptor)) {
			typeDescriptor.methods.remove(name);
			return methodDescriptor;
		}
		return null;
	}

	private String getMethodDisplayName(MethodDescriptor methodDescriptor, TypeDescriptor typeDescriptor) {
		String methodName = methodDescriptor.name;
		if (methodDescriptor.isConstructor()) {
			String typeName = typeDescriptor.name;
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
		return Signature.toString(methodDescriptor.descriptor, methodName, null, false, false);
	}

	private SignatureDescriptor getSignatureDescritor(String signature) {
		SignatureDescriptor signatureDescriptor = new SignatureDescriptor();
		SignatureReader signatureReader = new SignatureReader(signature);
		signatureReader.accept(new SignatureDecoder(signatureDescriptor));
		return signatureDescriptor;
	}
	private Set getSuperclassSet(TypeDescriptor typeDescriptor, IApiComponent apiComponent, IApiProfile profile) {
		return getSuperclassSet(typeDescriptor, apiComponent, profile, false);
	}
	private Set getSuperclassSet(TypeDescriptor typeDescriptor, IApiComponent apiComponent, IApiProfile profile, boolean includeObject) {
		TypeDescriptor descriptor = typeDescriptor;
		String superName = descriptor.superName;
		if (Util.isJavaLangObject(superName) && !includeObject) {
			return null;
		}
		HashSet set = new HashSet();
		IApiComponent sourceComponent = apiComponent; 
		try {
			loop: while (superName != null && (!Util.isJavaLangObject(superName) || includeObject)) {
				String packageName = Util.getPackageName(superName);
				IApiComponent[] components = profile.resolvePackage(sourceComponent, packageName);
				if (components == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERCLASS LOOKUP: Could not find package " + packageName + " in profile " + profile.getName() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					break loop;
				}
				sourceComponent = Util.getComponent(components, superName);
				if (sourceComponent == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERCLASS LOOKUP: Could not find package " + packageName + " in profile " + profile.getName() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					break loop;
				}
				IClassFile superclass = sourceComponent.findClassFile(superName);
				if (superclass == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERCLASS LOOKUP: Could not find class " + superName + " in profile " + profile.getName() + " from component " + sourceComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					break loop;
				}
				descriptor = new TypeDescriptor(superclass.getContents());
				set.add(descriptor);
				superName = descriptor.superName;
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return set;
	}
	
	private void reportFieldAddition(FieldDescriptor fieldDescriptor, TypeDescriptor descriptor) {
		int access = fieldDescriptor.access;
		String name = fieldDescriptor.name;
	
		if (Util.isSynthetic(access)) {
			// we ignore synthetic fields 
			return;
		}
		if (fieldDescriptor.isEnum()) {
			// report delta (addition of an enum constant - compatible
			this.addDelta(
					descriptor.getElementType(),
					IDelta.ADDED,
					IDelta.ENUM_CONSTANT,
					this.currentDescriptorRestrictions,
					descriptor.access,
					this.classFile,
					name,
					new String[] {Util.getDescriptorName(descriptor), name});
		} else {
			if (Util.isFinal(descriptor.access)) {
				this.addDelta(
						descriptor.getElementType(),
						IDelta.ADDED,
						IDelta.FIELD,
						this.currentDescriptorRestrictions | RestrictionModifiers.NO_EXTEND,
						fieldDescriptor.access,
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(descriptor), name});
			} else {
				this.addDelta(
						descriptor.getElementType(),
						IDelta.ADDED,
						IDelta.FIELD,
						this.currentDescriptorRestrictions,
						fieldDescriptor.access,
						this.classFile,
						name,
						new String[] {Util.getDescriptorName(descriptor), name});
			}
		}
	}
	private void reportMethodAddition(MethodDescriptor methodDescriptor, TypeDescriptor typeDescriptor) {
		if (methodDescriptor.isClinit()) {
			// report delta: addition of clinit method
			this.addDelta(
					typeDescriptor.getElementType(),
					IDelta.ADDED,
					IDelta.CLINIT,
					this.currentDescriptorRestrictions,
					methodDescriptor.access,
					this.classFile,
					typeDescriptor.name,
					Util.getDescriptorName(descriptor1));
			return;
		}
		int access = methodDescriptor.access;
		if (Util.isSynthetic(access)) {
			// we ignore synthetic method
			return;
		} else {
			String methodDisplayName = getMethodDisplayName(methodDescriptor, typeDescriptor);
			if (Util.isPublic(access) || Util.isProtected(access)) {
				if (methodDescriptor.isConstructor()) {
					this.addDelta(
							typeDescriptor.getElementType(),
							IDelta.ADDED,
							IDelta.CONSTRUCTOR,
							this.currentDescriptorRestrictions,
							access,
							this.classFile,
							getKeyForMethod(methodDescriptor, typeDescriptor),
							new String[] {Util.getDescriptorName(typeDescriptor), methodDisplayName});
				} else if (typeDescriptor.isAnnotation()) {
					if (methodDescriptor.defaultValue != null) {
						this.addDelta(
								typeDescriptor.getElementType(),
								IDelta.ADDED,
								IDelta.METHOD_WITH_DEFAULT_VALUE,
								this.currentDescriptorRestrictions,
								access,
								this.classFile,
								getKeyForMethod(methodDescriptor, typeDescriptor),
								new String[] {Util.getDescriptorName(typeDescriptor), methodDisplayName });
					} else {
						this.addDelta(
								typeDescriptor.getElementType(),
								IDelta.ADDED,
								IDelta.METHOD_WITHOUT_DEFAULT_VALUE,
								this.currentDescriptorRestrictions,
								typeDescriptor.access,
								this.classFile,
								getKeyForMethod(methodDescriptor, typeDescriptor),
								new String[] {Util.getDescriptorName(typeDescriptor), methodDisplayName });
					}
				} else {
					// check superclass
					// if null we need to walk the hierarchy of descriptor2
					TypeDescriptor typeDescriptor2 = this.descriptor2;
					boolean found = false;
					if (this.component2 != null) {
						String name = methodDescriptor.name;
						String descriptor = methodDescriptor.descriptor;
						if (this.descriptor1.isInterface()) {
							Set interfacesSet = getInterfacesSet(typeDescriptor2, this.component2, this.apiProfile2);
							if (interfacesSet != null) {
								for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
									TypeDescriptor superTypeDescriptor = (TypeDescriptor) iterator.next();
									MethodDescriptor methodDescriptor3 = getMethodDescriptor(superTypeDescriptor, name, descriptor);
									if (methodDescriptor3 == null) {
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
							Set superclassSet = getSuperclassSet(typeDescriptor2, this.component2, this.apiProfile2, true);
							if (superclassSet != null) {
								loop: for (Iterator iterator = superclassSet.iterator(); iterator.hasNext();) {
									TypeDescriptor superTypeDescriptor = (TypeDescriptor) iterator.next();
									MethodDescriptor methodDescriptor3 = getMethodDescriptor(superTypeDescriptor, name, descriptor);
									if (methodDescriptor3 == null) {
										continue;
									} else {
										int access3 = methodDescriptor3.access;
										if (Util.isPublic(access3)
												|| Util.isProtected(access3)) {
											// method has been move up in the hierarchy - report the delta and abort loop
											// TODO need to make the distinction between methods that need to be reimplemented and methods that don't
											found = true;
											break loop;
										}
									}
								}
							}
						}
					}
					this.addDelta(
							typeDescriptor.getElementType(),
							IDelta.ADDED,
							found ? IDelta.OVERRIDEN_METHOD : IDelta.METHOD,
							this.currentDescriptorRestrictions,
							methodDescriptor.access,
							this.classFile,
							getKeyForMethod(methodDescriptor, typeDescriptor),
							new String[] {Util.getDescriptorName(typeDescriptor), methodDisplayName });
				}
			} else {
				this.addDelta(
						typeDescriptor.getElementType(),
						IDelta.ADDED,
						methodDescriptor.isConstructor() ? IDelta.CONSTRUCTOR : IDelta.METHOD,
						this.currentDescriptorRestrictions,
						methodDescriptor.access,
						this.classFile,
						getKeyForMethod(methodDescriptor, typeDescriptor),
						new String[] {Util.getDescriptorName(typeDescriptor), methodDisplayName });
			}
		}
	}
	
	private String getKeyForMethod(MethodDescriptor methodDescriptor, TypeDescriptor typeDescriptor) {
		StringBuffer buffer = new StringBuffer();
		if (methodDescriptor.isConstructor()) {
			String name = typeDescriptor.name;
			int index = name.lastIndexOf('.');
			int dollarIndex = name.lastIndexOf('$');
			if (dollarIndex != -1 && typeDescriptor.isNestedType()) {
				buffer.append(typeDescriptor.name.substring(dollarIndex + 1));
			} else {
				buffer.append(typeDescriptor.name.substring(index + 1));
			}
		} else {
			buffer.append(methodDescriptor.name);
		}
		buffer.append(methodDescriptor.descriptor);
		return String.valueOf(buffer);
	}
}
