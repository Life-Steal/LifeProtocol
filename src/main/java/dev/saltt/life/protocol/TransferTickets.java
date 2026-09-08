package dev.saltt.life.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Signs and verifies {@link TransferTicket}s with a secret both the hub and the nodes hold.
 * Lives in the protocol so both ends compute the signature over exactly the same bytes.
 */
public final class TransferTickets {

    private static final String ALGORITHM = "HmacSHA256";

    private TransferTickets() {
    }

    public static TransferTicket sign(TransferTicket unsigned, String secret) {
        return unsigned.toBuilder()
                .setHmac(ByteString.copyFrom(mac(unsigned, secret)))
                .build();
    }

    public static byte[] toPayload(TransferTicket ticket) {
        return ticket.toByteArray();
    }

    /**
     * The ticket inside a referral payload, only if its signature is valid and it was issued
     * within {@code maxAgeMillis} of now, in either direction: the hub's and the node's clocks
     * need not agree to the millisecond.
     */
    public static Optional<TransferTicket> verify(byte[] payload, String secret, long maxAgeMillis,
                                                  long nowMillis) {
        if (payload == null || payload.length == 0) {
            return Optional.empty();
        }
        TransferTicket ticket;
        try {
            ticket = TransferTicket.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            return Optional.empty();
        }
        if (Math.abs(nowMillis - ticket.getIssuedAtMillis()) > maxAgeMillis) {
            return Optional.empty();
        }
        byte[] expected = mac(ticket.toBuilder().clearHmac().build(), secret);
        if (!MessageDigest.isEqual(expected, ticket.getHmac().toByteArray())) {
            return Optional.empty();
        }
        return Optional.of(ticket);
    }

    private static byte[] mac(TransferTicket ticket, String secret) {
        String canonical = ticket.getMatchId() + '\n' + ticket.getPlayerUuid() + '\n'
                + ticket.getIssuedAtMillis() + '\n' + ticket.getNodeId() + '\n'
                + ticket.getSpectator();
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHM + " unavailable", e);
        }
    }
}
