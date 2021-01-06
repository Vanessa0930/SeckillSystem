package com.seckillservice.common.models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Transaction {
    private String id;
    private String inventoryId;
    private Date createDate;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setInventoryId(String iId) {
        this.inventoryId = iId;
    }

    public void setCreateDate(Date time) {
        this.createDate = time;
    }

    public String prettyPrintRecord() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("Transaction id: %s, inventoryId: %s, createDate: %s",
                this.id, this.inventoryId, formatter.format(this.createDate));
    }
}
