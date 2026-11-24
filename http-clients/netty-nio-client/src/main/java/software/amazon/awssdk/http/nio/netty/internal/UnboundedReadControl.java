package software.amazon.awssdk.http.nio.netty.internal;

import io.netty.channel.ChannelHandlerContext;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.http.Protocol;

/** HTTP/1 SDK responses have one in-flight request per pooled connection. */
public final class UnboundedReadControl {
    private static final boolean PROBE = Boolean.getBoolean("sdk.followup.probe");
    private static volatile boolean recorded;

    private UnboundedReadControl() { }

    public static boolean enableIfEligible(ChannelHandlerContext ctx) {
        CompletableFuture<Protocol> protocol = ctx.channel().attr(ChannelAttributeKey.PROTOCOL_FUTURE).get();
        if (protocol == null || !protocol.isDone() || protocol.isCompletedExceptionally()
            || protocol.getNow(null) != Protocol.HTTP1_1
            || ctx.channel().attr(ChannelAttributeKey.REQUEST_CONTEXT_KEY).get() == null
            || ctx.channel().config().isAutoRead()) {
            return false;
        }
        ctx.channel().config().setAutoRead(true);
        if (PROBE && !recorded) {
            synchronized (UnboundedReadControl.class) {
                if (!recorded) {
                    recorded = true;
                    System.err.println("FOLLOWUP_EXECUTED unbounded-http1-auto-read="
                        + ctx.channel().config().isAutoRead());
                }
            }
        }
        return true;
    }
}
