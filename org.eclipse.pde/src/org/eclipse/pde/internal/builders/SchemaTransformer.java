/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.tools.ant.util.StringUtils;
import org.eclipse.core.internal.plugins.PluginDescriptor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SourceDOMParser;
import org.eclipse.pde.internal.core.ischema.IDocumentSection;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.eclipse.pde.internal.core.schema.ChoiceRestriction;
import org.eclipse.pde.internal.core.schema.DocumentSection;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaSimpleType;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SchemaTransformer implements ISchemaTransformer {
	private static final String KEY_BOOLEAN_INVALID =
		"Builders.Schema.Verifier.booleanInvalid";
	private static final String KEY_RESTRICTION_INVALID =
		"Builders.Schema.Verifier.restrictionInvalid";
	private static final String KEY_BASED_ON_INVALID =
		"Builders.Schema.Verifier.basedOnInvalid";
	private static final String KEY_VALUE_WITHOUT_DEFAULT =
		"Builders.Schema.Verifier.valueWithoutDefault";
	private static final String KEY_DEFAULT_WITHOUT_VALUE =
		"Builders.Schema.Verifier.defaultWithoutValue";
	public static final String KEY_DEPRECATED_TYPE =
		"Builders.Schema.deprecatedType";
	public static final String REPORT_UNMATCHED =
		"SchemaTransformer.Validator.unmatched";
	public static final String REPORT_FORBIDDEN = 
		"SchemaTransformer.Validator.forbidden";
	public static final String REPORT_OPTIONAL =
		"SchemaTransformer.Validator.optional";
	public static final String REPORT_GENERAL = 
		"SchemaTransformer.Validator.general";
	public static final String REPORT_OPEN = 
		"SchemaTransformer.Validator.open_tag";

	private static final String COLOR_TAG = "#000080";
	private static final String COLOR_CSTRING = "#008000";
	private static final String COLOR_DTD = "#800000";
	private static final String COLOR_COPYRIGHT = "#336699";
	private File tempCSSFile;
	public static final String[] forbiddenEndTagKeys =
		{
			"area",
			"base",
			"basefont",
			"br",
			"col",
			"frame",
			"hr",
			"img",
			"input",
			"isindex",
			"link",
			"meta",
			"param" };
	private static final String[] optionalEndTagKeys =
		{
			"body",
			"colgroup",
			"dd",
			"dt",
			"head",
			"html",
			"li",
			"option",
			"p",
			"tbody",
			"td",
			"tfoot",
			"th",
			"thead",
			"tr" };

	private void appendAttlist(
		PrintWriter out,
		ISchemaAttribute att,
		int maxWidth) {
		// add three spaces
		out.print("<br><samp class=dtd>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		// add name
		out.print(att.getName());
		// fill spaces to align data type
		int delta = maxWidth - att.getName().length();
		for (int i = 0; i < delta + 1; i++) {
			out.print("&nbsp;");
		}
		// add data type
		ISchemaSimpleType type = att.getType();
		ISchemaRestriction restriction = null;
		boolean choices = false;
		if (type != null)
			restriction = type.getRestriction();
		String typeName =
			type != null ? type.getName().toLowerCase() : "string";
		if (typeName.equals("boolean")) {
			out.print("(true | false) ");
			choices = true;
		} else if (restriction != null) {
			appendRestriction(restriction, out);
			choices = true;
		} else {
			out.print("CDATA ");
		}

		// add use
		if (att.getUse() == ISchemaAttribute.REQUIRED) {
			if (!choices)
				out.print("#REQUIRED");
		} else if (att.getUse() == ISchemaAttribute.DEFAULT) {
			out.print("\"" + att.getValue() + "\"");
		} else if (!choices)
			out.print("#IMPLIED");
		out.println("</samp>");
	}
	private void appendRestriction(
		ISchemaRestriction restriction,
		PrintWriter out) {
		if (restriction instanceof ChoiceRestriction) {
			ChoiceRestriction cr = (ChoiceRestriction) restriction;
			String[] choices = cr.getChoicesAsStrings();
			out.print("(");
			for (int i = 0; i < choices.length; i++) {
				if (i > 0)
					out.print("|");
				out.print(choices[i]);
			}
			out.print(") ");
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
	private SourceDOMParser createDOMTree(
		InputStream schema,
		PluginErrorReporter reporter) {
		SourceDOMParser parser = new SourceDOMParser();
		parser.setErrorHandler(reporter);
		try {
			InputSource source = new InputSource(schema);
			parser.parse(source);
			return parser;
		} catch (SAXException e) {
		} catch (IOException e) {
			PDE.logException(e);
		}
		return null;
	}
	private boolean isPreEnd(String text, int loc) {
		if (loc + 5 >= text.length())
			return false;
		String tag = text.substring(loc, loc + 6);
		if (tag.toLowerCase().equals("</pre>"))
			return true;
		return false;
	}
	private boolean isPreStart(String text, int loc) {
		if (loc + 4 >= text.length())
			return false;
		String tag = text.substring(loc, loc + 5);
		if (tag.toLowerCase().equals("<pre>"))
			return true;
		return false;
	}

	public void transform(
		URL schemaURL,
		InputStream is,
		PrintWriter out,
		PluginErrorReporter reporter) {
		transform(schemaURL, is, out, reporter, null);
	}

	public void transform(
		URL schemaURL,
		InputStream is,
		PrintWriter out,
		PluginErrorReporter reporter,
		URL cssURL) {
		SourceDOMParser parser = createDOMTree(is, reporter);

		if (parser == null)
			return;
		Node root = parser.getDocument().getDocumentElement();
		Schema schema = new Schema((ISchemaDescriptor) null, schemaURL);
		schema.traverseDocumentTree(root, parser.getLineTable());

		if (verifySchema(schema, reporter)
			&& verifySections(schema, reporter)
			&& CompilerFlags.getBoolean(CompilerFlags.S_CREATE_DOCS)
			&& CompilerFlags.getBoolean(CompilerFlags.S_OPEN_TAGS))
			transform(out, schema, cssURL);
	}

	private boolean verifySchema(Schema schema, PluginErrorReporter reporter) {
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

	private void checkFileType(IFile file, PluginErrorReporter reporter) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		if (dot != -1) {
			String ext = name.substring(dot + 1);
			if (ext.equalsIgnoreCase("xsd")) {
				String message = PDE.getResourceString(KEY_DEPRECATED_TYPE);
				reporter.reportWarning(message);
			}
		}
	}

	private int verifyAttribute(
		ISchemaElement element,
		ISchemaAttribute attribute,
		PluginErrorReporter reporter) {
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
				if (type.getName().equals("boolean")) {
					message =
						PDE.getFormattedMessage(KEY_BOOLEAN_INVALID, args);
					// this kind cannot have boolean type
					reporter.reportError(message, line);
					errors++;
				}
				if (type instanceof SchemaSimpleType
					&& ((SchemaSimpleType) type).getRestriction() != null) {
					// should not have restriction
					message =
						PDE.getFormattedMessage(KEY_RESTRICTION_INVALID, args);
					reporter.reportError(message, line);
					errors++;
				}
			}
		}
		if (attribute.getKind() != ISchemaAttribute.JAVA) {
			if (attribute.getBasedOn() != null) {
				// basedOn makes no sense
				message = PDE.getFormattedMessage(KEY_BASED_ON_INVALID, args);
				reporter.reportError(message, line);
				errors++;
			}
		}
		if (type != null && type.getName().equals("boolean")) {
			if (type instanceof SchemaSimpleType
				&& ((SchemaSimpleType) type).getRestriction() != null) {
				// should not have restriction
				message =
					PDE.getFormattedMessage(KEY_RESTRICTION_INVALID, args);
				reporter.reportError(message, line);
				errors++;
			}
		}
		if (attribute.getUse() != ISchemaAttribute.DEFAULT) {
			if (attribute.getValue() != null) {
				// value makes no sense without 'default' use
				message =
					PDE.getFormattedMessage(KEY_VALUE_WITHOUT_DEFAULT, args);
				reporter.reportError(message, line);
				errors++;
			}
		} else {
			if (attribute.getValue() == null) {
				// there must be a value set for this use
				message =
					PDE.getFormattedMessage(KEY_DEFAULT_WITHOUT_VALUE, args);
				reporter.reportError(message, line);
				errors++;
			}
		}
		return errors;
	}

	public void addPlatformCSS(PrintWriter out, URL cssURL) {
		File cssFile;

		if (cssURL == null) {
			PluginDescriptor descriptor =
				(PluginDescriptor) Platform
					.getPluginRegistry()
					.getPluginDescriptor(
					"org.eclipse.platform.doc.user");
			if (descriptor == null)
				return;
			cssFile =
				new File(
					descriptor.getInstallURLInternal().getFile() + "book.css");
		} else {
			cssFile = new File(cssURL.getFile());
		}
		try {
			tempCSSFile =
				PDECore.getDefault().getTempFileManager().createTempFile(
					this,
					"book",
					".css");
			FileReader freader = new FileReader(cssFile);
			BufferedReader breader = new BufferedReader(freader);
			PrintWriter pwriter =
				new PrintWriter(new FileOutputStream(tempCSSFile));
			while (breader.ready()) {
				pwriter.println(breader.readLine());
			}
			out.println(
				"<link rel=\"stylesheet\" type=\"text/css\" href=\""
					+ tempCSSFile.getName()
					+ "\"/>");
			pwriter.close();
			breader.close();
			freader.close();
		} catch (Exception e) {
			// do nothing if problem with css as it will only affect formatting.  
			// may want to log this error in the future.
		}
	}

	public void transform(PrintWriter out, ISchema schema) {
		transform(out, schema, null);
	}

	public void transform(PrintWriter out, ISchema schema, URL cssURL) {
		out.println(
			"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
		out.print("<HEAD>");
		out.println(
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
		addStyle(out);
		addPlatformCSS(out, cssURL);

		out.println("</HEAD>");
		out.println("<HTML>");
		out.println("<BODY>");
		out.println("<H1><CENTER>" + schema.getName() + "</CENTER></H1>");
		out.print("<div class=header>Identifier: </div>");
		out.print(schema.getQualifiedPointId());
		out.println("<p>");
		transformSection(out, schema, "Since:", IDocumentSection.SINCE);
		transformDescription(out, schema);
		out.println("<p><div class=header>Configuration Markup:</div></p>");
		transformMarkup(out, schema);
		transformSection(out, schema, "Examples:", IDocumentSection.EXAMPLES);
		transformSection(
			out,
			schema,
			"API Information:",
			IDocumentSection.API_INFO);
		transformSection(
			out,
			schema,
			"Supplied Implementation:",
			IDocumentSection.IMPLEMENTATION);
		out.println("<div class=copyright-text>");
		transformSection(out, schema, IDocumentSection.COPYRIGHT);
		out.println("</div>");
		out.println("</BODY>");
		out.println("</HTML>");
	}

	private void addStyle(PrintWriter out) {
		out.println("<STYLE type=\"text/css\">");
		out.println(
			".header {font-family: sans-serif; font-style: italic; font-weight: bold ; font-size:16px; display:inline}");
		out.println(
			".copyright-text {font-family: sans-serif; font-size: 10px; color: "
				+ COLOR_COPYRIGHT
				+ "; display:inline }");
		out.println(
			"samp.dtd {font-family: sans-serif; color: "
				+ COLOR_DTD
				+ "; display: inline}");
		out.println(
			".tag {font-family: sans-serif; color: "
				+ COLOR_TAG
				+ "; display:inline}");
		out.println(
			".cstring {font-family: sans-serif; color: "
				+ COLOR_CSTRING
				+ "; display:inline}");
		out.println("</STYLE>");
	}

	private void transformDescription(PrintWriter out, ISchema schema) {
		out.print("<div class=header>Description: </div>");
		transformText(out, schema.getDescription());
		ISchemaInclude[] includes = schema.getIncludes();
		for (int i = 0; i < includes.length; i++) {

			ISchema ischema = includes[i].getIncludedSchema();
			if (ischema != null) {
				out.println("<p>");
				transformText(out, ischema.getDescription());
			}
		}
	}

	private void transformElement(PrintWriter out, ISchemaElement element) {
		String name = element.getName();
		String dtd = element.getDTDRepresentation(true);
		String nameLink = "<a name=\"e." + name + "\">" + name + "</a>";
		//out.print("<div class=\"dtd-fragment\">");
		out.print(
			"<p><samp class=dtd>&nbsp;&nbsp; &lt;!ELEMENT "
				+ nameLink
				+ " "
				+ dtd);
		out.println("&gt;</samp>");

		ISchemaAttribute[] attributes = element.getAttributes();
		String description = element.getDescription();

		if (description != null && description.trim().length() > 0) {
			out.print("<p>");
			out.print("&nbsp;&nbsp; ");
			transformText(out, description);
			out.println("</p>");
			if (attributes.length > 0)
				out.println("<p></p>");
		} else if (attributes.length > 0) {
			out.print("<br><br>");
		}

		if (attributes.length > 0) {
			out.println(
				"<samp class=dtd>&nbsp;&nbsp; &lt;!ATTLIST "
					+ name
					+ "</samp>");
			int maxWidth = calculateMaxAttributeWidth(element.getAttributes());
			for (int i = 0; i < attributes.length; i++) {
				appendAttlist(out, attributes[i], maxWidth);
			}
			out.println("<br><samp class=dtd>&nbsp;&nbsp; &gt;</samp>");
		}
		//out.println("</div>");
		if (attributes.length == 0)
			return;

		out.println("<ul>");
		for (int i = 0; i < attributes.length; i++) {
			ISchemaAttribute att = attributes[i];
			if (name.equals("extension")) {
				if (att.getDescription() == null
					|| att.getDescription().trim().length() == 0) {
					continue;
				}
			}
			out.print("<li><b>" + att.getName() + "</b> - ");
			transformText(out, att.getDescription());
			out.println("</li>");
		}
		out.println("</ul>");
	}
	private void transformMarkup(PrintWriter out, ISchema schema) {
		ISchemaElement[] elements = schema.getResolvedElements();

		for (int i = 0; i < elements.length; i++) {
			ISchemaElement element = elements[i];
			transformElement(out, element);
		}
		if (elements.length > 0) {
			ISchemaElement lastElement = elements[elements.length - 1];
			if (lastElement.getAttributeCount() == 0
				&& lastElement.getDescription() == null) {
				out.print("<br><br>");
			}
		}
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
	private boolean verifyDescription(String desc, PlatformObject container, PluginErrorReporter reporter) {
		boolean openTag = false, isPre = false;
		boolean flagForbidden = CompilerFlags.getFlag(CompilerFlags.S_FORBIDDEN_END_TAGS)!= CompilerFlags.IGNORE;
		boolean flagOptional = CompilerFlags.getFlag(CompilerFlags.S_OPTIONAL_END_TAGS)!= CompilerFlags.IGNORE;
		boolean flagGeneral = CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)!= CompilerFlags.IGNORE;
		Stack tagStack = new Stack();
		Stack lineStack = new Stack();
		int linenum = 1;

		if (desc == null || desc.trim().length() == 0) 
			return false;
			
		StringTokenizer text = new StringTokenizer(desc, "<>", true);

		while (text.countTokens() > 0) {
			String next = text.nextToken();
			if (next.equals("<")) {
				openTag = true;

				if (text.hasMoreTokens()) {
					String tag = text.nextToken();
					String tempTag = tag;

					tag = tag.trim();

					// ignore eol char
					if (tag.indexOf('\n') != -1)
						tag = tag.replace('\n', ' ');

					int loc = tag.indexOf(" ");
					int locEnd = tag.lastIndexOf("/");
				
					// trim all attributes if existing (i.e. color=blue)
					// ignore opened tag if it is empty, ends itself or is a comment 
					if (tag.equalsIgnoreCase(">")
						|| (locEnd == tag.length() - 1 && text.hasMoreTokens() && text.nextToken().equals(">"))
						|| (tag.indexOf("!") == 0 && text.hasMoreTokens() && text.nextToken().equals(">"))) {
						openTag = false;
						if (StringUtils.lineSplit(tempTag).size() > 1)
							linenum += StringUtils.lineSplit(tempTag).size()-1;
						continue;
					}
					
					tag = (loc == -1 ? tag : tag.substring(0, loc));

					if (locEnd != 0) { // assert it is not an end tag
						if (tag.equalsIgnoreCase("pre") && !isPre) {
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
						tag = tag.substring(1); // take off "/" prefix and all existing attributes
						if (isPre) {
							if (tag.equalsIgnoreCase("pre")){
								isPre = false;
								tagStack.pop();
								lineStack.pop();
							} else {
								openTag=false;
								if (StringUtils.lineSplit(tempTag).size() > 1)
									linenum += StringUtils.lineSplit(tempTag).size()-1;
								continue;
							}
						} else if (!tagStack.isEmpty()
								&& tagStack.peek().toString().equalsIgnoreCase(tag)) {
							tagStack.pop();
							lineStack.pop();
						} else if (forbiddenEndTag(tag)) {
							if (flagForbidden) {
								report("FORBIDDEN", "/" + tag, linenum, container, reporter);
							}
						} else if (tagStack.isEmpty() || tagStack.search(tag)==-1) {
							if (flagGeneral){
								report("GENERAL", "/" + tag, linenum, container, reporter);
							}
						} else { // top of stack has general tag that has not been given an end tag
							int search = tagStack.search(tag);
							do{
								if (!flagGeneral){
									while (search >1 &&!optionalEndTag(tagStack.peek().toString())){
										tagStack.pop();
										lineStack.pop();
										search--;
									}
								} else {
									while (search >1 && !optionalEndTag(tagStack.peek().toString())){
										report("GENERAL", tagStack.pop().toString(), ((Integer) lineStack.pop()).intValue(), container, reporter);
										search--;
									}
								}	
								
								if (!flagOptional){
									while (search >1 && optionalEndTag(tagStack.peek().toString())){
										tagStack.pop();
										lineStack.pop();
										search--;
									}
								} else {
									while (search >1 && optionalEndTag(tagStack.peek().toString())){
										report("OPTIONAL", tagStack.pop().toString(), ((Integer) lineStack.pop()).intValue(), container, reporter);
										search--;
									} 
								}
								
								if (search==1){
									tagStack.pop();
									lineStack.pop();
									search--;
								}
							} while (search >0); 
						}
					}

					if (text.hasMoreTokens()
						&& text.nextToken().equals(">")) {
						openTag = false;
					} else {
						if (flagGeneral){
							if (locEnd ==-1) {
								report("OPEN_TAG", "null", linenum, container, reporter);
							} else {
								report("OPEN_TAG", "null", linenum, container, reporter);
							}  
							openTag = false;
						}
					}

					if (StringUtils.lineSplit(tempTag).size() > 1)
						linenum += StringUtils.lineSplit(tempTag).size()-1;
				}
				} else if (StringUtils.lineSplit(next).size() > 1){
					linenum += StringUtils.lineSplit(next).size()-1;
				}

		}


		
		if (openTag){
			report("OPEN_TAG", "null", linenum, container, reporter);
		} 

		while (!tagStack.isEmpty()) {
			
			if (optionalEndTag(tagStack.peek().toString())) {
				if (!flagOptional) {
					tagStack.pop();
					lineStack.pop();
				} else {
					report("OPTIONAL",  tagStack.pop().toString(), ((Integer) lineStack.pop()).intValue(), container, reporter);
				}
			} else {
				if (!flagGeneral){
					tagStack.pop();
					lineStack.pop();
				} else {
					report("GENERAL",  tagStack.pop().toString(), ((Integer) lineStack.pop()).intValue(), container, reporter);
				}
			}
			
		}

		return false;
	}

	private boolean report(String errType, String errTag, int linenum, PlatformObject container, PluginErrorReporter reporter){
		if (container instanceof SchemaObject) {
			if (errTag.equals("")) {
				reporter.report(PDE.getResourceString(REPORT_UNMATCHED),
					((SchemaObject) container).getStartLine() + linenum,
					CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
				return	CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)== CompilerFlags.ERROR;
			} else {
				if (errType.equals("FORBIDDEN")) {
					reporter.report(PDE.getFormattedMessage(REPORT_FORBIDDEN, errTag),
						((SchemaObject) container).getStartLine()+ linenum,
						CompilerFlags.getFlag(CompilerFlags.S_FORBIDDEN_END_TAGS));
					return CompilerFlags.getFlag(CompilerFlags.S_FORBIDDEN_END_TAGS)== CompilerFlags.ERROR;
				} else if (errType.equals("OPTIONAL")) {
					reporter.report(PDE.getFormattedMessage(REPORT_OPTIONAL, errTag),
						((SchemaObject) container).getStartLine()+ linenum,
						CompilerFlags.getFlag(CompilerFlags.S_OPTIONAL_END_TAGS));
					return CompilerFlags.getFlag(CompilerFlags.S_OPTIONAL_END_TAGS)== CompilerFlags.ERROR;
				} else if (errType.equals("OPEN_TAG")){
					reporter.report(PDE.getResourceString(REPORT_OPEN),
						((SchemaObject) container).getStartLine()+ linenum,
						CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
					return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)== CompilerFlags.ERROR;
				} else {
					reporter.report(PDE.getFormattedMessage(REPORT_GENERAL, errTag),
						((SchemaObject) container).getStartLine()+ linenum,
						CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
					return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)== CompilerFlags.ERROR;
				}
			}
		} else { //i.e. if (container instanceof Schema)
			if (errTag.equals("")) {
				reporter.report(PDE.getResourceString(REPORT_UNMATCHED),
					((Schema) container).getOverviewStartLine() + linenum,
					CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
				return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)== CompilerFlags.ERROR;
			} else {
				if (errType.equals("FORBIDDEN")) {
					reporter.report(PDE.getFormattedMessage(REPORT_FORBIDDEN, errTag),
						((Schema) container).getOverviewStartLine() + linenum,
						CompilerFlags.getFlag(CompilerFlags.S_FORBIDDEN_END_TAGS));
					return CompilerFlags.getFlag(CompilerFlags.S_FORBIDDEN_END_TAGS)== CompilerFlags.ERROR;
				} else if (errType.equals("OPTIONAL")) {
					reporter.report(PDE.getFormattedMessage(REPORT_OPTIONAL, errTag),
						((Schema) container).getOverviewStartLine() + linenum,
						CompilerFlags.getFlag(CompilerFlags.S_OPTIONAL_END_TAGS));
					return CompilerFlags.getFlag(CompilerFlags.S_OPTIONAL_END_TAGS)== CompilerFlags.ERROR;
				} else if (errType.equals("OPEN_TAG")){
					reporter.report(PDE.getResourceString(REPORT_OPEN),
						((Schema) container).getOverviewStartLine() + linenum,
						CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
					return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)== CompilerFlags.ERROR;
				} else {
					reporter.report(PDE.getFormattedMessage(REPORT_GENERAL, errTag),
						((Schema) container).getOverviewStartLine() + linenum,
						CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS));
					return CompilerFlags.getFlag(CompilerFlags.S_OPEN_TAGS)== CompilerFlags.ERROR;
				}
			}
		}
	}
	
	private boolean verifySections(
		ISchema schema,
		PluginErrorReporter reporter) {
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
				hasError = (verifyDescription(desc, section, reporter)) ? true : hasError;
			}
		}
		hasError = (verifyDescription(schema.getDescription(), (Schema)schema, reporter)) ? true : hasError;	
		

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
			out.print("<div class=header>" + title + " </div>");
		transformText(out, description);
		out.println("<p>");
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
					out.print("<pre>");
					i += 4;
					preformatted = true;
					continue;
				}
				if (isPreEnd(text, i)) {
					out.print("</pre>");
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
						out.print("<div class=tag>");
						out.print("&lt;");
						break;
					case '>' :
						out.print("&gt;");
						out.print("</div>");
						inTag = false;
						inCstring = false;
						break;
					case '&' :
						out.print("&amp;");
						break;
					case '\'' :
						out.print("&apos;");
						break;
					case '\"' :
						if (inTag) {
							if (inCstring) {
								out.print("&quot;");
								out.print("</div>");
								inCstring = false;
							} else {
								inCstring = true;
								out.print("<div class=cstring>");
								out.print("&quot;");
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

}
