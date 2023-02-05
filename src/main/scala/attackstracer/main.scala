package attackstracer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import attackstracer.Messages.{Message, Connect, End}


object Messages {
  trait Message

  final case class Connect(from: ActorRef[Message], fromName: String, to: ActorRef[Message], toName: String, proxy: ActorRef[Message], proxyName: String, terminal: ActorRef[Message]) extends Message
  //final case class Connect(from: ActorRef[Connect], fromName: String, to: ActorRef[Connect], toName: String, proxy: ActorRef[Connect], proxyName: String, terminal: ActorRef[Message]) extends Message
  final case class End() extends Message

}

object PC1 {
  def apply(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Connect(from, fromName, to, toName, proxy, proxyName, terminal) => {
        context.log.info(s"${fromName} => ${proxyName}")
        proxy ! Connect(from, fromName, to, toName, proxy, proxyName, terminal)
        Behaviors.same
      }
    }
  }
}

object PC2 {
  def apply(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Connect(from, fromName, to, toName, proxy, proxyName, terminal) => {
        context.log.info(s"${fromName} => ${proxyName}")
        proxy ! Connect(from, fromName, to, toName, proxy, proxyName, terminal)
        Behaviors.same
      }
    }
  }
}

object PC3 {
  def apply(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Connect(from, fromName, to, toName, proxy, proxyName, terminal) => {
        context.log.info(s"${fromName} => ${proxyName}")
        proxy ! Connect(from, fromName, to, toName, proxy, proxyName, terminal)
        Behaviors.same
      }
    }
  }
}

object Proxy {
  def apply(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Connect(from, fromName, to, toName, proxy, proxyName, terminal) => {
        context.log.info(s"${proxyName} => ${toName}")
        to ! Connect(from, fromName, to, toName, proxy, proxyName, terminal)
        Behaviors.same
      }
    }
  }
}

object WebServer {
  def apply(): Behavior[Message] = Behaviors.receive { (context, message) =>
    message match {
      case Connect(from, fromName, to, toName, proxy, proxyName, terminal) => {
        context.log.info(s"WebServer receives a message from ${fromName}")
        terminal ! End()
        Behaviors.same
      }
    }
  }
}

//object PC1 {
//  def apply(): Behavior[Connect] = Behaviors.receive { (context, message) =>
//    context.log.info(s"${message.fromName} => ${message.proxyName}")
//    message.proxy ! Connect(message.from, message.fromName, message.to, message.toName, message.proxy, message.proxyName, message.terminal)
//    Behaviors.same
//  }
//}
//
//object PC2 {
//  def apply(): Behavior[Connect] = Behaviors.receive { (context, message) =>
//    context.log.info(s"${message.fromName} => ${message.proxyName}")
//    message.proxy ! Connect(message.from, message.fromName, message.to, message.toName, message.proxy, message.proxyName, message.terminal)
//    Behaviors.same
//  }
//}
//
//object PC3 {
//  def apply(): Behavior[Connect] = Behaviors.receive { (context, message) =>
//    context.log.info(s"${message.fromName} => ${message.proxyName}")
//    message.proxy ! Connect(message.from, message.fromName, message.to, message.toName, message.proxy, message.proxyName, message.terminal)
//    Behaviors.same
//  }
//}
//
//object Proxy {
//  def apply(): Behavior[Connect] = Behaviors.receive { (context, message) =>
//    context.log.info(s"${message.proxyName} => ${message.toName}")
//    message.to ! Connect(message.from, message.fromName, message.to, message.toName, message.proxy, message.proxyName, message.terminal)
//    Behaviors.same
//  }
//}
//
//object WebServer {
//  def apply(): Behavior[Connect] = Behaviors.receive { (context, message) =>
//    context.log.info(s"WebServer receives a message from ${message.fromName}")
//    //最後にTerminalにメッセージを送ってTerminalアクターを終了させないとMainアクターも終了シグナルを検知して終了できない
//    message.terminal ! End()
//    Behaviors.same
//  }
//}

object Terminal {
  def apply(): Behavior[Message] = Behaviors.setup[Message] { context =>
    val PC1Ref = context.spawn(PC1(), "PC1")
    val PC2Ref = context.spawn(PC2(), "PC2")
    val PC3Ref = context.spawn(PC3(), "PC3")
    val ProxyRef = context.spawn(Proxy(), "Proxy")
    val WebServerRef = context.spawn(WebServer(), "WebServer")
    print(">")
    val command = io.StdIn.readLine().split(' ')
    if (command(0) == "send") {
      if (command(1) == "PC1") {
        PC1Ref ! Connect(PC1Ref, "PC1", WebServerRef, "WebServer", ProxyRef, "Proxy", context.self)
        loopTerminal(PC1Ref, "PC1", PC2Ref, "PC2", PC3Ref, "PC3", ProxyRef, "Proxy", WebServerRef, "WebServer", context.self)
      }
      else if (command(1) == "PC2") {
        PC2Ref ! Connect(PC2Ref, "PC2", WebServerRef, "WebServer", ProxyRef, "Proxy", context.self)
        loopTerminal(PC1Ref, "PC1", PC2Ref, "PC2", PC3Ref, "PC3", ProxyRef, "Proxy", WebServerRef, "WebServer", context.self)
      }
      else if (command(1) == "PC3") {
        PC3Ref ! Connect(PC3Ref, "PC3", WebServerRef, "WebServer", ProxyRef, "Proxy", context.self)
        loopTerminal(PC1Ref, "PC1", PC2Ref, "PC2", PC3Ref, "PC3", ProxyRef, "Proxy", WebServerRef, "WebServer", context.self)
      }
      else {
        println("入力されたコマンドは使用できません!")
        loopTerminal(PC1Ref, "PC1", PC2Ref, "PC2", PC3Ref, "PC3", ProxyRef, "Proxy", WebServerRef, "WebServer", context.self)
      }
    }
    else if (command(0) == "exit") {
      Behaviors.stopped
    }
    else {
      println("入力されたコマンドは使用できません")
      Behaviors.stopped
    }
  }

  def loopTerminal(PC1Ref: ActorRef[Message], PC1Name: String, PC2Ref: ActorRef[Message], PC2Name: String, PC3Ref: ActorRef[Message], PC3Name: String, ProxyRef: ActorRef[Message], ProxyName: String, WebServerRef: ActorRef[Message], WebServerName: String, Terminal: ActorRef[Message]): Behavior[Message] = {
    Behaviors.receiveMessage { message =>
      print(">")
      val command = io.StdIn.readLine().split(' ')
      if (command(0) == "send") {
        if (command(1) == "PC1") {
          PC1Ref ! Connect(PC1Ref, "PC1", WebServerRef, "WebServer", ProxyRef, "Proxy", Terminal)
          loopTerminal(PC1Ref, "PC1", PC2Ref, "PC2", PC3Ref, "PC3", ProxyRef, "Proxy", WebServerRef, "WebServer", Terminal)
        }
        else if (command(1) == "PC2") {
          PC2Ref ! Connect(PC2Ref, "PC2", WebServerRef, "WebServer", ProxyRef, "Proxy", Terminal)
          loopTerminal(PC1Ref, "PC1", PC2Ref, "PC2", PC3Ref, "PC3", ProxyRef, "Proxy", WebServerRef, "WebServer", Terminal)
        }
        else if (command(1) == "PC3") {
          PC3Ref ! Connect(PC3Ref, "PC3", WebServerRef, "WebServer", ProxyRef, "Proxy", Terminal)
          loopTerminal(PC1Ref, "PC1", PC2Ref, "PC2", PC3Ref, "PC3", ProxyRef, "Proxy", WebServerRef, "WebServer", Terminal)
        }
        else {
          println("入力されたコマンドは使用できません")
          Behaviors.stopped
        }
      }
      else if (command(0) == "exit") {
        Behaviors.stopped
      }
      else {
        println("入力されたコマンドは使用できません")
        Behaviors.stopped
      }
    }
  }
}


object Main extends App {
  def apply(): Behavior[Any]= Behaviors.setup[Any]{ context =>
    val terminalRef = context.spawn(Terminal(), "terminal")
    context.watch(terminalRef)
    Behaviors.receiveSignal{
      case (context, Terminated(ref)) if ref == terminalRef =>
        context.log.info("正常に終了しました")
        Behaviors.stopped
    }
  }




  ActorSystem(apply(), "main")
}