package com.danielasfregola.twitter4s
package http.clients.streaming.statuses

import com.danielasfregola.twitter4s.entities.enums.Language.Language
import com.danielasfregola.twitter4s.entities.streaming.CommonStreamingMessage
import com.danielasfregola.twitter4s.http.clients.streaming.statuses.parameters._
import com.danielasfregola.twitter4s.http.clients.streaming.{StreamingClient, TwitterStream}
import com.danielasfregola.twitter4s.util.Configurations._

import scala.concurrent.Future

trait TwitterStatusClient {

  protected val streamingClient: StreamingClient

  private val statusUrl = s"$statusStreamingTwitterUrl/$twitterVersion/statuses"

  /** Starts a streaming connection from Twitter's public API, filtered with the 'follow', 'track' and 'location' parameters.
    * Although all of those three params are optional, at least one must be specified.
    * The track, follow, and locations fields should be considered to be combined with an OR operator.
    * The function returns a future of a `TwitterStream` that can be use to close or replace the stream when needed.
    * If there are failures in establishing the initial connection, the Future returned will be completed with a failure.
    * Since it's an asynchronous event stream, all the events will be parsed as entities of type `CommonStreamingMessage`
    * and processed accordingly to the partial function `f`. All the messages that do not match `f` are automatically ignored.
    * For more information see
    * <a href="https://developer.twitter.com/en/docs/tweets/filter-realtime/api-reference/post-statuses-filter.html" target="_blank">
    *   https://developer.twitter.com/en/docs/tweets/filter-realtime/api-reference/post-statuses-filter.html</a>.
    *
    * @param follow : Empty by default. A comma separated list of user IDs, indicating the users to return statuses for in the stream.
    *                 For more information <a href="https://dev.twitter.com/streaming/overview/request-parameters#follow" target="_blank">
    *                   https://dev.twitter.com/streaming/overview/request-parameters#follow</a>
    * @param tracks : Empty by default. Keywords to track. Phrases of keywords are specified by a comma-separated list.
    *                For more information <a href="https://dev.twitter.com/streaming/overview/request-parameters#track" target="_blank">
    *                  https://dev.twitter.com/streaming/overview/request-parameters#track</a>
    * @param locations : Empty by default. Specifies a set of bounding boxes to track.
    *                    For more information <a href="https://dev.twitter.com/streaming/overview/request-parameters#locations" target="_blank">
    *                      https://dev.twitter.com/streaming/overview/request-parameters#locations</a>
    * @param languages : Empty by default. A comma separated list of 'BCP 47' language identifiers.
    *                    For more information <a href="https://dev.twitter.com/streaming/overview/request-parameters#language" target="_blank">
    *                      https://dev.twitter.com/streaming/overview/request-parameters#language</a>
    * @param stall_warnings : Default to false. Specifies whether stall warnings (`WarningMessage`) should be delivered as part of the updates.
    * @param f: the function that defines how to process the received messages
    */
  def filterStatuses(follow: Seq[Long] = Seq.empty,
                     tracks: Seq[String] = Seq.empty,
                     locations: Seq[Double] = Seq.empty,
                     languages: Seq[Language] = Seq.empty,
                     stall_warnings: Boolean = false)(f: PartialFunction[CommonStreamingMessage, Unit]): Future[TwitterStream] = {
    import streamingClient._
    require(follow.nonEmpty || tracks.nonEmpty || locations.nonEmpty, "At least one of 'follow', 'tracks' or 'locations' needs to be non empty")
    val filters = StatusFilters(follow, tracks, locations, languages, stall_warnings)
    preProcessing()
    Post(s"$statusUrl/filter.json", filters).processStream(f)
  }

  /** Starts a streaming connection from Twitter's public API, which is a a small random sample of all public statuses.
    * The Tweets returned by the default access level are the same, so if two different clients connect to this endpoint, they will see the same Tweets.
    * The function returns a future of a `TwitterStream` that can be use to close or replace the stream when needed.
    * If there are failures in establishing the initial connection, the Future returned will be completed with a failure.
    * Since it's an asynchronous event stream, all the events will be parsed as entities of type `CommonStreamingMessage`
    * and processed accordingly to the partial function `f`. All the messages that do not match `f` are automatically ignored.
    * For more information see
    * <a href="https://developer.twitter.com/en/docs/tweets/sample-realtime/overview/GET_statuse_sample" target="_blank">
    *   https://developer.twitter.com/en/docs/tweets/sample-realtime/overview/GET_statuse_sample</a>.
    *
    * @param languages : Empty by default. A comma separated list of 'BCP 47' language identifiers.
    *                    For more information <a href="https://dev.twitter.com/streaming/overview/request-parameters#language" target="_blank">
    *                      https://dev.twitter.com/streaming/overview/request-parameters#language</a>
    * @param stall_warnings : Default to false. Specifies whether stall warnings (`WarningMessage`) should be delivered as part of the updates.
    * @param f: the function that defines how to process the received messages
    */
  def sampleStatuses(languages: Seq[Language] = Seq.empty,
                     stall_warnings: Boolean = false)
                    (f: PartialFunction[CommonStreamingMessage, Unit]): Future[TwitterStream] = {
    import streamingClient._
    val parameters = StatusSampleParameters(languages, stall_warnings)
    preProcessing()
    Get(s"$statusUrl/sample.json", parameters).processStream(f)
  }

  /** Starts a streaming connection from Twitter's firehose API of all public statuses.
    * Few applications require this level of access.
    * Creative use of a combination of other resources and various access levels can satisfy nearly every application use case.
    * For more information see <a href="https://dev.twitter.com/streaming/reference/get/statuses/firehose" target="_blank">
    *   https://dev.twitter.com/streaming/reference/get/statuses/firehose</a>.
    * The function returns a future of a `TwitterStream` that can be use to close or replace the stream when needed.
    * If there are failures in establishing the initial connection, the Future returned will be completed with a failure.
    * Since it's an asynchronous event stream, all the events will be parsed as entities of type `CommonStreamingMessage`
    * and processed accordingly to the partial function `f`. All the messages that do not match `f` are automatically ignored.
    *
    * @param count: Optional. The number of messages to backfill.
    *               For more information see <a href="https://dev.twitter.com/streaming/overview/request-parameters#count" target="_blank">
    *                 https://dev.twitter.com/streaming/overview/request-parameters#count</a>
    * @param languages : Empty by default. A comma separated list of 'BCP 47' language identifiers.
    *                    For more information <a href="https://dev.twitter.com/streaming/overview/request-parameters#language" target="_blank">
    *                      https://dev.twitter.com/streaming/overview/request-parameters#language</a>
    * @param stall_warnings : Default to false. Specifies whether stall warnings (`WarningMessage`) should be delivered as part of the updates.
    * @param f: the function that defines how to process the received messages.
    */
  def firehoseStatuses(count: Option[Int] = None,
                       languages: Seq[Language] = Seq.empty,
                       stall_warnings: Boolean = false)
                      (f: PartialFunction[CommonStreamingMessage, Unit]): Future[TwitterStream] = {
    import streamingClient._
    val maxCount = 150000
    require(Math.abs(count.getOrElse(0)) <= maxCount, s"count must be between -$maxCount and +$maxCount")
    val parameters = StatusFirehoseParameters(languages, count, stall_warnings)
    preProcessing()
    Get(s"$statusUrl/firehose.json", parameters).processStream(f)
  }
}
