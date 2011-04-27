/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.pde.api.tools.internal.comparator.DeltaXmlVisitor;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.DeltaProcessor;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * This class is used to exclude some deltas from the generated report.
 */
public class FilterListDeltaVisitor extends DeltaXmlVisitor {
	public static final int CHECK_DEPRECATION = 0x01;
	public static final int CHECK_OTHER = 0x02;
	public static final int CHECK_ALL = CHECK_DEPRECATION | CHECK_OTHER;

	private FilteredElements excludedElements;
	private FilteredElements includedElements;
	private List nonExcludedElements;

	private int flags;
	
	public FilterListDeltaVisitor(FilteredElements excludedElements,FilteredElements includedElements, int flags) throws CoreException {
		super();
		this.excludedElements = excludedElements;
		this.includedElements = includedElements;
		this.nonExcludedElements = new ArrayList();
		this.flags = flags;
	}
	private boolean checkExclude(IDelta delta) {
		return isExcluded(delta);
	}
	public String getPotentialExcludeList() {
		if (this.nonExcludedElements == null) return Util.EMPTY_STRING;
		Collections.sort(this.nonExcludedElements);
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		for (Iterator iterator = this.nonExcludedElements.iterator(); iterator.hasNext(); ) {
			writer.println(iterator.next());
		}
		writer.close();
		return String.valueOf(stringWriter.getBuffer());
	}
	private boolean isExcluded(IDelta delta) {
		String typeName = delta.getTypeName();
		StringBuffer buffer = new StringBuffer();
		String componentId = delta.getComponentId();
		if (componentId != null) {
			if (this.excludedElements.containsExactMatch(componentId)
					|| this.excludedElements.containsPartialMatch(componentId)) {
				return true;
			}
			if (!this.includedElements.isEmpty() && !(this.includedElements.containsExactMatch(componentId)
					|| this.includedElements.containsPartialMatch(componentId))) {
				return true;
			}
			buffer.append(componentId).append(':');
		}
		if (typeName != null) {
			buffer.append(typeName);
		}
		int flags = delta.getFlags();
		switch(flags) {
			case IDelta.TYPE_MEMBER :
				buffer.append('.').append(delta.getKey());
				break;
			case IDelta.API_METHOD :
			case IDelta.API_CONSTRUCTOR :
			case IDelta.API_ENUM_CONSTANT :
			case IDelta.API_FIELD :
			case IDelta.API_METHOD_WITH_DEFAULT_VALUE :
			case IDelta.API_METHOD_WITHOUT_DEFAULT_VALUE :
			case IDelta.METHOD :
			case IDelta.CONSTRUCTOR :
			case IDelta.ENUM_CONSTANT :
			case IDelta.METHOD_WITH_DEFAULT_VALUE :
			case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
			case IDelta.FIELD :
			case IDelta.REEXPORTED_API_TYPE :
			case IDelta.REEXPORTED_TYPE :
			case IDelta.DEPRECATION :
				buffer.append('#').append(delta.getKey());
				break;
			case IDelta.MAJOR_VERSION :
			case IDelta.MINOR_VERSION :
				buffer
					.append(Util.getDeltaFlagsName(flags))
					.append('_')
					.append(Util.getDeltaKindName(delta.getKind()));
				break;
			case IDelta.API_COMPONENT :
				buffer.append(Util.getDeltaKindName(delta.getKind())).append('#').append(delta.getKey());
		}

		String listKey = String.valueOf(buffer);
		if (this.excludedElements.containsExactMatch(listKey)) {
			return true;
		}
		if (!this.includedElements.isEmpty() && !(this.includedElements.containsExactMatch(delta.getKey())
				|| this.includedElements.containsPartialMatch(delta.getKey()))) {
			return true;
		}
		this.nonExcludedElements.add(listKey);
		
		return false;
	}
	protected void processLeafDelta(IDelta delta) {
		if (DeltaProcessor.isCompatible(delta)) {
			switch(delta.getKind()) {
				case IDelta.ADDED :
					int modifiers = delta.getNewModifiers();
					if (Flags.isPublic(modifiers)) {
						if ((this.flags & CHECK_DEPRECATION) != 0) {
							switch(delta.getFlags()) {
								case IDelta.DEPRECATION :
									if (!checkExclude(delta)) {
										super.processLeafDelta(delta);
									}
							}
						}
						if ((this.flags & CHECK_OTHER) != 0) {
							switch(delta.getFlags()) {
								case IDelta.TYPE_MEMBER :
								case IDelta.METHOD :
								case IDelta.CONSTRUCTOR :
								case IDelta.ENUM_CONSTANT :
								case IDelta.METHOD_WITH_DEFAULT_VALUE :
								case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
								case IDelta.FIELD :
								case IDelta.TYPE :
								case IDelta.API_TYPE :
								case IDelta.API_METHOD :
								case IDelta.API_FIELD :
								case IDelta.API_CONSTRUCTOR :
								case IDelta.API_ENUM_CONSTANT :
								case IDelta.REEXPORTED_TYPE :
									if (!checkExclude(delta)) {
										super.processLeafDelta(delta);
									}
									break;
							}
						}
					} else if (Flags.isProtected(modifiers) && !RestrictionModifiers.isExtendRestriction(delta.getCurrentRestrictions())) {
						if ((this.flags & CHECK_DEPRECATION) != 0) {
							switch(delta.getFlags()) {
								case IDelta.DEPRECATION :
									if (!checkExclude(delta)) {
										super.processLeafDelta(delta);
									}
									break;
							}
						}
						if ((this.flags & CHECK_OTHER) != 0) {
							switch(delta.getFlags()) {
								case IDelta.TYPE_MEMBER :
								case IDelta.METHOD :
								case IDelta.CONSTRUCTOR :
								case IDelta.ENUM_CONSTANT :
								case IDelta.FIELD :
								case IDelta.TYPE :
								case IDelta.API_TYPE :
								case IDelta.API_METHOD :
								case IDelta.API_FIELD :
								case IDelta.API_CONSTRUCTOR :
								case IDelta.API_ENUM_CONSTANT :
								case IDelta.REEXPORTED_TYPE :
									if (!checkExclude(delta)) {
										super.processLeafDelta(delta);
									}
									break;
							}
						}
					}
					if (delta.getElementType() == IDelta.API_BASELINE_ELEMENT_TYPE
							&& ((this.flags & CHECK_OTHER) != 0)) {
						switch(delta.getKind()) {
							case IDelta.ADDED :
								if (delta.getFlags() == IDelta.API_COMPONENT) {
									if (!checkExclude(delta)) {
										super.processLeafDelta(delta);
									}
								}
						}
					}
					break;
				case IDelta.CHANGED :
					if ((this.flags & CHECK_OTHER) != 0) {
						switch(delta.getFlags()) {
							case IDelta.MAJOR_VERSION :
							case IDelta.MINOR_VERSION :
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
						}
					}
					break;
				case IDelta.REMOVED :
					if ((this.flags & CHECK_DEPRECATION) != 0) {
						switch(delta.getFlags()) {
							case IDelta.DEPRECATION :
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
						}
					}
			}
		} else if ((this.flags & CHECK_OTHER) != 0) {
			switch(delta.getKind()) {
				case IDelta.ADDED :
					switch(delta.getFlags()) {
						case IDelta.TYPE_MEMBER :
						case IDelta.METHOD :
						case IDelta.CONSTRUCTOR :
						case IDelta.ENUM_CONSTANT :
						case IDelta.METHOD_WITH_DEFAULT_VALUE :
						case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
						case IDelta.FIELD :
						case IDelta.TYPE :
						case IDelta.API_TYPE :
						case IDelta.API_METHOD :
						case IDelta.API_FIELD :
						case IDelta.API_CONSTRUCTOR :
						case IDelta.API_ENUM_CONSTANT :
						case IDelta.REEXPORTED_TYPE :
							if (Util.isVisible(delta.getNewModifiers())) {
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
							}
					}
					break;
				case IDelta.REMOVED :
					switch(delta.getFlags()) {
						case IDelta.TYPE_MEMBER :
						case IDelta.METHOD :
						case IDelta.CONSTRUCTOR :
						case IDelta.ENUM_CONSTANT :
						case IDelta.METHOD_WITH_DEFAULT_VALUE :
						case IDelta.METHOD_WITHOUT_DEFAULT_VALUE :
						case IDelta.FIELD :
						case IDelta.TYPE :
						case IDelta.API_TYPE :
						case IDelta.API_METHOD :
						case IDelta.API_FIELD :
						case IDelta.API_CONSTRUCTOR :
						case IDelta.API_ENUM_CONSTANT :
						case IDelta.REEXPORTED_API_TYPE :
						case IDelta.REEXPORTED_TYPE :
							if (Util.isVisible(delta.getOldModifiers())) {
								if (!checkExclude(delta)) {
									super.processLeafDelta(delta);
								}
							}
							break;
						case IDelta.API_COMPONENT :
							if (!checkExclude(delta)) {
								super.processLeafDelta(delta);
							}
					}
				break;
			}
		}
	}
}