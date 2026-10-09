package studio.modroll.checks.body;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import studio.modroll.checks.Checks;

/**
 * The 5e armor weight a worn piece belongs to, from the item tags {@code checks:light_armor},
 * {@code checks:medium_armor} and {@code checks:heavy_armor}; lightest first. An untagged piece counts
 * as no armor.
 */
public enum ArmorCategory {
    NONE("none", Optional.empty()),
    LIGHT("light", Optional.of(tag("light_armor"))),
    MEDIUM("medium", Optional.of(tag("medium_armor"))),
    HEAVY("heavy", Optional.of(tag("heavy_armor")));

    public static final StreamCodec<ByteBuf, ArmorCategory> STREAM_CODEC =
            ByteBufCodecs.idMapper(index -> values()[index], ArmorCategory::ordinal);

    private final String id;
    private final Optional<TagKey<Item>> tag;

    ArmorCategory(String id, Optional<TagKey<Item>> tag) {
        this.id = id;
        this.tag = tag;
    }

    public String id() {
        return id;
    }

    /** The heaviest category among the worn pieces decides, as in 5e. */
    public static ArmorCategory heaviest(Iterable<ItemStack> worn) {
        ArmorCategory heaviest = NONE;
        for (ItemStack piece : worn) {
            ArmorCategory category = of(piece);
            if (category.compareTo(heaviest) > 0) {
                heaviest = category;
            }
        }
        return heaviest;
    }

    /** A piece in more than one tag counts as the heaviest of them. */
    public static ArmorCategory of(ItemStack piece) {
        for (int i = values().length - 1; i > 0; i--) {
            ArmorCategory category = values()[i];
            if (category.tag.filter(piece::is).isPresent()) {
                return category;
            }
        }
        return NONE;
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Checks.id(path));
    }
}
