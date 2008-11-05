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
	private HashMap fTagItems = null;
	
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiJavadocTag#getTagId()
	 */
	public String getTagId() {
		return fId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiJavadocTag#getRestrictionModifier()
	 */
	public int getRestrictionModifier() {
		return fRModifier;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag#setApplicableTo(int, int, java.lang.String)
	 */
	public void setApplicableTo(int type, int member, String comment) {
		if(fTagItems == null) {
			fTagItems = new HashMap(6);
		}
		fTagItems.put(getTagKey(type, member), comment);
	}
	
	/**
	 * Returns the comment for the given type ad member
	 * @param type
	 * @param member
	 * @return the comment for the tag
	 */
	public String getTagComment(int type, int member) {
		if(fTagItems == null) {
			return EMPTY_STRING;
		}
		Object obj = fTagItems.get(getTagKey(type, member));
		return (String) (obj == null ? EMPTY_STRING : obj);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiJavadocTag#getTagLabel()
	 */
	public String getTagName() {
		if(fTaglabel == null) {
			StringBuffer tag = new StringBuffer();
			tag.append("@"); //$NON-NLS-1$
			tag.append(fName);
			fTaglabel = tag.toString();
		}
		return fTaglabel;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getTagName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiJavadocTag#getCompleteTag(int, int)
	 */
	public String getCompleteTag(int type, int member) {
		StringBuffer tag = new StringBuffer();
		tag.append(getTagName());
		String comment = getTagComment(type, member);
		if(EMPTY_STRING.equals(comment)) {
			return tag.toString();
		}
		tag.append(" "); //$NON-NLS-1$
		tag.append(comment);
		return tag.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiJavadocTag#isApplicable(int, int)
	 */
	public boolean isApplicable(int type, int member) {
		return fTagItems != null && fTagItems.keySet().contains(getTagKey(type, member));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof IApiJavadocTag) {
			return ((IApiJavadocTag)obj).getTagName().equals(getTagName());
		}
		if(obj instanceof String) {
			return ((String)obj).equals(getTagName());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return getTagName().hashCode();
	}
	
	/**
	 * Returns a key to use for tag when getting / setting comment related attributes
	 * @param type
	 * @param member
	 * @return a new key that can be used for map lookups
	 */
	private Integer getTagKey(int type, int member) {
		return new Integer((type | member) + hashCode());
	}
}
