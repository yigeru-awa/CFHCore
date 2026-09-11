package com.onehumanawa.cfhcore.foundation.recipe;

public class RecipeModifyRuntimeException extends RuntimeException {
    private String sourceLine = "unknown";

    public RecipeModifyRuntimeException(String message) {
        super(message);
    }

    public RecipeModifyRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecipeModifyRuntimeException source(String source) {
        this.sourceLine = source;
        return this;
    }

    public RecipeModifyRuntimeException sourceLine(String sourceLine) {
        this.sourceLine = sourceLine;
        return this;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " (at " + sourceLine + ")";
    }
}