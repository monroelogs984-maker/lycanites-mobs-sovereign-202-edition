package com.lycanitesmobs.client.event;

import com.lycanitesmobs.client.effect.FearAudioHandler;
import com.lycanitesmobs.client.effect.FearHeartbeatSound;
import com.lycanitesmobs.client.effect.FearVisualHandler;
import com.lycanitesmobs.client.effect.MuffledSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client side of the Fear effect: light dimming (with LightTextureMixin), muffled sound and the heartbeat.
 * Port: the fear parts of the official ClientEventListener.
 */
public class FearClientEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(FearClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(FearClientEvents::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(FearClientEvents::onPlaySound);
    }

    public static void onClientTick(ClientTickEvent.Pre event) {
        FearVisualHandler.tick();
        FearAudioHandler.tick();
    }

    /** The dimmed lightmap only applies to the world; it's swapped back to the clean one for the hand and GUI. **/
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            FearVisualHandler.uploadDimmedForWorld();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            FearVisualHandler.restoreCleanAfterWorld();
        }
    }

    public static void onPlaySound(PlaySoundEvent event) {
        if (!FearAudioHandler.isActive()) return;
        SoundInstance sound = event.getSound();
        if (sound == null || sound instanceof FearHeartbeatSound) return;

        float volumeScale = FearAudioHandler.computeVolumeScale(sound);
        float pitchScale = FearAudioHandler.computePitchScale();
        if (volumeScale >= 1.0F && pitchScale >= 1.0F) return;
        event.setSound(MuffledSoundInstance.wrap(sound, volumeScale, pitchScale));
    }
}
