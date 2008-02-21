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

class SignatureDescriptor {
	private static final TypeParameterDescriptor[] EMPTY_TYPE_PARAMETER_DESCRIPTORS = new TypeParameterDescriptor[0];
	private static final String[] EMPTY_TYPE_ARGUMENTS = new String[0];
	static final int INITIAL_SIZE = 1;
	
	TypeParameterDescriptor currentTypeParameterDescriptor;
	String superClass;
	TypeParameterDescriptor[] typeParameterDescriptors;
	int typeParameterDescriptorsCounter;
	String[] typeArguments;
	int typeArgumentsCounter;
	
	public SignatureDescriptor() {
	}
	
	public void addInterfaceBound(String bound) {
		this.currentTypeParameterDescriptor.addInterfaceBound(bound);
	}

	public void addTypeArgument(String typeArgument) {
		if (this.typeArguments == null) {
			this.typeArguments = new String[INITIAL_SIZE];
			this.typeArgumentsCounter = 0;
		} else {
			int length = typeArguments.length; 
			if (length == this.typeArgumentsCounter) {
				// resize
				System.arraycopy(this.typeArguments, 0, (this.typeArguments = new String[length * 2]), 0, length);
			}
		}
		this.typeArguments[this.typeArgumentsCounter++] = typeArgument;
	}

	public void addTypeParameterDescriptor(String name) {
		if (this.typeParameterDescriptors == null) {
			this.typeParameterDescriptors = new TypeParameterDescriptor[INITIAL_SIZE];
			this.typeParameterDescriptorsCounter = 0;
		} else {
			int length = this.typeParameterDescriptors.length;
			if (this.typeParameterDescriptorsCounter == length) {
				System.arraycopy(this.typeParameterDescriptors, 0, (this.typeParameterDescriptors = new TypeParameterDescriptor[length * 2]), 0, length);
			}
		}
		TypeParameterDescriptor typeParameterDescriptor = new TypeParameterDescriptor(name);
		this.currentTypeParameterDescriptor = typeParameterDescriptor;
		this.typeParameterDescriptors[this.typeParameterDescriptorsCounter++] = typeParameterDescriptor;
	}

	public TypeParameterDescriptor[] getTypeParameterDescriptors() {
		if (this.typeParameterDescriptors == null) {
			return EMPTY_TYPE_PARAMETER_DESCRIPTORS;
		}
		int length = this.typeParameterDescriptors.length;
		if (this.typeParameterDescriptorsCounter != length) {
			System.arraycopy(this.typeParameterDescriptors, 0, (this.typeParameterDescriptors = new TypeParameterDescriptor[this.typeParameterDescriptorsCounter]), 0, this.typeParameterDescriptorsCounter);
		}
		return this.typeParameterDescriptors;
	}

	public String[] getTypeArguments() {
		if (this.typeArguments == null) {
			return EMPTY_TYPE_ARGUMENTS;
		}
		int length = this.typeArguments.length;
		if (this.typeArgumentsCounter != length) {
			System.arraycopy(this.typeArguments, 0, (this.typeArguments= new String[this.typeArgumentsCounter]), 0, this.typeArgumentsCounter);
		}
		return this.typeArguments;
	}

	public void setClassBound(String bound) {
		this.currentTypeParameterDescriptor.setClassBound(bound);
	}
	
	public void setSuperclass(String superclass) {
		this.superClass = superclass;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, max = this.typeParameterDescriptorsCounter; i < max; i++) {
			if (i > 0) buffer.append(',');
			buffer.append(this.typeParameterDescriptors[i]);
		}
		buffer.append("superclass: " + this.superClass); //$NON-NLS-1$
		for (int i = 0, max = this.typeArgumentsCounter; i < max; i++) {
			if (i > 0) buffer.append(',');
			buffer.append(this.typeArguments[i]);
		}
		return String.valueOf(buffer);
	}
}