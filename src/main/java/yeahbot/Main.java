package yeahbot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String YEAH_GROUP = "yeah";
    private static final String MOOD_GROUP = "mood";
    private static final String yeahRegex = "y[y\\s]*(e[e\\s]*a[a\\s]*|e[e\\s]*|a[a\\s]*)(h[h\\s]*)?";
    private static final String regex = "\\b(?<" + YEAH_GROUP + ">" + yeahRegex + ")(\\b|b[b\\s]*o[o\\s]*t[t\\s]*)[^\".?!]*(?<" + MOOD_GROUP + ">[\".?!]+)?";
    private static final String exclamationRegex = "^!{2,}$";

    private static Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    private static Pattern exclamationPattern = Pattern.compile(exclamationRegex, Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        String token;
        try {
            token = System.getenv("TOKEN");
        } catch (Exception e) {
            logger.error("Failed to get TOKEN env", e);
            System.exit(1);
            return;
        }

        if (token == null) {
            logger.error("No TOKEN env provided");
            System.exit(1);
            return;
        }

        DiscordClientBuilder.create(token)
            .build()
            .withGateway(client -> {
                Mono<Void> onReady = client.getEventDispatcher().on(ReadyEvent.class)
                    .doOnNext(ready ->
                        logger.info("Logged in as " + ready.getSelf().getUsername())
                    ).then();

                Mono<Void> ping = client.getEventDispatcher().on(MessageCreateEvent.class)
                    .map(MessageCreateEvent::getMessage)
                    .filter(message -> !message.getAuthor().get().isBot())
                    .flatMap(message -> {
                        Matcher matcher = pattern.matcher(message.getContent());
                        return Mono.just(message)
                            .filter(msg -> matcher.find())
                            .flatMap(Message::getChannel)
                            .flatMap(channel -> {
                                String yeahText = matcher.group(YEAH_GROUP);
                                yeahText = yeahText.trim();

                                String moodText = matcher.group(MOOD_GROUP);
                                moodText = moodText != null ? moodText : "";
                                moodText = moodText.trim();

                                boolean quoteMode = moodText.equals("\"");
                                boolean exclamationMode = exclamationPattern.matcher(moodText).matches();

                                return channel.createMessage(
                                    (exclamationMode ? "**" : "") +
                                    (quoteMode ? "\"" : "") +
                                    yeahText +
                                    (quoteMode ? "\" - yeahbot" : moodText) +
                                    (exclamationMode ? "**" : "")
                                );
                            });
                    })
                    .onErrorResume(throwable -> {
                        logger.error("Failed to handle message", throwable);
                        return Mono.empty();
                    })
                    .then();

                return Mono.when(onReady, ping);
            })
            .block();
    }
}
