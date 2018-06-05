package ovh.not.javamusicbot.listener;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ResumedEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class PromStatsListener extends ListenerAdapter {
    static final Gauge guilds = Gauge.build()
            .name("dab_guilds_total").help("Total guilds.")
            .labelNames("shard").register();
    static final Counter resumes = Counter.build()
            .name("dab_resumes_total").help("Total resumes.")
            .register();
    static final Counter ready = Counter.build()
            .name("dab_ready_total").help("Total ready events.")
            .register();

    @Override
    public void onResume(ResumedEvent event) {
        resumes.inc();
    }

    @Override
    public void onReady(ReadyEvent event) {
        ready.inc();
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        setGuildCount(event.getJDA());
    }

    @Override
    public void onGuildLeave(GuildLeaveEvent event) {
        setGuildCount(event.getJDA());
    }

    private final void setGuildCount(JDA jda) {
        long count = jda.getGuildCache().size();
        String shard = Integer.toString(jda.getShardInfo().getShardId());

        guilds.labels(shard).set(count);
    }
}
