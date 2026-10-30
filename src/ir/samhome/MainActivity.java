package ir.samhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// A warm, calm home screen for the SAM TV (MStar, Android 6), made for people who watch antenna
// TV and one HDMI device and rarely open apps. This TV has no Android tv_input service: inputs
// are switched with the MStar player's own START_TV_PLAYER intent, the one its Source menu sends.
public class MainActivity extends Activity {

    private static final String TV_PLAYER = "com.mstar.tv.tvplayer.ui";
    private static final String START_TV_PLAYER = "com.mstar.android.intent.action.START_TV_PLAYER";
    private static final String SOURCE_MENU = "com.talent.intent.action.TV_SOURCE_MENU";
    // MStar EnumInputSource values, as the Source menu uses them.
    private static final int SOURCE_DTV = 28;
    private static final int SOURCE_HDMI1 = 23;
    private static final int APP_COLUMNS = 6;

    // Factory and test menus can change panel settings for good, so they never get a tile.
    private static final Set<String> HIDDEN = new HashSet<>(Arrays.asList(
            TV_PLAYER, "mstar.factorymenu.ui", "com.talent.TLFactory", "com.example.tlkeytest",
            "com.rtk.TSBFactoryMode", "tvtests.com.mstar.android.tv", "com.tl.mtlfacnvm",
            "com.fjq.serialport.app", "com.realtek.rtkbt.AutoPairUIDemo"));

    // Warm palette: cocoa background, cream ink, terracotta and olive cards.
    private static final int BG_TOP = 0xFF241B17;
    private static final int BG_BOTTOM = 0xFF17110F;
    private static final int CREAM = 0xFFF6EDE4;
    private static final int SAND = 0xFFC9A98C;
    private static final int CHIP = 0xFF2E2420;
    private static final int CHIP_INK = 0xFFD8C3B2;

    // Vazirmatn, bundled in assets/fonts (OFL).
    private Typeface regular, medium;
    private View tvCard, hdmiCard, appsChip;
    private TextView greetingView, weekdayView, dayMonthView, yearView;
    private FrameLayout appsPanel;
    private GridLayout appGrid;
    private List<String> shownApps = new ArrayList<>();

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { updateDate(); }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        regular = Typeface.createFromAsset(getAssets(), "fonts/Vazirmatn-Regular.ttf");
        medium = Typeface.createFromAsset(getAssets(), "fonts/Vazirmatn-Medium.ttf");

        FrameLayout root = new FrameLayout(this);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BG_TOP, BG_BOTTOM}));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(64), dp(40), dp(64), dp(36));
        page.setClipChildren(false);
        page.setClipToPadding(false);
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        page.addView(header());

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        cards.setClipChildren(false);
        tvCard = card(R.drawable.ic_tv, "تلویزیون", "شبکه‌های دیجیتال",
                0xFFD9734E, CREAM, 0xFF8E3B1E, 0xFFFBE3D6, SOURCE_DTV, "tv");
        hdmiCard = card(R.drawable.ic_hdmi, "HDMI", "ورودی ۱",
                0xFF5E7A5A, 0xFFE4EDDF, 0xFF33492F, 0xFFDCE7D7, SOURCE_HDMI1, "hdmi");
        cards.addView(tvCard, new LinearLayout.LayoutParams(0, dp(220), 1f));
        cards.addView(new View(this), new LinearLayout.LayoutParams(dp(28), 1));
        cards.addView(hdmiCard, new LinearLayout.LayoutParams(0, dp(220), 1f));
        page.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
        page.addView(cards, new LinearLayout.LayoutParams(-1, -2));
        page.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1.2f));

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setClipChildren(false);
        chips.addView(chip(R.drawable.ic_list, "کانال‌ها",
                new Intent("com.mstar.tvsetting.ui.intent.action.ChannelActivity")));
        chips.addView(chip(R.drawable.ic_guide, "جدول پخش", new Intent("com.talent.intent.action.epg")));
        chips.addView(chip(R.drawable.ic_input, "ورودی‌ها",
                new Intent(SOURCE_MENU)));
        chips.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        appsChip = chip(R.drawable.ic_apps, "برنامه‌ها", null);
        chips.addView(appsChip);
        chips.addView(chip(R.drawable.ic_tune, "تنظیمات", new Intent(Settings.ACTION_SETTINGS)));
        page.addView(chips);

        appsPanel = buildAppsPanel();
        appsPanel.setVisibility(View.GONE);
        root.addView(appsPanel, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
        focusLastUsed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDate();
        registerReceiver(tick, new IntentFilter(Intent.ACTION_TIME_TICK));
        if (appsPanel.getVisibility() == View.VISIBLE) loadApps();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(tick);
    }

    // Home is pressed while this screen is open: close the apps and go back to the last input.
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (appsPanel != null) {
            appsPanel.setVisibility(View.GONE);
            focusLastUsed();
        }
    }

    // Back closes the apps; a home screen has nowhere else to go back to.
    @Override
    public void onBackPressed() {
        if (appsPanel.getVisibility() == View.VISIBLE) {
            appsPanel.setVisibility(View.GONE);
            appsChip.requestFocus();
        }
    }

    // ---- Header: greeting and clock on the right, the Jalali date on the left ----

    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        greetingView = text("", 20, SAND);
        greetingView.setGravity(Gravity.START);
        clock.addView(greetingView);
        TextClock time = new TextClock(this);
        time.setFormat24Hour("HH:mm");
        time.setFormat12Hour("HH:mm");
        time.setTextColor(CREAM);
        time.setTypeface(regular);
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 60);
        time.setIncludeFontPadding(false);
        clock.addView(time);
        h.addView(clock, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout date = new LinearLayout(this);
        date.setOrientation(LinearLayout.VERTICAL);
        date.setGravity(Gravity.END);
        weekdayView = text("", 18, SAND);
        dayMonthView = text("", 30, CREAM);
        dayMonthView.setTypeface(medium);
        yearView = text("", 15, SAND);
        date.addView(weekdayView);
        date.addView(dayMonthView);
        date.addView(yearView);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(-2, -2);
        dateLp.topMargin = dp(10);
        h.addView(date, dateLp);
        return h;
    }

    private void updateDate() {
        Calendar c = Calendar.getInstance();
        String[] d = Jalali.today(c);
        weekdayView.setText(d[0]);
        dayMonthView.setText(d[1]);
        yearView.setText(d[2]);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        greetingView.setText(hour >= 4 && hour < 12 ? "صبح بخیر" : hour >= 12 && hour < 17 ? "ظهر بخیر"
                : hour >= 17 && hour < 21 ? "عصر بخیر" : "شب بخیر");
    }

    // ---- The two big cards: antenna TV and HDMI ----

    private View card(int icon, String title, String subtitle, int fill, int disc, int iconInk,
                      int subInk, final int source, final String key) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPaddingRelative(dp(36), 0, dp(28), 0);
        card.setBackground(ringed(fill, dp(30), dp(7), dp(3)));

        FrameLayout circle = new FrameLayout(this);
        GradientDrawable c = new GradientDrawable();
        c.setShape(GradientDrawable.OVAL);
        c.setColor(disc);
        circle.setBackground(c);
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setImageTintList(ColorStateList.valueOf(iconInk));
        circle.addView(iv, new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.CENTER));
        card.addView(circle, new LinearLayout.LayoutParams(dp(92), dp(92)));

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPaddingRelative(dp(24), 0, 0, 0);
        TextView t = text(title, 38, Color.WHITE);
        t.setTypeface(medium);
        t.setGravity(Gravity.START);
        words.addView(t);
        TextView s = text(subtitle, 17, subInk);
        s.setGravity(Gravity.START);
        words.addView(s);
        card.addView(words);

        focusable(card, 1.03f, new View.OnClickListener() {
            @Override public void onClick(View v) { switchTo(source, key); }
        });
        return card;
    }

    // A filled rounded rect that gets a cream ring a few dp outside it when focused.
    private static StateListDrawable ringed(int fill, int radius, int gap, int ringWidth) {
        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_focused}, ringLayer(fill, radius, gap, ringWidth, CREAM));
        sl.addState(new int[]{}, ringLayer(fill, radius, gap, ringWidth, Color.TRANSPARENT));
        return sl;
    }

    private static LayerDrawable ringLayer(int fill, int radius, int gap, int ringWidth, int ringColor) {
        GradientDrawable ring = new GradientDrawable();
        ring.setCornerRadius(radius + gap);
        ring.setStroke(ringWidth, ringColor);
        GradientDrawable body = new GradientDrawable();
        body.setCornerRadius(radius);
        body.setColor(fill);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{ring, body});
        ld.setLayerInset(1, gap, gap, gap, gap);
        return ld;
    }

    private void switchTo(int source, String key) {
        prefs().edit().putString("last", key).apply();
        Intent i = new Intent(START_TV_PLAYER)
                .putExtra("inputSrc", source)
                .putExtra("inputAntennaType", 0)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 0x2000);
        if (!launch(i, false)) launch(new Intent().setClassName(TV_PLAYER, TV_PLAYER + ".RootActivity"), true);
    }

    // The TV's source menu only switches inputs when it sits on top of the TV player. Opened on its
    // own over this screen it lands in a separate task, and the player never gets the choice.
    // So bring the player up first and put the menu in its task, as the remote's Source key does.
    private void openSourceMenu() {
        Intent player = new Intent().setClassName(TV_PLAYER, TV_PLAYER + ".RootActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivities(new Intent[]{player, new Intent(SOURCE_MENU)});
        } catch (ActivityNotFoundException | SecurityException e) {
            launch(new Intent(SOURCE_MENU), true);
        }
    }

    private void focusLastUsed() {
        ("hdmi".equals(prefs().getString("last", "tv")) ? hdmiCard : tvCard).requestFocus();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("home", MODE_PRIVATE);
    }

    // ---- The pill row below: channels, guide, inputs, and apart from them apps and settings ----

    private View chip(int icon, String label, final Intent intent) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(20), dp(11), dp(20), dp(11));
        chip.setBackground(states(pill(CREAM), pill(CHIP)));

        ColorStateList ink = new ColorStateList(
                new int[][]{{android.R.attr.state_focused}, {}}, new int[]{BG_BOTTOM, CHIP_INK});
        ImageView iv = new ImageView(this);
        iv.setImageResource(icon);
        iv.setImageTintList(ink);
        iv.setDuplicateParentStateEnabled(true);
        chip.addView(iv, new LinearLayout.LayoutParams(dp(20), dp(20)));

        TextView t = text(label, 16, 0);
        t.setTextColor(ink);
        t.setDuplicateParentStateEnabled(true);
        t.setPaddingRelative(dp(9), 0, 0, 0);
        chip.addView(t);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(dp(12));
        chip.setLayoutParams(lp);
        focusable(chip, 1.05f, new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (intent == null) openApps();
                else if (SOURCE_MENU.equals(intent.getAction())) openSourceMenu();
                else launch(new Intent(intent), true);
            }
        });
        return chip;
    }

    private GradientDrawable pill(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(30));
        return d;
    }

    // ---- Apps: a panel over the home screen, built only when opened ----

    private FrameLayout buildAppsPanel() {
        FrameLayout panel = new FrameLayout(this);
        panel.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{BG_TOP, BG_BOTTOM}));
        panel.setClickable(true);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(64), dp(40), dp(64), dp(16));
        col.setClipChildren(false);
        col.setClipToPadding(false);

        TextView title = text("برنامه‌ها", 32, CREAM);
        title.setTypeface(medium);
        title.setGravity(Gravity.START);
        col.addView(title);
        TextView hint = text("برای برگشتن، دکمه‌ی بازگشت را بزنید", 15, SAND);
        hint.setGravity(Gravity.START);
        hint.setPadding(0, dp(2), 0, dp(16));
        col.addView(hint);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(4), dp(8), dp(4), dp(16));
        appGrid = new GridLayout(this);
        appGrid.setColumnCount(APP_COLUMNS);
        appGrid.setClipChildren(false);
        scroll.addView(appGrid);
        col.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        panel.addView(col, new FrameLayout.LayoutParams(-1, -1));
        return panel;
    }

    private void openApps() {
        loadApps();
        appsPanel.setVisibility(View.VISIBLE);
        if (appGrid.getChildCount() > 0) appGrid.getChildAt(0).requestFocus();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> byPackage = new LinkedHashMap<>();
        // Leanback entries first, so a TV app's TV activity wins over its phone one.
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || HIDDEN.contains(pkg) || byPackage.containsKey(pkg)) continue;
                byPackage.put(pkg, ri);
            }
        }

        final Collator collator = Collator.getInstance(new Locale("fa"));
        final Map<ResolveInfo, String> labels = new LinkedHashMap<>();
        for (ResolveInfo ri : byPackage.values()) labels.put(ri, String.valueOf(ri.loadLabel(pm)));
        List<ResolveInfo> apps = new ArrayList<>(byPackage.values());
        Collections.sort(apps, (a, b) -> collator.compare(labels.get(a), labels.get(b)));

        List<String> keys = new ArrayList<>();
        for (ResolveInfo ri : apps) keys.add(ri.activityInfo.packageName + "/" + ri.activityInfo.name);
        if (keys.equals(shownApps)) return;
        shownApps = keys;

        appGrid.removeAllViews();
        for (ResolveInfo ri : apps) appGrid.addView(appTile(ri, labels.get(ri), pm));
    }

    private View appTile(ResolveInfo ri, String label, PackageManager pm) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(12), dp(14), dp(12), dp(10));
        tile.setBackground(ringed(CHIP, dp(18), dp(4), dp(3)));

        ImageView icon = new ImageView(this);
        icon.setImageDrawable(ri.loadIcon(pm));
        tile.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

        TextView t = text(label, 14, CREAM);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setPadding(0, dp(8), 0, 0);
        tile.addView(t);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = dp(128);
        lp.height = dp(118);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        tile.setLayoutParams(lp);

        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
        focusable(tile, 1.06f, new View.OnClickListener() {
            @Override public void onClick(View v) { launch(intent, true); }
        });
        return tile;
    }

    // ---- Shared bits ----

    private void focusable(View v, final float scale, View.OnClickListener onClick) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnClickListener(onClick);
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View view, boolean hasFocus) {
                float s = hasFocus ? scale : 1f;
                view.animate().scaleX(s).scaleY(s).setDuration(120).start();
                view.setElevation(hasFocus ? dp(10) : 0);
            }
        });
    }

    private static StateListDrawable states(Drawable focused, Drawable normal) {
        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_focused}, focused);
        sl.addState(new int[]{}, normal);
        return sl;
    }

    private TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(regular);
        if (color != 0) t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private boolean launch(Intent intent, boolean complain) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            if (complain) Toast.makeText(this, "این بخش باز نشد", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
