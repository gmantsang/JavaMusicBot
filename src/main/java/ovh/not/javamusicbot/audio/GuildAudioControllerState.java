package ovh.not.javamusicbot.audio;

import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GuildAudioControllerState {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private boolean isConnectionOpen = false;
    private Optional<Long> voiceChannelId = Optional.empty();

    public boolean isConnectionOpen() {
        rwLock.readLock().lock();
        try {
            return isConnectionOpen;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void setConnectionOpen(boolean connectionOpen) {
        rwLock.writeLock().lock();
        try {
            isConnectionOpen = connectionOpen;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Optional<Long> getVoiceChannelId() {
        rwLock.readLock().lock();
        try {
            return voiceChannelId;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void setVoiceChannelId(Optional<Long> voiceChannelId) {
        rwLock.writeLock().lock();
        try {
            this.voiceChannelId = voiceChannelId;
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}