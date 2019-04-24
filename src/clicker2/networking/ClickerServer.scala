package clicker2.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import play.api.libs.json.Json


case object UpdateGames

case object AutoSave

case class GameState(gameState: String)

class ClickerServer extends Actor {
  import Tcp._
  import context.system
  IO(Tcp) ! Bind(self, new  InetSocketAddress("localhost", 8000))
  var clients : scala.collection.mutable.Map[String,ActorRef] = scala.collection.mutable.Map()
  var user : Set[ActorRef] = Set()
  override def receive: Receive = {
    case b: Bound => println("Listening on port: " + b.localAddress.getPort)
    case c: Connected =>
      this.user = this.user + sender()
      sender() ! Register(self)
    case PeerClosed =>
      this.user = this.user - sender()
    case received: Received => {

      var data = received.data.utf8String
      var parse = Json.parse(data)
      var action = (parse \ "action").as[String]
      var username = (parse \ "“username").as[String]

      if(action == "connected"){
        val childActor = context.actorOf(Props(classOf[GameActor], username))
        childActor ! Setup
        this.clients = this.clients + (username -> childActor)
        sender() ! Register(self)
                                }
      else if(action == "disconnected"){
        this.clients(username) ! PoisonPill
        this.clients -= username
                                        }
      else if(action == "clickGold")
        this.clients(username) ! ClickGold

      else if(action == "buyEquipment"){
        val equipment = (parse \ "equipmentID").as[String]
        this.clients(username) ! BuyEquipment(equipment)
                                        }
                                }
    case UpdateGames => {
      for((key,value) <- this.clients){
          value ! Update
                                  }
    }
    case AutoSave => {
      for((key,value) <- this.clients){
        value ! Save
      }
    }
    case gs: GameState =>{
      val delimiter = "~"
        this.user.foreach((client : ActorRef) => client ! Write(ByteString(gs.gameState + delimiter)))
                          }
  }
}


object ClickerServer {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem()

    import actorSystem.dispatcher

    import scala.concurrent.duration._

    val server = actorSystem.actorOf(Props(classOf[ClickerServer]))

    actorSystem.scheduler.schedule(0 milliseconds, 100 milliseconds, server, UpdateGames)
    actorSystem.scheduler.schedule(0 milliseconds, 5000 milliseconds, server, AutoSave)
  }
}
