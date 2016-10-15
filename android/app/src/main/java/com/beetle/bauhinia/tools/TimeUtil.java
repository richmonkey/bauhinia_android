package com.beetle.bauhinia.tools;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by houxh on 16/10/15.
 */

public class TimeUtil {

    public static String formatTimeBase(long ts) {
        String s = "";
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis((long)(ts) * 1000);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String weeks[] = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        if (isToday(ts)) {
            s = String.format("%02d:%02d", hour, minute);
        } else if (isYesterday(ts)) {
            s = String.format("昨天 %02d:%02d", hour, minute);
        } else if (isInWeek(ts)) {
            s = String.format("%s %02d:%02d", weeks[dayOfWeek - 1], hour, minute);
        } else if (isInYear(ts)) {
            s = String.format("%02d-%02d %02d:%02d", month+1, dayOfMonth, hour, minute);
        } else {
            s = String.format("%d-%02d-%02d %02d:%02d", year, month+1, dayOfMonth, hour, minute);
        }
        return s;
    }

    private static boolean isToday(long ts) {
        int now = now();
        return isSameDay(now, ts);
    }

    private static boolean isYesterday(long ts) {
        int now = now();
        int yesterday = now - 24*60*60;
        return isSameDay(ts, yesterday);
    }

    private static boolean isInWeek(long ts) {
        int now = now();
        //6天前
        long day6 = now - 6*24*60*60;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(day6 * 1000);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        int zero = (int)(cal.getTimeInMillis()/1000);
        return (ts >= zero);
    }

    private static boolean isInYear(long ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts*1000);
        int year = cal.get(Calendar.YEAR);

        cal.setTime(new Date());
        int y = cal.get(Calendar.YEAR);

        return (year == y);
    }

    private static boolean isSameDay(long ts1, long ts2) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts1 * 1000);
        int year1 = cal.get(Calendar.YEAR);
        int month1 = cal.get(Calendar.MONTH);
        int day1 = cal.get(Calendar.DAY_OF_MONTH);


        cal.setTimeInMillis(ts2 * 1000);
        int year2 = cal.get(Calendar.YEAR);
        int month2 = cal.get(Calendar.MONTH);
        int day2 = cal.get(Calendar.DAY_OF_MONTH);

        return ((year1==year2) && (month1==month2) && (day1==day2));
    }

    private static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }
}
