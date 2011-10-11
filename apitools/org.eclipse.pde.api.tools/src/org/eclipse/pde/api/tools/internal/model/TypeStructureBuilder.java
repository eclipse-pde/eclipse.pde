/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.api.tools.internal.model;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.pde.api.tools.internal.model.StubArchiveApiTypeContainer.ArchiveApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceAnnotationVisitor;

/**
 * Class adapter used to create an API type structure
 */
public class TypeStructureBuilder extends ClassAdapter {
	ApiType fType;
	IApiComponent fComponent;
	IApiTypeRoot fFile;

	/**
	 * Builds a type structure for a class file. Note that if an API 
	 * component is not specified, then some operations on the resulting
	 * {@link IApiType} will not be available (navigating super types,
	 * member types, etc).
	 * 
	 * @param cv class file visitor
	 * @param component originating API component or <code>null</code> if unknown
	 */
	TypeStructureBuilder(ClassVisitor cv, IApiComponent component, IApiTypeRoot file) {
		super(cv);
		fComponent = component;
		fFile = file;
	}
	
	/**
	 * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		StringBuffer simpleSig = new StringBuffer();
		simpleSig.append('L');
		simpleSig.append(name);
		simpleSig.append(';');
		String enclosingName = null;
		int index = name.lastIndexOf('$');
		if (index > -1) {
			enclosingName = name.substring(0, index).replace('/', '.');
		}
		int laccess = access;
		// TODO: inner types should be have enclosing type as parent instead of component
		if ((laccess & Opcodes.ACC_DEPRECATED) != 0) {
			laccess &= ~Opcodes.ACC_DEPRECATED;
			laccess |= ClassFileConstants.AccDeprecated;
		}
		fType = new ApiType(fComponent, name.replace('/', '.'), simpleSig.toString(), signature, laccess, enclosingName, fFile);
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
		super.visit(version, laccess, name, signature, superName, interfaces);
	}
	/**
	 * @see org.objectweb.asm.ClassAdapter#visitInnerClass(java.lang.String, java.lang.String, java.lang.String, int)
	 */
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		super.visitInnerClass(name, outerName, innerName, access);
		String currentName = name.replace('/', '.');
		if (currentName.equals(fType.getName())) {
			if (innerName == null) {
				fType.setAnonymous();
			}
			else if(outerName == null) {
				fType.setLocal();
				fType.setSimpleName(innerName);
			}
		}
		if (outerName != null && innerName != null) {
			// technically speaking innerName != null is not necessary, but this is a workaround for some
			// bogus synthetic types created by another compiler
			String currentOuterName = outerName.replace('/', '.');
			if (currentOuterName.equals(fType.getName())) {
				// this is a real type member defined in the descriptor (not just a reference to a type member)
				fType.addMemberType(currentName, access);
			} else if (currentName.equals(fType.getName())) {
				fType.setModifiers(access);
				fType.setSimpleName(innerName);
				fType.setMemberType();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassAdapter#visitOuterClass(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void visitOuterClass(String owner, String name, String desc) {
		fType.setEnclosingMethodInfo(name, desc);
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassAdapter#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
	 */
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		int laccess = access;
		if ((access & Opcodes.ACC_DEPRECATED) != 0) {
			laccess &= ~Opcodes.ACC_DEPRECATED;
			laccess |= ClassFileConstants.AccDeprecated;
		}
		fType.addField(name, desc, signature, laccess, value);
		return null;
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		String[] names = null;
		int laccess = access;
		if ((laccess & Opcodes.ACC_DEPRECATED) != 0) {
			laccess &= ~Opcodes.ACC_DEPRECATED;
			laccess |= ClassFileConstants.AccDeprecated;
		}
		if (exceptions != null && exceptions.length > 0) {
			names = new String[exceptions.length];
			for (int i = 0; i < names.length; i++) {
				names[i] = exceptions[i].replace('/', '.');
			}
		}
		final ApiMethod method = fType.addMethod(name, desc, signature, laccess, names);
		return new MethodAdapter(super.visitMethod(laccess, name, desc, signature, exceptions)) {
			public AnnotationVisitor visitAnnotation(String sig, boolean visible) {
				if (visible && "Ljava/lang/invoke/MethodHandle$PolymorphicSignature;".equals(sig)) { //$NON-NLS-1$
					method.isPolymorphic();
				}
				return super.visitAnnotation(sig, visible);
			}
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
	public static IApiType buildTypeStructure(byte[] bytes, IApiComponent component, IApiTypeRoot file) {
		TypeStructureBuilder visitor = new TypeStructureBuilder(new ClassNode(), component, file);
		try {
			ClassReader classReader = new ClassReader(bytes);
			classReader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
		} catch (ArrayIndexOutOfBoundsException e) {
			ApiPlugin.log(e);
		}
		return visitor.fType;
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
	public static void setEnclosingMethod(IApiType enclosingType, ApiType currentAnonymousLocalType) {
		IApiTypeRoot typeRoot = enclosingType.getTypeRoot();
		if (typeRoot instanceof AbstractApiTypeRoot) {
			AbstractApiTypeRoot abstractApiTypeRoot = (AbstractApiTypeRoot) typeRoot;
			EnclosingMethodSetter visitor = new EnclosingMethodSetter(new ClassNode(), currentAnonymousLocalType.getName());
			try {
				ClassReader classReader = new ClassReader(abstractApiTypeRoot.getContents());
				classReader.accept(visitor, ClassReader.SKIP_FRAMES);
			} catch (ArrayIndexOutOfBoundsException e) {
				ApiPlugin.log(e);
			} catch(CoreException e) {
				// bytes could not be retrieved for abstractApiTypeRoot
				ApiPlugin.log(e);
			}
			if (visitor.found) {
				currentAnonymousLocalType.setEnclosingMethodInfo(visitor.name, visitor.signature);
			}
		}
	}
	static class EnclosingMethodSetter extends ClassAdapter {
		String name;
		String signature;
		boolean found = false;
		String typeName;

		public EnclosingMethodSetter(ClassVisitor cv, String typeName) {
			super(cv);
			this.typeName = typeName.replace('.', '/');
		}
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if ("<clinit>".equals(name)) { //$NON-NLS-1$
				return null;
			}
			if (!this.found) {
				if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
					this.name = name;
					this.signature = desc;
					if (signature != null) {
						this.signature = signature;
					}
					MethodVisitor mv;
					if ("<init>".equals(name)) { //$NON-NLS-1$
						mv = new TypeNameFinderInConstructor(cv.visitMethod(access, name, desc, signature, exceptions), this);
					} else {
						mv = new TypeNameFinder(cv.visitMethod(access, name, desc, signature, exceptions), this);
					}
					return mv;
				}
			}
			return null;
		}
	}
	static class TypeNameFinder extends MethodAdapter {
		protected EnclosingMethodSetter setter;
		
		public TypeNameFinder(MethodVisitor mv, EnclosingMethodSetter enclosingMethodSetter) {
			super(mv);
			this.setter = enclosingMethodSetter;
		}
		public void visitTypeInsn(int opcode, String type) {
			if (setter.typeName.equals(type)) {
				setter.found = true;
			}
		}
	}
	static class TypeNameFinderInConstructor extends TypeNameFinder {
		int lineNumberStart;
		int matchingLineNumber;
		int currentLineNumber = -1;
		
		public TypeNameFinderInConstructor(MethodVisitor mv, EnclosingMethodSetter enclosingMethodSetter) {
			super(mv, enclosingMethodSetter);
		}
		/* (non-Javadoc)
		 * @see org.objectweb.asm.MethodAdapter#visitFieldInsn(int, java.lang.String, java.lang.String, java.lang.String)
		 */
		public void visitFieldInsn(int opcode, String owner, String name,
				String desc) {
			super.visitFieldInsn(opcode, owner, name, desc);
		}
		public void visitTypeInsn(int opcode, String type) {
			if (!setter.found && setter.typeName.equals(type)) {
				this.matchingLineNumber = this.currentLineNumber;
				setter.found = true;
			}
		}
		public void visitLineNumber(int line, Label start) {
			if (this.currentLineNumber == -1) {
				this.lineNumberStart = line;
			}
			this.currentLineNumber = line;
		}
		public void visitEnd() {
			if (setter.found) {
				// check that the line number is between the constructor bounds
				if (this.matchingLineNumber < this.lineNumberStart || this.matchingLineNumber > this.currentLineNumber) {
					setter.found = false;
				}
			}
		}
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Type structure builder for: ").append(fType.getName()); //$NON-NLS-1$
		buffer.append("\nBacked by file: ").append(fFile.getName()); //$NON-NLS-1$
		return buffer.toString();
	}

	public static IApiType buildStubTypeStructure(byte[] contents,
			IApiComponent apiComponent, ArchiveApiTypeRoot archiveApiTypeRoot) {
		// decode the byte[]
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(contents));
		ApiType type = null;
		try {
			Map pool = new HashMap();
			short currentVersion = inputStream.readShort(); // read file version (for now there is only one version)
			short poolSize = inputStream.readShort();
			for (int i = 0; i < poolSize; i++) {
				String readUtf = inputStream.readUTF();
				int index = inputStream.readShort();
				pool.put(new Integer(index), readUtf);
			}
			int access = 0;
			// access flag was added in version 2 of the stub format
			if (currentVersion >= 2) {
				access = inputStream.readChar();
			}
			int classIndex = inputStream.readShort();
			String name = (String) pool.get(new Integer(classIndex));
			StringBuffer simpleSig = new StringBuffer();
			simpleSig.append('L');
			simpleSig.append(name);
			simpleSig.append(';');
			type = new ApiType(apiComponent, name.replace('/', '.'), simpleSig.toString(), null, access, null, archiveApiTypeRoot);
			int superclassNameIndex = inputStream.readShort();
			if (superclassNameIndex != -1) {
				String superclassName = (String) pool.get(new Integer(superclassNameIndex));
				type.setSuperclassName(superclassName.replace('/', '.'));
			}
			int interfacesLength = inputStream.readShort();
			if (interfacesLength != 0) {
				String[] names = new String[interfacesLength];
				for (int i = 0; i < names.length; i++) {
					String interfaceName = (String) pool.get(new Integer(inputStream.readShort()));
					names[i] = interfaceName.replace('/', '.');
				}
				type.setSuperInterfaceNames(names);
			}
			int fieldsLength = inputStream.readShort();
			for (int i = 0; i < fieldsLength; i++) {
				String fieldName = (String) pool.get(new Integer(inputStream.readShort()));
				type.addField(fieldName, null, null, 0, null);
			}
			int methodsLength = inputStream.readShort();
			for (int i = 0; i < methodsLength; i++) {
				int isPolymorphic = 0;
				String methodSelector = (String) pool.get(new Integer(inputStream.readShort()));
				String methodSignature = (String) pool.get(new Integer(inputStream.readShort()));
				if (currentVersion == 3) {
					isPolymorphic = inputStream.readByte();
				}
				type.addMethod(methodSelector, methodSignature, null, isPolymorphic == 1 ? ApiMethod.Polymorphic : 0, null);
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				// ignore
			}
		}
		return type;
	}
}