package com.onehumanawa.cfhcore.content.gui.emi.runtime;

import com.google.common.collect.Lists;
import com.onehumanawa.cfhcore.content.gui.emi.EmiPort;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiInitRegistry;
import com.onehumanawa.cfhcore.content.gui.emi.api.EmiRegistry;
import com.onehumanawa.cfhcore.content.gui.emi.api.recipe.EmiRecipe;
import com.onehumanawa.cfhcore.content.gui.emi.bom.BoM;
import com.onehumanawa.cfhcore.content.gui.emi.mixinsupport.EmiMixinTransformation;
import com.onehumanawa.cfhcore.content.gui.emi.platform.EmiAgnos;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiComparisonDefaults;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiDragDropHandlers;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiExclusionAreas;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiIngredientSerializers;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiInitRegistryImpl;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiPluginContainer;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipeFiller;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRecipes;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiRegistryImpl;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiStackList;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiStackProviders;
import com.onehumanawa.cfhcore.content.gui.emi.registry.EmiTags;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenBase;
import com.onehumanawa.cfhcore.content.gui.emi.screen.EmiScreenManager;
import com.onehumanawa.cfhcore.content.gui.emi.search.EmiSearch;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class EmiReloadManager {
	private static int loadedResourcesMask = 0;
	private static volatile boolean clear = false;
	private static volatile boolean restart = false;
	private static volatile int status = 0;
	private static Thread thread;
	public static volatile Component reloadStep = EmiPort.literal("");
	public static volatile long reloadWorry = Long.MAX_VALUE;

	public static void reloadTags() {
		loadedResourcesMask |= 1;
		if (loadedResourcesMask == 3) {
			EmiLog.info("Recipes synchronized, reloading EMI");
			loadedResourcesMask = 0;
			reload();
		} else {
			EmiLog.info("Recipes synchronized, waiting for tags to reload EMI...");
		}
	}

	public static void reloadRecipes() {
		loadedResourcesMask |= 2;
		if (loadedResourcesMask == 3) {
			EmiLog.info("Tags synchronized, reloading EMI");
			loadedResourcesMask = 0;
			reload();
		} else {
			EmiLog.info("Tags synchronized, waiting for recipes to reload EMI...");
		}
	}

	public static void clear() {
		synchronized (EmiReloadManager.class) {
			loadedResourcesMask = 0;
			clear = true;
			status = 0;
			reloadWorry = Long.MAX_VALUE;
			if (thread != null && thread.isAlive()) {
				restart = true;
			} else {
				thread = new Thread(new EmiReloadManager.ReloadWorker());
				thread.setDaemon(true);
				thread.start();
			}
		}
	}

	public static void reload() {
		synchronized (EmiReloadManager.class) {
			step(EmiPort.literal("Starting Reload"));
			status = 1;
			clear = false;
			if (thread != null && thread.isAlive()) {
				restart = true;
			} else {
				thread = new Thread(new EmiReloadManager.ReloadWorker());
				thread.setDaemon(false);
				thread.start();
			}
		}
	}

	public static boolean abandoned() {
		return clear || Minecraft.getInstance().level == null;
	}

	private static boolean worldGone() {
		if (!abandoned()) {
			return false;
		} else {
			synchronized (EmiReloadManager.class) {
				if (!clear) {
					clear = true;
					status = 0;
					reloadWorry = Long.MAX_VALUE;
				}

				restart = true;
			}

			EmiLog.info("The world was left while the viewer's data was loading (at \"" + reloadStep.getString() + "\"): the reload ends here");
			return true;
		}
	}

	public static void step(Component text) {
		step(text, 5000L);
	}

	public static void step(Component text, long worry) {
		EmiLog.info(text.getString());
		reloadStep = text;
		reloadWorry = System.currentTimeMillis() + worry;
	}

	public static boolean isLoaded() {
		return status == 2 && (thread == null || !thread.isAlive());
	}

	public static int getStatus() {
		return status;
	}

	static {
		EmiMixinTransformation.preach();
	}

	private static class ReloadWorker implements Runnable {
		@Override
		public void run() {
			int retries = 3;

			label161:
			do {
				try {
					if (!EmiReloadManager.clear) {
						EmiLog.info("Starting EMI reload...");
					}

					long reloadStart = System.currentTimeMillis();
					EmiReloadManager.restart = false;
					EmiReloadManager.step(EmiPort.literal("Clearing data"));
					EmiRecipes.clear();
					EmiStackList.clear();
					EmiIngredientSerializers.clear();
					EmiExclusionAreas.clear();
					EmiDragDropHandlers.clear();
					EmiStackProviders.clear();
					EmiRecipeFiller.clear();
					EmiHidden.clear();
					EmiTags.ADAPTERS_BY_CLASS.map().clear();
					EmiTags.ADAPTERS_BY_REGISTRY.clear();
					EmiScreenBase.clearScreenBoundsProviders();
					if (EmiReloadManager.clear) {
						EmiReloadManager.clear = false;
					} else {
						Minecraft client = Minecraft.getInstance();
						if (client.level == null) {
							EmiReloadLog.warn("World is null");
							break;
						}

						if (!ProxyRecipeManager.isAvailable()) {
							EmiReloadLog.warn("Recipe Manager is null");
							break;
						}

						List<EmiPluginContainer> plugins = Lists.newArrayList();
						plugins.addAll(EmiAgnos.getPlugins().stream().sorted((a, b) -> Integer.compare(entrypointPriority(a), entrypointPriority(b))).toList());

						EmiInitRegistry initRegistry = new EmiInitRegistryImpl();

						for (EmiPluginContainer container : plugins) {
							EmiReloadManager.step(EmiPort.literal("Initializing plugin from " + container.id()), 5000L);
							long start = System.currentTimeMillis();

							try {
								container.plugin().initialize(initRegistry);
							} catch (Throwable var15) {
								EmiReloadLog.warn("Exception initializing plugin provided by " + container.id(), var15);
								if (EmiReloadManager.restart) {
									continue label161;
								}
								continue;
							}

							EmiLog.info("Initialized plugin from " + container.id() + " in " + (System.currentTimeMillis() - start) + "ms");
						}

						if (!EmiReloadManager.worldGone()) {
							EmiHidden.reload();
							EmiReloadManager.step(EmiPort.literal("Processing tags"));
							EmiTags.reload();
							if (!EmiReloadManager.worldGone()) {
								EmiReloadManager.step(EmiPort.literal("Constructing index"));
								EmiComparisonDefaults.comparisons = new HashMap<>();
								EmiStackList.reload();
								if (!EmiReloadManager.restart) {
									EmiRegistry registry = new EmiRegistryImpl();

									for (EmiPluginContainer container : plugins) {
										if (EmiReloadManager.worldGone()) {
											continue label161;
										}

										EmiReloadManager.step(EmiPort.literal("Loading plugin from " + container.id()), 10000L);
										long start = System.currentTimeMillis();

										try {
											container.plugin().register(registry);
										} catch (Throwable var14) {
											EmiReloadLog.warn("Exception loading plugin provided by " + container.id(), var14);
											if (EmiReloadManager.restart) {
												continue label161;
											}
											continue;
										}

										EmiLog.info("Reloaded plugin from " + container.id() + " in " + (System.currentTimeMillis() - start) + "ms");
										if (EmiReloadManager.restart) {
											continue label161;
										}
									}

									if (!EmiReloadManager.restart && !EmiReloadManager.worldGone()) {
										EmiReloadManager.step(EmiPort.literal("Baking index"));
										EmiStackList.bake();
										EmiReloadManager.step(EmiPort.literal("Registering late recipes"), 10000L);
										Consumer<EmiRecipe> registerLateRecipe = registry::addRecipe;

										for (Consumer<Consumer<EmiRecipe>> consumer : EmiRecipes.lateRecipes) {
											try {
												consumer.accept(registerLateRecipe);
											} catch (Exception var13) {
												EmiReloadLog.warn("Exception loading late recipes for plugins:", var13);
												if (EmiReloadManager.restart) {
													continue label161;
												}
											}
										}

										if (!EmiReloadManager.worldGone()) {
											EmiReloadManager.step(EmiPort.literal("Baking recipes"), 15000L);
											EmiRecipes.bake();
											BoM.reload();
											EmiPersistentData.load();
											if (!EmiReloadManager.worldGone()) {
												EmiReloadManager.step(EmiPort.literal("Baking search"), 15000L);
												EmiSearch.bake();
												if (!EmiReloadManager.worldGone()) {
													EmiReloadManager.step(EmiPort.literal("Finishing up"));
													EmiScreenManager.search.update();
													EmiScreenManager.forceRecalculate();
													EmiReloadLog.bake();
													EmiLog.info("Reloaded EMI in " + (System.currentTimeMillis() - reloadStart) + "ms");
													EmiReloadManager.status = 2;
												}
											}
										}
									}
								}
							}
						}
					}
				} catch (Throwable var16) {
					boolean again = retries-- > 0;
					if (!again || !EmiReloadManager.worldGone()) {
						EmiReloadLog.warn("Critical error occured during reload:", var16);
						EmiReloadManager.status = -1;
						if (again) {
							EmiReloadManager.restart = true;
						}
					}
				}
			} while (EmiReloadManager.restart);

			EmiReloadManager.thread = null;
		}

		private static final int entrypointPriority(EmiPluginContainer container) {
			return container.id().equals("emi") ? 0 : 1;
		}
	}
}
