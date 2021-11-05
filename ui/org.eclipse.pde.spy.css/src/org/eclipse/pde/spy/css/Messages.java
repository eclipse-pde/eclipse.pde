package org.eclipse.pde.spy.css;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}

	public static String CSSScratchPadPart_Apply;
	public static String CSSScratchPadPart_Close;
	public static String CSSScratchPadPart_Engine;
	public static String CSSScratchPadPart_Error;
	public static String CSSScratchPadPart_Error_line_col;
	public static String CSSScratchPadPart_No_theme_engine_available;
	public static String CSSScratchPadPart_Time_ms;
	public static String CssSpyPart_actual_values;
	public static String CssSpyPart_All_shells;
	public static String CssSpyPart_Bounds;
	public static String CssSpyPart_Classes;
	public static String CssSpyPart_CSS;
	public static String CssSpyPart_CSS_Class;
	public static String CssSpyPart_CSS_Class_Element;
	public static String CssSpyPart_CSS_Classes;
	public static String CssSpyPart_CSS_ID;
	public static String CssSpyPart_CSS_ID_;
	public static String CssSpyPart_CSS_Inline_Styles;
	public static String CssSpyPart_CSS_Properties;
	public static String CssSpyPart_CSS_Properties_;
	public static String CssSpyPart_CSS_Rules;
	public static String CssSpyPart_CSS_Selector;
	public static String CssSpyPart_declared_in_CSS;
	public static String CssSpyPart_declared_in_CSS_rules;
	public static String CssSpyPart_DISPOSED;
	public static String CssSpyPart_Error;
	public static String CssSpyPart_Error_fetching_property;
	public static String CssSpyPart_Escape_to_dismiss;
	public static String CssSpyPart_Follow_UI_Selection;
	public static String CssSpyPart_Generates_CSS_rule_block_for_the_selected_widget;
	public static String CssSpyPart_Highlight_matching_widgets;
	public static String CssSpyPart_NamespaceURI;
	public static String CssSpyPart_Not_a_stylable_element;
	public static String CssSpyPart_plus_others;
	public static String CssSpyPart_Property;
	public static String CssSpyPart_Searching;
	public static String CssSpyPart_Searching_for;
	public static String CssSpyPart_Shell_parent;
	public static String CssSpyPart_Show_CSS_fragment;
	public static String CssSpyPart_Show_unset_properties;
	public static String CssSpyPart_Static_Pseudoinstances;
	public static String CssSpyPart_SWT_Layout;
	public static String CssSpyPart_SWT_Style_Bits;
	public static String CssSpyPart_Unable_to_set_property;
	public static String CssSpyPart_Value;
	public static String CssSpyPart_Widget;
	public static String CssSpyPart_Widget_data;
	public static String CssSpyPart_Widget_Skin_Class;
	public static String CssSpyPart_Widget_Skin_ID;
}
