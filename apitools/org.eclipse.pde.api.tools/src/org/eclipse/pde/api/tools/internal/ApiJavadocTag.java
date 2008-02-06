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
package org.eclipse.pde.api.tools.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;

/**
 * Base implementation of an {@link IApiJavadocTag}. Acts as a delegate
 * to a contributed doc tag (via plugins.xml)
 * 
 * @since 1.0.0
 */
public class ApiJavadocTag implements IApiJavadocTag {

	/**
	 * configuration element tag names
	 */
	private static final String TAGNAME = "tagname"; //$NON-NLS-1$
	private static final String COMMENT = "comment"; //$NON-NLS-1$
	private static final String TAGID = "tagid"; //$NON-NLS-1$
	private static final String JAVA_ELEMENT = "javaElement"; //$NON-NLS-1$
	private static final String CLASS = "class"; //$NON-NLS-1$
	private static final String INTERFACE = "interface"; //$NON-NLS-1$
	private static final String FIELD = "field";  //$NON-NLS-1$
	private static final String METHOD = "method";  //$NON-NLS-1$
	private static final String VISIBILITY = "visibilitymodifier"; //$NON-NLS-1$
	private static final String RESTRICTION = "restrictionmodifier"; //$NON-NLS-1$
	
	/**
	 * The backing {@link IConfigurationElement} for this tag
	 */
	private IConfigurationElement element = null;

	/**
	 * Array of element specific comments, or <code>null</code> if not applicable.
	 * Indexed by type and member constants in {@link IApiJavadocTag}.
	 */
	private String[][] comments = null;

	private String taglabel = null;
	
	/**
	 * Constructor
	 * @param element
	 */
	public ApiJavadocTag(IConfigurationElement element) {
		Assert.isNotNull(element);
		this.element = element;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiJavadocTag#getRestrictionModifier()
	 */
	public int getRestrictionModifier() {
		String restriction = element.getAttribute(RESTRICTION);
		if("noimplement".equals(restriction)) { //$NON-NLS-1$
			return RestrictionModifiers.NO_IMPLEMENT;
		}
		if("noextend".equals(restriction)) { //$NON-NLS-1$
			return RestrictionModifiers.NO_EXTEND;
		}
		if("noinstantiate".equals(restriction)) { //$NON-NLS-1$
			return RestrictionModifiers.NO_INSTANTIATE;	
		}
		if("noreference".equals(restriction)) { //$NON-NLS-1$
			return RestrictionModifiers.NO_REFERENCE;
		}
		return RestrictionModifiers.NO_RESTRICTIONS;
	}

	protected String getTagComment(int type, int member) {
		initializeElements();
		return comments[type][member];
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiJavadocTag#getTagId()
	 */
	public String getTagId() {
		return element.getAttribute(TAGID);
	}

	
	/**
	 * @return the name of the tag, removes a starting '@' symbol if it appears
	 */
	public String getTagName() {
		String tag = element.getAttribute(TAGNAME);
		return (tag.charAt(0) == '@' ? tag.substring(1) : tag);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiJavadocTag#getVisibilityModifier()
	 */
	public int getVisibilityModifier() {
		String visibility = element.getAttribute(VISIBILITY);
		if("spi".equals(visibility)) { //$NON-NLS-1$
			return VisibilityModifiers.SPI;
		}
		if("private".equals(visibility)) { //$NON-NLS-1$
			return VisibilityModifiers.PRIVATE;
		}
		if("private permissible".equals(visibility)) { //$NON-NLS-1$
			return VisibilityModifiers.PRIVATE_PERMISSIBLE; 
		}
		return VisibilityModifiers.API;
	}
	
	/**
	 * Initializes the cache of java element this tag applies to
	 */
	private void initializeElements() {
		if(comments == null) {
			comments = new String[2][3];
			IConfigurationElement[] children = element.getChildren(JAVA_ELEMENT);
			for(int i = 0; i < children.length; i++) {
				IConfigurationElement child = children[i];
				boolean clazz = getBoolean(child, CLASS, false);
				boolean inter = getBoolean(child, INTERFACE, false);
				boolean method = getBoolean(child, METHOD, false);
				boolean field = getBoolean(child, FIELD, false);
				String comment = child.getAttribute(COMMENT);
				if (clazz) {
					if (method) {
						comments[IApiJavadocTag.TYPE_CLASS][IApiJavadocTag.MEMBER_METHOD] = comment;
					}
					if (field) {
						comments[IApiJavadocTag.TYPE_CLASS][IApiJavadocTag.MEMBER_FIELD] = comment;
					}
					if (!method && !field) {
						comments[IApiJavadocTag.TYPE_CLASS][IApiJavadocTag.MEMBER_NONE] = comment;
					}
				}
				if (inter) {
					if (method) {
						comments[IApiJavadocTag.TYPE_INTERFACE][IApiJavadocTag.MEMBER_METHOD] = comment;
					}
					if (field) {
						comments[IApiJavadocTag.TYPE_INTERFACE][IApiJavadocTag.MEMBER_FIELD] = comment;
					}
					if (!method && !field) {
						comments[IApiJavadocTag.TYPE_INTERFACE][IApiJavadocTag.MEMBER_NONE] = comment;
					}
				}
			}
		}
	}
	
	/**
	 * Returns a boolean value from an XML attribute.
	 * 
	 * @param element configuration element
	 * @param attribute attribute name
	 * @param def default value of the attribute if unspecified
	 * @return attribute value
	 */
	private boolean getBoolean(IConfigurationElement element, String attribute, boolean def) {
		String value = element.getAttribute(attribute);
		if (value == null) {
			return def;
		}
		return Boolean.valueOf(value).booleanValue();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiJavadocTag#getTagCompletion(int, int)
	 */
	public String getTagCompletion(int type, int member) {
		StringBuffer tag = new StringBuffer();
		tag.append(getTagName());
		String comment = getTagComment(type, member);
		if(comment != null) {
			tag.append(" "); //$NON-NLS-1$
			tag.append(comment);
		}
		return tag.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.IApiJavadocTag#getTagLabel()
	 */
	public String getTagLabel() {
		if(taglabel == null) {
			StringBuffer tag = new StringBuffer();
			tag.append("@"); //$NON-NLS-1$
			tag.append(getTagName());
			taglabel = tag.toString();
		}
		return taglabel;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer tag = new StringBuffer();
		tag.append("@"); //$NON-NLS-1$
		tag.append(getTagName());
		return tag.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiJavadocTag#getCompleteTag(int, int)
	 */
	public String getCompleteTag(int type, int member) {
		StringBuffer tag = new StringBuffer();
		tag.append("@"); //$NON-NLS-1$
		tag.append(getTagName());
		tag.append(" "); //$NON-NLS-1$
		tag.append(getTagComment(type, member));
		return tag.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiJavadocTag#isApplicable(int, int)
	 */
	public boolean isApplicable(int type, int member) {
		initializeElements();
		return comments[type][member] != null;
	}
}
