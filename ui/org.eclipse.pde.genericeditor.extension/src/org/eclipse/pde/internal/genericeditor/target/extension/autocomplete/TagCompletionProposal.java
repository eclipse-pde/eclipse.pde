/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.autocomplete;

import java.util.HashMap;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;

public class TagCompletionProposal extends TargetCompletionProposal {

	private static final HashMap<String, Attribute[]> tagStartingAttributesAndValues = new HashMap<>();

	static {
		tagStartingAttributesAndValues.put(ITargetConstants.TARGET_TAG, new Attribute[] { 
				new Attribute(ITargetConstants.TARGET_NAME_ATTR, null),
				new Attribute(ITargetConstants.TARGET_SEQ_NO_ATTR, "1") });
		tagStartingAttributesAndValues.put(ITargetConstants.LOCATION_DIRECTORY_COMPLETION_LABEL, new Attribute[] {
				new Attribute(ITargetConstants.LOCATION_PATH_ATTR, null),
				new Attribute(ITargetConstants.LOCATION_TYPE_ATTR, ITargetConstants.LOCATION_TYPE_ATTR_VALUE_DIRECTORY) });
		tagStartingAttributesAndValues.put(ITargetConstants.LOCATION_FEATURE_COMPLETION_LABEL, new Attribute[] {
				new Attribute(ITargetConstants.LOCATION_PATH_ATTR, null),
				new Attribute(ITargetConstants.LOCATION_ID_ATTR, null),
				new Attribute(ITargetConstants.LOCATION_TYPE_ATTR, ITargetConstants.LOCATION_TYPE_ATTR_VALUE_FEATURE) });
		tagStartingAttributesAndValues.put(ITargetConstants.LOCATION_IU_COMPLETION_LABEL, new Attribute[] {
				new Attribute(ITargetConstants.LOCATION_TYPE_ATTR, ITargetConstants.LOCATION_TYPE_ATTR_VALUE_IU) });
		tagStartingAttributesAndValues.put(ITargetConstants.LOCATION_PROFILE_COMPLETION_LABEL, new Attribute[] {
				new Attribute(ITargetConstants.LOCATION_PATH_ATTR, null),
				new Attribute(ITargetConstants.LOCATION_TYPE_ATTR, ITargetConstants.LOCATION_TYPE_ATTR_VALUE_PROFILE) });
		tagStartingAttributesAndValues.put(ITargetConstants.UNIT_TAG, new Attribute[] {
				new Attribute(ITargetConstants.UNIT_ID_ATTR, null),
				new Attribute(ITargetConstants.UNIT_VERSION_ATTR, ITargetConstants.UNIT_VERSION_ATTR_GENERIC) });
		tagStartingAttributesAndValues.put(ITargetConstants.REPOSITORY_TAG, new Attribute[] {
				new Attribute(ITargetConstants.REPOSITORY_LOCATION_ATTR, null) });
		tagStartingAttributesAndValues.put(ITargetConstants.TARGET_JRE_TAG, new Attribute[] {
				new Attribute(ITargetConstants.TARGET_JRE_PATH_ATTR, null) });
	}

	private static class Attribute {
		public String name;
		public String value;

		public Attribute(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			if (value == null) {
				return name + "=\"\"";
			}
			return name + "=\"" + value + "\"";
		}
	}

	public TagCompletionProposal(String tagType, int replacementOffset,
			int replacementLength, StyledString displayString) {
		super(getReplacementStringFromTagType(tagType),
				getcursorPositionFromReplacementString(getReplacementStringFromTagType(tagType)), replacementOffset,
				replacementLength, displayString);
	}

	private static String getReplacementStringFromTagType(String tagType) {
		String handyAddition;
		String tagName = getTagNameFromTageType(tagType);

		if (tagType.equalsIgnoreCase(ITargetConstants.UNIT_TAG)
				|| tagType.equalsIgnoreCase(ITargetConstants.REPOSITORY_TAG)
				|| tagType.equalsIgnoreCase(ITargetConstants.TARGET_JRE_TAG)
				|| tagType.equalsIgnoreCase(ITargetConstants.LOCATION_PROFILE_COMPLETION_LABEL)
				|| tagType.equalsIgnoreCase(ITargetConstants.LOCATION_DIRECTORY_COMPLETION_LABEL)
				|| tagType.equalsIgnoreCase(ITargetConstants.LOCATION_FEATURE_COMPLETION_LABEL)) {
			handyAddition = "/>";
		} else {
			handyAddition = "></" + tagName + ">";
		}
		if (tagStartingAttributesAndValues.containsKey(tagType)) {
			for (Attribute attribute : tagStartingAttributesAndValues.get(tagType)) {
				tagName += " " + attribute.toString();
			}
		}
		return tagName + handyAddition;
	}
	
	private static String getTagNameFromTageType(String tagType) {
		if (ITargetConstants.LOCATION_DIRECTORY_COMPLETION_LABEL.equals(tagType)
				|| ITargetConstants.LOCATION_IU_COMPLETION_LABEL.equals(tagType)
				|| ITargetConstants.LOCATION_PROFILE_COMPLETION_LABEL.equals(tagType)
				|| ITargetConstants.LOCATION_FEATURE_COMPLETION_LABEL.equals(tagType)) {
			return ITargetConstants.LOCATION_TAG;
		}
		return tagType;
	}

	private static int getcursorPositionFromReplacementString(String replacementString) {
		int emptyAttributeIndex = replacementString.indexOf("\"\"");
		if (emptyAttributeIndex >= 0)
			return emptyAttributeIndex + 1;
		int tagEndingIndex = replacementString.indexOf('>');
		if (tagEndingIndex >= 0)
			return tagEndingIndex + 1;
		return replacementString.length() + 1;
	}

}
