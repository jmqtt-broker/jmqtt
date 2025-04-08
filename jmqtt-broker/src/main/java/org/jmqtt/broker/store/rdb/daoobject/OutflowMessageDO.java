package org.jmqtt.broker.store.rdb.daoobject;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "jmqtt_outflow_message")
public class OutflowMessageDO implements Serializable {

    private static final long serialVersionUID = 1213131231231L;

    @Id
    private Long id;

    private String clientId;

    private Integer msgId;

    private String content;

    private Long gmtCreate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getMsgId() {
        return msgId;
    }

    public void setMsgId(Integer msgId) {
        this.msgId = msgId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}
