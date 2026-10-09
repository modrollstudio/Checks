package studio.modroll.checks.social.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import studio.modroll.checks.social.HostilityPayload;
import studio.modroll.checks.social.ItemArc;
import studio.modroll.checks.social.ItemArcPayload;
import studio.modroll.checks.social.SpeechBubblePayload;

/**
 * Speech bubbles above heads, pickpocketed items in flight and Insight's markers over mobs after the
 * player, drawn in the world after entities, each loader calling {@link #render}. Time is the client
 * level's game time; whatever outlives its entity is dropped. A bubble is dark text on a soft light
 * background with rounded corners and a small tail pointing down at the head, wrapped at a fixed width and
 * faded in and out (see {@link BubbleFade}); closer than {@link BubbleScale#FULL_SIZE_DISTANCE} blocks it
 * shrinks, so it never looks bigger than it does from there. A marker is a small red "!"
 * just above the head, shown until the server says otherwise. Each shows only while the camera can see
 * its entity's head past blocks; then it is drawn whole and at full brightness, whatever is in front of it.
 *
 * <p>A bubble is drawn without a depth test, its edge, then its fill, then its text over them. Vanilla
 * draws text backgrounds as a quad in the same distance-sorted buffer as the letters, which leaves some
 * letters under the background depending on the view angle; the edge and fill each get their own flush
 * of a separate buffer, so each lands on top of the last.
 */
public final class ClientSocialFeel {

    private static final double ABOVE_HEAD = 0.25;
    private static final double ABOVE_NAME_TAG = 0.3;
    /** In font pixels: about three blocks at {@link BubbleScale#TEXT_SCALE}. */
    private static final int MAX_LINE_WIDTH = 120;

    private static final int TEXT_COLOR = 0x3A2D1E;
    private static final int FILL_COLOR = 0xF3ECDC;
    private static final int EDGE_COLOR = 0x9C8762;
    private static final int TEXT_ALPHA = 255;
    private static final int BACKGROUND_ALPHA = 240;
    /** In font pixels, from the bubble's edge to its text. */
    private static final int PADDING_X = 4;

    private static final int PADDING_Y = 3;
    /** Rows of the stepped tail, each one pixel narrower on both sides. */
    private static final int TAIL = 4;

    private static final int CORNER = 1;
    private static final RenderType BACKGROUND = RenderType.textBackgroundSeeThrough();
    /** Below this the font would draw the text opaque, as vanilla's own fades stop at. */
    private static final int MIN_VISIBLE_ALPHA = 8;

    private static final int NO_BACKGROUND = 0;
    private static final float FLAT = 0;
    private static final double HAND_HEIGHT = 0.5;
    private static final float ITEM_SCALE = 0.5f;
    private static final float SPIN_DEGREES_PER_TICK = 20;

    private static final Component MARKER = Component.literal("!");
    private static final int MARKER_COLOR = 0xFF5555;
    private static final int MARKER_ALPHA = 208;
    private static final float MARKER_SCALE = 0.04f;
    private static final double MARKER_ABOVE_HEAD = 0.25;

    private static final Map<Integer, Bubble> BUBBLES = new HashMap<>();
    private static final List<Flight> FLIGHTS = new ArrayList<>();
    private static Set<Integer> marked = Set.of();

    private ClientSocialFeel() {}

    private record Bubble(Component line, long start, long until) {}

    private record Flight(int fromId, int toId, ItemStack item, boolean caught, long start) {}

    /** A newer line replaces the entity's bubble. */
    public static void say(SpeechBubblePayload payload) {
        long now = now();
        BUBBLES.put(payload.entityId(), new Bubble(payload.line(), now, now + payload.durationTicks()));
    }

    public static void fly(ItemArcPayload payload) {
        FLIGHTS.add(new Flight(payload.fromId(), payload.toId(), payload.item(), payload.caught(), now()));
    }

    /** The server's latest set of mobs to mark replaces the last one. */
    public static void sense(HostilityPayload payload) {
        marked = Set.copyOf(payload.entityIds());
    }

    public static void clear() {
        BUBBLES.clear();
        FLIGHTS.clear();
        marked = Set.of();
    }

    /** {@code poseStack} is camera-relative, as entities are drawn. */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        BUBBLES.values().removeIf(bubble -> bubble.until() <= now);
        FLIGHTS.removeIf(flight -> now - flight.start() > ItemArc.ticks(flight.caught()));
        for (Entity mob : visibleFarthestFirst(level, camera, partialTick, marked)) {
            drawMarker(poseStack, buffers, camera, mob, partialTick);
        }
        visibleFarthestFirst(level, camera, partialTick, BUBBLES.keySet()).forEach(speaker -> {
            Bubble bubble = BUBBLES.get(speaker.getId());
            float fade = BubbleFade.at(now - bubble.start() + partialTick, bubble.until() - now - partialTick);
            drawBubble(poseStack, buffers, camera, speaker, bubble.line(), partialTick, fade);
        });
        for (Flight flight : FLIGHTS) {
            Entity from = level.getEntity(flight.fromId());
            Entity to = level.getEntity(flight.toId());
            if (from != null && to != null) {
                drawFlight(poseStack, buffers, camera, level, flight, from, to, partialTick);
            }
        }
    }

    /** Farthest first, so a nearer bubble or marker covers a farther one. */
    private static List<Entity> visibleFarthestFirst(
            ClientLevel level, Camera camera, float partialTick, Set<Integer> entityIds) {
        return entityIds.stream()
                .map(level::getEntity)
                .filter(speaker -> speaker != null && canSeeHead(level, camera, speaker, partialTick))
                .sorted(Comparator.comparingDouble((Entity speaker) ->
                                speaker.getPosition(partialTick).distanceToSqr(camera.getPosition()))
                        .reversed())
                .toList();
    }

    /** The tail's tip sits just above the head; the bubble, its text centered on each line, sits on the tail. */
    private static void drawBubble(
            PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            Entity speaker,
            Component line,
            float partialTick,
            float fade) {
        double height = speaker.getBbHeight() + ABOVE_HEAD + (speaker.shouldShowName() ? ABOVE_NAME_TAG : 0);
        Vec3 at = speaker.getPosition(partialTick).add(0, height, 0).subtract(camera.getPosition());
        int textAlpha = (int) (TEXT_ALPHA * fade);
        if (textAlpha <= MIN_VISIBLE_ALPHA) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(line, MAX_LINE_WIDTH);
        int widest = lines.stream().mapToInt(font::width).max().orElse(0);
        int backgroundAlpha = (int) (BACKGROUND_ALPHA * fade);
        int bottom = -TAIL - 1;
        int top = bottom - 2 * PADDING_Y - lines.size() * font.lineHeight + 1;
        int left = -widest / 2 - PADDING_X;
        int right = left + widest + 2 * PADDING_X;
        poseStack.pushPose();
        float scale = BubbleScale.at(at.length());
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(scale, -scale, scale);
        Matrix4f pose = poseStack.last().pose();
        drawShape(
                pose,
                buffers,
                left - 1,
                top - 1,
                right + 1,
                bottom + 1,
                CORNER + 1,
                FastColor.ARGB32.color(backgroundAlpha, EDGE_COLOR));
        drawShape(pose, buffers, left, top, right, bottom, CORNER, FastColor.ARGB32.color(backgroundAlpha, FILL_COLOR));
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence text = lines.get(i);
            font.drawInBatch(
                    text,
                    -font.width(text) / 2f,
                    top + PADDING_Y + i * font.lineHeight,
                    FastColor.ARGB32.color(textAlpha, TEXT_COLOR),
                    false,
                    pose,
                    buffers,
                    Font.DisplayMode.SEE_THROUGH,
                    NO_BACKGROUND,
                    LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    private static void drawMarker(
            PoseStack poseStack, MultiBufferSource buffers, Camera camera, Entity mob, float partialTick) {
        double height = mob.getBbHeight() + MARKER_ABOVE_HEAD + (mob.shouldShowName() ? ABOVE_NAME_TAG : 0);
        Vec3 at = mob.getPosition(partialTick).add(0, height, 0).subtract(camera.getPosition());
        Font font = Minecraft.getInstance().font;
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(MARKER_SCALE, -MARKER_SCALE, MARKER_SCALE);
        font.drawInBatch(
                MARKER,
                -font.width(MARKER) / 2f,
                -font.lineHeight,
                FastColor.ARGB32.color(MARKER_ALPHA, MARKER_COLOR),
                false,
                poseStack.last().pose(),
                buffers,
                Font.DisplayMode.SEE_THROUGH,
                NO_BACKGROUND,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    /**
     * A box with its corners cut {@code corner} pixels deep and a stepped tail under its middle, as quads
     * that never overlap, so a faded shape stays evenly see-through; flushed at once so what follows lands on top.
     */
    private static void drawShape(
            Matrix4f pose, MultiBufferSource buffers, int left, int top, int right, int bottom, int corner, int color) {
        VertexConsumer quads = buffers.getBuffer(BACKGROUND);
        for (int i = 0; i < corner; i++) {
            int cut = corner - i;
            rect(quads, pose, left + cut, top + i, right - cut, top + i + 1, color);
            rect(quads, pose, left + cut, bottom - i - 1, right - cut, bottom - i, color);
        }
        rect(quads, pose, left, top + corner, right, bottom - corner, color);
        for (int row = 0; row < TAIL; row++) {
            int half = TAIL - row;
            rect(quads, pose, -half, bottom + row, half, bottom + row + 1, color);
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(BACKGROUND);
        }
    }

    private static void rect(
            VertexConsumer quads, Matrix4f pose, float left, float top, float right, float bottom, int color) {
        corner(quads, pose, left, bottom, color);
        corner(quads, pose, right, bottom, color);
        corner(quads, pose, right, top, color);
        corner(quads, pose, left, top, color);
    }

    private static void corner(VertexConsumer quad, Matrix4f pose, float x, float y, int color) {
        quad.addVertex(pose, x, y, FLAT).setColor(color).setLight(LightTexture.FULL_BRIGHT);
    }

    /** Blocks the camera sees through, such as glass, do not hide the head; entities never do. */
    private static boolean canSeeHead(ClientLevel level, Camera camera, Entity speaker, float partialTick) {
        ClipContext sight = new ClipContext(
                camera.getPosition(),
                speaker.getEyePosition(partialTick),
                ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE,
                camera.getEntity());
        return level.clip(sight).getType() == HitResult.Type.MISS;
    }

    private static void drawFlight(
            PoseStack poseStack,
            MultiBufferSource buffers,
            Camera camera,
            ClientLevel level,
            Flight flight,
            Entity from,
            Entity to,
            float partialTick) {
        double tick = level.getGameTime() - flight.start() + partialTick;
        Vec3 world = ItemArc.position(
                hand(from, partialTick), hand(to, partialTick), from.getPosition(partialTick).y, flight.caught(), tick);
        Vec3 at = world.subtract(camera.getPosition());
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) tick * SPIN_DEGREES_PER_TICK));
        Minecraft.getInstance()
                .getItemRenderer()
                .renderStatic(
                        flight.item(),
                        ItemDisplayContext.GROUND,
                        LevelRenderer.getLightColor(level, BlockPos.containing(world)),
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        buffers,
                        level,
                        flight.fromId());
        poseStack.popPose();
    }

    private static Vec3 hand(Entity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0, entity.getBbHeight() * HAND_HEIGHT, 0);
    }

    private static long now() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }
}
