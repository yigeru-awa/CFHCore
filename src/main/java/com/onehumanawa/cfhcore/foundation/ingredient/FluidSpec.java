package com.onehumanawa.cfhcore.foundation.ingredient;

public class FluidSpec {
    private final String id;
    private final int amount;
    private final boolean isTag;

    private FluidSpec(String id, int amount, boolean isTag) {
        this.id = id;
        this.amount = amount;
        this.isTag = isTag;
    }

    public static FluidSpec of(String id, int amount) {
        return new FluidSpec(id, amount, false);
    }

    public static FluidSpec tag(String id, int amount) {
        return new FluidSpec(id, amount, true);
    }

    public static FluidSpec empty() {
        return new FluidSpec("", 0, false);
    }

    public String id() { return id; }
    public int amount() { return amount; }
    public boolean isTag() { return isTag; }
    public boolean isEmpty() { return id.isEmpty() || amount <= 0; }

    public com.google.gson.JsonObject toJson() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        if (isTag) {
            json.addProperty("type", "neoforge:tag");
            json.addProperty("tag", id);
        } else {
            json.addProperty("type", "neoforge:single");
            json.addProperty("fluid", id);
        }
        json.addProperty("amount", amount);
        return json;
    }

    public com.google.gson.JsonObject toResultJson() {
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("id", id);
        json.addProperty("amount", amount);
        return json;
    }
}