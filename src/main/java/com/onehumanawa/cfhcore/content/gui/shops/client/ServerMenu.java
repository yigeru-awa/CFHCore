package com.onehumanawa.cfhcore.content.gui.shops.client;

import com.onehumanawa.cfhcore.content.gui.server.client.ServerClient;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.admin.AdminPages;
import com.onehumanawa.cfhcore.content.gui.server.client.ui.admin.AdminPages.Extra;
import com.onehumanawa.cfhcore.content.gui.shops.client.ui.AdminShopsPage;

final class ServerMenu {
	static final String PAGE = "shops";

	static void init() {
		AdminPages.add(new Extra("shops", () -> ShopsState.available() && ShopsState.may("admin"), screen -> new AdminShopsPage("shops", screen)));
	}

	static boolean openAdmin() {
		if (!ServerClient.adminAvailable()) {
			return false;
		} else {
			ServerClient.openAdmin("shops");
			return true;
		}
	}

	private ServerMenu() {
	}
}
