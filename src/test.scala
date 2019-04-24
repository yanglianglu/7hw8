import clicker2.networking.Database
import org.scalatest.FunSuite

class test extends FunSuite{
  test(""){
    val db = Database
    db.createPlayer("13")
    assert(db.playerExists("13"))
  }

}
