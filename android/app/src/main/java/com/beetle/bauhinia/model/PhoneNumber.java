package com.beetle.bauhinia.model;

/**
 * Created by houxh on 14-8-10.
 */
public class PhoneNumber {
    private String zone;
    private String number;

    public String getZone() {
        return zone;
    }

    public String getNumber() {
        return number;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PhoneNumber)) {
            return false;
        }
        PhoneNumber o = (PhoneNumber)other;
        if (o.getZone().equals(this.getZone()) && o.getNumber().equals(this.getNumber())) {
            return true;
        }
        return false;
    }

    public PhoneNumber(String zone, String number) {
        this.zone = zone;
        this.number = number;
    }
    public PhoneNumber(String zoneNumber) {
        String[] t = zoneNumber.split("_");
        if (t.length != 2) {
            return;
        }
        zone = t[0];
        number = t[1];
    }
    public PhoneNumber() {

    }

    public boolean isValid() {
        if (zone == null || zone.length() == 0) return false;
        if (number == null || number.length() == 0) return false;

        return true;
    }

    public String getZoneNumber() {
        return zone + "_" + number;
    }

    public boolean parsePhoneNumber(String phoneNumber) {
        char[] dst = new char[64];
        int index = 0;

        for (int i = 0; i < phoneNumber.length(); i++) {
            char c = phoneNumber.charAt(i);
            if (Character.isDigit(c)) {
                dst[index++] = c;
            }
            if (index >= 64) {
                return false;
            }
        }
        if (index > 11) {
            this.number = new String(dst, index-11, 11);
            this.zone = new String(dst, 0, index-11);
            return true;
        } else if (index == 11) {
            this.number = new String(dst, 0, index);
            this.zone = "86";
            return true;
        } else {
            return false;
        }
    }
}
