/*******************************************************************************
 * Copyright (c) 2007, 2014 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;

/**
 * Manages contributed javadoc tags. This manager is lazy, in that nothing is
 * loaded until it is asked for.
 *
 * @since 1.0.0
 */
public final class JavadocTagManager {

	/**
	 * Compound key for the annotation cache
	 *
	 * @since 1.0.600
	 */
	class Key {
		int type;
		int member;

		public Key(int t, int m) {
			type = t;
			member = m;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Key) {
				Key other = (Key) obj;
				return type == other.type && member == other.member;
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return type + member;
		}
	}

	/**
	 * Constant for the <code>noinsantiate</code> tag <br>
	 * <br>
	 * Value is: <code>@noinstantiate</code>
	 *
	 * @since 1.0.500
	 */
	public static final String TAG_NOINSTANTIATE = "@noinstantiate"; //$NON-NLS-1$
	/**
	 * Constant for the <code>&#64;NoInstantiate</code> annotation <br>
	 * <br>
	 * Value is: <code>NoInstantiate</code>
	 *
	 * @since 1.0.600
	 */
	public static final String ANNOTATION_NOINSTANTIATE = "NoInstantiate"; //$NON-NLS-1$
	/**
	 * Constant for the <code>noextend</code> tag <br>
	 * <br>
	 * Value is: <code>@noextend</code>
	 *
	 * @since 1.0.500
	 */
	public static final String TAG_NOEXTEND = "@noextend"; //$NON-NLS-1$
	/**
	 * Constant for the <code>&#64;NoExtend</code> annotation <br>
	 * <br>
	 * Value is: <code>NoExtend</code>
	 *
	 * @since 1.0.600
	 */
	public static final String ANNOTATION_NOEXTEND = "NoExtend"; //$NON-NLS-1$
	/**
	 * Constant for the <code>noimplement</code> tag <br>
	 * <br>
	 * Value is: <code>@noimplement</code>
	 *
	 * @since 1.0.500
	 */
	public static final String TAG_NOIMPLEMENT = "@noimplement"; //$NON-NLS-1$
	/**
	 * Constant for the <code>&#64;NoImplement</code> annotation <br>
	 * <br>
	 * Value is: <code>NoImplement</code>
	 *
	 * @since 1.0.600
	 */
	public static final String ANNOTATION_NOIMPLEMENT = "NoImplement"; //$NON-NLS-1$
	/**
	 * Constant for the <code>nooverride</code> tag <br>
	 * <br>
	 * Value is: <code>@nooverride</code>
	 *
	 * @since 1.0.500
	 */
	public static final String TAG_NOOVERRIDE = "@nooverride"; //$NON-NLS-1$
	/**
	 * Constant for the <code>&#64;NoOverride</code> annotation <br>
	 * <br>
	 * Value is: <code>NoOverride</code>
	 *
	 * @since 1.0.600
	 */
	public static final String ANNOTATION_NOOVERRIDE = "NoOverride"; //$NON-NLS-1$
	/**
	 * Constant for the <code>noreference</code> tag <br>
	 * <br>
	 * Value is: <code>@noreference</code>
	 *
	 * @since 1.0.500
	 */
	public static final String TAG_NOREFERENCE = "@noreference"; //$NON-NLS-1$
	/**
	 * Constant for the <code>&#64;NoReference</code> annotation <br>
	 * <br>
	 * Value is: <code>NoReference</code>
	 *
	 * @since 1.0.600
	 */
	public static final String ANNOTATION_NOREFERENCE = "NoReference"; //$NON-NLS-1$

	/**
	 * The collection of all tags
	 *
	 * @see #TAG_NOEXTEND
	 * @see #TAG_NOIMPLEMENT
	 * @see #TAG_NOINSTANTIATE
	 * @see #TAG_NOOVERRIDE
	 * @see #TAG_NOREFERENCE
	 */
	public static final Set<String> ALL_TAGS;

	/**
	 * The collection of all annotation names
	 *
	 * @see #ANNOTATION_NOEXTEND
	 * @see #ANNOTATION_NOIMPLEMENT
	 * @see #ANNOTATION_NOINSTANTIATE
	 * @see #ANNOTATION_NOOVERRIDE
	 * @see #ANNOTATION_NOREFERENCE
	 *
	 * @sine 1.0.600
	 */
	public static final Set<String> ALL_ANNOTATIONS;

	/**
	 * Cache for simple annotation names mapped to their fully qualified name
	 *
	 * @since 1.0.600
	 */
	private static final HashMap<String, String> fqAnnotationNames;

	static {
		HashSet<String> tags = new HashSet<>(5, 1);
		tags.add(TAG_NOEXTEND);
		tags.add(TAG_NOIMPLEMENT);
		tags.add(TAG_NOINSTANTIATE);
		tags.add(TAG_NOOVERRIDE);
		tags.add(TAG_NOREFERENCE);
		ALL_TAGS = Collections.unmodifiableSet(tags);

		tags = new HashSet<>();
		tags.add(ANNOTATION_NOEXTEND);
		tags.add(ANNOTATION_NOIMPLEMENT);
		tags.add(ANNOTATION_NOINSTANTIATE);
		tags.add(ANNOTATION_NOOVERRIDE);
		tags.add(ANNOTATION_NOREFERENCE);
		ALL_ANNOTATIONS = Collections.unmodifiableSet(tags);

		fqAnnotationNames = new HashMap<>();
		fqAnnotationNames.put(ANNOTATION_NOEXTEND, "org.eclipse.pde.api.tools.annotations.NoExtend"); //$NON-NLS-1$
		fqAnnotationNames.put(ANNOTATION_NOIMPLEMENT, "org.eclipse.pde.api.tools.annotations.NoImplement"); //$NON-NLS-1$
		fqAnnotationNames.put(ANNOTATION_NOINSTANTIATE, "org.eclipse.pde.api.tools.annotations.NoInstantiate"); //$NON-NLS-1$
		fqAnnotationNames.put(ANNOTATION_NOOVERRIDE, "org.eclipse.pde.api.tools.annotations.NoOverride"); //$NON-NLS-1$
		fqAnnotationNames.put(ANNOTATION_NOREFERENCE, "org.eclipse.pde.api.tools.annotations.NoReference"); //$NON-NLS-1$
	}

	/**
	 * Cache for the contributed javadoc tags. Cache form:
	 */
	private HashMap<String, IApiJavadocTag> tagcache = null;

	/**
	 * Cache of annotations keyed by the member the apply to
	 *
	 * @since 1.0.600
	 */
	private HashMap<Key, Set<String>> fAnnotationCache = null;

	/**
	 * Collection of tag extensions
	 */
	private IApiJavadocTag[] tags;

	/**
	 * Initialize the annotation mapping
	 *
	 * @since 1.0.600
	 */
	private void initializeAnnotations() {
		if (fAnnotationCache == null) {
			fAnnotationCache = new HashMap<>();
			HashSet<String> annots = new HashSet<>();
			annots.add(ANNOTATION_NOEXTEND);
			annots.add(ANNOTATION_NOINSTANTIATE);
			annots.add(ANNOTATION_NOREFERENCE);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE), annots);
			annots = new HashSet<>();
			annots.add(ANNOTATION_NOEXTEND);
			annots.add(ANNOTATION_NOIMPLEMENT);
			annots.add(ANNOTATION_NOREFERENCE);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_NONE), annots);
			annots = new HashSet<>();
			annots.add(ANNOTATION_NOOVERRIDE);
			annots.add(ANNOTATION_NOREFERENCE);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_METHOD), annots);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_CONSTRUCTOR), annots);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_METHOD), annots);
			annots = new HashSet<>();
			annots.add(ANNOTATION_NOREFERENCE);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_FIELD), annots);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_FIELD), annots);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_NONE), annots);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_NONE), annots);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_FIELD), annots);
			fAnnotationCache.put(new Key(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_METHOD), annots);

		}
	}

	/**
	 * Initializes the cache of contributed Javadoc tags. If the cache has
	 * already been initialized no work is done.
	 */
	private void initializeJavadocTags() {
		if (tagcache == null) {
			tagcache = new LinkedHashMap<>();
			List<ApiJavadocTag> list = new ArrayList<>(4);

			// noimplement tag
			ApiJavadocTag newtag = new ApiJavadocTag(IApiJavadocTag.NO_IMPLEMENT_TAG_ID, "noimplement", //$NON-NLS-1$
			RestrictionModifiers.NO_IMPLEMENT);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_NONE, CoreMessages.JavadocTagManager_interface_no_implement);
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);

			// noextend tag
			newtag = new ApiJavadocTag(IApiJavadocTag.NO_EXTEND_TAG_ID, "noextend", //$NON-NLS-1$
			RestrictionModifiers.NO_EXTEND);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE, CoreMessages.JavadocTagManager_class_no_subclass);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_NONE, CoreMessages.JavadocTagManager_interface_no_extend);
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);

			// nooverride tag
			newtag = new ApiJavadocTag(IApiJavadocTag.NO_OVERRIDE_TAG_ID, "nooverride", //$NON-NLS-1$
			RestrictionModifiers.NO_OVERRIDE);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_METHOD, CoreMessages.JavadocTagManager_method_no_overried);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_METHOD, CoreMessages.JavadocTagManager_default_method_no_override);
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);

			// noinstantiate tag
			newtag = new ApiJavadocTag(IApiJavadocTag.NO_INSTANTIATE_TAG_ID, "noinstantiate", //$NON-NLS-1$
			RestrictionModifiers.NO_INSTANTIATE);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE, CoreMessages.JavadocTagManager_class_no_instantiate);
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);

			// noreference tag
			newtag = new ApiJavadocTag(IApiJavadocTag.NO_REFERENCE_TAG_ID, "noreference", //$NON-NLS-1$
			RestrictionModifiers.NO_REFERENCE);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_NONE, CoreMessages.JavadocTagManager_class_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_METHOD, CoreMessages.JavadocTagManager_method_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_CONSTRUCTOR, CoreMessages.JavadocTagManager_constructor_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_CLASS, IApiJavadocTag.MEMBER_FIELD, CoreMessages.JavadocTagManager_field_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_METHOD, CoreMessages.JavadocTagManager_method_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_FIELD, CoreMessages.JavadocTagManager_field_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_INTERFACE, IApiJavadocTag.MEMBER_NONE, CoreMessages.JavadocTagManager_interface_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_NONE, CoreMessages.JavadocTagManager_enum_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_FIELD, CoreMessages.JavadocTagManager_enum_field_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_ENUM, IApiJavadocTag.MEMBER_METHOD, CoreMessages.JavadocTagManager_enum_method_no_reference);
			newtag.setApplicableTo(IApiJavadocTag.TYPE_ANNOTATION, IApiJavadocTag.MEMBER_NONE, CoreMessages.JavadocTagManager_annotation_no_reference);
			tagcache.put(newtag.getTagId(), newtag);
			list.add(newtag);
			tags = list.toArray(new IApiJavadocTag[list.size()]);
		}
	}

	/**
	 * Returns all of the java doc tags for a given kind of type and member. See
	 * {@link IApiJavadocTag} for a complete listing of tag Java type and member
	 * types.
	 *
	 * @param type one of <code>CLASS</code> or <code>INTERFACE</code>
	 * @param member one of <code>METHOD</code> or <code>FIELD</code> or
	 *            <code>NONE</code>
	 * @return an array of {@link IApiJavadocTag}s that apply to the specified
	 *         Java type or an empty array, never <code>null</code>
	 */
	public synchronized IApiJavadocTag[] getTagsForType(int type, int member) {
		initializeJavadocTags();
		List<IApiJavadocTag> list = new ArrayList<>();
		for (IApiJavadocTag tag : tags) {
			if (tag.isApplicable(type, member)) {
				list.add(tag);
			}
		}
		return list.toArray(new IApiJavadocTag[list.size()]);
	}

	/**
	 * Returns the set of supported annotations for the given type and member
	 *
	 * @param type the type kind
	 * @param member the member kind
	 * @return the set of supported annotations or an empty set, never
	 *         <code>null</code>
	 * @since 1.0.600
	 */
	public synchronized Set<String> getAnntationsForType(int type, int member) {
		initializeAnnotations();
		Set<String> values = fAnnotationCache.get(new Key(type, member));
		if (values != null) {
			return values;
		}
		return Collections.EMPTY_SET;
	}

	/**
	 * Returns the fully qualified name of the class providing the annotation
	 * with the given simple type name. <code>null</code> is returned if the
	 * annotation is unknown. <br>
	 * <br>
	 * Example: <code>NoExtend</code> returns
	 * <code>org.eclipse.pde.api.toools.annnotations.NoExtend</code>
	 *
	 * @param typename
	 * @return the fully qualified type name of the annotation or
	 *         <code>null</code> if unknown
	 *
	 * @since 1.0.600
	 */
	public synchronized String getQualifiedNameForAnnotation(String typename) {
		return fqAnnotationNames.get(typename);
	}

	/**
	 * Returns the {@link IApiJavadocTag} that has the given id or
	 * <code>null</code> if there is no tag with the given id
	 *
	 * @param id the id of the tag to fetch
	 * @return the {@link IApiJavadocTag} with the given id or <code>null</code>
	 */
	public synchronized IApiJavadocTag getTag(String id) {
		initializeJavadocTags();
		return tagcache.get(id);
	}

	/**
	 * Returns the complete listing of {@link IApiJavadocTag}s contained in the
	 * manager or an empty array, never <code>null</code>
	 *
	 * @return the complete listing of tags in the manager or <code>null</code>
	 */
	public synchronized IApiJavadocTag[] getAllTags() {
		initializeJavadocTags();
		if (tagcache == null) {
			return new IApiJavadocTag[0];
		}
		Collection<IApiJavadocTag> values = tagcache.values();
		return values.toArray(new IApiJavadocTag[values.size()]);
	}

	/**
	 * @return The complete set of tags names that this manager currently knows
	 *         about.
	 */
	public synchronized Set<String> getAllTagNames() {
		IApiJavadocTag[] tags = getAllTags();
		HashSet<String> names = new HashSet<>(tags.length);
		for (IApiJavadocTag tag : tags) {
			names.add(tag.getTagName());
		}
		return names;
	}

	/**
	 * Returns the restriction modifier set on the javadoc tag with the given
	 * name. If the manager has no entry for the specified tag name
	 * <code>-1</code> is returned.
	 *
	 * @param tagname the name of the tag
	 * @param type one of <code>CLASS</code> or <code>INTERFACE</code>
	 * @param member one of <code>METHOD</code> or <code>FIELD</code> or
	 *            <code>NONE</code>
	 * @return the restriction modifier for the given tag name or
	 *         {@link RestrictionModifiers#NO_RESTRICTIONS} if not found
	 */
	public synchronized int getRestrictionsForTag(String tagname, int type, int member) {
		if (tagname == null) {
			return RestrictionModifiers.NO_RESTRICTIONS;
		}
		initializeJavadocTags();
		for (IApiJavadocTag tag : tags) {
			if (tag.getTagName().equals(tagname) && (tag.isApplicable(type, member))) {
				return tag.getRestrictionModifier();
			}
		}
		return RestrictionModifiers.NO_RESTRICTIONS;
	}
}
