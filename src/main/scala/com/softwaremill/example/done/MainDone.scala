package com.softwaremill.example.done

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.softwaremill.events.{EventsDatabase, EventsModule, Registry}
import com.softwaremill.example.{SchemaUpdate, TrollModel}
import com.softwaremill.id.DefaultIdGenerator
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success}

object MainDone extends App with StrictLogging with EventsModule {
  implicit lazy val _system = ActorSystem("slick-eventsourcing")
  implicit lazy val _materializer = ActorMaterializer()
  implicit lazy val ec = _system.dispatcher

  val dbUrl = "jdbc:h2:file:./data/slickeventsourcing"

  SchemaUpdate.update(dbUrl)

  lazy val idGenerator = new DefaultIdGenerator(datacenterId = 1)

  // ---

  lazy val eventsDatabase = EventsDatabase.createH2(dbUrl)
  lazy val trollModel = new TrollModel(eventsDatabase)

  lazy val commands = new CommandsDone(trollModel)
  lazy val eventListeners = new EventListenersDone()
  lazy val modelUpdates = new ModelUpdatesDone(trollModel)

  lazy val registry = Registry()
    .registerEventListener(eventListeners.notifyTrollOversightCouncil)
    .registerEventListener(eventListeners.equipWithAxe)
    .registerModelUpdate(modelUpdates.addedUpdate)
    .registerModelUpdate(modelUpdates.equipmentUpdate)

  lazy val routes = new RoutesDone(eventsDatabase, eventMachine, trollModel, commands)

  // ---

  lazy val routesWithIndex = routes.routes ~
      path("") {
        getFromResource("index.html")
      }

  Http()
    .bindAndHandle(routesWithIndex, "localhost", 8080)
    .onComplete {
      case Success(b) => logger.info(s"Server started")
      case Failure(e) => logger.error(s"Cannot start server", e)
    }
}
