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
package org.eclipse.pde.api.tools.internal.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.model.cache.MethodKey;
import org.eclipse.pde.api.tools.internal.model.cache.TypeStructureCache;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IReference;
import org.eclipse.pde.api.tools.internal.util.ClassFileResult;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.Opcodes;

import com.ibm.icu.text.MessageFormat;

/**
 * Base implementation of {@link IApiType}
 * 
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ApiType extends ApiMember implements IApiType {

	/**
	 * API component or <code>null</code>
	 */
	private IApiComponent fComponent;
	
	private String fSuperclassName;
	private String[] fSuperInterfaceNames;
	private String fEnclosingTypeName;
	private int fBits = 0;
	
	private static final int LOCAL_TYPE = 1;
	private static final int ANONYMOUS_TYPE = 2;
	
	private static final IApiMethod[] EMPTY_METHODS = new IApiMethod[0];
	private static final IApiField[] EMPTY_FIELDS = new IApiField[0];
	private static final IApiType[] EMPTY_TYPES = new IApiType[0];
	
	/**
	 * Maps field name to field element.
	 */
	private Map fFields;
	/**
	 * Maps method name/signature pair to method element.
	 */
	private Map fMethods;
	
	/**
	 * Map of member type names to class file (or null untill resolved)
	 */
	private Map fMemberTypes;
	
	/**
	 * Cached descriptor
	 */
	private IReferenceTypeDescriptor fHandle;
	
	/**
	 * Cached superclass or <code>null</code>
	 */
	private IApiType fSuperclass;
	
	/**
	 * Cached super interfaces or <code>null</code>
	 */
	private IApiType[] fSuperInterfaces;
	
	/**
	 * Creates an API type. Note that if an API component is not specified,
	 * then some operations will not be available (navigating super types,
	 * member types, etc).
	 * 
	 * @param component originating API component or <code>null</code> if unknown
	 * @param name
	 * @param signature
	 * @param genericSig
	 * @param flags
	 * @param enclosingName
	 */
	public ApiType(IApiComponent parent, String name, String signature, String genericSig, int flags, String enclosingName) {
		//TODO parent is either APIType or APiComponent
		super(null, name, signature, genericSig, IApiElement.TYPE, flags);
		fComponent = parent;
		fEnclosingTypeName = enclosingName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.model.ApiMember#getApiComponent()
	 */
	public IApiComponent getApiComponent() {
		return fComponent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#extractReferences(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IReference[] extractReferences(int referenceMask, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getField(java.lang.String)
	 */
	public IApiField getField(String name) {
		if (fFields != null) {
			return (IApiField) fFields.get(name);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getFields()
	 */
	public IApiField[] getFields() {
		if (fFields != null) {
			return (IApiField[]) fFields.values().toArray(new IApiField[fFields.size()]);
		}
		return EMPTY_FIELDS;
	}
	
	/**
	 * Used when building a type structure.
	 * 
	 * @param name method name
	 * @param signature method signature
	 * @param genericSig
	 * @param modifiers method modifiers
	 * @param exceptions names of thrown exceptions
	 */
	public ApiMethod addMethod(String name, String signature, String genericSig, int modifiers, String[] exceptions) {
		if (fMethods == null) {
			fMethods = new HashMap();
		}
		ApiMethod method = new ApiMethod(this, name, signature, genericSig, modifiers, exceptions);
		fMethods.put(new MethodKey(name, signature), method);
		return method;
	}

	/**
	 * Used when building a type structure.
	 * 
	 * @param name field name
	 * @param signature field signature
	 * @param genericSig
	 * @param modifiers field modifiers
	 * @param value constant value or <code>null</code> if none
	 */
	public ApiField addField(String name, String signature, String genericSig, int modifiers, Object value) {
		if (fFields == null) {
			fFields = new HashMap();
		}
		ApiField field = new ApiField(this, name, signature, genericSig, modifiers, value);
		fFields.put(name, field);
		return field;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getMethod(java.lang.String, java.lang.String)
	 */
	public IApiMethod getMethod(String name, String signature) {
		if (fMethods != null) {
			return (IApiMethod) fMethods.get(new MethodKey(name, signature));
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getMethods()
	 */
	public IApiMethod[] getMethods() {
		if (fMethods != null) {
			return (IApiMethod[]) fMethods.values().toArray(new IApiMethod[fMethods.size()]);
		}
		return EMPTY_METHODS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getSuperInterfaceNames()
	 */
	public String[] getSuperInterfaceNames() {
		return fSuperInterfaceNames;
	}
	
	public void setSuperInterfaceNames(String[] names) {
		fSuperInterfaceNames = names;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getSuperInterfaces()
	 */
	public IApiType[] getSuperInterfaces() throws CoreException {
		String[] names = getSuperInterfaceNames();
		if (names == null) {
			return null;
		}
		if (fSuperInterfaces == null) {
			IApiType[] interfaces = new IApiType[names.length];
			for (int i = 0; i < interfaces.length; i++) {
				interfaces[i] = resolveType(names[i]);
				if (interfaces[i] == null) {
					throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
							MessageFormat.format(Messages.ApiType_0, new String[]{names[i], getName()})));
				}
			}
			fSuperInterfaces = interfaces;
		}
		return fSuperInterfaces;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getSuperclass()
	 */
	public IApiType getSuperclass() throws CoreException {
		String name = getSuperclassName();
		if (name == null) {
			return null;
		}
		if (fSuperclass == null) {
			fSuperclass = resolveType(name);
			if (fSuperclass == null) {
				throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
					MessageFormat.format(Messages.ApiType_1, new String[]{name, getName()})));
			}
		}
		return fSuperclass;
	}
	
	/**
	 * Resolves and returns the specified fully qualified type name or <code>null</code>
	 * if none.
	 * 
	 * @param qName qualified name
	 * @return type or <code>null</code>
	 * @throws CoreException if unable to resolve
	 */
	private IApiType resolveType(String qName) throws CoreException {
		if (getApiComponent() == null) {
			requiresApiComponent();
		}
		String packageName = Util.getPackageName(qName);
		IApiComponent[] components = getApiComponent().getProfile().
			resolvePackage(getApiComponent(), packageName);
		ClassFileResult result = Util.getComponent(components, qName);
		if (result != null) {
			return TypeStructureCache.getTypeStructure(result.getClassFile(), result.getComponent());
		}
		return null;
	}
	
	/**
	 * Throws an exception due to the fact an API component was not provided when this type
	 * was created and is now required to perform navigation or resolution.
	 * 
	 * @throws CoreException
	 */
	private void requiresApiComponent() throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
			Messages.ApiType_2));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getSuperclassName()
	 */
	public String getSuperclassName() {
		return fSuperclassName;
	}
	
	public void setSuperclassName(String superName) {
		fSuperclassName = superName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isAnnotation()
	 */
	public boolean isAnnotation() {
		return (getModifiers() & Opcodes.ACC_ANNOTATION) != 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isAnonymous()
	 */
	public boolean isAnonymous() {
		return (fBits & ANONYMOUS_TYPE) != 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isLocal()
	 */
	public boolean isLocal() {
		return (fBits & LOCAL_TYPE) != 0;
	}
	
	/**
	 * Used when building a type structure.
	 */
	public void setAnonymous() {
		fBits |= ANONYMOUS_TYPE;
	}
	
	/**
	 * Used when building a type structure.
	 */
	public void setLocal() {
		fBits |= LOCAL_TYPE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isClass()
	 */
	public boolean isClass() {
		return (getModifiers() & (
				Opcodes.ACC_ANNOTATION |
				Opcodes.ACC_ENUM |
				Opcodes.ACC_INTERFACE)) == 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isEnum()
	 */
	public boolean isEnum() {
		return (getModifiers() & Opcodes.ACC_ENUM) != 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isInterface()
	 */
	public boolean isInterface() {
		return (getModifiers() & Opcodes.ACC_INTERFACE) != 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isMemberType()
	 */
	public boolean isMemberType() {
		return fEnclosingTypeName != null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#getHandle()
	 */
	public IMemberDescriptor getHandle() {
		if (fHandle == null) {
			fHandle = Util.getType(getName());
		}
		return fHandle;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IApiType) {
			IApiType type = (IApiType) obj;
			if (getApiComponent() == null) {
				return type.getApiComponent() == null &&
					getName().equals(type.getName());
			}
			return getApiComponent().equals(type.getApiComponent()) &&
				getName().equals(type.getName());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (getApiComponent() == null) {
			return getName().hashCode();
		}
		return getApiComponent().hashCode() + getName().hashCode();
	}
	
	/**
	 * Used when building a type structure.
	 * 
	 * @param name member type name
	 */
	public void addMemberType(String name, int modifiers) {
		if (fMemberTypes == null) {
			fMemberTypes = new HashMap();
		} 
		int index = name.lastIndexOf('$');
		String simpleName = name.substring(index + 1);
		fMemberTypes.put(simpleName, null);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getMemberType(java.lang.String)
	 */
	public IApiType getMemberType(String simpleName) throws CoreException {
		if (fMemberTypes == null) {
			return null;
		}
		if (getApiComponent() == null) {
			requiresApiComponent();
		}
		if (fMemberTypes.containsKey(simpleName)) {
			IClassFile file =  (IClassFile) fMemberTypes.get(simpleName);
			if (file == null) {
				// resolve
				StringBuffer qName = new StringBuffer();
				qName.append(getName());
				qName.append('$');
				qName.append(simpleName);
				file = getApiComponent().findClassFile(qName.toString());
				if (file == null) {
					throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
							MessageFormat.format(Messages.ApiType_3,
							new String[]{simpleName, getName()})));
				}
				fMemberTypes.put(simpleName, file);
			}
			return TypeStructureCache.getTypeStructure(file, getApiComponent());
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getMemberTypes()
	 */
	public IApiType[] getMemberTypes() throws CoreException {
		if (fMemberTypes == null) {
			return EMPTY_TYPES;
		}
		IApiType[] members = new IApiType[fMemberTypes.size()];
		Iterator iterator = fMemberTypes.keySet().iterator();
		int index = 0;
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			members[index] = getMemberType(name); 
			index++;
		}
		return members;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer
			.append("Type : access(") //$NON-NLS-1$
			.append(getModifiers())
			.append(") ") //$NON-NLS-1$
			.append(getName());
		if (getSuperclassName() != null) {
			buffer
				.append(" superclass: ") //$NON-NLS-1$
				.append(getSuperclassName());
		}
		if (getSuperInterfaceNames() != null) {
			buffer.append(" interfaces : "); //$NON-NLS-1$
			if (getSuperInterfaceNames().length > 0) {
				for (int i = 0; i < getSuperInterfaceNames().length; i++) {
					if (i > 0) buffer.append(',');
					buffer.append(getSuperInterfaceNames()[i]);
				}
			} else {
				buffer.append("none"); //$NON-NLS-1$
			}
		}
		buffer.append(';').append(Util.LINE_DELIMITER);
		if (getGenericSignature() != null) {
			buffer
				.append(" Signature : ") //$NON-NLS-1$
				.append(getGenericSignature()).append(Util.LINE_DELIMITER);
		}
		buffer.append(Util.LINE_DELIMITER).append("Methods : ").append(Util.LINE_DELIMITER); //$NON-NLS-1$
		IApiMethod[] methods = getMethods();
		for (int i = 0; i < methods.length; i++) {
			buffer.append(methods[i]);
		}
		buffer.append(Util.LINE_DELIMITER).append("Fields : ").append(Util.LINE_DELIMITER); //$NON-NLS-1$
		IApiField[] fields = getFields();
		for (int i = 0; i < fields.length; i++) {
			buffer.append(fields[i]);
		}
		return String.valueOf(buffer);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getSimpleName()
	 */
	public String getSimpleName() {
		String name = getName();
		int index = name.lastIndexOf('$');
		if (index == -1) {
			index = name.lastIndexOf('.');
		}
		if (index != -1) {
			return name.substring(index + 1);
		}
		return name;
	}
}
