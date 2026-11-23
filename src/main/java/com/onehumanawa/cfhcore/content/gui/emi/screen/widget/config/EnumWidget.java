package com.onehumanawa.cfhcore.content.gui.emi.screen.widget.config;

import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.config.ConfigEnum;
import com.onehumanawa.cfhcore.content.gui.emi.screen.ConfigScreen;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

public class EnumWidget extends ConfigEntryWidget {
	private final ConfigScreen.Mutator<ConfigEnum> mutator;
	private Button button;

	public EnumWidget(
		Component name, List<ClientTooltipComponent> tooltip, Supplier<String> search, ConfigScreen.Mutator<ConfigEnum> mutator, Predicate<ConfigEnum> filter
	) {
		super(name, tooltip, search, 20);
		this.mutator = mutator;
		this.button = EmiPort.newButton(0, 0, 150, 20, this.getText(), button -> page(mutator.get(), filter, mutator::set));
		this.setChildren(List.of(this.button));
	}

	public static void page(ConfigEnum original, Predicate<ConfigEnum> filter, Consumer<ConfigEnum> consumer) {
		// $VF: Couldn't be decompiled
		// Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
		// java.lang.NullPointerException: Cannot invoke "org.jetbrains.java.decompiler.struct.gen.VarType.isGeneric()" because "newRet" is null
		//   at org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent.getInferredExprType(InvocationExprent.java:634)
		//   at org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.getInferredExprType(FunctionExprent.java:243)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.getCastedExprent(ExprProcessor.java:966)
		//   at org.jetbrains.java.decompiler.modules.decompiler.exps.AssignmentExprent.toJava(AssignmentExprent.java:154)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.listToJava(ExprProcessor.java:895)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement.toJava(BasicBlockStatement.java:90)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement.toJava(IfStatement.java:203)
		//   at org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor.jmpWrapper(ExprProcessor.java:833)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.SequenceStatement.toJava(SequenceStatement.java:107)
		//   at org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement.toJava(RootStatement.java:36)
		//   at org.jetbrains.java.decompiler.main.ClassWriter.writeMethod(ClassWriter.java:1283)
		//
		// Bytecode:
		// 00: aload 0
		// 01: checkcast java/lang/Enum
		// 04: astore 3
		// 05: aload 3
		// 06: invokevirtual java/lang/Object.getClass ()Ljava/lang/Class;
		// 09: invokevirtual java/lang/Class.getEnumConstants ()[Ljava/lang/Object;
		// 0c: checkcast [Ljava/lang/Enum;
		// 0f: astore 4
		// 11: invokestatic net/minecraft/client/Minecraft.getInstance ()Lnet/minecraft/client/Minecraft;
		// 14: astore 5
		// 16: aload 5
		// 18: getfield net/minecraft/client/Minecraft.screen Lnet/minecraft/client/gui/screens/Screen;
		// 1b: astore 7
		// 1d: aload 7
		// 1f: instanceof dev/emi/emi/screen/ConfigScreen
		// 22: ifeq 5a
		// 25: aload 7
		// 27: checkcast dev/emi/emi/screen/ConfigScreen
		// 2a: astore 6
		// 2c: aload 5
		// 2e: new dev/emi/emi/screen/ConfigEnumScreen
		// 31: dup
		// 32: aload 6
		// 34: aload 4
		// 36: invokestatic java/util/stream/Stream.of ([Ljava/lang/Object;)Ljava/util/stream/Stream;
		// 39: aload 1
		// 3a: invokedynamic test (Ljava/util/function/Predicate;)Ljava/util/function/Predicate; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)Z, dev/emi/emi/screen/widget/config/EnumWidget.lambda$page$1 (Ljava/util/function/Predicate;Ljava/lang/Enum;)Z, (Ljava/lang/Enum;)Z ]
		// 3f: invokeinterface java/util/stream/Stream.filter (Ljava/util/function/Predicate;)Ljava/util/stream/Stream; 2
		// 44: invokedynamic apply ()Ljava/util/function/Function; bsm=java/lang/invoke/LambdaMetafactory.metafactory (Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; args=[ (Ljava/lang/Object;)Ljava/lang/Object;, dev/emi/emi/screen/widget/config/EnumWidget.lambda$page$2 (Ljava/lang/Enum;)Ldev/emi/emi/screen/ConfigEnumScreen$Entry;, (Ljava/lang/Enum;)Ldev/emi/emi/screen/ConfigEnumScreen$Entry; ]
		// 49: invokeinterface java/util/stream/Stream.map (Ljava/util/function/Function;)Ljava/util/stream/Stream; 2
		// 4e: invokeinterface java/util/stream/Stream.toList ()Ljava/util/List; 1
		// 53: aload 2
		// 54: invokespecial dev/emi/emi/screen/ConfigEnumScreen.<init> (Ldev/emi/emi/screen/ConfigScreen;Ljava/util/List;Ljava/util/function/Consumer;)V
		// 57: invokevirtual net/minecraft/client/Minecraft.setScreen (Lnet/minecraft/client/gui/screens/Screen;)V
		// 5a: return
	}

	public Component getText() {
		return this.mutator.get().getText();
	}

	@Override
	public void update(int y, int x, int width, int height) {
		this.button.x = x + width - this.button.getWidth();
		this.button.y = y;
	}
}
