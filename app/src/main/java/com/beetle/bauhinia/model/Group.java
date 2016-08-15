package com.beetle.bauhinia.model;

import java.util.ArrayList;

/**
 * Created by houxh on 15/3/21.
 */
public class Group {
    public long groupID;
    public long master;//管理员or创建者
    public String topic;
    public boolean disbanded;//是否解散

    private ArrayList<Long> members = new ArrayList<Long>();
    public void addMember(long uid) {
        if (members.contains(uid)) {
            return;
        }
        members.add(uid);
    }
    public void removeMember(long uid) {
        members.remove(uid);
    }

    public ArrayList<Long> getMembers() {
        return members;
    }
    public void setMembers(ArrayList<Long> members) {
        this.members = members;
    }
}
