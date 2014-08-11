package com.example.imservice.model;

import java.util.ArrayList;

public class Contact extends Object{

    public static class ContactData {
        public String value;
        public String label;
    }

    public long cid;
    public String displayName;
    public long updatedTimestamp;
    public ArrayList<ContactData> phoneNumbers = new ArrayList<ContactData>();

    public boolean equals(Object other) {
        if (!(other instanceof Contact)) return false;
        Contact o = (Contact)other;
        return o.cid == this.cid;
    }
}

