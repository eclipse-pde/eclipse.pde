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
package org.eclipse.pde.api.tools.internal.provisional.comparator;

import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.util.Util;


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
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
								return RestrictionModifiers.isImplementRestriction(delta.getRestrictions());
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
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
				}
				break;
			case IDelta.ANNOTATION_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
								return RestrictionModifiers.isImplementRestriction(delta.getRestrictions());
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
							case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
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
								return !Util.isVisible(delta);
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return !Util.isVisible(delta);
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.VARARGS_TO_ARRAY :
							case IDelta.NON_ABSTRACT_TO_ABSTRACT :
							case IDelta.NON_STATIC_TO_STATIC :
							case IDelta.STATIC_TO_NON_STATIC :
								return !Util.isVisible(delta);
							case IDelta.DECREASE_ACCESS :
								return false;
							case IDelta.NON_FINAL_TO_FINAL :
								return !Util.isVisible(delta) || RestrictionModifiers.isExtendRestriction(delta.getRestrictions());
						}
						break;
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
								return !Util.isVisible(delta);
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return !Util.isVisible(delta);
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.VARARGS_TO_ARRAY :
							case IDelta.NON_ABSTRACT_TO_ABSTRACT :
							case IDelta.NON_STATIC_TO_STATIC :
							case IDelta.STATIC_TO_NON_STATIC :
								return !Util.isVisible(delta);
							case IDelta.DECREASE_ACCESS :
								return false;
							case IDelta.NON_FINAL_TO_FINAL :
								return !Util.isVisible(delta) || RestrictionModifiers.isExtendRestriction(delta.getRestrictions());
						}
						break;
				}
				break;
			case IDelta.FIELD_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.VALUE :
							case IDelta.TYPE_ARGUMENTS :
								return !Util.isVisible(delta);
						}
						break;
					case IDelta.CHANGED :
						switch(delta.getFlags()) {
							case IDelta.TYPE :
							case IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT :
							case IDelta.NON_FINAL_TO_FINAL :
							case IDelta.STATIC_TO_NON_STATIC :
							case IDelta.NON_STATIC_TO_STATIC :
								return !Util.isVisible(delta);
							case IDelta.VALUE :
								if (Util.isVisible(delta)) {
									return RestrictionModifiers.isExtendRestriction(delta.getRestrictions()) || RestrictionModifiers.isImplementRestriction(delta.getRestrictions());
								}
								return true;
							case IDelta.DECREASE_ACCESS :
								return false;
						}
						break;
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.VALUE :
								return !Util.isVisible(delta);
						}
				}
				break;
			case IDelta.CLASS_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED:
						switch(delta.getFlags()) {
							case IDelta.FIELD :
								if (Util.isVisible(delta)) {
									if (Util.isStatic(delta.getModifiers())) {
										return true;
									}
									return RestrictionModifiers.isExtendRestriction(delta.getRestrictions()) || Util.isProtected(delta.getModifiers());
								}
								return true; 
							case IDelta.METHOD :
								if (Util.isVisible(delta)) {
									return RestrictionModifiers.isExtendRestriction(delta.getRestrictions()) || !Util.isAbstract(delta.getModifiers());
								}
								return true; 
							case IDelta.TYPE_PARAMETER :
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
								return !Util.isVisible(delta);
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
							case IDelta.TYPE_MEMBER :
								if (Util.isVisible(delta)) {
									return RestrictionModifiers.isExtendRestriction(delta.getRestrictions())
											&& Util.isProtected(delta.getModifiers());
								}
								return true;
							case IDelta.CLASS_BOUND :
							case IDelta.INTERFACE_BOUND :
							case IDelta.INTERFACE_BOUNDS :
							case IDelta.TYPE_PARAMETER :
							case IDelta.TYPE_PARAMETERS :
								return !Util.isVisible(delta);
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
							case IDelta.RESTRICTIONS :
								return !Util.isVisible(delta);
							case IDelta.DECREASE_ACCESS :
								return false;
						}
						break;
				}
				break;
			case IDelta.ENUM_ELEMENT_TYPE :
				switch(delta.getKind()) {
					case IDelta.ADDED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.METHOD :
								return !Util.isVisible(delta) || RestrictionModifiers.isExtendRestriction(delta.getRestrictions());
						}
						break;
					case IDelta.REMOVED :
						switch(delta.getFlags()) {
							case IDelta.FIELD :
							case IDelta.ENUM_CONSTANT :
							case IDelta.METHOD :
							case IDelta.CONSTRUCTOR :
							case IDelta.TYPE_MEMBER :
								return !Util.isVisible(delta);
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
							case IDelta.RESTRICTIONS :
								return !Util.isVisible(delta);
							case IDelta.DECREASE_ACCESS :
								return false;
						}
						break;
				}
		}
		return true;
	}
}
