package org.eclipse.pde.internal.builders;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;

import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.SourceDOMParser;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.w3c.dom.Node;
import org.xml.sax.*;
import org.eclipse.core.resources.IFile;

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

	private void appendAttlist(
		PrintWriter out,
		ISchemaAttribute att,
		int maxWidth) {
		// add three spaces
		out.print("<br><samp>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		// add name
		out.print(att.getName());
		// fill spaces to allign data type
		int delta = maxWidth - att.getName().length();
		for (int i = 0; i < delta + 1; i++) {
			out.print("&nbsp;");
		}
		// add data type
		ISchemaSimpleType type = att.getType();
		ISchemaRestriction restriction = null;
		if (type != null)
			restriction = type.getRestriction();
		String typeName = type != null ? type.getName().toLowerCase() : "string";
		if (typeName.equals("boolean")) {
			out.print("(true | false) \"false\"");
		} else if (restriction != null) {
			appendRestriction(restriction, out);
		} else {
			out.print("CDATA ");
		}

		// add use
		if (att.getUse() == ISchemaAttribute.REQUIRED)
			out.print("#REQUIRED");
		else if (att.getUse() == ISchemaAttribute.DEFAULT) {
			out.print("\"" + att.getValue() + "\"");
		} else
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
		InputStream is,
		PrintWriter out,
		PluginErrorReporter reporter) {
		SourceDOMParser parser = createDOMTree(is, reporter);
		if (parser == null)
			return;
		Node root = parser.getDocument().getDocumentElement();
		Schema schema = new Schema((ISchemaDescriptor) null, null);
		schema.traverseDocumentTree(root, parser.getLineTable());
		if (verifySchema(schema, reporter) && CompilerFlags.getBoolean(CompilerFlags.S_CREATE_DOCS))
			transform(out, schema);
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
					message = PDE.getFormattedMessage(KEY_BOOLEAN_INVALID, args);
					// this kind cannot have boolean type
					reporter.reportError(message, line);
					errors++;
				}
				if (type instanceof SchemaSimpleType
					&& ((SchemaSimpleType) type).getRestriction() != null) {
					// should not have restriction
					message = PDE.getFormattedMessage(KEY_RESTRICTION_INVALID, args);
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
				message = PDE.getFormattedMessage(KEY_RESTRICTION_INVALID, args);
				reporter.reportError(message, line);
				errors++;
			}
		}
		if (attribute.getUse() != ISchemaAttribute.DEFAULT) {
			if (attribute.getValue() != null) {
				// value makes no sense without 'default' use
				message = PDE.getFormattedMessage(KEY_VALUE_WITHOUT_DEFAULT, args);
				reporter.reportError(message, line);
				errors++;
			}
		} else {
			if (attribute.getValue() == null) {
				// there must be a value set for this use
				message = PDE.getFormattedMessage(KEY_DEFAULT_WITHOUT_VALUE, args);
				reporter.reportError(message, line);
				errors++;
			}
		}
		return errors;
	}

	public void transform(PrintWriter out, ISchema schema) {
		out.println(
			"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
		out.print("<HEAD>");
		out.println(
			"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
		out.println("</HEAD>");
		out.println("<HTML>");
		out.println("<BODY>");
		out.println("<H1><CENTER>" + schema.getName() + "</CENTER></H1>");
		out.println("<H2>Identifier</H2>");
		out.print(schema.getQualifiedPointId());
		transformSection(out, schema, "Since:", IDocumentSection.SINCE);
		out.println("<H2>Description</H2>");
		transformText(out, schema.getDescription());
		out.println("<H2>Markup</H2>");
		transformMarkup(out, schema);
		transformSection(out, schema, "Example", IDocumentSection.EXAMPLES);
		transformSection(out, schema, "API Information", IDocumentSection.API_INFO);
		transformSection(out, schema, "Supplied Implementation", IDocumentSection.IMPLEMENTATION);
		transformSection(out, schema, IDocumentSection.COPYRIGHT);
		out.println("</BODY>");
		out.println("</HTML>");
	}
	private void transformElement(PrintWriter out, ISchemaElement element) {
		String name = element.getName();
		String dtd = element.getDTDRepresentation();
		out.print("<p><samp>&nbsp;&nbsp; &lt;!ELEMENT " + name + " " + dtd);
		out.println("&gt;</samp>");

		ISchemaAttribute[] attributes = element.getAttributes();
		String description = element.getDescription();

		if (description!=null && description.trim().length()>0) {
			out.print("<p>");
			out.print("&nbsp;&nbsp; ");
			transformText(out, description);
			out.println("</p>");
			if (attributes.length>0)
				out.println("<p></p>");
		}		
		else if (attributes.length>0) {
			out.print("<br><br>");
		}
		
		if (attributes.length == 0)
			return;

		out.println("<samp>&nbsp;&nbsp; &lt;!ATTLIST " + name + "</samp>");
		int maxWidth = calculateMaxAttributeWidth(element.getAttributes());
		for (int i = 0; i < attributes.length; i++) {
			appendAttlist(out, attributes[i], maxWidth);
		}
		out.println("<br><samp>&nbsp;&nbsp; &gt;</samp>");

		out.println("<ul>");
		for (int i = 0; i < attributes.length; i++) {
			ISchemaAttribute att = attributes[i];
			out.print(
				"<li><b>" + att.getName() + "</b> - " + att.getDescription() + "</li>");
		}
		out.println("</ul>");
	}
	private void transformMarkup(PrintWriter out, ISchema schema) {
		ISchemaElement[] elements = schema.getElements();

		for (int i = 0; i < elements.length; i++) {
			ISchemaElement element = elements[i];
			transformElement(out, element);
		}
	}
	
	private void transformSection(PrintWriter out, ISchema schema, String sectionId) {
		transformSection(out, schema, null, sectionId);
	}
	private void transformSection(
		PrintWriter out,
		ISchema schema,
		String title,
		String sectionId) {
		IDocumentSection[] sections = schema.getDocumentSections();
		IDocumentSection section = null;
		for (int i = 0; i < sections.length; i++) {
			if (sections[i].getSectionId().equals(sectionId)) {
				section = sections[i];
				break;
			}
		}
		if (section == null)
			return;
		String description = section.getDescription();
		if (description==null || description.trim().length()==0)
			return;
		if (title!=null) out.println("<H2>"+title+"</H2>");
		transformText(out, description);
	}
	private void transformText(PrintWriter out, String text) {
		boolean preformatted = false;
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
					continue;
				}
			}
			if (preformatted) {
				switch (c) {
					case '<' :
						out.print("&lt;");
						break;
					case '>' :
						out.print("&gt;");
						break;
					case '&' :
						out.print("&amp;");
						break;
					default :
						out.print(c);
				}
			} else
				out.print(c);
		}
	}
}