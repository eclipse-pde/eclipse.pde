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
 * Class used to process the delta to find out if they are compatible or not.
 *
 * @since 1.0.0
 */
public class DeltaProcessor {
	
	/**
	 * Return true is the given delta is compatible, false otherwise.
	 * 
	 * @param delta the given delta
	 * @return true is the given delta is compatible, false otherwise.
	 */
	public static boolean isCompatible(IDelta delta) {
		class CompatibleVisitor extends DeltaVisitor {
			boolean isCompatible = true;
			
			public boolean visit(IDelta delta) {
				if (!this.isCompatible) return false;
				return true;
			}
			public void endVisit(IDelta delta) {
				if (this.isCompatible) {
					this.isCompatible = isCompatible0(delta);
				}
			}
		};
		if (delta.getChildren().length != 0) {
			CompatibleVisitor visitor = new CompatibleVisitor();
			delta.accept(visitor);
			return visitor.isCompatible;
		} else {
			return isCompatible0(delta);
		}
	}

	/**
	 * Returns if the delta is compatible or not
	 * @param delta
	 * @return true if the delta represents a compatible change or not
	 */
	private static boolean isCompatible0(IDelta delta) {
		switch(delta.getElementType()) {
			case IDelta.API_PROFILE_ELEMENT_TYPE : {
				return isApiProfileCompatible(delta);
			}
			case IDelta.API_COMPONENT_ELEMENT_TYPE : {
				return isApiComponentCompatible(delta);
			}
			case IDelta.INTERFACE_ELEMENT_TYPE : {
				return isInterfaceCompatible(delta);
			}
			case IDelta.ANNOTATION_ELEMENT_TYPE : {
				return isAnnotationCompatible(delta);
			}
			case IDelta.METHOD_ELEMENT_TYPE : {
				return isMethodCompatible(delta);
			}
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE : {
				return isConstructorCompatible(delta);
			}
			case IDelta.FIELD_ELEMENT_TYPE : {
				return isFieldCompatible(delta);
			}
			case IDelta.CLASS_ELEMENT_TYPE : {
				return isClassCompatible(delta);
			}
			case IDelta.ENUM_ELEMENT_TYPE : {
				return isEnumCompatible(delta);
			}
		}
		return true;
	}
	
	/**
	 * Returns if the API profile is compatible
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isApiProfileCompatible(IDelta delta) {
		switch(delta.getKind()) {
			case IDelta.REMOVED :
				switch(delta.getFlags()) {
					case IDelta.API_COMPONENT :
						return false;
				}
		}
		return true;
	}

	/**
	 * Returns if the API component is compatible
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isApiComponentCompatible(IDelta delta) {
		switch(delta.getKind()) {
			case IDelta.REMOVED :
				switch(delta.getFlags()) {
					case IDelta.TYPE :
					case IDelta.API_TYPE :
						return false;
				}
				break;
		}
		return true;
	}
	
	/**
	 * Returns if the annotation is compatible or not
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isAnnotationCompatible(IDelta delta) {
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
					case IDelta.API_FIELD :
					case IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE :
					case IDelta.API_METHOD_WITH_DEFAULT_VALUE :
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
		return true;
	}
	
	/**
	 * Returns if the method is compatible or not
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isMethodCompatible(IDelta delta) {
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
						int restrictions = delta.getRestrictions();
						return !Util.isVisible(delta) || RestrictionModifiers.isOverrideRestriction(restrictions)
							|| RestrictionModifiers.isExtendRestriction(restrictions);
				}
				break;
		}
		return true;
	}
	
	/**
	 * Returns if the field is compatible or not
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isFieldCompatible(IDelta delta) {
		switch(delta.getKind()) {
			case IDelta.REMOVED :
				switch(delta.getFlags()) {
					case IDelta.VALUE :
						if (Util.isProtected(delta.getModifiers())) {
							return RestrictionModifiers.isExtendRestriction(delta.getRestrictions());
						}
						if (Util.isPublic(delta.getModifiers())) {
							return false;
						}
						// not visible
						return true;
					case IDelta.TYPE_ARGUMENTS :
						return !Util.isVisible(delta);
				}
				break;
			case IDelta.CHANGED :
				switch(delta.getFlags()) {
					case IDelta.TYPE :
					case IDelta.TYPE_ARGUMENTS :
					case IDelta.NON_FINAL_TO_FINAL :
					case IDelta.STATIC_TO_NON_STATIC :
					case IDelta.NON_STATIC_TO_STATIC :
						return !Util.isVisible(delta);
					case IDelta.VALUE :
					case IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT :
						if (Util.isProtected(delta.getModifiers())) {
							return RestrictionModifiers.isExtendRestriction(delta.getRestrictions());
						}
						if (Util.isPublic(delta.getModifiers())) {
							return false;
						}
						// not visible
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
		return true;
	}
	
	/**
	 * Returns if the constructor is compatible or not
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isConstructorCompatible(IDelta delta) {
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
						return !Util.isVisible(delta) || RestrictionModifiers.isOverrideRestriction(delta.getRestrictions());
				}
				break;
		}
		return true;
	}
	
	/**
	 * Returns if the enum is compatible or not
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isEnumCompatible(IDelta delta) {
		switch(delta.getKind()) {
			case IDelta.ADDED :
				switch(delta.getFlags()) {
					case IDelta.FIELD :
					case IDelta.METHOD :
						return !Util.isVisible(delta);
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
					case IDelta.API_FIELD :
					case IDelta.API_ENUM_CONSTANT :
					case IDelta.API_METHOD :
					case IDelta.API_CONSTRUCTOR :
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
					case IDelta.RESTRICTIONS :
						return !Util.isVisible(delta);
					case IDelta.DECREASE_ACCESS :
						return false;
				}
				break;
		}
		return true;
	}
	
	/**
	 * Returns if a class file is compatible
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isClassCompatible(IDelta delta) {
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
					case IDelta.API_FIELD :
					case IDelta.API_METHOD :
					case IDelta.METHOD :
					case IDelta.TYPE_MEMBER :
						if (Util.isVisible(delta)) {
							return RestrictionModifiers.isExtendRestriction(delta.getRestrictions())
									&& Util.isProtected(delta.getModifiers());
						}
						return true;
					case IDelta.CONSTRUCTOR :
					case IDelta.API_CONSTRUCTOR :
						if (Util.isVisible(delta)) {
							return RestrictionModifiers.isExtendRestriction(delta.getRestrictions())
									&& (Util.isProtected(delta.getModifiers()) ||
											RestrictionModifiers.isInstantiateRestriction(delta.getRestrictions()));
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
					case IDelta.TO_ANNOTATION :
					case IDelta.TO_ENUM :
					case IDelta.TO_INTERFACE :
					case IDelta.RESTRICTIONS :
					case IDelta.STATIC_TO_NON_STATIC :
					case IDelta.NON_STATIC_TO_STATIC :
						return !Util.isVisible(delta);
					case IDelta.NON_FINAL_TO_FINAL:
						if (Util.isVisible(delta)) {
							return RestrictionModifiers.isExtendRestriction(delta.getRestrictions());
						}
						return true; 
					case IDelta.DECREASE_ACCESS :
						return false;
				}
				break;
		}
		return true;
	}
	
	/**
	 * Returns if the interface element is compatible 
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isInterfaceCompatible(IDelta delta) {
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
					case IDelta.API_FIELD :
					case IDelta.API_METHOD :
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
		return true;
	}
}
