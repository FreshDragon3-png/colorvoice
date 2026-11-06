package de.colorvoice;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.VoicechatServerApi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ColorVoicePlugin implements VoicechatPlugin {
    private VoicechatServerApi serverApi;
    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();

    @Override public String getPluginId() { return "colorvoice"; }

    @Override public void initialize(VoicechatApi api) {}

    @Override
    public void registerEvents(EventRegistration reg) {
        reg.registerEvent(VoicechatServerStartedEvent.class, e -> serverApi = e.getVoicechat());
        reg.registerEvent(MicrophonePacketEvent.class, this::onMic);
        reg.registerEvent(PlayerDisconnectedEvent.class, e -> {
            OpusDecoder d = decoders.remove(e.getPlayerUuid());
            if (d != null) d.close();
            if (ColorVoiceMod.speech != null) ColorVoiceMod.speech.remove(e.getPlayerUuid());
        });
    }

    private void onMic(MicrophonePacketEvent event) {
        if (serverApi == null || ColorVoiceMod.speech == null) return;
        VoicechatConnection con = event.getSenderConnection();
        if (con == null) return;
        byte[] opus = event.getPacket().getOpusEncodedData();
        if (opus == null || opus.length == 0) return;
        UUID id = con.getPlayer().getUuid();
        OpusDecoder dec = decoders.computeIfAbsent(id, k -> serverApi.createDecoder());
        short[] pcm = dec.decode(opus);
        ColorVoiceMod.speech.feed(id, pcm);
    }
}
