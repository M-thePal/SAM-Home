package ir.samhome;

import java.util.Calendar;

// Gregorian to Solar Hijri (Jalali) dates, for the date line on the home screen.
final class Jalali {

    private static final String[] MONTHS = {"فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"};
    // Indexed by Calendar.DAY_OF_WEEK (Sunday = 1).
    private static final String[] WEEKDAYS = {"", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه",
            "پنجشنبه", "جمعه", "شنبه"};

    private Jalali() {
    }

    static int[] fromGregorian(int gy, int gm, int gd) {
        int[] daysBeforeMonth = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        int gy2 = gm > 2 ? gy + 1 : gy;
        int days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
                + gd + daysBeforeMonth[gm - 1];
        int jy = -1595 + 33 * (days / 12053);
        days %= 12053;
        jy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int jm = days < 186 ? 1 + days / 31 : 7 + (days - 186) / 30;
        int jd = days < 186 ? 1 + days % 31 : 1 + (days - 186) % 30;
        return new int[]{jy, jm, jd};
    }

    // {"جمعه", "۸ آبان", "۱۴۰۵"}
    static String[] today(Calendar c) {
        int[] j = fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        return new String[]{WEEKDAYS[c.get(Calendar.DAY_OF_WEEK)], digits(j[2]) + " " + MONTHS[j[1] - 1], digits(j[0])};
    }

    static String digits(int n) {
        char[] s = String.valueOf(n).toCharArray();
        for (int i = 0; i < s.length; i++) if (s[i] >= '0' && s[i] <= '9') s[i] = (char) ('۰' + s[i] - '0');
        return new String(s);
    }
}
