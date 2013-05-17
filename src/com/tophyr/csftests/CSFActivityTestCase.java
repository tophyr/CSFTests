package com.tophyr.csftests;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
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
	
	private static class CSActivityMonitor extends ActivityMonitor {
		
		private static final Field s_mResumed;
		
		static {
			Field f;
			try {
				f = Activity.class.getDeclaredField("mResumed");
			} catch (SecurityException e) {
				f = null;
			} catch (NoSuchFieldException e) {
				f = null;
			}
			s_mResumed = f;
			s_mResumed.setAccessible(true);
		}
		
		private WeakReference<Activity> m_LastResumedActivity;
		private final Thread m_Thread;
		private volatile boolean m_Running;
		
		private final HashMap<Class<?>, Object> m_ResumeWaiters;
		private final HashMap<Activity, Object> m_FinishWaiters;
		
		public CSActivityMonitor() {
			super((IntentFilter)null, null, false);
			
			if (s_mResumed == null)
				throw new RuntimeException("Unable to access mResumed field of Activity.");
			
			m_LastResumedActivity = new WeakReference<Activity>(null);
			
			m_ResumeWaiters = new HashMap<Class<?>, Object>();
			m_FinishWaiters = new HashMap<Activity, Object>();
			
			m_Running = true;
			
			m_Thread = new Thread(new Runnable() {
				@Override
				public void run() {
					Activity a;
					
					synchronized (CSActivityMonitor.this) {
						while (m_Running) {
							try {
									CSActivityMonitor.this.wait();
									a = getLastActivity();
							} catch (InterruptedException e) {
								continue;
							}
							
							if (a == null)
								continue; // wtfbubbles
							
							try {
								if (s_mResumed.getBoolean(a) && m_LastResumedActivity.get() != a) {
									m_LastResumedActivity = new WeakReference<Activity>(a);
									
									Object waitLock = null;
									final Class<?> cls = a.getClass();
									synchronized (m_ResumeWaiters) {
										if (m_ResumeWaiters.containsKey(cls))
											waitLock = m_ResumeWaiters.get(cls);
									}
									if (waitLock != null) {
										synchronized (waitLock) {
											waitLock.notifyAll();
										}
									}
								} else if (a.isFinishing()) {
									Object waitLock = null;
									synchronized (m_FinishWaiters) {
										if (m_FinishWaiters.containsKey(a))
											waitLock = m_FinishWaiters.get(a);
									}
									if (waitLock != null) {
										synchronized (waitLock) {
											waitLock.notifyAll();
										}
									}
								}
							} catch (IllegalArgumentException e) {
								Log.e("CSFActivityTestCase", "Supposed-to-be-impossible error:", e);
							} catch (IllegalAccessException e) {
								Log.e("CSFActivityTestCase", "Supposed-to-be-impossible error:", e);
							}
						}
					}
				}
			});
			m_Thread.start();
		}
		
		public Activity getLastResumedActivity() {
			return m_LastResumedActivity.get();
		}
		
		public boolean waitForResumedActivity(Class<?> cls, long timeout) {
			if (getLastResumedActivity() != null && getLastResumedActivity().getClass() == cls)
				return true;
			
			final Object waitLock;
			synchronized (m_ResumeWaiters) {
				if (m_ResumeWaiters.containsKey(cls))
					waitLock = m_ResumeWaiters.get(cls);
				else {
					waitLock = new Object();
					m_ResumeWaiters.put(cls, waitLock);
				}	
			}
			
			synchronized (waitLock) {
				final long millisGoal = SystemClock.uptimeMillis() + timeout;
				while (timeout > 0) {
					try {
						waitLock.wait(timeout);
						return getLastResumedActivity().getClass() == cls;
					} catch (InterruptedException e) {
						timeout = millisGoal - SystemClock.uptimeMillis();
					}
				}
			}
			
			return false;
		}
		
		public boolean waitForFinishedActivity(Activity a, long timeout) {
			if (a == null)
				throw new IllegalArgumentException("Activity may not be null.");
			if (a.isFinishing())
				return true;
			
			final Object waitLock;
			synchronized (m_FinishWaiters) {
				if (m_FinishWaiters.containsKey(a))
					waitLock = m_FinishWaiters.get(a);
				else {
					waitLock = new Object();
					m_FinishWaiters.put(a, waitLock);
				}	
			}
			
			synchronized (waitLock) {
				final long millisGoal = SystemClock.uptimeMillis() + timeout;
				while (timeout > 0) {
					try {
						waitLock.wait(timeout);
						return a.isFinishing();
					} catch (InterruptedException e) {
						timeout = millisGoal - SystemClock.uptimeMillis();
					}
				}
			}
			
			return false;
		}

		@Override
		public void finalize() throws Throwable {
			m_Running = false;
			m_Thread.interrupt();
			super.finalize();
		}
	}

	private Solo m_Solo;
	private CSActivityMonitor m_ActivityMonitor;
	private Instrumentation m_Instrumentation;
	
	private boolean m_DontFinishActivities;
	
	public CSFActivityTestCase(Class<StartingActivity> startClass) {
		super(startClass);
		
		m_DontFinishActivities = false;
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		m_Instrumentation = getInstrumentation();
		m_ActivityMonitor = new CSActivityMonitor();
		m_Instrumentation.addMonitor(m_ActivityMonitor);
		m_Solo = new Solo(m_Instrumentation, null);
	}
	
	@Override
	protected void tearDown() throws Exception {
		if (!m_DontFinishActivities)
			m_Solo.finishOpenedActivities();
		
		m_Instrumentation.removeMonitor(m_ActivityMonitor);
		m_ActivityMonitor = null;
		
		super.tearDown();
	}
	
	// Accessors
	
	protected void setFinishActivitiesWhenDone(boolean finish) {
		m_DontFinishActivities = finish;
	}
	
	// Helpers
	
	protected Activity getCurrentActivity() {
		return m_ActivityMonitor.getLastResumedActivity();
	}
	
	protected boolean waitForActivity(Class<?> activityClass) {
		return waitForActivity(activityClass, Timeouts.LONG);
	}
	
	protected boolean waitForActivity(Class<?> activityClass, double timeout) {
		return m_ActivityMonitor.waitForResumedActivity(activityClass, (long)(timeout * 1000 + 1));
	}
	
	protected boolean waitForActivityToFinish(Activity a) {
		return waitForActivityToFinish(a, Timeouts.LONG);
	}
	
	protected boolean waitForActivityToFinish(Activity a, double timeout) {
		return m_ActivityMonitor.waitForFinishedActivity(a, (long)(timeout * 1000 + 1));
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
						activityClass.getSimpleName(), timeout, getCurrentActivity().getClass().getSimpleName());
			fail(msg);
		}
	}
	
	protected boolean waitForFragmentByTag(String tag) {
		return m_Solo.waitForFragmentByTag(tag);
	}
	
	protected boolean waitForText(CharSequence text) {
		return waitForText(text, Timeouts.LONG);
	}
	
	protected boolean waitForText(CharSequence text, double timeout) {
		final long wait = Math.min(100, (long)(timeout * 100));
		final long end = System.currentTimeMillis() + (long)(timeout * 1000);
		
		boolean found = false;
		while (System.currentTimeMillis() < end &&
			   !(found |= !findViewsOrEmpty(containsText(text)).isEmpty()))
			SystemClock.sleep(wait);
		
		return found;
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
	
	protected void assertTextNotShown(CharSequence text) {
		assertTextNotShown(null, text);
	}
	
	protected void assertTextNotShown(String msg, CharSequence text) {
		assertTextNotShown(msg, text, Timeouts.LONG);
	}
	
	protected void assertTextNotShown(CharSequence text, double timeout) {
		assertTextNotShown(null, text, timeout);
	}
	
	protected void assertTextNotShown(String msg, CharSequence text, double timeout) {
		if (msg == null)
			msg = String.format("%s still shown after %f seconds", text, timeout);
		
		assertFalse(msg, waitForText(text, timeout));
	}
	
	protected Intent assertActivityFinished(Activity a) {
		return assertActivityFinished(a, null);
	}
	
	protected Intent assertActivityFinished(String msg, Activity a) {
		return assertActivityFinished(msg, a, null);
	}
	
	protected Intent assertActivityFinished(Activity a, Integer resultCode) {
		return assertActivityFinished(a, resultCode, Timeouts.LONG);
	}
	
	protected Intent assertActivityFinished(String msg, Activity a, Integer resultCode) {
		return assertActivityFinished(null, a, resultCode, Timeouts.LONG);
	}
	
	protected Intent assertActivityFinished(Activity a, Integer resultCode, double timeout) {
		return assertActivityFinished(null, a, resultCode, timeout);
	}
	
	protected Intent assertActivityFinished(String msg, Activity a, Integer resultCode, double timeout) {
		if (!waitForActivityToFinish(a, timeout)) {
			if (msg == null)
				msg = String.format("%s didn't finish after %f seconds.", a, timeout);
			fail(msg);
		}
		
		try {
			if (resultCode != null) {
				Field mResultCode = Activity.class.getDeclaredField("mResultCode");
				mResultCode.setAccessible(true);									
				assertEquals(a.toString() + " finished, but with wrong result code", (Integer)mResultCode.get(a), resultCode);
			}
			
			Field mResultData = Activity.class.getDeclaredField("mResultData");
			mResultData.setAccessible(true);
			return (Intent)mResultData.get(a);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
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
	
	protected Solo getSolo() {
		return m_Solo;
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
	
	protected void clickBack() {
		m_Solo.goBack();
	}
	
	protected void clickActionBarHome() {
		m_Solo.clickOnActionBarHomeButton();
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
	
	protected void clickText(String text){
		m_Solo.clickOnText(text);
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
	
	private static abstract class Predicate<T> {
		abstract boolean test(T specimen);
	}
	
	private static abstract class TwoParamPredicate<A, B> {
		abstract boolean test(A a, B b);
	}
	
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
		
		void filter(Predicate<T> predicate) {
			Iterator<T> iter = views.iterator();
			while (iter.hasNext()) {
				if (!predicate.test(iter.next()))
					iter.remove();
			}
		}
		
		<A> void filter(final CombinationMatch<A> match, final TwoParamPredicate<A, T> test) {
			filter(new Predicate<T>() {
				@Override
				boolean test(T specimen) {
					return match.matches(test, specimen);
				}
			});
		}
	}
	
	protected <T extends View> T findView(FindViewResult<T> pattern) {
		return findViews(pattern).get(0);
	}
	
	protected <T extends View> List<T> findViews(FindViewResult<T> pattern) {
		assertNotNull("Tried to find views with null pattern.", pattern);
		assertFalse(String.format("Failed to find any %s", pattern.description), pattern.views.isEmpty());
		return pattern.views;
	}
	
	protected <T extends View> List<T> findViewsOrEmpty(FindViewResult<T> pattern) {
		assertNotNull("Tried to find views with null pattern.", pattern);
		return pattern.views;
	}
	
	private List<View> getTopWindowRootView(WindowManager wm) {
		try {
			if (wm.getClass().getName().equals("android.view.Window$LocalWindowManager")) {
				Field f = wm.getClass().getSuperclass().getDeclaredField("mWindowManager");
				f.setAccessible(true);
				wm = (WindowManager)f.get(wm);
			}
			
			Field rootsField = wm.getClass().getDeclaredField("mRoots");
			rootsField.setAccessible(true);
			Object[] roots = (Object[])rootsField.get(wm);
			Class<?> viewRootImplClass = roots[0].getClass();
			
			Field attrsField = viewRootImplClass.getDeclaredField("mWindowAttributes");
			attrsField.setAccessible(true);
			Field viewField = viewRootImplClass.getDeclaredField("mView");
			viewField.setAccessible(true);
			LinkedList<View> rootViews = new LinkedList<View>();
			for (int top = roots.length - 1; top >= 0; top--) {
				rootViews.add((View)viewField.get(roots[top]));
				
				WindowManager.LayoutParams lp = (WindowManager.LayoutParams)attrsField.get(roots[top]);
				if (lp.type <= WindowManager.LayoutParams.LAST_APPLICATION_WINDOW ||
					lp.type == WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
					break;
			}
			
			return rootViews;
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}		
	}
	
	private void walkTree(List<View> list, View root) {
		list.add(root);
		if (root instanceof ViewGroup) {
			ViewGroup vg = (ViewGroup)root;
			for (int i = 0; i < vg.getChildCount(); i++)
				walkTree(list, vg.getChildAt(i));
		}
	}
	
	protected FindViewResult<View> all() {
		FindViewResult<View> result = new FindViewResult<View>();
		result.description = "views";
		getInstrumentation().waitForIdleSync();
		for (View root : getTopWindowRootView(getCurrentActivity().getWindowManager()))
			walkTree(result.views, root);
		return result;
	}
	
	protected FindViewResult<View> withId(int id) {
		return withIds(Arrays.asList(id));
	}
	
	protected FindViewResult<View> withIds(List<Integer> ids) {
		if (ids == null || ids.isEmpty())
			fail("Tried to search on null or empty id list.");
		
		getInstrumentation().waitForIdleSync();
		
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
	
	protected FindViewResult<TextView> exactText(CharSequence text, boolean includeHint) {
		assertNotNull("Tried to search on null exact text.", text);
		
		return matchesRegex(String.format("\\A%s\\Z", Pattern.quote(text.toString())), includeHint);
	}
	
	protected FindViewResult<TextView> exactText(CharSequence text) {
		return exactText(text, false);
	}
	
	protected FindViewResult<TextView> containsText(CharSequence text, boolean includeHint) {
		assertNotNull("Tried to search on null text.", text);
		
		return matchesRegex(Pattern.quote(text.toString()), includeHint);
	}
	
	protected FindViewResult<TextView> containsText(CharSequence text) {
		return containsText(text, false);
	}
	
	protected FindViewResult<TextView> matchesRegex(CharSequence regex, final boolean includeHint) {
		assertNotNull("Tried to search on null regex.", regex);
		final Pattern p = Pattern.compile(regex.toString());
		
		FindViewResult<TextView> result = isTextView(all());
		
		result.description = String.format("%s that match%s '%s'", result.description, includeHint ? " with hint" : "", regex);
		result.filter(new Predicate<TextView>() { 
			@Override
			boolean test(TextView specimen) {
				return (specimen.getText() != null && p.matcher(specimen.getText()).matches()) ||
					   (includeHint && specimen.getHint() != null && p.matcher(specimen.getHint()).matches());
			} 
		});
		
		return result;
	}
	
	protected FindViewResult<TextView> isTextView(FindViewResult<? extends View> in) {
		return isType(in, TextView.class);
	}
	
	protected FindViewResult<EditText> isEditText(FindViewResult<? extends View> in) {
		return isType(in, EditText.class);
	}
	
	protected FindViewResult<Button> isButton(FindViewResult<? extends View> in) {
		return isType(in, Button.class);
	}
	
	
	protected <T extends View, R extends View> FindViewResult<R> isType(FindViewResult<T> result, final Class<R> type) {
		result.description = String.format("%s that are %ss", result.description, type.getSimpleName());
		
		result.filter(new Predicate<T>() { boolean test(T specimen) { return type.isAssignableFrom(specimen.getClass()); } });
		
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
	
	private class RectPredicate<T extends View, R extends View> extends TwoParamPredicate<T, R> {
		private TwoParamPredicate<Rect, Rect> m_Test;
		
		public RectPredicate(TwoParamPredicate<Rect, Rect> test) {
			m_Test = test;
		}
		
		@Override
		boolean test(T a, R b) {
			int xy[] = new int[2];
			a.getLocationOnScreen(xy);
			Rect ar = new Rect(xy[0], xy[1], a.getWidth() + xy[0], a.getHeight() + xy[1]);
			b.getLocationOnScreen(xy);
			Rect br = new Rect(xy[0], xy[1], b.getWidth() + xy[0], b.getHeight() + xy[1]);
			
			return m_Test.test(ar, br);
		}
	}
	
	private <T extends View, R extends View> FindViewResult<R> coveredBy_internal(CombinationMatch<T> covers, FindViewResult<R> result, final boolean over) {
		String desc;
		if (over)
			desc = "cover";
		else
			desc = "are covered by";
		result.description = String.format("%s that %s %s", result.description, desc, covers.getDescription());
		
		TwoParamPredicate<T, R> test = new TwoParamPredicate<T, R>() {
			private RectPredicate<T, R> m_Test = new RectPredicate<T, R>(new TwoParamPredicate<Rect, Rect>() { @Override boolean test(Rect a, Rect b) { return Rect.intersects(a, b); } });
			@Override
			boolean test(T a, R b) {
				return (a.getVisibility() == View.VISIBLE &&
						b.getVisibility() == View.VISIBLE &&
						m_Test.test(a, b) &&
						(over && isInFrontOf(a, b)) ||
						(!over && isInFrontOf(b, a)));
			}
		};
		
		result.filter(covers, test);
		
		return result;
	}
	
	protected <T extends View> FindViewResult<T> coveredBy(CombinationMatch<? extends View> covers, FindViewResult<T> result) {
		return coveredBy_internal(covers, result, false);
	}
	
	protected <T extends View> FindViewResult<T> covers(CombinationMatch<? extends View> under, FindViewResult<T> result) {
		return coveredBy_internal(under, result, true);
	}
	
	protected <T extends View, R extends View> FindViewResult<R> toLeftOf(CombinationMatch<T> anchor, FindViewResult<R> result) {
		result.description = String.format("%s to the left of %s", result.description, anchor.getDescription());
		
		TwoParamPredicate<Rect, Rect> test = new TwoParamPredicate<Rect, Rect>() { @Override boolean test(Rect a, Rect b) { return a.right <= b.left; } };
		
		result.filter(anchor, new RectPredicate<T, R>(test));
		
		return result;
	}
	
	protected <T extends View, R extends View> FindViewResult<R> toRightOf(CombinationMatch<T> anchor, FindViewResult<R> result) {
		result.description = String.format("%s to the right of %s", result.description, anchor.getDescription());
		
		TwoParamPredicate<Rect, Rect> test = new TwoParamPredicate<Rect, Rect>() { @Override boolean test(Rect a, Rect b) { return a.left >= b.right; } };
		
		result.filter(anchor, new RectPredicate<T, R>(test));
		
		return result;
	}
	
	protected <T extends View, R extends View> FindViewResult<R> above(CombinationMatch<T> anchor, FindViewResult<R> result) {
		result.description = String.format("%s above %s", result.description, anchor.getDescription());
		
		TwoParamPredicate<Rect, Rect> test = new TwoParamPredicate<Rect, Rect>() { @Override boolean test(Rect a, Rect b) { return a.bottom <= b.top; } };
		
		result.filter(anchor, new RectPredicate<T, R>(test));
		
		return result;
	}
	
	protected <T extends View, R extends View> FindViewResult<R> below(CombinationMatch<T> anchor, FindViewResult<R> result) {
		result.description = String.format("%s below %s", result.description, anchor.getDescription());
		
		TwoParamPredicate<Rect, Rect> test = new TwoParamPredicate<Rect, Rect>() { @Override boolean test(Rect a, Rect b) { return a.top >= b.bottom; } };
		
		result.filter(anchor, new RectPredicate<T, R>(test));
		
		return result;
	}
	
	// combination matching
	private static class CombinationMatch<T> {
		private List<T> m_Potentials;
		private String m_Description;
		private int m_MinMatches = 0;
		private int m_MaxMatches = Integer.MAX_VALUE;
		
		public CombinationMatch(List<T> potentials, int min, int max, String description) {
			if (potentials.size() == 0)
				throw new IllegalArgumentException("No potential matches.");
			if (min < 1)
				throw new IllegalArgumentException("Must match at least one potential.");
			if (max < min)
				throw new IllegalArgumentException("Maximum matches must be at least the number of minimum matches.");
			m_Potentials = potentials;
			m_MinMatches = min;
			m_MaxMatches = max;
			m_Description = description;
		}
		
		public <S> boolean matches(TwoParamPredicate<T, S> test, S specimen) {
			int matches = 0;
			Iterator<T> iter = m_Potentials.iterator();
			while (iter.hasNext() && matches <= m_MaxMatches) {
				if (test.test(iter.next(), specimen))
					matches++;
			}
			
			return (matches >= m_MinMatches && matches <= m_MaxMatches);
		}
		
		public String getDescription() {
			StringBuilder sb = new StringBuilder();
			if (m_MinMatches == m_MaxMatches && m_MinMatches == m_Potentials.size()) {
				sb.append("all ");
			}
			else if (m_MinMatches == 1 && m_MaxMatches == m_Potentials.size()) {
				sb.append("any ");
			} else {
				if (m_MinMatches > 1) {
					sb.append("at least ");
					sb.append(m_MinMatches);
					sb.append(" ");
				}
				if (m_MaxMatches < Integer.MAX_VALUE) {
					if (sb.length() > 0)
						sb.append(" but ");
					sb.append("at most ");
					sb.append(m_MaxMatches);
					sb.append(" ");
				}
			}
			sb.append("of ");
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
