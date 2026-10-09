package studio.modroll.checks.uishots;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import studio.modroll.checks.creation.CreationMethod;
import studio.modroll.checks.creation.CreationStep;
import studio.modroll.checks.creation.client.CharacterCreationScreen;
import studio.modroll.checks.sheet.SheetLayout;
import studio.modroll.checks.sheet.StatSheetRequestPayload;
import studio.modroll.checks.sheet.client.StatSheetScreen;
import studio.modroll.checks.social.SocialMenuRequestPayload;
import studio.modroll.checks.social.client.SocialScreen;
import studio.modroll.checks.ui.Area;

/**
 * Dev-only screenshot run: opens a superflat world and saves every Checks screen at several GUI scales,
 * then quits. Each loader's {@code runUiShots} task calls {@link #tick} every client tick and names the
 * output folder with the {@value #DIRECTORY_PROPERTY} system property.
 */
public final class UiShots {

    public static final String DIRECTORY_PROPERTY = "checks.uishots.dir";

    private static final List<Integer> GUI_SCALES = List.of(2, 3, 4);
    private static final String WORLD = "uishots";
    private static final long SEED = 1L;
    /** Long enough for a resize or a screen change to reach a rendered frame. */
    private static final int SETTLE_TICKS = 3;
    /** Long enough for the server to answer and the roll line and speech bubble to fade in. */
    private static final int REPLY_TICKS = 10;

    /** The first level with an ability score improvement waiting. */
    private static final int IMPROVEMENT_LEVEL = 4;

    private static final int OVERVIEW_TAB = 0;
    private static final int SKILLS_TAB = 1;

    private static final String LEVEL_UP = "screen.checks.level_up";
    private static final String IMPROVEMENT_TITLE = "screen.checks.improvement.title";
    private static final String PLUS = "+";
    private static final Set<String> FORWARD_BUTTONS =
            Set.of("screen.checks.creation.next", "screen.checks.creation.confirm");

    private static ShotScript script;

    private UiShots() {}

    public static Optional<Path> directory() {
        return Optional.ofNullable(System.getProperty(DIRECTORY_PROPERTY)).map(Path::of);
    }

    public static void tick() {
        if (script == null) {
            script = script(new Camera(directory().orElseThrow()));
        }
        Camera.forceSize();
        script.tick();
    }

    private static ShotScript script(Camera camera) {
        ShotScript script = new ShotScript()
                .waitFor(
                        "the title screen",
                        () -> Minecraft.getInstance().getOverlay() == null && Minecraft.getInstance().screen != null)
                .then(UiShots::quietOptions)
                .then(UiShots::createWorld)
                .waitFor("character creation", () -> screen() instanceof CharacterCreationScreen);
        for (CreationStep step : CreationStep.values()) {
            String name = "creation-" + (step.ordinal() + 1) + "-" + step.id();
            if (step == CreationStep.SCORES) {
                script.then(UiShots::chooseStandardArray);
                shootAtEveryScale(script, camera, name + "-unplaced");
            }
            script.then(() -> fillCreationPage(step));
            shootAtEveryScale(script, camera, name);
            script.then(() -> press(forwardButton()));
        }
        script.waitFor("creation to close", () -> screen() == null)
                .then(UiShots::placeVillager)
                .waitFor("the villager under the crosshair", () -> SocialScreen.target()
                        .isPresent())
                .then(() -> CharacterCreationScreen.sendToServer(
                        new SocialMenuRequestPayload(SocialScreen.target().orElseThrow())))
                .waitFor("the social menu", () -> screen() instanceof SocialScreen);
        shootAtEveryScale(script, camera, "social-menu");
        script.then(() -> press(firstActiveButton()))
                .waitFor("the social menu to close", () -> screen() == null)
                .waitTicks(REPLY_TICKS);
        shootAtEveryScale(script, camera, "roll-hud-and-speech-bubble");
        script.then(() -> CharacterCreationScreen.sendToServer(StatSheetRequestPayload.INSTANCE))
                .waitFor("the stat sheet", () -> screen() instanceof StatSheetScreen);
        for (int scale : GUI_SCALES) {
            shoot(script, camera, scale, "stat-sheet");
            script.then(UiShots::hoverFirstAbility).waitTicks(SETTLE_TICKS);
            script.then(() -> camera.save("stat-sheet-hover-gui" + scale));
            script.then(() -> clickTab(SKILLS_TAB));
            shoot(script, camera, scale, "stat-sheet-skills");
            script.then(UiShots::hoverFirstSkillDot).waitTicks(SETTLE_TICKS);
            script.then(() -> camera.save("stat-sheet-skills-hover-gui" + scale));
            script.then(() -> clickTab(OVERVIEW_TAB));
        }
        script.then(() -> command("checks level @p set " + IMPROVEMENT_LEVEL))
                .waitTicks(REPLY_TICKS)
                .then(() -> CharacterCreationScreen.sendToServer(StatSheetRequestPayload.INSTANCE))
                .waitFor("the Level up! button", () -> button(LEVEL_UP).isPresent())
                .then(() -> press(button(LEVEL_UP).orElseThrow()))
                .waitFor("the level-up screen", () -> titled(IMPROVEMENT_TITLE))
                .then(() -> press(button(PLUS).orElseThrow()));
        shootAtEveryScale(script, camera, "level-up");
        return script.then(() -> Minecraft.getInstance().stop());
    }

    private static void shootAtEveryScale(ShotScript script, Camera camera, String name) {
        GUI_SCALES.forEach(scale -> shoot(script, camera, scale, name));
    }

    /** The mouse waits in the corner so nothing under it lights up; vanilla toasts would only add noise. */
    private static void shoot(ShotScript script, Camera camera, int scale, String name) {
        script.then(() -> Camera.guiScale(scale))
                .then(() -> Camera.mouseAt(0, 0))
                .then(() -> Minecraft.getInstance().getToasts().clear())
                .waitTicks(SETTLE_TICKS)
                .then(() -> camera.save(name + "-gui" + scale));
    }

    /** No sound, no pausing when the window loses focus, no onboarding or tutorial over the screens. */
    private static void quietOptions() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
        minecraft.options.pauseOnLostFocus = false;
        minecraft.options.onboardAccessibility = false;
        minecraft.options.tutorialStep = TutorialSteps.NONE;
        // A hidden window gets no frame callbacks on Wayland, and vsync would then block rendering.
        minecraft.options.enableVsync().set(false);
        minecraft.getWindow().updateVsync(false);
    }

    private static void createWorld() {
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(
                WORLD, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        Minecraft.getInstance()
                .createWorldOpenFlows()
                .createFreshLevel(
                        WORLD,
                        settings,
                        new WorldOptions(SEED, false, false),
                        registries -> registries
                                .registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT)
                                .value()
                                .createWorldDimensions(),
                        new TitleScreen());
    }

    /** A farmer facing the player, close enough to talk to, the player looking straight at it. */
    private static void placeVillager() {
        command("tp @p 0.5 -60 0.5 0 0");
        command("summon villager 0.5 -60 3 {NoAI:1b,Rotation:[180f,0f],"
                + "VillagerData:{profession:\"minecraft:farmer\",level:2,type:\"minecraft:plains\"}}");
    }

    private static void command(String command) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static SheetLayout sheetLayout() {
        return (SheetLayout) Fields.get(StatSheetScreen.class, screen(), "layout");
    }

    private static void hoverFirstAbility() {
        Area abilities = sheetLayout().abilities();
        Camera.mouseAt(abilities.left() + SheetLayout.GAP, abilities.top() + SheetLayout.GAP);
    }

    /** Over the first skill's proficiency dot, which shows the legend. */
    private static void hoverFirstSkillDot() {
        Area skills = sheetLayout().skills();
        Camera.mouseAt(skills.left() + 1, skills.top() + SheetLayout.GAP);
    }

    private static void clickTab(int index) {
        Area tab = sheetLayout().tabs().get(index);
        screen().mouseClicked(tab.left() + tab.width() / 2.0, tab.top() + tab.height() / 2.0, 0);
    }

    private static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    private static CharacterCreationScreen creation() {
        if (screen() instanceof CharacterCreationScreen creation) {
            return creation;
        }
        throw new IllegalStateException("UI shots expected character creation, found " + screen());
    }

    /** Expects every page, as with presets on, so each file is named after the page it shows. */
    private static void fillCreationPage(CreationStep expected) {
        CharacterCreationScreen creation = creation();
        if (creation.draft().step() != expected) {
            throw new IllegalStateException("UI shots expected the " + expected.id() + " page, found "
                    + creation.draft().step().id());
        }
        CreationFiller.fill(creation.draft());
        creation.refresh();
    }

    /** The scores page with its chips still to place. */
    private static void chooseStandardArray() {
        creation().draft().select(CreationMethod.STANDARD_ARRAY);
        creation().refresh();
    }

    private static Button forwardButton() {
        return buttons().stream()
                .filter(button -> button.getMessage().getContents() instanceof TranslatableContents contents
                        && FORWARD_BUTTONS.contains(contents.getKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("UI shots found no Next or Confirm button"));
    }

    /** The first active button whose label is {@code key}, translated or literal. */
    private static Optional<Button> button(String key) {
        return buttons().stream()
                .filter(button -> button.active && key.equals(labelKey(button.getMessage())))
                .findFirst();
    }

    private static boolean titled(String key) {
        return screen() != null && key.equals(labelKey(screen().getTitle()));
    }

    private static String labelKey(Component label) {
        return label.getContents() instanceof TranslatableContents contents ? contents.getKey() : label.getString();
    }

    private static Button firstActiveButton() {
        return buttons().stream()
                .filter(button -> button.active)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("UI shots found no active button on " + screen()));
    }

    private static List<Button> buttons() {
        return screen().children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toList();
    }

    /** A disabled button means the page is incomplete; pressing it anyway would stall the run. */
    private static void press(Button button) {
        if (!button.active) {
            throw new IllegalStateException(
                    "UI shots cannot press the disabled " + button.getMessage().getString() + " button on " + screen());
        }
        button.onPress();
    }
}
