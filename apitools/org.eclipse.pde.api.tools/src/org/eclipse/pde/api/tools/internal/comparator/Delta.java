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
import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;

// TODO add javadoc
public class Delta implements IDelta {
	private static final IDelta[] EMPTY_CHILDREN = new IDelta[0];
	private static final int INITIAL_SIZE = 4;

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

	public Delta() {
		// use for root delta
	}

	public Delta(int elementType, int kind, int flags, IClassFile classFile, String key, Object data) {
		this(elementType, kind, flags, RestrictionModifiers.NO_RESTRICTIONS, 0, classFile, key, data);
	}

	public Delta(int elementType, int kind, int flags, int restrictions, int modifiers, IClassFile classFile, String key, Object data) {
		this.elementType = elementType;
		this.kind = kind;
		this.flags = flags;
		this.restrictions = restrictions;
		this.modifiers = modifiers;
		this.classFile = classFile;
		this.key = key;
		this.data = data;
	}

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

	public void add(IDelta delta) {
		if (delta == null) return;
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

	public IDelta[] getChildren() {
		if (this.children == null) return EMPTY_CHILDREN;
		int resizeLength = this.deltasCounter;
		if (resizeLength != this.children.length) {
			System.arraycopy(this.children, 0, (this.children = new IDelta[resizeLength]), 0, resizeLength);
		}
		return this.children;
	}


	public int getElementType() {
		return this.elementType;
	}
	
	public int getFlags() {
		return this.flags;
	}

	public String getKey() {
		return this.key;
	}
	public int getKind() {
		return this.kind;
	}

	public String getTypeName() {
		if (this.classFile != null) return this.classFile.getTypeName();
		return Util.EMPTY_STRING;
	}

	public boolean isEmpty() {
		return this.deltasCounter == 0;
	}
	
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
	
	public String getMessage() {
		StringBuffer buffer = new StringBuffer(Util.getDeltaKindName(this));
		switch(this.getKind()) {
			case IDelta.ADDED :
				switch(this.getFlags()) {
					case IDelta.METHOD :
					case IDelta.TYPE_MEMBER :
						if (!Util.isVisible(this)) {
							buffer.append("_NON_VISIBLE"); //$NON-NLS-1$
						} else {
							switch(this.elementType) {
								case IDelta.CLASS_ELEMENT_TYPE :
								case IDelta.ENUM_ELEMENT_TYPE :
									if (RestrictionModifiers.isExtendRestriction(this.getRestrictions())) {
										buffer.append("_EXTEND_RESTRICTION"); //$NON-NLS-1$
									} else {
										buffer.append("_NOT_EXTEND_RESTRICTION"); //$NON-NLS-1$
									}
									break;
								case IDelta.INTERFACE_ELEMENT_TYPE :
								case IDelta.ANNOTATION_ELEMENT_TYPE :
									if (RestrictionModifiers.isImplementRestriction(this.getRestrictions())) {
										buffer.append("_IMPLEMENT_RESTRICTION"); //$NON-NLS-1$
									} else {
										buffer.append("_NOT_IMPLEMENT_RESTRICTION"); //$NON-NLS-1$
									}
							}
						}
						break;
					case IDelta.FIELD :
						if (!Util.isVisible(this)) {
							buffer.append("_NON_VISIBLE"); //$NON-NLS-1$
						} else {
							switch(this.elementType) {
								case IDelta.CLASS_ELEMENT_TYPE :
								case IDelta.ENUM_ELEMENT_TYPE :
									if (RestrictionModifiers.isExtendRestriction(this.getRestrictions())) {
										buffer.append("_EXTEND_RESTRICTION"); //$NON-NLS-1$
									} else {
										buffer.append("_NOT_EXTEND_RESTRICTION"); //$NON-NLS-1$
										if (Util.isStatic(this.getModifiers())) {
											buffer.append("_STATIC"); //$NON-NLS-1$
										}
									}
									break;
								case IDelta.INTERFACE_ELEMENT_TYPE :
								case IDelta.ANNOTATION_ELEMENT_TYPE :
									if (RestrictionModifiers.isImplementRestriction(this.getRestrictions())) {
										buffer.append("_IMPLEMENT_RESTRICTION"); //$NON-NLS-1$
									} else {
										buffer.append("_NOT_IMPLEMENT_RESTRICTION"); //$NON-NLS-1$
									}
							}
						}
						break;
					case IDelta.CONSTRUCTOR :
					case IDelta.CHECKED_EXCEPTION :
						if (!Util.isVisible(this)) {
							buffer.append("_NON_VISIBLE"); //$NON-NLS-1$
						}
				}
				break;
			case IDelta.CHANGED :
				switch(this.getFlags()) {
					case IDelta.NON_FINAL_TO_FINAL :
						if (!RestrictionModifiers.isExtendRestriction(this.getRestrictions())) {
							buffer.append("_NOT_EXTEND_RESTRICTION"); //$NON-NLS-1$
						}
						break;
					case IDelta.TYPE :
					case IDelta.VALUE :
						if (!Util.isVisible(this)) {
							buffer.append("_NON_VISIBLE"); //$NON-NLS-1$
						}
						break;
				}
				break;
			case IDelta.REMOVED :
				switch(this.flags) {
					case IDelta.METHOD :
					case IDelta.FIELD :
					case IDelta.TYPE_MEMBER :
					case IDelta.CONSTRUCTOR :
						if (!Util.isVisible(this)) {
							buffer.append("_NON_VISIBLE"); //$NON-NLS-1$
						}
				}
		}

		buffer.append('_').append(Util.getDeltaFlagsName(this));
		String name = String.valueOf(buffer);
		try {
			Field field = DeltaMessages.class.getField(name);
			switch(this.kind) {
				case IDelta.REMOVED :
					return NLS.bind((String) field.get(null), this.data);
				default :
					return (String) field.get(null);
			}
		} catch (SecurityException e) {
			// ignore
		} catch (IllegalArgumentException e) {
			// ignore
		} catch (NoSuchFieldException e) {
			// ignore
		} catch (IllegalAccessException e) {
			// ignore
		}
		return NLS.bind(DeltaMessages.UNKNOWN_MESSAGE, name);
	}
	public int getModifiers() {
		return this.modifiers;
	}
	public int getRestrictions() {
		return this.restrictions;
	}
}
