package com.example.miyu;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** cmd: 0 = следовать, 1 = ждать, 2 = телепорт к игроку, 10..17 = жесты 1..8 */
public record CmdPayload(int id, int cmd) implements CustomPacketPayload {
    public static final Type<CmdPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Miyu.ID, "cmd"));
    public static final StreamCodec<ByteBuf, CmdPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CmdPayload::id, ByteBufCodecs.VAR_INT, CmdPayload::cmd, CmdPayload::new);

    @Override public Type<CmdPayload> type() { return TYPE; }

    public static void handle(CmdPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player.level().getEntity(p.id()) instanceof MiyuEntity m && m.isOwnedBy(player)) {
                if (p.cmd() >= 100) { m.startScene(p.cmd() - 100); return; }
                if (p.cmd() == 99) { m.stopScene(); return; }
                switch (p.cmd()) {
                    case 0 -> m.setOrderedToSit(false);
                    case 1 -> m.setOrderedToSit(true);
                    case 2 -> { m.setOrderedToSit(false); m.teleportTo(player.getX(), player.getY(), player.getZ()); }
                    default -> { if (p.cmd() >= 10 && p.cmd() <= 17) m.playGesture(p.cmd() - 9); }
                }
            }
        });
    }
}
