package org.jmqtt.broker.store.rdb.daoobject;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name = "jmqtt_will_message")
public class WillMessageDO implements Serializable {

    private static final long serialVersionUID = 12213131231231L;

    @Id
    private Long id;

    private String clientId;

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
