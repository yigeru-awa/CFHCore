package com.onehumanawa.cfhcore.foundation.recipe;

import com.onehumanawa.cfhcore.foundation.recipe.schema.RecipeSchema;

import java.util.ArrayList;
import java.util.List;

public class RecipeValidationContext {
    private final RecipeModification recipe;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private String currentPath = "";

    public RecipeValidationContext(RecipeModification recipe) {
        this.recipe = recipe;
    }

    public RecipeModification recipe() {
        return recipe;
    }

    public RecipeInfo info() {
        return new RecipeInfo(recipe.id, recipe.type.id());
    }

    public RecipeSchema schema() {
        return recipe.type.schema();
    }

    public void push(String path) {
        this.currentPath = currentPath.isEmpty() ? path : currentPath + "." + path;
    }

    public void push(int index) {
        this.currentPath = currentPath.isEmpty() ? "[" + index + "]" : currentPath + "[" + index + "]";
    }

    public void pop() {
        int lastDot = currentPath.lastIndexOf('.');
        int lastBracket = currentPath.lastIndexOf('[');
        int lastSeparator = Math.max(lastDot, lastBracket);
        this.currentPath = lastSeparator >= 0 ? currentPath.substring(0, lastSeparator) : "";
    }

    public String currentPath() {
        return currentPath;
    }

    public void error(String message) {
        errors.add(currentPath + ": " + message);
    }

    public void error(String message, Object... args) {
        errors.add(currentPath + ": " + format(message, args));
    }

    public void warning(String message) {
        warnings.add(currentPath + ": " + message);
    }

    public void warning(String message, Object... args) {
        warnings.add(currentPath + ": " + format(message, args));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public void clear() {
        errors.clear();
        warnings.clear();
    }

    public void setKey(int index) {
        push(index);
    }

    public void setKey(String name) {
        push(name);
    }

    private static String format(String message, Object... args) {
        if (message == null || args == null || args.length == 0) {
            return String.valueOf(message);
        }
        StringBuilder builder = new StringBuilder(message.length() + 32);
        int argIndex = 0;
        int cursor = 0;
        while (cursor < message.length()) {
            int placeholder = message.indexOf("{}", cursor);
            if (placeholder < 0 || argIndex >= args.length) {
                builder.append(message, cursor, message.length());
                break;
            }
            builder.append(message, cursor, placeholder);
            builder.append(args[argIndex++]);
            cursor = placeholder + 2;
        }
        return builder.toString();
    }
}