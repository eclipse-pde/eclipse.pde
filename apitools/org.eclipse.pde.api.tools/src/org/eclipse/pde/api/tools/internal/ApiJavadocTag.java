/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal;

import java.util.HashMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;

/**
 * Base API tools Javadoc tag implementation
 *
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ApiJavadocTag implements IApiJavadocTag {

	/**
	 * The id of the tag
	 */
	private String fId = null;
	/**
	 * The name of the tag (immediately proceeding the '@' symbol
	 */
	private String fName = null;
	/**
	 * Map of integer ids to comments
	 */
	private HashMap<Integer, String> fTagItems = null;

	private static String EMPTY_STRING = ""; //$NON-NLS-1$
	/**
	 * restriction modifier for the tag
	 */
	private int fRModifier = RestrictionModifiers.NO_RESTRICTIONS;

	/**
	 * Lazily computed tag label, cached once it has been computed
	 */
	private String fTaglabel = null;

	/**
	 * Constructor
	 *
	 * @param id the id of the tag
	 * @param name the name of the tag (not including the '@' symbol)
	 * @param rmodifier
	 */
	public ApiJavadocTag(String id, String name, int rmodifier) {
		Assert.isNotNull(id);
		fId = id;
		Assert.isNotNull(name);
		fName = name;
		fRModifier = rmodifier;
	}

	public String getTagId() {
		return fId;
	}

	@Override
	public int getRestrictionModifier() {
		return fRModifier;
	}

	@Override
	public void setApplicableTo(int type, int member, String comment) {
		if (fTagItems == null) {
			fTagItems = new HashMap<>(6);
		}
		fTagItems.put(getTagKey(type, member), comment);
	}

	/**
	 * Returns the comment for the given type ad member
	 *
	 * @param type
	 * @param member
	 * @return the comment for the tag
	 */
	public String getTagComment(int type, int member) {
		if (fTagItems == null) {
			return EMPTY_STRING;
		}
		Object obj = fTagItems.get(getTagKey(type, member));
		return (String) (obj == null ? EMPTY_STRING : obj);
	}

	@Override
	public String getTagName() {
		if (fTaglabel == null) {
			StringBuilder tag = new StringBuilder();
			tag.append("@"); //$NON-NLS-1$
			tag.append(fName);
			fTaglabel = tag.toString();
		}
		return fTaglabel;
	}

	@Override
	public String toString() {
		return getTagName();
	}

	@Override
	public String getCompleteTag(int type, int member) {
		StringBuilder tag = new StringBuilder();
		tag.append(getTagName());
		String comment = getTagComment(type, member);
		if (EMPTY_STRING.equals(comment)) {
			return tag.toString();
		}
		tag.append(" "); //$NON-NLS-1$
		tag.append(comment);
		return tag.toString();
	}

	@Override
	public boolean isApplicable(int type, int member) {
		return fTagItems != null && fTagItems.keySet().contains(getTagKey(type, member));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IApiJavadocTag) {
			return ((IApiJavadocTag) obj).getTagName().equals(getTagName());
		}
		if (obj instanceof String) {
			return ((String) obj).equals(getTagName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getTagName().hashCode();
	}

	/**
	 * Returns a key to use for tag when getting / setting comment related
	 * attributes
	 *
	 * @param type
	 * @param member
	 * @return a new key that can be used for map lookups
	 */
	private Integer getTagKey(int type, int member) {
		return Integer.valueOf((type | member) + hashCode());
	}
}
