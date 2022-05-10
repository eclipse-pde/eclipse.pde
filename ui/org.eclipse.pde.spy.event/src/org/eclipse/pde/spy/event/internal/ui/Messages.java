package org.eclipse.pde.spy.event.internal.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String CapturedEventFilters_AddFilter;
	public static String CapturedEventFilters_BasicTopicOfCapturedEvent;
	public static String CapturedEventFilters_CaptureEventWhen;
	public static String CapturedEventFilters_DefinedFilters;
	public static String CapturedEventFilters_ExpectedValueTitle;
	public static String CapturedEventFilters_FileToRemoveIsNotSelected;
	public static String CapturedEventFilters_FileToUpdateIsNotSelected;
	public static String CapturedEventFilters_FilterListIsEmpty;
	public static String CapturedEventFilters_IsEmpty;
	public static String CapturedEventFilters_IsNotSelected;
	public static String CapturedEventFilters_NewFilter;
	public static String CapturedEventFilters_RemoveAll;
	public static String CapturedEventFilters_RemoveSelected;
	public static String CapturedEventFilters_ResetToDefault;
	public static String CapturedEventFilters_UpdateSelected;
	public static String CapturedEventTree_Name;
	public static String CapturedEventTree_Param1;
	public static String CapturedEventTree_Param2;
	public static String EventSpyPart_HideFilters;
	public static String EventSpyPart_ShowFilters;
	public static String EventSpyPart_StartCapturingEvents;
	public static String EventSpyPart_StopCapturingEvents;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
