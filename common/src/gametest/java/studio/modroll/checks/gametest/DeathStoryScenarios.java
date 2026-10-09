package studio.modroll.checks.gametest;

import static studio.modroll.checks.gametest.ScenarioSupport.confirm;
import static studio.modroll.checks.gametest.ScenarioSupport.expect;
import static studio.modroll.checks.gametest.ScenarioSupport.loginWatched;
import static studio.modroll.checks.gametest.ScenarioSupport.logout;
import static studio.modroll.checks.gametest.ScenarioSupport.newProfile;
import static studio.modroll.checks.gametest.ScenarioSupport.unprofiled;
import static studio.modroll.checks.gametest.ScenarioSupport.waitOutSpawnProtection;
import static studio.modroll.checks.gametest.ScenarioSupport.withD20s;
import static studio.modroll.checks.gametest.ScenarioSupport.withDeathMessages;
import static studio.modroll.checks.gametest.ScenarioSupport.withSocial;
import static studio.modroll.checks.gametest.SocialScenarios.IN_FRONT;
import static studio.modroll.checks.gametest.SocialScenarios.SECOND_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.TARGET_SPOT;
import static studio.modroll.checks.gametest.SocialScenarios.act;
import static studio.modroll.checks.gametest.SocialScenarios.golem;
import static studio.modroll.checks.gametest.SocialScenarios.piglin;
import static studio.modroll.checks.gametest.SocialScenarios.settings;
import static studio.modroll.checks.gametest.SocialScenarios.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.GameType;
import studio.modroll.checks.body.PlayerBodies;
import studio.modroll.checks.data.ScoresConfig;
import studio.modroll.checks.social.SocialAction;
import studio.modroll.critfall.api.CombatSuppression;

/**
 * GameTest bodies for story death messages: a player killed shortly after a related Checks event gets a
 * line from that story's pool on the death screen, sent with an English fallback. The message is read
 * from the packet the server sends the dying player. Players are new survival players with every score 10
 * against unprofiled targets.
 */
public final class DeathStoryScenarios {

    private static final int NAT_ONE = 1;
    private static final int FAIL = 2;
    private static final float LETHAL = 100f;
    private static final String NAMED_GOLEM = "Bob";
    private static final int SHORT_WINDOW = 1;
    private static final int PAST_WINDOW_TICKS = 3;
    private static final ScoresConfig.DeathMessageSettings ON = ScoresConfig.DEFAULTS.deathMessages();

    private DeathStoryScenarios() {}

    /** Caught pickpocketing, then killed by a golem: the story names the item reached for. */
    public static void caughtPickpocketKilledByAGolem(GameTestHelper helper) {
        Victim victim = victim(helper, "story-pickpocket");
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            socially(() -> act(victim.player(), villager, SocialAction.PICKPOCKET, FAIL));
            victim.dieFrom(helper.getLevel().damageSources().mobAttack(golem(helper, false)));
            TranslatableContents story = expectStory(helper, victim, "caught_pickpocketing");
            String item = ((Component) story.getArgs()[2]).getString();
            expect(
                    helper,
                    item.equals("Bread") || item.equals("Emerald"),
                    "the story must name a trade, named " + item);
            expectKiller(helper, story, "the Iron Golem");
        } finally {
            logout(helper, victim.player());
        }
        helper.succeed();
    }

    /** A golem with a custom name is named by it alone. */
    public static void failedThreatKilledByAGolem(GameTestHelper helper) {
        Victim victim = victim(helper, "story-threat");
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            socially(() -> act(victim.player(), villager, SocialAction.INTIMIDATE, FAIL));
            IronGolem bob = golem(helper, false);
            bob.setCustomName(Component.literal(NAMED_GOLEM));
            victim.dieFrom(helper.getLevel().damageSources().mobAttack(bob));
            expectKiller(helper, expectStory(helper, victim, "failed_intimidate"), NAMED_GOLEM);
        } finally {
            logout(helper, victim.player());
        }
        helper.succeed();
    }

    public static void naturalOneThreatKilledByAMob(GameTestHelper helper) {
        Victim victim = victim(helper, "story-nat-one");
        LivingEntity piglin = piglin(helper, SECOND_SPOT);
        try {
            socially(() -> act(victim.player(), piglin, SocialAction.INTIMIDATE, NAT_ONE));
            victim.dieFrom(helper.getLevel().damageSources().mobAttack(piglin));
            expectStory(helper, victim, "intimidate_natural_one");
        } finally {
            logout(helper, victim.player());
        }
        helper.succeed();
    }

    /** The DEX save against the killing blast itself fails. */
    public static void failedDexSaveKilledByAnExplosion(GameTestHelper helper) {
        Victim victim = victim(helper, "story-blast");
        try {
            withD20s(
                    () -> {
                        victim.dieFrom(helper.getLevel().damageSources().explosion(null, null));
                        return null;
                    },
                    FAIL);
            expectStory(helper, victim, "failed_explosion_save");
        } finally {
            logout(helper, victim.player());
        }
        helper.succeed();
    }

    /** An orc's Relentless Endurance keeps them up once; the next killing blow tells the story. */
    public static void diedWithTheLastStandSpent(GameTestHelper helper) {
        Victim victim = victim(helper, "story-last-stand");
        ServerPlayer orc = victim.player();
        try {
            confirm(orc, ResourceLocation.fromNamespaceAndPath("checks", "orc"), Optional.empty());
            PlayerBodies.sync(orc);
            orc.setHealth(orc.getMaxHealth());
            orc.hurt(orc.damageSources().generic(), LETHAL);
            expect(helper, orc.isAlive(), "the last stand must keep the orc alive");
            orc.invulnerableTime = 0;
            victim.dieFrom(orc.damageSources().generic());
            expectStory(helper, victim, "last_stand_spent");
        } finally {
            logout(helper, orc);
        }
        helper.succeed();
    }

    /** Past the window, the death gets vanilla's message. */
    public static void storiesEndWithTheWindow(GameTestHelper helper) {
        Victim victim = victim(helper, "story-late");
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        socially(() -> act(victim.player(), villager, SocialAction.PICKPOCKET, FAIL));
        helper.runAfterDelay(PAST_WINDOW_TICKS, () -> {
            try {
                withDeathMessages(new ScoresConfig.DeathMessageSettings(true, SHORT_WINDOW), () -> {
                    victim.dieFrom(helper.getLevel().damageSources().mobAttack(golem(helper, false)));
                    return null;
                });
                expectVanilla(helper, victim);
            } finally {
                logout(helper, victim.player());
            }
            helper.succeed();
        });
    }

    /** A death the story does not explain, a golem story ended by a fall, gets vanilla's message. */
    public static void storiesOnlyTellDeathsTheyExplain(GameTestHelper helper) {
        Victim victim = victim(helper, "story-unrelated");
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            socially(() -> act(victim.player(), villager, SocialAction.PICKPOCKET, FAIL));
            victim.dieFrom(helper.getLevel().damageSources().fall());
            expectVanilla(helper, victim);
        } finally {
            logout(helper, victim.player());
        }
        helper.succeed();
    }

    public static void deathMessagesSwitchOff(GameTestHelper helper) {
        Victim victim = victim(helper, "story-off");
        Villager villager = villager(helper, VillagerProfession.FARMER, TARGET_SPOT);
        try {
            socially(() -> act(victim.player(), villager, SocialAction.PICKPOCKET, FAIL));
            withDeathMessages(new ScoresConfig.DeathMessageSettings(false, ON.windowTicks()), () -> {
                victim.dieFrom(helper.getLevel().damageSources().mobAttack(golem(helper, false)));
                return null;
            });
            expectVanilla(helper, victim);
        } finally {
            logout(helper, victim.player());
        }
        helper.succeed();
    }

    /** A player and every packet the server sends them. */
    private record Victim(ServerPlayer player, List<Packet<?>> sent) {

        /** Outside Critfall's combat, so a mob's blow always lands with the full amount. */
        void dieFrom(DamageSource source) {
            List<UUID> fighters = Stream.of(player, source.getEntity())
                    .filter(Objects::nonNull)
                    .map(Entity::getUUID)
                    .toList();
            fighters.forEach(CombatSuppression::suppress);
            try {
                player.hurt(source, LETHAL);
            } finally {
                fighters.forEach(CombatSuppression::release);
            }
        }

        String deathMessageKey() {
            return deathMessage().getContents() instanceof TranslatableContents message ? message.getKey() : "";
        }

        Component deathMessage() {
            return sent.stream()
                    .filter(ClientboundPlayerCombatKillPacket.class::isInstance)
                    .map(packet -> ((ClientboundPlayerCombatKillPacket) packet).message())
                    .findFirst()
                    .orElse(Component.empty());
        }
    }

    /** A survival player in front of the target spot, past spawn protection. */
    private static Victim victim(GameTestHelper helper, String name) {
        List<Packet<?>> sent = new ArrayList<>();
        ServerPlayer player = loginWatched(helper, newProfile(name), sent);
        player.setGameMode(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(IN_FRONT));
        waitOutSpawnProtection(player);
        return new Victim(player, sent);
    }

    private static void socially(Runnable body) {
        withSocial(
                settings(action -> true, false, 0),
                () -> unprofiled(() -> {
                    body.run();
                    return null;
                }));
    }

    private static TranslatableContents expectStory(GameTestHelper helper, Victim victim, String story) {
        expect(helper, victim.player().isDeadOrDying(), "the player must die");
        String key = victim.deathMessageKey();
        expect(helper, key.startsWith("checks.death." + story + "."), "the death must tell " + story + ", told " + key);
        TranslatableContents message =
                (TranslatableContents) victim.deathMessage().getContents();
        expect(helper, message.getFallback() != null, "a client without Checks must get the English line");
        return message;
    }

    private static void expectKiller(GameTestHelper helper, TranslatableContents story, String expected) {
        String killer = ((Component) story.getArgs()[1]).getString();
        expect(helper, killer.equals(expected), "the killer must be named " + expected + ", was " + killer);
    }

    private static void expectVanilla(GameTestHelper helper, Victim victim) {
        expect(helper, victim.player().isDeadOrDying(), "the player must die");
        String key = victim.deathMessageKey();
        expect(helper, key.startsWith("death."), "the death must get vanilla's message, got " + key);
    }
}
