/********************************************************************************
 * Copyright (c) 2018 ArSysOp and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - - initial API and implementation
 ********************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.expressions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.ui.IEditorInput;

public class EditorInputPropertyTester extends PropertyTester {

	private static final String CONTENT_TYPE_ID = "contentTypeId"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (receiver instanceof IEditorInput) {
			IEditorInput editorInput = (IEditorInput) receiver;
			if (CONTENT_TYPE_ID.equals(property)) {
				String identifier = String.valueOf(expectedValue);
				IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
				IContentType expected = contentTypeManager.getContentType(identifier);
				if (expected != null) {
					IContentType[] contentTypesFor = contentTypeManager.findContentTypesFor(editorInput.getName());
					for (IContentType contentType : contentTypesFor) {
						if (expected.equals(contentType)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

}
