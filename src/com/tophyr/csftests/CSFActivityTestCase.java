package com.tophyr.csftests;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.jayway.android.robotium.solo.Solo;

public class CSFActivityTestCase<StartingActivity extends Activity> extends ActivityInstrumentationTestCase2<StartingActivity> {
	
	public static class Timeouts {
		public static final double NOW = 0.0;
		public static final double SHORT = 1.0;
		public static final double MEDIUM = 5.0;
		public static final double LONG = 10.0;
		public static final double NETWORK = 30.0;
	}

	private Solo m_Solo;
	
	private boolean m_DontFinishActivities;
	
	public CSFActivityTestCase(Class<StartingActivity> startClass) {
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
	
	// Helpers
	
	protected boolean waitForActivity(Class<?> activityClass) {
		return waitForActivity(activityClass, Timeouts.LONG);
	}
	
	protected boolean waitForActivity(Class<?> activityClass, double timeout) {
		if (m_Solo.getCurrentActivity().getClass().equals(activityClass))
			return true;
		
		return m_Solo.waitForActivity(activityClass.getSimpleName(), (int)(timeout * 1000));
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
	
	protected CheckBox getCheckBox(int id) {
		return getView(id, CheckBox.class);
	}
	
	protected EditText getEditText(int id) {
		return getView(id, EditText.class);
	}
	
	protected ImageButton getImageButton(int id) {
		return getView(id, ImageButton.class);
	}
	
	protected ImageView getImageView(int id) {
		return getView(id, ImageView.class);
	}
	
	protected RadioButton getRadioButton(int id) {
		return getView(id, RadioButton.class);
	}
	
	protected TextView getTextView(int id) {
		return getView(id, TextView.class);
	}
	
	protected ToggleButton getToggleButton(int id) {
		return getView(id, ToggleButton.class);
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
	
	protected void clickCheckBox(int id) {
		clickCheckBox(getCheckBox(id));
	}
	
	protected void clickCheckBox(CheckBox cb) {
		assertNotNull("Tried to click null CheckBox.", cb);
		clickView(cb);
	}
	
	protected void clickEditText(int id) {
		clickEditText(getEditText(id));
	}
	
	protected void clickEditText(EditText editText) {
		assertNotNull("Tried to click null EditText.", editText);
		clickView(editText);
	}
	
	protected void clickImageButton(int id) {
		clickImageButton(getImageButton(id));
	}
	
	protected void clickImageButton(ImageButton ib) {
		assertNotNull("Tried to click null ImageButton.", ib);
		clickView(ib);
	}
	
	protected void clickImageView(int id) {
		clickImageView(getImageView(id));
	}
	
	protected void clickImageView(ImageView iv) {
		assertNotNull("Tried to click null ImageView.", iv);
		clickView(iv);
	}
	
	protected void clickRadioButton(int id) {
		clickRadioButton(getRadioButton(id));
	}
	
	protected void clickRadioButton(RadioButton rb) {
		assertNotNull("Tried to click null RadioButton.", rb);
		clickView(rb);
	}
	
	protected void clickTextView(int id) {
		clickTextView(getTextView(id));
	}
	
	protected void clickTextView(TextView tv) {
		assertNotNull("Tried to click null TextView.", tv);
		clickView(tv);
	}
	
	protected void clickToggleButton(int id) {
		clickToggleButton(getToggleButton(id));
	}
	
	protected void clickToggleButton(ToggleButton tb) {
		assertNotNull("Tried to click null ToggleButton.", tb);
		clickView(tb);
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
		assertNotNull(String.format("Tried to enter '%s' into null EditText.", text), editText);
		m_Solo.enterText(editText, text.toString());
	}
	
	protected void typeText(int id, CharSequence text) {
		typeText(getEditText(id), text);
	}
	
	protected void typeText(EditText editText, CharSequence text) {
		assertNotNull(String.format("Tried to type '%s' into null EditText.", text), editText);
		m_Solo.typeText(editText, text.toString());
	}
	
	
	// FindView stuff
	
	private static class FindViewResult<T extends View> {
		public List<T> views;
		public StringBuilder description;
		
		public FindViewResult() {
			views = new LinkedList<T>();
			description = new StringBuilder();
		}
	}
	
	protected <T extends View> T findView(FindViewResult<T> pattern) {
		return findViews(pattern).get(0);
	}
	
	protected <T extends View> List<T> findViews(FindViewResult<T> pattern) {
		assertNotNull("Tried to find views with null pattern.", pattern);
		assertFalse(pattern.description.toString(), pattern.views.isEmpty());
		return pattern.views;
	}
	
	protected FindViewResult<View> all() {
		FindViewResult<View> result = new FindViewResult<View>();
		result.description = new StringBuilder("all views");
		result.views = m_Solo.getViews();
		return result;
	}
	
	protected FindViewResult<View> withId(int id) {
		return withIds(Arrays.asList(id));
	}
	
	protected FindViewResult<View> withIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty())
			fail("Tried to search on null or empty id list.");
		
		FindViewResult<View> result = new FindViewResult<View>();
		
		result.description.append("with id");
		if (ids.size() > 1) result.description.append("s");
		result.description.append(" ");
		for (int i = 0; i < ids.size() - 1; i++) {
			result.description.append(ids.get(i));
			result.description.append(", ");
			View v = getView(ids.get(i), View.class, true);
			if (v != null)
				result.views.add(v);
		}
		
		result.description.append(ids.get(ids.size() - 1));
		View v = getView(ids.get(ids.size() - 1), View.class, true);
		if (v != null)
			result.views.add(v);
		
		return result;
	}
	
	protected FindViewResult<TextView> exactText(CharSequence text) {
		assertNotNull("Tried to search on null exact text.", text);
		String s = text.toString();
		
		FindViewResult<TextView> result = isTextView(all());
		
		result.description.append(String.format(" that exactly say '%s'", text));
		
		Iterator<TextView> iter = result.views.iterator();
		while (iter.hasNext()) {
			if (!s.contentEquals(iter.next().getText()))
				iter.remove();
		}
		
		return result;
	}
	
	protected FindViewResult<TextView> containsText(CharSequence text) {
		assertNotNull("Tried to search on null text.", text);
		
		FindViewResult<TextView> result = isTextView(all());
		
		result.description.append(String.format(" that say '%s'", text));
		
		Iterator<TextView> iter = result.views.iterator();
		while (iter.hasNext()) {
			CharSequence elemText = iter.next().getText();
			if (elemText == null || !elemText.toString().contains(text))
				iter.remove();
		}
		
		return result;
	}
	
	protected FindViewResult<TextView> matchesRegex(CharSequence regex) {
		assertNotNull("Tried to search on null regex.", regex);
		Pattern p = Pattern.compile(regex.toString());
		
		FindViewResult<TextView> result = isTextView(all());
		
		result.description.append(String.format(" that match '%s'", regex));
		
		Iterator<TextView> iter = result.views.iterator();
		while (iter.hasNext()) {
			if (!p.matcher(iter.next().getText()).matches())
				iter.remove();
		}
		
		return result;
	}
	
	protected FindViewResult<TextView> isTextView(FindViewResult<? extends View> in) {
		return isType(in, TextView.class);
	}
	
	protected <T extends View> FindViewResult<T> isType(FindViewResult<? extends View> in, Class<T> type) {
		FindViewResult<T> result = new FindViewResult<T>();
		
		for (View v : in.views) {
			if (type.isAssignableFrom(v.getClass()))
				result.views.add(type.cast(v));
		}
		
		return result;
	}
}
