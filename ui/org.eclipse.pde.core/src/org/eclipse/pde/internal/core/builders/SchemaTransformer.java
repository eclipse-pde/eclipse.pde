/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Locale;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.osgi.framework.Bundle;

public class SchemaTransformer {

	private static final String PLATFORM_PLUGIN = "org.eclipse.platform"; //$NON-NLS-1$
	private static final String PLATFORM_PLUGIN_DOC = "org.eclipse.platform.doc.isv"; //$NON-NLS-1$
	private static final String SCHEMA_CSS = "schema.css"; //$NON-NLS-1$
	private static final String PLATFORM_CSS = "book.css"; //$NON-NLS-1$

	public static final byte TEMP = 0x00;
	public static final byte BUILD = 0x01;

	private byte fCssPurpose;
	private PrintWriter fWriter;
	private ISchema fSchema;
	private URL fCssURL;

	public void transform(ISchema schema, PrintWriter out) {
		transform(schema, out, null, TEMP);
	}

	public void transform(ISchema schema, PrintWriter out, URL cssURL, byte cssPurpose) {
		fSchema = schema;
		fWriter = out;
		fCssPurpose = cssPurpose;
		setCssURL(cssURL);
		printHTMLContent();
	}

	private void setCssURL(URL cssURL) {
		try {
			if (cssURL != null)
				fCssURL = FileLocator.resolve(cssURL);
		} catch (IOException e) {
		}
		if (fCssURL == null && fCssPurpose != BUILD)
			fCssURL = getResourceURL(getProductPlugin(), PLATFORM_CSS);
		if (fCssURL == null && fCssPurpose != BUILD)
			fCssURL = getResourceURL(PDECore.PLUGIN_ID, PLATFORM_CSS);
	}

	private String getCssURL() {
		return (fCssURL != null) ? fCssURL.toString() : "../../" + PLATFORM_CSS; //$NON-NLS-1$
	}

	private String getSchemaCssURL() {
		if (fCssPurpose == BUILD)
			return "../../" + SCHEMA_CSS; //$NON-NLS-1$
		URL url = getResourceURL(PLATFORM_PLUGIN_DOC, SCHEMA_CSS);
		if (url == null) {
			// this CSS file is last resort and is always there.
			url = getResourceURL(PDECore.PLUGIN_ID, SCHEMA_CSS);
		}
		return url.toString();
	}

	private void printHTMLContent() {
		fWriter.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">"); //$NON-NLS-1$
		fWriter.println("<HTML>"); //$NON-NLS-1$
		printHeader();
		printBody();
		fWriter.println("</HTML>"); //$NON-NLS-1$
	}

	private void printHeader() {
		fWriter.print("<HEAD>"); //$NON-NLS-1$
		fWriter.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"); //$NON-NLS-1$
		fWriter.println("<title>" + fSchema.getName() + "</title>"); //$NON-NLS-1$ //$NON-NLS-2$
		printStyles();
		fWriter.println("</HEAD>"); //$NON-NLS-1$
	}

	private void printStyles() {
		fWriter.println("<style type=\"text/css\">@import url(\"" + getCssURL() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
		fWriter.println("<style type=\"text/css\">@import url(\"" + getSchemaCssURL() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private URL getResourceURL(String bundleID, String resourcePath) {
		try {
			Bundle bundle = Platform.getBundle(bundleID);
			if (bundle != null) {
				URL entry = bundle.getEntry(resourcePath);
				if (entry != null)
					return FileLocator.toFileURL(entry);
			}
		} catch (IOException e) {
		}
		return null;
	}

	private void printBody() {
		fWriter.println("<BODY>"); //$NON-NLS-1$
		fWriter.println("<H1 style=\"text-align:center\">" + fSchema.getName() + "</H1>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (fSchema.isDeperecated()) {
			fWriter.print("<div style=\"border: 1px solid #990000; padding: 5px; text-align: center; color: red;\">"); //$NON-NLS-1$
			fWriter.print("This extension point is deprecated"); //$NON-NLS-1$
			String suggestion = fSchema.getDeprecatedSuggestion();
			if (suggestion != null)
				fWriter.print(", use <i>" + suggestion + "</i> as a replacement."); //$NON-NLS-1$ //$NON-NLS-2$
			fWriter.println("</div>"); //$NON-NLS-1$
		}
		if (fSchema.isInternal()) {
			fWriter.print("<div style=\"border: 1px solid #990000; padding: 5px; text-align: center; color: red;\">"); //$NON-NLS-1$
			fWriter.print("This extension point is internal"); //$NON-NLS-1$
			fWriter.println("</div>"); //$NON-NLS-1$
		}
		fWriter.println("<p></p>"); //$NON-NLS-1$
		fWriter.print("<h6 class=\"CaptionFigColumn SchemaHeader\">Identifier: </h6>"); //$NON-NLS-1$
		fWriter.print(fSchema.getQualifiedPointId());
		fWriter.println("<p></p>"); //$NON-NLS-1$
		transformSection("Since:", IDocumentSection.SINCE); //$NON-NLS-1$
		transformDescription();
		fWriter.println("<h6 class=\"CaptionFigColumn SchemaHeader\">Configuration Markup:</h6>"); //$NON-NLS-1$
		transformMarkup();
		transformSection("Examples:", IDocumentSection.EXAMPLES); //$NON-NLS-1$
		transformSection("API Information:", IDocumentSection.API_INFO); //$NON-NLS-1$
		transformSection("Supplied Implementation:", IDocumentSection.IMPLEMENTATION); //$NON-NLS-1$
		fWriter.println("<br>"); //$NON-NLS-1$
		fWriter.println("<p class=\"note SchemaCopyright\">"); //$NON-NLS-1$
		transformSection(null, IDocumentSection.COPYRIGHT);
		fWriter.println("</p>"); //$NON-NLS-1$
		fWriter.println("</BODY>"); //$NON-NLS-1$		
	}

	private void transformSection(String title, String sectionId) {
		IDocumentSection section = findSection(fSchema.getDocumentSections(), sectionId);
		if (section == null)
			return;
		String description = section.getDescription();
		if (description == null || description.trim().length() == 0)
			return;
		if (title != null)
			fWriter.print("<h6 class=\"CaptionFigColumn SchemaHeader\">" + title + " </h6>"); //$NON-NLS-1$ //$NON-NLS-2$
		transformText(description);
		fWriter.println();
		if (!sectionId.equals(IDocumentSection.COPYRIGHT))
			fWriter.println("<p></p>"); //$NON-NLS-1$
		fWriter.println();
	}

	private DocumentSection findSection(IDocumentSection[] sections, String sectionId) {
		for (int i = 0; i < sections.length; i++) {
			if (sections[i].getSectionId().equalsIgnoreCase(sectionId)) {
				return (DocumentSection) sections[i];
			}
		}
		return null;
	}

	private void transformText(String text) {
		if (text == null)
			return;
		boolean preformatted = false;
		boolean inTag = false;
		boolean inCstring = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '<') {
				if (isPreStart(text, i)) {
					fWriter.print("<pre class=\"Example\"><span class=\"code SchemaTag\">"); //$NON-NLS-1$
					i += 4;
					preformatted = true;
					continue;
				}
				if (isPreEnd(text, i)) {
					fWriter.print("</span></pre>"); //$NON-NLS-1$
					i += 5;
					preformatted = false;
					inTag = false;
					inCstring = false;
					continue;
				}
			}
			if (preformatted) {
				switch (c) {
					case '<' :
						inTag = true;
						fWriter.print("&lt;"); //$NON-NLS-1$
						break;
					case '>' :
						fWriter.print("&gt;"); //$NON-NLS-1$
						inTag = false;
						inCstring = false;
						break;
					case '&' :
						fWriter.print("&amp;"); //$NON-NLS-1$
						break;
					case '\'' :
						fWriter.print("&apos;"); //$NON-NLS-1$
						break;
					case '\"' :
						if (inTag) {
							if (inCstring) {
								fWriter.print("&quot;"); //$NON-NLS-1$
								fWriter.print("</span><span class=\"code SchemaTag\">"); //$NON-NLS-1$
								inCstring = false;
							} else {
								inCstring = true;
								fWriter.print("</span><span class=\"code SchemaCstring\">"); //$NON-NLS-1$
								fWriter.print("&quot;"); //$NON-NLS-1$
							}
						} else {
							fWriter.print("\""); //$NON-NLS-1$						
						}
						break;
					default :
						fWriter.print(c);
				}
			} else
				fWriter.print(c);
		}
	}

	private void transformDescription() {
		fWriter.print("<h6 class=\"CaptionFigColumn SchemaHeader\">Description: </h6>"); //$NON-NLS-1$
		transformText(fSchema.getDescription());
		ISchemaInclude[] includes = fSchema.getIncludes();
		for (int i = 0; i < includes.length; i++) {
			ISchema ischema = includes[i].getIncludedSchema();
			if (ischema != null) {
				fWriter.println("<p>"); //$NON-NLS-1$
				transformText(ischema.getDescription());
				fWriter.println("</p>"); //$NON-NLS-1$
			}
		}
		fWriter.println("<p></p>"); //$NON-NLS-1$
	}

	private void transformMarkup() {
		fWriter.println("<p></p>"); //$NON-NLS-1$
		ISchemaElement[] elements = fSchema.getResolvedElements();
		for (int i = 0; i < elements.length; i++) {
			transformElement(elements[i]);
		}
	}

	private void transformElement(ISchemaElement element) {
		String name = element.getName();
		String dtd = element.getDTDRepresentation(true);
		String nameLink = "<a name=\"e." + name + "\">" + name + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		if (element.isDeprecated() && !(element instanceof SchemaRootElement))
			fWriter.print("<div style=\"color: red; font-style: italic;\">The <b>" + name + "</b> element is deprecated</div> "); //$NON-NLS-1$ //$NON-NLS-2$

		fWriter.print("<p class=\"code SchemaDtd\">&lt;!ELEMENT " //$NON-NLS-1$
				+ nameLink + " " //$NON-NLS-1$
				+ dtd);
		fWriter.println("&gt;</p>"); //$NON-NLS-1$

		ISchemaAttribute[] attributes = element.getAttributes();

		if (attributes.length > 0) {
			fWriter.println("<p class=\"code SchemaDtd\">&lt;!ATTLIST " //$NON-NLS-1$
					+ name + "</p>"); //$NON-NLS-1$
			int maxWidth = calculateMaxAttributeWidth(element.getAttributes());
			for (int i = 0; i < attributes.length; i++) {
				appendAttlist(attributes[i], maxWidth);
			}
			fWriter.println("&gt;</p>"); //$NON-NLS-1$

		}
		fWriter.println("<p></p>"); //$NON-NLS-1$

		// inserted desc here for element
		String description = element.getDescription();

		if (description != null && description.trim().length() > 0) {
			String elementType = containsParagraph(description) ? "div" : "p"; //$NON-NLS-1$ //$NON-NLS-2$
			fWriter.println("<" + elementType + " class=\"ConfigMarkupElementDesc\">"); //$NON-NLS-1$ //$NON-NLS-2$
			transformText(description);
			fWriter.println("</" + elementType + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// end of inserted desc for element
		if (attributes.length == 0) {
			fWriter.println("<br><br>"); //$NON-NLS-1$
			return;
		} else if (description != null && description.trim().length() > 0) {
			fWriter.println("<br>"); //$NON-NLS-1$
		}

		fWriter.println("<ul class=\"ConfigMarkupAttlistDesc\">"); //$NON-NLS-1$
		for (int i = 0; i < attributes.length; i++) {
			ISchemaAttribute att = attributes[i];
			if (name.equals("extension")) { //$NON-NLS-1$
				if (att.getDescription() == null || att.getDescription().trim().length() == 0) {
					continue;
				}
			}
			fWriter.print("<li>"); //$NON-NLS-1$
			if (att.isDeprecated())
				fWriter.print("<i style=\"color: red;\">Deprecated</i> "); //$NON-NLS-1$
			fWriter.print("<b>" + att.getName() + "</b> - "); //$NON-NLS-1$ //$NON-NLS-2$
			transformText(att.getDescription());
			fWriter.println("</li>"); //$NON-NLS-1$
		}
		fWriter.println("</ul>"); //$NON-NLS-1$
		// adding spaces for new shifted view
		fWriter.print("<br>"); //$NON-NLS-1$
	}

	private boolean containsParagraph(String input) {
		if (input.indexOf("<p>") != -1) //$NON-NLS-1$
			return true;
		if (input.indexOf("</p>") != -1) //$NON-NLS-1$
			return true;
		return false;
	}

	private void appendAttlist(ISchemaAttribute att, int maxWidth) {
		fWriter.print("<p class=\"code SchemaDtdAttlist\">"); //$NON-NLS-1$
		// add name
		fWriter.print(att.getName());
		// fill spaces to align data type
		int delta = maxWidth - att.getName().length();
		for (int i = 0; i < delta + 1; i++) {
			fWriter.print("&nbsp;"); //$NON-NLS-1$
		}
		// add data type
		ISchemaSimpleType type = att.getType();
		ISchemaRestriction restriction = null;
		boolean choices = false;
		if (type != null)
			restriction = type.getRestriction();
		String typeName = type != null ? type.getName().toLowerCase(Locale.ENGLISH) : "string"; //$NON-NLS-1$
		if (typeName.equals("boolean")) { //$NON-NLS-1$
			fWriter.print("(true | false) "); //$NON-NLS-1$
			choices = true;
		} else if (att.getKind() == IMetaAttribute.IDENTIFIER) {
			fWriter.print("IDREF "); //$NON-NLS-1$
		} else if (restriction != null) {
			appendRestriction(restriction);
			choices = true;
		} else {
			fWriter.print("CDATA "); //$NON-NLS-1$
		}

		// add use
		if (att.getUse() == ISchemaAttribute.REQUIRED) {
			if (!choices)
				fWriter.print("#REQUIRED"); //$NON-NLS-1$
		} else if (att.getUse() == ISchemaAttribute.DEFAULT) {
			fWriter.print("\"" + att.getValue() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (!choices)
			fWriter.print("#IMPLIED"); //$NON-NLS-1$
		fWriter.print("</p>"); //$NON-NLS-1$
	}

	private void appendRestriction(ISchemaRestriction restriction) {
		if (restriction instanceof ChoiceRestriction) {
			String[] choices = ((ChoiceRestriction) restriction).getChoicesAsStrings();
			fWriter.print("("); //$NON-NLS-1$
			for (int i = 0; i < choices.length; i++) {
				if (i > 0)
					fWriter.print("|"); //$NON-NLS-1$
				fWriter.print(choices[i]);
			}
			fWriter.print(") "); //$NON-NLS-1$
		}
	}

	private boolean isPreEnd(String text, int loc) {
		if (loc + 5 >= text.length())
			return false;
		return (text.substring(loc, loc + 6).toLowerCase(Locale.ENGLISH).equals("</pre>")); //$NON-NLS-1$
	}

	private boolean isPreStart(String text, int loc) {
		if (loc + 4 >= text.length())
			return false;
		return (text.substring(loc, loc + 5).toLowerCase(Locale.ENGLISH).equals("<pre>")); //$NON-NLS-1$
	}

	private int calculateMaxAttributeWidth(ISchemaAttribute[] attributes) {
		int width = 0;
		for (int i = 0; i < attributes.length; i++) {
			width = Math.max(width, attributes[i].getName().length());
		}
		return width;
	}

	private String getProductPlugin() {
		IProduct product = Platform.getProduct();
		if (product != null) {
			Bundle plugin = product.getDefiningBundle();
			if (plugin != null) {
				return plugin.getSymbolicName();
			}
		}
		return PLATFORM_PLUGIN;
	}
}
