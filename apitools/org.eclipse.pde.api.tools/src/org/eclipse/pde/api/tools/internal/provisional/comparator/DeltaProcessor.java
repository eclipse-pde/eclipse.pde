/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.provisional.comparator;

import org.eclipse.jdt.core.Flags;
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

			@Override
			public boolean visit(IDelta delta) {
				if (!this.isCompatible) {
					return false;
				}
				return true;
			}

			@Override
			public void endVisit(IDelta delta) {
				if (this.isCompatible) {
					this.isCompatible = isCompatible0(delta);
				}
			}
		}
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
	 *
	 * @param delta
	 * @return true if the delta represents a compatible change or not
	 */
	static boolean isCompatible0(IDelta delta) {
		switch (delta.getElementType()) {
			case IDelta.API_BASELINE_ELEMENT_TYPE: {
				return isApiProfileCompatible(delta);
			}
			case IDelta.API_COMPONENT_ELEMENT_TYPE: {
				return isApiComponentCompatible(delta);
			}
			case IDelta.INTERFACE_ELEMENT_TYPE: {
				return isInterfaceCompatible(delta);
			}
			case IDelta.ANNOTATION_ELEMENT_TYPE: {
				return isAnnotationCompatible(delta);
			}
			case IDelta.METHOD_ELEMENT_TYPE: {
				return isMethodCompatible(delta);
			}
			case IDelta.CONSTRUCTOR_ELEMENT_TYPE: {
				return isConstructorCompatible(delta);
			}
			case IDelta.FIELD_ELEMENT_TYPE: {
				return isFieldCompatible(delta);
			}
			case IDelta.CLASS_ELEMENT_TYPE: {
				return isClassCompatible(delta);
			}
			case IDelta.ENUM_ELEMENT_TYPE: {
				return isEnumCompatible(delta);
			}
			case IDelta.TYPE_PARAMETER_ELEMENT_TYPE: {
				return isTypeParameterCompatible(delta);
			}
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the API baseline is compatible
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isApiProfileCompatible(IDelta delta) {
		switch (delta.getKind()) {
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.API_COMPONENT:
						return false;
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the API component is compatible
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isApiComponentCompatible(IDelta delta) {
		switch (delta.getKind()) {
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.TYPE:
					case IDelta.API_TYPE:
					case IDelta.REEXPORTED_API_TYPE:
					case IDelta.REEXPORTED_TYPE:
						return false;
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the annotation is compatible or not
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isAnnotationCompatible(IDelta delta) {
		switch (delta.getKind()) {
			case IDelta.ADDED:
				switch (delta.getFlags()) {
					case IDelta.FIELD:
					case IDelta.TYPE_PARAMETER:
					case IDelta.METHOD_WITHOUT_DEFAULT_VALUE:
						return false;
					default:
						break;
				}
				break;
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.FIELD:
					case IDelta.METHOD_WITHOUT_DEFAULT_VALUE:
					case IDelta.METHOD_WITH_DEFAULT_VALUE:
					case IDelta.API_FIELD:
					case IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE:
					case IDelta.API_METHOD_WITH_DEFAULT_VALUE:
					case IDelta.TYPE_MEMBER:
					case IDelta.TYPE_PARAMETER:
						return false;
					default:
						break;
				}
				break;
			case IDelta.CHANGED:
				switch (delta.getFlags()) {
					case IDelta.CONTRACTED_SUPERINTERFACES_SET:
					case IDelta.TYPE_CONVERSION:
						return false;
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the method is compatible or not
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isMethodCompatible(IDelta delta) {
		int restrictions = delta.getCurrentRestrictions();
		if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
			return true;
		}
		switch (delta.getKind()) {
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.ANNOTATION_DEFAULT_VALUE:
					case IDelta.TYPE_PARAMETER:
						return !Util.isVisible(delta.getOldModifiers());
					default:
						break;
				}
				break;
			case IDelta.ADDED:
				switch (delta.getFlags()) {
					case IDelta.TYPE_PARAMETER:
					case IDelta.RESTRICTIONS:
						if (Util.isVisible(delta.getNewModifiers())) {
							return RestrictionModifiers.isExtendRestriction(delta.getPreviousRestrictions());
						}
						return true;
					default:
						break;
				}
				break;
			case IDelta.CHANGED:
				switch (delta.getFlags()) {
					case IDelta.VARARGS_TO_ARRAY:
					case IDelta.NON_ABSTRACT_TO_ABSTRACT:
					case IDelta.NON_STATIC_TO_STATIC:
					case IDelta.STATIC_TO_NON_STATIC:
						return !Util.isVisible(delta.getNewModifiers());
					case IDelta.DECREASE_ACCESS:
						return !Util.isVisible(delta.getOldModifiers()) || ( Flags.isProtected(delta.getOldModifiers()) && RestrictionModifiers.isExtendRestriction(restrictions));
					case IDelta.NON_FINAL_TO_FINAL:
						return !Util.isVisible(delta.getOldModifiers()) || !Util.isVisible(delta.getNewModifiers()) || RestrictionModifiers.isExtendRestriction(restrictions) || RestrictionModifiers.isOverrideRestriction(restrictions);
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the field is compatible or not
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isFieldCompatible(IDelta delta) {
		int restrictions = delta.getCurrentRestrictions();
		if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
			return true;
		}
		int newModifiers = delta.getNewModifiers();
		int oldModifiers = delta.getOldModifiers();
		switch (delta.getKind()) {
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.VALUE:
						if (Flags.isProtected(oldModifiers)) {
							return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
						}
						if (Flags.isPublic(oldModifiers)) {
							return false;
						}
						// not visible
						return true;
					case IDelta.TYPE_ARGUMENTS:
					case IDelta.TYPE_ARGUMENT:
						return !Util.isVisible(oldModifiers);
					default:
						break;
				}
				break;
			case IDelta.CHANGED:
				if (!Util.isVisible(oldModifiers)) {
					return true;
				}
				switch (delta.getFlags()) {
					case IDelta.TYPE:
						if (Flags.isProtected(newModifiers)) {
							return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
						}
						return !Util.isVisible(newModifiers);
					case IDelta.TYPE_ARGUMENT:
					case IDelta.NON_FINAL_TO_FINAL:
					case IDelta.STATIC_TO_NON_STATIC:
					case IDelta.NON_STATIC_TO_STATIC:
						return !Util.isVisible(newModifiers);
					case IDelta.VALUE:
					case IDelta.FINAL_TO_NON_FINAL_STATIC_CONSTANT:
						if (Flags.isProtected(newModifiers)) {
							return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
						}
						if (Flags.isPublic(newModifiers)) {
							return false;
						}
						// not visible
						return true;
					case IDelta.DECREASE_ACCESS:
						// if the initial flag was protected, decrease access is
						// compatible if class is extend restricted.
						return Flags.isProtected(delta.getOldModifiers()) && RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
					default:
						break;
				}
				break;
			case IDelta.ADDED:
				switch (delta.getFlags()) {
					case IDelta.TYPE_ARGUMENT:
						return !Util.isVisible(newModifiers);
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the constructor is compatible or not
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isConstructorCompatible(IDelta delta) {
		int restrictions = delta.getCurrentRestrictions();
		if (RestrictionModifiers.isReferenceRestriction(restrictions)) {
			return true;
		}
		switch (delta.getKind()) {
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.TYPE_PARAMETER:
						return !Util.isVisible(delta.getOldModifiers());
					default:
						break;
				}
				break;
			case IDelta.ADDED:
				switch (delta.getFlags()) {
					case IDelta.TYPE_PARAMETER:
						return !Util.isVisible(delta.getNewModifiers());
					default:
						break;
				}
				break;
			case IDelta.CHANGED:
				switch (delta.getFlags()) {
					case IDelta.VARARGS_TO_ARRAY:
					case IDelta.NON_ABSTRACT_TO_ABSTRACT:
					case IDelta.NON_STATIC_TO_STATIC:
					case IDelta.STATIC_TO_NON_STATIC:
						return !Util.isVisible(delta.getNewModifiers());
					case IDelta.DECREASE_ACCESS:
						return Flags.isProtected(delta.getOldModifiers()) && RestrictionModifiers.isExtendRestriction(restrictions);
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the enum is compatible or not
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isEnumCompatible(IDelta delta) {
		switch (delta.getKind()) {
			case IDelta.ADDED:
				switch (delta.getFlags()) {
					case IDelta.FIELD:
						return true;
					case IDelta.METHOD:
						return !Util.isVisible(delta.getNewModifiers());
					default:
						break;
				}
				break;
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.FIELD:
					case IDelta.ENUM_CONSTANT:
					case IDelta.METHOD:
					case IDelta.CONSTRUCTOR:
					case IDelta.TYPE_MEMBER:
						return !Util.isVisible(delta.getOldModifiers());
					case IDelta.API_FIELD:
					case IDelta.API_ENUM_CONSTANT:
					case IDelta.API_METHOD:
					case IDelta.API_CONSTRUCTOR:
						return false;
					default:
						break;
				}
				break;
			case IDelta.CHANGED:
				if (!Util.isVisible(delta.getNewModifiers())) {
					return true;
				}
				switch (delta.getFlags()) {
					case IDelta.CONTRACTED_SUPERINTERFACES_SET:
					case IDelta.NON_ABSTRACT_TO_ABSTRACT:
					case IDelta.TYPE_CONVERSION:
						return false;
					case IDelta.DECREASE_ACCESS:
						return !Util.isVisible(delta.getOldModifiers());
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if a class file is compatible
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isClassCompatible(IDelta delta) {
		switch (delta.getKind()) {
			case IDelta.ADDED:
				int newModifiers = delta.getNewModifiers();
				switch (delta.getFlags()) {
					case IDelta.FIELD:
						if (RestrictionModifiers.isReferenceRestriction(delta.getCurrentRestrictions())) {
							return true;
						}
						if (Util.isVisible(newModifiers)) {
							return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
						}
						return true;
					case IDelta.METHOD:
						if (Util.isVisible(newModifiers)) {
							if (Flags.isAbstract(newModifiers)) {
								// case where the implementation is provided and
								// the class cannot be instantiated by the
								// client
								return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
							}
						}
						return true;
					case IDelta.TYPE_PARAMETER:
					case IDelta.RESTRICTIONS:
						return !Util.isVisible(newModifiers);

					case IDelta.SUPERCLASS_BREAKING:
					case IDelta.EXPANDED_SUPERINTERFACES_SET_BREAKING:
						return false;
					default:
						break;
				}
				break;
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.FIELD:
					case IDelta.API_FIELD:
					case IDelta.API_METHOD:
					case IDelta.METHOD:
					case IDelta.TYPE_MEMBER:
						if (Flags.isPublic(delta.getOldModifiers())) {
							return false;
						}
						if (Flags.isProtected(delta.getOldModifiers())) {
							return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
						}
						return true;
					case IDelta.CONSTRUCTOR:
					case IDelta.API_CONSTRUCTOR:
						if (Util.isVisible(delta.getOldModifiers())) {
							return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions()) && (Flags.isProtected(delta.getOldModifiers()) || RestrictionModifiers.isInstantiateRestriction(delta.getCurrentRestrictions()));
						}
						return true;
					case IDelta.TYPE_PARAMETER:
					case IDelta.SUPERCLASS:
						return !Util.isVisible(delta.getOldModifiers());

					default:
						break;
				}
				break;
			case IDelta.CHANGED:
				switch (delta.getFlags()) {
					case IDelta.NON_ABSTRACT_TO_ABSTRACT:
						if (Util.isVisible(delta.getNewModifiers())) {
							return RestrictionModifiers.isInstantiateRestriction(delta.getCurrentRestrictions());
						}
						return true;
					case IDelta.TYPE_CONVERSION:
					case IDelta.CONTRACTED_SUPERINTERFACES_SET:
					case IDelta.STATIC_TO_NON_STATIC:
					case IDelta.NON_STATIC_TO_STATIC:
						return !Util.isVisible(delta.getNewModifiers());
					case IDelta.NON_FINAL_TO_FINAL:
						if (Util.isVisible(delta.getNewModifiers())) {
							return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
						}
						return true;
					case IDelta.DECREASE_ACCESS:
						return (Flags.isProtected(delta.getOldModifiers()) && RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions()));
					case IDelta.EXPANDED_SUPERINTERFACES_SET_BREAKING:
						return false;
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the interface element is compatible
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isTypeParameterCompatible(IDelta delta) {
		switch (delta.getKind()) {
			case IDelta.ADDED:
				switch (delta.getFlags()) {
					case IDelta.CLASS_BOUND:
					case IDelta.INTERFACE_BOUND:
						return false;
					default:
						break;
				}
				break;
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.CLASS_BOUND:
					case IDelta.INTERFACE_BOUND:
						return false;
					default:
						break;
				}
				break;
			case IDelta.CHANGED:
				switch (delta.getFlags()) {
					case IDelta.CLASS_BOUND:
					case IDelta.INTERFACE_BOUND:
						return false;
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}

	/**
	 * Returns if the interface element is compatible
	 *
	 * @param delta
	 * @return true if compatible, false otherwise
	 */
	private static boolean isInterfaceCompatible(IDelta delta) {
		switch (delta.getKind()) {
			case IDelta.ADDED:
				switch (delta.getFlags()) {
					case IDelta.FIELD:
						return RestrictionModifiers.isImplementRestriction(delta.getPreviousRestrictions()) || RestrictionModifiers.isImplementRestriction(delta.getCurrentRestrictions());
					case IDelta.METHOD:
					case IDelta.DEFAULT_METHOD:
					case IDelta.SUPER_INTERFACE_DEFAULT_METHOD:
					case IDelta.SUPER_INTERFACE_WITH_METHODS:
						boolean isStatic = Flags.isStatic(delta.getNewModifiers());
						if (isStatic == true) {
							return true;
						}
						return RestrictionModifiers.isImplementRestriction(delta.getPreviousRestrictions()) || RestrictionModifiers.isImplementRestriction(delta.getCurrentRestrictions());
					case IDelta.TYPE_PARAMETER:
						return false;
					case IDelta.RESTRICTIONS:
						return false;
					default:
						break;
				}
				break;
			case IDelta.REMOVED:
				switch (delta.getFlags()) {
					case IDelta.FIELD:
					case IDelta.METHOD:
					case IDelta.API_FIELD:
					case IDelta.API_METHOD:
					case IDelta.TYPE_MEMBER:
					case IDelta.TYPE_PARAMETER:
						return false;
					default:
						break;
				}
				break;
			case IDelta.CHANGED:
				switch (delta.getFlags()) {
					case IDelta.CONTRACTED_SUPERINTERFACES_SET:
					case IDelta.TYPE_CONVERSION:
						return false;
					case IDelta.DECREASE_ACCESS:
						return RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions());
					default:
						break;
				}
				break;
			default:
				break;
		}
		return true;
	}
}
