package client.gui;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * SoundManager - Handles All Game Audio
 * 
 * Supports: 
 * - Sound Effects
 * - Sound Tracks
 * - Volume Control
 */

public class SoundManager {

    private static final String SOUNDS_PATH = "assets/sounds/";

    private Map<String, Clip> soundEffects;

    private Map<String, Clip> musicTracks;
    private Clip currentMusic;
    private String currentMusicName;

    private float sfxVolume = 0.8f;
    private float musicVolume = 0.5f;

    private boolean sfxMuted = false;
    private boolean musicMuted = false;

    public SoundManager() {
        soundEffects = new HashMap<>();
        musicTracks = new HashMap<>();
    }

    public void loadSound(String name, String filename) {
        try {
            File audioFile = new File(SOUNDS_PATH + filename);
            if (!audioFile.exists()) {
                System.err.println("Sound file not found: " + filename);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            soundEffects.put(name, clip);
            System.out.println("Loaded sound: " + name);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Failed to load sound " + filename + ": " + e.getMessage());
        }
    }

    public void loadMusic(String name, String filename) {
        try {
            File audioFile = new File(SOUNDS_PATH + filename);
            if (!audioFile.exists()) {
                System.err.println("Music file not found: " + filename);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            musicTracks.put(name, clip);
            System.out.println("Loaded music: " + name);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Failed to load music " + filename + ": " + e.getMessage());
        }
    }

    public void loadAllSounds() {
        // ========= SOUND EFFECTS =========
        loadSound("freeze", "freeze_ray.wav");

        // ========= MUSIC TRACKS ========== 
        loadMusic("menu", "menu.music.wav");

        System.out.println("Sound loading complete.");
    }

    public void playSound(String name) {
        if (sfxMuted) return;

        Clip clip = soundEffects.get(name);
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            setClipVolume(clip, sfxVolume);
            clip.start();
        }
    }

    public void playSound(String name, float volume) {
        if (sfxMuted) return;

        Clip clip = soundEffects.get(name);
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            setClipVolume(clip, volume * sfxVolume);
            clip.start();
        }
    }

    public void playMusic(String name) {
        if (name.equals(currentMusicName) && currentMusic != null && currentMusic.isRunning()) {
            return;
        }

        stopMusic();

        Clip clip = musicTracks.get(name);
        if (clip != null) {
            clip.setFramePosition(0);
            setClipVolume(clip, musicMuted ? 0 : musicVolume);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            currentMusic = clip;
            currentMusicName = name;
        }
    }

    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.setFramePosition(0);
            currentMusic = null;
            currentMusicName = null;
        }
    }

    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isRunning()) {
            currentMusic.stop();
        }
    }

    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isRunning() && !musicMuted) {
            currentMusic.start();
        }
    }

    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0, Math.min(1, volume));
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
        if (currentMusic != null && !musicMuted) {
            setClipVolume(currentMusic, musicVolume);
        }
    }

    public void toogleSfxMute() {
        sfxMuted = !sfxMuted;
    }

    public void toggleMusicMute() {
        musicMuted = !musicMuted;
        if (currentMusic != null) {
            setClipVolume(currentMusic, musicMuted ? 0 : musicVolume);
        }
    }

    public boolean isSfxMuted() { return sfxMuted; }
    public boolean isMusicMuted() { return musicMuted; }
    public float getSfxVolume() { return sfxVolume; }
    public float getMusicVolume() { return musicVolume; }

    private void setClipVolume(Clip clip, float volume) {
        try {
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            float dB;
            if (volume <= 0) {
                dB = gainControl.getMinimum();
            } else {
                dB = (float) (20 * Math.log10(volume));
                dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
            }

            gainControl.setValue(dB);
        } catch (IllegalArgumentException e) {
        }
    }

    public void dispose() {
        stopMusic();

        for (Clip clip : soundEffects.values()) {
            clip.close();
        }
        soundEffects.clear();

        for (Clip clip : musicTracks.values()) {
            clip.close();
        }
        musicTracks.clear();

    }

}