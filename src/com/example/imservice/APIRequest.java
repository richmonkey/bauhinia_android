package com.example.imservice;

import android.util.Log;
import com.example.imservice.model.Contact;
import com.example.imservice.model.PhoneNumber;
import com.example.imservice.model.User;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by houxh on 14-8-11.
 */
public class APIRequest {
    private static final String URL = "http://106.186.122.158:5000";

    public static String requestVerifyCode(String zone, String number) {
        String uri = String.format("%s/verify_code?zone=%s&number=%s", URL, zone, number);
        try {
            HttpClient getClient = new DefaultHttpClient();
            HttpGet request = new HttpGet(uri);
            HttpResponse response = getClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK){
                return null;
            }
            int len = (int)response.getEntity().getContentLength();
            byte[] buf = new byte[len];
            InputStream inStream = response.getEntity().getContent();
            int pos = 0;
            while (pos < len) {
                int n = inStream.read(buf, pos, len - pos);
                if (n == -1) {
                    break;
                }
                pos += n;
            }
            inStream.close();
            if (pos != len) {
                return null;
            }
            String txt = new String(buf, "UTF-8");
            JSONObject jsonObject = new JSONObject(txt);
            return jsonObject.getString("code");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Token requestAuthToken(String zone, String number, String code) {
        String uri = String.format("%s/auth/token", URL);
        try {
            HttpClient getClient = new DefaultHttpClient();
            HttpPost request = new HttpPost(uri);
            JSONObject json = new JSONObject();
            json.put("code", code);
            json.put("zone", zone);
            json.put("number", number);
            StringEntity s = new StringEntity(json.toString());
            s.setContentEncoding((Header) new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            request.setEntity(s);

            HttpResponse response = getClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK){
                return null;
            }
            int len = (int)response.getEntity().getContentLength();
            byte[] buf = new byte[len];
            InputStream inStream = response.getEntity().getContent();
            int pos = 0;
            while (pos < len) {
                int n = inStream.read(buf, pos, len - pos);
                if (n == -1) {
                    break;
                }
                pos += n;
            }
            inStream.close();
            if (pos != len) {
                return null;
            }
            String txt = new String(buf, "UTF-8");
            JSONObject jsonObject = new JSONObject(txt);

            Token token = new Token();
            token.uid = jsonObject.getLong("uid");
            token.accessToken = jsonObject.getString("access_token");
            token.refreshToken = jsonObject.getString("refresh_token");
            token.expireTimestamp = now() + jsonObject.getInt("expires_in");
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Token refreshAccessToken(String refreshToken) {
        String uri = String.format("%s/auth/refresh_token", URL);
        try {
            HttpClient getClient = new DefaultHttpClient();
            HttpPost request = new HttpPost(uri);
            JSONObject json = new JSONObject();
            json.put("refresh_token", refreshToken);
            StringEntity s = new StringEntity(json.toString());
            s.setContentEncoding((Header) new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            request.setEntity(s);

            HttpResponse response = getClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK){
                return null;
            }
            int len = (int)response.getEntity().getContentLength();
            byte[] buf = new byte[len];
            InputStream inStream = response.getEntity().getContent();
            int pos = 0;
            while (pos < len) {
                int n = inStream.read(buf, pos, len - pos);
                if (n == -1) {
                    break;
                }
                pos += n;
            }
            inStream.close();
            if (pos != len) {
                return null;
            }
            String txt = new String(buf, "UTF-8");
            JSONObject jsonObject = new JSONObject(txt);

            Token token = new Token();
            token.accessToken = jsonObject.getString("access_token");
            token.refreshToken = jsonObject.getString("refresh_token");
            token.expireTimestamp = now() + jsonObject.getInt("expires_in");
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList<User> requestUsers(ArrayList<Contact> contacts) {
        String uri = String.format("%s/users", URL);
        try {
            HttpClient getClient = new DefaultHttpClient();
            HttpPost request = new HttpPost(uri);
            JSONArray json = new JSONArray();
            HashSet<String> sets = new HashSet<String>();
            for (int i = 0; i < contacts.size(); i++) {
                Contact c = contacts.get(i);
                if (c.phoneNumbers == null)
                    continue;

                for (int j = 0; j < c.phoneNumbers.size(); j++) {
                    Contact.ContactData data = c.phoneNumbers.get(j);
                    PhoneNumber n = new PhoneNumber();
                    if (!n.parsePhoneNumber(data.value)) {
                        continue;
                    }
                    if (sets.contains(n.getZoneNumber())) {
                        continue;
                    }
                    sets.add(n.getZoneNumber());
                    JSONObject o = new JSONObject();
                    o.put("zone", n.getZone());
                    o.put("number", n.getNumber());
                    json.put(o);
                }
            }
            if (json.length() == 0) {
                return new ArrayList<User>();
            }

            StringEntity s = new StringEntity(json.toString());
            s.setContentEncoding((Header) new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));

            request.setEntity(s);

            Log.i("imservice", "access token:" + Token.getInstance().accessToken);
            request.addHeader("Authorization", "Bearer " + Token.getInstance().accessToken);

            HttpResponse response = getClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK){
                return null;
            }
            int len = (int)response.getEntity().getContentLength();
            byte[] buf = new byte[len];
            InputStream inStream = response.getEntity().getContent();
            int pos = 0;
            while (pos < len) {
                int n = inStream.read(buf, pos, len - pos);
                if (n == -1) {
                    break;
                }
                pos += n;
            }
            inStream.close();
            if (pos != len) {
                return null;
            }

            String txt = new String(buf, "UTF-8");
            ArrayList<User> users = new ArrayList<User>();
            JSONArray jsonObject = new JSONArray(txt);
            for (int i = 0; i < jsonObject.length(); i++) {
                JSONObject o = (JSONObject)jsonObject.get(i);
                User u = new User();
                String z = o.getString("zone");
                String n = o.getString("number");
                u.number = new PhoneNumber(z, n);
                u.uid = o.getLong("uid");
                users.add(u);
            }
            return users;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }


}
