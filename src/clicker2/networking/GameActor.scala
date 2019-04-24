package clicker2.networking

import akka.actor.Actor
import Database._
import clicker2.Game
import play.api.libs.json.Json
case object Update

case object ClickGold

case object Save

case object Setup

case class BuyEquipment(equipmentID: String)

class GameActor(username: String) extends Actor {
//  setupTable()
//  playerExists(username: String)
//  createPlayer(username: String)
//  saveGame(username: String, gold: Double, shovels: Int, excavators: Int, mines: Int, lastUpdate: Long)
//  loadGame()
  var game = new Game(username)
  override def receive: Receive = {
    case Setup => {
      setupTable()
      if(playerExists(username))
        loadGame(username,game)
      else {
        createPlayer(username)
        loadGame(username,game)
            }

                  }
    case Update =>{
      val now : Long = System.nanoTime()
      game.update(now)
      sender() ! GameState(game.toJSON())
    }
    case Save => {
      val now : Long = System.nanoTime()
      game.update(now)
      val json = Json.parse(game.toJSON())
      val gold = (json \ "gold").as[Double]
      val shovels = (json \ "equipment" \ "shovel" \ "numberOwned").as[Int]
      val excavators = (json \ "equipment" \ "excavator" \ "numberOwned").as[Int]
      val mines = (json \ "equipment" \ "mine" \ "numberOwned").as[Int]
      val lastUpdate = (json \ "lastUpdateTime").as[Long]
      saveGame(username,gold,shovels,excavators,mines,lastUpdate)
    }
    case ClickGold => game.clickGold()
    case buy: BuyEquipment => game.buyEquipment(buy.equipmentID)
  }
}
