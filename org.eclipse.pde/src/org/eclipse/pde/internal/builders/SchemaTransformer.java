/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.osgi.framework.*;
import org.w3c.dom.*;

public class SchemaTransformer implements ISchemaTransformer {
	private static final String KEY_BOOLEAN_INVALID =
		"Builders.Schema.Verifier.booleanInvalid"; //$NON-NLS-1$
	private static final String KEY_RESTRICTION_INVALID =
		"Builders.Schema.Verifier.restrictionInvalid"; //$NON-NLS-1$
	private static final String KEY_BASED_ON_INVALID =
		"Builders.Schema.Verifier.basedOnInvalid"; //$NON-NLS-1$
	private static final String KEY_VALUE_WITHOUT_DEFAULT =
		"Builders.Schema.Verifier.valueWithoutDefault"; //$NON-NLS-1$
	private static final String KEY_DEFAULT_WITHOUT_VALUE =
		"Builders.Schema.Verifier.defaultWithoutValue"; //$NON-NLS-1$
	public static final String KEY_DEPRECATED_TYPE =
		"Builders.Schema.deprecatedType"; //$NON-NLS-1$
	public static final String REPORT_UNMATCHED =
		"SchemaTransformer.Validator.unmatched"; //$NON-NLS-1$
	public static final String REPORT_FORBIDDEN =
		"SchemaTransformer.Validator.forbidden"; //$NON-NLS-1$
	public static final String REPORT_OPTIONAL =
		"SchemaTransformer.Validator.optional"; //$NON-NLS-1$
	public static final String REPORT_GENERAL =
		"SchemaTransformer.Validator.general"; //$NON-NLS-1$
	public static final String REPORT_OPEN =
		"SchemaTransformer.Validator.open_tag"; //$NON-NLS-1$
	public static final String PLATFORM_PLUGIN_DOC =
		"org.eclipse.platform.doc.isv"; //$NON-NLS-1$
	public static final byte TEMP = 0x00;
	public static final byte BUILD = 0x01;
	public static final byte GENERATE_DOC = 0x02;
	public static final String[] forbiddenEndTagKeys =
		{
			"area", //$NON-NLS-1$
			"base", //$NON-NLS-1$
			"basefont", //$NON-NLS-1$
			"br", //$NON-NLS-1$
			"col", //$NON-NLS-1$
			"frame", //$NON-NLS-1$
			"hr", //$NON-NLS-1$
			"img", //$NON-NLS-1$
			"input", //$NON-NLS-1$
			"isindex", //$NON-NLS-1$
			"link", //$NON-NLS-1$
			"meta", //$NON-NLS-1$
			"param" }; //$NON-NLS-1$
	private static final String[] optionalEndTagKeys =
		{
			"body", //$NON-NLS-1$
			"colgroup", //$NON-NLS-1$
			"dd", //$NON-NLS-1$
			"dt", //$NON-NLS-1$
			"head", //$NON-NLS-1$
			"html", //$NON-NLS-1$
			"li", //$NON-NLS-1$
			"option", //$NON-NLS-1$
			"p", //$NON-NLS-1$
			"tbody", //$NON-NLS-1$
			"td", //$NON-NLS-1$
			"tfoot", //$NON-NLS-1$
			"th", //$NON-NLS-1$
			"thead", //$NON-NLS-1$
			"tr" }; //$NON-NLS-1$

	private void appendAttlist(
		PrintWriter out,
		ISchemaAttribute att,
		int maxWidth) {
		// add three spaces
//		out.print("<p class=code id=dtd>&nbsp;&nbsp;");
		out.print("<p class=code id=dtdAttlist>"); //$NON-NLS-1$
		// add name
		out.print(att.getName());
		// fill spaces to align data type
		int delta = maxWidth - att.getName().length();
		for (int i = 0; i < delta + 1; i++) {
			out.print("&nbsp;"); //$NON-NLS-1$
		}
		// add data type
		ISchemaSimpleType type = att.getType();
		ISchemaRestriction restriction = null;
		boolean choices = false;
		if (type != null)
			restriction = type.getRestriction();
		String typeName =
			type != null ? type.getName().toLowerCase() : "string"; //$NON-NLS-1$
		if (typeName.equals("boolean")) { //$NON-NLS-1$
			out.print("(true | false) "); //$NON-NLS-1$
			choices = true;
		} else if (restriction != null) {
			appendRestriction(restriction, out);
			choices = true;
		} else {
			out.print("CDATA "); //$NON-NLS-1$
		}

		// add use
		if (att.getUse() == ISchemaAttribute.REQUIRED) {
			if (!choices)
				out.print("#REQUIRED"); //$NON-NLS-1$
		} else if (att.getUse() == ISchemaAttribute.DEFAULT) {
			out.print("\"" + att.getValue() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		} else if (!choices)
			out.print("#IMPLIED"); //$NON-NLS-1$
	}
	private void appendRestriction(
		ISchemaRestriction restriction,
		PrintWriter out) {
		if (restriction instanceof ChoiceRestriction) {
			ChoiceRestriction cr = (ChoiceRestriction) restriction;
			String[] choices = cr.getChoicesAsStrings();
			out.print("("); //$NON-NLS-1$
			for (int i = 0; i < choices.length; i++) {
				if (i > 0)
					out.print("|"); //$NON-NLS-1$
				out.print(choices[i]);
			}
			out.print(") "); //$NON-NLS-1$
		}
	}
	private int calculateMaxAttributeWidth(ISchemaAttribute[] attributes) {
		int width = 0;
		for (int i = 0; i < attributes.length; i++) {
			ISchemaAttribute att = attributes[i];
			width = Math.max(width, att.getName().length());
		}
		return width;
	}
	
	private boolean isPreEnd(String text, int loc) {
		if (loc + 5 >= text.length())
			return false;
		String tag = text.substring(loc, loc + 6);
		if (tag.toLowerCase().equals("</pre>")) //$NON-NLS-1$
			return true;
		return false;
	}
	private boolean isPreStart(String text, int loc) {
		if (loc + 4 >= text.length())
			return false;
		String tag = text.substring(loc, loc + 5);
		if (tag.toLowerCase().equals("<pre>")) //$NON-NLS-1$
			return true;
		return false;
	}

	public void transform(
		ISchemaDescriptor desc,
		InputStream is,
		PrintWriter out,
		SchemaHandler reporter) {
		transform(desc, is, out, reporter, null);
	}

	public void transform(
		ISchemaDescriptor desc,
		InputStream is,
		PrintWriter out,
		SchemaHandler reporter,
		URL cssURL) {
		
		ValidatingSAXParser.parse(is, reporter);
		
		Node root = reporter.getDocumentElement();
		if (root == null || reporter.getErrorCount() > 0)
			return;
		Schema schema = new Schema(desc, desc.getSchemaURL());
		schema.traverseDocumentTree(root, reporter.getLineTable());

		if (verifySchema(schema, reporter)
			&& verifySections(schema, reporter)
			&& CompilerFlags.getBoolean(CompilerFlags.S_CREATE_DOCS)) {
			transform(out, schema, cssURL, GENERATE_DOC);

		}
	}

	private boolean verifySchema(Schema schema, XMLErrorReporter reporter) {
		if (schema.isLoaded() == false)
			return false;
		if (schema.isValid() == false)
			return false;
		checkFileType(reporter.getFile(), reporter);
		ISchemaElement[] elements = schema.getElements();
		int errors = 0;
		for (int i = 0; i < elements.length; i++) {
			ISchemaElement element = elements[i];
			ISchemaAttribute[] attributes = element.getAttributes();
			for (int j = 0; j < attributes.length; j++) {
				ISchemaAttribute attribute = attributes[j];
				errors += verifyAttribute(element, attribute, reporter);
			}
		}
		return (errors == 0);
	}

	private void checkFileType(IFile file, XMLErrorReporter reporter) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		if (dot != -1) {
			String ext = name.substring(dot + 1);
			if (ext.equalsIgnoreCase("xsd")) { //$NON-NLS-1$
				String message = PDE.getResourceString(KEY_DEPRECATED_TYPE);
				reporter.report(message, 1, IMarker.SEVERITY_WARNING);
			}
		}
	}

	private int verifyAttribute(
		ISchemaElement element,
		ISchemaAttribute attribute,
		XMLErrorReporter reporter) {
		int errors = 0;
		ISchemaType type = attribute.getType();
		String message;
		String[] args = new String[] { element.getName(), attribute.getName()};
		int line = -1;
		if (attribute instanceof ISourceObject) {
			line = ((ISourceObject) attribute).getStartLine();
		}

		if (attribute.getKind() != ISchemaAttribute.STRING) {
			if (type != null) {
				if (type.getName().equals("boolean")) { //$NON-NLS-1$
					message =
						PDE.getFormattedMessage(KEY_BOOLEAN_INVALID, args);
					// this kind cannot have boolean type
					reporter.report(message, line, IMarker.SEVERITY_ERROR);
					errors++;
				}
				if (type instanceof SchemaSimpleType
					&& ((SchemaSimpleType) type).getRestriction() != null) {
					// should not have restriction
					message =
						PDE.getFormattedMessage(KEY_RESTRICTION_INVALID, args);
					reporter.report(message, line, IMarker.SEVERITY_ERROR);
					errors++;
				}
			}
		}
		if (attribute.getKind() != ISchemaAttribute.JAVA) {
			if (attribute.getBasedOn() != null) {
				// basedOn makes no sense
				message = PDE.getFormattedMessage(KEY_BASED_ON_INVALID, args);
				reporter.report(message, line, IMarker.SEVERITY_ERROR);
				errors++;
			}
		}
		if (type != null && type.getName().equals("boolean")) { //$NON-NLS-1$
			if (type instanceof SchemaSimpleType
				&& ((SchemaSimpleType) type).getRestriction() != null) {
				// should not have restriction
				message =
					PDE.getFormattedMessage(KEY_RESTRICTION_INVALID, args);
				reporter.report(message, line, IMarker.SEVERITY_ERROR);
				errors++;
			}
		}
		if (attribute.getUse() != ISchemaAttribute.DEFAULT) {
			if (attribute.getValue() != null) {
				// value makes no sense without 'default' use
				message =
					PDE.getFormattedMessage(KEY_VALUE_WITHOUT_DEFAULT, args);
				reporter.report(message, line, IMarker.SEVERITY_ERROR);
				errors++;
			}
		} else {
			if (attribute.getValue() == null) {
				// there must be a value set for this use
				message =
					PDE.getFormattedMessage(KEY_DEFAULT_WITHOUT_VALUE, args);
				reporter.report(message, line, IMarker.SEVERITY_ERROR);
				errors++;
			}
		}
		return errors;
	}

	public static String getSchemaCSSName() {
		return "schema.css"; //$NON-NLS-1$
	}
	public static String getPlatformCSSName() {
		return "book.css"; //$NON-NLS-1$
	}

	public void addCSS(PrintWriter out, URL cssURL, byte cssPurpose) {
		File cssFile;

		if (cssURL == null) {
			if (cssPurpose == GENERATE_DOC) {
				out.println("<!-- default platform documentation stylesheets -->"); //$NON-NLS-1$
				//out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+ getPlatformCSSName()+ "\"/>");
				out.println("<style>@import url(\"" + getPlatformCSSName() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			} else if (cssPurpose == BUILD) {
				out.println(
					"<!-- default platform documentation stylesheets -->"); //$NON-NLS-1$
				//out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"../../"+ getPlatformCSSName()+ "\"/>");	
				out.println("<style>@import url(\"../../" + getPlatformCSSName() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			} else { // cssPurpose is TEMP
				Bundle bundle = Platform.getBundle(PLATFORM_PLUGIN_DOC);
				if (bundle == null)
					return;
				
				URL url = bundle.getEntry(getPlatformCSSName());
				if (url == null)
					return;
				try {
					cssFile = new File(Platform.resolve(url).getFile());
				} catch (IOException e1) {
					return;
				} 
			}
		} else {
			try {
				cssURL = Platform.resolve(cssURL);
			} catch (IOException e1) {
				return;
			}
			cssFile = new File(cssURL.getFile());
			if (cssPurpose == GENERATE_DOC) {
				out.println("<!-- custom platform documentation stylesheets -->"); //$NON-NLS-1$
				//out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + cssFile.getName()+ "\"/>");
				out.println("<style>@import url(\"" + cssFile.getName() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			} else if (cssPurpose == BUILD) {
				out.println("<!-- custom platform documentation stylesheets -->"); //$NON-NLS-1$
				//out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\""+ cssURL.toString()+ "\"/>");
				out.println("<style>@import url(\"" + cssURL.toString() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
		}
		try {
			File tempCSSFile = File.createTempFile("book", ".css"); //$NON-NLS-1$ //$NON-NLS-2$
			tempCSSFile.deleteOnExit();
			FileReader freader = new FileReader(cssFile);
			BufferedReader breader = new BufferedReader(freader);
			PrintWriter pwriter =
				new PrintWriter(new FileOutputStream(tempCSSFile));
			while (breader.ready()) {
				pwriter.println(breader.readLine());
			}
			out.println("<!-- temporary documentation stylesheets -->"); //$NON-NLS-1$
//			out.println(
//				"<link rel=\"stylesheet\" type=\"text/css\" href=\""
//					+ tempCSSFile.getName()
//					+ "\"/>");
			out.println("<style>@import url(\"" + tempCSSFile.getName() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
			pwriter.close();
			breader.close();
			freader.close();
		} catch (Exception e) {
			// do nothing if problem with css as it will only affect
			// formatting.
			// may want to log this error in the future.
		}
	}

	public void transform(PrintWriter out, ISchema schema) {
		transform(out, schema, null, TEMP);
	}

	public void transform(
		PrintWriter out,
		ISchema schema,
		URL cssURL,
		byte cssPurpose) {
		out.println(
			"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">"); //$NON-NLS-1$
		out.println("<HTML>"); //$NON-NLS-1$
		out.print("<HEAD>"); //$NON-NLS-1$
		out.println(
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">"); //$NON-NLS-1$
		out.println("<title>" + schema.getName() + "</title>"); //$NON-NLS-1$ //$NON-NLS-2$
		addCSS(out, cssURL, cssPurpose);
		addSchemaStyle(out, cssPurpose);

		out.println("</HEAD>"); //$NON-NLS-1$
		out.println("<BODY>"); //$NON-NLS-1$
		out.println("<H1><CENTER>" + schema.getName() + "</CENTER></H1>"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("<p></p>"); //$NON-NLS-1$
		out.print("<h6 class=CaptionFigColumn id=header>Identifier: </h6>"); //$NON-NLS-1$
		out.print(schema.getQualifiedPointId());
		out.println("<p></p>"); //$NON-NLS-1$
		transformSection(out, schema, "Since:", IDocumentSection.SINCE); //$NON-NLS-1$
		transformDescription(out, schema);
		out.println(
			"<p><h6 class=CaptionFigColumn id=header>Configuration Markup:</h6></p>"); //$NON-NLS-1$
		transformMarkup(out, schema);
		transformSection(out, schema, "Examples:", IDocumentSection.EXAMPLES); //$NON-NLS-1$
		transformSection(
			out,
			schema,
			"API Information:", //$NON-NLS-1$
			IDocumentSection.API_INFO);
		transformSection(
			out,
			schema,
			"Supplied Implementation:", //$NON-NLS-1$
			IDocumentSection.IMPLEMENTATION);
		out.println("<br>"); //$NON-NLS-1$
		out.println("<p class=note id=copyright>"); //$NON-NLS-1$
		transformSection(out, schema, IDocumentSection.COPYRIGHT);
		out.println("</p>"); //$NON-NLS-1$
		out.println("</BODY>"); //$NON-NLS-1$
		out.println("</HTML>"); //$NON-NLS-1$
	}

	private void addSchemaStyle(PrintWriter out, byte cssPurpose) {
		switch (cssPurpose) {
			case (TEMP) :
				Bundle bundle = Platform.getBundle(PLATFORM_PLUGIN_DOC);
				if (bundle == null)
					return;
				addCSS(out,bundle.getEntry(getSchemaCSSName()),cssPurpose); //$NON-NLS-1$
				break;
			case (GENERATE_DOC) :
				out.println(
					"<!-- default schema documentation stylesheets -->"); //$NON-NLS-1$
				out.println("<style>@import url(\"" + getSchemaCSSName() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			case (BUILD) :
				out.println(
					"<!-- default schema documentation stylesheets -->"); //$NON-NLS-1$
				// defect 43227
			out.println("<style>@import url(\"../../" + getSchemaCSSName() + "\");</style>"); //$NON-NLS-1$ //$NON-NLS-2$
				break;
			default :
				break;
		}		
	}

	private void transformDescription(PrintWriter out, ISchema schema) {
		out.println("<p>"); //$NON-NLS-1$
		out.print("<h6 class=CaptionFigColumn id=header>Description: </h6>"); //$NON-NLS-1$
		transformText(out, schema.getDescription());
		ISchemaInclude[] includes = schema.getIncludes();
		for (int i = 0; i < includes.length; i++) {

			ISchema ischema = includes[i].getIncludedSchema();
			if (ischema != null) {
				out.println("<p>"); //$NON-NLS-1$
				transformText(out, ischema.getDescription());
			}
		}
		out.println("</p>"); //$NON-NLS-1$
	}

	private void transformElement(PrintWriter out, ISchemaElement element) {
		String name = element.getName();
		String dtd = element.getDTDRepresentation(true);
		String nameLink = "<a name=\"e." + name + "\">" + name + "</a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		out.print(
			"<p class=code id=dtd>&lt;!ELEMENT " //$NON-NLS-1$
				+ nameLink
				+ " " //$NON-NLS-1$
				+ dtd);
		out.println("&gt;</p>"); //$NON-NLS-1$

		ISchemaAttribute[] attributes = element.getAttributes();

		if (attributes.length > 0) { 
			out.println(
				"<p class=code id=dtd>&lt;!ATTLIST " //$NON-NLS-1$
					+ name
					+ "</p>"); //$NON-NLS-1$
			int maxWidth = calculateMaxAttributeWidth(element.getAttributes());
			for (int i = 0; i < attributes.length; i++) {
				appendAttlist(out, attributes[i], maxWidth);
			}
			out.println("&gt;</p>"); //$NON-NLS-1$
			
		}
		out.println("<p></p>"); //$NON-NLS-1$
		
		// inserted desc here for element
		String description = element.getDescription();

		if (description != null && description.trim().length() > 0) {
			out.println("<p class=ConfigMarkup id=elementDesc>");  //$NON-NLS-1$
			transformText(out, description);
			out.println("</p>"); //$NON-NLS-1$
		} 
		// end of inserted desc for element
		if (attributes.length == 0){
			out.println("<br><br>"); //$NON-NLS-1$
			return;
		} else if (description != null && description.trim().length() > 0){
			out.println("<br>"); //$NON-NLS-1$
		}
		
		out.println("<ul class=ConfigMarkup id=attlistDesc>"); //$NON-NLS-1$
		for (int i = 0; i < attributes.length; i++) {
			ISchemaAttribute att = attributes[i];
			if (name.equals("extension")) { //$NON-NLS-1$
				if (att.getDescription() == null
					|| att.getDescription().trim().length() == 0) {
					continue;
				}
			}
			out.print("<li><b>" + att.getName() + "</b> - "); //$NON-NLS-1$ //$NON-NLS-2$
			transformText(out, att.getDescription());
			out.println("</li>");			 //$NON-NLS-1$
		}
		out.println("</ul>"); //$NON-NLS-1$
		// adding spaces for new shifted view
		out.print("<br>"); //$NON-NLS-1$
	}
	private void transformMarkup(PrintWriter out, ISchema schema) {
		ISchemaElement[] elements = schema.getResolvedElements();

		for (int i = 0; i < elements.length; i++) {
			ISchemaElement element = elements[i];
			transformElement(out, element);
		}
//		if (elements.length > 0) {
//			ISchemaElement lastElement = elements[elements.length - 1];
//			if (lastElement.getAttributeCount() == 0
//				&& lastElement.getDescription() == null) {
//				out.print("<br><br>");
//			}
//		}
	}

	private void transformSection(
		PrintWriter out,
		ISchema schema,
		String sectionId) {
		transformSection(out, schema, null, sectionId);
	}

	private DocumentSection findSection(
		IDocumentSection[] sections,
		String sectionId) {
		for (int i = 0; i < sections.length; i++) {
			if (sections[i].getSectionId().equals(sectionId)) {
				return (DocumentSection) sections[i];
			}
		}
		return null;
	}

	private boolean optionalEndTag(String tag) {
		for (int i = 0; i < optionalEndTagKeys.length; i++) {
			if (tag.equalsIgnoreCase(optionalEndTagKeys[i]))
				return true;
		}
		return false;
	}

	private boolean forbiddenEndTag(String tag) {
		for (int i = 0; i < forbiddenEndTagKeys.length; i++) {
			if (tag.equalsIgnoreCase(forbiddenEndTagKeys[i]))
				return true;
		}
		return false;
	}

	private boolean verifyDescription(
		String desc,
		PlatformObject container,
		XMLErrorReporter reporter) {
		boolean openTag = false, isPre = false;
		boolean flagForbidden =
			CompilerFlags.getFlag(CompilerFlags.S_FORBIDDEN_END_TAGS)
				!= CompilerFlags.IGNORE;
		boolean flagOptional =
			CompilerFlags.getFlag(CompilerFlags.S_OPTIONAL_END_TAGS)
				!= CompilerFlags.IGNORE;
		boolean flagGeneral =
			CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)
				!= CompilerFlags.IGNORE;
		Stack tagStack = new Stack();
		Stack lineStack = new Stack();
		int linenum = 1;

		if (desc == null || desc.trim().length() == 0)
			return false;

		StringTokenizer text = new StringTokenizer(desc, "<>", true); //$NON-NLS-1$

		while (text.countTokens() > 0) {
			String next = text.nextToken();
			if (next.equals("<")) { //$NON-NLS-1$
				openTag = true;

				if (text.hasMoreTokens()) {
					String tag = text.nextToken();
					String tempTag = tag;

					tag = tag.trim();

					// ignore eol char
					if (tag.indexOf('\n') != -1)
						tag = tag.replace('\n', ' ');

					int loc = tag.indexOf(" "); //$NON-NLS-1$
					int locEnd = tag.lastIndexOf("/"); //$NON-NLS-1$

					// trim all attributes if existing (i.e. color=blue)
					// ignore opened tag if it is empty, ends itself or is a
					// comment
					if (tag.equalsIgnoreCase(">") //$NON-NLS-1$
						|| (locEnd == tag.length() - 1
							&& text.hasMoreTokens()
							&& text.nextToken().equals(">")) //$NON-NLS-1$
						|| (tag.indexOf("!") == 0 //$NON-NLS-1$
							&& text.hasMoreTokens()
							&& text.nextToken().equals(">"))) { //$NON-NLS-1$
						openTag = false;
						linenum += getLineBreakCount(tempTag);
						continue;
					}

					tag = (loc == -1 ? tag : tag.substring(0, loc));

					if (locEnd != 0) { // assert it is not an end tag
						if (tag.equalsIgnoreCase("pre") && !isPre) { //$NON-NLS-1$
							isPre = true;
							tagStack.push(tag);
							lineStack.push(new Integer(linenum));
						} else if (!isPre) {
							if (!forbiddenEndTag(tag)) {
								tagStack.push(tag);
								lineStack.push(new Integer(linenum));
							}
						}
					} else {
						tag = tag.substring(1);
						// take off "/" prefix and all existing attributes
						if (isPre) {
							if (tag.equalsIgnoreCase("pre")) { //$NON-NLS-1$
								isPre = false;
								tagStack.pop();
								lineStack.pop();
							} else {
								openTag = false;
								linenum	+= getLineBreakCount(tempTag);
								continue;
							}
						} else if (
							!tagStack.isEmpty()
								&& tagStack.peek().toString().equalsIgnoreCase(
									tag)) {
							tagStack.pop();
							lineStack.pop();
						} else if (forbiddenEndTag(tag)) {
							if (flagForbidden) {
								report(
									"FORBIDDEN", //$NON-NLS-1$
									"/" + tag, //$NON-NLS-1$
									linenum,
									container,
									reporter);
							}
						} else if (
							tagStack.isEmpty() || tagStack.search(tag) == -1) {
							if (flagGeneral) {
								report(
									"GENERAL", //$NON-NLS-1$
									"/" + tag, //$NON-NLS-1$
									linenum,
									container,
									reporter);
							}
						} else { // top of stack has general tag that has not
								 // been given an end tag
							int search = tagStack.search(tag);
							do {
								if (!flagGeneral) {
									while (search > 1
										&& !optionalEndTag(tagStack
											.peek()
											.toString())) {
										tagStack.pop();
										lineStack.pop();
										search--;
									}
								} else {
									while (search > 1
										&& !optionalEndTag(tagStack
											.peek()
											.toString())) {
										report(
											"GENERAL", //$NON-NLS-1$
											tagStack.pop().toString(),
											((Integer) lineStack.pop())
												.intValue(),
											container,
											reporter);
										search--;
									}
								}

								if (!flagOptional) {
									while (search > 1
										&& optionalEndTag(
											tagStack.peek().toString())) {
										tagStack.pop();
										lineStack.pop();
										search--;
									}
								} else {
									while (search > 1
										&& optionalEndTag(
											tagStack.peek().toString())) {
										report(
											"OPTIONAL", //$NON-NLS-1$
											tagStack.pop().toString(),
											((Integer) lineStack.pop())
												.intValue(),
											container,
											reporter);
										search--;
									}
								}

								if (search == 1) {
									tagStack.pop();
									lineStack.pop();
									search--;
								}
							}
							while (search > 0);
						}
					}

					if (text.hasMoreTokens() && text.nextToken().equals(">")) { //$NON-NLS-1$
						openTag = false;
					} else {
						if (flagGeneral) {
							if (locEnd == -1) {
								report(
									"OPEN_TAG", //$NON-NLS-1$
									"null", //$NON-NLS-1$
									linenum,
									container,
									reporter);
							} else {
								report(
									"OPEN_TAG", //$NON-NLS-1$
									"null", //$NON-NLS-1$
									linenum,
									container,
									reporter);
							}
							openTag = false;
						}
					}
					linenum += getLineBreakCount(tempTag);
				}
			} else {
				linenum +=getLineBreakCount(next);
			}

		}

		if (openTag) {
			report("OPEN_TAG", "null", linenum, container, reporter); //$NON-NLS-1$ //$NON-NLS-2$
		}

		while (!tagStack.isEmpty()) {

			if (optionalEndTag(tagStack.peek().toString())) {
				if (!flagOptional) {
					tagStack.pop();
					lineStack.pop();
				} else {
					report(
						"OPTIONAL", //$NON-NLS-1$
						tagStack.pop().toString(),
						((Integer) lineStack.pop()).intValue(),
						container,
						reporter);
				}
			} else {
				if (!flagGeneral) {
					tagStack.pop();
					lineStack.pop();
				} else {
					report(
						"GENERAL", //$NON-NLS-1$
						tagStack.pop().toString(),
						((Integer) lineStack.pop()).intValue(),
						container,
						reporter);
				}
			}

		}

		return false;
	}

	private boolean report(
		String errType,
		String errTag,
		int linenum,
		PlatformObject container,
		XMLErrorReporter reporter) {
		if (container instanceof SchemaObject) {
			if (errTag.equals("")) { //$NON-NLS-1$
				reporter.report(
					PDE.getResourceString(REPORT_UNMATCHED),
					((SchemaObject) container).getStartLine() + linenum,
					CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
				return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)
					== CompilerFlags.ERROR;
			}
			if (errType.equals("FORBIDDEN")) { //$NON-NLS-1$
				reporter.report(
					PDE.getFormattedMessage(REPORT_FORBIDDEN, errTag),
					((SchemaObject) container).getStartLine() + linenum,
					CompilerFlags.getFlag(
						CompilerFlags.S_FORBIDDEN_END_TAGS));
				return CompilerFlags.getFlag(
					CompilerFlags.S_FORBIDDEN_END_TAGS)
					== CompilerFlags.ERROR;
			} 
			if (errType.equals("OPTIONAL")) { //$NON-NLS-1$
				reporter.report(
					PDE.getFormattedMessage(REPORT_OPTIONAL, errTag),
					((SchemaObject) container).getStartLine() + linenum,
					CompilerFlags.getFlag(
						CompilerFlags.S_OPTIONAL_END_TAGS));
				return CompilerFlags.getFlag(
					CompilerFlags.S_OPTIONAL_END_TAGS)
					== CompilerFlags.ERROR;
			} 
			if (errType.equals("OPEN_TAG")) { //$NON-NLS-1$
				reporter.report(
					PDE.getResourceString(REPORT_OPEN),
					((SchemaObject) container).getStartLine() + linenum,
					CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
				return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)
					== CompilerFlags.ERROR;
			} 
			reporter.report(
				PDE.getFormattedMessage(REPORT_GENERAL, errTag),
				((SchemaObject) container).getStartLine() + linenum,
				CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
			return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)
				== CompilerFlags.ERROR;		
		} 
		
		if (errTag.equals("")) { //$NON-NLS-1$
			reporter.report(
				PDE.getResourceString(REPORT_UNMATCHED),
				((Schema) container).getOverviewStartLine() + linenum,
				CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
			return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)
				== CompilerFlags.ERROR;
		} 
		if (errType.equals("FORBIDDEN")) { //$NON-NLS-1$
			reporter.report(
				PDE.getFormattedMessage(REPORT_FORBIDDEN, errTag),
				((Schema) container).getOverviewStartLine() + linenum,
				CompilerFlags.getFlag(
					CompilerFlags.S_FORBIDDEN_END_TAGS));
			return CompilerFlags.getFlag(
				CompilerFlags.S_FORBIDDEN_END_TAGS)
				== CompilerFlags.ERROR;
		} 
		if (errType.equals("OPTIONAL")) { //$NON-NLS-1$
			reporter.report(
				PDE.getFormattedMessage(REPORT_OPTIONAL, errTag),
				((Schema) container).getOverviewStartLine() + linenum,
				CompilerFlags.getFlag(
					CompilerFlags.S_OPTIONAL_END_TAGS));
			return CompilerFlags.getFlag(
				CompilerFlags.S_OPTIONAL_END_TAGS)
				== CompilerFlags.ERROR;
		} 
		if (errType.equals("OPEN_TAG")) { //$NON-NLS-1$
			reporter.report(
				PDE.getResourceString(REPORT_OPEN),
				((Schema) container).getOverviewStartLine() + linenum,
				CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
			return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)
				== CompilerFlags.ERROR;
		} 
		reporter.report(
			PDE.getFormattedMessage(REPORT_GENERAL, errTag),
			((Schema) container).getOverviewStartLine() + linenum,
			CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
		return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)
			== CompilerFlags.ERROR;
	}

	private boolean verifySections(
		ISchema schema,
		XMLErrorReporter reporter) {
		if (CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)
			== CompilerFlags.IGNORE
			&& CompilerFlags.getFlag(CompilerFlags.S_FORBIDDEN_END_TAGS)
				== CompilerFlags.IGNORE
			&& CompilerFlags.getFlag(CompilerFlags.S_OPTIONAL_END_TAGS)
				== CompilerFlags.IGNORE)
			return true;
		boolean hasError = false;
		DocumentSection section = null;
		String sectionIds[] =
			{
				DocumentSection.API_INFO,
				DocumentSection.EXAMPLES,
				DocumentSection.IMPLEMENTATION,
				DocumentSection.P_DESCRIPTION,
				DocumentSection.COPYRIGHT,
				DocumentSection.SINCE };
		for (int i = 0; i < sectionIds.length; i++) {
			section = findSection(schema.getDocumentSections(), sectionIds[i]);
			if (section != null) {
				String desc = section.getDescription();
				hasError =
					(verifyDescription(desc, section, reporter))
						? true
						: hasError;
			}
		}
		hasError =
			(verifyDescription(schema.getDescription(),
				(Schema) schema,
				reporter))
				? true
				: hasError;

		return !hasError;
	}

	private void transformSection(
		PrintWriter out,
		ISchema schema,
		String title,
		String sectionId) {
		IDocumentSection section =
			findSection(schema.getDocumentSections(), sectionId);

		if (section == null)
			return;
		String description = section.getDescription();
		if (description == null || description.trim().length() == 0)
			return;
		if (title != null)
			out.print(
				"<h6 class=CaptionFigColumn id=header>" + title + " </h6>"); //$NON-NLS-1$ //$NON-NLS-2$
		transformText(out, description);
		out.println();
		out.println("<p></p>"); //$NON-NLS-1$
		out.println();
	}
	private void transformText(PrintWriter out, String text) {
		if (text == null)
			return;
		boolean preformatted = false;
		boolean inTag = false;
		boolean inCstring = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '<') {
				if (isPreStart(text, i)) {
					out.print("<pre>"); //$NON-NLS-1$
					i += 4;
					preformatted = true;
					continue;
				}
				if (isPreEnd(text, i)) {
					out.print("</pre>"); //$NON-NLS-1$
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
						out.print("<p class=code id=tag>"); //$NON-NLS-1$
						out.print("&lt;"); //$NON-NLS-1$
						break;
					case '>' :
						out.print("&gt;"); //$NON-NLS-1$
						out.print("</p>"); //$NON-NLS-1$
						inTag = false;
						inCstring = false;
						break;
					case '&' :
						out.print("&amp;"); //$NON-NLS-1$
						break;
					case '\'' :
						out.print("&apos;"); //$NON-NLS-1$
						break;
					case '\"' :
						if (inTag) {
							if (inCstring) {
								out.print("&quot;"); //$NON-NLS-1$
								out.print("</p>"); //$NON-NLS-1$
								out.print("<p class=code id=tag>"); //$NON-NLS-1$
								inCstring = false;
							} else {
								inCstring = true;
								out.print("<p class=code id=cstring>"); //$NON-NLS-1$
								out.print("&quot;"); //$NON-NLS-1$
							}
						}
						break;
					default :
						out.print(c);
				}
			} else
				out.print(c);
		}
	}
	
	public int getLineBreakCount(String tag){
		StringTokenizer tokenizer = new StringTokenizer(tag, "\n", true); //$NON-NLS-1$
		int token = 0;
		while (tokenizer.hasMoreTokens()){
			if (tokenizer.nextToken().equals("\n")) //$NON-NLS-1$
				token++;
		}
		return token;
	}

}
