package org.eclipse.pde.spy.event.internal.model;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String CapturedEventFilter_InvalidValueFormat;
	public static String CapturedEventFilter_Name;
	public static String CapturedEventFilter_Value;
	public static String CapturedEventFilter_When;
	public static String ItemToFilter_ChangedElement;
	public static String ItemToFilter_EventPublisher;
	public static String ItemToFilter_Name;
	public static String ItemToFilter_NameAndValue;
	public static String ItemToFilter_NotFound;
	public static String ItemToFilter_SomeParameterValue;
	public static String ItemToFilter_Title;
	public static String ItemToFilter_Topic;
	public static String Operator_Contains;
	public static String Operator_EqualsTo;
	public static String Operator_NotContains;
	public static String Operator_NotEqualsTo;
	public static String Operator_NotFoundFor;
	public static String Operator_NotStartsWith;
	public static String Operator_StartsWith;
	public static String Operator_Title;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
