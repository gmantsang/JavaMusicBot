package ovh.not.javamusicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.MusicBot;
import ovh.not.javamusicbot.TrackScheduler;

import java.util.Optional;

import static ovh.not.javamusicbot.utils.Utils.getPrivateChannel;

public class GuildAudioController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuildAudioController.class);

    private final GuildAudioControllerState state = new GuildAudioControllerState();

    private final long guildId;
    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final TrackScheduler scheduler;
    private final MusicBot bot;

    GuildAudioController(MusicBot bot, Guild guild, long textChannelId, AudioPlayerManager playerManager) {
        this.bot = bot;
        this.guildId = guild.getIdLong();
        this.playerManager = playerManager;
        this.player = playerManager.createPlayer();

        this.scheduler = new TrackScheduler(bot, this, player, textChannelId);
        this.player.addListener(scheduler);

        AudioPlayerSendHandler sendHandler = new AudioPlayerSendHandler(player);
        guild.getAudioManager().setSendingHandler(sendHandler);
    }

    public GuildAudioControllerState getState() {
        return state;
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public TrackScheduler getScheduler() {
        return scheduler;
    }

    private void submitTask(Runnable runnable) {
        new Thread(runnable).start();
    }

    public void open(VoiceChannel channel, User user) {
        submitTask(() -> {
            Guild guild = bot.getShardManager().getGuildById(guildId);

            if (guild == null) {
                // todo what if guild no longer exists
                LOGGER.error("Error getting guild with ID {} from shard manager", guildId);
                return;
            }

            try {
                final Member self = guild.getSelfMember();

                if (!self.hasPermission(channel, Permission.VOICE_CONNECT)) {
                    throw new PermissionException(Permission.VOICE_CONNECT.getName());
                }

                guild.getAudioManager().openAudioConnection(channel);
                guild.getAudioManager().setSelfDeafened(true);

                state.setConnectionOpen(true); // todo remove this, should be updated by setting voiceChannelId
                state.setVoiceChannelId(Optional.of(channel.getIdLong()));
            } catch (PermissionException e) {
                if (user != null && !user.isBot()) {
                    getPrivateChannel(user).sendMessage("**dabBot does not have permission to connect to the "
                            + channel.getName() + " voice channel.**\nTo fix this, allow dabBot to `View Channel`, " +
                            "`Connect` and `Speak` in that voice channel.\nIf you are not the guild owner, please send " +
                            "this to them.").complete();
                } else {
                    LOGGER.error("an error occured opening voice connection", e);
                }
            }
        });
    }

    public void close() {
        submitTask(() -> {
            Guild guild = bot.getShardManager().getGuildById(guildId);

            if (guild == null) {
                // todo what if guild no longer exists
                LOGGER.error("Error getting guild with ID {} from shard manager", guildId);
                return;
            }

            guild.getAudioManager().closeAudioConnection();

            // todo remove double write locking & unlocking
            state.setConnectionOpen(false);
            state.setVoiceChannelId(Optional.empty());
        });
    }
}
