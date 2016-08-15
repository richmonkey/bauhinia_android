package com.beetle.bauhinia.model;

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

    public Contact() {

    }

    public Contact(Contact c) {
        this.cid = c.cid;
        this.displayName = c.displayName;
        this.updatedTimestamp = c.updatedTimestamp;
        if (phoneNumbers == null) {
            return;
        }
        this.phoneNumbers = new ArrayList<ContactData>();
        for (int i = 0; i < c.phoneNumbers.size(); i++) {
            ContactData d = c.phoneNumbers.get(i);
            ContactData data = new ContactData();
            data.value = d.value;
            data.label = d.label;
            this.phoneNumbers.add(data);
        }
    }
}

