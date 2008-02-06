/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.comparator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * This represents the type contained in a class file. It is used to create the deltas.
 */
public class TypeDescriptor extends ElementDescriptor {
	public static final int ANONYMOUS = 1;
	public static final int LOCAL = 2;
	public static final int MEMBER = 4;
	
	Map fields;
	Set interfaces;
	Map methods;
	String signature;
	Set typeMembers;

	String superName;
	int version;
	public IClassFile classFile;
	public int bits;

	public TypeDescriptor(IClassFile classFile) throws CoreException {
		this.classFile = classFile;
		this.fields = new HashMap();
		this.methods = new HashMap();
		initialize(classFile.getContents());
	}

	TypeDescriptor(byte[] bytes) {
		this.fields = new HashMap();
		this.methods = new HashMap();
		initialize(bytes);
	}

	public void addTypeMember(String memberName, int access) {
		if (this.typeMembers == null) {
			this.typeMembers = new HashSet();
		}
		this.typeMembers.add(new MemberTypeDescriptor(memberName, access));
	}

	private void initialize(byte[] bytes) {
		ClassFileDescriptorBuilder visitor = new ClassFileDescriptorBuilder(new ClassNode(), this);
		try {
			ClassReader classReader = new ClassReader(bytes);
			classReader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
		} catch (ArrayIndexOutOfBoundsException e) {
			ApiPlugin.log(e);
		}
	}

	public void initialize(int version,
			int access,
			String name,
			String signature,
			String superName,
			String[] interfaces) {
		
		this.access = access;
		this.name = name.replace('/', '.');
		this.handle = Factory.typeDescriptor(this.name);
		this.signature = signature;
		if (superName != null) {
			this.superName = superName.replace('/', '.');
		}
		if (interfaces != null && interfaces.length != 0) {
			this.interfaces = new HashSet();
			for (int i = 0, max = interfaces.length; i < max; i++) {
				this.interfaces.add(interfaces[i].replace('/', '.'));
			}
		}
	}

	public boolean isAnnotation() {
		return (this.access & Opcodes.ACC_ANNOTATION) != 0;
	}

	public boolean isEnum() {
		return (this.access & Opcodes.ACC_ENUM) != 0;
	}

	public boolean isInterface() {
		return (this.access & Opcodes.ACC_INTERFACE) != 0;
	}

	public boolean isNestedType() {
		return (this.bits & (ANONYMOUS | LOCAL | MEMBER)) != 0;
	}
	
	public void setAnonymous() {
		this.bits |= ANONYMOUS;
	}

	public void setLocal() {
		this.bits |= LOCAL;
	}

	public void setMember() {
		this.bits |= MEMBER;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer
			.append("Type : access(") //$NON-NLS-1$
			.append(this.access)
			.append(") ") //$NON-NLS-1$
			.append(this.name);
		if (this.superName != null) {
			buffer
				.append(" superclass: ") //$NON-NLS-1$
				.append(this.superName);
		}
		if (this.interfaces != null) {
			buffer.append(" interfaces : "); //$NON-NLS-1$
			int length = this.interfaces.size();
			if (length != 0) {
				int i = 0;
				for (Iterator iterator = this.interfaces.iterator(); iterator.hasNext(); ) {
					if (i > 0) buffer.append(',');
					i++;
					buffer.append(iterator.next());
				}
			} else {
				buffer.append("none"); //$NON-NLS-1$
			}
		}
		buffer.append(';').append(Util.LINE_DELIMITER);
		if (this.signature != null) {
			buffer
				.append(" Signature : ") //$NON-NLS-1$
				.append(this.signature).append(Util.LINE_DELIMITER);
		}
		buffer.append(Util.LINE_DELIMITER).append("Methods : ").append(Util.LINE_DELIMITER); //$NON-NLS-1$
		for (Iterator iterator = this.methods.values().iterator(); iterator.hasNext();) {
			Object next = iterator.next();
			if (next instanceof List) {
				List list = (List) next;
				for (Iterator iterator2 = list.iterator(); iterator2.hasNext(); ) {
					buffer.append(iterator2.next());
				}
			} else {
				buffer.append(next);
			}
		}
		buffer.append(Util.LINE_DELIMITER).append("Fields : ").append(Util.LINE_DELIMITER); //$NON-NLS-1$
		for (Iterator iterator = this.fields.values().iterator(); iterator.hasNext();) {
			Object next = iterator.next();
			if (next instanceof List) {
				List list = (List) next;
				for (Iterator iterator2 = list.iterator(); iterator2.hasNext(); ) {
					buffer.append(iterator2.next());
				}
			} else {
				buffer.append(next);
			}
		}
		return String.valueOf(buffer);
	}
	
	int getElementType() {
		if ((this.access & Opcodes.ACC_ENUM) != 0) {
			return IDelta.ENUM_ELEMENT_TYPE;
		}
		if ((this.access & Opcodes.ACC_ANNOTATION) != 0) {
			return IDelta.ANNOTATION_ELEMENT_TYPE;
		}
		if ((this.access & Opcodes.ACC_INTERFACE) != 0) {
			return IDelta.INTERFACE_ELEMENT_TYPE;
		}
		return IDelta.CLASS_ELEMENT_TYPE;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof TypeDescriptor) {
			TypeDescriptor typeDescriptor = (TypeDescriptor) obj;
			return this.name.equals(typeDescriptor.name);
		}
		return false;
	}
	
	public int hashCode() {
		return this.name.hashCode();
	}
}