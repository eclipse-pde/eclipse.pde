package org.eclipse.pde.internal.ui.ischema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Document section is portion of the extension point schema
 * definition that will be taken and built into the final reference
 * HTML document. There are several predefined document sections
 * that PDE recognizes:
 * <ul>
 * <li>MARKUP - will be used for "Markup" section</li>
 * <li>EXAMPLES - will be used for "Examples" section</li>
 * <li>API_INFO - will be used for "API information" section</li>
 * <li>IMPLEMENTATION - will be used for "Supplied Implementation" section</li>
 * </ul>
 * Text that objects of this class carry can contain HTML tags that
 * will be copied into the target document as-is.
 */
public interface IDocumentSection extends ISchemaObject {
	/**
	 * Section Id for the "Markup" section of the target reference document
	 */
	String MARKUP = "markup";
	/**
	 * Section Id for the "Examples" section of the target reference document
	 */
	String EXAMPLES = "examples";
	/**
	 * Section Id for the "Supplied Implementation" section of the target reference document
	 */
	String IMPLEMENTATION = "implementation";
	/**
	 * Section Id for the "API Information" section of the target reference document
	 */
	String API_INFO = "apiInfo";
	/**
	 * Section Id for the copyright statement section of the target reference document
	 */
	String COPYRIGHT = "copyright";
/**
 * Returns the Id of this section.
 */
public String getSectionId();
}
