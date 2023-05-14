/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.contentassist;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.swt.graphics.Image;

public class BuildPropertiesContentAssistProcessor extends TypePackageCompletionProcessor {

	protected PDESourcePage fSourcePage;

	public BuildPropertiesContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument doc = fSourcePage.getDocumentProvider().getDocument(fSourcePage.getInputContext().getInput());
		try {
			int lineNum = doc.getLineOfOffset(offset);
			int lineStart = doc.getLineOffset(lineNum);
			String value = doc.get(lineStart, offset - lineStart);
			ArrayList<TypeCompletionProposal> completions = new ArrayList<>();
			Field[] properties = IBuildPropertiesConstants.class.getFields();
			for (Field f : properties) {
				String key = f.getName();
				String element;
				try {
					element = (String) f.get(key);
				} catch (IllegalAccessException e) {
					continue;
				}
				if (element.regionMatches(true, 0, value, 0, value.length())) {
					Image img = getImage(element);
					TypeCompletionProposal proposal = new TypeCompletionProposal(element, img, element, lineStart,
							value.length());
					completions.add(proposal);
				}
			}
			return completions.toArray(ICompletionProposal[]::new);
		} catch (BadLocationException e) {
		}
		return null;
	}

	@SuppressWarnings("nls")
	public Image getImage(String element) {
		ImageDescriptor desc = switch (element)
			{
			case "src.includes", "src.excludes", "src.additionalRoots", "permissions", "root.", // linebreak
					"root", ".permissions.", ".link", "folder.", ".folder.", "link", // linebreak
					"source.", "sourceFileExtensions", "bin.includes", "bin.excludes", "javacCustomEncodings.", // linebreak
					"javacDefaultEncoding." -> PDEPluginImages.DESC_CATEGORY_OBJ;
			case ".jar", "jars.compile.order", "jars.extra.classpath" -> PDEPluginImages.DESC_JAR_LIB_OBJ;
			case "javacProjectSettings" -> PDEPluginImages.DESC_SETTINGS_OBJ;
			case "javacErrors." -> PDEPluginImages.DESC_ERROR_ST_OBJ;
			case "significantVersionDigits", "generatedVersionLength" -> PDEPluginImages.DESC_INFO_ST_OBJ;
			case "javacWarnings." -> PDEPluginImages.DESC_ALERT_OBJ;
			case "jre.compilation.profile" -> PDEPluginImages.DESC_TARGET_ENVIRONMENT;
			case "manifest." -> PDEPluginImages.DESC_FOLDER_OBJ;
			default -> PDEPluginImages.DESC_DEFAULT_OBJ;
			};
		return desc != null ? desc.createImage() : null;
	}
}
