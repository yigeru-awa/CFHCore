package com.onehumanawa.cfhcore.foundation;

import com.onehumanawa.cfhcore.CFHCore;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ModLangProvider extends LanguageProvider {
    private final String locale;
    private final Set<String> addedKeys = new HashSet<>();

    public ModLangProvider(PackOutput output, String locale) {
        super(output, CFHCore.ID, locale);
        this.locale = locale;
    }
    @Override
    public void add(@NotNull String key, @NotNull String value) {
        if (key.isBlank() || value.isBlank()) return;
        if (addedKeys.add(key)) {
            super.add(key, value);
        }
    }
    @Override
    protected void addTranslations() {
        for (TooltipHelper.Entry entry : TooltipHelper.entries()) {
            entry.translations(locale).forEach(this::add);
        }

        boolean zh = locale.equals("zh_cn");
        CFHCore.langCollector().getEntries().forEach((key, entry) ->
                add(key, zh ? entry.zh() : entry.en()));
    }
}