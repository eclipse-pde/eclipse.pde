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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;

/**
 * Manages contributed javadoc tags. This manager is lazy, in that
 * nothing is loaded until it is asked for.
 * 
 * @since 1.0.0
 */
public final class JavadocTagManager {

	/**
	 * Cache for the contributed javadoc tags. 
	 * Cache form:
	 * <pre>
	 * HashMap<String(id), tag>
	 * </pre>
	 */
	private HashMap tagcache = null;
	
	/**
	 * Collection of tag extensions
	 */
	private IApiJavadocTag[] tags;
	
	/**
	 * Initializes the cache of contributed Javadoc tags.
	 * If the cache has already been initialized no work is done.
	 */
	private void initializeJavadocTags() {
		if(tagcache == null) {
			tagcache = new HashMap();
			List list = new ArrayList(4);
			
			//noimplement tag
			ApiJavadocTag newtag = new ApiJavadocTag(IApiJavadocTag.NO_IMPLEMENT_TAG_ID, 
					"noimplement", //$NON-NLS-1$
					RestrictionModifiers.NO_IMPLEMENT);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, 
					IApiJavadocTag.MEMBER_NONE, 
					"This interface is not intended to be implemented by clients.");   //$NON-NLS-1$
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);
			
			//noextend tag
			newtag = new ApiJavadocTag(IApiJavadocTag.NO_EXTEND_TAG_ID, 
					"noextend", //$NON-NLS-1$
					RestrictionModifiers.NO_EXTEND);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, 
					IApiJavadocTag.MEMBER_NONE, 
					"This class is not intended to be subclassed by clients.");  //$NON-NLS-1$
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, 
					IApiJavadocTag.MEMBER_NONE, 
					"This interface is not intended to be extended by clients.");  //$NON-NLS-1$
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);
			
			//nooverride tag
			newtag = new ApiJavadocTag(IApiJavadocTag.NO_OVERRIDE_TAG_ID,
					"nooverride", //$NON-NLS-1$
					RestrictionModifiers.NO_OVERRIDE);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, 
					 IApiJavadocTag.MEMBER_METHOD, 
					 "This method is not intended to be re-implemented or extended by clients.");  //$NON-NLS-1$
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);
			
			//noinstantiate tag
			newtag = new ApiJavadocTag(IApiJavadocTag.NO_INSTANTIATE_TAG_ID,
					"noinstantiate", //$NON-NLS-1$
					RestrictionModifiers.NO_INSTANTIATE);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS,
					IApiJavadocTag.MEMBER_NONE, 
					"This class is not intended to be instantiated by clients.");  //$NON-NLS-1$
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);
			
			//noreference tag
			newtag = new ApiJavadocTag(IApiJavadocTag.NO_REFERENCE_TAG_ID,
					"noreference", //$NON-NLS-1$
					RestrictionModifiers.NO_REFERENCE);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, 
					IApiJavadocTag.MEMBER_METHOD, 
					"This method is not intended to be referenced by clients."); //$NON-NLS-1$
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, 
					IApiJavadocTag.MEMBER_METHOD, 
					"This method is not intended to be referenced by clients."); //$NON-NLS-1$
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, 
					IApiJavadocTag.MEMBER_FIELD, 
					"This field is not intended to be referenced by clients."); //$NON-NLS-1$
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, 
					IApiJavadocTag.MEMBER_FIELD, 
					"This field is not intended to be referenced by clients."); //$NON-NLS-1$
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, 
					IApiJavadocTag.MEMBER_CONSTRUCTOR, 
					"This constructor is not intended to be referenced by clients."); //$NON-NLS-1$
			newtag.setApplicableTo(IApiJavadocTag.TYPE_ENUM, 
					IApiJavadocTag.MEMBER_FIELD, 
					"This enum field is not intended to be referenced by clients."); //$NON-NLS-1$
			newtag.setApplicableTo(IApiJavadocTag.TYPE_ENUM, 
					IApiJavadocTag.MEMBER_METHOD, 
					"This enum method is not intended to be referenced by clients."); //$NON-NLS-1$
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);
			tags = (IApiJavadocTag[]) list.toArray(new IApiJavadocTag[list.size()]);
		}
	}
	
	/**
	 * Returns all of the java doc tags for a given kind of type and member. See
	 * {@link IApiJavadocTag} for a complete listing of tag Java type and member types.
	 * 
	 * @param type one of <code>CLASS</code> or <code>INTERFACE</code>
	 * @param member one of <code>METHOD</code> or <code>FIELD</code> or <code>NONE</code> 
	 * @return an array of {@link IApiJavadocTag}s that apply to the specified 
	 * Java type or an empty array, never <code>null</code>
	 */
	public synchronized IApiJavadocTag[] getTagsForType(int type, int member) {
		initializeJavadocTags();
		List list = new ArrayList();
		for (int i = 0; i < tags.length; i++) {
			if (tags[i].isApplicable(type, member)) {
				list.add(tags[i]);
			}
		}
		return (IApiJavadocTag[]) list.toArray(new IApiJavadocTag[list.size()]);
	}
	
	/**
	 * Returns the {@link IApiJavadocTag} that has the given id or <code>null</code> if there is 
	 * no tag with the given id
	 * @param id the id of the tag to fetch
	 * @return the {@link IApiJavadocTag} with the given id or <code>null</code>
	 */
	public synchronized IApiJavadocTag getTag(String id) {
		initializeJavadocTags();
		return (IApiJavadocTag) tagcache.get(id);
	}
	
	/**
	 * Returns the complete listing of {@link IApiJavadocTag}s contained in the manager or an empty 
	 * array, never <code>null</code>
	 * @return the complete listing of tags in the manager or <code>null</code>
	 */
	public synchronized IApiJavadocTag[] getAllTags() {
		initializeJavadocTags();
		if(tagcache == null) {
			return new IApiJavadocTag[0];
		}
		Collection values = tagcache.values();
		return (IApiJavadocTag[]) values.toArray(new IApiJavadocTag[values.size()]);
	}
	
	/**
	 * @return The complete set of tags names that this manager currently knows about.
	 */
	public synchronized Set getAllTagNames() {
		IApiJavadocTag[] tags = getAllTags();
		HashSet names = new HashSet(tags.length);
		for(int i = 0; i < tags.length; i++) {
			names.add(tags[i].getTagName());
		}
		return names;
	}
	
	/**
	 * Returns the restriction modifier set on the javadoc tag with the given name.
	 * If the manager has no entry for the specified tag name <code>-1</code> is returned.
	 * 
	 * @param tagname the name of the tag
	 * @param type one of <code>CLASS</code> or <code>INTERFACE</code>
	 * @param member one of <code>METHOD</code> or <code>FIELD</code> or <code>NONE</code> 
	 * @return the restriction modifier for the given tag name or {@link RestrictionModifiers#NO_RESTRICTIONS} if not found
	 */
	public synchronized int getRestrictionsForTag(String tagname, int type, int member) {
		if(tagname == null) {
			return RestrictionModifiers.NO_RESTRICTIONS;
		}
		initializeJavadocTags();
		ApiJavadocTag tag = null;
		for (int i = 0; i < tags.length; i++) {
			tag = (ApiJavadocTag) tags[i];
			if (tag.getTagName().equals(tagname) && (tag.isApplicable(type, member))) {
				return tag.getRestrictionModifier();
			}
		}
		return RestrictionModifiers.NO_RESTRICTIONS;
	}
}
