package com.beetle.bauhinia.model;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;

public class ContactDB {

    public static interface ContactObserver {
        public void OnExternalChange();
    }

    private final String TAG = "beetle";
    private static ContactDB instance = new ContactDB();

    public static ContactDB getInstance() {
        return instance;
    }

    private ArrayList<ContactObserver> observers = new ArrayList<ContactObserver>();
    private ArrayList<Contact> contacts = new ArrayList<Contact>();
    private ContentResolver contentResolver;

    public void setContentResolver(ContentResolver resolver) {
        this.contentResolver = resolver;
    }

    public ArrayList<Contact> getContacts() {
        return contacts;
    }

    public ArrayList<Contact> copyContacts() {
        ArrayList<Contact> array = new ArrayList<Contact>();
        for (int i = 0; i < contacts.size(); i++) {
            array.add(new Contact(contacts.get(i)));
        }
        return array;
    }

    public void addObserver(ContactObserver ob) {
        if (observers.contains(ob)) {
            return;
        }
        observers.add(ob);
    }

    public void removeObserver(ContactObserver ob) {
        observers.remove(ob);
    }

    private ContentObserver contentObserver;
    private Handler handler;
    public void monitorConctat(Context appContext) {
        this.handler = new Handler() {

        };
        this.contentObserver = new ContentObserver(this.handler) {
            @Override
            public void onChange(boolean selfChange) {
                Log.i(TAG, "contact changed");
                ContactDB.this.refreshContacts();
            }
        };
        appContext.getContentResolver().registerContentObserver ( ContactsContract.Contacts.CONTENT_VCARD_URI, false, contentObserver);
    }

    public void loadContacts() {
        this.contacts = new ArrayList<Contact>();
        readContacts(this.contacts);
        readData();
    }

    public void refreshContacts() {
        boolean changed = false;
        ArrayList<Contact> newContacts = new ArrayList<Contact>();
        readContacts(newContacts);


        ArrayList<Contact> oldContacts = contacts;
        if (newContacts.size() != oldContacts.size()) {
            changed = true;
        }

        ArrayList<Contact> addedContacts = new ArrayList<Contact>(newContacts);
        addedContacts.removeAll(oldContacts);

        ArrayList<Contact> updatedContacts = new ArrayList<Contact>(newContacts);
        updatedContacts.removeAll(addedContacts);

        ArrayList<Contact> result = new ArrayList<Contact>();

        for (int i = 0; i < addedContacts.size(); i++) {
            Contact c = addedContacts.get(i);
            loadContactData(c);
            result.add(c);
            changed = true;
        }

        for (int i = 0; i < updatedContacts.size(); i++) {
            Contact c1 = updatedContacts.get(i);
            Contact c2 = findContact(c1.cid);
            if (c1.updatedTimestamp > 0 && c1.updatedTimestamp == c2.updatedTimestamp) {
                result.add(c2);
                continue;
            }
            loadContactData(c1);
            result.add(c1);
            changed = true;
        }

        if (changed) {
            this.contacts = result;
            for (int i = 0; i < observers.size(); i++) {
                ContactObserver ob = observers.get(i);
                ob.OnExternalChange();
            }
        }
    }

    public Contact loadContact(PhoneNumber number) {
        for (int i = 0; i < contacts.size(); i++) {
            Contact c = contacts.get(i);
            ArrayList<Contact.ContactData> numbers = c.phoneNumbers;
            for (int j = 0; j < numbers.size(); j++) {
                Contact.ContactData data = numbers.get(j);
                PhoneNumber n = new PhoneNumber();
                if (!n.parsePhoneNumber(data.value)) {
                    continue;
                }
                if (n.equals(number)) {
                    return c;
                }
            }
        }
        return null;
    }

    private void readContacts(ArrayList<Contact> contacts) {
        if (Build.VERSION.SDK_INT >= 18) {
            readContactsAfterV18(contacts);
        } else {
            readContactsBeforeV18(contacts);
        }
    }

    private void readContactsAfterV18(ArrayList<Contact> contacts) {
        String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
                ContactsContract.Contacts.IN_VISIBLE_GROUP,
        };

        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                null);

        if (null == cursor) {
            Log.i(TAG, "cursor is null");
            return;
        }


        int index1 = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        int index2 = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        int index3 = cursor.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP);
        int index4 = cursor.getColumnIndex(ContactsContract.Contacts.IN_VISIBLE_GROUP);
        while (cursor.moveToNext()) {
            Contact c = new Contact();
            long id = cursor.getLong(index1);
            String name = cursor.getString(index2);
            int isVisible = cursor.getInt(index4);

            long updatedTimestamp = cursor.getLong(index3);
            Log.i(TAG, "contact id:" + id + " name:" + name + " visisble:" +isVisible);
            c.cid = id;
            c.displayName = name;
            c.updatedTimestamp = updatedTimestamp;
            contacts.add(c);
        }

        cursor.close();
    }

    private void readContactsBeforeV18(ArrayList<Contact> contacts) {
        String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
        };

        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                null);

        if (null == cursor) {
            Log.i(TAG, "cursor is null");
            return;
        }


        int index1 = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        int index2 = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        while (cursor.moveToNext()) {
            Contact c = new Contact();
            long id = cursor.getLong(index1);
            String name = cursor.getString(index2);
            Log.i(TAG, ""+id + " " + name);
            c.cid = id;
            c.displayName = name;
            c.updatedTimestamp = 0;
            contacts.add(c);
        }

        cursor.close();
    }

    private void readRaw() {
        String[] mProjection = {
                ContactsContract.RawContacts._ID,
                ContactsContract.RawContacts.ACCOUNT_NAME,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.DELETED,
                ContactsContract.RawContacts.CONTACT_ID,
        };

        Cursor cursor = contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                mProjection,
                null,
                null,
                null);

        if (null == cursor) {
            Log.i(TAG, "cursor is null");
            return;
        }

        int index1 = cursor.getColumnIndex(ContactsContract.RawContacts._ID);
        int index2 = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME);
        int index3 = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE);
        int index4 = cursor.getColumnIndex(ContactsContract.RawContacts.DELETED);
        int index5 = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(index1);
            String name = cursor.getString(index2);
            String type = cursor.getString(index3);
            int deleted = cursor.getInt(index4);
            long cid = cursor.getLong(index5);
            Log.i(TAG, ""+id + " " + name + " " + type + " " + deleted + " " + cid);
        }
        cursor.close();
    }

    private Contact findContact(long cid) {
        for (int i = 0; i < contacts.size(); i++) {
            Contact c = contacts.get(i);
            if (c.cid == cid) {
                return c;
            }
        }
        return null;
    }

    private void loadContactData(Contact c) {
        String[] projection = {
                ContactsContract.Data._ID,
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.IS_PRIMARY,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.DATA4,
                ContactsContract.Data.DATA5,
                ContactsContract.Data.DATA6,
                ContactsContract.Data.DATA7,
                ContactsContract.Data.DATA8,
                ContactsContract.Data.DATA9,
                ContactsContract.Data.DATA10,
                ContactsContract.Data.DATA11,
                ContactsContract.Data.DATA12,
                ContactsContract.Data.DATA13,
                ContactsContract.Data.DATA14,
                ContactsContract.Data.DATA15,
        };
        final int ID_INDEX = 0;
        final int CONTACT_ID_INDEX = 1;
        final int RAW_CONTACT_ID_INDEX = 2;
        final int MIMETYPE_INDEX = 3;
        final int IS_PRIMARY_INDEX = 4;

        String selection = ContactsContract.Data.CONTACT_ID + " = ?";
        String[] selectionArgs = {""+c.cid};
        Cursor cursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null);

        if (null == cursor) {
            Log.i(TAG, "cursor is null");
            return;
        }
        while (cursor.moveToNext()) {
            long id = cursor.getLong(ID_INDEX);
            long cid = cursor.getLong(CONTACT_ID_INDEX);
            long rid = cursor.getLong(RAW_CONTACT_ID_INDEX);
            int isPrimary = cursor.getInt(IS_PRIMARY_INDEX);
            String type = cursor.getString(MIMETYPE_INDEX);
            if (type.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                int index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(index);
                index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
                String label = cursor.getString(index);

                Contact.ContactData data = new Contact.ContactData();
                data.value = number;
                data.label = label;
                c.phoneNumbers.add(data);
                Log.i(TAG, "contact:" + c.displayName + " cid:" + c.cid + " phone:" + number + " lable:" + label);
            }
        }

        cursor.close();
    }

    private void readData() {
        String[] projection = {
                ContactsContract.Data._ID,
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.RAW_CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.Data.IS_PRIMARY,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA2,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.DATA4,
                ContactsContract.Data.DATA5,
                ContactsContract.Data.DATA6,
                ContactsContract.Data.DATA7,
                ContactsContract.Data.DATA8,
                ContactsContract.Data.DATA9,
                ContactsContract.Data.DATA10,
                ContactsContract.Data.DATA11,
                ContactsContract.Data.DATA12,
                ContactsContract.Data.DATA13,
                ContactsContract.Data.DATA14,
                ContactsContract.Data.DATA15,
        };
        final int ID_INDEX = 0;
        final int CONTACT_ID_INDEX = 1;
        final int RAW_CONTACT_ID_INDEX = 2;
        final int MIMETYPE_INDEX = 3;
        final int IS_PRIMARY_INDEX = 4;

        Cursor cursor = contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                null,
                null,
                null);

        if (null == cursor) {
            Log.i(TAG, "cursor is null");
            return;
        }

        while (cursor.moveToNext()) {
            long id = cursor.getLong(ID_INDEX);
            long cid = cursor.getLong(CONTACT_ID_INDEX);
            long rid = cursor.getLong(RAW_CONTACT_ID_INDEX);
            int isPrimary = cursor.getInt(IS_PRIMARY_INDEX);
            String type = cursor.getString(MIMETYPE_INDEX);
            if (type.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                int index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String number = cursor.getString(index);
                index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
                String label = cursor.getString(index);

                Contact.ContactData data = new Contact.ContactData();
                data.value = number;
                data.label = label;
                Contact c = findContact(cid);
                if (c == null) {
                    Log.w(TAG, "can't find contact:" + cid);
                    continue;
                }
                c.phoneNumbers.add(data);
                Log.i(TAG, "contact:" + c.displayName + " cid:" + c.cid + " phone:" + number + " lable:" + label);
            }
        }

        cursor.close();
    }
}
