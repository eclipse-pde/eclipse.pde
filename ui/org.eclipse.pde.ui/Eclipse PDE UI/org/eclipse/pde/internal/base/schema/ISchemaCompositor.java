package org.eclipse.pde.internal.base.schema;

import java.util.*;
/**
 * Compositor is a container that can contain other compositors or
 * references to element defined at the global scope. Compositors
 * are used to recursively define content model for the type.
 * Compositor kind (all, choice, sequence or group) defines
 * how to interpret its children.
 */
public interface ISchemaCompositor extends ISchemaObject, ISchemaRepeatable {
	/**
	 * Indicates that the children can be in any order and cardinality.
	 */
	public static final int ALL = 0;
	/**
	 * Indicates that an only one of the compositor's children can
	 * appear at this location (DTD eq: "|")
	 */
	public static final int CHOICE = 1;
	/**
	 * Indicates that the children must appear in sequence in the schema documents (DTD eq: "," )
	 */
	public static final int SEQUENCE = 2;
	/**
	 * Indicates that this compositor simply serves as a group (DTD eq: "()" )
	 */
	public static final int GROUP = 3;
	/**
	 * Keyword table for compositors.
	 */
	public static final String[] kindTable =
		{ "all", "choice", "sequence", "group" };
/**
 * Returns the number of children of this compositor.
 * @return number of compositor children
 */
public int getChildCount();
/**
 * Returns children of this compositor.
 * @return compositor children
 */
public ISchemaObject[] getChildren();
/**
 * Returns a flag that defines how the children of this compositors should be
 * treated when computing type grammar (one of ALL, CHOICE, GROUP, SEQUENCE).
 * @return compositor kind value
 */
public int getKind();
}
