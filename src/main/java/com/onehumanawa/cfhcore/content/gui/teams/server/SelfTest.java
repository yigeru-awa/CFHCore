package com.onehumanawa.cfhcore.content.gui.teams.server;

import com.onehumanawa.cfhcore.content.gui.core.net.JsonWire;
import com.onehumanawa.cfhcore.content.gui.teams.CFHGuiTeams;
import com.onehumanawa.cfhcore.content.gui.teams.TeamsServerConfig;
import com.onehumanawa.cfhcore.content.gui.teams.model.Access;
import com.onehumanawa.cfhcore.content.gui.teams.model.BannerData;
import com.onehumanawa.cfhcore.content.gui.teams.model.ClaimsUpdate;
import com.onehumanawa.cfhcore.content.gui.teams.model.Privacy;
import com.onehumanawa.cfhcore.content.gui.teams.model.Rank;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamData;
import com.onehumanawa.cfhcore.content.gui.teams.model.TeamSummary;
import com.onehumanawa.cfhcore.content.gui.teams.server.platform.TeamsPlatform;
import com.onehumanawa.cfhcore.content.gui.teams.server.protect.LandGuard;
import com.onehumanawa.cfhcore.content.gui.teams.server.protect.Protection;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class SelfTest {
	private static final String OVERWORLD = "minecraft:overworld";
	private final TeamService s;
	private final TeamsServerConfig rules;
	private final List<String> trace = new ArrayList<>();
	private long time = 1000000000000L;
	private int passed;
	private int failed;

	private SelfTest(MinecraftServer server) {
		this.rules = new TeamsServerConfig();
		this.rules.maxMembers = 3;
		this.rules.maxAllies = 1;
		this.rules.maxWaypoints = 2;
		this.rules.inviteMinutes = 30;
		this.rules.baseClaims = 2;
		this.rules.claimsPerMember = 1;
		this.rules.claimsPerLevel = 1;
		this.rules.maxClaims = 6;
		this.rules.baseForceLoads = 1;
		this.rules.forceLoadsPerLevel = 1;
		this.rules.maxForceLoads = 2;
		this.rules.claimReach = 4;
		this.s = TeamService.scratch(server, this.rules);
		this.s.clock(() -> this.time);
		this.s.trace(this.trace);
	}

	static SelfTest.Outcome run(MinecraftServer server, @Nullable ServerPlayer player) {
		SelfTest t = new SelfTest(server);

		try {
			t.script();
			if (player != null) {
				t.hooks(player);
			} else {
				CFHGuiTeams.LOGGER.info("[CFHGui Teams] selftest: run by the console, so the protection hooks were not tried on a player");
			}
		} catch (RuntimeException var4) {
			t.failed++;
			CFHGuiTeams.LOGGER.error("[CFHGui Teams] selftest FAILED: the script itself broke", var4);
		}

		CFHGuiTeams.LOGGER.info("[CFHGui Teams] selftest: {} passed, {} failed", t.passed, t.failed);
		return new SelfTest.Outcome(t.passed, t.failed);
	}

	private void hooks(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos pos = player.blockPosition();
		BlockPos outside = pos.offset(16, 0, 0);
		String dim = level.dimension().location().toString();
		Actor frank = Actor.fake(UUID.randomUUID(), "frank", dim, pos.getX() >> 4, pos.getZ() >> 4);
		this.s.store.remember(frank.id(), "frank");
		this.expect("founding a team to guard", this.s.create(frank, "Hookland", "HK", null), "ok");
		TeamData land = this.s.store.ofPlayer(frank.id());
		if (land != null) {
			long here = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
			ClaimMap.Claim taken = this.s.claims.at(dim, here);
			if (taken != null) {
				this.s.free(dim, here, taken);
			}

			ClaimMap.Claim beside = this.s.claims.at(dim, ChunkPos.asLong(pos.getX() + 16 >> 4, pos.getZ() >> 4));
			if (beside != null) {
				this.s.free(dim, ChunkPos.asLong(pos.getX() + 16 >> 4, pos.getZ() >> 4), beside);
			}

			this.expect("claiming where the player stands", this.s.claim(frank, null, new long[]{here}), "ok");
			boolean bypassBefore = this.rules.operatorsBypass;
			this.rules.operatorsBypass = false;
			TeamsServer.standIn(
				this.s,
				() -> {
					this.check("hook: a stranger's break is refused", LandGuard.breakRefused(player, level, pos));
					this.check("hook: the loader's own break event refuses it", !TeamsPlatform.get().breakAllowed(player, level, pos, level.getBlockState(pos)));
					this.check("hook: the loader's break event allows it next door", TeamsPlatform.get().breakAllowed(player, level, outside, Blocks.AIR.defaultBlockState()));
					this.check("hook: a stranger's block is not placed", LandGuard.placeRefused(player, level, pos) && !LandGuard.placeRefused(player, level, outside));
					this.check("hook: a stranger does not start on a block", LandGuard.attackBlockRefused(player, level, pos));
					BlockHitResult click = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
					this.check("hook: a stranger's click on a block is refused", (LandGuard.useBlock(player, level, InteractionHand.MAIN_HAND, click) & 1) != 0);
					this.check("hook: a stranger does not trample or trigger", LandGuard.trampleRefused(level, pos, player) && LandGuard.stepRefused(level, pos, player));
					List<BlockPos> blast = new ArrayList<>(List.of(pos, pos.above(), outside));
					LandGuard.filterExplosion(level, blast, null);
					this.check("hook: an explosion spares the land and nothing else", blast.size() == 1 && blast.get(0).equals(outside));
					this.check(
						"hook: fire and mobs are kept off the land",
						LandGuard.fireRefused(level, pos)
							&& LandGuard.mobGriefRefused(level, pos)
							&& !LandGuard.fireRefused(level, outside)
							&& !LandGuard.mobGriefRefused(level, outside)
					);
					this.check(
						"hook: nothing flows in from outside, anything may flow out",
						LandGuard.reachRefused(level, outside, pos) && !LandGuard.reachRefused(level, pos, outside) && !LandGuard.reachRefused(level, pos, pos.above())
					);
					this.check("hook: the player is not taken for a machine", !TeamsPlatform.get().isFakePlayer(player));
					this.check("mixin: the classes behind the rarer hooks load with their mixins on", mixinsApply(level));
					this.check("mixin: the game's own 'may this player change that block' says no", !player.mayInteract(level, pos));
					Entity cow = EntityType.COW.create(level);
					if (cow != null) {
						cow.setPos(player.getX(), player.getY(), player.getZ());
						this.check("mixin: a stranger's blow does not reach the team's animals", cow.isInvulnerableTo(level.damageSources().playerAttack(player)));
					}

					boolean emptyHanded = player.getMainHandItem().isEmpty();
					if (emptyHanded) {
						this.check(
							"mixin: an item is not used on the team's blocks",
							player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, click)) == InteractionResult.FAIL
						);
					}

					this.s.setLandRule(frank, "explosions", "true");
					List<BlockPos> allowed = new ArrayList<>(List.of(pos, outside));
					LandGuard.filterExplosion(level, allowed, null);
					this.check("hook: with the rule on, explosions take the land too", allowed.size() == 2);
					if (player.hasPermissions(2) && (player.isCreative() || player.isSpectator())) {
						this.rules.operatorsBypass = true;
						this.check(
							"hook: an operator in creative passes when the server says so",
							!LandGuard.breakRefused(player, level, pos) && TeamsPlatform.get().breakAllowed(player, level, pos, level.getBlockState(pos))
						);
						this.rules.operatorsBypass = false;
					}

					TeamData.Member member = new TeamData.Member();
					member.id = player.getUUID();
					member.name = player.getGameProfile().getName();
					this.s.store.addMember(land, member);
					this.check(
						"hook: a member may do all of it",
						!LandGuard.breakRefused(player, level, pos)
							&& !LandGuard.placeRefused(player, level, pos)
							&& LandGuard.useBlock(player, level, InteractionHand.MAIN_HAND, click) == 0
							&& !LandGuard.trampleRefused(level, pos, player)
							&& TeamsPlatform.get().breakAllowed(player, level, pos, level.getBlockState(pos))
					);
					if (cow != null) {
						this.check("mixin: a member's blow reaches them", !cow.isInvulnerableTo(level.damageSources().playerAttack(player)));
					}

					long day = 86400000L;
					this.rules.claimExpiryDays = 7;
					land.lastSeen = this.time - 7776000000L;
					this.s.expiry.run();
					this.check(
						"expiry: a member who is on keeps the team's land, and the team is seen now",
						this.s.claims.count(land.id) == 1
							&& land.lastSeen == this.time
							&& this.s.expiry.standings().stream().anyMatch(st -> st.team() == land && st.online() && st.due() == 0L)
					);
					this.s.store.removeMember(land, player.getUUID());
					land.lastSeen = this.time - 7776000000L;
					this.check("expiry: with nobody on, the same team's land goes", this.s.expiry.run().contains(land) && this.s.claims.count(land.id) == 0);
					this.rules.claimExpiryDays = 0;
					Actor operator = frank.asAdmin();

					for (String rule : new String[]{"build", "interact", "containers", "entities"}) {
						this.s.ruleServerLand(operator, rule, "nobody");
					}

					for (String rule : new String[]{"explosions", "mobGriefing", "fireSpread", "pvp"}) {
						this.s.ruleServerLand(operator, rule, "false");
					}

					this.expect("claiming where the player stands for the server", this.s.claimServerLand(operator, null, new long[]{here}), "ok");
					this.check(
						"hook: server land refuses the player's break, through the loader's own event too",
						LandGuard.breakRefused(player, level, pos)
							&& !TeamsPlatform.get().breakAllowed(player, level, pos, level.getBlockState(pos))
							&& LandGuard.placeRefused(player, level, pos)
					);
					this.check(
						"hook: and their click, their step and their blow",
						(LandGuard.useBlock(player, level, InteractionHand.MAIN_HAND, click) & 1) != 0
							&& LandGuard.stepRefused(level, pos, player)
							&& !player.mayInteract(level, pos)
							&& (cow == null || cow.isInvulnerableTo(level.damageSources().playerAttack(player)))
					);
					List<BlockPos> spared = new ArrayList<>(List.of(pos, outside));
					LandGuard.filterExplosion(level, spared, null);
					this.check(
						"hook: server land is spared by explosions, fire and mobs",
						spared.size() == 1 && LandGuard.fireRefused(level, pos) && LandGuard.mobGriefRefused(level, pos) && LandGuard.reachRefused(level, outside, pos)
					);
					this.s.ruleServerLand(operator, "build", "everyone");
					this.check(
						"hook: opened to everyone, server land lets the player build and still not open things",
						!LandGuard.breakRefused(player, level, pos)
							&& TeamsPlatform.get().breakAllowed(player, level, pos, level.getBlockState(pos))
							&& player.mayInteract(level, pos)
							&& (LandGuard.useBlock(player, level, InteractionHand.MAIN_HAND, click) & 1) != 0
					);
					this.s.ruleServerLand(operator, "build", "nobody");
					if (player.hasPermissions(2) && (player.isCreative() || player.isSpectator())) {
						this.rules.operatorsBypass = true;
						this.check(
							"hook: an operator in creative passes on server land as on any",
							!LandGuard.breakRefused(player, level, pos) && TeamsPlatform.get().breakAllowed(player, level, pos, level.getBlockState(pos))
						);
						this.rules.operatorsBypass = false;
					}
				}
			);
			this.rules.operatorsBypass = bypassBefore;
			this.check("hook: the server's own teams are back in place", TeamsServer.service() != this.s);
		}
	}

	private static boolean mixinsApply(ServerLevel level) {
		try {
			List<Object> touched = new ArrayList<>();
			touched.add(Explosion.class.getName());
			touched.add(RemoveBlockGoal.class.getName());
			touched.add(BreakDoorGoal.class.getName());
			touched.add(EntityType.ENDERMAN.create(level));
			touched.add(EntityType.ZOMBIE.create(level));
			return !touched.contains(null);
		} catch (Throwable var2) {
			CFHGuiTeams.LOGGER.error("[CFHGui Teams] selftest: a protection mixin did not apply", var2);
			return false;
		}
	}

	private void check(String what, boolean holds) {
		if (holds) {
			this.passed++;
		} else {
			this.failed++;
			CFHGuiTeams.LOGGER.error("[CFHGui Teams] selftest FAILED: {}", what);
		}
	}

	private OpResult expect(String what, OpResult r, String code) {
		if (r.code().equals(code)) {
			this.passed++;
			return r;
		} else {
			this.failed++;
			CFHGuiTeams.LOGGER.error("[CFHGui Teams] selftest FAILED: {}: expected {}, got {} {}", new Object[]{what, code, r.code(), r.args()});
			return r;
		}
	}

	private Actor actor(String name) {
		Actor a = Actor.fake(UUID.randomUUID(), name, "minecraft:overworld", 0, 0);
		this.s.store.remember(a.id(), name);
		return a;
	}

	private boolean traced(String line) {
		return this.trace.contains(line);
	}

	private static long chunk(int x, int z) {
		return ChunkPos.asLong(x, z);
	}

	private static long[] chunks(int... xz) {
		long[] out = new long[xz.length / 2];

		for (int i = 0; i < out.length; i++) {
			out[i] = ChunkPos.asLong(xz[i * 2], xz[i * 2 + 1]);
		}

		return out;
	}

	private void script() {
		Actor alice = this.actor("alice");
		Actor bob = this.actor("bob");
		Actor carol = this.actor("carol");
		Actor dave = this.actor("dave");
		Actor erin = this.actor("erin");
		this.expect("a name of two letters", this.s.create(alice, "ab", "", null), "name_invalid");
		this.expect("a name with a sign in it", this.s.create(alice, "bad!name", "", null), "name_invalid");
		this.expect("a name of 25 letters", this.s.create(alice, "abcdefghijklmnopqrstuvwxy", "", null), "name_invalid");
		this.expect("a name of signs only", this.s.create(alice, "- - -", "", null), "name_invalid");
		this.expect("a tag of one letter", this.s.create(alice, "Emberwatch", "x", null), "tag_invalid");
		this.expect("a tag of six letters", this.s.create(alice, "Emberwatch", "toolng", null), "tag_invalid");
		this.expect("a tag with a space", this.s.create(alice, "Emberwatch", "a b", null), "tag_invalid");
		this.expect("founding a team", this.s.create(alice, "  Emberwatch ", "EMB", 316244062), "ok");
		TeamData ember = this.s.store.ofPlayer(alice.id());
		this.check("the founder is in the team", ember != null);
		if (ember != null) {
			this.check("the name was trimmed", ember.name.equals("Emberwatch"));
			this.check("the colour is RGB", ember.color == 14254174);
			this.check("the founder is the owner", ember.rankOf(alice.id()) == Rank.OWNER);
			this.check("founding is in the log", !ember.log.isEmpty() && ember.log.get(0).type.equals("created"));
			this.check(
				"founding fired created, then joined",
				this.trace.indexOf("event created Emberwatch") >= 0 && this.trace.indexOf("event created Emberwatch") < this.trace.indexOf("event joined alice")
			);
			this.expect("founding a second team", this.s.create(alice, "Another", "", null), "already_in_team");
			this.expect("a name that differs only in case", this.s.create(bob, "emberWATCH", "", null), "name_taken");
			this.expect("a tag that differs only in case", this.s.create(bob, "Mossback", "emb", null), "tag_taken");
			this.expect("founding a second team by another", this.s.create(bob, "Mossback", "MOS", null), "ok");
			TeamData moss = this.s.store.ofPlayer(bob.id());
			if (moss == null) {
				this.check("the second team exists", false);
			} else {
				this.check("a team without a colour gets one", moss.color != 0 && (moss.color & 0xFF000000) == 0);
				this.rules.playersCreateTeams = false;
				this.expect("founding where only operators may", this.s.create(carol, "Carols", "", null), "disabled");
				this.rules.playersCreateTeams = true;
				this.expect("the console founding a team", this.s.create(Actor.console("Server"), "Consoles", "", null), "player_only");
				this.expect("renaming by a stranger", this.s.rename(carol, "Whatever"), "not_in_team");
				this.expect("renaming to a taken name", this.s.rename(alice, "mossback"), "name_taken");
				this.expect("renaming", this.s.rename(alice, "Ember Watch"), "ok");
				this.check("the new name finds the team", this.s.store.named("ember  watch") == ember);
				this.check("the old name is free", this.s.store.named("Emberwatch") == null);
				this.expect("renaming back", this.s.rename(alice, "Emberwatch"), "ok");
				this.expect("taking the tag off", this.s.setTag(alice, ""), "ok");
				this.check("a tag taken off is free", this.s.store.tagged("EMB") == null);
				this.expect("a new tag", this.s.setTag(alice, "EW"), "ok");
				this.expect("another team's tag", this.s.setTag(alice, "mos"), "tag_taken");
				this.expect("a colour", this.s.setColor(alice, -13408615), "ok");
				this.check("the colour is kept as RGB", ember.color == 3368601);
				this.expect("a motto", this.s.setMotto(alice, "x".repeat(300) + "§k"), "ok");
				this.check("the motto is cut to its length", ember.motto.length() == 96 && ember.motto.indexOf(167) < 0);
				this.expect("no privacy at all", this.s.setPrivacy(alice, null), "invalid");
				this.expect("friendly fire", this.s.setFriendlyFire(alice, true), "ok");
				this.check("friendly fire is on", ember.friendlyFire);
				BannerData banner = new BannerData("no_such_dye");
				banner.layers.add(new BannerData.Layer("not an id", "red"));
				banner.layers.add(new BannerData.Layer("minecraft:skull", "no_such_dye"));
				banner.layers.add(new BannerData.Layer("cfhgui_teams:no_such_pattern", "red"));

				for (int i = 0; i < 3; i++) {
					banner.layers.add(new BannerData.Layer("minecraft:creeper", "RED"));
				}

				this.expect("a banner", this.s.setBanner(alice, banner), "ok");
				this.check(
					"the banner was mended",
					ember.banner.base.equals("white")
						&& ember.banner.layers.size() == 3
						&& ember.banner.layers.stream().allMatch(l -> l.color.equals("red") && l.pattern.equals("minecraft:creeper"))
				);
				this.expect("a land rule that is none", this.s.setLandRule(alice, "nonsense", "true"), "invalid");
				this.expect("a land rule with a value it cannot have", this.s.setLandRule(alice, "build", "sometimes"), "invalid");
				this.expect("a land rule, spelled loosely", this.s.setLandRule(alice, "mob_griefing", "on"), "ok");
				this.check("the land rule took", ember.land.mobGriefing);
				this.expect("the land rule back", this.s.setLandRule(alice, "mobGriefing", "false"), "ok");
				this.expect("inviting someone nobody knows", this.s.invite(alice, null, "nobody_at_all"), "no_such_player");
				this.expect("inviting by id", this.s.invite(alice, carol.id(), null), "ok");
				this.check("the invited player sees the invitation", this.s.invitesOf(carol.id()).size() == 1 && this.s.invitesOf(carol.id()).get(0).team.equals(ember.id));
				this.check("the invited player is told", this.traced("notice invited carol"));
				this.expect("inviting twice", this.s.invite(alice, carol.id(), null), "already_invited");
				this.expect("inviting a member", this.s.invite(alice, alice.id(), null), "already_member");
				this.expect("a second team inviting by name", this.s.invite(bob, null, "CAROL"), "ok");
				this.check("two invitations are open", this.s.invitesOf(carol.id()).size() == 2);
				this.expect("accepting", this.s.acceptInvite(carol, ember.id), "ok");
				this.check("the new member is a member", ember.rankOf(carol.id()) == Rank.MEMBER);
				this.check("joining answered the other invitation", this.s.invitesOf(carol.id()).isEmpty() && moss.invites.isEmpty() && ember.invites.isEmpty());
				this.check("the team hears of the new member", this.traced("notice joined alice"));
				this.expect("accepting what is gone", this.s.acceptInvite(carol, moss.id), "not_invited");
				this.expect("a plain member inviting", this.s.invite(carol, dave.id(), null), "no_permission");
				this.expect("inviting again", this.s.invite(alice, dave.id(), null), "ok");
				this.time += 1860000L;
				this.expect("accepting after it ran out", this.s.acceptInvite(dave, ember.id), "not_invited");
				this.expect("inviting once more", this.s.invite(alice, erin.id(), null), "ok");
				this.time += 1860000L;
				this.s.sweep();
				this.check("an invitation that ran out is swept away", ember.invites.isEmpty() && this.s.store.invitersOf(erin.id()).isEmpty());
				this.expect("inviting to decline", this.s.invite(alice, dave.id(), null), "ok");
				this.expect("declining", this.s.declineInvite(dave, ember.id), "ok");
				this.check("the one who invited hears of the decline", this.traced("notice declined alice"));
				this.expect("declining twice", this.s.declineInvite(dave, ember.id), "not_invited");
				this.expect("inviting to take it back", this.s.invite(alice, dave.id(), null), "ok");
				this.expect("taking an invitation back", this.s.cancelInvite(alice, dave.id()), "ok");
				this.expect("accepting what was taken back", this.s.acceptInvite(dave, ember.id), "not_invited");
				this.expect("joining an invite-only team", this.s.join(dave, ember.id), "not_invited");
				this.expect("joining a team that is none", this.s.join(dave, UUID.randomUUID()), "no_such_team");
				this.expect("opening the team", this.s.setPrivacy(alice, Privacy.OPEN), "ok");
				OpResult r = this.expect("joining an open team", this.s.join(dave, ember.id), "ok");
				this.check("joining says joined", !r.args().isEmpty() && r.args().get(0).equals("joined"));
				this.expect("joining a full team", this.s.join(erin, ember.id), "team_full");
				this.expect("inviting into a full team", this.s.invite(alice, erin.id(), null), "team_full");
				this.expect("a plain member kicking", this.s.kick(carol, dave.id()), "no_permission");
				this.expect("kicking", this.s.kick(alice, dave.id()), "ok");
				this.check("the kicked player is out and told", this.s.store.ofPlayer(dave.id()) == null && this.traced("notice kicked dave"));
				this.expect("kicking a stranger", this.s.kick(alice, dave.id()), "not_a_member");
				this.expect("kicking oneself", this.s.kick(alice, alice.id()), "not_yourself");
				this.expect("taking requests", this.s.setPrivacy(alice, Privacy.REQUEST), "ok");
				r = this.expect("asking to join", this.s.join(dave, ember.id), "ok");
				this.check("asking says requested", !r.args().isEmpty() && r.args().get(0).equals("requested"));
				this.check(
					"the request is open", ember.requests.size() == 1 && this.s.store.askedBy(dave.id()).contains(ember.id) && this.s.store.ofPlayer(dave.id()) == null
				);
				this.check("those who may answer are told", this.traced("notice request alice") && !this.traced("notice request carol"));
				this.expect("asking twice", this.s.join(dave, ember.id), "already_requested");
				this.expect("a plain member answering a request", this.s.acceptRequest(carol, dave.id()), "no_permission");
				this.expect("denying a request", this.s.denyRequest(alice, dave.id()), "ok");
				this.check("a denied request is gone", ember.requests.isEmpty() && this.s.store.askedBy(dave.id()).isEmpty());
				this.expect("denying it twice", this.s.denyRequest(alice, dave.id()), "no_request");
				this.expect("asking again", this.s.join(dave, ember.id), "ok");
				this.expect("withdrawing the request", this.s.cancelRequest(dave, ember.id), "ok");
				this.expect("withdrawing it twice", this.s.cancelRequest(dave, ember.id), "no_request");
				this.expect("asking a third time", this.s.join(dave, ember.id), "ok");
				this.expect("accepting a request", this.s.acceptRequest(alice, dave.id()), "ok");
				this.check("the accepted player is a member", ember.rankOf(dave.id()) == Rank.MEMBER && ember.requests.isEmpty());
				this.expect("a member asking elsewhere", this.s.join(dave, moss.id), "already_in_team");
				this.expect("promoting a member", this.s.promote(alice, carol.id()), "ok");
				this.check("a promoted member is a moderator", ember.rankOf(carol.id()) == Rank.MODERATOR);
				this.expect("promoting a moderator", this.s.promote(alice, carol.id()), "ok");
				this.expect("promoting an admin", this.s.promote(alice, carol.id()), "cannot_promote");
				this.expect("an admin promoting a member", this.s.promote(carol, dave.id()), "ok");
				this.expect("an admin making another admin", this.s.promote(carol, dave.id()), "rank_too_low");
				this.expect("a moderator promoting", this.s.promote(dave, carol.id()), "no_permission");
				this.expect("a moderator kicking an admin", this.s.kick(dave, carol.id()), "rank_too_low");
				this.expect("an admin demoting the owner", this.s.demote(carol, alice.id()), "rank_too_low");
				this.expect("demoting oneself", this.s.demote(carol, carol.id()), "not_yourself");
				this.expect("demoting a moderator", this.s.demote(carol, dave.id()), "ok");
				this.expect("demoting a member", this.s.demote(carol, dave.id()), "cannot_demote");
				this.expect("the owner leaving", this.s.leave(alice), "owner_must_transfer");
				this.expect("an admin handing the team over", this.s.transfer(carol, dave.id()), "no_permission");
				this.expect("handing the team to a stranger", this.s.transfer(alice, erin.id()), "not_a_member");
				this.expect("handing the team over", this.s.transfer(alice, carol.id()), "ok");
				this.check(
					"there is one owner, and it is the new one",
					ember.rankOf(carol.id()) == Rank.OWNER && ember.rankOf(alice.id()) == Rank.ADMIN && ember.members.stream().filter(m -> m.rank == Rank.OWNER).count() == 1L
				);
				this.expect("the old owner leaving", this.s.leave(alice), "ok");
				this.check(
					"the one who left is out, and it was announced",
					this.s.store.ofPlayer(alice.id()) == null && this.traced("event left alice") && this.traced("notice left carol")
				);
				this.expect("leaving without a team", this.s.leave(alice), "not_in_team");
				this.expect("an ally declared by a plain member", this.s.ally(dave, moss.id), "no_permission");
				this.expect("declaring oneself an ally", this.s.ally(carol, ember.id), "not_yourself");
				this.expect("declaring a team that is none", this.s.ally(carol, UUID.randomUUID()), "no_such_team");
				this.expect("declaring an ally", this.s.ally(carol, moss.id), "ok");
				this.check("the declared team is told", this.traced("notice ally bob"));
				this.expect("declaring it twice", this.s.ally(carol, moss.id), "already_ally");
				Protection p = this.s.protection;
				this.check("our ally is an ally on our land", p.relation(ember, bob.id()) == Access.Relation.ALLY);
				this.check("we are no ally on theirs until they say so", p.relation(moss, carol.id()) == Access.Relation.OUTSIDER);
				this.check("a member is a member", p.relation(ember, dave.id()) == Access.Relation.MEMBER);
				this.expect("founding a third team", this.s.create(erin, "Tidewater", "", null), "ok");
				TeamData tide = this.s.store.ofPlayer(erin.id());
				if (tide == null) {
					this.check("the third team exists", false);
				} else {
					this.expect("one ally too many", this.s.ally(carol, tide.id), "ally_limit");
					this.expect("the other side answering in kind", this.s.ally(bob, ember.id), "ok");
					this.check("an alliance both ways is announced as such", this.traced("notice ally_mutual carol"));
					this.check("the limit follows the members", this.s.claimLimit(ember) == 4 && this.s.claimLimit(null) == 3);
					this.rules.claims = false;
					this.expect("claiming where land cannot be claimed", this.s.claim(carol, null, chunks(0, 0)), "claims_disabled");
					this.rules.claims = true;
					this.expect("claiming in a dimension that is none", this.s.claim(carol, "minecraft:nowhere_at_all", chunks(0, 0)), "wrong_dimension");
					this.rules.claimDimensions.add("minecraft:the_nether");
					this.expect("claiming in a dimension the server left out", this.s.claim(carol, null, chunks(0, 0)), "wrong_dimension");
					this.rules.claimDimensions.clear();
					this.expect("a plain member claiming", this.s.claim(dave, null, chunks(0, 0)), "no_permission");
					this.rules.soloLand = false;
					this.expect("a stranger claiming where only teams may claim", this.s.claim(alice, null, chunks(0, 0)), "not_in_team");
					this.check("a stranger's refused claim leaves nothing behind", this.s.store.ofPlayer(alice.id()) == null && this.s.ownerOf(Level.OVERWORLD, 0, 0) == null);
					this.rules.soloLand = true;
					r = this.expect("claiming a chunk", this.s.claim(carol, null, chunks(0, 0)), "ok");
					this.check("one taken, none left", r.number(0) == 1 && r.number(1) == 0);
					this.check("the chunk is the team's", this.s.ownerOf(Level.OVERWORLD, 0, 0) == ember && this.s.claims.count(ember.id) == 1);
					this.expect("claiming it again", this.s.claim(carol, null, chunks(0, 0)), "already_claimed");
					r = this.expect("another team claiming it", this.s.claim(bob, null, chunks(0, 0)), "already_claimed");
					this.check("the answer names the holder", !r.args().isEmpty() && r.args().get(0).equals("Emberwatch"));
					this.expect("claiming out of reach", this.s.claim(carol, null, chunks(9, 0)), "too_far");
					this.expect("claiming in another dimension from here", this.s.claim(carol, "minecraft:the_nether", chunks(0, 0)), "too_far");
					this.expect("an operator claiming out of reach", this.s.claim(carol.asAdmin(), null, chunks(9, 0)), "ok");
					this.expect("giving that one up", this.s.unclaim(carol, null, chunks(9, 0)), "ok");
					r = this.expect("claiming more than the limit", this.s.claim(carol, null, chunks(1, 0, 2, 0, 0, 0, 3, 0, 4, 0)), "ok");
					this.check("it took what it could and said so", r.number(0) == 3 && r.number(1) == 2 && this.s.claims.count(ember.id) == 4);
					r = this.expect("claiming at the limit", this.s.claim(carol, null, chunks(0, 1)), "claim_limit");
					this.check("the answer names the limit", !r.args().isEmpty() && r.args().get(0).equals("4"));
					this.expect("giving up land that is not ours", this.s.unclaim(carol, null, chunks(7, 7)), "not_claimed");
					r = this.expect("giving up land", this.s.unclaim(carol, null, chunks(3, 0, 7, 7)), "ok");
					this.check(
						"one given up, one left", r.number(0) == 1 && r.number(1) == 1 && this.s.claims.count(ember.id) == 3 && this.s.ownerOf(Level.OVERWORLD, 3, 0) == null
					);
					this.expect("giving two more up", this.s.unclaim(carol, null, chunks(1, 0, 2, 0)), "ok");
					this.rules.claimsMustTouch = true;
					this.expect("land that does not touch ours", this.s.claim(carol, null, chunks(0, 3)), "must_touch");
					r = this.expect("a strip handed in from the far end", this.s.claim(carol, null, chunks(0, 3, 0, 2, 0, 1)), "ok");
					this.check("the whole strip was taken", r.number(0) == 3 && this.s.claims.count(ember.id) == 4);
					this.rules.claimsMustTouch = false;
					this.expect("another team beside it", this.s.claim(bob, null, chunks(1, 0)), "ok");
					List<ClaimsUpdate> whole = this.s.claims.whole();
					this.check(
						"a joining client is sent every claim",
						whole.size() == 1
							&& whole.get(0).reset()
							&& whole.get(0).dimension().equals("minecraft:overworld")
							&& whole.get(0).chunks().length == 5
							&& whole.get(0).teams().size() == 2
					);
					List<ClaimsUpdate> changes = this.s.claims.drain();
					this.check("what changed is told once", changes.size() == 1 && !changes.get(0).reset() && this.s.claims.drain().isEmpty());
					this.check("the loaded limit follows the level", this.s.forceLimit(ember) == 1);
					this.expect("keeping land loaded that is not ours", this.s.forceLoad(carol, null, chunks(1, 0), true), "not_claimed");
					this.expect("keeping a chunk loaded", this.s.forceLoad(carol, null, chunks(0, 0), true), "ok");
					r = this.expect("one chunk too many", this.s.forceLoad(carol, null, chunks(0, 1), true), "force_load_limit");
					this.check("the answer names the loaded limit", !r.args().isEmpty() && r.args().get(0).equals("1"));
					this.check("one chunk is kept loaded", this.s.claims.forced(ember.id) == 1);
					this.s.addXp(ember, TeamData.xpForLevel(1) + 5L);
					this.check("experience raised the level and kept the rest", ember.level == 2 && ember.xp == 5L && this.traced("notice level carol"));
					this.check("a level is more land and more loaded chunks", this.s.claimLimit(ember) == 5 && this.s.forceLimit(ember) == 2);
					this.expect("a second loaded chunk at level two", this.s.forceLoad(carol, null, chunks(0, 1), true), "ok");
					this.expect("letting one go", this.s.forceLoad(carol, null, chunks(0, 0), false), "ok");
					this.check("giving land up lets its chunk go", this.s.unclaim(carol, null, chunks(0, 1)).ok() && this.s.claims.forced(ember.id) == 0);
					this.check("a member may build", p.refuses(dave.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == null);
					this.check("a stranger may not build", p.refuses(erin.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == ember);
					this.check("a stranger may not use a door", p.refuses(erin.id(), Level.OVERWORLD, 0, 0, Protection.Kind.INTERACT) == ember);
					this.check("an ally may use a door", p.refuses(bob.id(), Level.OVERWORLD, 0, 0, Protection.Kind.INTERACT) == null);
					this.check("an ally may not build", p.refuses(bob.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == ember);
					this.check("an ally may not open a chest", p.refuses(bob.id(), Level.OVERWORLD, 0, 0, Protection.Kind.CONTAINERS) == ember);
					this.check("an ally may not hurt the animals", p.refuses(bob.id(), Level.OVERWORLD, 0, 0, Protection.Kind.ENTITIES) == ember);
					this.check("land nobody holds refuses nothing", p.refuses(erin.id(), Level.OVERWORLD, 40, 40, Protection.Kind.BUILD) == null);
					this.check("the same chunk elsewhere is nobody's", p.refuses(erin.id(), Level.NETHER, 0, 0, Protection.Kind.BUILD) == null);
					this.expect("letting everyone open chests", this.s.setLandRule(carol, "containers", "EVERYONE"), "ok");
					this.check("a stranger may open a chest then", p.refuses(erin.id(), Level.OVERWORLD, 0, 0, Protection.Kind.CONTAINERS) == null);
					this.expect("letting allies build", this.s.setLandRule(carol, "build", "allies"), "ok");
					this.check(
						"an ally may build then, a stranger still not",
						p.refuses(bob.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == null && p.refuses(erin.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == ember
					);
					ClaimMap.Claim ours = this.s.claims.at(Level.OVERWORLD, 0, 0);
					ClaimMap.Claim theirs = this.s.claims.at(Level.OVERWORLD, 1, 0);
					this.check(
						"water may flow within a team's land, and in from an ally who may build",
						ours != null && theirs != null && p.mayReachInto(ours, ours) && p.mayReachInto(theirs, ours)
					);
					this.check(
						"nothing reaches in from land nobody holds, nor into land whose team lets only members build",
						ours != null && theirs != null && !p.mayReachInto(null, ours) && !p.mayReachInto(ours, theirs)
					);
					this.check(
						"explosions, mobs and fire are kept out unless the team says otherwise",
						!this.s.pins().explosions(ember) && !this.s.pins().mobGriefing(ember) && !this.s.pins().fireSpread(ember) && this.s.pins().pvp(ember)
					);
					this.rules.pinnedLandRules.add("build=MEMBERS");
					this.rules.pinnedLandRules.add("pvp = false");
					this.s.repin();
					this.check(
						"a pinned rule overrides the team's",
						p.refuses(bob.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == ember && !this.s.pins().pvp(ember) && this.s.pins().names().size() == 2
					);
					this.check("the team's own choice is kept under the pin", ember.land.build == Access.ALLIES && ember.land.pvp);
					this.expect("changing a pinned rule", this.s.setLandRule(carol, "build", "everyone"), "pinned");
					this.rules.pinnedLandRules.clear();
					this.s.repin();
					this.check("the team's rule holds again once the pin is gone", p.refuses(bob.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == null);
					this.expect("ending the alliance", this.s.unally(carol, moss.id), "ok");
					this.check("without the declaration the other is a stranger again", p.refuses(bob.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == ember);
					this.expect("ending it twice", this.s.unally(carol, moss.id), "not_ally");
					r = this.expect("a waypoint", this.s.addWaypoint(dave, " Mine ", 10, 64, -20, null, -15654349, "Mine"), "ok");
					String wp = r.args().size() > 1 ? r.args().get(1) : "";
					this.check(
						"the waypoint is kept as given",
						ember.waypoint(wp) != null
							&& ember.waypoint(wp).name.equals("Mine")
							&& ember.waypoint(wp).color == 1122867
							&& ember.waypoint(wp).dimension.equals("minecraft:overworld")
							&& ember.waypoint(wp).icon.equals("mine")
					);
					this.expect("a waypoint without a name", this.s.addWaypoint(dave, "  ", 0, 0, 0, null, null, null), "invalid");
					r = this.expect("a second waypoint", this.s.addWaypoint(carol, "Gate", 0, 70, 0, "minecraft:the_nether", null, null), "ok");
					String gate = r.args().size() > 1 ? r.args().get(1) : "";
					this.expect("one waypoint too many", this.s.addWaypoint(carol, "Third", 0, 0, 0, null, null, null), "waypoint_limit");
					CompoundTag move = new CompoundTag();
					move.putInt("y", 80);
					move.putString("name", "The Gate");
					this.expect("a member changing another's waypoint", this.s.editWaypoint(dave, gate, move), "no_permission");
					this.expect("a member changing their own", this.s.editWaypoint(dave, wp, move), "ok");
					this.check("the change took", ember.waypoint(wp) != null && ember.waypoint(wp).y == 80 && ember.waypoint(wp).name.equals("The Gate"));
					this.expect("a waypoint that is none", this.s.removeWaypoint(carol, "nope"), "no_such_waypoint");
					this.expect("the owner removing a member's waypoint", this.s.removeWaypoint(carol, wp), "ok");
					this.expect("a home set by nobody in the world", this.s.setHome(carol), "player_only");
					this.expect("clearing a home there is none of", this.s.clearHome(carol), "no_home");
					this.expect("going to a home there is none of", this.s.goHome(carol), "no_home");
					this.expect("a plain member setting the home", this.s.setHome(dave), "no_permission");
					this.expect("an empty line of chat", this.s.chat(carol, "  §r "), "message_empty");
					this.expect("a line of chat", this.s.chat(carol, "hello §cthere"), "ok");
					this.check("chat reads [TAG] name: message, without the codes", this.traced("chat [EW] carol: hello there"));
					Component line = TeamService.chatLine(ember, "carol", "hi");
					this.check(
						"the first part of a chat line carries the mark",
						!line.getSiblings().isEmpty() && "cfhgui_teams:chat".equals(((Component)line.getSiblings().get(0)).getStyle().getInsertion())
					);
					this.expect("a second line at once", this.s.chat(carol, "again"), "too_fast");
					this.expect("chat without a team", this.s.chat(alice, "anyone?"), "not_in_team");
					this.expect("a ping", this.s.ping(carol, 1.5, 64.0, -3.5, 99, -5), "ok");
					this.check("a ping's kind is kept in range", this.traced("ping help"));
					this.rules.pings = false;
					this.expect("a ping where there are none", this.s.ping(dave, 0.0, 0.0, 0.0, 0, -1), "disabled");
					this.rules.pings = true;
					this.expect("the stash for nobody in the world", this.s.openStash(carol), "player_only");
					this.s.count(ember, "quests", 2L);
					this.s.count(ember, "quests", 3L);
					this.check("a counter adds up", ember.counter("quests") == 5L);

					for (int i = 0; i < 80; i++) {
						this.s.log(ember, "quest", carol.id(), "carol", "Quest " + i);
					}

					this.check("the log keeps the newest and no more than it may", ember.log.size() == 60 && ember.log.get(ember.log.size() - 1).subject.equals("Quest 79"));
					CompoundTag args = new CompoundTag();
					args.putString("name", "Payload Team");
					args.putString("tag", "PAY");
					args.putInt("color", 4478310);
					this.expect("an action as a client sends it", this.s.run(alice, "create", args), "ok");
					this.check(
						"the action did what its arguments said",
						this.s.store.ofPlayer(alice.id()) != null
							&& this.s.store.ofPlayer(alice.id()).color == 4478310
							&& this.s.store.tagged("pay") == this.s.store.ofPlayer(alice.id())
					);
					this.expect("an action nobody knows", this.s.run(alice, "frobnicate", new CompoundTag()), "unknown");
					this.expect("an action without its arguments", this.s.run(alice, "kick", new CompoundTag()), "not_a_member");
					CompoundTag claim = new CompoundTag();
					claim.putLongArray("chunks", chunks(-2, -2));
					this.expect("claiming as a client sends it", this.s.run(alice, "claim", claim), "ok");
					this.expect("an action that names no team", this.s.run(erin, "join", new CompoundTag()), "no_such_team");
					r = this.expect("giving up all of a team's land", this.s.unclaimAll(alice), "ok");
					this.check(
						"all of it is given up",
						r.number(0) == 1 && this.s.claims.count(this.s.store.ofPlayer(alice.id()).id) == 0 && this.s.ownerOf(Level.OVERWORLD, -2, -2) == null
					);
					this.expect("giving up land there is none of", this.s.unclaimAll(alice), "not_claimed");
					this.serverLand(carol, dave, erin, ember);
					this.roundTrip(ember);
					this.expect("a member ending the team", this.s.disband(dave), "no_permission");
					int before = this.trace.size();
					this.expect("ending the team", this.s.disband(carol), "ok");
					this.check(
						"the team is gone, its members free, its land given up",
						this.s.store.get(ember.id) == null
							&& this.s.store.ofPlayer(carol.id()) == null
							&& this.s.store.ofPlayer(dave.id()) == null
							&& this.s.claims.count(ember.id) == 0
							&& this.s.ownerOf(Level.OVERWORLD, 0, 0) == null
					);
					this.check("those who had declared it an ally no longer name it", !moss.isAlly(ember.id));
					List<String> after = this.trace.subList(before, this.trace.size());
					this.check(
						"every member was let go before the team was ended",
						after.indexOf("event left carol") >= 0
							&& after.indexOf("event left dave") >= 0
							&& after.indexOf("event disbanded Emberwatch") > Math.max(after.indexOf("event left carol"), after.indexOf("event left dave"))
					);
					this.check("its name and tag are free again", this.s.create(carol, "Emberwatch", "EW", null).ok());
					this.expiry(bob, moss);
					this.soloLand(bob);
				}
			}
		}
	}

	private Actor actorAt(String name, int chunkX, int chunkZ) {
		Actor a = Actor.fake(UUID.randomUUID(), name, "minecraft:overworld", chunkX, chunkZ);
		this.s.store.remember(a.id(), name);
		return a;
	}

	private void soloLand(Actor bob) {
		Actor ivy = this.actorAt("ivy", 40, 40);
		Actor jay = this.actorAt("jay", 40, 40);
		Actor kim = this.actorAt("kim", 40, 40);
		Actor lou = this.actorAt("lou", 40, 40);
		Protection p = this.s.protection;
		Backend api = new Backend(this.s);
		this.expect("the console claiming land of its own", this.s.claim(Actor.console("Server"), null, chunks(40, 40)), "not_in_team");
		this.expect("a stranger's first claim out of reach", this.s.claim(ivy, null, chunks(50, 50)), "too_far");
		this.check("a refused first claim leaves no land of one's own behind", this.s.store.ofPlayer(ivy.id()) == null);
		this.expect("giving up land one never had", this.s.unclaim(ivy, null, chunks(40, 40)), "not_claimed");
		OpResult r = this.expect("a stranger claiming land of their own", this.s.claim(ivy, null, chunks(40, 40)), "ok");
		TeamData own = this.s.store.ofPlayer(ivy.id());
		this.check(
			"their own land is made for it: flagged, named after them, theirs alone",
			own != null
				&& own.solo
				&& own.name.equals("ivy")
				&& own.members.size() == 1
				&& own.rankOf(ivy.id()) == Rank.OWNER
				&& (own.color & 16777215) != 0
				&& own.tag.isEmpty()
				&& this.s.ownerOf(Level.OVERWORLD, 40, 40) == own
				&& r.number(0) == 1
		);
		if (own != null) {
			this.check(
				"it is no team: not found by name, not the player's team",
				this.s.store.named("ivy") == null
					&& this.s.teamOf(ivy.id()) == null
					&& this.s.holderOf(ivy.id()) == own
					&& this.s.personalOf(ivy.id()) == own
					&& this.s.team(own.id) == null
			);
			TeamSummary told = this.s.sync.summaries().stream().filter(x -> x.id.equals(own.id)).findFirst().orElse(null);
			this.check(
				"the clients are told of it as one player's land, which no list shows",
				told != null
					&& told.solo
					&& told.ownerName.equals("ivy")
					&& told.claims == 1
					&& !this.s.teams().contains(own)
					&& this.s.teams().size() == this.s.store.size() - 1
			);
			this.check("its limits are a team of one's", this.s.claimLimit(own) == 3 && this.s.claimLimit(own) == this.s.claimLimit(null) && this.s.forceLimit(own) == 1);
			r = this.expect("claiming more than one's own land may hold", this.s.claim(ivy, null, chunks(41, 40, 42, 40, 43, 40)), "ok");
			this.check("it took what fits and said so", r.number(0) == 2 && r.number(1) == 1 && this.s.claims.count(own.id) == 3);
			r = this.expect("claiming past the limit", this.s.claim(ivy, null, chunks(40, 41)), "claim_limit");
			this.check("the answer names the limit", !r.args().isEmpty() && r.args().get(0).equals("3"));
			this.expect("keeping a chunk of one's own loaded", this.s.forceLoad(ivy, null, chunks(42, 40), true), "ok");
			this.expect("a second one past the limit", this.s.forceLoad(ivy, null, chunks(41, 40), true), "force_load_limit");
			this.expect("a rule of one's own land", this.s.setLandRule(ivy, "interact", "everyone"), "ok");
			this.check(
				"its rules hold on it, and its player may do all of it",
				p.refuses(jay.id(), Level.OVERWORLD, 40, 40, Protection.Kind.INTERACT) == null
					&& p.refuses(jay.id(), Level.OVERWORLD, 40, 40, Protection.Kind.BUILD) == own
					&& p.refuses(ivy.id(), Level.OVERWORLD, 40, 40, Protection.Kind.BUILD) == null
			);
			this.expect("inviting someone onto one's own land", this.s.invite(ivy, jay.id(), null), "not_in_team");
			this.expect("chat with one's own land", this.s.chat(ivy, "anyone?"), "not_in_team");
			this.expect("a waypoint of one's own land", this.s.addWaypoint(ivy, "Here", 640, 64, 640, null, null, null), "not_in_team");
			this.expect("renaming one's own land", this.s.rename(ivy, "Ivyland"), "not_in_team");
			this.expect("a home for one's own land", this.s.setHome(ivy), "not_in_team");
			this.expect("joining someone's own land", this.s.join(jay, own.id), "no_such_team");
			this.expect("a team declaring someone's own land its ally", this.s.ally(bob, own.id), "no_such_team");
			this.check(
				"other mods take it for no team, and its land for no team's",
				api.teamOf(ivy.id()) == null
					&& api.team(own.id) == null
					&& !api.teams().contains(own)
					&& own.id.equals(api.claimOwner("minecraft:overworld", chunk(40, 40)))
			);
			UUID id = own.id;
			this.expect("founding a team with land of one's own", this.s.create(ivy, "Ivy League", "IVY", null), "ok");
			TeamData league = this.s.store.ofPlayer(ivy.id());
			this.check(
				"the land became the team: the same id, its land kept, no longer one player's",
				league == own
					&& league.id.equals(id)
					&& !league.solo
					&& league.name.equals("Ivy League")
					&& this.s.store.named("ivy league") == league
					&& this.s.store.tagged("ivy") == league
					&& this.s.claims.count(id) == 3
					&& this.s.claims.forced(id) == 1
					&& this.s.ownerOf(Level.OVERWORLD, 40, 40) == league
			);
			this.check(
				"it is a team to everyone now",
				this.traced("event created Ivy League") && this.s.teamOf(ivy.id()) == league && this.s.teams().contains(league) && api.teamOf(ivy.id()) == league
			);
			this.expect("a second player's land", this.s.claim(jay, null, chunks(41, 43, 40, 43, 42, 43)), "ok");
			TeamData jays = this.s.store.ofPlayer(jay.id());
			this.check("their own land too", jays != null && jays.solo && this.s.claims.count(jays.id) == 3);
			if (jays != null) {
				this.expect("keeping a chunk of it loaded", this.s.forceLoad(jay, null, chunks(40, 43), true), "ok");
				this.expect("inviting a player who holds land of their own", this.s.invite(ivy, jay.id(), null), "ok");
				this.expect("they accept", this.s.acceptInvite(jay, league.id), "ok");
				this.check(
					"the land moved in as far as the team may hold, what was kept loaded first",
					this.s.store.ofPlayer(jay.id()) == league
						&& this.s.store.get(jays.id) == null
						&& this.s.claims.count(league.id) == 4
						&& this.s.claims.count(jays.id) == 0
						&& this.s.ownerOf(Level.OVERWORLD, 40, 43) == league
						&& this.s.ownerOf(Level.OVERWORLD, 41, 43) == null
						&& this.s.ownerOf(Level.OVERWORLD, 42, 43) == null
				);
				this.check(
					"and the team keeps no more loaded than it may", this.s.claims.forced(league.id) == 1 && !this.s.claims.at("minecraft:overworld", chunk(40, 43)).forced
				);
				this.check("they are told how it went", this.traced("notice land_cut jay") && this.traced("event joined jay"));
				this.expect("a third player's land", this.s.claim(kim, null, chunks(44, 44)), "ok");
				this.expect("the league opening its door", this.s.setPrivacy(ivy, Privacy.OPEN), "ok");
				this.expect("joining an open team with land of one's own", this.s.join(kim, league.id), "ok");
				this.check(
					"all of it moved, and they are told",
					this.s.claims.count(league.id) == 5
						&& this.s.ownerOf(Level.OVERWORLD, 44, 44) == league
						&& this.s.personalOf(kim.id()) == null
						&& this.traced("notice land_moved kim")
				);
				this.expect("a fourth player's land", this.s.claim(lou, null, chunks(44, 40, 44, 41)), "ok");
				TeamData lous = this.s.store.ofPlayer(lou.id());
				UUID first = lous == null ? id : lous.id;
				r = this.expect("leaving land of one's own", this.s.leave(lou), "ok");
				this.check(
					"leaving gives all of it up, and the record goes",
					this.s.store.ofPlayer(lou.id()) == null
						&& this.s.store.get(first) == null
						&& this.s.claims.count(first) == 0
						&& this.s.ownerOf(Level.OVERWORLD, 44, 40) == null
						&& r.number(1) == 2
						&& this.traced("notice land_given_up lou")
				);
				this.expect("claiming again", this.s.claim(lou, null, chunks(44, 40)), "ok");
				lous = this.s.store.ofPlayer(lou.id());
				this.expect("ending land of one's own", this.s.disband(lou), "ok");
				this.check("ending it gives it up as well", this.s.store.ofPlayer(lou.id()) == null && lous != null && this.s.claims.count(lous.id) == 0);
				this.expect("claiming once more", this.s.claim(lou, null, chunks(44, 40)), "ok");
				lous = this.s.store.ofPlayer(lou.id());
				this.expect("giving up its last chunk", this.s.unclaim(lou, null, chunks(44, 40)), "ok");
				this.check("land of one's own with no land left is no more", this.s.store.ofPlayer(lou.id()) == null && lous != null && this.s.store.get(lous.id) == null);
				this.expect("claiming to keep", this.s.claim(lou, null, chunks(44, 41)), "ok");
				lous = this.s.store.ofPlayer(lou.id());
				if (lous != null) {
					this.soloRoundTrip(lous);
					this.rules.claimExpiryDays = 7;
					lous.lastSeen = this.time - 691200000L;
					List<TeamData> quiet = this.s.expiry.run();
					this.check(
						"land of one's own gone quiet is given up, and is no more",
						quiet.contains(lous) && this.s.store.get(lous.id) == null && this.s.store.ofPlayer(lou.id()) == null && this.s.ownerOf(Level.OVERWORLD, 44, 41) == null
					);
					this.rules.claimExpiryDays = 0;
				}
			}
		}
	}

	private void soloRoundTrip(TeamData lous) {
		TeamData wire = (TeamData)JsonWire.decode(JsonWire.encode(lous), TeamData.class);
		this.check("land of one's own survives the wire", wire != null && wire.sanitize().solo && wire.name.equals(lous.name));
		Path dir = null;

		try {
			dir = Files.createTempDirectory("cfhgui-teams-selftest-solo");
			TeamStore out = new TeamStore(dir);
			out.add(lous);
			TeamData namesake = new TeamData();
			namesake.id = UUID.randomUUID();
			namesake.name = lous.name;
			TeamData.Member m = new TeamData.Member();
			m.id = UUID.randomUUID();
			m.name = "someone";
			m.rank = Rank.OWNER;
			namesake.members.add(m);
			out.add(namesake);
			Saver saver = new Saver();
			out.save(saver);
			saver.close();
			TeamStore in = new TeamStore(dir);
			in.load();
			TeamData back = in.get(lous.id);
			TeamData team = in.get(namesake.id);
			this.check(
				"land of one's own comes back from disk as it was, and leaves a team its name",
				back != null
					&& back.solo
					&& back.name.equals(lous.name)
					&& in.ofPlayer(lous.members.get(0).id) == back
					&& team != null
					&& team.name.equals(lous.name)
					&& in.named(lous.name) == team
			);
		} catch (Exception var29) {
			this.check("writing and reading land of one's own (" + var29 + ")", false);
		} finally {
			if (dir != null) {
				try (Stream<Path> files = Files.walk(dir)) {
					files.sorted(Comparator.reverseOrder()).forEach(f -> {
						try {
							Files.deleteIfExists(f);
						} catch (Exception var2x) {
						}
					});
				} catch (Exception var28) {
				}
			}
		}
	}

	private void serverLand(Actor carol, Actor dave, Actor erin, TeamData ember) {
		Protection p = this.s.protection;
		TeamData spawn = this.s.serverLand().data;
		Actor op = this.actor("opal").asAdmin();
		int held = this.s.claims.count(ember.id);
		this.expect("a player claiming for the server", this.s.claimServerLand(carol, null, chunks(20, 20)), "operators_only");
		CompoundTag one = new CompoundTag();
		one.putLongArray("chunks", chunks(20, 20));
		one.putBoolean("server", true);
		this.expect("a player claiming for the server as a client asks", this.s.run(carol, "claim", one), "operators_only");
		this.check(
			"nothing came of it, and the clients hear of no server land", this.s.claims.count(TeamSummary.SERVER_LAND) == 0 && this.s.sync.serverLand() == null
		);
		CompoundTag three = new CompoundTag();
		three.putLongArray("chunks", chunks(20, 20, 21, 20, 0, 0));
		three.putBoolean("server", true);
		OpResult r = this.expect("an operator claiming for the server, far out of a team's reach", this.s.run(op, "claim", three), "ok");
		this.check(
			"it took the free chunks and left the team's",
			r.number(0) == 2
				&& r.number(1) == 1
				&& this.s.ownerOf(Level.OVERWORLD, 20, 20) == spawn
				&& this.s.ownerOf(Level.OVERWORLD, 21, 20) == spawn
				&& this.s.ownerOf(Level.OVERWORLD, 0, 0) == ember
				&& this.s.claims.count(TeamSummary.SERVER_LAND) == 2
		);
		r = this.expect("the server claiming a team's land", this.s.claimServerLand(op, null, chunks(0, 0)), "already_claimed");
		this.check("the answer names the team", !r.args().isEmpty() && r.args().get(0).equals(ember.name));
		r = this.expect("a team claiming the server's land", this.s.claim(carol.asAdmin(), null, chunks(20, 20)), "already_claimed");
		this.check("the answer names the server's land", !r.args().isEmpty() && r.args().get(0).equals(spawn.name));
		this.rules.claims = false;
		this.expect("the server claiming where teams may not claim at all", this.s.claimServerLand(op, null, chunks(22, 20)), "ok");
		this.rules.claims = true;
		this.expect("giving that one up", this.s.unclaimServerLand(op, null, chunks(22, 20)), "ok");
		this.check(
			"it is no team and counts towards nobody's limit",
			this.s.store.get(TeamSummary.SERVER_LAND) == null
				&& this.s.store.named(spawn.name) == null
				&& this.s.claims.count(ember.id) == held
				&& !this.s.store.all().contains(spawn)
		);
		this.expect("joining it", this.s.join(this.actor("fern"), TeamSummary.SERVER_LAND), "no_such_team");
		this.expect("declaring it an ally", this.s.ally(carol, TeamSummary.SERVER_LAND), "no_such_team");
		TeamSummary told = this.s.sync.serverLand();
		this.check(
			"the clients are told of it as land that is no team",
			told != null
				&& told.server
				&& told.id.equals(TeamSummary.SERVER_LAND)
				&& told.claims == 2
				&& told.members == 0
				&& told.name.equals(spawn.name)
				&& told.color == spawn.color
		);
		boolean closed = true;

		for (Protection.Kind kind : Protection.Kind.values()) {
			closed &= p.refuses(dave.id(), Level.OVERWORLD, 20, 20, kind) == spawn
				&& p.refuses(erin.id(), Level.OVERWORLD, 20, 20, kind) == spawn
				&& p.refuses(op.id(), Level.OVERWORLD, 20, 20, kind) == spawn;
		}

		this.check("to begin with nobody may build, use, open or hurt on it", closed);
		this.check(
			"and nothing explodes, burns, is carried off or fights there",
			!this.s.pins().explosions(spawn) && !this.s.pins().mobGriefing(spawn) && !this.s.pins().fireSpread(spawn) && !this.s.pins().pvp(spawn)
		);
		this.check("everyone is a stranger to it", p.relation(spawn, dave.id()) == Access.Relation.OUTSIDER && p.relation(spawn, op.id()) == Access.Relation.OUTSIDER);
		String[] who = new String[]{"build", "interact", "containers", "entities"};

		for (int i = 0; i < who.length; i++) {
			Protection.Kind kind = Protection.Kind.values()[i];
			this.expect("opening " + who[i] + " on server land", this.s.ruleServerLand(op, who[i], "everyone"), "ok");
			boolean others = true;

			for (Protection.Kind k : Protection.Kind.values()) {
				if (k != kind) {
					others &= p.refuses(erin.id(), Level.OVERWORLD, 20, 20, k) == spawn;
				}
			}

			this.check("server land: " + who[i] + " opened lets everyone, and only that", p.refuses(erin.id(), Level.OVERWORLD, 20, 20, kind) == null && others);
			r = this.expect("closing " + who[i] + " on server land", this.s.ruleServerLand(op, who[i], "nobody"), "ok");
			this.check(
				"server land: " + who[i] + " closed lets nobody",
				p.refuses(erin.id(), Level.OVERWORLD, 20, 20, kind) == spawn && r.args().size() > 1 && r.args().get(1).equals("nobody")
			);
		}

		this.expect("server land: explosions on", this.s.ruleServerLand(op, "explosions", "true"), "ok");
		this.expect("server land: mob griefing on", this.s.ruleServerLand(op, "mob_griefing", "on"), "ok");
		this.expect("server land: fire spread on", this.s.ruleServerLand(op, "fireSpread", "true"), "ok");
		this.expect("server land: fights on", this.s.ruleServerLand(op, "pvp", "true"), "ok");
		this.check(
			"server land: each switch holds when on",
			this.s.pins().explosions(spawn) && this.s.pins().mobGriefing(spawn) && this.s.pins().fireSpread(spawn) && this.s.pins().pvp(spawn)
		);

		for (String rule : new String[]{"explosions", "mobGriefing", "fireSpread", "pvp"}) {
			this.s.ruleServerLand(op, rule, "false");
		}

		this.check(
			"server land: and when off again",
			!this.s.pins().explosions(spawn) && !this.s.pins().mobGriefing(spawn) && !this.s.pins().fireSpread(spawn) && !this.s.pins().pvp(spawn)
		);
		this.expect("a rule of server land set by a player", this.s.ruleServerLand(carol, "pvp", "true"), "operators_only");
		this.expect("a rule server land has not", this.s.ruleServerLand(op, "claimRank", "member"), "invalid");
		this.expect("a value a rule of server land cannot have", this.s.ruleServerLand(op, "build", "sometimes"), "invalid");
		this.rules.pinnedLandRules.add("pvp=true");
		this.rules.pinnedLandRules.add("build=EVERYONE");
		this.s.repin();
		this.check(
			"what the server pins for teams does not reach its own land",
			!this.s.pins().pvp(spawn)
				&& this.s.pins().pvp(ember)
				&& p.refuses(erin.id(), Level.OVERWORLD, 20, 20, Protection.Kind.BUILD) == spawn
				&& p.refuses(erin.id(), Level.OVERWORLD, 0, 0, Protection.Kind.BUILD) == null
		);
		this.rules.pinnedLandRules.clear();
		this.s.repin();
		ClaimMap.Claim a = this.s.claims.at(Level.OVERWORLD, 20, 20);
		ClaimMap.Claim b = this.s.claims.at(Level.OVERWORLD, 21, 20);
		ClaimMap.Claim teams = this.s.claims.at(Level.OVERWORLD, 0, 0);
		this.check(
			"water flows within server land, and nothing reaches into it from outside",
			a != null && b != null && teams != null && p.mayReachInto(a, b) && !p.mayReachInto(null, a) && !p.mayReachInto(teams, a) && !p.mayReachInto(a, teams)
		);
		this.expect("naming server land", this.s.nameServerLand(op, "  Hub §4Town "), "ok");
		this.check(
			"its name is tidied like any", spawn.name.equals("Hub Town") && this.s.sync.serverLand() != null && this.s.sync.serverLand().name.equals("Hub Town")
		);
		this.expect("a name of nothing", this.s.nameServerLand(op, "  "), "invalid");
		this.expect("naming it by a player", this.s.nameServerLand(carol, "Mine"), "operators_only");
		this.expect("colouring server land", this.s.colorServerLand(op, -15584170), "ok");
		this.check("its colour is kept as RGB", spawn.color == 1193046);
		this.expect("a player giving server land up", this.s.unclaimServerLand(carol, null, chunks(20, 20)), "operators_only");
		this.expect("a team giving server land up", this.s.unclaim(carol, null, chunks(20, 20)), "not_claimed");
		this.expect("the server giving up a team's land", this.s.unclaimServerLand(op, null, chunks(0, 0)), "not_server_land");
		CompoundTag two = new CompoundTag();
		two.putLongArray("chunks", chunks(21, 20, 0, 0));
		two.putBoolean("server", true);
		r = this.expect("the server giving land up, as a client asks", this.s.run(op, "unclaim", two), "ok");
		this.check(
			"its own chunk is free again and the team's is untouched",
			r.number(0) == 1
				&& r.number(1) == 1
				&& this.s.ownerOf(Level.OVERWORLD, 21, 20) == null
				&& this.s.ownerOf(Level.OVERWORLD, 0, 0) == ember
				&& this.s.ownerOf(Level.OVERWORLD, 20, 20) == spawn
				&& this.s.claims.count(TeamSummary.SERVER_LAND) == 1
		);
		this.s.ruleServerLand(op, "interact", "everyone");
		this.s.ruleServerLand(op, "pvp", "true");
	}

	private void expiry(Actor bob, TeamData moss) {
		long day = 86400000L;
		long one = chunk(1, 0);
		this.expect("keeping the quiet team's chunk loaded", this.s.forceLoad(bob, null, new long[]{one}, true), "ok");
		this.check("the quiet team holds land, one chunk of it kept loaded", this.s.claims.count(moss.id) == 1 && this.s.claims.forced(moss.id) == 1);
		moss.lastSeen = this.time;
		this.time += 34560000000L;
		this.check(
			"with no days set, land is never given up",
			this.s.expiry.run().isEmpty() && this.s.claims.count(moss.id) == 1 && this.s.expiry.standings().get(0).due() == 0L
		);
		this.rules.claimExpiryDays = 7;
		moss.lastSeen = this.time - 518400000L;
		this.check("not yet: six days away of seven", this.s.expiry.run().isEmpty() && this.s.claims.count(moss.id) == 1 && moss.lastSeen == this.time - 518400000L);
		List<Expiry.Standing> standings = this.s.expiry.standings();
		this.check(
			"the standing says when the land would go",
			standings.size() == 1
				&& standings.get(0).team() == moss
				&& !standings.get(0).online()
				&& standings.get(0).chunks() == 1
				&& standings.get(0).due() == this.time + 86400000L
		);
		moss.lastSeen = 0L;
		this.check(
			"a team without a date is taken as seen now, not as gone", this.s.expiry.run().isEmpty() && moss.lastSeen == this.time && this.s.claims.count(moss.id) == 1
		);
		moss.lastSeen = this.time + 2592000000L;
		this.check("a date in the future is taken as now", this.s.expiry.run().isEmpty() && moss.lastSeen == this.time && this.s.claims.count(moss.id) == 1);
		this.s.claims.drain();
		this.time += 604800000L;
		List<TeamData> gone = this.s.expiry.run();
		this.check(
			"now: seven days away, and the land is given up",
			gone.size() == 1 && gone.get(0) == moss && this.s.claims.count(moss.id) == 0 && this.s.ownerOf(Level.OVERWORLD, 1, 0) == null
		);
		this.check("the chunks it kept loaded go with the land", this.s.claims.forced(moss.id) == 0);
		TeamData.LogEntry last = moss.log.isEmpty() ? null : moss.log.get(moss.log.size() - 1);
		this.check(
			"the team itself stays, with a line in its log",
			this.s.store.get(moss.id) == moss && moss.isMember(bob.id()) && last != null && last.type.equals("expired") && last.subject.equals("7")
		);
		List<ClaimsUpdate> told = this.s.claims.drain();
		this.check("the clients are told the land is free", told.size() == 1 && told.get(0).removed().length == 1 && told.get(0).removed()[0] == one);
		this.check("a second look finds nothing more to give up", this.s.expiry.run().isEmpty() && this.s.expiry.standings().isEmpty());
		this.check("the server's own land is never given up for being away", this.s.claims.count(TeamSummary.SERVER_LAND) == 1);
		this.expect("the team claims again once it is back", this.s.claim(bob, null, new long[]{one}), "ok");
		this.rules.claimExpiryDays = 0;
	}

	private void roundTrip(TeamData sample) {
		TeamData copy = (TeamData)JsonWire.decode(JsonWire.encode(sample), TeamData.class);
		this.check(
			"a team survives the wire",
			copy != null
				&& copy.sanitize().id.equals(sample.id)
				&& copy.members.size() == sample.members.size()
				&& copy.log.size() == sample.log.size()
				&& copy.land.build == sample.land.build
		);
		Path dir = null;

		try {
			dir = Files.createTempDirectory("cfhgui-teams-selftest");
			Saver saver = new Saver();
			TeamStore out = new TeamStore(dir);

			for (TeamData t : this.s.store.all()) {
				out.add(t);
			}

			out.save(saver);
			ClaimMap claims = new ClaimMap(dir.resolve("claims.json"));

			for (ClaimMap.Dim d : this.s.claims.dims()) {
				ObjectIterator spawn = d.chunks.long2ObjectEntrySet().iterator();

				while (spawn.hasNext()) {
					Entry<ClaimMap.Claim> e = (Entry<ClaimMap.Claim>)spawn.next();
					ClaimMap.Claim c = claims.put(d.id, e.getLongKey(), ((ClaimMap.Claim)e.getValue()).team);
					if (c != null && ((ClaimMap.Claim)e.getValue()).forced) {
						claims.setForced(claims.dim(d.id), e.getLongKey(), c, true);
					}
				}
			}

			claims.save(saver);
			saver.close();
			TeamStore in = new TeamStore(dir);
			in.load();
			TeamData read = in.get(sample.id);
			this.check(
				"the teams come back from disk",
				in.size() == this.s.store.size()
					&& read != null
					&& read.name.equals(sample.name)
					&& read.members.size() == sample.members.size()
					&& in.ofPlayer(sample.members.get(0).id) == read
					&& in.named(sample.name) == read
					&& read.lastSeen == sample.lastSeen
					&& sample.lastSeen > 0L
			);
			TeamData spawn = this.s.serverLand().data;
			ServerLand landBack = ServerLand.read(this.s.serverLand().json());
			this.check(
				"the server's land comes back as it was kept",
				landBack.data.id.equals(TeamSummary.SERVER_LAND)
					&& landBack.data.name.equals(spawn.name)
					&& landBack.data.color == spawn.color
					&& landBack.data.land.interact == Access.EVERYONE
					&& landBack.data.land.pvp
					&& landBack.data.land.build == Access.MEMBERS
					&& !landBack.data.land.explosions
			);
			ServerLand sparse = ServerLand.read("{\"name\":\"Market\",\"rules\":{\"interact\":\"EVERYONE\",\"build\":\"nonsense\"}}");
			this.check(
				"a rule its file does not name stays closed",
				sparse.data.name.equals("Market")
					&& sparse.data.land.interact == Access.EVERYONE
					&& sparse.data.land.build == Access.MEMBERS
					&& sparse.data.land.containers == Access.MEMBERS
					&& !sparse.data.land.pvp
					&& !sparse.data.land.explosions
			);
			ClaimMap back = new ClaimMap(dir.resolve("claims.json"));
			back.load(id -> TeamSummary.SERVER_LAND.equals(id) ? landBack.data : in.get(id));
			this.check(
				"the claims come back from disk",
				back.total() == this.s.claims.total()
					&& back.count(sample.id) == this.s.claims.count(sample.id)
					&& back.forced(sample.id) == this.s.claims.forced(sample.id)
					&& back.at(Level.OVERWORLD, 0, 0) != null
					&& back.at(Level.OVERWORLD, 0, 0).team == read
			);
			this.check(
				"the server's land is among them, and is the server's",
				back.count(TeamSummary.SERVER_LAND) == 1 && back.at(Level.OVERWORLD, 20, 20) != null && back.at(Level.OVERWORLD, 20, 20).team == landBack.data
			);
			this.check("no temp file is left behind", !Files.exists(dir.resolve("teams.json.tmp")) && Files.isRegularFile(dir.resolve("teams.json")));
		} catch (Exception var31) {
			this.check("writing and reading the files (" + var31 + ")", false);
		} finally {
			if (dir != null) {
				try (Stream<Path> files = Files.walk(dir)) {
					files.sorted(Comparator.reverseOrder()).forEach(f -> {
						try {
							Files.deleteIfExists(f);
						} catch (Exception var2x) {
						}
					});
				} catch (Exception var30) {
				}
			}
		}
	}

	static record Outcome(int passed, int failed) {
	}
}
