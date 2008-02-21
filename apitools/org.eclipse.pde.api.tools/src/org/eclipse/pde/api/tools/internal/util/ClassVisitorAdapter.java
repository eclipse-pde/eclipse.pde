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
package org.eclipse.pde.api.tools.internal.util;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * An adapter for a class visitor. Can be subclassed to override desired methods.
 * 
 * @since 1.0.0
 */
public class ClassVisitorAdapter implements ClassVisitor {

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitAnnotation(java.lang.String, boolean)
	 */
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitAttribute(org.objectweb.asm.Attribute)
	 */
	public void visitAttribute(Attribute attr) {
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitEnd()
	 */
	public void visitEnd() {
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
	 */
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitInnerClass(java.lang.String, java.lang.String, java.lang.String, int)
	 */
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitOuterClass(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void visitOuterClass(String owner, String name, String desc) {
	}

	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassVisitor#visitSource(java.lang.String, java.lang.String)
	 */
	public void visitSource(String source, String debug) {
	}

}
