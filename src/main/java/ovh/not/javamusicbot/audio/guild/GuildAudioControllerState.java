package ovh.not.javamusicbot.audio.guild;

import io.prometheus.client.Gauge;

import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GuildAudioControllerState {
    static final Gauge audioStreams = Gauge.build()
            .name("dab_streams_total").help("Total audio streams.")
            .register();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private Optional<Long> voiceChannelId = Optional.empty();

    public boolean isConnectionOpen() {
        rwLock.readLock().lock();
        try {
            return voiceChannelId.isPresent();
        } finally {
            rwLock.readLock().unlock();
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

    private void setVoiceChannelId(Optional<Long> voiceChannelId) {
        rwLock.writeLock().lock();
        try {
            this.voiceChannelId = voiceChannelId;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void setVoiceConnectionOpen(long voiceChannelId) {
        setVoiceChannelId(Optional.of(voiceChannelId));
        audioStreams.inc();
    }

    public void setVoiceConnectionClosed() {
        setVoiceChannelId(Optional.empty());
        audioStreams.dec();
    }
}