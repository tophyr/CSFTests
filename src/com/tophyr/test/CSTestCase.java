package com.tophyr.test;

import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CSTestCase<StartingActivity extends Activity> extends ActivityInstrumentationTestCase2<StartingActivity> {
	
	public static final double SHORT_TIMEOUT = 1.0;
	public static final double MEDIUM_TIMEOUT = 5.0;
	public static final double LONG_TIMEOUT = 10.0;

	private Solo m_Solo;
	
	private boolean m_DontFinishActivities;
	
	public CSTestCase(Class<StartingActivity> startClass) {
		super(startClass);
		
		m_DontFinishActivities = false;
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		m_Solo = new Solo(getInstrumentation(), null);
	}
	
	@Override
	protected void tearDown() throws Exception {
		if (!m_DontFinishActivities)
			m_Solo.finishOpenedActivities();
		
		super.tearDown();
	}
	
	// Accessors
	
	protected void setFinishActivitiesWhenDone(boolean finish) {
		m_DontFinishActivities = finish;
	}
	
	// SHOULD GO AWAY
	protected Solo getSolo() {
		return m_Solo;
	}
	
	// Helpers
	
	protected boolean waitForActivity(String name) {
		return m_Solo.waitForActivity(name);
	}
	
	protected boolean waitForActivity(String name, double timeout) {
		return m_Solo.waitForActivity(name, (int)(timeout * 1000));
	}
	
	protected boolean waitForActivity(Class<?> activityClass) {
		return waitForActivity(activityClass.getSimpleName());
	}
	
	protected boolean waitForActivity(Class<?> activityClass, double timeout) {
		return waitForActivity(activityClass.getSimpleName(), timeout);
	}
	
	protected void assertActivityShown(Class<?> activityClass) {
		assertActivityShown(null, activityClass);
	}
	
	protected void assertActivityShown(String msg, Class<?> activityClass) {
		assertActivityShown(msg, activityClass, 10000);
	}
	
	protected void assertActivityShown(Class<?> activityClass, double timeout) {
		assertActivityShown(null, activityClass, timeout);
	}
	
	protected void assertActivityShown(String msg, Class<?> activityClass, double timeout) {
		if (msg == null)
			msg = String.format("%s not shown after %f seconds.", activityClass.getSimpleName(), timeout);
		
		assertTrue(msg, waitForActivity(activityClass, timeout));
	}
	
	protected View getView(int id) {
		return m_Solo.getView(id);
	}
	
	protected <T> T getView(int id, Class<T> type) {
		return getView(id, type, false);
	}
	
	protected <T> T getView(int id, Class<T> type, boolean allowNull) {
		try {
			View v = getView(id);
			if (!allowNull)
				assertNotNull(String.format("%s with id %d not found.", type.getSimpleName(), id), v);
			return type.cast(v);
		} catch (ClassCastException e) {
			fail(String.format("View with id %d not a %s", id, type.getSimpleName()));
			return null; // lame, fail throws an error, shouldn't have to fake a return! oh well.
		}
	}
	
	protected Button getButton(int id) {
		return getView(id, Button.class);
	}
	
	protected EditText getEditText(int id) {
		return getView(id, EditText.class);
	}
	
	protected void clickButton(int id) {
		clickButton(getButton(id));
	}
	
	protected void clickButton(Button btn) {
		assertNotNull("Tried to click null Button.", btn);
		m_Solo.clickOnView(btn);
	}
	
	protected void clearEditText(int id) {
		clearEditText(getEditText(id));
	}
	
	protected void clearEditText(EditText editText) {
		assertNotNull("Tried to clear null EditText.", editText);
		m_Solo.clearEditText(editText);
	}
	
	protected void enterText(int id, CharSequence text) {
		enterText(getEditText(id), text);
	}
	
	protected void enterText(EditText editText, CharSequence text) {
		m_Solo.enterText(editText, text.toString());
	}
}
