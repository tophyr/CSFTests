package com.tophyr.test;

import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class CSTestCase<StartingActivity extends Activity> extends ActivityInstrumentationTestCase2<StartingActivity> {
	
	public static class Timeouts {
		public static final double NOW = 0.0;
		public static final double SHORT = 1.0;
		public static final double MEDIUM = 5.0;
		public static final double LONG = 10.0;
		public static final double NETWORK = 30.0;
	}

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
		return waitForActivity(name, Timeouts.LONG);
	}
	
	protected boolean waitForActivity(String name, double timeout) {
		if (m_Solo.getCurrentActivity().getClass().getSimpleName().equals(name))
			return true;
		
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
		assertActivityShown(msg, activityClass, Timeouts.LONG);
	}
	
	protected void assertActivityShown(Class<?> activityClass, double timeout) {
		assertActivityShown(null, activityClass, timeout);
	}
	
	protected void assertActivityShown(String msg, Class<?> activityClass, double timeout) {
		if (!waitForActivity(activityClass, timeout)) {
			if (msg == null)
				msg = String.format("%s not shown after %f seconds. Current activity: %s", 
						activityClass.getSimpleName(), timeout, m_Solo.getCurrentActivity().getClass().getSimpleName());
			fail(msg);
		}
	}
	
	protected boolean waitForText(CharSequence text) {
		return waitForText(text, Timeouts.LONG);
	}
	
	protected boolean waitForText(CharSequence text, double timeout) {
		return m_Solo.waitForText(text.toString(), 1, (long)(timeout * 1000)); // flaky - polls instead of listens for updates
	}
	
	protected void assertTextShown(CharSequence text) {
		assertTextShown(null, text);
	}
	
	protected void assertTextShown(String msg, CharSequence text) {
		assertTextShown(msg, text, Timeouts.LONG);
	}
	
	protected void assertTextShown(CharSequence text, double timeout) {
		assertTextShown(null, text, timeout);
	}
	
	protected void assertTextShown(String msg, CharSequence text, double timeout) {
		if (msg == null)
			msg = String.format("%s not shown after %f seconds", text, timeout);
		
		assertTrue(msg, waitForText(text, timeout));
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
	
	protected ImageView getImageView(int id) {
		return getView(id, ImageView.class);
	}
	
	protected void clickView(int id) {
		clickView(getView(id));
	}
	
	protected void clickView(View view) {
		m_Solo.clickOnView(view);
	}
	
	protected void clickButton(int id) {
		clickButton(getButton(id));
	}
	
	protected void clickButton(Button btn) {
		assertNotNull("Tried to click null Button.", btn);
		clickView(btn);
	}
	
	protected void clickImageView(int id) {
		clickImageView(getImageView(id));
	}
	
	protected void clickImageView(ImageView iv) {
		assertNotNull("Tried to click null ImageView.", iv);
		clickView(iv);
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
