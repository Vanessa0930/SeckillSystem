package com.seckillservice.common.models;

public class Inventory {
    private String id;
    private String name;
    private int count;
    private int sales;
    private int version;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return this.count;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    public int getSales() {
        return this.sales;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return this.version;
    }

    public String prettyPrintRecord() {
        return String.format("Inventory id: %s, name: %s, count: %d, sales: %d, version: %d",
                this.id, this.name, this.count, this.sales, this.version);
    }
}
