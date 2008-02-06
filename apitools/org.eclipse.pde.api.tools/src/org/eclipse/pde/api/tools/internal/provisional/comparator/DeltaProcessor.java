/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.comparator;


/**
 * Class used to process the delta to find out if they are binary compatible or not.
 *
 * @since 1.0.0
 */
public class DeltaProcessor {
	
	/**
	 * Return true is the given delta is binary compatible, false otherwise.
	 * 
	 * @param delta the given delta
	 * @return true is the given delta is binary compatible, false otherwise.
	 */
	public static boolean isBinaryCompatible(IDelta delta) {
		class BinaryCompatibleVisitor extends DeltaVisitor {
			boolean isBinaryCompatible = true;
			
			public boolean visit(IDelta delta) {
				if (!this.isBinaryCompatible) return false;
				return true;
			}
			public void endVisit(IDelta delta) {
				if (this.isBinaryCompatible) {
					this.isBinaryCompatible = isBinaryCompatible0(delta);
				}
			}
		};
		if (delta.getChildren().length != 0) {
			BinaryCompatibleVisitor visitor = new BinaryCompatibleVisitor();
			delta.accept(visitor);
			return visitor.isBinaryCompatible;
		} else {
			return isBinaryCompatible0(delta);
		}
	}

	static boolean isBinaryCompatible0(IDelta delta) {
		switch(delta.getElementType()) {
			case IDelta.API_PROFILE_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.API_COMPONENT :
								return false;
						}
				}
				break;
			case IDelta.API_COMPONENT_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.TYPE :
							case IDelta.DUPLICATED_TYPE :
							case IDelta.EXECUTION_ENVIRONMENT :
								return false;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.EXECUTION_ENVIRONMENT :
								return false;
						}
				}
				break;
			case IDelta.INTERFACE_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
							case IDelta.TYPE_MEMBER :
								return false;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
							case IDelta.TYPE_MEMBER :
							case IDelta.TYPE_PARAMETERS :
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return false;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.CONTRACTED_SUPERINTERFACES_SET :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.TO_ANNOTATION :
							case IDelta.TO_CLASS :
							case IDelta.TO_ENUM :
							case IDelta.RESTRICTIONS :
								return false;
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return false;
						}
				}
				break;
			case IDelta.ANNOTATION_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED_NOT_IMPLEMENT_RESTRICTION :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
							case IDelta.TYPE_MEMBER :
								return false;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
							case IDelta.METHOD_WITH_DEFAULT_VALUE :
							case IDelta.TYPE_MEMBER :
							case IDelta.TYPE_PARAMETERS :
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return false;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.CONTRACTED_SUPERINTERFACES_SET :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.TO_CLASS :
							case IDelta.TO_ENUM :
							case IDelta.TO_INTERFACE :
							case IDelta.RESTRICTIONS :
								return false;
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
							case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
								return false;
						}
				}
				break;
			case IDelta.METHOD_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.ANNOTATION_DEFAULT_VALUE :
							case IDelta.TYPE_PARAMETERS :
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return false;
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return false;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.VARARGS_TO_ARRAY :
							case IDelta.DECREASE_ACCESS :
							case IDelta.NON_ABSTRACT_TO_ABSTRACT :
							case IDelta.NON_STATIC_TO_STATIC :
							case IDelta.STATIC_TO_NON_STATIC :
								return false;
						}
						break;
					case IDelta.CHANGED_NOT_EXTEND_RESTRICTION :
						switch(delta.getFlags()) {
							case IDelta.NON_FINAL_TO_FINAL :
								return false;
						}
				}
				break;
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETERS :
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return false;
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return false;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.VARARGS_TO_ARRAY :
							case IDelta.DECREASE_ACCESS :
							case IDelta.NON_ABSTRACT_TO_ABSTRACT :
							case IDelta.NON_STATIC_TO_STATIC :
							case IDelta.STATIC_TO_NON_STATIC :
								return false;
						}
						break;
					case IDelta.CHANGED_NOT_EXTEND_RESTRICTION :
						switch(delta.getFlags()) {
							case IDelta.NON_FINAL_TO_FINAL :
								return false;
						}
				}
				break;
			case IDelta.FIELD_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.VALUE :
							case IDelta.TYPE_ARGUMENTS :
								return false;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.TYPE :
							case IDelta.VALUE :
							case IDelta.DECREASE_ACCESS :
							case IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT :
							case IDelta.NON_FINAL_TO_FINAL :
							case IDelta.STATIC_TO_NON_STATIC :
							case IDelta.NON_STATIC_TO_STATIC :
								return false;
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.VALUE :
								return false;
						}
				}
				break;
			case IDelta.CLASS_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED_NOT_EXTEND_RESTRICTION :
						// this means that TYPE_MEMBER are binary compatible
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
								return false;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
							case IDelta.TYPE_MEMBER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
							case IDelta.TYPE_PARAMETER :
							case IDelta.TYPE_PARAMETERS :
								return false;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.CONTRACTED_SUPERINTERFACES_SET :
							case IDelta.CONTRACTED_SUPERCLASS_SET :
							case IDelta.SUPERCLASS :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.NON_ABSTRACT_TO_ABSTRACT :
							case IDelta.NON_FINAL_TO_FINAL:
							case IDelta.TO_ANNOTATION :
							case IDelta.TO_ENUM :
							case IDelta.TO_INTERFACE :
							case IDelta.DECREASE_ACCESS :
							case IDelta.RESTRICTIONS :
								return false;
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return false;
						}
				}
				break;
			case IDelta.ENUM_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED_NOT_EXTEND_RESTRICTION :
						// this means that TYPE_MEMBER are binary compatible
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
								return false;
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.ENUM_CONSTANT :
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
							case IDelta.TYPE_MEMBER :
								return false;
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.CONTRACTED_SUPERINTERFACES_SET :
							case IDelta.NON_ABSTRACT_TO_ABSTRACT :
							case IDelta.TO_ANNOTATION :
							case IDelta.TO_CLASS :
							case IDelta.TO_ENUM :
							case IDelta.TO_INTERFACE :
							case IDelta.DECREASE_ACCESS :
							case IDelta.RESTRICTIONS :
								return false;
						}
						break;
				}
		}
		return true;
	}
}
