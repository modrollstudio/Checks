package studio.modroll.checks.social;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.Checks;

public enum SocialAction {
    PERSUADE("persuade", "persuasion", Optional.empty(), Set.of(SocialTarget.VILLAGER, SocialTarget.WANDERING_TRADER)),
    DECEIVE(
            "deceive",
            "deception",
            Optional.of("insight"),
            Set.of(SocialTarget.VILLAGER, SocialTarget.WANDERING_TRADER, SocialTarget.PIGLIN)),
    PLEAD("plead", "persuasion", Optional.empty(), Set.of(SocialTarget.VILLAGER)),
    LIE("lie", "deception", Optional.empty(), Set.of(SocialTarget.VILLAGER)),
    INTIMIDATE("intimidate", "intimidation", Optional.empty(), Set.of(SocialTarget.VILLAGER, SocialTarget.PIGLIN)),
    PICKPOCKET("pickpocket", "sleight_of_hand", Optional.of("perception"), Set.of(SocialTarget.VILLAGER)),
    INTIMIDATE_MOB("intimidate_mob", "intimidation", Optional.empty(), Set.of(SocialTarget.HOSTILE)),
    CALM("calm", "persuasion", Optional.empty(), Set.of(SocialTarget.NEUTRAL));

    private final String id;
    private final ResourceLocation skill;
    private final Optional<ResourceLocation> passiveDc;
    private final Set<SocialTarget> targets;

    SocialAction(String id, String skill, Optional<String> passiveDc, Set<SocialTarget> targets) {
        this.id = id;
        this.skill = Checks.id(skill);
        this.passiveDc = passiveDc.map(Checks::id);
        this.targets = targets;
    }

    public String id() {
        return id;
    }

    public ResourceLocation skill() {
        return skill;
    }

    public Optional<ResourceLocation> passiveDc() {
        return passiveDc;
    }

    public boolean worksOn(SocialTarget target) {
        return targets.contains(target);
    }

    public boolean onMobs() {
        return targets.stream().allMatch(SocialTarget::mob);
    }

    public boolean deEscalates() {
        return this == PLEAD || this == LIE;
    }

    public boolean villagersGuardAgainst() {
        return this == PICKPOCKET || this == INTIMIDATE;
    }

    public static Optional<SocialAction> byId(String id) {
        return Arrays.stream(values()).filter(action -> action.id().equals(id)).findFirst();
    }
}
