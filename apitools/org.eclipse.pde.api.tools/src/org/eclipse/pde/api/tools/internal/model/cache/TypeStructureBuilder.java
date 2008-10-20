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

package org.eclipse.pde.api.tools.internal.model.cache;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.pde.api.tools.internal.model.ApiMethod;
import org.eclipse.pde.api.tools.internal.model.ApiType;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceAnnotationVisitor;

/**
 * Class adapter used to create an API type structure
 */
class TypeStructureBuilder extends ClassAdapter {
	ApiType fType;
	IApiComponent fComponent;
	IClassFile fFile;

	/**
	 * Builds a type structure for a class file. Note that if an API 
	 * component is not specified, then some operations on the resulting
	 * {@link IApiType} will not be available (navigating super types,
	 * member types, etc).
	 * 
	 * @param cv class file visitor
	 * @param component originating API component or <code>null</code> if unknown
	 */
	TypeStructureBuilder(ClassVisitor cv, IApiComponent component, IClassFile file) {
		super(cv);
		fComponent = component;
		fFile = file;
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		StringBuffer simpleSig = new StringBuffer();
		simpleSig.append('L');
		simpleSig.append(name);
		simpleSig.append(';');
		String enclosingName = null;
		int index = name.lastIndexOf('$');
		if (index > -1) {
			enclosingName = name.substring(0, index);
		}
		fType = new ApiType(fComponent, name.replace('/', '.'), simpleSig.toString(), signature, access, enclosingName, fFile);
		if (superName != null) {
			fType.setSuperclassName(superName.replace('/', '.'));
		}
		if (interfaces != null && interfaces.length > 0) {
			String[] names = new String[interfaces.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = interfaces[i].replace('/', '.');
			}
			fType.setSuperInterfaceNames(names);
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		super.visitInnerClass(name, outerName, innerName, access);
		String currentName = name.replace('/', '.');
		if (fType == null) {
			// TODO: signature? generic signature?
			fType = new ApiType(fComponent, currentName, null, null, access, outerName, fFile);
			// this is a nested type
			if (outerName == null) {
				// this is a local or an anonymous type
				if (innerName == null) {
					this.fType.setAnonymous();
				} else {
					this.fType.setLocal();
				}
			}
		} else if (outerName != null && innerName != null) {
			// technically speaking innerName != null is not necessary, but this is a workaround for some
			// bogus synthetic types created by another compiler
			String currentOuterName = outerName.replace('/', '.');
			if (currentOuterName.equals(fType.getName())) {
				// this is a real type member defined in the descriptor (not just a reference to a type member)
				fType.addMemberType(currentName, access);
			} else if (currentName.equals(fType.getName())) {
				fType.setModifiers(access);
			}
		}
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		fType.addField(name, desc, signature, access, value);
		return null;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		String[] names = null;
		if (exceptions != null && exceptions.length > 0) {
			names = new String[exceptions.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = exceptions[i].replace('/', '.');
			}
		}
		final ApiMethod method = fType.addMethod(name, desc, signature, access, names);
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
						String def = String.valueOf(stringWriter.getBuffer());
						method.setDefaultValue(def);
					}
				};
			}
		};
	}
	
	/**
	 * Builds a type structure with the given .class file bytes in the specified
	 * API component.
	 * 
	 * @param bytes class file bytes
	 * @param component originating API component
	 * @param file associated class file
	 * @return
	 */
	public static IApiType buildTypeStructure(byte[] bytes, IApiComponent component, IClassFile file) {
		TypeStructureBuilder visitor = new TypeStructureBuilder(new ClassNode(), component, file);
		try {
			ClassReader classReader = new ClassReader(bytes);
			classReader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
		} catch (ArrayIndexOutOfBoundsException e) {
			ApiPlugin.log(e);
		}
		return visitor.fType;
	}
}