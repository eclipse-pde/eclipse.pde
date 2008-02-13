package org.eclipse.pde.api.tools.internal.comparator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.signature.SignatureReader;

/*
 * TODO provide a way to link the right resource from a delta
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

	private static boolean isCheckedException(IApiProfile state, IApiComponent apiComponent, String exceptionName) {
		if (state == null) return true;
		try {
			String packageName = Util.getPackageName(exceptionName);
			IClassFile classFile = Util.getClassFile(
					state.resolvePackage(apiComponent, packageName),
					exceptionName);
			if (classFile != null) {
				// TODO should this be reported as a checked exception
				byte[] contents = classFile.getContents();
				TypeDescriptor typeDescriptor = new TypeDescriptor(contents);
				while (!Util.isJavaLangObject(typeDescriptor.name)) {
					String superName = typeDescriptor.superName;
					packageName = Util.getPackageName(superName);
					classFile = Util.getClassFile(
							state.resolvePackage(apiComponent, packageName),
							superName);
					if (classFile == null) {
						// TODO should we report this failure ?
						if (DEBUG) {
							System.err.println("CHECKED EXCEPTION LOOKUP: Could not find " + superName + " in profile " + state.getId() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

	IApiProfile apiState;
	IApiProfile apiState2;

	IClassFile classFile;
	IClassFile classFile2;
	
	IApiComponent component;
	IApiComponent component2;

	Delta delta;
	TypeDescriptor descriptor1;

	TypeDescriptor descriptor2;
	
	int visibilityModifiers;

	public ClassFileComparator(IClassFile classFile, IClassFile classFile2, IApiComponent component, IApiComponent component2, IApiProfile apiState, IApiProfile apiState2, int visibilityModifiers) {
		try {
			this.component = component;
			this.component2 = component2;
			this.descriptor1 = new TypeDescriptor(classFile.getContents());
			this.descriptor2 = new TypeDescriptor(classFile2.getContents());
			this.apiState = apiState;
			this.apiState2 = apiState2;
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
			this.apiState = apiState;
			this.apiState2 = apiState2;
			this.classFile = typeDescriptor.classFile;
			this.classFile2 = classFile2;
			this.visibilityModifiers = visibilityModifiers;
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}

	void addDelta(
			ElementDescriptor descriptor,
			ElementDescriptor descriptor2,
			int kind,
			int flags,
			IClassFile classFile,
			String key) {
		addDelta(descriptor, descriptor2, kind, flags, classFile, key, null);
	}
	void addDelta(
			ElementDescriptor descriptor,
			ElementDescriptor descriptor2,
			int kind,
			int flags,
			IClassFile classFile,
			String key,
			Object data) {
		int actualKind = kind;
		if (Util.isPrivate(descriptor2.access)
				|| Util.isDefault(descriptor2.access)) {
			switch(kind) {
				case IDelta.ADDED :
				case IDelta.ADDED_NOT_EXTEND_RESTRICTION :
				case IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION :
					actualKind = IDelta.ADDED_NON_VISIBLE;
					break;
				case IDelta.REMOVED :
					actualKind = IDelta.REMOVED_NON_VISIBLE;
					break;
				case IDelta.CHANGED :
					actualKind = IDelta.CHANGED_NON_VISIBLE;
			}
		}
		int actualFlags = flags;
		switch(flags) {
			case IDelta.METHOD :
				if (descriptor2.getElementType() == IDelta.CONSTRUCTOR_ELEMENT_TYPE) {
					actualFlags = IDelta.CONSTRUCTOR;
				}
		}
		this.addDelta(descriptor.getElementType(), actualKind, actualFlags, classFile, key, data);
	}

	void addDelta(
			ElementDescriptor descriptor,
			int kind,
			int flags,
			IClassFile classFile,
			String key,
			Object data) {
		this.addDelta(descriptor, descriptor, kind, flags, classFile, key, data);
	}

	void addDelta(
			ElementDescriptor descriptor,
			int kind,
			int flags,
			IClassFile classFile,
			String key) {
		this.addDelta(descriptor, descriptor, kind, flags, classFile, key, null);
	}

	void addDelta(IDelta delta) {
		this.delta.add(delta);
	}

	void addDelta(int elementType, int kind, int flags, IClassFile classFile, String key) {
		this.addDelta(elementType, kind, flags, classFile, key, null);
	}
	
	void addDelta(int elementType, int kind, int flags, IClassFile classFile, String key, Object data) {
		
		this.delta.add(new Delta(elementType, kind, flags, classFile, key, data));
	}
	
	private void checkSuperclass() {
		// check superclass set
		Set superclassSet1 = getSuperclassSet(this.descriptor1, this.component, this.apiState);
		Set superclassSet2 = getSuperclassSet(this.descriptor2, this.component2, this.apiState2);
		if (superclassSet1 == null) {
			if (superclassSet2 != null) {
				// this means the direct super class of descriptor1 is java.lang.Object
				this.addDelta(this.descriptor1, IDelta.ADDED, IDelta.SUPERCLASS, this.classFile, this.descriptor1.name);
				return;
			}
			// both types extends java.lang.Object
			return;
		}
		if (superclassSet2 == null) {
			// this means the direct super class of descriptor2 is java.lang.Object
			this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.SUPERCLASS, this.classFile, this.descriptor1.name);
			return;
		}
		for (Iterator iterator = superclassSet1.iterator(); iterator.hasNext();) {
			TypeDescriptor superclassTypeDescriptor = (TypeDescriptor) iterator.next();
			if (!superclassSet2.contains(superclassTypeDescriptor)) {
				this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.CONTRACTED_SUPERCLASS_SET, this.classFile, this.descriptor1.name);
				return;
			}
		}
		if (superclassSet1.size() < superclassSet2.size()) {
			this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.EXPANDED_SUPERCLASS_SET, this.classFile, this.descriptor1.name);
		}
		
		// TODO check super class if they are not checked anyway
		// case where an API type inherits from a non-API type that contains public methods/fields
	}

	private void checkSuperInterfaces() {
		Set superinterfacesSet1 = getInterfacesSet(this.descriptor1, this.component, this.apiState);
		Set superinterfacesSet2 = getInterfacesSet(this.descriptor2, this.component2, this.apiState2);

		if (superinterfacesSet1 == null) {
			if (superinterfacesSet2 != null) {
				this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.EXPANDED_SUPERINTERFACES_SET, this.classFile, this.descriptor1.name);
			}
		} else if (superinterfacesSet2 == null) {
			this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.CONTRACTED_SUPERINTERFACES_SET, this.classFile, this.descriptor1.name);
		} else {
			for (Iterator iterator = superinterfacesSet1.iterator(); iterator.hasNext();) {
				TypeDescriptor superInterfaceTypeDescriptor = (TypeDescriptor) iterator.next();
				if (!superinterfacesSet2.contains(superInterfaceTypeDescriptor)) {
					this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.CONTRACTED_SUPERINTERFACES_SET, this.classFile, this.descriptor1.name);
					return;
				}
			}
			if (superinterfacesSet1.size() < superinterfacesSet2.size()) {
				this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.EXPANDED_SUPERINTERFACES_SET, this.classFile, this.descriptor1.name);
			}
		}
	}

	private void checkTypeMembers() {
		Set typeMembers = this.descriptor1.typeMembers;
		Set typeMembers2 = this.descriptor2.typeMembers;
		if (typeMembers != null) {
			if (typeMembers2 == null) {
				for (Iterator iterator = typeMembers.iterator(); iterator.hasNext();) {
					MemberTypeDescriptor typeMember = (MemberTypeDescriptor ) iterator.next();
					this.addDelta(this.descriptor1, typeMember, IDelta.REMOVED, IDelta.TYPE_MEMBER, this.classFile, typeMember.name, typeMember.name.replace('$', '.'));
				}
				return;
			}
			// check removed or added type members
			List removedTypeMembers = new ArrayList();
			for (Iterator iterator = typeMembers.iterator(); iterator.hasNext();) {
				MemberTypeDescriptor typeMember = (MemberTypeDescriptor ) iterator.next();
				String typeMemberName = typeMember.name;
				if (!typeMembers2.remove(typeMember)) {
					removedTypeMembers.add(typeMember);
				} else {
					// check deltas inside the type member
					try {
						IClassFile memberType1 = this.component.findClassFile(typeMemberName);
						IClassFile memberType2 = this.component2.findClassFile(typeMemberName);
						ClassFileComparator comparator = new ClassFileComparator(memberType1, memberType2, this.component, this.component2, this.apiState, this.apiState2, this.visibilityModifiers);
						IDelta delta2 = comparator.getDelta();
						if (delta2 != null && delta2 != ApiComparator.NO_DELTA) {
							this.addDelta(delta2);
						}
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
				}
			}
			for (Iterator iterator = removedTypeMembers.iterator(); iterator.hasNext();) {
				MemberTypeDescriptor typeMember = (MemberTypeDescriptor) iterator.next();
				this.addDelta(this.descriptor1, typeMember, IDelta.REMOVED, IDelta.TYPE_MEMBER, this.classFile, typeMember.name, typeMember.name.replace('$', '.'));
			}
		}
		if (typeMembers2 == null) return;
		// report remaining types in type members2 as addition
		if (this.descriptor1.isInterface()) {
			if (RestrictionModifiers.isImplementRestriction(this.getCurrentTypeApiRestrictions())) {
				// Report delta as a breakage
				for (Iterator iterator = typeMembers2.iterator(); iterator.hasNext();) {
					MemberTypeDescriptor typeMember = (MemberTypeDescriptor) iterator.next();
					this.addDelta(this.descriptor1, IDelta.ADDED_IMPLEMENT_RESTRICTION, IDelta.TYPE_MEMBER, this.classFile, typeMember.name);
				}
				return;
			}
			for (Iterator iterator = typeMembers2.iterator(); iterator.hasNext();) {
				MemberTypeDescriptor typeMember = (MemberTypeDescriptor) iterator.next();
				this.addDelta(this.descriptor1, IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION, IDelta.TYPE_MEMBER, this.classFile, typeMember.name);
			}
		} else {
			if (RestrictionModifiers.isExtendRestriction(this.getCurrentTypeApiRestrictions())) {
				for (Iterator iterator = typeMembers2.iterator(); iterator.hasNext();) {
					MemberTypeDescriptor typeMember = (MemberTypeDescriptor) iterator.next();
					int access = typeMember.access;
					if (Util.isPublic(access)
							|| Util.isProtected(access)) {
						this.addDelta(this.descriptor1, IDelta.ADDED_EXTEND_RESTRICTION, IDelta.TYPE_MEMBER, this.classFile, typeMember.name);
					} else {
						this.addDelta(this.descriptor1, typeMember, IDelta.ADDED, IDelta.TYPE_MEMBER, this.classFile, typeMember.name);
					}
				}
				return;
			}
			for (Iterator iterator = typeMembers2.iterator(); iterator.hasNext();) {
				MemberTypeDescriptor typeMember = (MemberTypeDescriptor) iterator.next();
				int access = typeMember.access;
				if (Util.isPublic(access)
						|| Util.isProtected(access)) {
					this.addDelta(this.descriptor1, IDelta.ADDED_NOT_EXTEND_RESTRICTION, IDelta.TYPE_MEMBER, this.classFile, typeMember.name);
				} else {
					this.addDelta(this.descriptor1, typeMember, IDelta.ADDED, IDelta.TYPE_MEMBER, this.classFile, typeMember.name);
				}
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
					this.addDelta(elementDescriptor1, IDelta.ADDED, IDelta.TYPE_PARAMETERS, this.classFile, elementDescriptor1.name);
				} else if (signatureDescriptor2.getTypeArguments().length != 0) {
					this.addDelta(elementDescriptor1, IDelta.ADDED, IDelta.TYPE_ARGUMENTS, this.classFile, elementDescriptor1.name);
				}
			}
		} else if (signature2 == null) {
			// removed type parameters
			SignatureDescriptor signatureDescriptor = getSignatureDescritor(signature1);
			if (signatureDescriptor.getTypeParameterDescriptors().length != 0) {
				this.addDelta(elementDescriptor1, IDelta.REMOVED, IDelta.TYPE_PARAMETERS, this.classFile, elementDescriptor1.name);
			} else if (signatureDescriptor.getTypeArguments().length != 0) {
				this.addDelta(elementDescriptor1, IDelta.REMOVED, IDelta.TYPE_ARGUMENTS, this.classFile, elementDescriptor1.name);
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
				// report delta: binary incompatible
				this.addDelta(elementDescriptor1, IDelta.ADDED, IDelta.TYPE_PARAMETER, this.classFile, elementDescriptor1.name);
				return;
			} else if (typeParameterDescriptorsLength1 > typeParameterDescriptorsLength2) {
				this.addDelta(elementDescriptor1, IDelta.REMOVED, IDelta.TYPE_PARAMETER, this.classFile, elementDescriptor1.name, elementDescriptor1.name);
				return;
			}
			// same number of type parameter descriptors
			for (int i = 0; i < typeParameterDescriptorsLength1; i++) {
				TypeParameterDescriptor parameterDescriptor1 = typeParameterDescriptors1[i];
				TypeParameterDescriptor parameterDescriptor2 = typeParameterDescriptors2[i];
				String name = parameterDescriptor1.name;
				if (!name.equals(parameterDescriptor2.name)) {
					this.addDelta(elementDescriptor1, IDelta.CHANGED, IDelta.TYPE_PARAMETER_NAME, this.classFile, name);
				}
				if (parameterDescriptor1.classBound == null) {
					if (parameterDescriptor2.classBound != null) {
						// report delta added class bound of a type parameter
						this.addDelta(elementDescriptor1, IDelta.ADDED, IDelta.CLASS_BOUND, this.classFile, name);
					}
				} else if (parameterDescriptor2.classBound == null) {
					// report delta removed class bound of a type parameter
					this.addDelta(elementDescriptor1, IDelta.REMOVED, IDelta.CLASS_BOUND, this.classFile, name, name);
				} else if (!parameterDescriptor1.classBound.equals(parameterDescriptor2.classBound)) {
					// report delta changed class bound of a type parameter
					this.addDelta(elementDescriptor1, IDelta.CHANGED, IDelta.CLASS_BOUND, this.classFile, name);
				}
				List interfaceBounds1 = parameterDescriptor1.interfaceBounds;
				List interfaceBounds2 = parameterDescriptor2.interfaceBounds;
				if (interfaceBounds1 == null) {
					if (interfaceBounds2 != null) {
						// report delta added interface bounds
						this.addDelta(elementDescriptor1, IDelta.ADDED, IDelta.INTERFACE_BOUNDS, this.classFile, name);
					}
				} else if (interfaceBounds2 == null) {
					// report delta removed interface bounds
					this.addDelta(elementDescriptor1, IDelta.REMOVED, IDelta.INTERFACE_BOUNDS, this.classFile, name, name);
				} else if (interfaceBounds1.size() < interfaceBounds2.size()) {
					// report delta added some interface bounds
					this.addDelta(elementDescriptor1, IDelta.ADDED, IDelta.INTERFACE_BOUND, this.classFile, name);
				} else if (interfaceBounds1.size() > interfaceBounds2.size()) {
					// report delta removed some interface bounds
					this.addDelta(elementDescriptor1, IDelta.REMOVED, IDelta.INTERFACE_BOUND, this.classFile, name, name);
				} else {
					loop: for (int j = 0, max = interfaceBounds1.size(); j < max; j++) {
						if (!interfaceBounds1.get(j).equals(interfaceBounds2.get(j))) {
							// report delta: different interface bounds (or reordered interface bound)
							this.addDelta(elementDescriptor1, IDelta.CHANGED, IDelta.INTERFACE_BOUND, this.classFile, name);
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
					this.addDelta(elementDescriptor1, IDelta.CHANGED, IDelta.TYPE, this.classFile, elementDescriptor1.name);
					return;
				}
			}
		}
	}

	private void collectAllInterfaces(TypeDescriptor typeDescriptor, IApiComponent apiComponent, IApiProfile profile, Set set) {
		try {
			Set interfaces = typeDescriptor.interfaces;
			if (interfaces == null) return;
			for (Iterator iterator = interfaces.iterator(); iterator.hasNext();) {
				String interfaceName = (String) iterator.next();
				String packageName = Util.getPackageName(interfaceName);
				IApiComponent[] components = profile.resolvePackage(apiComponent, packageName);
				if (components == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERINTERFACES LOOKUP: Could not find package " + packageName + " in profile " + profile.getId() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					continue;
				}
				IClassFile superinterface = Util.getClassFile(components, interfaceName);
				if (superinterface == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERINTERFACES LOOKUP: Could not find interface " + interfaceName + " in profile " + profile.getId() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					continue;
				}
				TypeDescriptor typeDescriptor2 = new TypeDescriptor(superinterface.getContents());
				set.add(typeDescriptor2);
				collectAllInterfaces(typeDescriptor2, apiComponent, profile, set);
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
	}

	public IDelta getDelta() {
		try {
			this.delta = new Delta();
			// check visibility
			final IApiDescription referenceApiDescription = component2.getApiDescription();
			IApiAnnotations elementDescription = referenceApiDescription.resolveAnnotations(null, this.descriptor2.handle);
			if (elementDescription != null) {
				int visibility = elementDescription.getVisibility();
				if ((visibility & visibilityModifiers) == 0) {
					// visibility has been changed
					if (((visibility & VisibilityModifiers.API) == 0)
							&& ((visibilityModifiers & VisibilityModifiers.API) != 0)) {
						// was API and is no longer API
						return new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.TYPE, classFile, this.descriptor1.name, this.descriptor1.name);
					}
					// no delta
					return new Delta(IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.CHANGED_VISIBILITY, IDelta.TYPE, classFile, this.descriptor1.name, null);
				}
			}
			// first make sure that we compare interface with interface, class with class,
			// annotation with annotation and enum with enums
			int typeAccess = this.descriptor1.access;
			int typeAccess2 = this.descriptor2.access;

			if (Util.isProtected(typeAccess)) {
				if (Util.isPrivate(typeAccess2) || Util.isDefault(typeAccess2)) {
					// report delta - decrease access: protected to default or private
					this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.DECREASE_ACCESS, this.classFile, this.descriptor1.name);
				} else if (Util.isPublic(typeAccess2)) {
					// report delta - increase access: protected to public
					this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, this.descriptor1.name);
				}
			} else if (Util.isPublic(typeAccess)
					&& (Util.isProtected(typeAccess2)
							|| Util.isPrivate(typeAccess2)
							|| Util.isDefault(typeAccess2))) {
				// report delta - decrease access: public to protected, default or private
				this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.DECREASE_ACCESS, this.classFile, this.descriptor1.name);
			} else if (Util.isDefault(typeAccess)
					&& (Util.isPublic(typeAccess2)
							|| Util.isProtected(typeAccess2))) {
				this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, this.descriptor1.name);
			} else if (Util.isPrivate(typeAccess)
					&& (Util.isDefault(typeAccess2)
							|| Util.isPublic(typeAccess2)
							|| Util.isProtected(typeAccess2))) {
				this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, this.descriptor1.name);
			}

			if ((visibilityModifiers & VisibilityModifiers.API) != 0) {
				if (!Util.isPublic(typeAccess2) && !Util.isProtected(typeAccess2)) {
					return this.delta.isEmpty() ? ApiComparator.NO_DELTA : this.delta;
				}
			}
			
			if (Util.isAnnotation(typeAccess)) {
				if (!Util.isAnnotation(typeAccess2)) {
					if (Util.isInterface(typeAccess2)) {
						// report conversion from annotation to interface
						this.addDelta(IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_INTERFACE, this.classFile, this.descriptor1.name);
					} else if (Util.isEnum(typeAccess2)) {
						// report conversion from annotation to enum
						this.addDelta(IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_ENUM, this.classFile, this.descriptor1.name);
					} else {
						// report conversion from annotation to class
						this.addDelta(IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_CLASS, this.classFile, this.descriptor1.name);
					}
					return this.delta;
				}
			} else if (Util.isInterface(typeAccess)) {
				if (Util.isAnnotation(typeAccess2)) {
					// conversion from interface to annotation
					this.addDelta(IDelta.INTERFACE_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_ANNOTATION, this.classFile, this.descriptor1.name);
					return this.delta;
				} else if (!Util.isInterface(typeAccess2)) {
					if (Util.isEnum(typeAccess2)) {
						// conversion from interface to enum
						this.addDelta(IDelta.INTERFACE_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_ENUM, this.classFile, this.descriptor1.name);
					} else {
						// conversion from interface to class
						this.addDelta(IDelta.INTERFACE_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_CLASS, this.classFile, this.descriptor1.name);
					}
					return this.delta;
				}
			} else if (Util.isEnum(typeAccess)) {
				if (!Util.isEnum(typeAccess2)) {
					if (Util.isAnnotation(typeAccess2)) {
						// report conversion from enum to annotation
						this.addDelta(IDelta.ENUM_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_ANNOTATION, this.classFile, this.descriptor1.name);
					} else if (Util.isInterface(typeAccess2)) {
						// report conversion from enum to interface
						this.addDelta(IDelta.ENUM_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_INTERFACE, this.classFile, this.descriptor1.name);
					} else {
						// report conversion from enum to class
						this.addDelta(IDelta.ENUM_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_CLASS, this.classFile, this.descriptor1.name);
					}
					return this.delta;
				}
			} else if (!Util.isClass(typeAccess2)) {
				if (Util.isAnnotation(typeAccess2)) {
					// report conversion from class to annotation
					this.addDelta(IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_ANNOTATION, this.classFile, this.descriptor1.name);
				} else if (Util.isInterface(typeAccess2)) {
					// report conversion from class to interface
					this.addDelta(IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_INTERFACE, this.classFile, this.descriptor1.name);
				} else {
					// report conversion from class to enum
					this.addDelta(IDelta.CLASS_ELEMENT_TYPE, IDelta.CHANGED, IDelta.TO_ENUM, this.classFile, this.descriptor1.name);
				}
				return this.delta;
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
					this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.ABSTRACT_TO_NON_ABSTRACT, this.classFile, this.descriptor1.name);
				}
			} else if (Util.isAbstract(typeAccess2)){
				// report delta - changed from non-abstract to abstract
				this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.NON_ABSTRACT_TO_ABSTRACT, this.classFile, this.descriptor1.name);
			}

			if (Util.isFinal(typeAccess)) {
				if (!Util.isFinal(typeAccess2)) {
					// report delta - changed from final to non-final
					this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.FINAL_TO_NON_FINAL, this.classFile, this.descriptor1.name);
				}
			} else if (Util.isFinal(typeAccess2)){
				// report delta - changed from non-final to final
				this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.NON_FINAL_TO_FINAL, this.classFile, this.descriptor1.name);
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
				this.addDelta(this.descriptor1, IDelta.REMOVED_NON_VISIBLE, IDelta.FIELD, this.classFile, name, name);
			} else {
				boolean found = false;
				if (this.component2 != null) {
					if (this.descriptor1.isInterface()) {
						Set interfacesSet = getInterfacesSet(this.descriptor2, this.component2, this.apiState2);
						if (interfacesSet != null) {
							for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
								TypeDescriptor superTypeDescriptor = (TypeDescriptor) iterator.next();
								FieldDescriptor fieldDescriptor3 = getFieldDescriptor(superTypeDescriptor, name);
								if (fieldDescriptor3 == null) {
									continue;
								} else {
									// interface method can only be public
									// method has been move up in the hierarchy - report the delta and abort loop
									this.addDelta(this.descriptor1, IDelta.REMOVED, IDelta.FIELD_MOVED_UP, this.classFile, name, name);
									found = true;
									break;
								}
							}
						}
					} else {
						Set superclassSet = getSuperclassSet(this.descriptor2, this.component2, this.apiState2);
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
										this.addDelta(this.descriptor1, IDelta.REMOVED, IDelta.FIELD_MOVED_UP, this.classFile, name);
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
						// report delta (removal of an enum constant - not binary compatible)
						this.addDelta(this.descriptor1, IDelta.REMOVED, IDelta.ENUM_CONSTANT, this.classFile, name, name);
						return;
					} else if (Util.isProtected(access)) {
						// check the subclass restriction of descriptor1
						if (RestrictionModifiers.isExtendRestriction(this.getCurrentTypeApiRestrictions())) {
							// subclass = false
							// Report delta as binary compatible
							// TODO might need to be reviewed since type in the same package can access this field
							this.addDelta(this.descriptor1, IDelta.REMOVED_EXTEND_RESTRICTION, IDelta.FIELD, this.classFile, name, name);
							return;
						}
					}
					// removing a public field is a breakage
					this.addDelta(this.descriptor1, IDelta.REMOVED, IDelta.FIELD, this.classFile, name, name);
				}
			}
			return;
		}
		int deltaChangedFlags = IDelta.CHANGED;
		if (Util.isPrivate(access) || Util.isDefault(access)) {
			deltaChangedFlags = IDelta.CHANGED_NON_VISIBLE;
		}
		if (!fieldDescriptor.descriptor.equals(fieldDescriptor2.descriptor)) {
			// report delta
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.TYPE, this.classFile, name);
		} else {
			// check type parameters
			String signature1 = fieldDescriptor.signature;
			String signature2 = fieldDescriptor2.signature;
			checkGenericSignature(signature1, signature2, fieldDescriptor, fieldDescriptor2);
		}
		if (fieldDescriptor.value != null) {
			if (fieldDescriptor2.value == null) {
				// report delta - removal of constant value
				if (Util.isPrivate(access) || Util.isDefault(access)) {
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.REMOVED_NON_VISIBLE, IDelta.VALUE, this.classFile, name, fieldDescriptor.value);
				} else {
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.REMOVED, IDelta.VALUE, this.classFile, name, fieldDescriptor.value);
				}
			} else if (!fieldDescriptor.value.equals(fieldDescriptor2.value)) {
				// report delta - modified constant value
				if (isProtectedWithExtendRestriction(access)) {
					// consider static protected field in a class that cannot be subclassed as NON_VISIBLE
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED_NON_VISIBLE, IDelta.VALUE, this.classFile, name);
				} else {
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.VALUE, this.classFile, name);
				}
			}
		} else if (fieldDescriptor2.value != null) {
			// report delta
			if (Util.isPrivate(access) || Util.isDefault(access)) {
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.ADDED_NON_VISIBLE, IDelta.VALUE, this.classFile, name);
			} else {
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.ADDED, IDelta.VALUE, this.classFile, name);
			}
		}
		int access2 = fieldDescriptor2.access;
		if (Util.isProtected(access)) {
			if (Util.isPrivate(access2) || Util.isDefault(access2)) {
				// report delta - decrease access: protected to default or private
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.DECREASE_ACCESS, this.classFile, name);
			} else if (Util.isPublic(access2)) {
				// report delta - increase access: protected to public
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, name);
			}
		} else if (Util.isPublic(access)
				&& (Util.isProtected(access2)
						|| Util.isPrivate(access2)
						|| Util.isDefault(access2))) {
			// report delta - decrease access: public to protected, default or private
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.DECREASE_ACCESS, this.classFile, name);
		} else if (Util.isPrivate(access)
				&& (Util.isProtected(access2)
						|| Util.isDefault(access2)
						|| Util.isPublic(access2))) {
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, name);
		} else if (Util.isDefault(access)
				&& (Util.isProtected(access2)
						|| Util.isPublic(access2))) {
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, name);
		}
		if (Util.isFinal(access)) {
			if (!Util.isFinal(access2)) {
				if (!Util.isStatic(access2)) {
					// report delta - final to non-final for a non static field
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.FINAL_TO_NON_FINAL_NON_STATIC, this.classFile, name);
				} else if (fieldDescriptor.value != null) {
					// report delta - final to non-final for a static field with a compile time constant
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT, this.classFile, name);
				} else {
					// report delta - final to non-final for a static field with no compile time constant
					this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.FINAL_TO_NON_FINAL_STATIC_NON_CONSTANT, this.classFile, name);
				}
			}
		} else if (Util.isFinal(access2)) {
			// report delta - non-final to final
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.NON_FINAL_TO_FINAL, this.classFile, name);
		}
		if (Util.isStatic(access)) {
			if (!Util.isStatic(access2)) {
				// report delta - static to non-static
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.STATIC_TO_NON_STATIC, this.classFile, name);
			}
		} else if (Util.isStatic(access2)) {
			// report delta - non-static to static
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.NON_STATIC_TO_STATIC, this.classFile, name);
		}
		if (Util.isTransient(access)) {
			if (!Util.isTransient(access2)) {
				// report delta - transient to non-transient
				this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.TRANSIENT_TO_NON_TRANSIENT, this.classFile, name);
			}
		} else if (Util.isTransient(access2)) {
			// report delta - non-tansient to transient
			this.addDelta(IDelta.FIELD_ELEMENT_TYPE, deltaChangedFlags, IDelta.NON_TRANSIENT_TO_TRANSIENT, this.classFile, name);
		}
	}

	private boolean isProtectedWithExtendRestriction(int access) {
		return Util.isProtected(access) && RestrictionModifiers.isExtendRestriction(this.getCurrentTypeApiRestrictions());
	}
	
	private int getCurrentTypeApiRestrictions() {
		try {
			IApiDescription apiDescription = this.component2.getApiDescription();
			if (!apiDescription.containsAnnotatedElements()) return RestrictionModifiers.NO_RESTRICTIONS;
			IApiAnnotations resolvedAPIDescription = apiDescription.resolveAnnotations(null, this.descriptor2.handle);
			if (resolvedAPIDescription != null) {
				IApiDescription apiDescription2 = this.component.getApiDescription();
				if (apiDescription2.containsAnnotatedElements()) {
					IApiAnnotations referenceAPIDescription = apiDescription2.resolveAnnotations(null, this.descriptor1.handle);
					if (referenceAPIDescription == null || referenceAPIDescription.getRestrictions() != resolvedAPIDescription.getRestrictions()) {
						// report different restrictions
						this.addDelta(this.descriptor1, IDelta.CHANGED, IDelta.RESTRICTIONS, this.classFile, this.descriptor1.name);
					}
				}
				return resolvedAPIDescription.getRestrictions();
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return RestrictionModifiers.NO_RESTRICTIONS;
	}

	private void getDeltaForMethodDescriptor(MethodDescriptor methodDescriptor) {
		int access = methodDescriptor.access;
		if (Util.isSynthetic(access)) {
			// we ignore synthetic methods
			return;
		}
		String name = methodDescriptor.name;
		String descriptor = methodDescriptor.descriptor;
		String key = name + descriptor;
		MethodDescriptor methodDescriptor2 = getMethodDescriptor(this.descriptor2, name, descriptor);
		if (methodDescriptor2 == null) {
			if (methodDescriptor.isClinit()) {
				// report delta: removal of a clinit method
				this.addDelta(this.descriptor1, IDelta.REMOVED, IDelta.CLINIT, this.classFile, this.descriptor1.name, this.descriptor1.name);
				return;
			} else if (Util.isPrivate(access)
					|| Util.isDefault(access)) {
				this.addDelta(this.descriptor1, methodDescriptor, IDelta.REMOVED, IDelta.METHOD, this.classFile, this.descriptor1.name, getMethodDisplayName(methodDescriptor, this.descriptor1));
				return;
			}
			// if null we need to walk the hierarchy of descriptor2
			TypeDescriptor typeDescriptor = this.descriptor2;
			boolean found = false;
			if (this.component2 != null) {
				if (this.descriptor1.isInterface()) {
					Set interfacesSet = getInterfacesSet(typeDescriptor, this.component2, this.apiState2);
					if (interfacesSet != null) {
						for (Iterator iterator = interfacesSet.iterator(); iterator.hasNext();) {
							TypeDescriptor superTypeDescriptor = (TypeDescriptor) iterator.next();
							MethodDescriptor methodDescriptor3 = getMethodDescriptor(superTypeDescriptor, name, descriptor);
							if (methodDescriptor3 == null) {
								continue;
							} else {
								// interface method can only be public
								// method has been move up in the hierarchy - report the delta and abort loop
								this.addDelta(this.descriptor1, IDelta.REMOVED, IDelta.METHOD_MOVED_UP, this.classFile, this.descriptor1.name, getMethodDisplayName(methodDescriptor, this.descriptor1));
								found = true;
								break;
							}
						}
					}
				} else {
					Set superclassSet = getSuperclassSet(typeDescriptor, this.component2, this.apiState2);
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
									this.addDelta(this.descriptor1, IDelta.REMOVED, IDelta.METHOD_MOVED_UP, this.classFile, this.descriptor1.name, getMethodDisplayName(methodDescriptor, this.descriptor1));
									found = true;
									break loop;
								}
							}
						}
					}
				}
			}
			if (!found) {
				if (Util.isPublic(access)) {
					if (this.descriptor1.isAnnotation()) {
						this.addDelta(
								this.descriptor1,
								IDelta.REMOVED,
								methodDescriptor.defaultValue != null ? IDelta.METHOD_WITH_DEFAULT_VALUE : IDelta.METHOD_WITHOUT_DEFAULT_VALUE,
								this.classFile,
								this.descriptor1.name,
								getMethodDisplayName(methodDescriptor, this.descriptor1));
					} else {
						this.addDelta(
								this.descriptor1,
								IDelta.REMOVED,
								methodDescriptor.isConstructor() ? IDelta.CONSTRUCTOR : IDelta.METHOD,
								this.classFile,
								this.descriptor1.name,
								getMethodDisplayName(methodDescriptor, this.descriptor1));
					}
				} else {
					// protected access
					if (RestrictionModifiers.isExtendRestriction(this.getCurrentTypeApiRestrictions())) {
						// subclass = false
						// Report delta as binary compatible
						// TODO might need to be reviewed since type in the same package can access this field
						this.addDelta(
								this.descriptor1,
								IDelta.REMOVED_EXTEND_RESTRICTION,
								methodDescriptor.isConstructor() ? IDelta.CONSTRUCTOR : IDelta.METHOD,
								this.classFile,
								this.descriptor1.name,
								getMethodDisplayName(methodDescriptor, this.descriptor1));
						return;
					}
					this.addDelta(
							this.descriptor1,
							IDelta.REMOVED,
							methodDescriptor.isConstructor() ? IDelta.CONSTRUCTOR : IDelta.METHOD,
							this.classFile,
							this.descriptor1.name,
							getMethodDisplayName(methodDescriptor, this.descriptor1));
				}
			}
			return;
		}
		if (methodDescriptor.exceptions != null) {
			if (methodDescriptor2.exceptions == null) {
				// check all exception in method descriptor to see if they are checked or unchecked exceptions
				loop: for (Iterator iterator = methodDescriptor.exceptions.iterator(); iterator.hasNext(); ) {
					String exceptionName = ((String) iterator.next()).replace('/', '.');
					if (isCheckedException(this.apiState, this.component, exceptionName)) {
						// report delta - removal of checked exception
						// TODO should we continue the loop for all remaining exceptions
						this.addDelta(methodDescriptor, IDelta.REMOVED, IDelta.CHECKED_EXCEPTION, this.classFile, key, exceptionName);
						break loop;
					} else {
						// report delta - removal of unchecked exception
						this.addDelta(methodDescriptor, IDelta.REMOVED, IDelta.UNCHECKED_EXCEPTION, this.classFile, key, exceptionName);
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
						if (isCheckedException(this.apiState, this.component, exceptionName)) {
							// report delta - removal of checked exception
							// TODO should we continue the loop for all remaining exceptions
							this.addDelta(methodDescriptor, IDelta.REMOVED, IDelta.CHECKED_EXCEPTION, this.classFile, key, exceptionName);
							break loop;
						} else {
							// report delta - removal of unchecked exception
							this.addDelta(methodDescriptor, IDelta.REMOVED, IDelta.UNCHECKED_EXCEPTION, this.classFile, key, exceptionName);
						}
					}
				}
				loop: for (Iterator iterator = methodDescriptor2.exceptions.iterator(); iterator.hasNext(); ) {
					String exceptionName = ((String) iterator.next()).replace('/', '.');
					if (isCheckedException(this.apiState2, this.component2, exceptionName)) {
						// report delta - addition of checked exception
						// TODO should we continue the loop for all remaining exceptions
						this.addDelta(methodDescriptor, IDelta.ADDED, IDelta.CHECKED_EXCEPTION, this.classFile, key);
						break loop;
					} else {
						// report delta - addition of unchecked exception
						this.addDelta(methodDescriptor, IDelta.ADDED, IDelta.UNCHECKED_EXCEPTION, this.classFile, key);
					}
				}
			}
		} else if (methodDescriptor2.exceptions != null) {
			// check all exception in methoddescriptor to see if they are checked or unchecked exceptions
			loop: for (Iterator iterator = methodDescriptor2.exceptions.iterator(); iterator.hasNext(); ) {
				String exceptionName = ((String) iterator.next()).replace('/', '.');
				if (isCheckedException(this.apiState2, this.component2, exceptionName)) {
					// report delta - addition of checked exception
					this.addDelta(methodDescriptor, IDelta.ADDED, IDelta.CHECKED_EXCEPTION, this.classFile, key);
					// TODO should we continue the loop for all remaining exceptions
					break loop;
				} else {
					// report delta - addition of unchecked exception
					this.addDelta(methodDescriptor, IDelta.ADDED, IDelta.UNCHECKED_EXCEPTION, this.classFile, key);
				}
			}
		}
		int access2 = methodDescriptor2.access;
		if (Util.isVarargs(access)) {
			if (!Util.isVarargs(access2)) {
				// report delta: conversion from T... to T[] - break compatibility 
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.VARARGS_TO_ARRAY, this.classFile, key);
			}
		} else if (Util.isVarargs(access2)) {
			// report delta: conversion from T[] to T... binary compatible
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.ARRAY_TO_VARARGS, this.classFile, key);
		}
		if (Util.isProtected(access)) {
			if (Util.isPrivate(access2) || Util.isDefault(access2)) {
				// report delta - decrease access: protected to default or private
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.DECREASE_ACCESS, this.classFile, key);
			} else if (Util.isPublic(access2)) {
				// report delta - increase access: protected to public
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, key);
			}
		} else if (Util.isPublic(access)
				&& (Util.isProtected(access2)
						|| Util.isPrivate(access2)
						|| Util.isDefault(access2))) {
			// report delta - decrease access: public to protected, default or private
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.DECREASE_ACCESS, this.classFile, key);
		} else if (Util.isDefault(access)
				&& (Util.isPublic(access2)
						|| Util.isProtected(access2))) {
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, key);
		} else if (Util.isPrivate(access)
				&& (Util.isDefault(access2)
						|| Util.isPublic(access2)
						|| Util.isProtected(access2))) {
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.INCREASE_ACCESS, this.classFile, key);
		}
		if (Util.isAbstract(access)) {
			if (!Util.isAbstract(access2)) {
				// report delta - changed from abstract to non-abstract
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.ABSTRACT_TO_NON_ABSTRACT, this.classFile, key);
			}
		} else if (Util.isAbstract(access2)){
			// report delta - changed from non-abstract to abstract
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.NON_ABSTRACT_TO_ABSTRACT, this.classFile, key);
		}
		if (Util.isFinal(access)) {
			if (!Util.isFinal(access2)) {
				// report delta - changed from final to non-final
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.FINAL_TO_NON_FINAL, this.classFile, key);
			}
		} else if (Util.isFinal(access2)
				&& (Util.isProtected(access2) || Util.isPublic(access2))) {
			// report delta - changed from non-final to final
			if (RestrictionModifiers.isExtendRestriction(this.getCurrentTypeApiRestrictions())) {
				// subclass = false
				// Report delta as binary compatible
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.NON_FINAL_TO_FINAL, this.classFile, key);
			} else {
				// Report delta as a breakage
				this.addDelta(methodDescriptor, IDelta.CHANGED_NOT_EXTEND_RESTRICTION, IDelta.NON_FINAL_TO_FINAL, this.classFile, key);
			}
		}
		if (Util.isStatic(access)) {
			if (!Util.isStatic(access2)) {
				// report delta: change from static to non-static
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.STATIC_TO_NON_STATIC, this.classFile, key);
			}
		} else if (Util.isStatic(access2)){
			// report delta: change from non-static to static
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.NON_STATIC_TO_STATIC, this.classFile, key);
		}
		if (Util.isNative(access)) {
			if (!Util.isNative(access2)) {
				// report delta: change from native to non-native
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.NATIVE_TO_NON_NATIVE, this.classFile, key);
			}
		} else if (Util.isNative(access2)){
			// report delta: change from non-native to native
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.NON_NATIVE_TO_NATIVE, this.classFile, key);
		}
		if (Util.isSynchronized(access)) {
			if (!Util.isSynchronized(access2)) {
				// report delta: change from synchronized to non-synchronized
				this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.SYNCHRONIZED_TO_NON_SYNCHRONIZED, this.classFile, key);
			}
		} else if (Util.isSynchronized(access2)){
			// report delta: change from non-synchronized to synchronized
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.NON_SYNCHRONIZED_TO_SYNCHRONIZED, this.classFile, key);
		}
		// check type parameters
		String signature1 = methodDescriptor.signature;
		String signature2 = methodDescriptor2.signature;
		checkGenericSignature(signature1, signature2, methodDescriptor, methodDescriptor2);
		
		if (methodDescriptor.defaultValue == null) {
			if (methodDescriptor2.defaultValue != null) {
				// report delta : default value has been added - compatible
				this.addDelta(methodDescriptor, IDelta.ADDED, IDelta.ANNOTATION_DEFAULT_VALUE, this.classFile, key);
			}
		} else if (methodDescriptor2.defaultValue == null) {
			// report delta : default value has been removed - binary incompatible
			this.addDelta(methodDescriptor, IDelta.REMOVED, IDelta.ANNOTATION_DEFAULT_VALUE, this.classFile, key, getMethodDisplayName(methodDescriptor, this.descriptor1));
		} else if (!methodDescriptor.defaultValue.equals(methodDescriptor2.defaultValue)) {
			// report delta: default value has changed
			this.addDelta(methodDescriptor, IDelta.CHANGED, IDelta.ANNOTATION_DEFAULT_VALUE, this.classFile, key);
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
		TypeDescriptor descriptor = typeDescriptor;
		if (descriptor.interfaces == null) return null;
		HashSet set = new HashSet();
		collectAllInterfaces(typeDescriptor, apiComponent, profile, set);
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
			methodName = typeDescriptor.name;
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
		TypeDescriptor descriptor = typeDescriptor;
		String superName = descriptor.superName;
		if (Util.isJavaLangObject(superName)) {
			return null;
		}
		HashSet set = new HashSet();
		IApiComponent sourceComponent = apiComponent; 
		try {
			loop: while (!Util.isJavaLangObject(superName)) {
				String packageName = Util.getPackageName(superName);
				IApiComponent[] components = profile.resolvePackage(sourceComponent, packageName);
				if (components == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERCLASS LOOKUP: Could not find package " + packageName + " in profile " + profile.getId() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					break loop;
				}
				sourceComponent = Util.getComponent(components, superName);
				if (sourceComponent == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERCLASS LOOKUP: Could not find package " + packageName + " in profile " + profile.getId() + " from component " + apiComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					break loop;
				}
				IClassFile superclass = sourceComponent.findClassFile(superName);
				if (superclass == null) {
					// TODO should we report this failure ?
					if (DEBUG) {
						System.err.println("SUPERCLASS LOOKUP: Could not find class " + superName + " in profile " + profile.getId() + " from component " + sourceComponent.getId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
			// report delta (addition of an enum constant - binary compatible
			this.addDelta(descriptor, IDelta.ADDED, IDelta.ENUM_CONSTANT, this.classFile, name);
		} else if (Util.isPublic(access)
				|| Util.isProtected(access)) {
			if (this.descriptor1.isInterface()) {
				if (RestrictionModifiers.isImplementRestriction(this.getCurrentTypeApiRestrictions())) {
					// Report delta as binary compatible
					this.addDelta(descriptor, IDelta.ADDED_IMPLEMENT_RESTRICTION, IDelta.FIELD, this.classFile, name);
					return;
				}
				// Report delta as a breakage
				this.addDelta(descriptor, IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION, IDelta.FIELD, this.classFile, name);
			} else {
				if (RestrictionModifiers.isExtendRestriction(this.getCurrentTypeApiRestrictions())) {
					// subclass = false
					// Report delta as binary compatible
					this.addDelta(descriptor, IDelta.ADDED_EXTEND_RESTRICTION, IDelta.FIELD, this.classFile, name);
					return;
				}
				if (Util.isFinal(descriptor.access)) {
					// final class cannot be implicitely subclassed
					this.addDelta(descriptor, IDelta.ADDED_EXTEND_RESTRICTION, IDelta.FIELD, this.classFile, name);
				} else if (Util.isStatic(access)) {
					// Report delta as binary compatible - this is not source compatible
					this.addDelta(descriptor, IDelta.ADDED_NOT_EXTEND_RESTRICTION_STATIC, IDelta.FIELD, this.classFile, name);
				} else {
					// Report delta as a breakage
					this.addDelta(descriptor, IDelta.ADDED_NOT_EXTEND_RESTRICTION, IDelta.FIELD, this.classFile, name);
				}
			}
		} else {
			this.addDelta(descriptor, fieldDescriptor, IDelta.ADDED, IDelta.FIELD, this.classFile, name);
		}
	}
	private void reportMethodAddition(MethodDescriptor methodDescriptor, TypeDescriptor typeDescriptor) {
		if (methodDescriptor.isClinit()) {
			// report delta: addition of clinit method
			this.addDelta(typeDescriptor, IDelta.ADDED, IDelta.CLINIT, this.classFile, typeDescriptor.name);
			return;
		}
		int access = methodDescriptor.access;
		if (Util.isSynthetic(access)) {
			// we ignore synthetic method
			return;
		} else if (Util.isPublic(access) || Util.isProtected(access)) {
			if (methodDescriptor.isConstructor()) {
				this.addDelta(typeDescriptor, IDelta.ADDED, IDelta.CONSTRUCTOR, this.classFile, getKeyForMethod(methodDescriptor));
			} else if (typeDescriptor.isAnnotation()) {
				if (methodDescriptor.defaultValue != null) {
					this.addDelta(typeDescriptor, IDelta.ADDED, IDelta.METHOD_WITH_DEFAULT_VALUE, this.classFile, getKeyForMethod(methodDescriptor));
				} else {
					this.addDelta(typeDescriptor, IDelta.ADDED, IDelta.METHOD_WITHOUT_DEFAULT_VALUE, this.classFile, getKeyForMethod(methodDescriptor));
				}
			} else if (typeDescriptor.isInterface()) {
				// this is an interface
				if (RestrictionModifiers.isImplementRestriction(this.getCurrentTypeApiRestrictions())) {
					// implements = false
					// Report delta as binary compatible
					this.addDelta(typeDescriptor, IDelta.ADDED_IMPLEMENT_RESTRICTION, IDelta.METHOD, this.classFile, getKeyForMethod(methodDescriptor));
					return;
				}
				// Report delta as a breakage
				this.addDelta(typeDescriptor, IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION, IDelta.METHOD, this.classFile, getKeyForMethod(methodDescriptor));
			} else {
				// this is a class
				if (RestrictionModifiers.isExtendRestriction(this.getCurrentTypeApiRestrictions())) {
					// subclass = false
					// Report delta as binary compatible
					this.addDelta(
							typeDescriptor,
							Util.isPublic(access) ? IDelta.ADDED_EXTEND_RESTRICTION : IDelta.ADDED_NON_VISIBLE,
							IDelta.METHOD,
							this.classFile,
							getKeyForMethod(methodDescriptor));
					return;
				}
				if (Util.isAbstract(access)) {
					// Report delta as a breakage
					this.addDelta(typeDescriptor, IDelta.ADDED_NOT_EXTEND_RESTRICTION, IDelta.METHOD, this.classFile, getKeyForMethod(methodDescriptor));
				} else {
					// consider this as ok
					this.addDelta(typeDescriptor, IDelta.ADDED_EXTEND_RESTRICTION, IDelta.METHOD, this.classFile, getKeyForMethod(methodDescriptor));
				}
			}
		} else {
			this.addDelta(typeDescriptor, methodDescriptor, IDelta.ADDED_NON_VISIBLE, methodDescriptor.isConstructor() ? IDelta.CONSTRUCTOR : IDelta.METHOD, this.classFile, getKeyForMethod(methodDescriptor));
		}
	}
	
	private String getKeyForMethod(MethodDescriptor methodDescriptor) {
		StringBuffer buffer = new StringBuffer();
		buffer
			.append(methodDescriptor.name)
			.append(methodDescriptor.descriptor);
		return String.valueOf(buffer);
	}
}
