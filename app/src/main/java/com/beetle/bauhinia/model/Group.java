package com.beetle.bauhinia.model;

/**
 * Created by houxh on 15/3/21.
 */
public class Group {
    public long groupID;
    public long master;//管理员or创建者
    public String topic;
    public boolean disbanded;//是否解散
}
