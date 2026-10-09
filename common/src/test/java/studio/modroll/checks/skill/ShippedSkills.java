package studio.modroll.checks.skill;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import studio.modroll.checks.Checks;
import studio.modroll.checks.api.Skill;
import studio.modroll.checks.data.SkillParser;

/** Parses the skill files Checks ships in its built-in datapack, straight from the source tree. */
public final class ShippedSkills {

    private static final Path DIRECTORY = Path.of("src/main/resources/data/checks/checks/skill");
    private static final String SUFFIX = ".json";

    private ShippedSkills() {}

    public static Map<ResourceLocation, Skill> load(Consumer<String> warn) {
        Map<ResourceLocation, Skill> skills = new HashMap<>();
        for (Path file : files()) {
            String name = file.getFileName().toString();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    Checks.MOD_ID, name.substring(0, name.length() - SUFFIX.length()));
            skills.put(
                    id, SkillParser.parse(id, JsonParser.parseString(read(file)).getAsJsonObject(), warn));
        }
        return skills;
    }

    private static List<Path> files() {
        try (Stream<Path> files = Files.list(DIRECTORY)) {
            return files.filter(file -> file.toString().endsWith(SUFFIX)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
