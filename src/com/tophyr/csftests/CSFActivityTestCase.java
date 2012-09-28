package com.tophyr.csftests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.graphics.Rect;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
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
	
//	private static abstract class Predicate<T> {
//		abstract boolean test(T specimen);
//	}
	
	protected static class FindViewResult<T extends View> {
		
		@SuppressWarnings("unchecked")
		static <T extends View> FindViewResult<T> cast(FindViewResult<? extends View> result, Class<T> type) {
			return (FindViewResult<T>)result;
		}
		
		List<T> views;
		String description;
		
		FindViewResult() {
			views = new LinkedList<T>();
			description = "";
		}
		
		// YAGNI
		// more functional, but actually *more* verbose as well
		// required for decent encapsulation, but that's not a goal of this class at this time; it's entirely opaque outside this package
//		void filter(Predicate<T> predicate) {
//			Iterator<T> iter = views.iterator();
//			while (iter.hasNext()) {
//				if (!predicate.test(iter.next()))
//					iter.remove();
//			}
//		}
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
		result.description = "all views";
		result.views.addAll(m_Solo.getViews());
		return result;
	}
	
	protected FindViewResult<View> withId(int id) {
		return withIds(Arrays.asList(id));
	}
	
	protected FindViewResult<View> withIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty())
			fail("Tried to search on null or empty id list.");
		
		FindViewResult<View> result = new FindViewResult<View>();
		
		StringBuilder description = new StringBuilder();
		description.append("with id");
		if (ids.size() > 1) description.append("s");
		description.append(" ");
		for (int i = 0; i < ids.size() - 1; i++) {
			description.append(ids.get(i));
			description.append(", ");
			View v = getView(ids.get(i), View.class, true);
			if (v != null)
				result.views.add(v);
		}
		
		description.append(ids.get(ids.size() - 1));
		View v = getView(ids.get(ids.size() - 1), View.class, true);
		if (v != null)
			result.views.add(v);
		
		result.description = description.toString();
		return result;
	}
	
	protected FindViewResult<TextView> exactText(CharSequence text) {
		assertNotNull("Tried to search on null exact text.", text);
		String s = text.toString();
		
		FindViewResult<TextView> result = isTextView(all());
		
		result.description = String.format("%s that exactly say '%s'", result.description, text);
		
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
		
		result.description = String.format("%s that say '%s'", result.description, text);
		
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
		
		result.description = String.format("%s that match '%s'", result.description, regex);
		
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
	
	protected <T extends View> FindViewResult<T> isType(FindViewResult<? extends View> result, Class<T> type) {
		result.description = String.format("%s that are %ss", result.description, type.getSimpleName());
		
		Iterator<? extends View> iter = result.views.iterator();
		while (iter.hasNext()) {
			if (!type.isAssignableFrom(iter.next().getClass()))
				iter.remove();
		}
		
		return FindViewResult.cast(result, type);
	}
	
	private boolean isInFrontOf(View front, View back) {
		if (front == null || back == null)
			return false;
		
		if (front == back)
			return true; // if you are yourself, we'll say yes you're in front of yourself too
		
		if (front.getParent() == back)
			return true;
		
		if (back.getParent() == front)
			return false;
		
		HashSet<ViewParent> frontParents = new HashSet<ViewParent>();
		for (ViewParent p = (ViewParent)front; p != null; p = p.getParent()) {
			frontParents.add(p);
		}
		
		ViewGroup commonParent = null;
		ViewParent lastUniqueFrontAncestor = null, lastUniqueBackAncestor = null;
		for (ViewParent p = (ViewParent)back, prev = null; p != null; prev = p, p = p.getParent()) {
			if (frontParents.contains(p)) {
				commonParent = (ViewGroup)p;
				lastUniqueBackAncestor = prev;
				break;
			}
		}
		
		if (lastUniqueBackAncestor == null)
			return true; // front is a descendant (really, is a child - should have gotten caught above) of back
		
		if (commonParent == null)
			return false; // views are not in the same tree
		
		for (lastUniqueFrontAncestor = (ViewParent)front; 
			 lastUniqueFrontAncestor != null && lastUniqueFrontAncestor.getParent() != commonParent;
			 lastUniqueFrontAncestor = lastUniqueFrontAncestor.getParent())
			;
		
		if (lastUniqueFrontAncestor == null)
			return false; // back is a descendant of front
		
		for (int i = 0; i < commonParent.getChildCount(); i++) {
			// views are drawn in their iteration order, so their iteration order effectively is their z-order
			// thus if we iterate, whichever view we find first is behind
			if (commonParent.getChildAt(i) == lastUniqueFrontAncestor)
				return false;
			if (commonParent.getChildAt(i) == lastUniqueBackAncestor)
				return true;
		}
		
		// shouldn't ever get here
		throw new RuntimeException("Found a common parent, but neither unique ancestor was a child of it.");
	}
	
	private <T extends View> FindViewResult<T> coveredBy_internal(CombinationMatch<View> covers, FindViewResult<T> result, final boolean over) {
		String desc;
		if (over)
			desc = "cover";
		else
			desc = "are covered by";
		result.description = String.format("%s that %s %s", result.description, desc, covers.getDescription());
		
		MatchTest<View, View> test = new MatchTest<View, View>() {
			@Override
			boolean matches(View a, View b) {
				int xy[] = new int[2];
				a.getLocationOnScreen(xy);
				Rect ar = new Rect(xy[0], xy[1], a.getWidth() + xy[0], a.getHeight() + xy[1]);
				b.getLocationOnScreen(xy);
				Rect br = new Rect(xy[0], xy[1], b.getWidth() + xy[0], b.getHeight() + xy[1]);
				
				return (a.getVisibility() == View.VISIBLE &&
						b.getVisibility() == View.VISIBLE &&
						Rect.intersects(ar, br) &&
						(over && isInFrontOf(a, b)) ||
						(!over && isInFrontOf(b, a)));
			}
		};
		
		Iterator<T> iter = result.views.iterator();
		while (iter.hasNext()) {
			if (!covers.matches(test, iter.next()))
				iter.remove();
		}
		
		return result;
	}
	
	protected <T extends View> FindViewResult<T> coveredBy(CombinationMatch<View> covers, FindViewResult<T> result) {
		return coveredBy_internal(covers, result, false);
	}
	
	protected <T extends View> FindViewResult<T> covers(CombinationMatch<View> under, FindViewResult<T> result) {
		return coveredBy_internal(under, result, true);
	}
	
	protected <T extends View> FindViewResult<T> toLeftOf(CombinationMatch<View> anchor, FindViewResult<T> result) {
		result.description = String.format("%s to the left of %s", result.description, anchor.getDescription());
		
		MatchTest<View, View> test = new MatchTest<View, View>() {
			@Override
			boolean matches(View a, View b) {
				int xy[] = new int[2];
				a.getLocationOnScreen(xy);
				Rect ar = new Rect(xy[0], xy[1], a.getWidth() + xy[0], a.getHeight() + xy[1]);
				b.getLocationOnScreen(xy);
				Rect br = new Rect(xy[0], xy[1], b.getWidth() + xy[0], b.getHeight() + xy[1]);
				
				return (ar.right <= br.left);
			}
		};
		
		Iterator<T> iter = result.views.iterator();
		while (iter.hasNext()) {
			if (!anchor.matches(test, iter.next()))
				iter.remove();
		}
		
		return result;
	}
	
	protected <T extends View> FindViewResult<T> toRightOf(CombinationMatch<View> anchor, FindViewResult<T> result) {
		result.description = String.format("%s to the right of %s", result.description, anchor.getDescription());
		
		MatchTest<View, View> test = new MatchTest<View, View>() {
			@Override
			boolean matches(View a, View b) {
				int xy[] = new int[2];
				a.getLocationOnScreen(xy);
				Rect ar = new Rect(xy[0], xy[1], a.getWidth() + xy[0], a.getHeight() + xy[1]);
				b.getLocationOnScreen(xy);
				Rect br = new Rect(xy[0], xy[1], b.getWidth() + xy[0], b.getHeight() + xy[1]);
				
				return (ar.left >= br.right);
			}
		};
		
		Iterator<T> iter = result.views.iterator();
		while (iter.hasNext()) {
			if (!anchor.matches(test, iter.next()))
				iter.remove();
		}
		
		return result;
	}
	
	protected <T extends View> FindViewResult<T> above(CombinationMatch<View> anchor, FindViewResult<T> result) {
		result.description = String.format("%s above %s", result.description, anchor.getDescription());
		
		MatchTest<View, View> test = new MatchTest<View, View>() {
			@Override
			boolean matches(View a, View b) {
				int xy[] = new int[2];
				a.getLocationOnScreen(xy);
				Rect ar = new Rect(xy[0], xy[1], a.getWidth() + xy[0], a.getHeight() + xy[1]);
				b.getLocationOnScreen(xy);
				Rect br = new Rect(xy[0], xy[1], b.getWidth() + xy[0], b.getHeight() + xy[1]);
				
				return (ar.bottom <= br.top);
			}
		};
		
		Iterator<T> iter = result.views.iterator();
		while (iter.hasNext()) {
			if (!anchor.matches(test, iter.next()))
				iter.remove();
		}
		
		return result;
	}
	
	protected <T extends View> FindViewResult<T> below(CombinationMatch<View> anchor, FindViewResult<T> result) {
		result.description = String.format("%s below %s", result.description, anchor.getDescription());
		
		MatchTest<View, View> test = new MatchTest<View, View>() {
			@Override
			boolean matches(View a, View b) {
				int xy[] = new int[2];
				a.getLocationOnScreen(xy);
				Rect ar = new Rect(xy[0], xy[1], a.getWidth() + xy[0], a.getHeight() + xy[1]);
				b.getLocationOnScreen(xy);
				Rect br = new Rect(xy[0], xy[1], b.getWidth() + xy[0], b.getHeight() + xy[1]);
				
				return (ar.top >= br.bottom);
			}
		};
		
		Iterator<T> iter = result.views.iterator();
		while (iter.hasNext()) {
			if (!anchor.matches(test, iter.next()))
				iter.remove();
		}
		
		return result;
	}
	
	// combination matching
	private static abstract class MatchTest<A, B> {
		abstract boolean matches(A a, B b);
	}
	
	private static class CombinationMatch<T> {
		private List<T> m_Potentials;
		private String m_Description;
		private int m_MinMatches;
		private int m_MaxMatches;
		
		public CombinationMatch(List<T> potentials, int min, int max, String description) {
			m_Potentials = potentials;
			m_MinMatches = min;
			m_MaxMatches = max;
			m_Description = description;
		}
		
		public boolean matches(MatchTest<T, View> test, View specimen) {
			int matches = 0;
			Iterator<T> iter = m_Potentials.iterator();
			while (iter.hasNext() && matches <= m_MaxMatches) {
				if (test.matches(iter.next(), specimen))
					matches++;
			}
			
			return (matches >= m_MinMatches && matches <= m_MaxMatches);
		}
		
		public String getDescription() {
			StringBuilder sb = new StringBuilder();
			if (m_MinMatches > 0) {
				sb.append("at least ");
				sb.append(m_MinMatches);
				sb.append(" ");
			}
			if (m_MaxMatches > 0) {
				if (sb.length() > 0)
					sb.append(" but ");
				sb.append("at most ");
				sb.append(m_MaxMatches);
				sb.append(" ");
			}
			sb.append(m_Description);
			
			return sb.toString();
		}
	}
	
	protected <T extends View> CombinationMatch<T> all(FindViewResult<T> result) {
		return new CombinationMatch<T>(result.views, result.views.size(), Integer.MAX_VALUE, result.description);
	}
	
	protected <T extends View> CombinationMatch<T> any(FindViewResult<T> result) {
		return new CombinationMatch<T>(result.views, 1, Integer.MAX_VALUE, result.description);
	}
}
