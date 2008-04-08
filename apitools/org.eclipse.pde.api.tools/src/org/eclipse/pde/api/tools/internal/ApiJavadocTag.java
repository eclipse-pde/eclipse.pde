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

import org.eclipse.core.runtime.Assert;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;

/**
 * Base implementation of an {@link IApiJavadocTag}. Acts as a delegate
 * to a contributed doc tag (via plugins.xml)
 * 
 * @since 1.0.0
 */
public class ApiJavadocTag implements IApiJavadocTag {

	/**
	 * Array of element specific comments, or <code>null</code> if not applicable.
	 * Indexed by type and member constants in {@link IApiJavadocTag}.
	 */
	private String[][] comments = null;

	/**
	 * The id of the tag
	 */
	private String fId = null;
	/**
	 * The name of the tag (immediately proceeding the '@' symbol
	 */
	private String fName = null;
	/**
	 * collection of elements this tag applies to
	 */
	private int[] fElements = null;
	/**
	 * matching collection of comments for the elements
	 */
	private String[] fComments = null;
	/**
	 * restriction modifier for the tag
	 */
	private int fRModifier = RestrictionModifiers.NO_RESTRICTIONS;

	/**
	 * Lazily computed tag label, cached once it has been computed
	 */
	private String taglabel = null;
	
	/**
	 * Constructor
	 * @param id the id of the tag
	 * @param name the name of the tag (not including the '@' symbol)
	 * @param rmodifier 
	 * @param elements
	 * @param comments
	 */
	public ApiJavadocTag(String id, String name, int rmodifier, int[] elements, String[] comments) {
		Assert.isNotNull(id);
		fId = id;
		Assert.isNotNull(name);
		fName = name;
		Assert.isNotNull(elements);
		fElements = elements;
		fComments = comments;
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

	/**
	 * Returns the comment for the given type ad member
	 * @param type
	 * @param member
	 * @return the comment for the tag
	 */
	protected String getTagComment(int type, int member) {
		initializeElements();
		int i1 = 0;
		if(type == IApiJavadocTag.TYPE_INTERFACE) {
			i1 = 1;
		}
		int i2 = 2;
		if(member == IApiJavadocTag.MEMBER_FIELD) {
			i2 = 1;
		}
		else if(member == IApiJavadocTag.MEMBER_METHOD) {
			i2 = 0;
		}
		else if(member == IApiJavadocTag.MEMBER_CONSTRUCTOR) {
			i2 = 3;
		}
		return comments[i1][i2];
	}
	
	/**
	 * Initializes the cache of java element this tag applies to
	 */
	private void initializeElements() {
		if(comments == null) {
			comments = new String[2][4];
			for(int i = 0; i < fElements.length; i++) {
				boolean clazz = (IApiJavadocTag.TYPE_CLASS & fElements[i]) != 0;
				boolean inter = (IApiJavadocTag.TYPE_INTERFACE & fElements[i]) != 0;
				boolean method = (IApiJavadocTag.MEMBER_METHOD & fElements[i]) != 0;
				boolean field = (IApiJavadocTag.MEMBER_FIELD & fElements[i]) != 0;
				boolean constructor = (IApiJavadocTag.MEMBER_CONSTRUCTOR & fElements[i]) != 0;
				if(clazz) {
					if(constructor) {
						comments[0][3] = fComments[i];
					}
					if (method) {
						comments[0][0] = fComments[i];
					}
					if (field) {
						comments[0][1] = fComments[i];
					}
					if(!field & !method & !constructor) {
						comments[0][2] = fComments[i];
					}
				}
				if(inter) {
					if (method) {
						comments[1][0] = fComments[i];
					}
					if (field) {
						comments[1][1] = fComments[i];
					}
					if(!field & !method) {
						comments[1][2] = fComments[i];
					}
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiJavadocTag#getTagLabel()
	 */
	public String getTagLabel() {
		if(taglabel == null) {
			StringBuffer tag = new StringBuffer();
			tag.append("@"); //$NON-NLS-1$
			tag.append(fName);
			taglabel = tag.toString();
		}
		return taglabel;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getTagLabel();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiJavadocTag#getCompleteTag(int, int)
	 */
	public String getCompleteTag(int type, int member) {
		StringBuffer tag = new StringBuffer();
		tag.append("@"); //$NON-NLS-1$
		tag.append(fName);
		tag.append(" "); //$NON-NLS-1$
		tag.append(getTagComment(type, member));
		return tag.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiJavadocTag#isApplicable(int, int)
	 */
	public boolean isApplicable(int type, int member) {
		initializeElements();
		return getTagComment(type, member) != null;
	}
}
