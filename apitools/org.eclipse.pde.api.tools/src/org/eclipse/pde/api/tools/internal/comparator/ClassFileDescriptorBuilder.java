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

package org.eclipse.pde.api.tools.internal.comparator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.TraceAnnotationVisitor;

/**
 * Class adapter used to create all elements from a class file.
 */
class ClassFileDescriptorBuilder extends ClassAdapter {
	TypeDescriptor descriptor;

	ClassFileDescriptorBuilder(ClassVisitor cv, TypeDescriptor descriptor) {
		super(cv);
		this.descriptor = descriptor;
	}
	
	private void addDescriptor(Map map, String name, Object descriptor) {
		Object object = map.get(name);
		if (object != null) {
			// already a method with the same descriptor
			if (object instanceof List) {
				((List) object).add(descriptor);
			} else {
				ArrayList list = new ArrayList();
				list.add(object);
				list.add(descriptor);
				map.put(name, list);
			}
		} else {
			map.put(name, descriptor);
		}
	}
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.descriptor.initialize(version, access, name, signature, superName, interfaces);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		super.visitInnerClass(name, outerName, innerName, access);
		String currentName = name.replace('/', '.');
		if (currentName.equals(this.descriptor.name)) {
			this.descriptor.access = access;
			// this is a nested type
			if (outerName == null) {
				// this is a local or an anonymous type
				if (innerName == null) {
					this.descriptor.setAnonymous();
				} else {
					this.descriptor.setLocal();
				}
			} else {
				this.descriptor.setMember();
			}
		} else if (outerName != null && innerName != null) {
			// technically speaking innerName != null is not necessary, but this is a workaround for some
			// boggus synthetic types created by javac
			String currentOuterName = outerName.replace('/', '.');
			if (currentOuterName.equals(this.descriptor.name)) {
				// this is a real type member defined in the descriptor (not just a reference to a type member)
				this.descriptor.addTypeMember(currentName, access);
			}
		}
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		FieldDescriptor fieldDescriptor = new FieldDescriptor(access, name, desc, signature, value);
		fieldDescriptor.handle = Factory.fieldDescriptor(this.descriptor.name, name);
		addDescriptor(this.descriptor.fields, name, fieldDescriptor);
		return null;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		final MethodDescriptor methodDescriptor = new MethodDescriptor(access, name, desc, signature, exceptions);
		methodDescriptor.handle = Factory.methodDescriptor(this.descriptor.name, name, Util.dequalifySignature(desc));
		addDescriptor(this.descriptor.methods, name, methodDescriptor);
		return new MethodAdapter(super.visitMethod(access, name, desc, signature, exceptions)) {
			public AnnotationVisitor visitAnnotationDefault() {
				return new TraceAnnotationVisitor() {
					public void visitEnd() {
						super.visitEnd();
						StringWriter stringWriter = new StringWriter();
						PrintWriter writer = new PrintWriter(stringWriter);
						print(writer);
						writer.flush();
						writer.close();
						methodDescriptor.defaultValue = String.valueOf(stringWriter.getBuffer());
					}
				};
			}
		};
	}
}