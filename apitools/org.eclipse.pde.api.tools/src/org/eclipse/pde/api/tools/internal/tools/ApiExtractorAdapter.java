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
package org.eclipse.pde.api.tools.internal.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;

public class ApiExtractorAdapter extends ClassAdapter {
	private List localCollector;
	private boolean ignore;
	private String name;
	private Set collector;

	public ApiExtractorAdapter(Set collector) {
		super(new EmptyVisitor());
		this.ignore = true;
		this.collector = collector;
		this.localCollector = new ArrayList();
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if ((access & Opcodes.ACC_PUBLIC) != 0) {
			this.ignore = false;
		}
		this.name = name;
	}
	/* (non-Javadoc)
	 * @see org.objectweb.asm.ClassAdapter#visitInnerClass(java.lang.String, java.lang.String, java.lang.String, int)
	 */
	public void visitInnerClass(String innerClassName, String outerName, String innerName, int access) {
		if (this.name.equals(innerClassName) && (outerName == null || innerName == null)) {
			// local class
			this.ignore = true;
		}
	}
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
			StringBuffer buffer = new StringBuffer(this.name);
			buffer.append('#').append(name).append(desc);
			this.collect(String.valueOf(buffer));
		}
		return null;
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
			StringBuffer buffer = new StringBuffer(this.name);
			buffer.append('#').append(name).append(desc);
			this.collect(String.valueOf(buffer));
		}
		return null;
	}
	private void collect(String value) {
		this.localCollector.add(value);
	}

	public void visitEnd() {
		if (!this.ignore) {
			this.collector.addAll(this.localCollector);
		}
	}
}
