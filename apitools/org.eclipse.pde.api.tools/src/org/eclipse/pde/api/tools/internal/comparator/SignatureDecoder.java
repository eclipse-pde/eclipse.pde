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

import org.objectweb.asm.signature.SignatureVisitor;

/**
 * This class is used to decode a generic signature for class or method
 */
final class SignatureDecoder implements SignatureVisitor {
	static final int CLASS_BOUND = 1;
	static final int DEFAULT = 0;
	static final int INTERFACE_BOUND = 2;
	static final int SUPER_CLASS = 3;
	static final int NORMAL_TYPE_ARGUMENT = 4;
	static final int EXTENDS_TYPE_ARGUMENT = 5;
	static final int SUPER_TYPE_ARGUMENT = 6;

	int mode = DEFAULT;
	SignatureDescriptor signatureDescriptor;

	public SignatureDecoder(SignatureDescriptor signatureDescriptor) {
		this.signatureDescriptor = signatureDescriptor;
	}
	public SignatureVisitor visitArrayType() {
		return this;
	}

	public void visitBaseType(char descriptor) {
		// nothing to do
	}

	public SignatureVisitor visitClassBound() {
		this.mode = CLASS_BOUND;
		return this;
	}

	public void visitClassType(String name) {
		String classTypeName = name.replace('/', '.');
		switch(this.mode) {
			case CLASS_BOUND :
				this.signatureDescriptor.setClassBound(classTypeName);
				break;
			case INTERFACE_BOUND :
				this.signatureDescriptor.addInterfaceBound(classTypeName);
				break;
			case SUPER_CLASS :
				this.signatureDescriptor.setSuperclass(classTypeName);
				break;
			case EXTENDS_TYPE_ARGUMENT :
			case SUPER_TYPE_ARGUMENT :
			case NORMAL_TYPE_ARGUMENT :
				this.signatureDescriptor.addTypeArgument(classTypeName);
				break;
		}
		this.mode = DEFAULT;
	}

	public void visitEnd() {
		// nothing to do
	}

	public SignatureVisitor visitExceptionType() {
		// nothing to do
		return this;
	}

	public void visitFormalTypeParameter(String name) {
		this.signatureDescriptor.addTypeParameterDescriptor(name);
	}

	public void visitInnerClassType(String name) {
	}

	public SignatureVisitor visitInterface() {
		return this;
	}

	public SignatureVisitor visitInterfaceBound() {
		this.mode = INTERFACE_BOUND;
		return this;
	}

	public SignatureVisitor visitParameterType() {
		return this;
	}

	public SignatureVisitor visitReturnType() {
		return this;
	}

	public SignatureVisitor visitSuperclass() {
		this.mode = SUPER_CLASS;
		return this;
	}

	public void visitTypeArgument() {
	}

	public SignatureVisitor visitTypeArgument(char wildcard) {
		switch(wildcard) {
			case SignatureVisitor.EXTENDS :
				this.mode= EXTENDS_TYPE_ARGUMENT;
				break;
			case SignatureVisitor.SUPER :
				this.mode= SUPER_TYPE_ARGUMENT;
				break;
			case SignatureVisitor.INSTANCEOF :
				this.mode= NORMAL_TYPE_ARGUMENT;
		}
		return this;
	}

	public void visitTypeVariable(String name) {
	}
}