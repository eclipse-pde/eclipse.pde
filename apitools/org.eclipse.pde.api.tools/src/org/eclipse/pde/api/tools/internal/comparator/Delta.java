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

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;

// TODO add javadoc
public class Delta implements IDelta {
	private static final IDelta[] EMPTY_CHILDREN = new IDelta[0];
	private static final int INITIAL_SIZE = 4;
	
	IDelta[] children;
	IClassFile classFile;
	int deltasCounter;
	int elementType;
	int flags;
	String key;
	int restrictions;
	int modifiers;

	int kind;
	Object data;

	/**
	 * Constructor
	 */
	public Delta() {
		// use for root delta
	}

	/**
	 * Constructor
	 * @param elementType
	 * @param kind
	 * @param flags
	 * @param classFile
	 * @param key
	 * @param data
	 */
	public Delta(int elementType, int kind, int flags, IClassFile classFile, String key, Object data) {
		this(elementType, kind, flags, RestrictionModifiers.NO_RESTRICTIONS, 0, classFile, key, data);
	}

	/**
	 * Constructor
	 * @param elementType
	 * @param kind
	 * @param flags
	 * @param restrictions
	 * @param modifiers
	 * @param classFile
	 * @param key
	 * @param data
	 */
	public Delta(int elementType, int kind, int flags, int restrictions, int modifiers, IClassFile classFile, String key, Object data) {
		this.elementType = elementType;
		this.kind = kind;
		this.flags = flags;
		this.modifiers = modifiers;
		this.classFile = classFile;
		this.restrictions = restrictions;
		this.key = key;
		this.data = data;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#accept(org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor)
	 */
	public void accept(DeltaVisitor visitor) {
		if (visitor.visit(this)) {
			if (this.children != null) {
				for (int i = 0, max = this.deltasCounter; i < max; i++) {
					IDelta delta = this.children[i];
					delta.accept(visitor);
				}
			}
		}
		visitor.endVisit(this);
	}

	/**
	 * Adds a child delta to this delta. If the specified delta 
	 * is <code>null</code> no work is done.
	 * @param delta the new child delta
	 */
	public void add(IDelta delta) {
		if (delta == null) {
			return;
		}
		if (this.children == null) {
			this.children = new Delta[INITIAL_SIZE];
			this.deltasCounter = 0;
		}
		int length = this.children.length;
		if (this.deltasCounter == length) {
			System.arraycopy(this.children, 0, (this.children = new IDelta[length * 2]), 0, length);
		}
		this.children[this.deltasCounter++] = delta;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getChildren()
	 */
	public IDelta[] getChildren() {
		if (this.children == null) return EMPTY_CHILDREN;
		int resizeLength = this.deltasCounter;
		if (resizeLength != this.children.length) {
			System.arraycopy(this.children, 0, (this.children = new IDelta[resizeLength]), 0, resizeLength);
		}
		return this.children;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getElementType()
	 */
	public int getElementType() {
		return this.elementType;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getFlags()
	 */
	public int getFlags() {
		return this.flags;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getKey()
	 */
	public String getKey() {
		return this.key;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getKind()
	 */
	public int getKind() {
		return this.kind;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getTypeName()
	 */
	public String getTypeName() {
		if (this.classFile != null) return this.classFile.getTypeName();
		return Util.EMPTY_STRING;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#isEmpty()
	 */
	public boolean isEmpty() {
		return this.deltasCounter == 0;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		if (this.children == null) {
			print(this, printWriter);
		} else {
			printWriter.print('[');
			for (int i = 0, max = this.deltasCounter; i < max; i++) {
				if (i > 0) {
					printWriter.println(',');
				}
				printWriter.print(this.children[i]);
			}
			printWriter.print(']');
		}
		return String.valueOf(writer.getBuffer());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getMessage()
	 */
	public String getMessage() {
		return ApiProblemFactory.getLocalizedMessage(IApiProblem.CATEGORY_BINARY, 
				this.elementType, this.kind, this.flags, (this.data != null ? new String[] {this.data.toString()} : null));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getArguments()
	 */
	public String[] getArguments() {
		if(this.data == null) {
			return new String[] {};
		}
		return new String[] {(String) this.data};
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getModifiers()
	 */
	public int getModifiers() {
		return this.modifiers;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta#getRestrictions()
	 */
	public int getRestrictions() {
		return this.restrictions;
	}
	
	/**
	 * Writes the delta to the given {@link PrintWriter}
	 * @param delta
	 * @param writer
	 */
	private static void print(IDelta delta, PrintWriter writer) {
		writer.print("delta (elementType: "); //$NON-NLS-1$
		switch(delta.getElementType()) {
			case IDelta.FIELD_ELEMENT_TYPE :
				writer.print("field"); //$NON-NLS-1$
				break;
			case IDelta.ANNOTATION_ELEMENT_TYPE :
				writer.print("annotation type"); //$NON-NLS-1$
				break;
			case IDelta.CLASS_ELEMENT_TYPE :
				writer.print("class type"); //$NON-NLS-1$
				break;
			case IDelta.INTERFACE_ELEMENT_TYPE :
				writer.print("interface type"); //$NON-NLS-1$
				break;
			case IDelta.ENUM_ELEMENT_TYPE :
				writer.print("enum type"); //$NON-NLS-1$
				break;
			case IDelta.API_COMPONENT_ELEMENT_TYPE :
				writer.print("API component type"); //$NON-NLS-1$
				break;
			case IDelta.METHOD_ELEMENT_TYPE :
				writer.print("method"); //$NON-NLS-1$
				break;
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
				writer.print("constructor"); //$NON-NLS-1$
				break;
			case IDelta.API_PROFILE_ELEMENT_TYPE :
				writer.print("API profile"); //$NON-NLS-1$
				break;
		}
		writer.print(", kind : "); //$NON-NLS-1$
		writer.print(delta.getKind());
		writer.print(", flags : "); //$NON-NLS-1$
		writer.print(delta.getFlags());
		writer.print(')');
		writer.print('-');
		writer.print(Util.getDetail(delta));
	}
}
