/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.comparator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaVisitor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;

public class Delta implements IDelta {
	private static final IDelta[] EMPTY_CHILDREN = new IDelta[0];
	private static final int INITIAL_SIZE = 4;

	public static final int RESTRICTIONS_MASK = 0xFFFF;
	public static final int PREVIOUS_RESTRICTIONS_OFFSET = 16;

	/**
	 * Writes the delta to the given {@link PrintWriter}
	 *
	 * @param delta
	 * @param writer
	 */
	private static void print(IDelta delta, PrintWriter writer) {
		writer.print("delta (elementType: "); //$NON-NLS-1$
		switch (delta.getElementType()) {
			case IDelta.FIELD_ELEMENT_TYPE:
				writer.print("field"); //$NON-NLS-1$
				break;
			case IDelta.ANNOTATION_ELEMENT_TYPE:
				writer.print("annotation type"); //$NON-NLS-1$
				break;
			case IDelta.CLASS_ELEMENT_TYPE:
				writer.print("class type"); //$NON-NLS-1$
				break;
			case IDelta.INTERFACE_ELEMENT_TYPE:
				writer.print("interface type"); //$NON-NLS-1$
				break;
			case IDelta.ENUM_ELEMENT_TYPE:
				writer.print("enum type"); //$NON-NLS-1$
				break;
			case IDelta.API_COMPONENT_ELEMENT_TYPE:
				writer.print("API component type"); //$NON-NLS-1$
				break;
			case IDelta.METHOD_ELEMENT_TYPE:
				writer.print("method"); //$NON-NLS-1$
				break;
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE:
				writer.print("constructor"); //$NON-NLS-1$
				break;
			case IDelta.API_BASELINE_ELEMENT_TYPE:
				writer.print("API baseline"); //$NON-NLS-1$
				break;
			default:
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

	private IDelta[] children;
	private String componentID;
	private String[] datas;
	private int deltasCounter;
	private int elementType;
	private int flags;
	private String key;

	private int kind;
	private int oldModifiers;
	private int newModifiers;
	private int restrictions;

	private String typeName;

	/**
	 * Constructor
	 */
	public Delta() {
		// use for root delta
	}

	/**
	 * Constructor
	 *
	 * @param elementType
	 * @param kind
	 * @param flags
	 * @param restrictions
	 * @param modifiers
	 * @param classFile
	 * @param key
	 * @param data
	 */
	public Delta(String componentID, int elementType, int kind, int flags, int restrictions, int oldModifiers, int newModifiers, String typeName, String key, String data) {
		this(componentID, elementType, kind, flags, restrictions, 0, oldModifiers, newModifiers, typeName, key, new String[] { data });
	}

	public Delta(String componentID, int elementType, int kind, int flags, int restrictions, int previousRestrictions, int oldModifiers, int newModifiers, String typeName, String key, String[] datas) {
		this.componentID = componentID;
		this.elementType = elementType;
		this.kind = kind;
		this.flags = flags;
		this.oldModifiers = oldModifiers;
		this.newModifiers = newModifiers;
		this.typeName = typeName == null ? Util.EMPTY_STRING : typeName;
		this.restrictions = (previousRestrictions & RESTRICTIONS_MASK) << PREVIOUS_RESTRICTIONS_OFFSET | (restrictions & RESTRICTIONS_MASK);
		this.key = key;
		this.datas = datas;
	}

	/**
	 * Constructor
	 *
	 * @param elementType
	 * @param kind
	 * @param flags
	 * @param classFile
	 * @param key
	 * @param data
	 */
	public Delta(String componentID, int elementType, int kind, int flags, String typeName, String key, String data) {
		this(componentID, elementType, kind, flags, RestrictionModifiers.NO_RESTRICTIONS, 0, 0, typeName, key, data);
	}

	@Override
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
	 * Adds a child delta to this delta. If the specified delta is
	 * <code>null</code> no work is done.
	 *
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Delta other)) {
			return false;
		}
		if (this.elementType != other.elementType) {
			return false;
		}
		if (this.flags != other.flags) {
			return false;
		}
		if (this.kind != other.kind) {
			return false;
		}
		if (this.oldModifiers != other.oldModifiers) {
			return false;
		}
		if (this.newModifiers != other.newModifiers) {
			return false;
		}
		if (this.restrictions != other.restrictions) {
			return false;
		}
		if (this.typeName == null) {
			if (other.typeName != null) {
				return false;
			}
		} else if (!this.typeName.equals(other.typeName)) {
			return false;
		}
		if (this.key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!this.key.equals(other.key)) {
			return false;
		}
		if (this.datas == null) {
			if (other.datas != null) {
				return false;
			}
		} else if (other.datas == null) {
			return false;
		} else if (!Arrays.equals(this.datas, other.datas)) {
			return false;
		}
		if (this.componentID == null) {
			if (other.componentID != null) {
				return false;
			}
		} else if (!this.componentID.equals(other.componentID)) {
			return false;
		}
		return true;
	}

	@Override
	public String getComponentVersionId() {
		return this.componentID;
	}

	@Override
	public String getComponentId() {
		if (this.componentID == null) {
			return null;
		}
		int index = this.componentID.indexOf(Util.VERSION_SEPARATOR);
		return this.componentID.substring(0, index);
	}

	@Override
	public String[] getArguments() {
		if (this.datas == null) {
			return new String[] { typeName };
		}
		return this.datas;
	}

	@Override
	public IDelta[] getChildren() {
		if (this.children == null) {
			return EMPTY_CHILDREN;
		}
		int resizeLength = this.deltasCounter;
		if (resizeLength != this.children.length) {
			System.arraycopy(this.children, 0, (this.children = new IDelta[resizeLength]), 0, resizeLength);
		}
		return this.children;
	}

	@Override
	public int getElementType() {
		return this.elementType;
	}

	@Override
	public int getFlags() {
		return this.flags;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public int getKind() {
		return this.kind;
	}

	@Override
	public String getMessage() {
		if (DeltaProcessor.isCompatible(this)) {
			return Messages.getCompatibleLocalizedMessage(this);
		}
		int id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, this.elementType, this.kind, this.flags);
		return ApiProblemFactory.getLocalizedMessage(id, (this.datas != null ? this.datas : null));
	}

	@Override
	public int getNewModifiers() {
		return this.newModifiers;
	}

	@Override
	public int getOldModifiers() {
		return this.oldModifiers;
	}

	@Override
	public int getCurrentRestrictions() {
		return (this.restrictions & RESTRICTIONS_MASK);
	}

	@Override
	public int getPreviousRestrictions() {
		return (this.restrictions >>> PREVIOUS_RESTRICTIONS_OFFSET);
	}

	@Override
	public String getTypeName() {
		return this.typeName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.datas == null) ? 0 : Arrays.hashCode(this.datas));
		result = prime * result + this.elementType;
		result = prime * result + this.flags;
		result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
		result = prime * result + ((this.typeName == null) ? 0 : this.typeName.hashCode());
		result = prime * result + this.kind;
		result = prime * result + this.oldModifiers;
		result = prime * result + this.newModifiers;
		result = prime * result + this.restrictions;
		result = prime * result + ((this.componentID == null) ? 0 : this.componentID.hashCode());
		return result;
	}

	@Override
	public boolean isEmpty() {
		return this.deltasCounter == 0;
	}

	@Override
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
}
