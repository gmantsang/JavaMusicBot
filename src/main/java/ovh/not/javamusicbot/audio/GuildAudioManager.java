package ovh.not.javamusicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import ovh.not.javamusicbot.MusicBot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuildAudioManager {
    private final MusicBot bot;
    
    private final Map<Long, GuildAudioController> guildAudioControllers = new ConcurrentHashMap<>();

    public GuildAudioManager(MusicBot bot) {
        this.bot = bot;
    }

    public GuildAudioController getOrCreate(Guild guild, TextChannel textChannel, AudioPlayerManager playerManager) {
        GuildAudioController manager = guildAudioControllers.computeIfAbsent(guild.getIdLong(), $ ->
                new GuildAudioController(bot, guild, textChannel.getIdLong(), playerManager));

        manager.getScheduler().setTextChannelId(textChannel.getIdLong());
        return manager;
    }

    public GuildAudioController get(long guildId) {
        return guildAudioControllers.get(guildId);
    }

    public GuildAudioController remove(long guildId) {
        return guildAudioControllers.remove(guildId);
    }

}