package yeahbot

import com.typesafe.scalalogging.Logger
import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.publisher.{Flux, Mono}

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}
import scala.jdk.OptionConverters.*
import scala.util.matching.Regex

private val logger = Logger("yeah")

private val yeahRegex: Regex = "(?i)\\b(?<yeah>y[y\\s]*(e[e\\s]*a[a\\s]*|e[e\\s]*|a[a\\s]*)(h[h\\s]*)?)(\\b|b[b\\s]*o[o\\s]*t[t\\s]*)[^\".?!]*(?<mood>[.?!][.?!\\s]*)?(?<quote>\")?".r
private val exclamationRegex: Regex = "!{2,}".r

def run(token: String): Unit =
  val client = DiscordClientBuilder.create(token).build();

  client
    .withGateway(gateway => {
      val onReady = gateway.getEventDispatcher.on(classOf[ReadyEvent])
        .doOnNext(ready => {
          logger.info(s"Logged in as ${ready.getSelf.getUsername}")
        })
        .`then`()

      val onYeah = gateway.getEventDispatcher.on(classOf[MessageCreateEvent])
        .map(_.getMessage)
        .filter(_.getAuthor.toScala match {
          case Some(author) => !author.isBot
          case None => false
        })
        .flatMap(message =>
          yeahRegex.findFirstMatchIn(message.getContent) match
            case Some(firstMatch) => {
              val yeah = firstMatch.group("yeah").trim
              val mood = Option(firstMatch.group("mood")) match
                case Some(value) => value.trim
                case None => ""

              val isQuoted = Option(firstMatch.group("quote")) match
                case Some(_) => true
                case None => false
              val isExclamation = exclamationRegex.matches(mood)

              def punctuate(text: String) = s"$text$mood"
              def bold(text: String) = s"**$text**"
              def quote(text: String) = s"*“*​${text}​*” — yeahbot*"

              message.getChannel.flatMap(_.createMessage(
                Function.chain(Seq(
                  punctuate,
                  if isExclamation then bold else identity,
                  if isQuoted then quote else identity
                ))(yeah)
              ))
            }
            case None => Mono.empty()
        )
        .onErrorResume(throwable => {
          logger.error("Failed to handle message", throwable)
          Mono.empty()
        })
        .`then`()

      onReady and onYeah
    })
    .block()

@main
def main(): Unit =
  sys.env.get("TOKEN") match
    case Some(token) => run(token)
    case None =>
      logger.error("Failed to get TOKEN evn")
      System.exit(1)

