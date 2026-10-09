package studio.modroll.checks.creation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreationPayloadTest {

    @Test
    void offerSurvivesTheWire() {
        CreationOffer offer = Offers.rolled(CreationMethod.ROLL, List.of(17, 9, 12, 12, 14, 6));
        ByteBuf buffer = Unpooled.buffer();

        CreationOfferPayload.STREAM_CODEC.encode(buffer, new CreationOfferPayload(offer));

        assertEquals(offer, CreationOfferPayload.STREAM_CODEC.decode(buffer).offer());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void offerWithPresetsSurvivesTheWire() {
        CreationOffer offer = Offers.withSpeciesSkillChoices();
        ByteBuf buffer = Unpooled.buffer();

        CreationOfferPayload.STREAM_CODEC.encode(buffer, new CreationOfferPayload(offer));

        assertEquals(offer, CreationOfferPayload.STREAM_CODEC.decode(buffer).offer());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void submissionSurvivesTheWire() {
        CreationSubmission submission = new CreationSubmission(
                CreationMethod.POINT_BUY,
                List.of(15, 15, 15, 8, 8, 8),
                List.of(2, 0, 1, 0, 0, 0),
                Offers.TWO_SKILLS,
                new PresetChoices(
                        Optional.of(Offers.ELF), Optional.of(Offers.SOLDIER), Optional.empty(), Optional.empty()),
                List.of(Offers.PERCEPTION));
        ByteBuf buffer = Unpooled.buffer();

        CreationSubmitPayload.STREAM_CODEC.encode(buffer, new CreationSubmitPayload(submission));

        assertEquals(
                submission, CreationSubmitPayload.STREAM_CODEC.decode(buffer).submission());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void rollRequestNamesOnlyTheMethod() {
        ByteBuf buffer = Unpooled.buffer();

        CreationRollPayload.STREAM_CODEC.encode(buffer, new CreationRollPayload(CreationMethod.HARDCORE));

        assertEquals(
                CreationMethod.HARDCORE,
                CreationRollPayload.STREAM_CODEC.decode(buffer).method());
    }
}
