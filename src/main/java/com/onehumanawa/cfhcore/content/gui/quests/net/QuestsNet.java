package com.onehumanawa.cfhcore.content.gui.quests.net;

import com.onehumanawa.cfhcore.content.gui.core.CFHGui;
import com.onehumanawa.cfhcore.content.gui.core.net.Flow;
import com.onehumanawa.cfhcore.content.gui.core.net.CFHGuiNetwork;
import com.onehumanawa.cfhcore.content.gui.core.platform.CFHGuiPlatform;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestActions;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsClient;
import com.onehumanawa.cfhcore.content.gui.quests.client.QuestsState;
import com.onehumanawa.cfhcore.content.gui.quests.server.QuestsServer;
import com.onehumanawa.cfhcore.content.gui.quests.server.adv.AdvNet;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;

public final class QuestsNet {
	private static final int MAX_BYTES = 2097152;
	private static boolean registered;

	public static synchronized void register() {
		if (!registered) {
			registered = true;
			CFHGuiNetwork net = CFHGuiNetwork.get();
			net.register(QuestsNet.Action.TYPE, QuestsNet.Action.CODEC, Flow.C2S, (p, ctx) -> {
				if (ctx.sender() != null) {
					QuestsServer.onAction(ctx.sender(), p);
				}
			});
			net.register(QuestsNet.BookStart.TYPE, QuestsNet.BookStart.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.bookStart(p);
				}
			});
			net.register(QuestsNet.ChapterData.TYPE, QuestsNet.ChapterData.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.chapter(p);
				}
			});
			net.register(QuestsNet.ChapterGone.TYPE, QuestsNet.ChapterGone.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.chapterGone(p);
				}
			});
			net.register(QuestsNet.BookEnd.TYPE, QuestsNet.BookEnd.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.bookEnd(p);
				}
			});
			net.register(QuestsNet.Progress.TYPE, QuestsNet.Progress.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.progress(p);
				}
			});
			net.register(QuestsNet.Happened.TYPE, QuestsNet.Happened.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.happened(p);
				}
			});
			net.register(QuestsNet.Result.TYPE, QuestsNet.Result.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.result(p);
				}
			});
			net.register(QuestsNet.OpenBook.TYPE, QuestsNet.OpenBook.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.open(p);
				}
			});
			net.register(QuestsNet.Notice.TYPE, QuestsNet.Notice.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.notice(p);
				}
			});
			net.register(QuestsNet.Words.TYPE, QuestsNet.Words.CODEC, Flow.S2C, (p, ctx) -> {
				if (client()) {
					QuestsNet.ClientSide.words(p);
				}
			});
			AdvNet.register();
		}
	}

	private static boolean client() {
		return CFHGuiPlatform.get().isClient();
	}

	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		CFHGuiNetwork net = CFHGuiNetwork.get();
		if (net.canSendToPlayer(player, payload.type())) {
			net.sendToPlayer(player, payload);
		}
	}

	public static boolean has(ServerPlayer player) {
		return CFHGuiNetwork.get().canSendToPlayer(player, QuestsNet.BookStart.TYPE);
	}

	private QuestsNet() {
	}

	public static record Action(int request, String op, CompoundTag args) implements CustomPacketPayload {
		public static final Type<QuestsNet.Action> TYPE = new Type(CFHGui.id("quests_action"));
		public static final StreamCodec<ByteBuf, QuestsNet.Action> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			QuestsNet.Action::request,
			ByteBufCodecs.stringUtf8(48),
			QuestsNet.Action::op,
			ByteBufCodecs.TRUSTED_COMPOUND_TAG,
			QuestsNet.Action::args,
			QuestsNet.Action::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record BookEnd(long revision) implements CustomPacketPayload {
		public static final Type<QuestsNet.BookEnd> TYPE = new Type(CFHGui.id("quests_book_end"));
		public static final StreamCodec<ByteBuf, QuestsNet.BookEnd> CODEC = ByteBufCodecs.VAR_LONG.map(QuestsNet.BookEnd::new, QuestsNet.BookEnd::revision);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record BookStart(byte[] head, int chapters, boolean editor) implements CustomPacketPayload {
		public static final Type<QuestsNet.BookStart> TYPE = new Type(CFHGui.id("quests_book_start"));
		public static final StreamCodec<ByteBuf, QuestsNet.BookStart> CODEC = StreamCodec.composite(
			ByteBufCodecs.byteArray(2097152),
			QuestsNet.BookStart::head,
			ByteBufCodecs.VAR_INT,
			QuestsNet.BookStart::chapters,
			ByteBufCodecs.BOOL,
			QuestsNet.BookStart::editor,
			QuestsNet.BookStart::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record ChapterData(String id, byte[] chapter) implements CustomPacketPayload {
		public static final Type<QuestsNet.ChapterData> TYPE = new Type(CFHGui.id("quests_chapter"));
		public static final StreamCodec<ByteBuf, QuestsNet.ChapterData> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(64), QuestsNet.ChapterData::id, ByteBufCodecs.byteArray(2097152), QuestsNet.ChapterData::chapter, QuestsNet.ChapterData::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record ChapterGone(String id) implements CustomPacketPayload {
		public static final Type<QuestsNet.ChapterGone> TYPE = new Type(CFHGui.id("quests_chapter_gone"));
		public static final StreamCodec<ByteBuf, QuestsNet.ChapterGone> CODEC = ByteBufCodecs.stringUtf8(64)
			.map(QuestsNet.ChapterGone::new, QuestsNet.ChapterGone::id);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private static final class ClientSide {
		static void bookStart(QuestsNet.BookStart p) {
			QuestsState.onBookStart(p);
		}

		static void chapter(QuestsNet.ChapterData p) {
			QuestsState.onChapter(p);
		}

		static void chapterGone(QuestsNet.ChapterGone p) {
			QuestsState.onChapterGone(p);
		}

		static void bookEnd(QuestsNet.BookEnd p) {
			QuestsState.onBookEnd(p);
		}

		static void progress(QuestsNet.Progress p) {
			QuestsState.onProgress(p);
		}

		static void happened(QuestsNet.Happened p) {
			QuestsState.onHappened(p);
		}

		static void result(QuestsNet.Result p) {
			QuestActions.onResult(p);
		}

		static void notice(QuestsNet.Notice p) {
			QuestsState.onNotice(p);
		}

		static void words(QuestsNet.Words p) {
			QuestsState.onWords(p);
		}

		static void open(QuestsNet.OpenBook p) {
			QuestsClient.open(p.chapter().isEmpty() ? null : p.chapter(), p.quest().isEmpty() ? null : p.quest());
		}
	}

	public static record Happened(String kind, String subject, String who) implements CustomPacketPayload {
		public static final String QUEST_DONE = "quest";
		public static final String CHAPTER_DONE = "chapter";
		public static final String UNLOCKED = "unlocked";
		public static final Type<QuestsNet.Happened> TYPE = new Type(CFHGui.id("quests_happened"));
		public static final StreamCodec<ByteBuf, QuestsNet.Happened> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32),
			QuestsNet.Happened::kind,
			ByteBufCodecs.stringUtf8(160),
			QuestsNet.Happened::subject,
			ByteBufCodecs.stringUtf8(64),
			QuestsNet.Happened::who,
			QuestsNet.Happened::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Notice(String title, String text, String icon) implements CustomPacketPayload {
		public static final Type<QuestsNet.Notice> TYPE = new Type(CFHGui.id("quests_notice"));
		public static final StreamCodec<ByteBuf, QuestsNet.Notice> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(256),
			QuestsNet.Notice::title,
			ByteBufCodecs.stringUtf8(1024),
			QuestsNet.Notice::text,
			ByteBufCodecs.stringUtf8(512),
			QuestsNet.Notice::icon,
			QuestsNet.Notice::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record OpenBook(String chapter, String quest) implements CustomPacketPayload {
		public static final Type<QuestsNet.OpenBook> TYPE = new Type(CFHGui.id("quests_open"));
		public static final StreamCodec<ByteBuf, QuestsNet.OpenBook> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(64), QuestsNet.OpenBook::chapter, ByteBufCodecs.stringUtf8(160), QuestsNet.OpenBook::quest, QuestsNet.OpenBook::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Progress(boolean reset, byte[] states) implements CustomPacketPayload {
		public static final Type<QuestsNet.Progress> TYPE = new Type(CFHGui.id("quests_progress"));
		public static final StreamCodec<ByteBuf, QuestsNet.Progress> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, QuestsNet.Progress::reset, ByteBufCodecs.byteArray(2097152), QuestsNet.Progress::states, QuestsNet.Progress::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Result(int request, boolean ok, String code, List<String> args) implements CustomPacketPayload {
		public static final Type<QuestsNet.Result> TYPE = new Type(CFHGui.id("quests_result"));
		public static final StreamCodec<ByteBuf, QuestsNet.Result> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT,
			QuestsNet.Result::request,
			ByteBufCodecs.BOOL,
			QuestsNet.Result::ok,
			ByteBufCodecs.stringUtf8(64),
			QuestsNet.Result::code,
			ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(8)),
			QuestsNet.Result::args,
			QuestsNet.Result::new
		);

		public static QuestsNet.Result ok(int request) {
			return new QuestsNet.Result(request, true, "ok", List.of());
		}

		public static QuestsNet.Result fail(int request, String code, String... args) {
			return new QuestsNet.Result(request, false, code, List.of(args));
		}

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static record Words(String locale, byte[] words) implements CustomPacketPayload {
		public static final Type<QuestsNet.Words> TYPE = new Type(CFHGui.id("quests_words"));
		public static final StreamCodec<ByteBuf, QuestsNet.Words> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(32), QuestsNet.Words::locale, ByteBufCodecs.byteArray(2097152), QuestsNet.Words::words, QuestsNet.Words::new
		);

		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
