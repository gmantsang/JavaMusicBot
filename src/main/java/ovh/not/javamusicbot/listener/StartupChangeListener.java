package ovh.not.javamusicbot.listener;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.StatusChangeEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ovh.not.javamusicbot.Config;
import ovh.not.javamusicbot.MusicBot;

import java.awt.*;
import java.io.IOException;
import java.util.Date;

import static ovh.not.javamusicbot.MusicBot.GSON;
import static ovh.not.javamusicbot.MusicBot.JSON_MEDIA_TYPE;

public class StartupChangeListener extends ListenerAdapter {
    private class GlanceMessage {
        public final int id;
        public final int status;

        public GlanceMessage(StatusChangeEvent event) {
            this.id = event.getEntity().getShardInfo().getShardId();

            switch (event.getNewStatus()) {
                case WAITING_TO_RECONNECT:
                case RECONNECT_QUEUED:
                    status = 1;
                    break;
                case ATTEMPTING_TO_RECONNECT:
                case CONNECTING_TO_WEBSOCKET:
                case IDENTIFYING_SESSION:
                    status = 2;
                    break;
                case CONNECTED:
                    status = 3;
                    break;
                case SHUTTING_DOWN:
                case DISCONNECTED:
                    status = 4;
                    break;
                case FAILED_TO_LOGIN:
                case SHUTDOWN:
                    status = 5;
                    break;
                default:
                    status = 0;
                    break;
            }
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupChangeListener.class);

    private final MusicBot bot;

    public StartupChangeListener(MusicBot bot) {
        this.bot = bot;
    }

    @Override
    public void onStatusChange(StatusChangeEvent event) {
        switch (event.getNewStatus()) {
            case INITIALIZING:
            case INITIALIZED:
            case LOGGING_IN:
            case SHUTDOWN:
            case SHUTTING_DOWN:
            case LOADING_SUBSYSTEMS:
                return;
        }

        JDA.Status oldStatus = event.getOldStatus();
        JDA.Status status = event.getNewStatus();

        LOGGER.info("Status changed from {} to {}", oldStatus.name(), status.name());

        Config config = bot.getConfigs().config;
        if (config.glanceWebhook != null && config.glanceWebhook.length() > 0) {
            GlanceMessage msg = new GlanceMessage(event);

            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, GSON.toJson(msg));

            Request request = new Request.Builder()
                    .url(config.glanceWebhook)
                    .method("POST", body)
                    .build();

            try {
                MusicBot.HTTP_CLIENT.newCall(request).execute().close();
            } catch (IOException e) {
                LOGGER.error("Error posting to glance", e);
            }
        }
        if (config.statusWebhook != null && config.statusWebhook.length() > 0) {
            JDA jda = event.getJDA();
            if (jda.getSelfUser() == null) {
                return;
            }

            Color color;

            switch (status) {
                case FAILED_TO_LOGIN:
                case DISCONNECTED:
                case ATTEMPTING_TO_RECONNECT:
                    color = Color.RED;
                    break;
                case RECONNECT_QUEUED:
                case WAITING_TO_RECONNECT:
                    color = Color.ORANGE;
                    break;
                default:
                    color = Color.GREEN;
            }

            String content = String.format("[%s] %s status changed from %s to %s",
                    jda.getSelfUser().getName(), jda.getShardInfo(), oldStatus.name(), status.name());

            if (status == JDA.Status.ATTEMPTING_TO_RECONNECT) {
                content = String.format("**%s**", content);
            }

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(color)
                    .setDescription(content)
                    .setTimestamp(new Date().toInstant())
                    .build();

            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, embed.toJSONObject().toString());

            Request request = new Request.Builder()
                    .url(config.statusWebhook)
                    .method("POST", body)
                    .addHeader("Authorization", config.statusToken)
                    .build();

            try {
                MusicBot.HTTP_CLIENT.newCall(request).execute().close();
            } catch (IOException e) {
                LOGGER.error("Error posting webhook status", e);
            }
        }
    }
}
