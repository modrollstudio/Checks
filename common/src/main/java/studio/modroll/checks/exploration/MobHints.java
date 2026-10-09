package studio.modroll.checks.exploration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import studio.modroll.checks.text.FallbackText;

/**
 * A monster lore hint: the lang line {@code checks.lore.<namespace>.<path>} for the mob's type, which Checks
 * ships for every vanilla mob and a mod or resource pack can add for its own. Without one, a hint built from
 * the mob itself: about how many hearts it has and, if it attacks, how many hearts a hit takes, both to the
 * nearest half heart; the enchantment it is weak to, if any; and whether fire can hurt it.
 */
public final class MobHints {

    private static final String HINTS = "checks.lore.";
    private static final String GENERATED = "checks.exploration.lore_hint.";
    private static final double HEALTH_PER_HEART = 2;
    private static final double HALF_HEART = 0.5;
    private static final double ONE_HEART = 1;

    /** The enchantment a mob's type is weak to, in this order when a type is in more than one tag. */
    enum Weakness {
        SMITE(EntityTypeTags.UNDEAD),
        BANE_OF_ARTHROPODS(EntityTypeTags.ARTHROPOD),
        IMPALING(EntityTypeTags.AQUATIC);

        private final TagKey<EntityType<?>> mobs;

        Weakness(TagKey<EntityType<?>> mobs) {
            this.mobs = mobs;
        }

        private static Optional<Weakness> of(EntityType<?> type) {
            for (Weakness weakness : values()) {
                if (type.is(weakness.mobs)) {
                    return Optional.of(weakness);
                }
            }
            return Optional.empty();
        }

        private String key() {
            return GENERATED + "weak." + name().toLowerCase(Locale.ROOT);
        }
    }

    private MobHints() {}

    public static Component hint(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return FallbackText.ofOrElse(HINTS + id.getNamespace() + "." + id.getPath(), () -> generated(mob)
                .getString());
    }

    /** The hint built from the mob itself, e.g. {@code About 10 hearts, hits for 1.5. Weak to Smite.} */
    public static Component generated(Mob mob) {
        return generated(
                hearts(mob.getMaxHealth()),
                hits(mob),
                Weakness.of(mob.getType()),
                mob.getType().fireImmune());
    }

    static Component generated(double hearts, OptionalDouble hits, Optional<Weakness> weakness, boolean fireproof) {
        Component health = hearts == ONE_HEART
                ? FallbackText.of(GENERATED + "heart")
                : FallbackText.of(GENERATED + "hearts", number(hearts));
        List<Component> sentences = new ArrayList<>();
        sentences.add(
                hits.isPresent()
                        ? FallbackText.of(GENERATED + "health_and_hits", health, number(hits.getAsDouble()))
                        : FallbackText.of(GENERATED + "health", health));
        weakness.ifPresent(weak -> sentences.add(FallbackText.of(weak.key())));
        if (fireproof) {
            sentences.add(FallbackText.of(GENERATED + "fireproof"));
        }
        return ComponentUtils.formatList(sentences, CommonComponents.SPACE);
    }

    /** Health points in hearts, rounded to the nearest half heart, at least half a heart. */
    static double hearts(double health) {
        return Math.max(HALF_HEART, Math.round(health) / HEALTH_PER_HEART);
    }

    /** Attack damage in hearts, for a mob that attacks. */
    private static OptionalDouble hits(Mob mob) {
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null || attack.getValue() <= 0) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(hearts(attack.getValue()));
    }

    /** {@code 15} for a whole number of hearts, {@code 1.5} for a half. */
    private static String number(double hearts) {
        return hearts == Math.rint(hearts)
                ? Long.toString(Math.round(hearts))
                : String.format(Locale.ROOT, "%.1f", hearts);
    }
}
