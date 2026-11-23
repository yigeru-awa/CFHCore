package com.onehumanawa.cfhcore.content.gui.crafting.config;

import com.onehumanawa.cfhcore.content.gui.emi.com.unascribed.qdcss.QDCSS;
import com.onehumanawa.cfhcore.content.gui.emi.config.ConfigEnum;
import com.onehumanawa.cfhcore.content.gui.emi.config.EmiConfig;
import com.onehumanawa.cfhcore.content.gui.emi.config.IntGroup;
import com.onehumanawa.cfhcore.content.gui.emi.config.ScreenAlign;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarPages;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarSubpanels;
import com.onehumanawa.cfhcore.content.gui.emi.config.SidebarType;
import com.onehumanawa.cfhcore.content.gui.emi.input.EmiBind;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.emi.search.EmiSearch;
import com.onehumanawa.cfhcore.content.gui.config.CFHGuiConfigApi;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs;
import com.onehumanawa.cfhcore.content.gui.config.api.SettingsTabs.Tab;
import com.onehumanawa.cfhcore.content.gui.config.hub.ConfigHubScreen;
import com.onehumanawa.cfhcore.content.gui.config.option.Binding;
import com.onehumanawa.cfhcore.content.gui.config.option.Choice;
import com.onehumanawa.cfhcore.content.gui.config.option.NumberRange;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionType;
import com.onehumanawa.cfhcore.content.gui.config.option.OptionValues;
import com.onehumanawa.cfhcore.content.gui.config.ui.OptionPageBase;
import com.onehumanawa.cfhcore.content.gui.config.ui.Section;
import com.onehumanawa.cfhcore.content.gui.core.gfx.Icon;
import com.onehumanawa.cfhcore.content.gui.crafting.CFHGuiCrafting;
import com.onehumanawa.cfhcore.content.gui.crafting.tree.TreeSettings;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public final class CraftingSettingsTab {
	public static final String TAB_ID = "crafting";
	public static final String PATH = "gameplay/crafting";
	private static final Map<String, NumberRange> RANGES = Map.of(
		"ui.maximum-recipe-screen-height",
		new NumberRange(96.0, 512.0, 8.0),
		"ui.minimum-recipe-screen-width",
		new NumberRange(56.0, 400.0, 4.0),
		"ui.vertical-margin",
		new NumberRange(0.0, 100.0, 1.0),
		"ui.recipe-screenshot-scale",
		new NumberRange(0.0, 8.0, 1.0)
	);

	public static void register() {
		SettingsTabs.register("gameplay", new Tab("crafting", Component.translatable("cfhgui_crafting.settings.tab"), Icon.GRID, CraftingSettingsTab::sections, 130));
	}

	public static void open(@Nullable Screen parent) {
		open(parent, null);
	}

	public static void open(@Nullable Screen parent, @Nullable String key) {
		CFHGuiConfigApi.openHub(parent, "gameplay/crafting");
		if (key != null && Minecraft.getInstance().screen instanceof ConfigHubScreen hub && hub.leaf() instanceof OptionPageBase page) {
			page.focusRow("emi:" + key);
		}
	}

	private static List<Section> sections() {
		QDCSS defaults = QDCSS.load("default", EmiConfig.DEFAULT_CONFIG);
		Map<String, Section> sections = new LinkedHashMap<>();
		sections.put("crafting_tree", TreeSettings.section());
		String subgroup = null;

		for (Field field : EmiConfig.class.getFields()) {
			EmiConfig.ConfigValue value = field.getAnnotation(EmiConfig.ConfigValue.class);
			if (value != null) {
				String key = value.value();
				int dot = key.indexOf(46);
				String group = dot < 0 ? key : key.substring(0, dot);
				if (!group.equals("persistent")) {
					EmiConfig.ConfigGroup start = field.getAnnotation(EmiConfig.ConfigGroup.class);
					if (start != null) {
						subgroup = start.value();
					}

					Section section = sections.computeIfAbsent(subgroup != null ? subgroup : group, CraftingSettingsTab::section);

					try {
						rows(section, field, key, defaults);
					} catch (RuntimeException | ReflectiveOperationException var14) {
						CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] settings: no row for {}: {}", key, var14.toString());
					}

					if (field.getAnnotation(EmiConfig.ConfigGroupEnd.class) != null) {
						subgroup = null;
					}
				}
			}
		}

		return new ArrayList<>(sections.values());
	}

	private static Section section(String group) {
		String name = group.replace('.', '_').replace('-', '_');
		String own = "cfhgui_crafting.settings.section." + name;
		Component title = I18n.exists(own) ? Component.translatable(own) : Component.translatable("config.emi.group." + group.replace('-', '_'));
		String about = own + ".about";
		return new Section("emi_" + name, title, I18n.exists(about) ? Component.translatable(about) : null);
	}

	private static void rows(Section section, Field field, String key, QDCSS defaults) throws ReflectiveOperationException {
		// $VF: Couldn't be decompiled
		// Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
		// java.lang.NullPointerException: Cannot invoke "org.jetbrains.java.decompiler.struct.gen.VarType.isGeneric()" because "newRet" is null
		//   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:634)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.DoStatement.toJava(DoStatement.java:146)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:241)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:254)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:254)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:254)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:254)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:254)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:254)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:254)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
		//   at org.jetbrains.java.decompiler.main.ClassWriter.writeMethod(ClassWriter.java:1283)
		//
		// Bytecode:
		// 000: aload 1
		// 001: invokevirtual java/lang/reflect/Field.getType ()Ljava/lang/Class;
		// 004: astore 4
		// 006: aload 2
		// 007: bipush 45
		// 009: bipush 95
		// 00b: invokevirtual java/lang/String.replace (CC)Ljava/lang/String;
		// 00e: invokedynamic makeConcatWithConstants (Ljava/lang/String;)Ljava/lang/String; bsm=java/lang/invoke/StringConcatFactory.makeConcatWithConstants (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; args=[ "config.emi.\u0001" ]
		// 013: invokestatic net/minecraft/network/chat/Component.translatable (Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;
		// 016: astore 5
		// 018: aload 1
		// 019: aload 2
		// 01a: invokestatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.tooltip (Ljava/lang/reflect/Field;Ljava/lang/String;)Lnet/minecraft/network/chat/Component;
		// 01d: astore 6
		// 01f: aload 4
		// 021: getstatic java/lang/Boolean.TYPE Ljava/lang/Class;
		// 024: if_acmpne 058
		// 027: aload 0
		// 028: aload 2
		// 029: getstatic dev/fallingcloud/slate/config/option/OptionType.BOOLEAN Ldev/fallingcloud/slate/config/option/OptionType;
		// 02c: aload 5
		// 02e: aload 6
		// 030: invokestatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.row (Ljava/lang/String;Ldev/fallingcloud/slate/config/option/OptionType;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)Ldev/fallingcloud/slate/config/option/Binding;
		// 033: aload 1
		// 034: invokedynamic get (Ljava/lang/reflect/Field;)Ljava/util/function/Supplier; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ ()Ljava/lang/Object;, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$0 (Ljava/lang/reflect/Field;)Ljava/lang/Object;, ()Ljava/lang/Object; ]
		// 039: invokevirtual dev/fallingcloud/slate/config/option/Binding.getter (Ljava/util/function/Supplier;)Ldev/fallingcloud/slate/config/option/Binding;
		// 03c: aload 1
		// 03d: invokedynamic accept (Ljava/lang/reflect/Field;)Ljava/util/function/Consumer; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)V, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$1 (Ljava/lang/reflect/Field;Ljava/lang/Object;)V, (Ljava/lang/Object;)V ]
		// 042: invokevirtual dev/fallingcloud/slate/config/option/Binding.setter (Ljava/util/function/Consumer;)Ldev/fallingcloud/slate/config/option/Binding;
		// 045: aload 3
		// 046: aload 2
		// 047: invokevirtual dev/emi/emi/com/unascribed/qdcss/QDCSS.getBoolean (Ljava/lang/String;)Ljava/util/Optional;
		// 04a: aconst_null
		// 04b: invokevirtual java/util/Optional.orElse (Ljava/lang/Object;)Ljava/lang/Object;
		// 04e: invokevirtual dev/fallingcloud/slate/config/option/Binding.def (Ljava/lang/Object;)Ldev/fallingcloud/slate/config/option/Binding;
		// 051: invokevirtual dev/fallingcloud/slate/config/ui/Section.add (Ldev/fallingcloud/slate/config/option/OptionBinding;)Ldev/fallingcloud/slate/config/ui/Section;
		// 054: pop
		// 055: goto 233
		// 058: aload 4
		// 05a: getstatic java/lang/Integer.TYPE Ljava/lang/Class;
		// 05d: if_acmpne 0b0
		// 060: aload 2
		// 061: getstatic dev/fallingcloud/slate/config/option/OptionType.INT Ldev/fallingcloud/slate/config/option/OptionType;
		// 064: aload 5
		// 066: aload 6
		// 068: invokestatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.row (Ljava/lang/String;Ldev/fallingcloud/slate/config/option/OptionType;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)Ldev/fallingcloud/slate/config/option/Binding;
		// 06b: aload 1
		// 06c: invokedynamic get (Ljava/lang/reflect/Field;)Ljava/util/function/Supplier; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ ()Ljava/lang/Object;, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$2 (Ljava/lang/reflect/Field;)Ljava/lang/Object;, ()Ljava/lang/Object; ]
		// 071: invokevirtual dev/fallingcloud/slate/config/option/Binding.getter (Ljava/util/function/Supplier;)Ldev/fallingcloud/slate/config/option/Binding;
		// 074: aload 1
		// 075: invokedynamic accept (Ljava/lang/reflect/Field;)Ljava/util/function/Consumer; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)V, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$3 (Ljava/lang/reflect/Field;Ljava/lang/Object;)V, (Ljava/lang/Object;)V ]
		// 07a: invokevirtual dev/fallingcloud/slate/config/option/Binding.setter (Ljava/util/function/Consumer;)Ldev/fallingcloud/slate/config/option/Binding;
		// 07d: aload 3
		// 07e: aload 2
		// 07f: invokevirtual dev/emi/emi/com/unascribed/qdcss/QDCSS.getInt (Ljava/lang/String;)Ljava/util/Optional;
		// 082: aconst_null
		// 083: invokevirtual java/util/Optional.orElse (Ljava/lang/Object;)Ljava/lang/Object;
		// 086: invokevirtual dev/fallingcloud/slate/config/option/Binding.def (Ljava/lang/Object;)Ldev/fallingcloud/slate/config/option/Binding;
		// 089: astore 7
		// 08b: getstatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.RANGES Ljava/util/Map;
		// 08e: aload 2
		// 08f: invokeinterface java/util/Map.get (Ljava/lang/Object;)Ljava/lang/Object; 2
		// 094: checkcast dev/fallingcloud/slate/config/option/NumberRange
		// 097: astore 8
		// 099: aload 8
		// 09b: ifnull 0a6
		// 09e: aload 7
		// 0a0: aload 8
		// 0a2: invokevirtual dev/fallingcloud/slate/config/option/Binding.range (Ldev/fallingcloud/slate/config/option/NumberRange;)Ldev/fallingcloud/slate/config/option/Binding;
		// 0a5: pop
		// 0a6: aload 0
		// 0a7: aload 7
		// 0a9: invokevirtual dev/fallingcloud/slate/config/ui/Section.add (Ldev/fallingcloud/slate/config/option/OptionBinding;)Ldev/fallingcloud/slate/config/ui/Section;
		// 0ac: pop
		// 0ad: goto 233
		// 0b0: aload 4
		// 0b2: ldc_w dev/emi/emi/input/EmiBind
		// 0b5: if_acmpne 0d3
		// 0b8: aload 1
		// 0b9: aconst_null
		// 0ba: invokevirtual java/lang/reflect/Field.get (Ljava/lang/Object;)Ljava/lang/Object;
		// 0bd: checkcast dev/emi/emi/input/EmiBind
		// 0c0: astore 7
		// 0c2: aload 0
		// 0c3: aload 7
		// 0c5: aload 6
		// 0c7: invokedynamic apply (Ldev/emi/emi/input/EmiBind;Lnet/minecraft/network/chat/Component;)Ljava/util/function/IntFunction; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (I)Ljava/lang/Object;, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$4 (Ldev/emi/emi/input/EmiBind;Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/client/gui/components/AbstractWidget;, (I)Lnet/minecraft/client/gui/components/AbstractWidget; ]
		// 0cc: invokevirtual dev/fallingcloud/slate/config/ui/Section.custom (Ljava/util/function/IntFunction;)Ldev/fallingcloud/slate/config/ui/Section;
		// 0cf: pop
		// 0d0: goto 233
		// 0d3: aload 4
		// 0d5: ldc_w dev/emi/emi/config/ScreenAlign
		// 0d8: if_acmpne 0ff
		// 0db: aload 0
		// 0dc: aload 2
		// 0dd: aload 5
		// 0df: aload 6
		// 0e1: aload 1
		// 0e2: aconst_null
		// 0e3: invokevirtual java/lang/reflect/Field.get (Ljava/lang/Object;)Ljava/lang/Object;
		// 0e6: checkcast dev/emi/emi/config/ScreenAlign
		// 0e9: aload 3
		// 0ea: aload 2
		// 0eb: invokevirtual dev/emi/emi/com/unascribed/qdcss/QDCSS.get (Ljava/lang/String;)Ljava/util/Optional;
		// 0ee: aconst_null
		// 0ef: invokevirtual java/util/Optional.orElse (Ljava/lang/Object;)Ljava/lang/Object;
		// 0f2: checkcast java/lang/String
		// 0f5: invokestatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.align (Ljava/lang/String;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Ldev/emi/emi/config/ScreenAlign;Ljava/lang/String;)Ldev/fallingcloud/slate/config/option/Binding;
		// 0f8: invokevirtual dev/fallingcloud/slate/config/ui/Section.add (Ldev/fallingcloud/slate/config/option/OptionBinding;)Ldev/fallingcloud/slate/config/ui/Section;
		// 0fb: pop
		// 0fc: goto 233
		// 0ff: aload 4
		// 101: ldc_w dev/emi/emi/config/SidebarPages
		// 104: if_acmpne 139
		// 107: aload 1
		// 108: aconst_null
		// 109: invokevirtual java/lang/reflect/Field.get (Ljava/lang/Object;)Ljava/lang/Object;
		// 10c: checkcast dev/emi/emi/config/SidebarPages
		// 10f: astore 7
		// 111: aload 3
		// 112: aload 2
		// 113: invokevirtual dev/emi/emi/com/unascribed/qdcss/QDCSS.get (Ljava/lang/String;)Ljava/util/Optional;
		// 116: ldc_w "none"
		// 119: invokevirtual java/util/Optional.orElse (Ljava/lang/Object;)Ljava/lang/Object;
		// 11c: checkcast java/lang/String
		// 11f: invokestatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.kinds (Ljava/lang/String;)Ljava/util/List;
		// 122: astore 8
		// 124: aload 0
		// 125: aload 5
		// 127: aload 6
		// 129: aload 7
		// 12b: aload 8
		// 12d: invokedynamic apply (Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Ldev/emi/emi/config/SidebarPages;Ljava/util/List;)Ljava/util/function/IntFunction; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (I)Ljava/lang/Object;, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$5 (Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Ldev/emi/emi/config/SidebarPages;Ljava/util/List;I)Lnet/minecraft/client/gui/components/AbstractWidget;, (I)Lnet/minecraft/client/gui/components/AbstractWidget; ]
		// 132: invokevirtual dev/fallingcloud/slate/config/ui/Section.custom (Ljava/util/function/IntFunction;)Ldev/fallingcloud/slate/config/ui/Section;
		// 135: pop
		// 136: goto 233
		// 139: aload 4
		// 13b: ldc_w dev/emi/emi/config/SidebarSubpanels
		// 13e: if_acmpne 159
		// 141: aload 0
		// 142: aload 2
		// 143: aload 5
		// 145: aload 6
		// 147: aload 1
		// 148: aconst_null
		// 149: invokevirtual java/lang/reflect/Field.get (Ljava/lang/Object;)Ljava/lang/Object;
		// 14c: checkcast dev/emi/emi/config/SidebarSubpanels
		// 14f: invokestatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.subpanels (Ljava/lang/String;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Ldev/emi/emi/config/SidebarSubpanels;)Ldev/fallingcloud/slate/config/option/Binding;
		// 152: invokevirtual dev/fallingcloud/slate/config/ui/Section.add (Ldev/fallingcloud/slate/config/option/OptionBinding;)Ldev/fallingcloud/slate/config/ui/Section;
		// 155: pop
		// 156: goto 233
		// 159: ldc_w dev/emi/emi/config/IntGroup
		// 15c: aload 4
		// 15e: invokevirtual java/lang/Class.isAssignableFrom (Ljava/lang/Class;)Z
		// 161: ifeq 184
		// 164: aload 0
		// 165: aload 2
		// 166: aload 5
		// 168: aload 6
		// 16a: aload 1
		// 16b: aconst_null
		// 16c: invokevirtual java/lang/reflect/Field.get (Ljava/lang/Object;)Ljava/lang/Object;
		// 16f: checkcast dev/emi/emi/config/IntGroup
		// 172: aload 3
		// 173: aload 2
		// 174: invokevirtual dev/emi/emi/com/unascribed/qdcss/QDCSS.get (Ljava/lang/String;)Ljava/util/Optional;
		// 177: aconst_null
		// 178: invokevirtual java/util/Optional.orElse (Ljava/lang/Object;)Ljava/lang/Object;
		// 17b: checkcast java/lang/String
		// 17e: invokestatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.numbers (Ldev/fallingcloud/slate/config/ui/Section;Ljava/lang/String;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Ldev/emi/emi/config/IntGroup;Ljava/lang/String;)V
		// 181: goto 233
		// 184: ldc_w dev/emi/emi/config/ConfigEnum
		// 187: aload 4
		// 189: invokevirtual java/lang/Class.isAssignableFrom (Ljava/lang/Class;)Z
		// 18c: ifeq 233
		// 18f: getstatic dev/emi/emi/config/EmiConfig.FILTERS Ljava/util/Map;
		// 192: aload 2
		// 193: invokedynamic test ()Ljava/util/function/Predicate; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)Z, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$6 (Ljava/lang/Object;)Z, (Ljava/lang/Object;)Z ]
		// 198: invokeinterface java/util/Map.getOrDefault (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 3
		// 19d: checkcast java/util/function/Predicate
		// 1a0: astore 7
		// 1a2: new java/util/ArrayList
		// 1a5: dup
		// 1a6: invokespecial java/util/ArrayList.<init> ()V
		// 1a9: astore 8
		// 1ab: aload 4
		// 1ad: invokevirtual java/lang/Class.getEnumConstants ()[Ljava/lang/Object;
		// 1b0: astore 9
		// 1b2: aload 9
		// 1b4: arraylength
		// 1b5: istore 10
		// 1b7: bipush 0
		// 1b8: istore 11
		// 1ba: iload 11
		// 1bc: iload 10
		// 1be: if_icmpge 1fe
		// 1c1: aload 9
		// 1c3: iload 11
		// 1c5: aaload
		// 1c6: astore 12
		// 1c8: aload 12
		// 1ca: checkcast dev/emi/emi/config/ConfigEnum
		// 1cd: astore 13
		// 1cf: aload 7
		// 1d1: aload 13
		// 1d3: invokeinterface java/util/function/Predicate.test (Ljava/lang/Object;)Z 2
		// 1d8: ifeq 1f8
		// 1db: aload 8
		// 1dd: new dev/fallingcloud/slate/config/option/Choice
		// 1e0: dup
		// 1e1: aload 13
		// 1e3: invokeinterface dev/emi/emi/config/ConfigEnum.getName ()Ljava/lang/String; 1
		// 1e8: aload 13
		// 1ea: invokeinterface dev/emi/emi/config/ConfigEnum.getText ()Lnet/minecraft/network/chat/Component; 1
		// 1ef: invokespecial dev/fallingcloud/slate/config/option/Choice.<init> (Ljava/lang/String;Lnet/minecraft/network/chat/Component;)V
		// 1f2: invokeinterface java/util/List.add (Ljava/lang/Object;)Z 2
		// 1f7: pop
		// 1f8: iinc 11 1
		// 1fb: goto 1ba
		// 1fe: aload 0
		// 1ff: aload 2
		// 200: getstatic dev/fallingcloud/slate/config/option/OptionType.CHOICE Ldev/fallingcloud/slate/config/option/OptionType;
		// 203: aload 5
		// 205: aload 6
		// 207: invokestatic dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.row (Ljava/lang/String;Ldev/fallingcloud/slate/config/option/OptionType;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;)Ldev/fallingcloud/slate/config/option/Binding;
		// 20a: aload 8
		// 20c: invokevirtual dev/fallingcloud/slate/config/option/Binding.choices (Ljava/util/List;)Ldev/fallingcloud/slate/config/option/Binding;
		// 20f: aload 1
		// 210: invokedynamic get (Ljava/lang/reflect/Field;)Ljava/util/function/Supplier; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ ()Ljava/lang/Object;, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$7 (Ljava/lang/reflect/Field;)Ljava/lang/Object;, ()Ljava/lang/Object; ]
		// 215: invokevirtual dev/fallingcloud/slate/config/option/Binding.getter (Ljava/util/function/Supplier;)Ldev/fallingcloud/slate/config/option/Binding;
		// 218: aload 4
		// 21a: aload 1
		// 21b: invokedynamic accept (Ljava/lang/Class;Ljava/lang/reflect/Field;)Ljava/util/function/Consumer; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)V, dev/fallingcloud/slate/crafting/config/CraftingSettingsTab.lambda$rows$8 (Ljava/lang/Class;Ljava/lang/reflect/Field;Ljava/lang/Object;)V, (Ljava/lang/Object;)V ]
		// 220: invokevirtual dev/fallingcloud/slate/config/option/Binding.setter (Ljava/util/function/Consumer;)Ldev/fallingcloud/slate/config/option/Binding;
		// 223: aload 3
		// 224: aload 2
		// 225: invokevirtual dev/emi/emi/com/unascribed/qdcss/QDCSS.get (Ljava/lang/String;)Ljava/util/Optional;
		// 228: aconst_null
		// 229: invokevirtual java/util/Optional.orElse (Ljava/lang/Object;)Ljava/lang/Object;
		// 22c: invokevirtual dev/fallingcloud/slate/config/option/Binding.def (Ljava/lang/Object;)Ldev/fallingcloud/slate/config/option/Binding;
		// 22f: invokevirtual dev/fallingcloud/slate/config/ui/Section.add (Ldev/fallingcloud/slate/config/option/OptionBinding;)Ldev/fallingcloud/slate/config/ui/Section;
		// 232: pop
		// 233: return
	}

	private static Binding row(String key, OptionType type, Component label, @Nullable Component tip) {
		return Binding.of("emi:" + key, type, label).tooltip(tip).searchWords("crafting recipes items emi " + key.replace('.', ' ').replace('-', ' '));
	}

	@Nullable
	private static Component tooltip(Field field, String key) {
		String translated = "config.emi.tooltip." + key.replace('-', '_');
		if (I18n.exists(translated)) {
			return Component.translatable(translated);
		} else {
			EmiConfig.Comment comment = field.getAnnotation(EmiConfig.Comment.class);
			return comment == null ? null : Component.literal(comment.value());
		}
	}

	private static Binding align(String key, Component label, @Nullable Component tip, ScreenAlign align, @Nullable String def) {
		List<Choice> choices = new ArrayList<>();

		for (ScreenAlign.Vertical v : ScreenAlign.Vertical.values()) {
			for (ScreenAlign.Horizontal h : ScreenAlign.Horizontal.values()) {
				choices.add(new Choice(h.getName() + "," + v.getName(), Component.translatable("cfhgui_crafting.settings.align", new Object[]{v.getText(), h.getText()})));
			}
		}

		return row(key, OptionType.CHOICE, label, tip).choices(choices).getter(() -> align.horizontal.getName() + "," + align.vertical.getName()).setter(vx -> {
			String[] parts = String.valueOf(vx).split(",");
			if (parts.length == 2) {
				align.horizontal = ScreenAlign.Horizontal.fromName(parts[0].trim());
				align.vertical = ScreenAlign.Vertical.fromName(parts[1].trim());
				changed();
			}
		}).def(def == null ? null : def.replace(" ", ""));
	}

	private static void numbers(Section section, String key, Component label, @Nullable Component tip, IntGroup group, @Nullable String def) {
		String[] defaults = def == null ? new String[0] : def.split(",");
		boolean size = key.endsWith("-size");

		for (int i = 0; i < group.size; i++) {
			int index = i;
			Integer d = null;

			try {
				if (defaults.length == group.size) {
					d = Integer.parseInt(defaults[i].trim());
				}
			} catch (NumberFormatException var12) {
			}

			section.add(
				Binding.of(
						"emi:" + key + "." + group.names.get(i),
						OptionType.INT,
						Component.translatable("cfhgui_crafting.settings.of", new Object[]{label, group.getValueTranslation(i)})
					)
					.tooltip(tip)
					.searchWords("crafting emi " + key.replace('.', ' ').replace('-', ' ') + " " + group.names.get(i))
					.range(size ? new NumberRange(0.0, i == 0 ? 32.0 : 100.0, 1.0) : new NumberRange(0.0, 64.0, 1.0))
					.getter(() -> group.values.getInt(index))
					.setter(v -> {
						group.values.set(index, OptionValues.asInt(v, group.values.getInt(index)));
						changed();
					})
					.def(d)
			);
		}
	}

	private static Binding subpanels(String key, Component label, @Nullable Component tip, SidebarSubpanels subpanels) {
		Component help = Component.translatable("cfhgui_crafting.settings.subpanels.tip");
		return row(key, OptionType.LIST, label, (Component)(tip == null ? help : Component.literal(tip.getString() + "\n" + help.getString()))).getter(() -> {
			List<String> out = new ArrayList<>();

			for (SidebarSubpanels.Subpanel s : subpanels.subpanels) {
				out.add(s.type.getName() + " " + s.rows);
			}

			return out;
		}).setter(v -> {
			subpanels.subpanels.clear();

			for (String entry : OptionValues.asList(v)) {
				String[] parts = entry.trim().toLowerCase(Locale.ROOT).split("\\s+");
				SidebarType type = SidebarType.fromName(parts[0]);
				int rows = 1;

				try {
					if (parts.length > 1) {
						rows = Integer.parseInt(parts[1]);
					}
				} catch (NumberFormatException var8) {
				}

				if (type != SidebarType.NONE && rows >= 1) {
					subpanels.subpanels.add(new SidebarSubpanels.Subpanel(type, rows));
				}
			}

			subpanels.unique();
			changed();
		}).def(List.of());
	}

	private static List<SidebarType> kinds(String value) {
		List<SidebarType> out = new ArrayList<>();

		for (String name : value.split(",")) {
			SidebarType type = SidebarType.fromName(name.trim().toLowerCase(Locale.ROOT));
			if (type != SidebarType.NONE && !out.contains(type)) {
				out.add(type);
			}
		}

		return out;
	}

	@Nullable
	private static Object read(Field field) {
		try {
			return field.get(null);
		} catch (RuntimeException | ReflectiveOperationException var2) {
			return null;
		}
	}

	private static void write(Field field, Object value) {
		try {
			field.set(null, value);
			changed();
		} catch (RuntimeException | ReflectiveOperationException var3) {
			CFHGuiCrafting.LOGGER.warn("[CFHGui Crafting] settings: cannot set {}: {}", field.getName(), var3.toString());
		}
	}

	private static void changed() {
		EmiConfig.writeConfig();
		EmiSearch.update();
		EmiScreenManager.dockChanged();
	}

	private CraftingSettingsTab() {
	}
}
