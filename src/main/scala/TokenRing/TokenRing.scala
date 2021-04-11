package TokenRing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.collection.mutable

case class Start(left : ActorRef, right : ActorRef)
case object TokenM//clockwise motion
case object TokenL//counter-clockwise motion
case class Marker(sender: ActorRef)

class TokenRing extends Actor {
  var leftNeighbor, rightNeighbor, name: String = ""
//  var rightNeighbor: String = ""
//  var name = ""
  var inSnap = false;
  var leftOpen, rightOpen = true;
  var markerCount = 0
  var msgMap = Map[String, mutable.Queue[String]]()
  var snapshot = ""
  var counter = Array(0, 0) //(L, M)
  def receive = {
    case Marker(sender) =>
      if (!inSnap){

        inSnap = true
        snapshot = "Snapshot of " + self.path.name +" is "+counter(0) + " and " + counter(1)
        println("Recording started for: "+ self.path.name)
        if (sender.path.toString == leftNeighbor){
          msgMap(leftNeighbor).removeAll()
          leftOpen = false;
          markerCount+=1
        }
        if (sender.path.toString == rightNeighbor){
          msgMap(rightNeighbor).removeAll()
          rightOpen = false;
          markerCount+=1
        }
        context.actorSelection(leftNeighbor) ! Marker(self)
        context.actorSelection(rightNeighbor) ! Marker(self)

      }
      else {
        markerCount+=1
        if (sender.path.toString == leftNeighbor) {
          leftOpen = false
        };

        if (sender.path.toString == rightNeighbor){
          rightOpen = false;
        }
        if (markerCount==2){
          var leftTransit = ""
          var rightTransit = ""
          if (!msgMap(rightNeighbor).isEmpty){
            leftTransit ="Left: " +msgMap(rightNeighbor).toString()
          }
          if (!msgMap(leftNeighbor).isEmpty){
            rightTransit ="Right: "+msgMap(leftNeighbor).toString()
          }

          println(snapshot + "and messages in transit to "+leftTransit+rightTransit)
          inSnap = false
          msgMap(rightNeighbor).removeAll()
          msgMap(leftNeighbor).removeAll()
          markerCount=0
          leftOpen = true
          rightOpen = true;
        }


      }


    case TokenM =>
      if (inSnap && rightOpen){
        msgMap(rightNeighbor)+= "M"
      }
      counter(1) += 1

      println(name +" M is "+counter(1))
      Thread.sleep(1000)
      context.actorSelection(leftNeighbor) ! TokenM


    case TokenL =>
      if (inSnap && leftOpen){
        msgMap(leftNeighbor)+= "L"
      }
      counter(0) += 1
      println(name +" L is "+counter(0))
      Thread.sleep(1000)
      context.actorSelection(rightNeighbor) ! TokenL

    case Start(left, right) =>
      leftNeighbor = left.path.toString()
      rightNeighbor = right.path.toString()
      name = self.path.name
      msgMap = Map(leftNeighbor -> mutable.Queue[String](), rightNeighbor -> mutable.Queue[String]())


  }
}

object Server extends App {
  val system = ActorSystem("TokenRing")
  val actor1 = system.actorOf(Props[TokenRing](), name = "actor1")
  val actor2 = system.actorOf(Props[TokenRing](), name = "actor2")
  val actor3 = system.actorOf(Props[TokenRing](), name = "actor3")
  actor1 ! Start(actor2, actor3)

  actor2 ! Start(actor3, actor1)

  actor3 ! Start(actor1, actor2)

  actor1 ! TokenL
  actor1 ! TokenM
  Thread.sleep(1000)

  println("Ready")
  Thread.sleep(2000)

  var input = ""
  while (input != "stop"){
    input= scala.io.StdIn.readLine()
    actor2 ! Marker(actor2)
  }
  system.terminate()


}
