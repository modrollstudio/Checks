package studio.modroll.checks.social;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.modroll.checks.ModLang;
import studio.modroll.checks.text.FallbackText;

class SocialTextTest {

    private Language before;

    @BeforeEach
    void loadModLang() throws IOException {
        before = Language.getInstance();
        ModLang.inject();
    }

    @AfterEach
    void restoreLang() {
        Language.inject(before);
    }

    @Test
    void durationsRoundUpToWholeSecondsThenWholeMinutes() {
        assertEquals("1 s", SocialText.duration(1).getString());
        assertEquals("59 s", SocialText.duration(1180).getString());
        assertEquals("1 min", SocialText.duration(1200).getString());
        assertEquals("2 min", SocialText.duration(1201).getString());
        assertEquals("60 min", SocialText.duration(72000).getString());
    }

    @Test
    void eachReasonHasItsText() {
        assertEquals(
                "Try again in 5 min.",
                SocialText.unavailable(option(SocialAction.PERSUADE, SocialMenu.Availability.COOLDOWN, 6000))
                        .getString());
        assertEquals(
                "Only villagers have pockets worth picking.",
                SocialText.unavailable(option(SocialAction.PICKPOCKET, SocialMenu.Availability.WRONG_TARGET, 0))
                        .getString());
        assertEquals(
                "Has nothing to trade.",
                SocialText.unavailable(option(SocialAction.INTIMIDATE, SocialMenu.Availability.NO_TRADES, 0))
                        .getString());
        assertEquals(
                "Everyone's watching you.",
                SocialText.unavailable(option(SocialAction.PICKPOCKET, SocialMenu.Availability.ON_GUARD, 0))
                        .getString());
        assertEquals(
                "They've already heard your plea.",
                SocialText.unavailable(option(SocialAction.PLEAD, SocialMenu.Availability.TRIED, 0))
                        .getString());
        assertEquals(
                "They've already heard your story.",
                SocialText.unavailable(option(SocialAction.LIE, SocialMenu.Availability.TRIED, 0))
                        .getString());
    }

    @Test
    void everyActionHasAWrongTargetText() {
        for (SocialAction action : SocialAction.values()) {
            SocialText.unavailable(option(action, SocialMenu.Availability.WRONG_TARGET, 0));
            SocialText.actionName(action);
        }
    }

    @Test
    void everyOutcomeHasAPoolOfFourToSixLines() {
        List<String> pools = new ArrayList<>();
        for (SocialOutcome outcome : SocialOutcome.values()) {
            pools.add("persuade." + outcome.id());
            pools.add("deceive." + outcome.id());
        }
        pools.addAll(List.of(
                "deceive.piglin.success",
                "deceive.piglin.critical_success",
                "deceive.piglin.failure",
                "intimidate.success",
                "intimidate.failure",
                "intimidate.piglin.success",
                "intimidate.piglin.failure",
                "pickpocket.success",
                "pickpocket.failure"));
        for (String pool : pools) {
            int size = FallbackText.poolSize("checks.social." + pool);
            assertTrue(size >= 4 && size <= 6, pool + " has " + size + " lines");
        }
    }

    @Test
    void outcomesThatHandSomethingOverNameIt() {
        for (String pool : List.of("intimidate.success", "intimidate.piglin.success", "pickpocket.success")) {
            String key = "checks.social." + pool;
            for (int line = 1; line <= FallbackText.poolSize(key); line++) {
                String text =
                        FallbackText.of(key + "." + line, "Farmer", "Bread").getString();
                assertTrue(text.contains("Bread"), key + "." + line + " never names the item: " + text);
            }
        }
    }

    @Test
    void aLineIsPickedFromThePool() {
        Set<String> seen = new HashSet<>();
        RandomSource random = RandomSource.create(1);
        String key = "checks.social.pickpocket.failure";
        for (int i = 0; i < 200; i++) {
            seen.add(FallbackText.oneOf(key, random, "Farmer").getString());
        }
        assertEquals(FallbackText.poolSize(key), seen.size());
    }

    @Test
    void aWanderingTraderGetsHisOwnPoolWhereThereIsOne() {
        Optional<SocialTarget> trader = Optional.of(SocialTarget.WANDERING_TRADER);
        assertEquals(
                "checks.social.persuade.wandering_trader.success",
                SocialText.flavorPool(SocialAction.PERSUADE, trader, "success"));
        assertEquals(
                "checks.social.deceive.wandering_trader.failure",
                SocialText.flavorPool(SocialAction.DECEIVE, trader, "failure"));
        assertEquals(
                "checks.social.persuade.success",
                SocialText.flavorPool(SocialAction.PERSUADE, Optional.of(SocialTarget.VILLAGER), "success"));
    }

    @Test
    void aWanderingTraderFallsBackToTheGenericPool() {
        assertEquals(
                "checks.social.intimidate.failure",
                SocialText.flavorPool(SocialAction.INTIMIDATE, Optional.of(SocialTarget.WANDERING_TRADER), "failure"));
    }

    @Test
    void everyOutcomeHasALabel() {
        for (SocialOutcome outcome : SocialOutcome.values()) {
            FallbackText.of("checks.social.outcome." + outcome.id());
        }
    }

    @Test
    void theTitleTalksToPeopleFacesHostileMobsAndCalmsNeutralOnes() {
        Component name = Component.literal("the Zombie");
        assertEquals(
                "Talk to the Zombie",
                SocialText.title(menu(option(SocialAction.PERSUADE, SocialMenu.Availability.AVAILABLE, 0)), name)
                        .getString());
        assertEquals(
                "Face the Zombie",
                SocialText.title(
                                menu(
                                        option(SocialAction.INTIMIDATE_MOB, SocialMenu.Availability.FEARLESS, 0),
                                        option(SocialAction.CALM, SocialMenu.Availability.WRONG_TARGET, 0)),
                                name)
                        .getString());
        assertEquals(
                "Calm the Zombie",
                SocialText.title(
                                menu(
                                        option(SocialAction.INTIMIDATE_MOB, SocialMenu.Availability.WRONG_TARGET, 0),
                                        option(SocialAction.CALM, SocialMenu.Availability.AVAILABLE, 0)),
                                name)
                        .getString());
        assertEquals(
                "Face the Zombie",
                SocialText.title(
                                menu(
                                        option(SocialAction.INTIMIDATE_MOB, SocialMenu.Availability.AVAILABLE, 0),
                                        option(SocialAction.CALM, SocialMenu.Availability.AVAILABLE, 0)),
                                name)
                        .getString());
    }

    private static SocialMenu menu(SocialMenu.Option... options) {
        return new SocialMenu(0, List.of(options));
    }

    private static SocialMenu.Option option(SocialAction action, SocialMenu.Availability availability, long ticks) {
        return new SocialMenu.Option(action, 0, false, false, availability, ticks);
    }
}
