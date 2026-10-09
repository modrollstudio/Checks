package studio.modroll.checks.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MatchEntryTest {

    private static final ResourceLocation ZOMBIE = ResourceLocation.parse("minecraft:zombie");

    @Test
    void exactMatchesOnlyItsId() {
        MatchEntry e = MatchEntry.parse("minecraft:zombie");
        assertEquals(3, e.specificity());
        assertTrue(e.matches(ZOMBIE, t -> false));
        assertFalse(e.matches(ResourceLocation.parse("minecraft:pig"), t -> false));
    }

    @Test
    void tagMatchesViaPredicate() {
        MatchEntry e = MatchEntry.parse("#minecraft:undead");
        assertEquals(2, e.specificity());
        Set<ResourceLocation> tags = Set.of(ResourceLocation.parse("minecraft:undead"));
        assertTrue(e.matches(ZOMBIE, tags::contains));
        assertFalse(e.matches(ZOMBIE, t -> false));
    }

    @Test
    void namespaceMatchesWholeNamespace() {
        MatchEntry e = MatchEntry.parse("alexsmobs:*");
        assertEquals(1, e.specificity());
        assertTrue(e.matches(ResourceLocation.parse("alexsmobs:grizzly_bear"), t -> false));
        assertFalse(e.matches(ZOMBIE, t -> false));
    }

    @Test
    void blankEntryIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> MatchEntry.parse("  "));
    }
}
